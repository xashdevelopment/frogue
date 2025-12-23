package io.github.necrashter.natural_revenge.cheats;

import java.util.HashMap;
import java.util.Map;

/**
 * Singleton cheat state management system.
 * Maintains all cheat states and provides centralized access.
 * 
 * Memory efficient: Uses single HashMap instance, minimal object allocation.
 */
public class CheatManager {
    private static final CheatManager INSTANCE = new CheatManager();
    
    // Core cheat states
    public boolean godMode = false;
    public boolean infiniteAmmo = false;
    public boolean noReload = false;
    public boolean rapidFire = false;
    public boolean superSpeed = false;
    public boolean noRecoil = false;
    public boolean wallHack = false;
    public boolean infiniteJump = false;
    public boolean oneHitKill = false;
    public boolean showFps = false;
    
    // Track which cheats have been modified from defaults
    private final Map<String, Boolean> cheatStates = new HashMap<>(32, 0.5f);
    
    private CheatManager() {
        // Initialize default states
        resetToDefaults();
    }
    
    public static CheatManager getInstance() {
        return INSTANCE;
    }
    
    /**
     * Reset all cheats to default (disabled) state.
     */
    public void resetToDefaults() {
        godMode = false;
        infiniteAmmo = false;
        noReload = false;
        rapidFire = false;
        superSpeed = false;
        noRecoil = false;
        wallHack = false;
        infiniteJump = false;
        oneHitKill = false;
        showFps = false;
        
        cheatStates.clear();
        cheatStates.put("godMode", false);
        cheatStates.put("infiniteAmmo", false);
        cheatStates.put("noReload", false);
        cheatStates.put("rapidFire", false);
        cheatStates.put("superSpeed", false);
        cheatStates.put("noRecoil", false);
        cheatStates.put("wallHack", false);
        cheatStates.put("infiniteJump", false);
        cheatStates.put("oneHitKill", false);
        cheatStates.put("showFps", false);
    }
    
    /**
     * Set a cheat state by name and update the corresponding field.
     * @param cheatName The name of the cheat (must match field name)
     * @param enabled The new state
     * @return true if cheat was found and updated
     */
    public boolean setCheatState(String cheatName, boolean enabled) {
        Boolean prevValue = cheatStates.get(cheatName);
        if (prevValue == null) {
            return false; // Unknown cheat
        }
        
        // Update both the HashMap and the actual field
        cheatStates.put(cheatName, enabled);
        
        switch (cheatName) {
            case "godMode": godMode = enabled; break;
            case "infiniteAmmo": infiniteAmmo = enabled; break;
            case "noReload": noReload = enabled; break;
            case "rapidFire": rapidFire = enabled; break;
            case "superSpeed": superSpeed = enabled; break;
            case "noRecoil": noRecoil = enabled; break;
            case "wallHack": wallHack = enabled; break;
            case "infiniteJump": infiniteJump = enabled; break;
            case "oneHitKill": oneHitKill = enabled; break;
            case "showFps": showFps = enabled; break;
            default: return false;
        }
        
        return true;
    }
    
    /**
     * Get a cheat state by name.
     * @param cheatName The name of the cheat
     * @return The cheat state, or null if not found
     */
    public Boolean getCheatState(String cheatName) {
        return cheatStates.get(cheatName);
    }
    
    /**
     * Toggle a cheat state by name.
     * @param cheatName The name of the cheat
     * @return The new state, or null if cheat not found
     */
    public Boolean toggleCheat(String cheatName) {
        Boolean current = cheatStates.get(cheatName);
        if (current == null) {
            return null;
        }
        boolean newValue = !current;
        setCheatState(cheatName, newValue);
        return newValue;
    }
    
    /**
     * Get all cheat definitions for UI generation.
     * @return Array of cheat names
     */
    public String[] getAllCheatNames() {
        return cheatStates.keySet().toArray(new String[0]);
    }
    
    /**
     * Get the number of active cheats.
     * @return Count of enabled cheats
     */
    public int getActiveCheatCount() {
        int count = 0;
        for (Boolean value : cheatStates.values()) {
            if (value) count++;
        }
        return count;
    }
    
    /**
     * Check if any cheats are currently active.
     * @return true if at least one cheat is enabled
     */
    public boolean hasActiveCheats() {
        for (Boolean value : cheatStates.values()) {
            if (value) return true;
        }
        return false;
    }
}
