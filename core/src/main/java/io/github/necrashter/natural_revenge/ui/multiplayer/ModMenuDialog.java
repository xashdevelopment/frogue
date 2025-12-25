package io.github.necrashter.natural_revenge.ui.multiplayer;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;

import io.github.necrashter.natural_revenge.Main;
import io.github.necrashter.natural_revenge.network.server.GameServer;
import io.github.necrashter.natural_revenge.network.server.PlayerConnection;
import io.github.necrashter.natural_revenge.network.server.ServerPlayer;
import io.github.necrashter.natural_revenge.network.server.PlayerModifiers;

/**
 * Testing mod menu for server hosts.
 * Allows enabling cheats/mods for specific players.
 */
public class ModMenuDialog extends Dialog {
    private final GameServer server;
    private SelectBox<PlayerEntry> playerSelect;
    private CheckBox godmodeCheck;
    private CheckBox infAmmoCheck;
    private CheckBox infJumpCheck;
    private CheckBox noRecoilCheck;
    private CheckBox noSpreadCheck;
    private CheckBox espPlayersCheck;
    private CheckBox espMonstersCheck;
    private CheckBox espEntitiesCheck;
    private TextField healthField;
    private Label statusLabel;
    
    public ModMenuDialog(Stage stage, GameServer server) {
        super("Test Mod Menu", Main.skin);
        this.server = server;
        
        float dialogWidth = Main.isMobile() ? 500 : 450;
        float dialogHeight = Main.isMobile() ? 650 : 580;
        setSize(dialogWidth, dialogHeight);
        
        buildUI();
        refreshPlayerList();
    }
    
    private void buildUI() {
        Table content = getContentTable();
        content.pad(15);
        
        float fieldWidth = Main.isMobile() ? 280 : 250;
        float buttonHeight = Main.isMobile() ? 50 : 40;
        
        // Player selection
        content.add(new Label("Select Player:", Main.skin)).left().padBottom(5).row();
        playerSelect = new SelectBox<>(Main.skin);
        content.add(playerSelect).width(fieldWidth).padBottom(15).row();
        
        // Refresh button
        TextButton refreshBtn = new TextButton("Refresh Players", Main.skin);
        refreshBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                refreshPlayerList();
            }
        });
        content.add(refreshBtn).width(fieldWidth).height(buttonHeight).padBottom(15).row();
        
        // Mod toggles
        content.add(new Label("-- Mod Options --", Main.skin)).padBottom(10).row();
        
        godmodeCheck = new CheckBox(" God Mode (Invincible)", Main.skin);
        content.add(godmodeCheck).left().padBottom(5).row();
        
        infAmmoCheck = new CheckBox(" Infinite Ammo", Main.skin);
        content.add(infAmmoCheck).left().padBottom(5).row();
        
        infJumpCheck = new CheckBox(" Infinite Jump", Main.skin);
        content.add(infJumpCheck).left().padBottom(5).row();
        
        noRecoilCheck = new CheckBox(" No Recoil", Main.skin);
        content.add(noRecoilCheck).left().padBottom(5).row();
        
        noSpreadCheck = new CheckBox(" No Spread", Main.skin);
        content.add(noSpreadCheck).left().padBottom(10).row();
        
        // ESP options
        content.add(new Label("-- ESP Options --", Main.skin)).padBottom(5).row();
        
        espPlayersCheck = new CheckBox(" ESP Players", Main.skin);
        content.add(espPlayersCheck).left().padBottom(5).row();
        
        espMonstersCheck = new CheckBox(" ESP Monsters", Main.skin);
        content.add(espMonstersCheck).left().padBottom(5).row();
        
        espEntitiesCheck = new CheckBox(" ESP All Entities", Main.skin);
        content.add(espEntitiesCheck).left().padBottom(10).row();
        
        // Health setting
        Table healthRow = new Table();
        healthRow.add(new Label("Set Health: ", Main.skin));
        healthField = new TextField("100", Main.skin);
        healthField.setTextFieldFilter(new TextField.TextFieldFilter.DigitsOnlyFilter());
        healthRow.add(healthField).width(100);
        content.add(healthRow).padBottom(15).row();
        
        // Apply button
        TextButton applyBtn = new TextButton("Apply Mods", Main.skin);
        applyBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                applyMods();
            }
        });
        content.add(applyBtn).width(fieldWidth).height(buttonHeight).padBottom(10).row();
        
        // Status label
        statusLabel = new Label("", Main.skin);
        content.add(statusLabel).padBottom(10).row();
        
        // Close button
        TextButton closeBtn = new TextButton("Close", Main.skin);
        closeBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                hide();
            }
        });
        getButtonTable().add(closeBtn).width(120).height(buttonHeight);
    }
    
    private void refreshPlayerList() {
        Array<PlayerEntry> entries = new Array<>();
        entries.add(new PlayerEntry(-1, "-- Select Player --"));
        
        if (server != null && server.isRunning()) {
            for (PlayerConnection conn : server.getConnections()) {
                entries.add(new PlayerEntry(conn.getPlayerID(), conn.getPlayerName()));
            }
        }
        
        playerSelect.setItems(entries);
    }
    
    private void applyMods() {
        PlayerEntry selected = playerSelect.getSelected();
        if (selected == null || selected.playerID < 0) {
            statusLabel.setText("Please select a player!");
            return;
        }
        
        PlayerConnection conn = server.getConnection(selected.playerID);
        if (conn == null) {
            statusLabel.setText("Player not found!");
            return;
        }
        
        ServerPlayer player = conn.getServerPlayer();
        if (player == null) {
            statusLabel.setText("Player data not found!");
            return;
        }
        
        // Get or create modifiers
        PlayerModifiers mods = player.getModifiers();
        if (mods == null) {
            mods = new PlayerModifiers();
            player.setModifiers(mods);
        }
        
        // Apply mod settings
        mods.godMode = godmodeCheck.isChecked();
        mods.infAmmo = infAmmoCheck.isChecked();
        mods.infJump = infJumpCheck.isChecked();
        mods.noRecoil = noRecoilCheck.isChecked();
        mods.noSpread = noSpreadCheck.isChecked();
        mods.espPlayers = espPlayersCheck.isChecked();
        mods.espMonsters = espMonstersCheck.isChecked();
        mods.espEntities = espEntitiesCheck.isChecked();
        
        // Set health
        try {
            float health = Float.parseFloat(healthField.getText());
            if (health > 0) {
                player.setHealth(health);
                player.setMaxHealth(Math.max(health, player.getMaxHealth()));
            }
        } catch (NumberFormatException e) {
            // Ignore invalid health
        }
        
        statusLabel.setText("Mods applied to " + selected.playerName + "!");
    }
    
    /**
     * Load current mod settings when selecting a player
     */
    private void loadPlayerMods(int playerID) {
        if (playerID < 0) return;
        
        PlayerConnection conn = server.getConnection(playerID);
        if (conn == null) return;
        
        ServerPlayer player = conn.getServerPlayer();
        if (player == null) return;
        
        PlayerModifiers mods = player.getModifiers();
        if (mods != null) {
            godmodeCheck.setChecked(mods.godMode);
            infAmmoCheck.setChecked(mods.infAmmo);
            infJumpCheck.setChecked(mods.infJump);
            noRecoilCheck.setChecked(mods.noRecoil);
            noSpreadCheck.setChecked(mods.noSpread);
            espPlayersCheck.setChecked(mods.espPlayers);
            espMonstersCheck.setChecked(mods.espMonsters);
            espEntitiesCheck.setChecked(mods.espEntities);
        } else {
            godmodeCheck.setChecked(false);
            infAmmoCheck.setChecked(false);
            infJumpCheck.setChecked(false);
            noRecoilCheck.setChecked(false);
            noSpreadCheck.setChecked(false);
            espPlayersCheck.setChecked(false);
            espMonstersCheck.setChecked(false);
            espEntitiesCheck.setChecked(false);
        }
        
        healthField.setText(String.valueOf((int) player.getHealth()));
    }
    
    /**
     * Player entry for select box
     */
    private static class PlayerEntry {
        final int playerID;
        final String playerName;
        
        PlayerEntry(int id, String name) {
            this.playerID = id;
            this.playerName = name;
        }
        
        @Override
        public String toString() {
            return playerName + (playerID >= 0 ? " (ID:" + playerID + ")" : "");
        }
    }
}
