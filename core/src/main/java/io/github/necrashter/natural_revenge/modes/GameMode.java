package io.github.necrashter.natural_revenge.modes;

import com.badlogic.gdx.utils.Array;
import io.github.necrashter.natural_revenge.network.server.ServerPlayer;
import io.github.necrashter.natural_revenge.network.server.ServerWorld;
import io.github.necrashter.natural_revenge.network.server.ServerEntity;

import java.util.Map;

/**
 * Interface for game modes in multiplayer.
 * Defines the rules and behavior for different multiplayer modes.
 */
public interface GameMode {
    
    /**
     * Initialize the game mode with world and settings
     */
    void initialize(ServerWorld world, Map<String, Object> settings);
    
    /**
     * Update game mode logic
     */
    void update(float delta);
    
    /**
     * Called when a player joins the game
     */
    void onPlayerJoin(ServerPlayer player);
    
    /**
     * Called when a player leaves the game
     */
    void onPlayerLeave(ServerPlayer player);
    
    /**
     * Called when an entity is killed
     */
    void onEntityKilled(ServerEntity victim, ServerPlayer attacker);
    
    /**
     * Called when a player is killed
     */
    void onPlayerKilled(ServerPlayer victim, ServerPlayer killer);
    
    /**
     * Called when an objective is completed
     */
    void onObjectiveComplete(ServerPlayer player, int objectiveID);
    
    /**
     * End the current game
     */
    void endGame();
    
    /**
     * Check if the game has ended
     */
    boolean isGameOver();
    
    /**
     * Get the current scoreboard
     */
    ScoreBoard getScoreBoard();
    
    /**
     * Get the mode name
     */
    String getModeName();
    
    /**
     * Get respawn delay in seconds
     */
    float getRespawnDelay();
    
    /**
     * Check if friendly fire is enabled
     */
    boolean isFriendlyFireEnabled();
    
    /**
     * Get the team for a new player (-1 for auto-assign)
     */
    int getTeamForPlayer(ServerPlayer player);
    
    /**
     * Scoreboard data class
     */
    class ScoreBoard {
        public Array<ScoreEntry> entries = new Array<>();
        public int[] teamScores;
        public float gameTime;
        public float timeLimit;
        public int scoreLimit;
        
        public void addEntry(ServerPlayer player) {
            ScoreEntry entry = new ScoreEntry();
            entry.playerID = player.getPlayerID();
            entry.playerName = player.getPlayerName();
            entry.kills = player.getKills();
            entry.deaths = player.getDeaths();
            entry.score = player.getScore();
            entry.team = player.getTeam();
            entries.add(entry);
        }
        
        public void sortByScore() {
            entries.sort((a, b) -> b.score - a.score);
        }
        
        public void sortByKills() {
            entries.sort((a, b) -> b.kills - a.kills);
        }
    }
    
    /**
     * Individual score entry
     */
    class ScoreEntry {
        public int playerID;
        public String playerName;
        public int kills;
        public int deaths;
        public int score;
        public int team;
        public int ping;
    }
}
