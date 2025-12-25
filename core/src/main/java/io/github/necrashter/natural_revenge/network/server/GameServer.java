package io.github.necrashter.natural_revenge.network.server;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.Disposable;

import io.github.necrashter.natural_revenge.network.NetworkConfig;
import io.github.necrashter.natural_revenge.network.messages.*;
import io.github.necrashter.natural_revenge.network.messages.ConnectionMessages.*;
import io.github.necrashter.natural_revenge.network.messages.PlayerMessages.*;
import io.github.necrashter.natural_revenge.network.messages.EntityMessages.*;
import io.github.necrashter.natural_revenge.network.messages.GameMessages.*;
import io.github.necrashter.natural_revenge.modes.GameMode;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

/**
 * Dedicated game server for multiplayer games.
 * Maintains authoritative game state and handles all client connections.
 */
public class GameServer implements Disposable {
    private NetworkConfig config;
    
    // Network
    private ServerSocket tcpServer;
    private DatagramSocket udpSocket;
    private ExecutorService executor;
    private volatile boolean running = false;
    
    // Player connections
    private final IntMap<PlayerConnection> connections = new IntMap<>();
    private int nextPlayerID = 1;
    private final Object connectionsLock = new Object();
    
    // Game state
    private ServerWorld serverWorld;
    private GameMode gameMode;
    private int currentLevel = 1;
    private float easiness = 1.0f;
    private long seed;
    
    // Server tick
    private int serverTick = 0;
    private float tickAccumulator = 0;
    private float networkAccumulator = 0;
    
    // Server info
    private String serverName = "Frogue Server";
    
    // Listeners
    private final Array<ServerListener> listeners = new Array<>();
    
    public GameServer(NetworkConfig config) {
        this.config = config;
        this.serverName = config.serverName;
        this.seed = System.currentTimeMillis();
    }
    
    /**
     * Start the server
     */
    public void start() throws IOException {
        if (running) return;
        
        running = true;
        executor = Executors.newCachedThreadPool();
        
        // Start TCP server
        tcpServer = new ServerSocket(config.serverPort);
        
        // Start UDP socket
        udpSocket = new DatagramSocket(config.serverPort);
        udpSocket.setSoTimeout(10);
        
        // Start accept thread
        executor.submit(this::acceptLoop);
        
        // Start UDP receive thread
        executor.submit(this::udpReceiveLoop);
        
        // Start query response thread
        executor.submit(this::queryLoop);
        
        log("Server started on port " + config.serverPort);
        
        for (ServerListener listener : listeners) {
            listener.onServerStarted();
        }
    }
    
    /**
     * Stop the server
     */
    public void stop() {
        if (!running) return;
        
        running = false;
        
        // Disconnect all clients
        synchronized (connectionsLock) {
            for (PlayerConnection conn : connections.values()) {
                conn.disconnect("Server shutting down");
            }
            connections.clear();
        }
        
        try {
            if (tcpServer != null) tcpServer.close();
            if (udpSocket != null) udpSocket.close();
            if (executor != null) executor.shutdownNow();
        } catch (Exception ignored) {}
        
        log("Server stopped");
        
        for (ServerListener listener : listeners) {
            listener.onServerStopped();
        }
    }
    
    /**
     * Server update tick
     */
    public void update(float delta) {
        if (!running) return;
        
        tickAccumulator += delta;
        networkAccumulator += delta;
        
        // Fixed timestep physics update
        float tickDelta = config.getServerDelta();
        while (tickAccumulator >= tickDelta) {
            serverTick++;
            
            // Process player inputs
            processPlayerInputs();
            
            // Update world
            if (serverWorld != null) {
                serverWorld.update(tickDelta);
            }
            
            // Update game mode
            if (gameMode != null) {
                gameMode.update(tickDelta);
            }
            
            // Check for disconnections
            checkTimeouts();
            
            tickAccumulator -= tickDelta;
        }
        
        // Network update at configured rate
        float networkDelta = config.getNetworkDelta();
        if (networkAccumulator >= networkDelta) {
            broadcastWorldState();
            networkAccumulator -= networkDelta;
        }
    }
    
    /**
     * Process all pending player inputs
     */
    private void processPlayerInputs() {
        synchronized (connectionsLock) {
            for (PlayerConnection conn : connections.values()) {
                PlayerInputState input;
                while ((input = conn.pollInput()) != null) {
                    processPlayerInput(conn, input);
                }
            }
        }
    }
    
    /**
     * Process a single player input
     */
    private void processPlayerInput(PlayerConnection conn, PlayerInputState input) {
        // Validate and apply input
        ServerPlayer player = conn.getServerPlayer();
        if (player == null || player.isDead()) return;
        
        // Apply movement
        player.applyInput(input);
        
        // Update last processed input sequence
        conn.setLastProcessedInput(input.sequenceNumber);
    }
    
    /**
     * Broadcast world state to all clients
     */
    private void broadcastWorldState() {
        synchronized (connectionsLock) {
            for (PlayerConnection conn : connections.values()) {
                // Send player positions
                for (PlayerConnection other : connections.values()) {
                    PlayerPositionUpdate update = createPositionUpdate(other);
                    conn.sendUDP(update);
                }
                
                // Send entity updates if world exists
                if (serverWorld != null) {
                    EntityUpdateBatch batch = serverWorld.createEntityUpdateBatch(conn);
                    if (batch != null && batch.count > 0) {
                        conn.sendUDP(batch);
                    }
                }
            }
        }
    }
    
    /**
     * Create a position update message for a player
     */
    private PlayerPositionUpdate createPositionUpdate(PlayerConnection conn) {
        ServerPlayer player = conn.getServerPlayer();
        PlayerPositionUpdate update = new PlayerPositionUpdate();
        update.playerID = conn.getPlayerID();
        update.serverTick = serverTick;
        update.lastProcessedInput = conn.getLastProcessedInput();
        
        if (player != null) {
            update.setPosition(player.getPosition());
            update.setVelocity(player.getVelocity());
            update.setForward(player.getForward());
            update.pitch = player.getPitch();
            update.onGround = player.isOnGround();
        }
        
        return update;
    }
    
    /**
     * Check for timed out connections
     */
    private void checkTimeouts() {
        synchronized (connectionsLock) {
            Array<Integer> toRemove = new Array<>();
            
            for (PlayerConnection conn : connections.values()) {
                if (conn.isTimedOut(30000)) {
                    toRemove.add(conn.getPlayerID());
                }
            }
            
            for (int id : toRemove) {
                PlayerConnection conn = connections.get(id);
                if (conn != null) {
                    removePlayer(conn, "Connection timed out");
                }
            }
        }
    }
    
    /**
     * Handle new client connection
     */
    private void handleConnect(Socket socket) {
        try {
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            
            // Read connect request
            Object request = in.readObject();
            if (!(request instanceof ClientConnectRequest)) {
                socket.close();
                return;
            }
            
            ClientConnectRequest connectRequest = (ClientConnectRequest) request;
            
            // Validate
            if (!NetworkConfig.PROTOCOL_VERSION.equals(connectRequest.protocolVersion)) {
                out.writeObject(ClientConnectResponse.failure("Version mismatch"));
                socket.close();
                return;
            }
            
            if (config.serverPassword != null && !config.serverPassword.equals(connectRequest.password)) {
                out.writeObject(ClientConnectResponse.failure("Invalid password"));
                socket.close();
                return;
            }
            
            synchronized (connectionsLock) {
                if (connections.size >= config.maxPlayers) {
                    out.writeObject(ClientConnectResponse.failure("Server is full"));
                    socket.close();
                    return;
                }
                
                // Create player connection
                int playerID = nextPlayerID++;
                PlayerConnection conn = new PlayerConnection(this, socket, in, out, playerID, connectRequest.playerName);
                
                // Create server player
                ServerPlayer serverPlayer = new ServerPlayer(playerID, connectRequest.playerName);
                if (serverWorld != null) {
                    Vector2 spawnPoint = serverWorld.getSpawnPoint();
                    serverPlayer.setPosition(spawnPoint.x, serverWorld.getTerrainHeight(spawnPoint.x, spawnPoint.y) + 1f, spawnPoint.y);
                }
                conn.setServerPlayer(serverPlayer);
                
                connections.put(playerID, conn);
                
                // Send success response
                ClientConnectResponse response = ClientConnectResponse.success(
                    playerID, serverName, config.serverTickRate,
                    config.maxPlayers, connections.size, currentLevel, easiness
                );
                out.writeObject(response);
                out.flush();
                
                // Start connection threads
                conn.start(executor);
                
                // Broadcast player joined
                PlayerJoined joined = new PlayerJoined();
                joined.playerID = playerID;
                joined.playerName = connectRequest.playerName;
                Vector3 pos = serverPlayer.getPosition();
                joined.positionX = pos.x;
                joined.positionY = pos.y;
                joined.positionZ = pos.z;
                
                broadcastExcept(joined, playerID);
                
                // Send world snapshot to new player
                if (serverWorld != null) {
                    WorldSnapshot snapshot = serverWorld.createWorldSnapshot(serverTick, connections);
                    conn.sendTCP(snapshot);
                }
                
                log("Player connected: " + connectRequest.playerName + " (ID: " + playerID + ")");
                
                for (ServerListener listener : listeners) {
                    listener.onPlayerConnected(conn);
                }
            }
        } catch (Exception e) {
            log("Connection error: " + e.getMessage());
            try { socket.close(); } catch (Exception ignored) {}
        }
    }
    
    /**
     * Remove a player from the server
     */
    public void removePlayer(PlayerConnection conn, String reason) {
        synchronized (connectionsLock) {
            connections.remove(conn.getPlayerID());
        }
        
        conn.disconnect(reason);
        
        // Broadcast player left
        PlayerLeft left = new PlayerLeft(conn.getPlayerID(), reason);
        broadcast(left);
        
        log("Player disconnected: " + conn.getPlayerName() + " - " + reason);
        
        for (ServerListener listener : listeners) {
            listener.onPlayerDisconnected(conn, reason);
        }
    }
    
    /**
     * Broadcast a message to all connected clients
     */
    public void broadcast(NetworkMessage message) {
        synchronized (connectionsLock) {
            for (PlayerConnection conn : connections.values()) {
                conn.send(message);
            }
        }
    }
    
    /**
     * Broadcast a message to all clients except one
     */
    public void broadcastExcept(NetworkMessage message, int exceptPlayerID) {
        synchronized (connectionsLock) {
            for (PlayerConnection conn : connections.values()) {
                if (conn.getPlayerID() != exceptPlayerID) {
                    conn.send(message);
                }
            }
        }
    }
    
    /**
     * Handle incoming message from a client
     */
    public void handleMessage(PlayerConnection conn, NetworkMessage message) {
        if (message instanceof PlayerInputState) {
            conn.queueInput((PlayerInputState) message);
        } else if (message instanceof KeepAlive) {
            KeepAlive ka = (KeepAlive) message;
            conn.send(ka.respond(System.currentTimeMillis()));
            conn.updateLastActivity();
        } else if (message instanceof ChatMessage) {
            // Broadcast chat
            ChatMessage chat = (ChatMessage) message;
            chat.senderID = conn.getPlayerID();
            chat.senderName = conn.getPlayerName();
            broadcast(chat);
        } else if (message instanceof ClientDisconnect) {
            removePlayer(conn, "Client disconnected");
        }
    }
    
    // Network loops
    private void acceptLoop() {
        while (running) {
            try {
                Socket socket = tcpServer.accept();
                socket.setTcpNoDelay(true);
                socket.setSoTimeout(config.connectionTimeout);
                executor.submit(() -> handleConnect(socket));
            } catch (SocketTimeoutException ignored) {
            } catch (Exception e) {
                if (running) {
                    log("Accept error: " + e.getMessage());
                }
            }
        }
    }
    
    private void udpReceiveLoop() {
        byte[] buffer = new byte[config.maxPacketSize];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        
        while (running) {
            try {
                udpSocket.receive(packet);
                ByteArrayInputStream bais = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
                ObjectInputStream ois = new ObjectInputStream(bais);
                Object obj = ois.readObject();
                
                if (obj instanceof NetworkMessage) {
                    NetworkMessage msg = (NetworkMessage) obj;
                    // Route to appropriate connection based on message content
                    if (msg instanceof PlayerInputState) {
                        PlayerInputState input = (PlayerInputState) msg;
                        synchronized (connectionsLock) {
                            PlayerConnection conn = connections.get(input.playerID);
                            if (conn != null) {
                                handleMessage(conn, msg);
                            }
                        }
                    }
                }
            } catch (SocketTimeoutException ignored) {
            } catch (Exception e) {
                if (running) {
                    // Ignore UDP errors
                }
            }
        }
    }
    
    private void queryLoop() {
        try {
            DatagramSocket querySocket = new DatagramSocket(config.queryPort);
            querySocket.setSoTimeout(100);
            byte[] buffer = new byte[256];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            
            while (running) {
                try {
                    querySocket.receive(packet);
                    
                    // Create query response
                    ServerQueryResponse response = new ServerQueryResponse();
                    response.serverName = serverName;
                    response.currentPlayers = connections.size;
                    response.maxPlayers = config.maxPlayers;
                    response.currentLevel = currentLevel;
                    response.gameMode = gameMode != null ? gameMode.getModeName() : "Cooperative";
                    response.version = NetworkConfig.PROTOCOL_VERSION;
                    response.passwordProtected = config.serverPassword != null;
                    
                    // Send response
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ObjectOutputStream oos = new ObjectOutputStream(baos);
                    oos.writeObject(response);
                    oos.flush();
                    
                    byte[] data = baos.toByteArray();
                    DatagramPacket responsePacket = new DatagramPacket(
                        data, data.length, packet.getAddress(), packet.getPort()
                    );
                    querySocket.send(responsePacket);
                } catch (SocketTimeoutException ignored) {
                } catch (Exception e) {
                    if (running) {
                        // Ignore query errors
                    }
                }
            }
            
            querySocket.close();
        } catch (Exception e) {
            log("Query socket error: " + e.getMessage());
        }
    }
    
    private void log(String message) {
        System.out.println("[Server] " + message);
        if (Gdx.app != null) {
            Gdx.app.log("Server", message);
        }
    }
    
    // Getters and setters
    public int getServerTick() { return serverTick; }
    public int getPlayerCount() { return connections.size; }
    public NetworkConfig getConfig() { return config; }
    public boolean isRunning() { return running; }
    public String getServerName() { return serverName; }
    public void setServerName(String name) { this.serverName = name; }
    public ServerWorld getServerWorld() { return serverWorld; }
    public void setServerWorld(ServerWorld world) { this.serverWorld = world; }
    public GameMode getGameMode() { return gameMode; }
    public void setGameMode(GameMode mode) { this.gameMode = mode; }
    public int getCurrentLevel() { return currentLevel; }
    public void setCurrentLevel(int level) { this.currentLevel = level; }
    public float getEasiness() { return easiness; }
    public void setEasiness(float easiness) { this.easiness = easiness; }
    public PlayerConnection getConnection(int playerID) { return connections.get(playerID); }
    public Iterable<PlayerConnection> getConnections() { return connections.values(); }
    
    public void addListener(ServerListener listener) { listeners.add(listener); }
    public void removeListener(ServerListener listener) { listeners.removeValue(listener, true); }
    
    @Override
    public void dispose() {
        stop();
    }
    
    /**
     * Server event listener interface
     */
    public interface ServerListener {
        void onServerStarted();
        void onServerStopped();
        void onPlayerConnected(PlayerConnection connection);
        void onPlayerDisconnected(PlayerConnection connection, String reason);
    }
}
