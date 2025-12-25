package io.github.necrashter.natural_revenge.ui.multiplayer;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

import io.github.necrashter.natural_revenge.Main;
import com.badlogic.gdx.Gdx;

/**
 * Main multiplayer menu dialog with options to host, join, or browse servers.
 */
public class MultiplayerMenuDialog extends Dialog {
    private final Stage stage;
    private final MultiplayerMenuListener listener;
    
    public MultiplayerMenuDialog(Stage stage, MultiplayerMenuListener listener) {
        super("Multiplayer", Main.skin);
        this.stage = stage;
        this.listener = listener;
        
        buildUI();
    }
    
    private void buildUI() {
        Table content = getContentTable();
        content.pad(20);
        
        // Scale for mobile
        float buttonWidth = Main.isMobile() ? 350 : 300;
        float buttonHeight = Main.isMobile() ? 70 : 50;
        float fieldWidth = Main.isMobile() ? 250 : 200;
        
        // Player name field
        Label nameLabel = new Label("Player Name:", Main.skin);
        TextField nameField = new TextField("Player", Main.skin);
        nameField.setMaxLength(16);
        
        content.add(nameLabel).padRight(10);
        content.add(nameField).width(fieldWidth).row();
        content.add().height(20).row();
        
        // Host Server button
        TextButton hostButton = new TextButton("Host Server", Main.skin);
        hostButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                hide();
                new HostServerDialog(stage, nameField.getText(), listener).show(stage);
            }
        });
        content.add(hostButton).colspan(2).width(buttonWidth).height(buttonHeight).padBottom(10).row();
        
        // Join Server button (direct connect)
        TextButton joinButton = new TextButton("Join Server", Main.skin);
        joinButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                hide();
                new JoinServerDialog(stage, nameField.getText(), listener).show(stage);
            }
        });
        content.add(joinButton).colspan(2).width(buttonWidth).height(buttonHeight).padBottom(10).row();
        
        // Server Browser button
        TextButton browserButton = new TextButton("Server Browser", Main.skin);
        browserButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                hide();
                new ServerBrowserDialog(stage, nameField.getText(), listener).show(stage);
            }
        });
        content.add(browserButton).colspan(2).width(buttonWidth).height(buttonHeight).padBottom(10).row();
        
        // Back button
        TextButton backButton = new TextButton("Back", Main.skin);
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                hide();
            }
        });
        content.add(backButton).colspan(2).width(buttonWidth).height(buttonHeight).row();
    }
    
    /**
     * Listener interface for multiplayer menu actions
     */
    public interface MultiplayerMenuListener {
        void onHostServer(String playerName, int port, String password, int maxPlayers, int gameMode);
        void onJoinServer(String playerName, String host, int port, String password);
        void onCancel();
    }
}
