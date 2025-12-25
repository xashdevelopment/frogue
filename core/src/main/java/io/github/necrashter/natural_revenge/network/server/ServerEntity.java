package io.github.necrashter.natural_revenge.network.server;

import com.badlogic.gdx.math.Vector3;

/**
 * Server-side entity representation.
 * Base class for all networked entities on the server.
 */
public class ServerEntity {
    private final int entityID;
    private final int entityType;
    
    // Physics state
    private final Vector3 position = new Vector3();
    private final Vector3 velocity = new Vector3();
    private final Vector3 forward = new Vector3(1, 0, 0);
    
    // Health
    private float health = 100f;
    private float maxHealth = 100f;
    private boolean dead = false;
    
    // Animation
    private int animationState = 0;
    private float animationTime = 0;
    
    // Ownership
    private int ownerPlayerID = -1;
    private int team = 0;
    
    // Removal flag
    private boolean shouldRemove = false;
    
    // AI state (for NPCs/enemies)
    private int aiState = 0;
    private int targetPlayerID = -1;
    
    public ServerEntity(int entityID, int entityType) {
        this.entityID = entityID;
        this.entityType = entityType;
    }
    
    /**
     * Update entity state
     */
    public void update(float delta, ServerWorld world) {
        if (dead) {
            shouldRemove = true;
            return;
        }
        
        // Apply velocity
        position.mulAdd(velocity, delta);
        
        // Apply gravity for ground entities
        velocity.y -= 20f * delta;
        
        // Ground collision
        if (world != null) {
            float terrainHeight = world.getTerrainHeight(position.x, position.z);
            if (position.y < terrainHeight + 0.5f) {
                position.y = terrainHeight + 0.5f;
                velocity.y = 0;
            }
            
            // Boundary clamping
            position.x = world.clampX(position.x, 1f);
            position.z = world.clampZ(position.z, 1f);
        }
        
        // Update animation time
        animationTime += delta;
    }
    
    /**
     * Apply damage to entity
     */
    public boolean takeDamage(float damage, int attackerID) {
        if (dead) return false;
        
        health -= damage;
        if (health <= 0) {
            health = 0;
            die();
            return true;
        }
        return false;
    }
    
    /**
     * Kill entity
     */
    public void die() {
        dead = true;
    }
    
    /**
     * Set position
     */
    public void setPosition(float x, float y, float z) {
        position.set(x, y, z);
    }
    
    /**
     * Set velocity
     */
    public void setVelocity(float x, float y, float z) {
        velocity.set(x, y, z);
    }
    
    /**
     * Move towards a target position
     */
    public void moveTowards(Vector3 target, float speed, float delta) {
        Vector3 direction = new Vector3(target).sub(position);
        direction.y = 0;
        float distance = direction.len();
        
        if (distance > 0.1f) {
            direction.nor();
            forward.set(direction);
            
            float moveDistance = Math.min(speed * delta, distance);
            position.mulAdd(direction, moveDistance);
        }
    }
    
    // Getters
    public int getEntityID() { return entityID; }
    public int getEntityType() { return entityType; }
    public Vector3 getPosition() { return position; }
    public Vector3 getVelocity() { return velocity; }
    public Vector3 getForward() { return forward; }
    public float getHealth() { return health; }
    public float getMaxHealth() { return maxHealth; }
    public boolean isDead() { return dead; }
    public boolean shouldRemove() { return shouldRemove; }
    public int getAnimationState() { return animationState; }
    public float getAnimationTime() { return animationTime; }
    public int getOwnerPlayerID() { return ownerPlayerID; }
    public int getTeam() { return team; }
    public int getAIState() { return aiState; }
    public int getTargetPlayerID() { return targetPlayerID; }
    
    // Setters
    public void setHealth(float health) { this.health = health; }
    public void setMaxHealth(float maxHealth) { this.maxHealth = maxHealth; }
    public void setAnimationState(int state) { this.animationState = state; }
    public void setOwnerPlayerID(int id) { this.ownerPlayerID = id; }
    public void setTeam(int team) { this.team = team; }
    public void setAIState(int state) { this.aiState = state; }
    public void setTargetPlayerID(int id) { this.targetPlayerID = id; }
    public void markForRemoval() { this.shouldRemove = true; }
}
