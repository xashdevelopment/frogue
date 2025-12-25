package io.github.necrashter.natural_revenge.ui.multiplayer;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.*;

import io.github.necrashter.natural_revenge.Main;
import io.github.necrashter.natural_revenge.modes.GameMode.ScoreBoard;
import io.github.necrashter.natural_revenge.modes.GameMode.ScoreEntry;
import io.github.necrashter.natural_revenge.modes.TeamDeathmatchMode;
import io.github.necrashter.natural_revenge.network.NetworkManager;

/**
 * In-game scoreboard widget.
 * Shows player scores, kills, deaths, and team information.
 */
public class ScoreboardWidget extends Table {
    private Table headerTable;
    private Table playerTable;
    private Label gameInfoLabel;
    
    private int localPlayerID = -1;
    private boolean showTeams = false;
    
    public ScoreboardWidget() {
        super(Main.skin);
        
        setBackground(Main.skin.newDrawable("white", new Color(0, 0, 0, 0.8f)));
        pad(20);
        
        // Title
        Label titleLabel = new Label("SCOREBOARD", Main.skin);
        titleLabel.setFontScale(1.5f);
        add(titleLabel).center().padBottom(15).row();
        
        // Game info
        gameInfoLabel = new Label("", Main.skin);
        gameInfoLabel.setColor(Color.GRAY);
        add(gameInfoLabel).center().padBottom(10).row();
        
        // Header
        headerTable = new Table(Main.skin);
        buildHeader();
        add(headerTable).fillX().padBottom(5).row();
        
        // Player list
        playerTable = new Table(Main.skin);
        ScrollPane scrollPane = new ScrollPane(playerTable, Main.skin);
        scrollPane.setFadeScrollBars(false);
        add(scrollPane).fillX().height(300).row();
        
        localPlayerID = NetworkManager.getInstance().getLocalPlayerID();
    }
    
    private void buildHeader() {
        headerTable.clear();
        
        headerTable.add(new Label("Player", Main.skin)).width(150).left().padRight(10);
        headerTable.add(new Label("K", Main.skin)).width(40).center();
        headerTable.add(new Label("D", Main.skin)).width(40).center();
        headerTable.add(new Label("Score", Main.skin)).width(60).center();
        headerTable.add(new Label("Ping", Main.skin)).width(50).center();
        
        if (showTeams) {
            headerTable.add(new Label("Team", Main.skin)).width(60).center();
        }
    }
    
    /**
     * Update scoreboard with current data
     */
    public void update(ScoreBoard scoreBoard) {
        if (scoreBoard == null) return;
        
        // Update game info
        StringBuilder infoText = new StringBuilder();
        if (scoreBoard.timeLimit > 0) {
            float remaining = scoreBoard.timeLimit - scoreBoard.gameTime;
            int mins = (int)(remaining / 60);
            int secs = (int)(remaining % 60);
            infoText.append(String.format("Time: %d:%02d", mins, secs));
        }
        if (scoreBoard.scoreLimit > 0) {
            if (infoText.length() > 0) infoText.append("  |  ");
            infoText.append("Goal: ").append(scoreBoard.scoreLimit);
        }
        if (scoreBoard.teamScores != null && scoreBoard.teamScores.length > 2) {
            if (infoText.length() > 0) infoText.append("  |  ");
            infoText.append("Red: ").append(scoreBoard.teamScores[1]);
            infoText.append("  Blue: ").append(scoreBoard.teamScores[2]);
        }
        gameInfoLabel.setText(infoText.toString());
        
        // Check if we need to show teams
        showTeams = false;
        for (ScoreEntry entry : scoreBoard.entries) {
            if (entry.team > 0) {
                showTeams = true;
                break;
            }
        }
        buildHeader();
        
        // Update player list
        playerTable.clear();
        
        int currentTeam = -1;
        for (ScoreEntry entry : scoreBoard.entries) {
            // Team separator
            if (showTeams && entry.team != currentTeam) {
                currentTeam = entry.team;
                Table separator = new Table(Main.skin);
                separator.setBackground(Main.skin.newDrawable("white", getTeamColor(currentTeam).cpy().mul(0.3f)));
                separator.add(new Label(TeamDeathmatchMode.getTeamName(currentTeam) + " Team", Main.skin))
                    .left().padLeft(10);
                playerTable.add(separator).fillX().padTop(5).padBottom(5).row();
            }
            
            // Player row
            Table row = new Table(Main.skin);
            
            // Highlight local player
            if (entry.playerID == localPlayerID) {
                row.setBackground(Main.skin.newDrawable("white", new Color(0.3f, 0.3f, 0.5f, 0.5f)));
            } else {
                row.setBackground(Main.skin.newDrawable("white", new Color(0.2f, 0.2f, 0.2f, 0.3f)));
            }
            
            // Name with team color
            Label nameLabel = new Label(entry.playerName, Main.skin);
            if (showTeams) {
                nameLabel.setColor(getTeamColor(entry.team));
            }
            row.add(nameLabel).width(150).left().padLeft(10).padRight(10);
            
            // K/D/Score
            row.add(new Label(String.valueOf(entry.kills), Main.skin)).width(40).center();
            row.add(new Label(String.valueOf(entry.deaths), Main.skin)).width(40).center();
            row.add(new Label(String.valueOf(entry.score), Main.skin)).width(60).center();
            
            // Ping with color
            Label pingLabel = new Label(entry.ping + "ms", Main.skin);
            if (entry.ping < 50) {
                pingLabel.setColor(Color.GREEN);
            } else if (entry.ping < 150) {
                pingLabel.setColor(Color.YELLOW);
            } else {
                pingLabel.setColor(Color.RED);
            }
            row.add(pingLabel).width(50).center();
            
            // Team indicator
            if (showTeams) {
                Label teamLabel = new Label(TeamDeathmatchMode.getTeamName(entry.team), Main.skin);
                teamLabel.setColor(getTeamColor(entry.team));
                row.add(teamLabel).width(60).center();
            }
            
            playerTable.add(row).fillX().padBottom(2).row();
        }
    }
    
    private Color getTeamColor(int team) {
        switch (team) {
            case TeamDeathmatchMode.TEAM_RED: return Color.RED;
            case TeamDeathmatchMode.TEAM_BLUE: return Color.CYAN;
            default: return Color.WHITE;
        }
    }
    
    /**
     * Set the local player ID for highlighting
     */
    public void setLocalPlayerID(int id) {
        this.localPlayerID = id;
    }
}
