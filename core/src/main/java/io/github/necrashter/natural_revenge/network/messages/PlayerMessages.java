package io.github.necrashter.natural_revenge.network.messages;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

/**
 * Player-related network messages.
 */
public class PlayerMessages {
    
    /**
     * Player input state sent from client to server
     */
    public static class PlayerInputState extends NetworkMessage {
        public int playerID;
        public int sequenceNumber;
        public float movementX;
        public float movementY;
        public float pitch;
        public float yaw;
        public boolean firing1;
        public boolean firing2;
        public boolean jumping;
        public boolean reloading;
        public boolean switchingWeapon;
        public int selectedWeapon;
        public boolean using;
        public float deltaTime;
        
        public PlayerInputState() {
            priority = MessagePriority.HIGH;
        }
        
        public PlayerInputState(int playerID, int seq, Vector2 movement, float pitch, float yaw, float dt) {
            this();
            this.playerID = playerID;
            this.sequenceNumber = seq;
            this.movementX = movement.x;
            this.movementY = movement.y;
            this.pitch = pitch;
            this.yaw = yaw;
            this.deltaTime = dt;
        }
    }
    
    /**
     * Player position update from server to clients
     */
    public static class PlayerPositionUpdate extends NetworkMessage {
        public int playerID;
        public int serverTick;
        public int lastProcessedInput;
        public float positionX;
        public float positionY;
        public float positionZ;
        public float velocityX;
        public float velocityY;
        public float velocityZ;
        public float forwardX;
        public float forwardZ;
        public float pitch;
        public boolean onGround;
        
        public PlayerPositionUpdate() {
            priority = MessagePriority.HIGH;
        }
        
        public void setPosition(Vector3 pos) {
            this.positionX = pos.x;
            this.positionY = pos.y;
            this.positionZ = pos.z;
        }
        
        public void setVelocity(Vector3 vel) {
            this.velocityX = vel.x;
            this.velocityY = vel.y;
            this.velocityZ = vel.z;
        }
        
        public void setForward(Vector3 forward) {
            this.forwardX = forward.x;
            this.forwardZ = forward.z;
        }
        
        public Vector3 getPosition(Vector3 out) {
            return out.set(positionX, positionY, positionZ);
        }
        
        public Vector3 getVelocity(Vector3 out) {
            return out.set(velocityX, velocityY, velocityZ);
        }
    }
    
    /**
     * Player health and status update
     */
    public static class PlayerHealthUpdate extends NetworkMessage {
        public int playerID;
        public float health;
        public float maxHealth;
        public float armor;
        public boolean isDead;
        
        public PlayerHealthUpdate() {
            priority = MessagePriority.MEDIUM;
        }
        
        public PlayerHealthUpdate(int playerID, float health, float maxHealth) {
            this();
            this.playerID = playerID;
            this.health = health;
            this.maxHealth = maxHealth;
        }
    }
    
    /**
     * Player weapon state update
     */
    public static class PlayerWeaponUpdate extends NetworkMessage {
        public int playerID;
        public int activeWeaponIndex;
        public int ammoInClip;
        public int totalAmmo;
        public int weaponState; // 0=idle, 1=firing, 2=reloading
        
        public PlayerWeaponUpdate() {
            priority = MessagePriority.MEDIUM;
        }
    }
    
    /**
     * Player joined notification
     */
    public static class PlayerJoined extends NetworkMessage {
        public int playerID;
        public String playerName;
        public float positionX;
        public float positionY;
        public float positionZ;
        public int team;
        
        public PlayerJoined() {
            priority = MessagePriority.CRITICAL;
        }
    }
    
    /**
     * Player left notification
     */
    public static class PlayerLeft extends NetworkMessage {
        public int playerID;
        public String reason;
        
        public PlayerLeft() {
            priority = MessagePriority.CRITICAL;
        }
        
        public PlayerLeft(int playerID, String reason) {
            this();
            this.playerID = playerID;
            this.reason = reason;
        }
    }
    
    /**
     * Player respawn message
     */
    public static class PlayerRespawn extends NetworkMessage {
        public int playerID;
        public float spawnX;
        public float spawnY;
        public float spawnZ;
        
        public PlayerRespawn() {
            priority = MessagePriority.CRITICAL;
        }
        
        public PlayerRespawn(int playerID, Vector3 spawnPos) {
            this();
            this.playerID = playerID;
            this.spawnX = spawnPos.x;
            this.spawnY = spawnPos.y;
            this.spawnZ = spawnPos.z;
        }
    }
    
    /**
     * Player damage event
     */
    public static class PlayerDamage extends NetworkMessage {
        public int victimID;
        public int attackerID;
        public float damage;
        public int damageType; // 0=bullet, 1=melee, 2=explosion
        
        public PlayerDamage() {
            priority = MessagePriority.HIGH;
        }
    }
}
