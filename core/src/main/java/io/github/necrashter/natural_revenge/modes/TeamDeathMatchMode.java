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
 * Team Deathmatch mode - two teams compete for kills.
 */
public class TeamDeathMatchMode implements GameMode {
    public static final int TEAM_RED = 1;
    public static final int TEAM_BLUE = 2;
    
    private ServerWorld world;
    private final IntMap<ServerPlayer> players = new IntMap<>();
    private final IntMap<Float> respawnTimers = new IntMap<>();
    private final ScoreBoard scoreBoard = new ScoreBoard();
    
    // Team data
    private int redTeamCount = 0;
    private int blueTeamCount = 0;
    private int redTeamScore = 0;
    private int blueTeamScore = 0;
    
    // Settings
    private float respawnDelay = 3f;
    private int scoreLimit = 50;
    private float timeLimit = 600f;
    private boolean friendlyFire = false;
    private boolean autoBalance = true;
    
    // State
    private float gameTime = 0;
    private boolean gameOver = false;
    private int winningTeam = 0;
    
    @Override
    public void initialize(ServerWorld world, Map<String, Object> settings) {
        this.world = world;
        scoreBoard.teamScores = new int[3]; // 0=none, 1=red, 2=blue
        
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
            if (settings.containsKey("friendlyFire")) {
                friendlyFire = (Boolean) settings.get("friendlyFire");
            }
            if (settings.containsKey("autoBalance")) {
                autoBalance = (Boolean) settings.get("autoBalance");
            }
        }
    }
    
    @Override
    public void update(float delta) {
        if (gameOver) return;
        
        gameTime += delta;
        
        // Process respawns
        Array<Integer> toRemove = new Array<>();
        for (IntMap.Entry<Float> entry : respawnTimers.entries()) {
            float timer = entry.value - delta;
            if (timer <= 0) {
                ServerPlayer player = players.get(entry.key);
                if (player != null && player.isDead()) {
                    respawnPlayer(player);
                }
                toRemove.add(entry.key);
            } else {
                respawnTimers.put(entry.key, timer);
            }
        }
        for (int key : toRemove) {
            respawnTimers.remove(key);
        }
        
        // Check time limit
        if (timeLimit > 0 && gameTime >= timeLimit) {
            determineWinner();
            endGame();
        }
    }
    
    private void respawnPlayer(ServerPlayer player) {
        Vector2 spawn = world.getRandomSpawnPoint();
        float height = world.getTerrainHeight(spawn.x, spawn.y);
        player.respawn(new Vector3(spawn.x, height + 1f, spawn.y));
    }
    
    private void determineWinner() {
        if (redTeamScore > blueTeamScore) {
            winningTeam = TEAM_RED;
        } else if (blueTeamScore > redTeamScore) {
            winningTeam = TEAM_BLUE;
        } else {
            winningTeam = 0; // Tie
        }
    }
    
    @Override
    public void onPlayerJoin(ServerPlayer player) {
        players.put(player.getPlayerID(), player);
        
        // Auto-assign to team with fewer players
        int team = getTeamForPlayer(player);
        player.setTeam(team);
        
        if (team == TEAM_RED) redTeamCount++;
        else blueTeamCount++;
    }
    
    @Override
    public void onPlayerLeave(ServerPlayer player) {
        if (player.getTeam() == TEAM_RED) redTeamCount--;
        else if (player.getTeam() == TEAM_BLUE) blueTeamCount--;
        
        players.remove(player.getPlayerID());
        respawnTimers.remove(player.getPlayerID());
    }
    
    @Override
    public void onEntityKilled(ServerEntity victim, ServerPlayer attacker) {
        // Entities don't count in TDM
    }
    
    @Override
    public void onPlayerKilled(ServerPlayer victim, ServerPlayer killer) {
        if (killer != null && killer != victim) {
            // Check if it's a team kill
            if (killer.getTeam() == victim.getTeam()) {
                if (!friendlyFire) return; // Shouldn't happen, but safety check
                // Team kill - could penalize killer
            } else {
                // Enemy kill
                killer.addKill();
                
                // Add to team score
                if (killer.getTeam() == TEAM_RED) {
                    redTeamScore++;
                    if (redTeamScore >= scoreLimit) {
                        winningTeam = TEAM_RED;
                        endGame();
                    }
                } else if (killer.getTeam() == TEAM_BLUE) {
                    blueTeamScore++;
                    if (blueTeamScore >= scoreLimit) {
                        winningTeam = TEAM_BLUE;
                        endGame();
                    }
                }
            }
        }
        
        // Queue respawn
        respawnTimers.put(victim.getPlayerID(), respawnDelay);
    }
    
    @Override
    public void onObjectiveComplete(ServerPlayer player, int objectiveID) {
        // No objectives in TDM
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
        scoreBoard.teamScores[TEAM_RED] = redTeamScore;
        scoreBoard.teamScores[TEAM_BLUE] = blueTeamScore;
        
        for (ServerPlayer player : players.values()) {
            scoreBoard.addEntry(player);
        }
        
        // Sort by team then by kills
        scoreBoard.entries.sort((a, b) -> {
            if (a.team != b.team) return a.team - b.team;
            return b.kills - a.kills;
        });
        
        return scoreBoard;
    }
    
    @Override
    public String getModeName() {
        return "Team Deathmatch";
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
        if (!autoBalance) {
            return TEAM_RED; // Default to red if auto-balance disabled
        }
        
        // Assign to team with fewer players
        return redTeamCount <= blueTeamCount ? TEAM_RED : TEAM_BLUE;
    }
    
    public int getRedTeamScore() { return redTeamScore; }
    public int getBlueTeamScore() { return blueTeamScore; }
    public int getWinningTeam() { return winningTeam; }
}
