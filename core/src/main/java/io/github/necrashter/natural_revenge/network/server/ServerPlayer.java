package io.github.necrashter.natural_revenge.network.server;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;

import io.github.necrashter.natural_revenge.network.messages.PlayerMessages.PlayerInputState;

/**
 * Server-side representation of a player.
 * Contains authoritative state for physics simulation and validation.
 */
public class ServerPlayer {
    private final int playerID;
    private final String playerName;
    
    // Physics state
    private final Vector3 position = new Vector3();
    private final Vector3 velocity = new Vector3();
    private final Vector3 forward = new Vector3(1, 0, 0);
    private float pitch = 0;
    private boolean onGround = true;
    
    // Player stats
    private float health = 100f;
    private float maxHealth = 100f;
    private float armor = 0f;
    private boolean dead = false;
    
    // Weapon state
    private int activeWeaponIndex = 0;
    private int ammoInClip = 30;
    private int totalAmmo = 90;
    private int weaponState = 0; // 0=idle, 1=firing, 2=reloading
    
    // Team
    private int team = 0;
    
    // Statistics
    private int kills = 0;
    private int deaths = 0;
    private int score = 0;
    
    // Movement settings
    private static final float MOVEMENT_SPEED = 4f;
    private static final float JUMP_VELOCITY = 6f;
    private static final float GRAVITY = -20f;
    private static final float PLAYER_RADIUS = 0.375f;
    
    // Anti-cheat
    private final Vector3 lastValidPosition = new Vector3();
    private float speedViolationCounter = 0f;
    
    // Modifiers (mod menu)
    private final PlayerModifiers modifiers = new PlayerModifiers();
    
    public ServerPlayer(int playerID, String playerName) {
        this.playerID = playerID;
        this.playerName = playerName;
    }
    
    /**
     * Apply input from client
     */
    public void applyInput(PlayerInputState input) {
        if (dead) return;
        
        float delta = MathUtils.clamp(input.deltaTime, 0.001f, 0.1f);
        
        // Update forward direction from input
        float yaw = input.yaw;
        forward.set((float)Math.cos(yaw), 0, (float)Math.sin(yaw)).nor();
        pitch = MathUtils.clamp(input.pitch, -90f, 90f);
        
        // Calculate movement
        Vector3 right = new Vector3(forward).crs(Vector3.Y);
        Vector3 movement = new Vector3();
        
        movement.x = input.movementX * forward.x + input.movementY * right.x;
        movement.z = input.movementX * forward.z + input.movementY * right.z;
        
        if (movement.len2() > 1) movement.nor();
        movement.scl(MOVEMENT_SPEED);
        
        // Validate speed (anti-cheat)
        float maxSpeed = MOVEMENT_SPEED * 1.5f; // Allow some tolerance
        if (movement.len() > maxSpeed) {
            movement.nor().scl(maxSpeed);
            speedViolationCounter += delta;
        } else {
            speedViolationCounter = Math.max(0, speedViolationCounter - delta);
        }
        
        // Apply movement
        position.mulAdd(movement, delta);
        
        // Apply gravity
        if (!onGround) {
            velocity.y += GRAVITY * delta;
        }
        position.mulAdd(velocity, delta);
        
        // Handle jumping
        if (input.jumping && (onGround || modifiers.infJump)) {
            velocity.y = JUMP_VELOCITY;
            onGround = false;
        }
        
        // Handle weapon switching
        if (input.switchingWeapon) {
            activeWeaponIndex = input.selectedWeapon;
        }
        
        // Handle firing
        if (input.firing1 && weaponState == 0 && (ammoInClip > 0 || modifiers.infAmmo)) {
            weaponState = 1;
            if (!modifiers.infAmmo) ammoInClip--;
        }
        
        // Handle reloading
        if (input.reloading && weaponState == 0) {
            weaponState = 2;
        }
        
        // Store last valid position for anti-cheat
        lastValidPosition.set(position);
    }
    
    /**
     * Update physics (called by server world)
     */
    public void update(float delta, ServerWorld world) {
        if (dead) return;
        
        // Ground check
        if (world != null) {
            float terrainHeight = world.getTerrainHeight(position.x, position.z);
            float groundY = terrainHeight + PLAYER_RADIUS;
            
            if (position.y <= groundY) {
                position.y = groundY;
                velocity.y = 0;
                onGround = true;
            } else {
                onGround = false;
            }
            
            // Boundary clamping
            position.x = world.clampX(position.x, PLAYER_RADIUS);
            position.z = world.clampZ(position.z, PLAYER_RADIUS);
        }
    }
    
    /**
     * Apply damage to this player
     */
    public boolean takeDamage(float damage, int attackerID) {
        if (dead) return false;
        if (modifiers.godMode) return false;
        
        // Apply armor reduction
        if (armor > 0) {
            float armorDamage = Math.min(armor, damage * 0.5f);
            armor -= armorDamage;
            damage -= armorDamage;
        }
        
        health -= damage;
        
        if (health <= 0) {
            health = 0;
            die();
            return true;
        }
        return false;
    }
    
    /**
     * Kill this player
     */
    public void die() {
        dead = true;
        deaths++;
        velocity.setZero();
    }
    
    /**
     * Respawn at a position
     */
    public void respawn(Vector3 spawnPosition) {
        position.set(spawnPosition);
        velocity.setZero();
        health = modifiers.customHealth > 0 ? modifiers.customHealth : maxHealth;
        armor = 0;
        dead = false;
        onGround = true;
        weaponState = 0;
        ammoInClip = 30;
    }
    
    /**
     * Heal this player
     */
    public void heal(float amount) {
        health = Math.min(maxHealth, health + amount);
    }
    
    /**
     * Add armor
     */
    public void addArmor(float amount) {
        armor = Math.min(100f, armor + amount);
    }
    
    /**
     * Record a kill
     */
    public void addKill() {
        kills++;
        score += 100;
    }
    
    // Getters
    public int getPlayerID() { return playerID; }
    public String getPlayerName() { return playerName; }
    public Vector3 getPosition() { return position; }
    public Vector3 getVelocity() { return velocity; }
    public Vector3 getForward() { return forward; }
    public float getPitch() { return pitch; }
    public boolean isOnGround() { return onGround; }
    public float getHealth() { return health; }
    public float getMaxHealth() { return maxHealth; }
    public float getArmor() { return armor; }
    public boolean isDead() { return dead; }
    public int getTeam() { return team; }
    public int getKills() { return kills; }
    public int getDeaths() { return deaths; }
    public int getScore() { return score; }
    public int getActiveWeaponIndex() { return activeWeaponIndex; }
    public int getAmmoInClip() { return ammoInClip; }
    public int getWeaponState() { return weaponState; }
    
    // Setters
    public void setPosition(float x, float y, float z) { position.set(x, y, z); }
    public void setTeam(int team) { this.team = team; }
    public void setHealth(float health) { this.health = health; }
    public void setMaxHealth(float maxHealth) { this.maxHealth = maxHealth; }
    
    /**
     * Check if player has too many speed violations (potential cheater)
     */
    public boolean hasSpeedViolations() {
        return speedViolationCounter > 2f;
    }
    
    public PlayerModifiers getModifiers() { return modifiers; }
    
    public void setModifiers(PlayerModifiers mods) {
        this.modifiers.godMode = mods.godMode;
        this.modifiers.infAmmo = mods.infAmmo;
        this.modifiers.infJump = mods.infJump;
        this.modifiers.noRecoil = mods.noRecoil;
        this.modifiers.noSpread = mods.noSpread;
        this.modifiers.customHealth = mods.customHealth;
        this.modifiers.espPlayers = mods.espPlayers;
        this.modifiers.espMonsters = mods.espMonsters;
        this.modifiers.espEntities = mods.espEntities;
        if (mods.customHealth > 0) {
            this.health = mods.customHealth;
        }
    }
}
