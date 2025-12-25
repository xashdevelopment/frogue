package io.github.necrashter.natural_revenge.network.messages;

/**
 * Base class for all network messages.
 * All messages should extend this class for proper serialization.
 */
public abstract class NetworkMessage {
    /** Timestamp when message was created (client/server time) */
    public long timestamp;
    
    /** Message priority level */
    public transient MessagePriority priority = MessagePriority.MEDIUM;
    
    public NetworkMessage() {
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * Message priority levels for network transmission
     */
    public enum MessagePriority {
        /** Critical messages that require acknowledgment (connection, game events) */
        CRITICAL,
        /** High priority messages (player position updates) */
        HIGH,
        /** Medium priority messages (entity positions, health) */
        MEDIUM,
        /** Low priority messages (chat, statistics) */
        LOW
    }
}
