package io.github.necrashter.natural_revenge.modes;

import com.badlogic.gdx.utils.Array;
import io.github.necrashter.natural_revenge.network.server.ServerPlayer;
import io.github.necrashter.natural_revenge.network.server.ServerWorld;
import io.github.necrashter.natural_revenge.network.server.ServerEntity;

import java.util.Map;

/**
 * Team-based deathmatch game mode (Red vs Blue).
 */
public class TeamDeathmatchMode implements GameMode {
    public static final int TEAM_RED = 1;
    public static final int TEAM_BLUE = 2;
    
    private ServerWorld world;
    private final Array<ServerPlayer> players = new Array<>();
    private final ScoreBoard scoreBoard = new ScoreBoard();
    
    // Team scores
    private int redScore = 0;
    private int blueScore = 0;
    
    // Settings
    private boolean friendlyFire = false;
    private float respawnDelay = 3f;
    private int scoreLimit = 50;
    private float timeLimit = 900f; // 15 minutes
    private boolean autoBalance = true;
    
    // State
    private float gameTime = 0;
    private boolean gameOver = false;
    private int winningTeam = 0;
    
    @Override
    public void initialize(ServerWorld world, Map<String, Object> settings) {
        this.world = world;
        
        if (settings != null) {
            if (settings.containsKey("friendlyFire")) {
                friendlyFire = (Boolean) settings.get("friendlyFire");
            }
            if (settings.containsKey("scoreLimit")) {
                scoreLimit = ((Number) settings.get("scoreLimit")).intValue();
            }
            if (settings.containsKey("timeLimit")) {
                timeLimit = ((Number) settings.get("timeLimit")).floatValue();
            }
            if (settings.containsKey("respawnDelay")) {
                respawnDelay = ((Number) settings.get("respawnDelay")).floatValue();
            }
            if (settings.containsKey("autoBalance")) {
                autoBalance = (Boolean) settings.get("autoBalance");
            }
        }
        
        scoreBoard.teamScores = new int[3]; // Index 0 unused, 1=red, 2=blue
    }
    
    @Override
    public void update(float delta) {
        if (gameOver) return;
        
        gameTime += delta;
        
        // Update team scores in scoreboard
        if (scoreBoard.teamScores != null) {
            scoreBoard.teamScores[TEAM_RED] = redScore;
            scoreBoard.teamScores[TEAM_BLUE] = blueScore;
        }
        
        // Check time limit
        if (timeLimit > 0 && gameTime >= timeLimit) {
            if (redScore > blueScore) {
                winningTeam = TEAM_RED;
            } else if (blueScore > redScore) {
                winningTeam = TEAM_BLUE;
            } else {
                winningTeam = 0; // Draw
            }
            gameOver = true;
        }
    }
    
    @Override
    public void onPlayerJoin(ServerPlayer player) {
        players.add(player);
        
        // Auto-assign to team with fewer players
        int team = getTeamForPlayer(player);
        player.setTeam(team);
    }
    
    @Override
    public void onPlayerLeave(ServerPlayer player) {
        players.removeValue(player, true);
    }
    
    @Override
    public void onEntityKilled(ServerEntity victim, ServerPlayer attacker) {
        // Not relevant for team deathmatch
    }
    
    @Override
    public void onPlayerKilled(ServerPlayer victim, ServerPlayer killer) {
        if (killer != null && killer != victim) {
            // Check for team kill
            if (killer.getTeam() == victim.getTeam()) {
                if (friendlyFire) {
                    // Penalty for team kill
                    if (killer.getTeam() == TEAM_RED) {
                        redScore = Math.max(0, redScore - 1);
                    } else {
                        blueScore = Math.max(0, blueScore - 1);
                    }
                }
            } else {
                // Enemy kill - add score
                killer.addKill();
                if (killer.getTeam() == TEAM_RED) {
                    redScore++;
                    if (redScore >= scoreLimit) {
                        winningTeam = TEAM_RED;
                        gameOver = true;
                    }
                } else if (killer.getTeam() == TEAM_BLUE) {
                    blueScore++;
                    if (blueScore >= scoreLimit) {
                        winningTeam = TEAM_BLUE;
                        gameOver = true;
                    }
                }
            }
        }
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
        
        for (ServerPlayer player : players) {
            scoreBoard.addEntry(player);
        }
        
        // Sort by team then kills
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
            return TEAM_RED; // Default to red
        }
        
        // Count players on each team
        int redCount = 0;
        int blueCount = 0;
        for (ServerPlayer p : players) {
            if (p.getTeam() == TEAM_RED) redCount++;
            else if (p.getTeam() == TEAM_BLUE) blueCount++;
        }
        
        // Assign to smaller team
        return redCount <= blueCount ? TEAM_RED : TEAM_BLUE;
    }
    
    /**
     * Request team change for a player
     */
    public boolean requestTeamChange(ServerPlayer player, int requestedTeam) {
        if (requestedTeam != TEAM_RED && requestedTeam != TEAM_BLUE) {
            return false;
        }
        
        if (autoBalance) {
            // Check balance
            int redCount = 0;
            int blueCount = 0;
            for (ServerPlayer p : players) {
                if (p == player) continue;
                if (p.getTeam() == TEAM_RED) redCount++;
                else if (p.getTeam() == TEAM_BLUE) blueCount++;
            }
            
            // Allow if it doesn't unbalance teams too much
            if (requestedTeam == TEAM_RED && redCount > blueCount + 1) {
                return false;
            }
            if (requestedTeam == TEAM_BLUE && blueCount > redCount + 1) {
                return false;
            }
        }
        
        player.setTeam(requestedTeam);
        return true;
    }
    
    // Getters
    public int getRedScore() { return redScore; }
    public int getBlueScore() { return blueScore; }
    public int getWinningTeam() { return winningTeam; }
    public int getScoreLimit() { return scoreLimit; }
    public float getTimeLimit() { return timeLimit; }
    
    public static String getTeamName(int team) {
        switch (team) {
            case TEAM_RED: return "Red";
            case TEAM_BLUE: return "Blue";
            default: return "None";
        }
    }
}
