package io.github.necrashter.natural_revenge;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.TimeUtils;

import io.github.necrashter.natural_revenge.world.GameWorld;
import io.github.necrashter.natural_revenge.world.GameWorldRenderer;
import io.github.necrashter.natural_revenge.world.LowResWorldRenderer;
import io.github.necrashter.natural_revenge.world.levels.LevelMenuBg;
import io.github.necrashter.natural_revenge.ui.multiplayer.MultiplayerMenuDialog;

public class MenuScreen implements Screen {
    final Main game;
    private final Stage stage;

    private final GameWorld world;
    private final GameWorldRenderer worldRenderer;

    public MenuScreen(final Main game) {
        this.game = game;

        stage = new Stage(Main.createViewport());
        Gdx.input.setInputProcessor(stage);

        final TextButton start=new TextButton("Start Game", Main.skin);
        start.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y) {
                startLevel(1);
            }
        });

        final TextButton levelSelect=new TextButton("Level Select", Main.skin);
        levelSelect.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y) {
                levelSelectDialog();
            }
        });

        final TextButton multiplayerButton = new TextButton("Multiplayer", Main.skin);
        multiplayerButton.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y) {
                showMultiplayerMenu();
            }
        });

        final TextButton optionsButton = new TextButton("Options", Main.skin);
        optionsButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                new OptionsDialog(null).show(stage);
            }
        });


        TextButton exit=new TextButton("Exit", Main.skin);
        exit.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.exit();
            }
        });

        Table table=new Table();
        table.setFillParent(true);


        // Center the whole table content for a cleaner look
        table.center();

        table.row().padTop(10);
        table.add(start);
        table.row().padTop(10);
        table.add(levelSelect);
        table.row().padTop(10);
        table.add(multiplayerButton);
        table.row().padTop(10);
        table.add(optionsButton);
        table.row().padTop(10);
        table.add(exit);

        stage.addActor(table);

        // Background world
        world = new LevelMenuBg(game);
        worldRenderer = new LowResWorldRenderer(world);
    }

    public void startLevel(int level) {
        game.setScreen(game.getLevel(level, 1.0f));
        dispose();
    }

    public void showMultiplayerMenu() {
        new MultiplayerMenuDialog(stage, new MultiplayerMenuDialog.MultiplayerMenuListener() {
            @Override
            public void onHostServer(String playerName, int port, String password, int maxPlayers, int gameMode) {
                // Start hosting a server
                startMultiplayerHost(playerName, port, password, maxPlayers, gameMode);
            }

            @Override
            public void onJoinServer(String playerName, String host, int port, String password) {
                // Join an existing server
                startMultiplayerClient(playerName, host, port, password);
            }

            @Override
            public void onCancel() {
                // Do nothing
            }
        }).show(stage);
    }

    private void startMultiplayerHost(String playerName, int port, String password, int maxPlayers, int gameMode) {
        // Start the server
        Main.startServer(port, String.valueOf(gameMode), maxPlayers);
        
        if (Main.getGameServer() != null) {
            // Server started successfully - launch the game as host
            Main.isMultiplayerHost = true;
            Main.multiplayerPlayerName = playerName;
            game.setScreen(game.getLevel(1, 1.0f, true, true)); // Start level 1 as multiplayer host
            dispose();
        } else {
            // Server failed to start
            Dialog dialog = new Dialog("Error", Main.skin);
            dialog.text("Failed to start server on port " + port);
            dialog.button("OK");
            dialog.show(stage);
        }
    }

    private void startMultiplayerClient(String playerName, String host, int port, String password) {
        // Show connecting dialog
        final Dialog connectingDialog = new Dialog("Connecting...", Main.skin);
        connectingDialog.text("Connecting to " + host + ":" + port);
        connectingDialog.show(stage);
        
        // Get NetworkManager and connect
        io.github.necrashter.natural_revenge.network.NetworkManager netManager = 
            io.github.necrashter.natural_revenge.network.NetworkManager.getInstance();
        
        // Add listener for connection result
        netManager.addListener(new io.github.necrashter.natural_revenge.network.NetworkManager.NetworkListenerAdapter() {
            @Override
            public void onConnected(io.github.necrashter.natural_revenge.network.messages.ConnectionMessages.ClientConnectResponse response) {
                connectingDialog.hide();
                Main.isMultiplayerHost = false;
                Main.multiplayerPlayerName = playerName;
                Main.setNetworkManager(netManager);
                game.setScreen(game.getLevel(response.currentLevel, response.easiness, true, false));
                dispose();
            }
            
            @Override
            public void onConnectionFailed(String reason) {
                connectingDialog.hide();
                Dialog error = new Dialog("Connection Failed", Main.skin);
                error.text("Could not connect to server:\n" + reason);
                error.button("OK");
                error.show(stage);
            }
        });
        
        // Start connection
        netManager.connect(host, port, playerName, password);
    }

    public void levelSelectDialog() {
        Dialog dialog = new Dialog("Select Level", Main.skin) {
            @Override
            protected void result(Object object) {
                int i = (int) object;
                if (i > 0) startLevel(i);
            }
        };
        dialog.button("Go Back", 0);
        dialog.getButtonTable().row();
        dialog.button("Level 1: Swamp", 1);
        dialog.getButtonTable().row();
        dialog.button("Level 2: Flying", 2);
        dialog.getButtonTable().row();
//        dialog.button("Level 3: Zombie", 3);
//        dialog.getButtonTable().row();
        dialog.button("Boss Rush", 3);
//        dialog.getButtonTable().row();
        dialog.padTop(new GlyphLayout(Main.skin.getFont("default-font"),"Pause Menu").height*1.2f);
        dialog.padLeft(16); dialog.padRight(16);
        dialog.show(stage);
    }

    @Override
    public void show() {
        Main.music.fadeOut();
    }

    @Override
    public void render(float delta) {
        world.update(delta);
        stage.act(delta);

        double s = (double) TimeUtils.millis() / 100.0;
        double y = 100 * Math.sin(s) + 100;
        double x = 100 * Math.cos(s) + 100;
        double b = y > 100 ? (y - 100) * 0.01 : 0;
//        ScreenUtils.clear((float)b, (float)b, (float)b, 1);
        ScreenUtils.clear(0, 0, 0, 1);
        worldRenderer.render();
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        worldRenderer.screenResize(width, height);
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {

    }

    @Override
    public void dispose() {
        // World renderer is supposed to dispose world as well.
        worldRenderer.dispose();
        stage.dispose();
    }
}
