package io.github.necrashter.natural_revenge.network.messages;

import com.badlogic.gdx.math.Vector3;

/**
 * Entity-related network messages.
 */
public class EntityMessages {
    
    /** Entity type identifiers */
    public static final int ENTITY_ZOMBIE = 1;
    public static final int ENTITY_FROG1 = 2;
    public static final int ENTITY_FROG2 = 3;
    public static final int ENTITY_NPC = 4;
    public static final int ENTITY_PICKUP = 10;
    public static final int ENTITY_PROJECTILE = 20;
    
    /**
     * Entity creation message
     */
    public static class EntityCreate extends NetworkMessage {
        public int entityID;
        public int entityType;
        public int ownerPlayerID;
        public float positionX;
        public float positionY;
        public float positionZ;
        public float rotationY;
        public float health;
        public int team;
        public int subType; // For specific variants
        
        public EntityCreate() {
            priority = MessagePriority.CRITICAL;
        }
        
        public void setPosition(Vector3 pos) {
            this.positionX = pos.x;
            this.positionY = pos.y;
            this.positionZ = pos.z;
        }
        
        public Vector3 getPosition(Vector3 out) {
            return out.set(positionX, positionY, positionZ);
        }
    }
    
    /**
     * Entity position/state update (batched)
     */
    public static class EntityUpdate extends NetworkMessage {
        public int entityID;
        public int serverTick;
        public float positionX;
        public float positionY;
        public float positionZ;
        public float velocityX;
        public float velocityY;
        public float velocityZ;
        public float forwardX;
        public float forwardZ;
        public int animationState;
        public float animationTime;
        
        public EntityUpdate() {
            priority = MessagePriority.MEDIUM;
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
    }
    
    /**
     * Entity removal message
     */
    public static class EntityRemove extends NetworkMessage {
        public int entityID;
        public int removeReason; // 0=despawn, 1=killed, 2=collected
        
        public EntityRemove() {
            priority = MessagePriority.CRITICAL;
        }
        
        public EntityRemove(int entityID, int reason) {
            this();
            this.entityID = entityID;
            this.removeReason = reason;
        }
    }
    
    /**
     * Entity damage event
     */
    public static class EntityDamage extends NetworkMessage {
        public int entityID;
        public int attackerID; // Player ID or -1 for non-player
        public float damageAmount;
        public int damageType;
        public float hitX, hitY, hitZ;
        
        public EntityDamage() {
            priority = MessagePriority.HIGH;
        }
    }
    
    /**
     * Batch entity updates for efficiency
     */
    public static class EntityUpdateBatch extends NetworkMessage {
        public EntityUpdate[] updates;
        public int count;
        
        public EntityUpdateBatch() {
            priority = MessagePriority.MEDIUM;
        }
        
        public EntityUpdateBatch(int maxSize) {
            this();
            updates = new EntityUpdate[maxSize];
            count = 0;
        }
    }
    
    /**
     * Projectile spawn message
     */
    public static class ProjectileSpawn extends NetworkMessage {
        public int projectileID;
        public int ownerPlayerID;
        public float startX, startY, startZ;
        public float directionX, directionY, directionZ;
        public float speed;
        public float damage;
        public int projectileType;
        
        public ProjectileSpawn() {
            priority = MessagePriority.HIGH;
        }
    }
    
    /**
     * Projectile hit/explosion event
     */
    public static class ProjectileHit extends NetworkMessage {
        public int projectileID;
        public float hitX, hitY, hitZ;
        public int hitEntityID; // -1 if hit terrain/object
        public int hitType; // 0=terrain, 1=entity, 2=player
        
        public ProjectileHit() {
            priority = MessagePriority.HIGH;
        }
    }
}
