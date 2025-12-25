package io.github.necrashter.natural_revenge.ui.multiplayer;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

import io.github.necrashter.natural_revenge.Main;
import io.github.necrashter.natural_revenge.network.messages.GameMessages;

/**
 * Dialog for hosting a new server.
 */
public class HostServerDialog extends Dialog {
    private final Stage stage;
    private final String playerName;
    private final MultiplayerMenuDialog.MultiplayerMenuListener listener;
    
    private TextField portField;
    private TextField passwordField;
    private TextField maxPlayersField;
    private SelectBox<String> gameModeSelect;
    
    public HostServerDialog(Stage stage, String playerName, MultiplayerMenuDialog.MultiplayerMenuListener listener) {
        super("Host Server", Main.skin);
        this.stage = stage;
        this.playerName = playerName;
        this.listener = listener;
        
        buildUI();
    }
    
    private void buildUI() {
        Table content = getContentTable();
        content.pad(20);
        
        // Scale for mobile
        float fieldWidth = Main.isMobile() ? 200 : 150;
        float buttonWidth = Main.isMobile() ? 180 : 150;
        float buttonHeight = Main.isMobile() ? 60 : 40;
        
        // Port
        content.add(new Label("Port:", Main.skin)).padRight(10);
        portField = new TextField("7777", Main.skin);
        portField.setTextFieldFilter(new TextField.TextFieldFilter.DigitsOnlyFilter());
        content.add(portField).width(fieldWidth).row();
        
        // Password (optional)
        content.add(new Label("Password:", Main.skin)).padRight(10);
        passwordField = new TextField("", Main.skin);
        passwordField.setMessageText("(optional)");
        content.add(passwordField).width(fieldWidth).row();
        
        // Max Players
        content.add(new Label("Max Players:", Main.skin)).padRight(10);
        maxPlayersField = new TextField("8", Main.skin);
        maxPlayersField.setTextFieldFilter(new TextField.TextFieldFilter.DigitsOnlyFilter());
        content.add(maxPlayersField).width(fieldWidth).row();
        
        // Game Mode
        content.add(new Label("Game Mode:", Main.skin)).padRight(10);
        gameModeSelect = new SelectBox<>(Main.skin);
        gameModeSelect.setItems("Cooperative", "Deathmatch", "Team Deathmatch");
        content.add(gameModeSelect).width(fieldWidth).row();
        
        content.add().height(20).row();
        
        // Buttons
        Table buttonTable = new Table();
        
        TextButton startButton = new TextButton("Start Server", Main.skin);
        startButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                startServer();
            }
        });
        buttonTable.add(startButton).width(buttonWidth).height(buttonHeight).padRight(10);
        
        TextButton cancelButton = new TextButton("Cancel", Main.skin);
        cancelButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                hide();
                new MultiplayerMenuDialog(stage, listener).show(stage);
            }
        });
        buttonTable.add(cancelButton).width(buttonWidth).height(buttonHeight);
        
        content.add(buttonTable).colspan(2).row();
    }
    
    private void startServer() {
        try {
            int port = Integer.parseInt(portField.getText());
            String password = passwordField.getText().isEmpty() ? null : passwordField.getText();
            int maxPlayers = Integer.parseInt(maxPlayersField.getText());
            maxPlayers = Math.max(2, Math.min(64, maxPlayers));
            
            int gameMode = gameModeSelect.getSelectedIndex();
            
            hide();
            listener.onHostServer(playerName, port, password, maxPlayers, gameMode);
        } catch (NumberFormatException e) {
            // Show error
            Dialog error = new Dialog("Error", Main.skin);
            error.text("Invalid port or player count");
            error.button("OK");
            error.show(stage);
        }
    }
}
