package io.github.necrashter.natural_revenge.network;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

/**
 * Snapshot of player input at a given time.
 * Used for client-side prediction and server reconciliation.
 */
public class InputSnapshot {
    /** Sequence number for ordering and reconciliation */
    public int sequenceNumber;
    
    /** Timestamp when this input was captured */
    public long timestamp;
    
    /** Movement input vector */
    public final Vector2 movementInput = new Vector2();
    
    /** Camera pitch (vertical look) */
    public float pitch;
    
    /** Camera yaw (horizontal look) derived from forward vector */
    public float yaw;
    
    /** Forward direction vector */
    public final Vector3 forward = new Vector3();
    
    /** Jump action */
    public boolean jumping;
    
    /** Primary fire button */
    public boolean firing1;
    
    /** Secondary fire (ADS) */
    public boolean firing2;
    
    /** Reload action */
    public boolean reloading;
    
    /** Weapon switch */
    public boolean switchingWeapon;
    
    /** Selected weapon index */
    public int selectedWeapon;
    
    /** Use/interact action */
    public boolean using;
    
    /** Delta time for this input frame */
    public float deltaTime;
    
    /** Predicted position after applying this input */
    public final Vector3 predictedPosition = new Vector3();
    
    /** Predicted velocity after applying this input */
    public final Vector3 predictedVelocity = new Vector3();
    
    public InputSnapshot() {}
    
    public InputSnapshot(int sequence) {
        this.sequenceNumber = sequence;
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * Copy values from another snapshot
     */
    public void set(InputSnapshot other) {
        this.sequenceNumber = other.sequenceNumber;
        this.timestamp = other.timestamp;
        this.movementInput.set(other.movementInput);
        this.pitch = other.pitch;
        this.yaw = other.yaw;
        this.forward.set(other.forward);
        this.jumping = other.jumping;
        this.firing1 = other.firing1;
        this.firing2 = other.firing2;
        this.reloading = other.reloading;
        this.switchingWeapon = other.switchingWeapon;
        this.selectedWeapon = other.selectedWeapon;
        this.using = other.using;
        this.deltaTime = other.deltaTime;
        this.predictedPosition.set(other.predictedPosition);
        this.predictedVelocity.set(other.predictedVelocity);
    }
    
    /**
     * Reset all values
     */
    public void reset() {
        sequenceNumber = 0;
        timestamp = 0;
        movementInput.setZero();
        pitch = 0;
        yaw = 0;
        forward.set(1, 0, 0);
        jumping = false;
        firing1 = false;
        firing2 = false;
        reloading = false;
        switchingWeapon = false;
        selectedWeapon = 0;
        using = false;
        deltaTime = 0;
        predictedPosition.setZero();
        predictedVelocity.setZero();
    }
}
