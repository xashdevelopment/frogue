package io.github.necrashter.natural_revenge.network.client;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

import io.github.necrashter.natural_revenge.network.InputSnapshot;
import io.github.necrashter.natural_revenge.network.messages.PlayerMessages.PlayerPositionUpdate;
import io.github.necrashter.natural_revenge.world.GameWorld;
import io.github.necrashter.natural_revenge.world.player.Player;

/**
 * Manages client-side prediction for the local player.
 * Handles input buffering, prediction, and server reconciliation.
 */
public class PredictionManager {
    /** Maximum number of inputs to buffer */
    private static final int MAX_INPUT_BUFFER = 128;
    
    /** Position error threshold for correction */
    private static final float CORRECTION_THRESHOLD = 0.5f;
    
    /** Smooth correction interpolation speed */
    private static final float CORRECTION_SPEED = 10f;
    
    private final Player player;
    private final GameWorld world;
    
    /** Input history for reconciliation */
    private final Array<InputSnapshot> inputHistory = new Array<>(MAX_INPUT_BUFFER);
    
    /** Last acknowledged input sequence from server */
    private int lastAcknowledgedSequence = 0;
    
    /** Server-authoritative position */
    private final Vector3 serverPosition = new Vector3();
    private final Vector3 serverVelocity = new Vector3();
    
    /** Current correction offset */
    private final Vector3 correctionOffset = new Vector3();
    
    /** Whether we're currently applying a correction */
    private boolean isCorrectingPosition = false;
    
    /** Prediction enabled flag */
    private boolean enabled = true;
    
    public PredictionManager(Player player, GameWorld world) {
        this.player = player;
        this.world = world;
    }
    
    /**
     * Record an input snapshot for prediction and send to server
     */
    public InputSnapshot recordInput(int sequence) {
        InputSnapshot snapshot = new InputSnapshot(sequence);
        
        // Capture current input state
        snapshot.movementInput.set(player.movementInput);
        snapshot.pitch = player.pitch;
        snapshot.forward.set(player.forward);
        snapshot.yaw = (float) Math.atan2(player.forward.z, player.forward.x);
        snapshot.firing1 = player.firing1;
        snapshot.firing2 = player.firing2;
        snapshot.jumping = false; // Will be set if jump pressed
        snapshot.reloading = player.shouldReload;
        snapshot.selectedWeapon = player.activeWeaponIndex;
        snapshot.timestamp = System.currentTimeMillis();
        
        // Store predicted position after applying input
        snapshot.predictedPosition.set(player.hitBox.position);
        snapshot.predictedVelocity.set(player.hitBox.velocity);
        
        // Add to history
        if (inputHistory.size >= MAX_INPUT_BUFFER) {
            inputHistory.removeIndex(0);
        }
        inputHistory.add(snapshot);
        
        return snapshot;
    }
    
    /**
     * Process a server position update and reconcile if needed (convenience overload)
     */
    public void receiveServerUpdate(int serverTick, int lastProcessedInput, 
                                    float x, float y, float z,
                                    float velX, float velY, float velZ) {
        lastAcknowledgedSequence = lastProcessedInput;
        serverPosition.set(x, y, z);
        serverVelocity.set(velX, velY, velZ);
        
        reconcile();
    }
    
    /**
     * Process a server position update and reconcile if needed
     */
    public void receiveServerUpdate(PlayerPositionUpdate update) {
        lastAcknowledgedSequence = update.lastProcessedInput;
        update.getPosition(serverPosition);
        update.getVelocity(serverVelocity);
        
        reconcile();
    }
    
    private void reconcile() {
        // Remove acknowledged inputs from history
        while (inputHistory.size > 0 && inputHistory.first().sequenceNumber <= lastAcknowledgedSequence) {
            inputHistory.removeIndex(0);
        }
        
        if (!enabled) {
            // No prediction - just set position directly
            player.hitBox.position.set(serverPosition);
            player.hitBox.velocity.set(serverVelocity);
            return;
        }
        
        // Find the predicted position for the acknowledged input
        Vector3 predictedServerPos = new Vector3(serverPosition);
        
        // Re-apply unacknowledged inputs to get predicted current position
        for (InputSnapshot input : inputHistory) {
            applyInputToPosition(predictedServerPos, input);
        }
        
        // Calculate position error
        Vector3 currentPos = player.hitBox.position;
        float error = currentPos.dst(predictedServerPos);
        
        if (error > CORRECTION_THRESHOLD) {
            // Significant desync - apply correction
            isCorrectingPosition = true;
            correctionOffset.set(predictedServerPos).sub(currentPos);
            
            // If error is very large, snap immediately
            if (error > 3.0f) {
                player.hitBox.position.set(predictedServerPos);
                correctionOffset.setZero();
                isCorrectingPosition = false;
            }
        }
    }
    
    /**
     * Update correction smoothing
     */
    public void update(float delta) {
        if (isCorrectingPosition && correctionOffset.len2() > 0.0001f) {
            // Smoothly apply correction
            float correction = Math.min(1f, CORRECTION_SPEED * delta);
            Vector3 step = new Vector3(correctionOffset).scl(correction);
            player.hitBox.position.add(step);
            correctionOffset.sub(step);
            
            if (correctionOffset.len2() < 0.0001f) {
                correctionOffset.setZero();
                isCorrectingPosition = false;
            }
        }
    }
    
    /**
     * Apply an input snapshot to a position (simplified physics simulation)
     */
    private void applyInputToPosition(Vector3 position, InputSnapshot input) {
        // Simplified movement calculation
        float speed = player.movementSpeed * input.deltaTime;
        
        // Calculate movement direction
        Vector3 movement = new Vector3();
        Vector3 right = new Vector3(input.forward).crs(Vector3.Y);
        
        movement.x = input.movementInput.x * input.forward.x + input.movementInput.y * right.x;
        movement.z = input.movementInput.x * input.forward.z + input.movementInput.y * right.z;
        
        if (movement.len2() > 1) movement.nor();
        movement.scl(speed);
        
        position.add(movement);
        
        // Clamp to terrain bounds
        position.x = world.terrain.clampX(position.x, player.hitBox.radius);
        position.z = world.terrain.clampZ(position.z, player.hitBox.radius);
        
        // Snap to terrain height
        float terrainHeight = world.terrain.getHeight(position.x, position.z);
        if (position.y < terrainHeight + player.hitBox.radius) {
            position.y = terrainHeight + player.hitBox.radius;
        }
    }
    
    /**
     * Get the smoothed render position accounting for correction
     */
    public Vector3 getRenderPosition(Vector3 out) {
        return out.set(player.hitBox.position);
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public int getLastAcknowledgedSequence() {
        return lastAcknowledgedSequence;
    }
    
    public int getPendingInputCount() {
        return inputHistory.size;
    }
    
    /**
     * Clear all buffered inputs (e.g., on respawn)
     */
    public void clear() {
        inputHistory.clear();
        correctionOffset.setZero();
        isCorrectingPosition = false;
        lastAcknowledgedSequence = 0;
    }
}
