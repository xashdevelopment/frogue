package io.github.necrashter.natural_revenge.network.server;

/**
 * Player modifier flags for testing/mod menu.
 * These are server-side cheats that can be enabled by the host.
 */
public class PlayerModifiers {
    public boolean godMode = false;
    public boolean infAmmo = false;
    public boolean infJump = false;
    public boolean noRecoil = false;
    public boolean noSpread = false;
    public float customHealth = 0;
    
    // ESP options
    public boolean espPlayers = false;
    public boolean espMonsters = false;
    public boolean espEntities = false;
    
    public PlayerModifiers() {
    }
    
    public void reset() {
        godMode = false;
        infAmmo = false;
        infJump = false;
        noRecoil = false;
        noSpread = false;
        customHealth = 0;
        espPlayers = false;
        espMonsters = false;
        espEntities = false;
    }
    
    public boolean hasAnyMod() {
        return godMode || infAmmo || infJump || noRecoil || noSpread || customHealth > 0 
            || espPlayers || espMonsters || espEntities;
    }
}
