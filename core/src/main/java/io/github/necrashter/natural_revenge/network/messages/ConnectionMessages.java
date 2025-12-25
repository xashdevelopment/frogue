package io.github.necrashter.natural_revenge.network.messages;

import io.github.necrashter.natural_revenge.network.NetworkConfig;

/**
 * Connection-related network messages.
 */
public class ConnectionMessages {
    
    /**
     * Request to connect to server
     */
    public static class ClientConnectRequest extends NetworkMessage {
        public String playerName;
        public String protocolVersion;
        public String password;
        
        public ClientConnectRequest() {
            priority = MessagePriority.CRITICAL;
        }
        
        public ClientConnectRequest(String playerName, String password) {
            this();
            this.playerName = playerName;
            this.protocolVersion = NetworkConfig.PROTOCOL_VERSION;
            this.password = password;
        }
    }
    
    /**
     * Server response to connection request
     */
    public static class ClientConnectResponse extends NetworkMessage {
        public boolean success;
        public int assignedPlayerID;
        public String serverName;
        public int tickRate;
        public int maxPlayers;
        public int currentPlayers;
        public String rejectReason;
        public int currentLevel;
        public float easiness;
        
        public ClientConnectResponse() {
            priority = MessagePriority.CRITICAL;
        }
        
        public static ClientConnectResponse success(int playerID, String serverName, int tickRate, 
                                                     int maxPlayers, int currentPlayers, int level, float easiness) {
            ClientConnectResponse response = new ClientConnectResponse();
            response.success = true;
            response.assignedPlayerID = playerID;
            response.serverName = serverName;
            response.tickRate = tickRate;
            response.maxPlayers = maxPlayers;
            response.currentPlayers = currentPlayers;
            response.currentLevel = level;
            response.easiness = easiness;
            return response;
        }
        
        public static ClientConnectResponse failure(String reason) {
            ClientConnectResponse response = new ClientConnectResponse();
            response.success = false;
            response.rejectReason = reason;
            return response;
        }
    }
    
    /**
     * Client disconnect notification
     */
    public static class ClientDisconnect extends NetworkMessage {
        public int playerID;
        public String reason;
        
        public ClientDisconnect() {
            priority = MessagePriority.CRITICAL;
        }
        
        public ClientDisconnect(int playerID, String reason) {
            this();
            this.playerID = playerID;
            this.reason = reason;
        }
    }
    
    /**
     * Keep-alive ping message
     */
    public static class KeepAlive extends NetworkMessage {
        public long sendTime;
        public long serverTime;
        public boolean isResponse;
        
        public KeepAlive() {
            priority = MessagePriority.HIGH;
        }
        
        public KeepAlive(long sendTime) {
            this();
            this.sendTime = sendTime;
            this.isResponse = false;
        }
        
        public KeepAlive respond(long serverTime) {
            KeepAlive response = new KeepAlive();
            response.sendTime = this.sendTime;
            response.serverTime = serverTime;
            response.isResponse = true;
            return response;
        }
    }
    
    /**
     * Server query request (for server browser)
     */
    public static class ServerQueryRequest extends NetworkMessage {
        public ServerQueryRequest() {
            priority = MessagePriority.LOW;
        }
    }
    
    /**
     * Server query response
     */
    public static class ServerQueryResponse extends NetworkMessage implements java.io.Serializable {
        private static final long serialVersionUID = 1L;
        public String serverName;
        public int currentPlayers;
        public int maxPlayers;
        public int currentLevel;
        public String gameMode;
        public int ping;
        public String version;
        public boolean passwordProtected;
        public long timestamp;
        
        public ServerQueryResponse() {
            priority = MessagePriority.LOW;
            timestamp = System.currentTimeMillis();
        }
    }
}
