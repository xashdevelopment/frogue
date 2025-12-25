package io.github.necrashter.natural_revenge.network.client;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Queue;

import io.github.necrashter.natural_revenge.Main;
import io.github.necrashter.natural_revenge.network.messages.PlayerMessages.PlayerPositionUpdate;
import io.github.necrashter.natural_revenge.world.GameWorld;
import io.github.necrashter.natural_revenge.world.entities.GameEntity;

/**
 * Represents a remote player in the game world.
 * Handles interpolation, rendering, and state management for other players.
 */
public class RemotePlayer extends GameEntity {
    /** Network player ID */
    public int playerID;
    
    /** Player display name */
    public String playerName;
    
    /** Team ID (0 = no team) */
    public int team;
    
    /** Current ping to this player (via server) */
    public long ping;
    
    /** Kill/death statistics */
    public int kills;
    public int deaths;
    public int score;
    
    /** Weapon state */
    public int activeWeaponIndex;
    public int weaponState; // 0=idle, 1=firing, 2=reloading
    
    /** Player model instance */
    private ModelInstance modelInstance;
    private AnimationController animController;
    
    /** Interpolation buffer */
    private static final int BUFFER_SIZE = 20;
    private final Queue<PositionSnapshot> positionBuffer = new Queue<>(BUFFER_SIZE);
    private float interpolationTime = 0;
    
    /** Interpolation settings */
    private static final float INTERPOLATION_DELAY = 0.1f; // 100ms delay
    
    /** Target position for smooth movement */
    private final Vector3 targetPosition = new Vector3();
    private final Vector3 targetForward = new Vector3();
    private float targetPitch;
    
    /** Interpolation state */
    private final Vector3 renderPosition = new Vector3();
    private final Vector3 renderForward = new Vector3(1, 0, 0);
    private float renderPitch;
    
    /** Last update time */
    private long lastUpdateTime;
    private int lastServerTick;
    
    public RemotePlayer(int playerID, String playerName) {
        this(null, playerID, playerName);
    }
    
    public RemotePlayer(GameWorld world, int playerID, String playerName) {
        super(world, 1.5f, 0.375f);
        this.playerID = playerID;
        this.playerName = playerName;
        this.maxHealth = 100f;
        this.health = 100f;
        
        // Create model instance (using NPC model as placeholder)
        try {
            // Use zombie model as remote player representation
            modelInstance = new ModelInstance(Main.assets.npcModel);
            animController = new AnimationController(modelInstance);
        } catch (Exception e) {
            modelInstance = null;
        }
        
        lastUpdateTime = System.currentTimeMillis();
    }
    
    /**
     * Receive a position update from the server
     */
    public void receivePositionUpdate(PlayerPositionUpdate update) {
        PositionSnapshot snapshot = new PositionSnapshot();
        snapshot.serverTick = update.serverTick;
        snapshot.timestamp = System.currentTimeMillis();
        update.getPosition(snapshot.position);
        update.getVelocity(snapshot.velocity);
        snapshot.forward.set(update.forwardX, 0, update.forwardZ).nor();
        snapshot.pitch = update.pitch;
        snapshot.onGround = update.onGround;
        
        // Add to buffer
        if (positionBuffer.size >= BUFFER_SIZE) {
            positionBuffer.removeFirst();
        }
        positionBuffer.addLast(snapshot);
        
        lastServerTick = update.serverTick;
        lastUpdateTime = System.currentTimeMillis();
    }
    
    @Override
    public void update(float delta) {
        interpolationTime += delta;
        
        // Interpolate between buffered positions
        if (positionBuffer.size >= 2) {
            float renderTime = interpolationTime - INTERPOLATION_DELAY;
            
            PositionSnapshot from = null;
            PositionSnapshot to = null;
            
            // Find the two snapshots to interpolate between
            for (int i = 0; i < positionBuffer.size - 1; i++) {
                PositionSnapshot a = positionBuffer.get(i);
                PositionSnapshot b = positionBuffer.get(i + 1);
                
                float aTime = (a.timestamp - positionBuffer.first().timestamp) / 1000f;
                float bTime = (b.timestamp - positionBuffer.first().timestamp) / 1000f;
                
                if (renderTime >= aTime && renderTime < bTime) {
                    from = a;
                    to = b;
                    
                    float t = (renderTime - aTime) / (bTime - aTime);
                    t = MathUtils.clamp(t, 0, 1);
                    
                    // Interpolate position
                    renderPosition.set(from.position).lerp(to.position, t);
                    
                    // Interpolate forward direction
                    renderForward.set(from.forward).lerp(to.forward, t).nor();
                    
                    // Interpolate pitch
                    renderPitch = MathUtils.lerp(from.pitch, to.pitch, t);
                    
                    break;
                }
            }
            
            // If no valid interpolation pair, extrapolate from latest
            if (from == null && positionBuffer.size > 0) {
                PositionSnapshot latest = positionBuffer.last();
                float timeSinceUpdate = (System.currentTimeMillis() - latest.timestamp) / 1000f;
                
                // Simple extrapolation based on velocity
                renderPosition.set(latest.position).mulAdd(latest.velocity, Math.min(timeSinceUpdate, 0.2f));
                renderForward.set(latest.forward);
                renderPitch = latest.pitch;
            }
        } else if (positionBuffer.size == 1) {
            PositionSnapshot snapshot = positionBuffer.first();
            renderPosition.set(snapshot.position);
            renderForward.set(snapshot.forward);
            renderPitch = snapshot.pitch;
        }
        
        // Update hitbox position for collision
        hitBox.position.set(renderPosition);
        forward.set(renderForward);
        
        // Update model transform
        if (modelInstance != null) {
            modelInstance.transform.setToTranslation(renderPosition);
            float angle = MathUtils.atan2(renderForward.z, renderForward.x) * MathUtils.radiansToDegrees;
            modelInstance.transform.rotate(Vector3.Y, -angle + 90);
        }
        
        // Update animation
        if (animController != null) {
            animController.update(delta);
        }
    }
    
    @Override
    public void render(GameWorld world) {
        if (modelInstance != null && isVisible(world.cam) && isInViewDistance(world.cam, world.viewDistance)) {
            world.modelBatch.render(modelInstance, world.environment);
        }
    }
    
    /**
     * Render player name above head
     */
    public void renderNameTag(GameWorld world) {
        // Name tag rendering would be done through UI overlay
    }
    
    @Override
    public void die() {
        super.die();
        deaths++;
    }
    
    /**
     * Respawn the player at a position
     */
    public void respawn(Vector3 position) {
        dead = false;
        health = maxHealth;
        hitBox.position.set(position);
        renderPosition.set(position);
        hitBox.velocity.setZero();
        positionBuffer.clear();
        interpolationTime = 0;
    }
    
    /**
     * Check if this player is timed out (no updates for too long)
     */
    public boolean isTimedOut(long timeoutMs) {
        return System.currentTimeMillis() - lastUpdateTime > timeoutMs;
    }
    
    public Vector3 getRenderPosition() {
        return renderPosition;
    }
    
    public Vector3 getRenderForward() {
        return renderForward;
    }
    
    public float getRenderPitch() {
        return renderPitch;
    }
    
    /**
     * Set the player's position directly
     */
    public void setPosition(float x, float y, float z) {
        hitBox.position.set(x, y, z);
        renderPosition.set(x, y, z);
        positionBuffer.clear();
    }
    
    /**
     * Set dead state
     */
    public void setDead(boolean dead) {
        this.dead = dead;
        if (dead) {
            health = 0;
        }
    }
    
    /**
     * Dispose resources
     */
    public void dispose() {
        // Clean up any resources
        positionBuffer.clear();
        modelInstance = null;
        animController = null;
    }
    
    /**
     * Position snapshot for interpolation buffer
     */
    private static class PositionSnapshot {
        int serverTick;
        long timestamp;
        final Vector3 position = new Vector3();
        final Vector3 velocity = new Vector3();
        final Vector3 forward = new Vector3();
        float pitch;
        boolean onGround;
    }
}
