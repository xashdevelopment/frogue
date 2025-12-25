package io.github.necrashter.natural_revenge.modes;

import com.badlogic.gdx.utils.Array;
import io.github.necrashter.natural_revenge.network.server.ServerPlayer;
import io.github.necrashter.natural_revenge.network.server.ServerWorld;
import io.github.necrashter.natural_revenge.network.server.ServerEntity;

import java.util.Map;

/**
 * Cooperative game mode - players work together against AI enemies.
 */
public class CooperativeMode implements GameMode {
    private ServerWorld world;
    private final Array<ServerPlayer> players = new Array<>();
    private final ScoreBoard scoreBoard = new ScoreBoard();
    
    // Settings
    private boolean friendlyFire = false;
    private float respawnDelay = 5f;
    private int maxWaves = 10;
    private float waveDifficulty = 1f;
    
    // State
    private int currentWave = 0;
    private int enemiesRemaining = 0;
    private float waveTimer = 0;
    private float gameTime = 0;
    private boolean gameOver = false;
    private boolean victory = false;
    
    @Override
    public void initialize(ServerWorld world, Map<String, Object> settings) {
        this.world = world;
        
        if (settings != null) {
            if (settings.containsKey("friendlyFire")) {
                friendlyFire = (Boolean) settings.get("friendlyFire");
            }
            if (settings.containsKey("respawnDelay")) {
                respawnDelay = ((Number) settings.get("respawnDelay")).floatValue();
            }
            if (settings.containsKey("maxWaves")) {
                maxWaves = ((Number) settings.get("maxWaves")).intValue();
            }
        }
        
        // Start first wave after delay
        waveTimer = 10f;
    }
    
    @Override
    public void update(float delta) {
        if (gameOver) return;
        
        gameTime += delta;
        
        // Wave management
        if (enemiesRemaining <= 0) {
            waveTimer -= delta;
            if (waveTimer <= 0) {
                startNextWave();
            }
        }
        
        // Check for all players dead
        boolean allDead = true;
        for (ServerPlayer player : players) {
            if (!player.isDead()) {
                allDead = false;
                break;
            }
        }
        
        if (allDead && players.size > 0) {
            gameOver = true;
            victory = false;
        }
    }
    
    private void startNextWave() {
        currentWave++;
        
        if (currentWave > maxWaves) {
            // Victory!
            gameOver = true;
            victory = true;
            return;
        }
        
        // Calculate enemies for this wave
        int baseEnemies = 5 + currentWave * 2;
        int playerMultiplier = Math.max(1, players.size);
        enemiesRemaining = (int)(baseEnemies * playerMultiplier * waveDifficulty);
        
        // Spawn enemies
        spawnWaveEnemies(enemiesRemaining);
        
        // Reset wave timer for next wave
        waveTimer = 15f;
    }
    
    private void spawnWaveEnemies(int count) {
        // Spawn enemies around the map
        for (int i = 0; i < count; i++) {
            float x = (float)(Math.random() * (world.getWorldSizeX() - 20) + 10);
            float z = (float)(Math.random() * (world.getWorldSizeZ() - 20) + 10);
            float y = world.getTerrainHeight(x, z) + 1f;
            
            // Spawn zombie or frog based on wave
            int entityType = currentWave > 5 ? 3 : 1; // FROG2 after wave 5, FROG1 before
            world.spawnEntity(entityType, x, y, z);
        }
    }
    
    @Override
    public void onPlayerJoin(ServerPlayer player) {
        players.add(player);
        player.setTeam(1); // All players on same team
    }
    
    @Override
    public void onPlayerLeave(ServerPlayer player) {
        players.removeValue(player, true);
    }
    
    @Override
    public void onEntityKilled(ServerEntity victim, ServerPlayer attacker) {
        enemiesRemaining = Math.max(0, enemiesRemaining - 1);
        
        if (attacker != null) {
            attacker.addKill();
        }
    }
    
    @Override
    public void onPlayerKilled(ServerPlayer victim, ServerPlayer killer) {
        // In co-op, player deaths just add to death count
        // Respawn is handled separately
    }
    
    @Override
    public void onObjectiveComplete(ServerPlayer player, int objectiveID) {
        // Handle objective completion
    }
    
    @Override
    public void endGame() {
        gameOver = true;
    }
    
    @Override
    public boolean isGameOver() {
        return gameOver;
    }
    
    @Override
    public ScoreBoard getScoreBoard() {
        scoreBoard.entries.clear();
        scoreBoard.gameTime = gameTime;
        
        for (ServerPlayer player : players) {
            scoreBoard.addEntry(player);
        }
        scoreBoard.sortByKills();
        
        return scoreBoard;
    }
    
    @Override
    public String getModeName() {
        return "Cooperative";
    }
    
    @Override
    public float getRespawnDelay() {
        return respawnDelay;
    }
    
    @Override
    public boolean isFriendlyFireEnabled() {
        return friendlyFire;
    }
    
    @Override
    public int getTeamForPlayer(ServerPlayer player) {
        return 1; // All players on team 1
    }
    
    public int getCurrentWave() { return currentWave; }
    public int getEnemiesRemaining() { return enemiesRemaining; }
    public boolean isVictory() { return victory; }
}
