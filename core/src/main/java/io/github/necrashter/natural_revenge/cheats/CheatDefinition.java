package io.github.necrashter.natural_revenge.cheats;

/**
 * Cheat metadata structure containing all cheat information.
 * Categorized for organized UI display.
 */
public class CheatDefinition {
    public final String fieldName;
    public final String displayName;
    public final String description;
    public final Category category;
    public final boolean isToggle;
    public final boolean isDangerous;
    
    /**
     * Categories for organizing cheats in the menu.
     */
    public enum Category {
        COMBAT("Combat"),
        MOVEMENT("Movement"),
        VISUAL("Visual"),
        FUN("Fun");
        
        public final String displayName;
        
        Category(String displayName) {
            this.displayName = displayName;
        }
    }
    
    /**
     * Create a toggle cheat definition.
     */
    public CheatDefinition(String fieldName, String displayName, String description, 
                          Category category, boolean isDangerous) {
        this.fieldName = fieldName;
        this.displayName = displayName;
        this.description = description;
        this.category = category;
        this.isToggle = true;
        this.isDangerous = isDangerous;
    }
    
    /**
     * Create a button/action cheat definition.
     */
    public static CheatDefinition createAction(String fieldName, String displayName, 
                                               String description, Category category) {
        return new CheatDefinition(fieldName, displayName, description, category, false);
    }
    
    /**
     * Create all standard cheat definitions.
     */
    public static CheatDefinition[] createAllCheats() {
        return new CheatDefinition[] {
            // COMBAT Category
            new CheatDefinition("godmode", "God Mode", "Take no damage", Category.COMBAT, false),
            new CheatDefinition("instantAimAndShoot", "Instant Aim & Shoot", 
                               "Always hit your target", Category.COMBAT, false),
            new CheatDefinition("infiniteAmmo", "Infinite Ammo", 
                               "Never run out of bullets", Category.COMBAT, false),
            new CheatDefinition("rapidfire", "Rapid Fire", 
                               "Fire at maximum speed", Category.COMBAT, false),
            new CheatDefinition("norecoil", "No Recoil", 
                               "Perfect accuracy", Category.COMBAT, false),
            new CheatDefinition("nospread", "No Spread", 
                               "All shots land exactly where aimed", Category.COMBAT, false),
            
            // MOVEMENT Category
            new CheatDefinition("bunnyhop", "Bunnyhop", 
                               "Jump while moving without speed loss", Category.MOVEMENT, false),
            new CheatDefinition("airStrafe", "Air Strafe", 
                               "Move faster in air", Category.MOVEMENT, false),
            new CheatDefinition("noclip", "No Clip", 
                               "Walk through walls", Category.MOVEMENT, true),
            new CheatDefinition("fly", "Fly Mode", 
                               "Fly around freely", Category.MOVEMENT, false),
            new CheatDefinition("speedMultiplier", "Speed Boost", 
                               "Move faster (2x speed)", Category.MOVEMENT, false),
            
            // VISUAL Category
            new CheatDefinition("enemyESP", "Enemy ESP", 
                               "See enemies through walls", Category.VISUAL, false),
            new CheatDefinition("invisibleToEnemies", "Invisible to Enemies", 
                               "Enemies can't see you", Category.VISUAL, false),
            new CheatDefinition("invisibleEnemies", "Invisible Enemies", 
                               "Enemies become invisible", Category.VISUAL, false),
            
            // FUN Category
            new CheatDefinition("lgbtWorld", "LGBT World", 
                               "Rainbow effects everywhere", Category.FUN, false),
            new CheatDefinition("rotatingWorld", "Rotating World", 
                               "World spins around you", Category.FUN, false),
            new CheatDefinition("rotatingWeapon", "Rotating Weapon", 
                               "Weapon spins constantly", Category.FUN, false),
            new CheatDefinition("peterGriffin", "Peter Griffin", 
                               "Something is missing...", Category.FUN, false),
            new CheatDefinition("earthquakeMode", "Earthquake", 
                               "Everything shakes!", Category.FUN, false),
            new CheatDefinition("discoMode", "Disco Mode", 
                               "Party time!", Category.FUN, false),
        };
    }
}
