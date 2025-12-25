package io.github.necrashter.natural_revenge.network.messages;

/**
 * Game event and state network messages.
 */
public class GameMessages {
    
    /** Game mode identifiers */
    public static final int MODE_COOPERATIVE = 0;
    public static final int MODE_DEATHMATCH = 1;
    public static final int MODE_TEAM_DEATHMATCH = 2;
    
    /**
     * Level/game start notification
     */
    public static class LevelStart extends NetworkMessage {
        public int levelID;
        public float easiness;
        public long seed;
        public int gameMode;
        public int timeLimit; // seconds, 0 for no limit
        public int scoreLimit;
        
        public LevelStart() {
            priority = MessagePriority.CRITICAL;
        }
        
        public LevelStart(int levelID, float easiness, long seed, int gameMode) {
            this();
            this.levelID = levelID;
            this.easiness = easiness;
            this.seed = seed;
            this.gameMode = gameMode;
        }
    }
    
    /**
     * Game over notification
     */
    public static class GameOver extends NetworkMessage {
        public int winnerID; // Player ID or team ID
        public int winnerType; // 0=player, 1=team
        public String reason;
        public int[] scores;
        public int[] playerIDs;
        
        public GameOver() {
            priority = MessagePriority.CRITICAL;
        }
    }
    
    /**
     * Score update
     */
    public static class ScoreUpdate extends NetworkMessage {
        public int playerID;
        public int kills;
        public int deaths;
        public int assists;
        public int score;
        
        public ScoreUpdate() {
            priority = MessagePriority.MEDIUM;
        }
    }
    
    /**
     * Full scoreboard state
     */
    public static class ScoreboardUpdate extends NetworkMessage {
        public int[] playerIDs;
        public String[] playerNames;
        public int[] kills;
        public int[] deaths;
        public int[] scores;
        public int[] teams;
        public int[] pings;
        
        public ScoreboardUpdate() {
            priority = MessagePriority.LOW;
        }
    }
    
    /**
     * Chat message
     */
    public static class ChatMessage extends NetworkMessage {
        public int senderID;
        public String senderName;
        public String message;
        public boolean teamOnly;
        public int messageType; // 0=chat, 1=system, 2=kill
        
        public ChatMessage() {
            priority = MessagePriority.LOW;
        }
        
        public ChatMessage(int senderID, String senderName, String message) {
            this();
            this.senderID = senderID;
            this.senderName = senderName;
            this.message = message;
            this.teamOnly = false;
            this.messageType = 0;
        }
        
        public static ChatMessage system(String message) {
            ChatMessage msg = new ChatMessage();
            msg.senderID = -1;
            msg.senderName = "Server";
            msg.message = message;
            msg.messageType = 1;
            return msg;
        }
    }
    
    /**
     * World state snapshot for late joiners
     */
    public static class WorldSnapshot extends NetworkMessage {
        public int serverTick;
        public int levelID;
        public float easiness;
        public int gameMode;
        public float gameTime;
        
        // Players in game
        public int[] playerIDs;
        public String[] playerNames;
        public float[] playerPositionsX;
        public float[] playerPositionsY;
        public float[] playerPositionsZ;
        public float[] playerHealth;
        public int[] playerTeams;
        
        // Active entities
        public int[] entityIDs;
        public int[] entityTypes;
        public float[] entityPositionsX;
        public float[] entityPositionsY;
        public float[] entityPositionsZ;
        public float[] entityHealth;
        
        public WorldSnapshot() {
            priority = MessagePriority.CRITICAL;
        }
    }
    
    /**
     * Objective update
     */
    public static class ObjectiveUpdate extends NetworkMessage {
        public int objectiveID;
        public int objectiveType;
        public float progress;
        public float maxProgress;
        public String description;
        public boolean completed;
        
        public ObjectiveUpdate() {
            priority = MessagePriority.MEDIUM;
        }
    }
    
    /**
     * Request to change team
     */
    public static class TeamChangeRequest extends NetworkMessage {
        public int playerID;
        public int requestedTeam;
        
        public TeamChangeRequest() {
            priority = MessagePriority.MEDIUM;
        }
    }
    
    /**
     * Game setting change
     */
    public static class GameSettingChange extends NetworkMessage {
        public String settingName;
        public String settingValue;
        
        public GameSettingChange() {
            priority = MessagePriority.CRITICAL;
        }
    }
}
