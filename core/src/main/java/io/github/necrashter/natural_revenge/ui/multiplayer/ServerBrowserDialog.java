package io.github.necrashter.natural_revenge.ui.multiplayer;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;

import io.github.necrashter.natural_revenge.Main;
import io.github.necrashter.natural_revenge.network.NetworkConfig;
import io.github.necrashter.natural_revenge.network.messages.ConnectionMessages.*;

import java.io.*;
import java.net.*;

/**
 * Server browser dialog for discovering and connecting to servers.
 */
public class ServerBrowserDialog extends Dialog {
    private final Stage stage;
    private final String playerName;
    private final MultiplayerMenuDialog.MultiplayerMenuListener listener;
    
    private Table serverListTable;
    private Label statusLabel;
    private final Array<ServerInfo> servers = new Array<>();
    private ServerInfo selectedServer = null;
    
    public ServerBrowserDialog(Stage stage, String playerName, MultiplayerMenuDialog.MultiplayerMenuListener listener) {
        super("Server Browser", Main.skin);
        this.stage = stage;
        this.playerName = playerName;
        this.listener = listener;
        
        // Adjust size for mobile
        float dialogWidth = Main.isMobile() ? 650 : 700;
        float dialogHeight = Main.isMobile() ? 450 : 500;
        setSize(dialogWidth, dialogHeight);
        buildUI();
        
        // Start scanning for servers
        refreshServerList();
    }
    
    private void buildUI() {
        Table content = getContentTable();
        content.pad(10);
        
        // Header
        Table header = new Table(Main.skin);
        header.add(new Label("Server Name", Main.skin)).width(200).padRight(10);
        header.add(new Label("Players", Main.skin)).width(80).padRight(10);
        header.add(new Label("Mode", Main.skin)).width(100).padRight(10);
        header.add(new Label("Ping", Main.skin)).width(60).padRight(10);
        header.add(new Label("", Main.skin)).width(30); // Lock icon
        content.add(header).fillX().padBottom(5).row();
        
        // Server list scroll pane
        serverListTable = new Table(Main.skin);
        ScrollPane scrollPane = new ScrollPane(serverListTable, Main.skin);
        scrollPane.setFadeScrollBars(false);
        content.add(scrollPane).fill().expand().row();
        
        // Status label
        statusLabel = new Label("Searching for servers...", Main.skin);
        statusLabel.setColor(Color.GRAY);
        content.add(statusLabel).padTop(10).row();
        
        // Buttons
        Table buttonTable = new Table();
        
        TextButton refreshButton = new TextButton("Refresh", Main.skin);
        refreshButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                refreshServerList();
            }
        });
        float buttonWidth = Main.isMobile() ? 120 : 100;
        float buttonHeight = Main.isMobile() ? 55 : 40;
        buttonTable.add(refreshButton).width(buttonWidth).height(buttonHeight).padRight(10);
        
        TextButton connectButton = new TextButton("Connect", Main.skin);
        connectButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                connectToSelected();
            }
        });
        buttonTable.add(connectButton).width(buttonWidth).height(buttonHeight).padRight(10);
        
        TextButton directButton = new TextButton("Direct Connect", Main.skin);
        directButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                hide();
                new JoinServerDialog(stage, playerName, listener).show(stage);
            }
        });
        buttonTable.add(directButton).width(Main.isMobile() ? 170 : 150).height(buttonHeight).padRight(10);
        
        TextButton backButton = new TextButton("Back", Main.skin);
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                hide();
                new MultiplayerMenuDialog(stage, listener).show(stage);
            }
        });
        buttonTable.add(backButton).width(80);
        
        content.add(buttonTable).padTop(10).row();
    }
    
    private void refreshServerList() {
        servers.clear();
        serverListTable.clear();
        selectedServer = null;
        statusLabel.setText("Searching for servers...");
        statusLabel.setColor(Color.GRAY);
        
        // Start LAN discovery in background thread
        new Thread(this::discoverLANServers).start();
    }
    
    private void discoverLANServers() {
        try {
            DatagramSocket socket = new DatagramSocket();
            socket.setBroadcast(true);
            socket.setSoTimeout(3000);
            
            // Send broadcast query
            byte[] sendData = "FROGUE_QUERY".getBytes();
            DatagramPacket sendPacket = new DatagramPacket(
                sendData, sendData.length,
                InetAddress.getByName("255.255.255.255"),
                7778
            );
            socket.send(sendPacket);
            
            // Also try localhost
            DatagramPacket localPacket = new DatagramPacket(
                sendData, sendData.length,
                InetAddress.getByName("127.0.0.1"),
                7778
            );
            socket.send(localPacket);
            
            // Receive responses
            byte[] receiveBuffer = new byte[1024];
            long endTime = System.currentTimeMillis() + 3000;
            
            while (System.currentTimeMillis() < endTime) {
                try {
                    DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                    socket.receive(receivePacket);
                    
                    // Parse response
                    ByteArrayInputStream bais = new ByteArrayInputStream(
                        receivePacket.getData(), 0, receivePacket.getLength()
                    );
                    ObjectInputStream ois = new ObjectInputStream(bais);
                    Object obj = ois.readObject();
                    
                    if (obj instanceof ServerQueryResponse) {
                        ServerQueryResponse response = (ServerQueryResponse) obj;
                        ServerInfo info = new ServerInfo();
                        info.address = receivePacket.getAddress().getHostAddress();
                        info.port = 7777; // Default game port
                        info.name = response.serverName;
                        info.currentPlayers = response.currentPlayers;
                        info.maxPlayers = response.maxPlayers;
                        info.gameMode = response.gameMode;
                        info.ping = (int) (System.currentTimeMillis() - response.timestamp);
                        info.passwordProtected = response.passwordProtected;
                        info.version = response.version;
                        
                        servers.add(info);
                    }
                } catch (SocketTimeoutException ignored) {
                    break;
                } catch (Exception e) {
                    // Ignore parse errors
                }
            }
            
            socket.close();
            
            // Update UI on main thread
            com.badlogic.gdx.Gdx.app.postRunnable(this::updateServerList);
            
        } catch (Exception e) {
            com.badlogic.gdx.Gdx.app.postRunnable(() -> {
                statusLabel.setText("Error searching for servers: " + e.getMessage());
                statusLabel.setColor(Color.RED);
            });
        }
    }
    
    private void updateServerList() {
        serverListTable.clear();
        
        if (servers.size == 0) {
            statusLabel.setText("No servers found. Try Direct Connect.");
            statusLabel.setColor(Color.YELLOW);
            return;
        }
        
        statusLabel.setText("Found " + servers.size + " server(s)");
        statusLabel.setColor(Color.GREEN);
        
        for (ServerInfo server : servers) {
            Table row = new Table(Main.skin);
            row.setBackground(Main.skin.newDrawable("white", new Color(0.2f, 0.2f, 0.2f, 0.5f)));
            
            // Server name
            Label nameLabel = new Label(server.name, Main.skin);
            row.add(nameLabel).width(200).padRight(10).padLeft(5);
            
            // Players
            Label playersLabel = new Label(server.currentPlayers + "/" + server.maxPlayers, Main.skin);
            row.add(playersLabel).width(80).padRight(10);
            
            // Game mode
            Label modeLabel = new Label(server.gameMode, Main.skin);
            row.add(modeLabel).width(100).padRight(10);
            
            // Ping with color
            Label pingLabel = new Label(server.ping + "ms", Main.skin);
            if (server.ping < 50) {
                pingLabel.setColor(Color.GREEN);
            } else if (server.ping < 150) {
                pingLabel.setColor(Color.YELLOW);
            } else {
                pingLabel.setColor(Color.RED);
            }
            row.add(pingLabel).width(60).padRight(10);
            
            // Password icon
            Label lockLabel = new Label(server.passwordProtected ? "[X]" : "", Main.skin);
            row.add(lockLabel).width(30);
            
            // Click handler
            row.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    selectServer(server, row);
                }
            });
            
            serverListTable.add(row).fillX().padBottom(2).row();
        }
    }
    
    private void selectServer(ServerInfo server, Table row) {
        // Deselect previous
        for (int i = 0; i < serverListTable.getChildren().size; i++) {
            serverListTable.getChildren().get(i).setColor(Color.WHITE);
        }
        
        // Select new
        selectedServer = server;
        row.setColor(Color.CYAN);
    }
    
    private void connectToSelected() {
        if (selectedServer == null) {
            Dialog error = new Dialog("Error", Main.skin);
            error.text("Please select a server first");
            error.button("OK");
            error.show(stage);
            return;
        }
        
        // Check version compatibility
        if (!NetworkConfig.PROTOCOL_VERSION.equals(selectedServer.version)) {
            Dialog error = new Dialog("Version Mismatch", Main.skin);
            error.text("Server version: " + selectedServer.version + "\nYour version: " + NetworkConfig.PROTOCOL_VERSION);
            error.button("OK");
            error.show(stage);
            return;
        }
        
        // Check if password required
        if (selectedServer.passwordProtected) {
            new PasswordDialog(stage, selectedServer, playerName, listener).show(stage);
            hide();
        } else {
            hide();
            listener.onJoinServer(playerName, selectedServer.address, selectedServer.port, null);
        }
    }
    
    /**
     * Server information
     */
    private static class ServerInfo {
        String address;
        int port;
        String name;
        int currentPlayers;
        int maxPlayers;
        String gameMode;
        int ping;
        boolean passwordProtected;
        String version;
    }
    
    /**
     * Password entry dialog
     */
    private static class PasswordDialog extends Dialog {
        private final TextField passwordField;
        
        public PasswordDialog(Stage stage, ServerInfo server, String playerName, 
                             MultiplayerMenuDialog.MultiplayerMenuListener listener) {
            super("Enter Password", Main.skin);
            
            passwordField = new TextField("", Main.skin);
            passwordField.setPasswordMode(true);
            passwordField.setPasswordCharacter('*');
            
            getContentTable().add(new Label("Server requires password:", Main.skin)).row();
            getContentTable().add(passwordField).width(200).row();
            
            button("Connect", true);
            button("Cancel", false);
        }
        
        public String getPassword() {
            return passwordField.getText();
        }
        
        @Override
        protected void result(Object object) {
            // Handle in click listener
        }
    }
}
