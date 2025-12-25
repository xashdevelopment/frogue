package io.github.necrashter.natural_revenge.network.server;

import com.badlogic.gdx.utils.Queue;

import io.github.necrashter.natural_revenge.network.messages.*;
import io.github.necrashter.natural_revenge.network.messages.PlayerMessages.*;

import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;

/**
 * Represents a connected client on the server.
 * Handles communication and player state for one client.
 */
public class PlayerConnection {
    private final GameServer server;
    private final Socket socket;
    private final ObjectInputStream in;
    private final ObjectOutputStream out;
    
    private final int playerID;
    private final String playerName;
    
    private ServerPlayer serverPlayer;
    
    private volatile boolean connected = true;
    private long lastActivityTime;
    private long ping = 0;
    
    // Input buffer
    private final Queue<PlayerInputState> inputQueue = new Queue<>();
    private final Object inputLock = new Object();
    private int lastProcessedInput = 0;
    
    // UDP address for this client
    private InetAddress udpAddress;
    private int udpPort;
    
    public PlayerConnection(GameServer server, Socket socket, ObjectInputStream in, 
                           ObjectOutputStream out, int playerID, String playerName) {
        this.server = server;
        this.socket = socket;
        this.in = in;
        this.out = out;
        this.playerID = playerID;
        this.playerName = playerName;
        this.lastActivityTime = System.currentTimeMillis();
        this.udpAddress = socket.getInetAddress();
        this.udpPort = socket.getPort();
    }
    
    /**
     * Start receive thread
     */
    public void start(ExecutorService executor) {
        executor.submit(this::receiveLoop);
    }
    
    /**
     * Disconnect this client
     */
    public void disconnect(String reason) {
        if (!connected) return;
        connected = false;
        
        try {
            // Send disconnect message
            sendTCP(new ConnectionMessages.ClientDisconnect(playerID, reason));
        } catch (Exception ignored) {}
        
        try {
            socket.close();
        } catch (Exception ignored) {}
    }
    
    /**
     * Send a message (auto-selects TCP or UDP based on priority)
     */
    public void send(NetworkMessage message) {
        if (!connected) return;
        
        if (message.priority == NetworkMessage.MessagePriority.CRITICAL) {
            sendTCP(message);
        } else {
            sendUDP(message);
        }
    }
    
    /**
     * Send a message via TCP (reliable)
     */
    public void sendTCP(NetworkMessage message) {
        if (!connected) return;
        
        try {
            synchronized (out) {
                out.writeObject(message);
                out.flush();
                out.reset(); // Prevent memory leak from object caching
            }
        } catch (Exception e) {
            handleError(e);
        }
    }
    
    /**
     * Send a message via UDP (fast, unreliable)
     */
    public void sendUDP(NetworkMessage message) {
        // UDP sending is handled by the server's UDP socket
        // For simplicity, we'll use TCP for now
        // In a production implementation, this would send via UDP
        sendTCP(message);
    }
    
    /**
     * Queue an input for processing
     */
    public void queueInput(PlayerInputState input) {
        synchronized (inputLock) {
            inputQueue.addLast(input);
            // Limit queue size
            while (inputQueue.size > 32) {
                inputQueue.removeFirst();
            }
        }
        updateLastActivity();
    }
    
    /**
     * Poll next input from queue
     */
    public PlayerInputState pollInput() {
        synchronized (inputLock) {
            if (inputQueue.isEmpty()) return null;
            return inputQueue.removeFirst();
        }
    }
    
    /**
     * Receive loop for this connection
     */
    private void receiveLoop() {
        while (connected) {
            try {
                Object obj = in.readObject();
                if (obj instanceof NetworkMessage) {
                    server.handleMessage(this, (NetworkMessage) obj);
                }
            } catch (SocketTimeoutException ignored) {
            } catch (Exception e) {
                if (connected) {
                    handleError(e);
                }
                break;
            }
        }
    }
    
    private void handleError(Exception e) {
        if (connected) {
            server.removePlayer(this, "Connection error: " + e.getMessage());
        }
    }
    
    public void updateLastActivity() {
        lastActivityTime = System.currentTimeMillis();
    }
    
    public boolean isTimedOut(long timeoutMs) {
        return System.currentTimeMillis() - lastActivityTime > timeoutMs;
    }
    
    // Getters and setters
    public int getPlayerID() { return playerID; }
    public String getPlayerName() { return playerName; }
    public boolean isConnected() { return connected; }
    public long getPing() { return ping; }
    public void setPing(long ping) { this.ping = ping; }
    
    public ServerPlayer getServerPlayer() { return serverPlayer; }
    public void setServerPlayer(ServerPlayer player) { this.serverPlayer = player; }
    
    public int getLastProcessedInput() { return lastProcessedInput; }
    public void setLastProcessedInput(int seq) { this.lastProcessedInput = seq; }
    
    public InetAddress getUdpAddress() { return udpAddress; }
    public int getUdpPort() { return udpPort; }
}
