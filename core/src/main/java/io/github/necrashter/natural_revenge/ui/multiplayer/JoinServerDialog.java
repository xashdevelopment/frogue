package io.github.necrashter.natural_revenge.ui.multiplayer;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

import io.github.necrashter.natural_revenge.Main;

/**
 * Dialog for direct connection to a server.
 */
public class JoinServerDialog extends Dialog {
    private final Stage stage;
    private final String playerName;
    private final MultiplayerMenuDialog.MultiplayerMenuListener listener;
    
    private TextField hostField;
    private TextField portField;
    private TextField passwordField;
    
    public JoinServerDialog(Stage stage, String playerName, MultiplayerMenuDialog.MultiplayerMenuListener listener) {
        super("Join Server", Main.skin);
        this.stage = stage;
        this.playerName = playerName;
        this.listener = listener;
        
        buildUI();
    }
    
    private void buildUI() {
        Table content = getContentTable();
        content.pad(20);
        
        // Scale for mobile
        float fieldWidth = Main.isMobile() ? 250 : 200;
        float buttonWidth = Main.isMobile() ? 150 : 120;
        float buttonHeight = Main.isMobile() ? 60 : 40;
        
        // Host address
        content.add(new Label("Server Address:", Main.skin)).padRight(10);
        hostField = new TextField("localhost", Main.skin);
        content.add(hostField).width(fieldWidth).row();
        
        // Port
        content.add(new Label("Port:", Main.skin)).padRight(10);
        portField = new TextField("7777", Main.skin);
        portField.setTextFieldFilter(new TextField.TextFieldFilter.DigitsOnlyFilter());
        content.add(portField).width(fieldWidth).row();
        
        // Password (optional)
        content.add(new Label("Password:", Main.skin)).padRight(10);
        passwordField = new TextField("", Main.skin);
        passwordField.setMessageText("(if required)");
        passwordField.setPasswordMode(true);
        passwordField.setPasswordCharacter('*');
        content.add(passwordField).width(fieldWidth).row();
        
        content.add().height(20).row();
        
        // Buttons
        Table buttonTable = new Table();
        
        TextButton connectButton = new TextButton("Connect", Main.skin);
        connectButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                connect();
            }
        });
        buttonTable.add(connectButton).width(buttonWidth).height(buttonHeight).padRight(10);
        
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
    
    private void connect() {
        String host = hostField.getText().trim();
        if (host.isEmpty()) {
            showError("Please enter a server address");
            return;
        }
        
        try {
            int port = Integer.parseInt(portField.getText());
            String password = passwordField.getText().isEmpty() ? null : passwordField.getText();
            
            hide();
            listener.onJoinServer(playerName, host, port, password);
        } catch (NumberFormatException e) {
            showError("Invalid port number");
        }
    }
    
    private void showError(String message) {
        Dialog error = new Dialog("Error", Main.skin);
        error.text(message);
        error.button("OK");
        error.show(stage);
    }
}
