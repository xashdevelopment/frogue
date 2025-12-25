package io.github.necrashter.natural_revenge.network.client;

import com.badlogic.gdx.Gdx;

import io.github.necrashter.natural_revenge.network.NetworkManager;
import io.github.necrashter.natural_revenge.network.messages.*;
import io.github.necrashter.natural_revenge.network.messages.ConnectionMessages.*;
import io.github.necrashter.natural_revenge.network.messages.PlayerMessages.*;
import io.github.necrashter.natural_revenge.network.messages.EntityMessages.*;
import io.github.necrashter.natural_revenge.network.messages.GameMessages.*;
import io.github.necrashter.natural_revenge.world.GameWorld;
import io.github.necrashter.natural_revenge.world.player.Player;
import io.github.necrashter.natural_revenge.ui.multiplayer.ChatPanel;

/**
 * Handles incoming network messages on the client side
 * and updates the game world accordingly.
 */
public class ClientNetworkHandler extends NetworkManager.NetworkListenerAdapter {
    private final GameWorld world;
    private final Player localPlayer;
    private ChatPanel chatPanel;
    
    public ClientNetworkHandler(GameWorld world, Player localPlayer) {
        this.world = world;
        this.localPlayer = localPlayer;
    }
    
    public void setChatPanel(ChatPanel chatPanel) {
        this.chatPanel = chatPanel;
    }
    
    @Override
    public void onMessage(NetworkMessage message) {
        // Handle different message types
        if (message instanceof PlayerPositionUpdate) {
            handlePlayerPosition((PlayerPositionUpdate) message);
        } else if (message instanceof PlayerJoined) {
            handlePlayerJoined((PlayerJoined) message);
        } else if (message instanceof PlayerLeft) {
            handlePlayerLeft((PlayerLeft) message);
        } else if (message instanceof PlayerHealthUpdate) {
            handlePlayerHealth((PlayerHealthUpdate) message);
        } else if (message instanceof EntityUpdateBatch) {
            handleEntityBatch((EntityUpdateBatch) message);
        } else if (message instanceof ChatMessage) {
            handleChatMessage((ChatMessage) message);
        } else if (message instanceof ScoreboardUpdate) {
            handleScoreboardUpdate((ScoreboardUpdate) message);
        } else if (message instanceof GameOver) {
            handleGameOver((GameOver) message);
        }
    }
    
    private void handlePlayerPosition(PlayerPositionUpdate update) {
        int playerID = update.playerID;
        int localID = NetworkManager.getInstance().getLocalPlayerID();
        
        if (playerID == localID) {
            // This is our own position - use for reconciliation
            reconcileLocalPlayer(update);
        } else {
            // Update remote player
            world.updateRemotePlayerPosition(update);
        }
    }
    
    private void reconcileLocalPlayer(PlayerPositionUpdate update) {
        if (localPlayer.predictionManager != null) {
            // Apply server correction via prediction manager
            localPlayer.predictionManager.receiveServerUpdate(
                update.serverTick,
                update.lastProcessedInput,
                update.positionX, update.positionY, update.positionZ,
                update.velocityX, update.velocityY, update.velocityZ
            );
        }
        
        // Update server tick
        localPlayer.serverTick = update.serverTick;
    }
    
    private void handlePlayerJoined(PlayerJoined join) {
        int localID = NetworkManager.getInstance().getLocalPlayerID();
        
        if (join.playerID != localID) {
            // Add remote player
            world.addRemotePlayer(join.playerID, join.playerName);
            RemotePlayer remote = world.getRemotePlayer(join.playerID);
            if (remote != null) {
                remote.setPosition(join.positionX, join.positionY, join.positionZ);
            }
            
            // Show system message
            if (chatPanel != null) {
                chatPanel.addSystemMessage(join.playerName + " joined the game");
            }
        }
    }
    
    private void handlePlayerLeft(PlayerLeft leave) {
        world.removeRemotePlayer(leave.playerID);
        
        // Show system message
        if (chatPanel != null) {
            chatPanel.addSystemMessage("A player left: " + leave.reason);
        }
    }
    
    private void handlePlayerHealth(PlayerHealthUpdate health) {
        int localID = NetworkManager.getInstance().getLocalPlayerID();
        
        if (health.playerID == localID) {
            // Update local player health
            localPlayer.health = health.health;
            localPlayer.maxHealth = health.maxHealth;
            localPlayer.dead = health.isDead;
        } else {
            // Update remote player health
            RemotePlayer remote = world.getRemotePlayer(health.playerID);
            if (remote != null) {
                remote.health = health.health;
                remote.maxHealth = health.maxHealth;
                remote.setDead(health.isDead);
            }
        }
    }
    
    private void handleEntityBatch(EntityUpdateBatch batch) {
        // Process entity updates - for future expansion
        // This would update NPCs, pickups, projectiles etc.
    }
    
    private void handleChatMessage(ChatMessage chat) {
        if (chatPanel != null) {
            chatPanel.receiveMessage(chat);
        }
    }
    
    private void handleScoreboardUpdate(ScoreboardUpdate update) {
        // Update scoreboard widget with new data
        Gdx.app.log("Network", "Received scoreboard update");
    }
    
    private void handleGameOver(GameOver gameOver) {
        Gdx.app.log("Network", "Game over: " + gameOver.reason);
    }
    
    private String getPlayerName(int playerID) {
        int localID = NetworkManager.getInstance().getLocalPlayerID();
        if (playerID == localID) {
            return localPlayer.playerName;
        }
        RemotePlayer remote = world.getRemotePlayer(playerID);
        return remote != null ? remote.playerName : "Player " + playerID;
    }
    
    @Override
    public void onConnected(ClientConnectResponse response) {
        Gdx.app.log("Network", "Connected! Player ID: " + response.assignedPlayerID);
        world.localPlayerID = response.assignedPlayerID;
        localPlayer.playerID = response.assignedPlayerID;
        
        // Existing players will be received via PlayerJoined messages
        Gdx.app.log("Network", "Server has " + response.currentPlayers + " players");
    }
    
    @Override
    public void onDisconnected(String reason) {
        Gdx.app.log("Network", "Disconnected: " + reason);
        world.clearRemotePlayers();
        world.isMultiplayer = false;
    }
}
