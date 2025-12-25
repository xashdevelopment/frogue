package io.github.necrashter.natural_revenge.ui.multiplayer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;

import io.github.necrashter.natural_revenge.Main;
import io.github.necrashter.natural_revenge.network.NetworkManager;
import io.github.necrashter.natural_revenge.network.messages.GameMessages.ChatMessage;

/**
 * In-game chat panel for multiplayer.
 */
public class ChatPanel extends Table {
    private static final int MAX_MESSAGES = 50;
    private static final float MESSAGE_FADE_TIME = 5f;
    private static final int MAX_MESSAGE_LENGTH = 128;
    
    private final Array<ChatEntry> messages = new Array<>();
    private final Table messageTable;
    private final ScrollPane scrollPane;
    private final TextField inputField;
    private final Table inputTable;
    
    private boolean isOpen = false;
    private float lastActivityTime = 0;
    private ChatListener listener;
    
    public ChatPanel() {
        super(Main.skin);
        
        setFillParent(false);
        
        // Message display area
        messageTable = new Table(Main.skin);
        messageTable.top().left();
        
        scrollPane = new ScrollPane(messageTable, Main.skin);
        scrollPane.setFadeScrollBars(true);
        scrollPane.setScrollingDisabled(true, false);
        
        add(scrollPane).width(400).height(200).left().row();
        
        // Input area
        inputTable = new Table(Main.skin);
        inputField = new TextField("", Main.skin);
        inputField.setMaxLength(MAX_MESSAGE_LENGTH);
        inputField.setMessageText("Press Enter to chat...");
        
        inputField.addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (keycode == Input.Keys.ENTER) {
                    sendMessage();
                    return true;
                } else if (keycode == Input.Keys.ESCAPE) {
                    closeChat();
                    return true;
                }
                return false;
            }
        });
        
        TextButton sendButton = new TextButton("Send", Main.skin);
        sendButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                sendMessage();
            }
        });
        
        inputTable.add(inputField).width(320).padRight(5);
        inputTable.add(sendButton).width(70);
        
        add(inputTable).left().padTop(5).row();
        
        // Initially hidden input
        inputTable.setVisible(false);
    }
    
    /**
     * Update chat panel
     */
    public void update(float delta) {
        lastActivityTime += delta;
        
        // Update message visibility (fade old messages when chat is closed)
        if (!isOpen) {
            for (ChatEntry entry : messages) {
                entry.age += delta;
                if (entry.age > MESSAGE_FADE_TIME && entry.label != null) {
                    float alpha = Math.max(0, 1 - (entry.age - MESSAGE_FADE_TIME) / 2f);
                    entry.label.setColor(entry.color.r, entry.color.g, entry.color.b, alpha);
                }
            }
        }
        
        // Check for chat toggle key
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.T)) {
            if (!isOpen) {
                openChat();
            }
        }
    }
    
    /**
     * Open chat input
     */
    public void openChat() {
        isOpen = true;
        inputTable.setVisible(true);
        if (getStage() != null) {
            getStage().setKeyboardFocus(inputField);
        }
        
        // Show all messages
        for (ChatEntry entry : messages) {
            if (entry.label != null) {
                entry.label.setColor(entry.color);
            }
        }
    }
    
    /**
     * Close chat input
     */
    public void closeChat() {
        isOpen = false;
        inputTable.setVisible(false);
        inputField.setText("");
        if (getStage() != null) {
            getStage().setKeyboardFocus(null);
        }
    }
    
    /**
     * Toggle chat open/closed
     */
    public void toggle() {
        if (isOpen) {
            closeChat();
        } else {
            openChat();
        }
    }
    
    /**
     * Send the current message
     */
    private void sendMessage() {
        String text = inputField.getText().trim();
        if (!text.isEmpty()) {
            if (listener != null) {
                listener.onSendMessage(text);
            }
            
            // Also add to local display
            addMessage(NetworkManager.getInstance().getLocalPlayerName(), text, Color.WHITE);
        }
        closeChat();
    }
    
    /**
     * Receive a chat message
     */
    public void receiveMessage(ChatMessage message) {
        Color color = Color.WHITE;
        
        switch (message.messageType) {
            case 0: // Regular chat
                color = Color.WHITE;
                break;
            case 1: // System message
                color = Color.YELLOW;
                break;
            case 2: // Kill message
                color = Color.RED;
                break;
        }
        
        addMessage(message.senderName, message.message, color);
    }
    
    /**
     * Add a message to the chat
     */
    public void addMessage(String sender, String text, Color color) {
        ChatEntry entry = new ChatEntry();
        entry.sender = sender;
        entry.text = text;
        entry.color = color.cpy();
        entry.age = 0;
        
        messages.add(entry);
        
        // Trim old messages
        while (messages.size > MAX_MESSAGES) {
            messages.removeIndex(0);
        }
        
        // Rebuild message display
        rebuildMessages();
        
        // Reset fade timer
        lastActivityTime = 0;
    }
    
    /**
     * Add a system message
     */
    public void addSystemMessage(String text) {
        addMessage("System", text, Color.YELLOW);
    }
    
    /**
     * Add a kill message
     */
    public void addKillMessage(String killer, String victim, String weapon) {
        String text = killer + " killed " + victim;
        if (weapon != null && !weapon.isEmpty()) {
            text += " with " + weapon;
        }
        addMessage(null, text, Color.RED);
    }
    
    private void rebuildMessages() {
        messageTable.clear();
        
        for (ChatEntry entry : messages) {
            String displayText = entry.sender != null ? 
                "[" + entry.sender + "] " + entry.text : entry.text;
            
            Label label = new Label(displayText, Main.skin);
            label.setWrap(true);
            label.setColor(entry.color);
            entry.label = label;
            
            messageTable.add(label).width(380).left().padBottom(2).row();
        }
        
        // Scroll to bottom
        scrollPane.layout();
        scrollPane.setScrollPercentY(1);
    }
    
    public boolean isOpen() {
        return isOpen;
    }
    
    public void setListener(ChatListener listener) {
        this.listener = listener;
    }
    
    /**
     * Chat entry data
     */
    private static class ChatEntry {
        String sender;
        String text;
        Color color;
        float age;
        Label label;
    }
    
    /**
     * Chat listener interface
     */
    public interface ChatListener {
        void onSendMessage(String message);
    }
}
