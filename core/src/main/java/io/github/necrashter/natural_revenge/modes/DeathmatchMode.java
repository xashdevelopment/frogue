package io.github.necrashter.natural_revenge.modes;

import com.badlogic.gdx.utils.Array;
import io.github.necrashter.natural_revenge.network.server.ServerPlayer;
import io.github.necrashter.natural_revenge.network.server.ServerWorld;
import io.github.necrashter.natural_revenge.network.server.ServerEntity;

import java.util.Map;

/**
 * Free-for-all deathmatch game mode.
 */
public class DeathmatchMode implements GameMode {
    private ServerWorld world;
    private final Array<ServerPlayer> players = new Array<>();
    private final ScoreBoard scoreBoard = new ScoreBoard();
    
    // Settings
    private float respawnDelay = 3f;
    private int scoreLimit = 25;
    private float timeLimit = 600f; // 10 minutes
    
    // State
    private float gameTime = 0;
    private boolean gameOver = false;
    private int winnerID = -1;
    
    @Override
    public void initialize(ServerWorld world, Map<String, Object> settings) {
        this.world = world;
        
        if (settings != null) {
            if (settings.containsKey("scoreLimit")) {
                scoreLimit = ((Number) settings.get("scoreLimit")).intValue();
            }
            if (settings.containsKey("timeLimit")) {
                timeLimit = ((Number) settings.get("timeLimit")).floatValue();
            }
            if (settings.containsKey("respawnDelay")) {
                respawnDelay = ((Number) settings.get("respawnDelay")).floatValue();
            }
        }
    }
    
    @Override
    public void update(float delta) {
        if (gameOver) return;
        
        gameTime += delta;
        
        // Check time limit
        if (timeLimit > 0 && gameTime >= timeLimit) {
            // Find player with most kills
            ServerPlayer winner = null;
            int maxKills = -1;
            for (ServerPlayer player : players) {
                if (player.getKills() > maxKills) {
                    maxKills = player.getKills();
                    winner = player;
                }
            }
            if (winner != null) {
                winnerID = winner.getPlayerID();
            }
            gameOver = true;
        }
    }
    
    @Override
    public void onPlayerJoin(ServerPlayer player) {
        players.add(player);
        player.setTeam(0); // No team in FFA
    }
    
    @Override
    public void onPlayerLeave(ServerPlayer player) {
        players.removeValue(player, true);
    }
    
    @Override
    public void onEntityKilled(ServerEntity victim, ServerPlayer attacker) {
        // Not relevant for deathmatch
    }
    
    @Override
    public void onPlayerKilled(ServerPlayer victim, ServerPlayer killer) {
        if (killer != null && killer != victim) {
            killer.addKill();
            
            // Check score limit
            if (killer.getKills() >= scoreLimit) {
                winnerID = killer.getPlayerID();
                gameOver = true;
            }
        }
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
        
        for (ServerPlayer player : players) {
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
        return 0; // No teams
    }
    
    public int getWinnerID() { return winnerID; }
    public int getScoreLimit() { return scoreLimit; }
    public float getTimeLimit() { return timeLimit; }
}
