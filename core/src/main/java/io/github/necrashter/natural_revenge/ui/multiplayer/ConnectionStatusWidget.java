package io.github.necrashter.natural_revenge.ui.multiplayer;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.*;

import io.github.necrashter.natural_revenge.Main;
import io.github.necrashter.natural_revenge.network.NetworkManager;
import io.github.necrashter.natural_revenge.network.NetworkManager.ConnectionState;

/**
 * Widget showing network connection status and latency.
 */
public class ConnectionStatusWidget extends Table {
    private final Label statusLabel;
    private final Label pingLabel;
    private final Label packetLossLabel;
    
    private float updateTimer = 0;
    private static final float UPDATE_INTERVAL = 0.5f;
    
    // Packet loss tracking
    private int packetsReceived = 0;
    private int packetsExpected = 0;
    private float packetLossPercent = 0;
    
    public ConnectionStatusWidget() {
        super(Main.skin);
        
        setBackground(Main.skin.newDrawable("white", new Color(0, 0, 0, 0.5f)));
        pad(5);
        
        statusLabel = new Label("Offline", Main.skin);
        pingLabel = new Label("-- ms", Main.skin);
        packetLossLabel = new Label("0% loss", Main.skin);
        
        add(statusLabel).left().row();
        add(pingLabel).left().row();
        add(packetLossLabel).left().row();
        
        updateDisplay();
    }
    
    /**
     * Update the widget
     */
    public void update(float delta) {
        updateTimer += delta;
        
        if (updateTimer >= UPDATE_INTERVAL) {
            updateTimer = 0;
            updateDisplay();
        }
    }
    
    private void updateDisplay() {
        NetworkManager network = NetworkManager.getInstance();
        ConnectionState state = network.getState();
        
        // Update status
        switch (state) {
            case DISCONNECTED:
                statusLabel.setText("Offline");
                statusLabel.setColor(Color.GRAY);
                break;
            case CONNECTING:
                statusLabel.setText("Connecting...");
                statusLabel.setColor(Color.YELLOW);
                break;
            case CONNECTED:
                statusLabel.setText("Connected");
                statusLabel.setColor(Color.GREEN);
                break;
            case RECONNECTING:
                statusLabel.setText("Reconnecting...");
                statusLabel.setColor(Color.ORANGE);
                break;
            case FAILED:
                statusLabel.setText("Failed");
                statusLabel.setColor(Color.RED);
                break;
        }
        
        // Update ping
        if (state == ConnectionState.CONNECTED) {
            long ping = network.getLatency();
            pingLabel.setText(ping + " ms");
            
            // Color code ping
            if (ping < 50) {
                pingLabel.setColor(Color.GREEN);
            } else if (ping < 100) {
                pingLabel.setColor(Color.YELLOW);
            } else if (ping < 200) {
                pingLabel.setColor(Color.ORANGE);
            } else {
                pingLabel.setColor(Color.RED);
            }
        } else {
            pingLabel.setText("-- ms");
            pingLabel.setColor(Color.GRAY);
        }
        
        // Update packet loss
        packetLossLabel.setText(String.format("%.1f%% loss", packetLossPercent));
        if (packetLossPercent < 1) {
            packetLossLabel.setColor(Color.GREEN);
        } else if (packetLossPercent < 5) {
            packetLossLabel.setColor(Color.YELLOW);
        } else {
            packetLossLabel.setColor(Color.RED);
        }
    }
    
    /**
     * Track a received packet
     */
    public void trackPacketReceived() {
        packetsReceived++;
        updatePacketLoss();
    }
    
    /**
     * Track an expected packet
     */
    public void trackPacketExpected() {
        packetsExpected++;
        updatePacketLoss();
    }
    
    private void updatePacketLoss() {
        if (packetsExpected > 0) {
            packetLossPercent = (1 - (float) packetsReceived / packetsExpected) * 100;
        }
        
        // Reset counters periodically
        if (packetsExpected > 100) {
            packetsExpected = 0;
            packetsReceived = 0;
        }
    }
    
    /**
     * Get connection quality indicator (0-3)
     * 0 = Poor, 1 = Fair, 2 = Good, 3 = Excellent
     */
    public int getConnectionQuality() {
        NetworkManager network = NetworkManager.getInstance();
        if (network.getState() != ConnectionState.CONNECTED) {
            return 0;
        }
        
        long ping = network.getLatency();
        if (ping < 50 && packetLossPercent < 1) {
            return 3; // Excellent
        } else if (ping < 100 && packetLossPercent < 3) {
            return 2; // Good
        } else if (ping < 200 && packetLossPercent < 10) {
            return 1; // Fair
        } else {
            return 0; // Poor
        }
    }
}
