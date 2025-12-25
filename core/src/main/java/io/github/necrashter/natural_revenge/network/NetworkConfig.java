package io.github.necrashter.natural_revenge.network;

/**
 * Network configuration for multiplayer games.
 * Contains all configurable network parameters for both client and server.
 */
public class NetworkConfig {
    /** Server host address */
    public String serverHost = "localhost";
    
    /** Server port for game traffic */
    public int serverPort = 7777;
    
    /** Server query port for server browser */
    public int queryPort = 7778;
    
    /** Client port (0 for auto-assign) */
    public int clientPort = 0;
    
    /** Connection timeout in milliseconds */
    public int connectionTimeout = 5000;
    
    /** Keep-alive interval in milliseconds */
    public int keepAliveInterval = 1000;
    
    /** Maximum packet size in bytes */
    public int maxPacketSize = 1400;
    
    /** Compression threshold (compress packets larger than this) */
    public int compressionThreshold = 512;
    
    /** Server tick rate (updates per second) */
    public int serverTickRate = 60;
    
    /** Network update rate (position broadcasts per second) */
    public int networkUpdateRate = 20;
    
    /** Maximum players allowed on server */
    public int maxPlayers = 16;
    
    /** Interpolation buffer size in milliseconds */
    public int interpolationBuffer = 100;
    
    /** Client-side prediction enabled */
    public boolean clientPrediction = true;
    
    /** Server-side lag compensation window in milliseconds */
    public int lagCompensationWindow = 200;
    
    /** Server password (null for no password) */
    public String serverPassword = null;
    
    /** Server name for browser */
    public String serverName = "Frogue Server";
    
    /** Game version for compatibility checking */
    public static final String PROTOCOL_VERSION = "1.0.0";
    
    public NetworkConfig() {}
    
    public NetworkConfig(String host, int port) {
        this.serverHost = host;
        this.serverPort = port;
    }
    
    /**
     * Calculate the delta time between network updates
     */
    public float getNetworkDelta() {
        return 1.0f / networkUpdateRate;
    }
    
    /**
     * Calculate the server tick delta time
     */
    public float getServerDelta() {
        return 1.0f / serverTickRate;
    }
}
