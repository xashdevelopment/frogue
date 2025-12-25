package io.github.necrashter.natural_revenge.network;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Queue;

import io.github.necrashter.natural_revenge.network.messages.*;
import io.github.necrashter.natural_revenge.network.messages.ConnectionMessages.*;
import io.github.necrashter.natural_revenge.network.messages.PlayerMessages.*;
import io.github.necrashter.natural_revenge.network.messages.EntityMessages.*;
import io.github.necrashter.natural_revenge.network.messages.GameMessages.*;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

/**
 * Singleton network manager handling all client-side network operations.
 * Manages connection to server, message sending/receiving, and reconnection logic.
 */
public class NetworkManager implements Disposable {
    private static NetworkManager instance;
    
    public enum ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        RECONNECTING,
        FAILED
    }
    
    private ConnectionState state = ConnectionState.DISCONNECTED;
    private NetworkConfig config;
    
    // Network I/O
    private Socket tcpSocket;
    private DatagramSocket udpSocket;
    private ObjectOutputStream tcpOut;
    private ObjectInputStream tcpIn;
    private InetAddress serverAddress;
    
    // Threading
    private ExecutorService executor;
    private volatile boolean running = false;
    
    // Message queues
    private final Queue<NetworkMessage> outgoingQueue = new Queue<>();
    private final Queue<NetworkMessage> incomingQueue = new Queue<>();
    private final Object incomingLock = new Object();
    private final Object outgoingLock = new Object();
    
    // Connection info
    private int localPlayerID = -1;
    private String localPlayerName = "Player";
    private long lastKeepAlive;
    private long latency = 0;
    private int reconnectAttempts = 0;
    private static final int MAX_RECONNECT_ATTEMPTS = 3;
    
    // Listeners
    private final Array<NetworkListener> listeners = new Array<>();
    
    // Sequence tracking
    private int inputSequence = 0;
    
    private NetworkManager() {
        config = new NetworkConfig();
    }
    
    public static NetworkManager getInstance() {
        if (instance == null) {
            instance = new NetworkManager();
        }
        return instance;
    }
    
    /**
     * Connect to a game server
     */
    public void connect(String host, int port, String playerName, String password) {
        if (state == ConnectionState.CONNECTED || state == ConnectionState.CONNECTING) {
            disconnect("New connection requested");
        }
        
        config.serverHost = host;
        config.serverPort = port;
        localPlayerName = playerName;
        state = ConnectionState.CONNECTING;
        reconnectAttempts = 0;
        
        executor = Executors.newFixedThreadPool(3);
        running = true;
        
        executor.submit(() -> {
            try {
                // Establish TCP connection
                serverAddress = InetAddress.getByName(host);
                tcpSocket = new Socket();
                tcpSocket.connect(new InetSocketAddress(serverAddress, port), config.connectionTimeout);
                tcpSocket.setTcpNoDelay(true);
                
                tcpOut = new ObjectOutputStream(tcpSocket.getOutputStream());
                tcpIn = new ObjectInputStream(tcpSocket.getInputStream());
                
                // Setup UDP socket
                udpSocket = new DatagramSocket(config.clientPort);
                udpSocket.setSoTimeout(100);
                
                // Send connect request
                ClientConnectRequest request = new ClientConnectRequest(playerName, password);
                sendTCP(request);
                
                // Wait for response
                Object response = tcpIn.readObject();
                if (response instanceof ClientConnectResponse) {
                    ClientConnectResponse connectResponse = (ClientConnectResponse) response;
                    if (connectResponse.success) {
                        localPlayerID = connectResponse.assignedPlayerID;
                        state = ConnectionState.CONNECTED;
                        lastKeepAlive = System.currentTimeMillis();
                        
                        // Start receive threads
                        executor.submit(this::tcpReceiveLoop);
                        executor.submit(this::udpReceiveLoop);
                        executor.submit(this::sendLoop);
                        
                        notifyConnected(connectResponse);
                    } else {
                        state = ConnectionState.FAILED;
                        notifyConnectionFailed(connectResponse.rejectReason);
                    }
                }
            } catch (Exception e) {
                Gdx.app.error("Network", "Connection failed: " + e.getMessage());
                state = ConnectionState.FAILED;
                notifyConnectionFailed(e.getMessage());
            }
        });
    }
    
    /**
     * Disconnect from server
     */
    public void disconnect(String reason) {
        if (state == ConnectionState.DISCONNECTED) return;
        
        running = false;
        
        try {
            if (tcpOut != null && state == ConnectionState.CONNECTED) {
                sendTCP(new ClientDisconnect(localPlayerID, reason));
            }
        } catch (Exception ignored) {}
        
        cleanup();
        state = ConnectionState.DISCONNECTED;
        notifyDisconnected(reason);
    }
    
    private void cleanup() {
        try {
            if (tcpSocket != null) tcpSocket.close();
            if (udpSocket != null) udpSocket.close();
            if (executor != null) executor.shutdownNow();
        } catch (Exception ignored) {}
        
        tcpSocket = null;
        udpSocket = null;
        tcpOut = null;
        tcpIn = null;
        localPlayerID = -1;
        
        synchronized (incomingLock) {
            incomingQueue.clear();
        }
        synchronized (outgoingLock) {
            outgoingQueue.clear();
        }
    }
    
    /**
     * Send a message to the server
     */
    public void send(NetworkMessage message) {
        if (state != ConnectionState.CONNECTED) return;
        
        synchronized (outgoingLock) {
            outgoingQueue.addLast(message);
        }
    }
    
    /**
     * Send input state to server
     */
    public void sendInput(InputSnapshot input) {
        if (state != ConnectionState.CONNECTED) return;
        
        PlayerInputState msg = new PlayerInputState();
        msg.playerID = localPlayerID;
        msg.sequenceNumber = input.sequenceNumber;
        msg.movementX = input.movementInput.x;
        msg.movementY = input.movementInput.y;
        msg.pitch = input.pitch;
        msg.yaw = input.yaw;
        msg.firing1 = input.firing1;
        msg.firing2 = input.firing2;
        msg.jumping = input.jumping;
        msg.reloading = input.reloading;
        msg.switchingWeapon = input.switchingWeapon;
        msg.selectedWeapon = input.selectedWeapon;
        msg.using = input.using;
        msg.deltaTime = input.deltaTime;
        
        send(msg);
    }
    
    /**
     * Process incoming messages on main thread
     */
    public void update(float delta) {
        if (state == ConnectionState.CONNECTED) {
            // Check keep-alive
            if (System.currentTimeMillis() - lastKeepAlive > config.keepAliveInterval) {
                send(new KeepAlive(System.currentTimeMillis()));
                lastKeepAlive = System.currentTimeMillis();
            }
        }
        
        // Process incoming messages
        synchronized (incomingLock) {
            while (incomingQueue.notEmpty()) {
                NetworkMessage message = incomingQueue.removeFirst();
                processMessage(message);
            }
        }
    }
    
    private void processMessage(NetworkMessage message) {
        if (message instanceof KeepAlive) {
            KeepAlive ka = (KeepAlive) message;
            if (ka.isResponse) {
                latency = System.currentTimeMillis() - ka.sendTime;
            }
        }
        
        // Notify listeners
        for (NetworkListener listener : listeners) {
            listener.onMessage(message);
        }
    }
    
    private void tcpReceiveLoop() {
        while (running) {
            try {
                Object obj = tcpIn.readObject();
                if (obj instanceof NetworkMessage) {
                    synchronized (incomingLock) {
                        incomingQueue.addLast((NetworkMessage) obj);
                    }
                }
            } catch (SocketTimeoutException ignored) {
            } catch (Exception e) {
                if (running) {
                    Gdx.app.error("Network", "TCP receive error: " + e.getMessage());
                    handleDisconnect();
                }
                break;
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
                    synchronized (incomingLock) {
                        incomingQueue.addLast((NetworkMessage) obj);
                    }
                }
            } catch (SocketTimeoutException ignored) {
            } catch (Exception e) {
                if (running) {
                    Gdx.app.error("Network", "UDP receive error: " + e.getMessage());
                }
            }
        }
    }
    
    private void sendLoop() {
        while (running) {
            try {
                NetworkMessage message = null;
                synchronized (outgoingLock) {
                    if (outgoingQueue.notEmpty()) {
                        message = outgoingQueue.removeFirst();
                    }
                }
                
                if (message != null) {
                    if (message.priority == NetworkMessage.MessagePriority.CRITICAL) {
                        sendTCP(message);
                    } else {
                        sendUDP(message);
                    }
                } else {
                    Thread.sleep(1);
                }
            } catch (Exception e) {
                if (running) {
                    Gdx.app.error("Network", "Send error: " + e.getMessage());
                }
            }
        }
    }
    
    private void sendTCP(NetworkMessage message) throws IOException {
        synchronized (tcpOut) {
            tcpOut.writeObject(message);
            tcpOut.flush();
            tcpOut.reset();
        }
    }
    
    private void sendUDP(NetworkMessage message) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(message);
        oos.flush();
        
        byte[] data = baos.toByteArray();
        DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, config.serverPort);
        udpSocket.send(packet);
    }
    
    private void handleDisconnect() {
        if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
            state = ConnectionState.RECONNECTING;
            reconnectAttempts++;
            // Attempt reconnect
            Gdx.app.postRunnable(() -> {
                connect(config.serverHost, config.serverPort, localPlayerName, config.serverPassword);
            });
        } else {
            disconnect("Connection lost");
        }
    }
    
    // Listener management
    public void addListener(NetworkListener listener) {
        listeners.add(listener);
    }
    
    public void removeListener(NetworkListener listener) {
        listeners.removeValue(listener, true);
    }
    
    private void notifyConnected(ClientConnectResponse response) {
        Gdx.app.postRunnable(() -> {
            for (NetworkListener listener : listeners) {
                listener.onConnected(response);
            }
        });
    }
    
    private void notifyDisconnected(String reason) {
        Gdx.app.postRunnable(() -> {
            for (NetworkListener listener : listeners) {
                listener.onDisconnected(reason);
            }
        });
    }
    
    private void notifyConnectionFailed(String reason) {
        Gdx.app.postRunnable(() -> {
            for (NetworkListener listener : listeners) {
                listener.onConnectionFailed(reason);
            }
        });
    }
    
    // Getters
    public ConnectionState getState() { return state; }
    public boolean isConnected() { return state == ConnectionState.CONNECTED; }
    public int getLocalPlayerID() { return localPlayerID; }
    public String getLocalPlayerName() { return localPlayerName; }
    public long getLatency() { return latency; }
    public int getNextInputSequence() { return ++inputSequence; }
    public NetworkConfig getConfig() { return config; }
    
    @Override
    public void dispose() {
        disconnect("Client shutdown");
        instance = null;
    }
    
    /**
     * Network event listener interface
     */
    public interface NetworkListener {
        void onConnected(ClientConnectResponse response);
        void onDisconnected(String reason);
        void onConnectionFailed(String reason);
        void onMessage(NetworkMessage message);
    }
    
    /**
     * Adapter for NetworkListener with empty default implementations
     */
    public static class NetworkListenerAdapter implements NetworkListener {
        @Override public void onConnected(ClientConnectResponse response) {}
        @Override public void onDisconnected(String reason) {}
        @Override public void onConnectionFailed(String reason) {}
        @Override public void onMessage(NetworkMessage message) {}
    }
}
