package io.github.necrashter.natural_revenge.modes;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntMap;

import io.github.necrashter.natural_revenge.network.server.ServerPlayer;
import io.github.necrashter.natural_revenge.network.server.ServerWorld;
import io.github.necrashter.natural_revenge.network.server.ServerEntity;

import java.util.Map;

/**
 * Free-for-all deathmatch mode - every player vs everyone else.
 */
public class DeathMatchMode implements GameMode {
    private ServerWorld world;
    private final IntMap<ServerPlayer> players = new IntMap<>();
    private final IntMap<Float> respawnTimers = new IntMap<>();
    private final ScoreBoard scoreBoard = new ScoreBoard();
    
    // Settings
    private float respawnDelay = 3f;
    private int scoreLimit = 20;
    private float timeLimit = 600f; // 10 minutes
    
    // State
    private float gameTime = 0;
    private boolean gameOver = false;
    private int winnerID = -1;
    
    @Override
    public void initialize(ServerWorld world, Map<String, Object> settings) {
        this.world = world;
        
        if (settings != null) {
            if (settings.containsKey("respawnDelay")) {
                respawnDelay = ((Number) settings.get("respawnDelay")).floatValue();
            }
            if (settings.containsKey("scoreLimit")) {
                scoreLimit = ((Number) settings.get("scoreLimit")).intValue();
            }
            if (settings.containsKey("timeLimit")) {
                timeLimit = ((Number) settings.get("timeLimit")).floatValue();
            }
        }
    }
    
    @Override
    public void update(float delta) {
        if (gameOver) return;
        
        gameTime += delta;
        
        // Process respawns
        for (IntMap.Entry<Float> entry : respawnTimers.entries()) {
            float timer = entry.value - delta;
            if (timer <= 0) {
                ServerPlayer player = players.get(entry.key);
                if (player != null && player.isDead()) {
                    respawnPlayer(player);
                }
                respawnTimers.remove(entry.key);
            } else {
                respawnTimers.put(entry.key, timer);
            }
        }
        
        // Check time limit
        if (timeLimit > 0 && gameTime >= timeLimit) {
            findWinner();
            endGame();
        }
    }
    
    private void respawnPlayer(ServerPlayer player) {
        Vector2 spawn = world.getRandomSpawnPoint();
        float height = world.getTerrainHeight(spawn.x, spawn.y);
        player.respawn(new Vector3(spawn.x, height + 1f, spawn.y));
    }
    
    private void findWinner() {
        int highestScore = -1;
        for (ServerPlayer player : players.values()) {
            if (player.getScore() > highestScore) {
                highestScore = player.getScore();
                winnerID = player.getPlayerID();
            }
        }
    }
    
    @Override
    public void onPlayerJoin(ServerPlayer player) {
        players.put(player.getPlayerID(), player);
        player.setTeam(player.getPlayerID()); // Each player is their own team
    }
    
    @Override
    public void onPlayerLeave(ServerPlayer player) {
        players.remove(player.getPlayerID());
        respawnTimers.remove(player.getPlayerID());
    }
    
    @Override
    public void onEntityKilled(ServerEntity victim, ServerPlayer attacker) {
        // Entities don't count in deathmatch
    }
    
    @Override
    public void onPlayerKilled(ServerPlayer victim, ServerPlayer killer) {
        if (killer != null && killer != victim) {
            killer.addKill();
            
            // Check for score limit
            if (killer.getKills() >= scoreLimit) {
                winnerID = killer.getPlayerID();
                endGame();
            }
        }
        
        // Queue respawn
        respawnTimers.put(victim.getPlayerID(), respawnDelay);
    }
    
    @Override
    public void onObjectiveComplete(ServerPlayer player, int objectiveID) {
        // No objectives in deathmatch
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
        scoreBoard.timeLimit = timeLimit;
        scoreBoard.scoreLimit = scoreLimit;
        
        for (ServerPlayer player : players.values()) {
            scoreBoard.addEntry(player);
        }
        scoreBoard.sortByKills();
        
        return scoreBoard;
    }
    
    @Override
    public String getModeName() {
        return "Deathmatch";
    }
    
    @Override
    public float getRespawnDelay() {
        return respawnDelay;
    }
    
    @Override
    public boolean isFriendlyFireEnabled() {
        return true; // Everyone is an enemy
    }
    
    @Override
    public int getTeamForPlayer(ServerPlayer player) {
        return player.getPlayerID(); // Each player on their own team
    }
    
    public int getWinnerID() { return winnerID; }
    public int getScoreLimit() { return scoreLimit; }
    public float getTimeLimit() { return timeLimit; }
}
