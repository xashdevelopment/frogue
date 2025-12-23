package io.github.necrashter.natural_revenge.cheats.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;

/**
 * Sticker-styled button component with tactile, physical feel.
 * Implements the "chubby" sticker aesthetic with hard shadows and thick borders.
 * 
 * Memory efficient: Uses shared font and texture instances.
 */
public class StickerButton extends Actor {
    
    // ==================== SHARED RESOURCES (Memory Critical) ====================
    
    /** Shared body font instance - MAXIMUM 3 fonts total in entire UI */
    private static BitmapFont sharedFont;
    
    /** Shared white texture for programmatic drawing */
    private static final Color tempColor = new Color();
    
    /** Static font initializer */
    static {
        initSharedResources();
    }
    
    /**
     * Initialize shared resources. Called once on first button creation.
     */
    private static void initSharedResources() {
        if (sharedFont == null) {
            try {
                // Try to use the game's font, fallback to default
                if (Main.skin != null && Main.skin.getFont("default-font") != null) {
                    sharedFont = Main.skin.getFont("default-font");
                } else {
                    sharedFont = new BitmapFont();
                }
            } catch (Exception e) {
                Gdx.app.error("StickerButton", "Failed to load font, using default", e);
                sharedFont = new BitmapFont();
            }
        }
    }
    
    /**
     * Set the shared font instance. Call this once during game initialization.
     * @param font The font to share across all buttons
     */
    public static void setSharedFont(BitmapFont font) {
        sharedFont = font;
    }
    
    /**
     * Dispose shared resources. Call on application shutdown.
     */
    public static void disposeSharedResources() {
        // Note: Don't dispose the font if it's from Main.skin
        // Only dispose if we created it ourselves
        if (sharedFont != null && Main.skin == null) {
            sharedFont.dispose();
            sharedFont = null;
        }
    }
    
    // ==================== INSTANCE PROPERTIES ====================
    
    /** Button text */
    private String text;
    
    /** Current pressed state */
    private boolean isPressed = false;
    
    /** Current hover state */
    private boolean isHovered = false;
    
    /** Whether the button is enabled */
    private boolean isEnabled = true;
    
    /** Click listener (reused, no allocation in render) */
    private final ButtonClickListener clickListener;
    
    // ==================== CONSTRUCTORS ====================
    
    /**
     * Create a sticker button with the specified text.
     * @param text Button label text
     */
    public StickerButton(String text) {
        this.text = text;
        this.clickListener = new ButtonClickListener();
        setupDefaults();
        addListener(clickListener);
    }
    
    /**
     * Create an empty sticker button.
     */
    public StickerButton() {
        this.text = "";
        this.clickListener = new ButtonClickListener();
        setupDefaults();
        addListener(clickListener);
    }
    
    /**
     * Set default dimensions and properties.
     */
    private void setupDefaults() {
        setSize(calculateWidth(), StickerStyles.BUTTON_HEIGHT);
    }
    
    /**
     * Calculate preferred width based on text.
     */
    private float calculateWidth() {
        if (text == null || text.isEmpty()) {
            return StickerStyles.BUTTON_MIN_WIDTH;
        }
        
        initSharedResources();
        float[] dims = measureText(text);
        return Math.max(StickerStyles.BUTTON_MIN_WIDTH, dims[0] + StickerStyles.PADDING_LARGE * 2);
    }
    
    // ==================== STATE MANAGEMENT ====================
    
    /**
     * Set the button text.
     * @param text New text label
     */
    public void setText(String text) {
        this.text = text;
        setWidth(calculateWidth());
    }
    
    /**
     * Get the button text.
     * @return Current text
     */
    public String getText() {
        return text;
    }
    
    /**
     * Set whether the button is enabled.
     * @param enabled New enabled state
     */
    public void setEnabled(boolean enabled) {
        this.isEnabled = enabled;
        setTouchable(enabled ? Touchable.enabled : Touchable.disabled);
    }
    
    /**
     * Check if button is enabled.
     * @return true if enabled
     */
    public boolean isEnabled() {
        return isEnabled;
    }
    
    /**
     * Programmatically press the button.
     */
    public void press() {
        if (isEnabled) {
            isPressed = true;
        }
    }
    
    /**
     * Programmatically release the button.
     */
    public void release() {
        isPressed = false;
    }
    
    /**
     * Add a click listener.
     * @param listener Listener to add
     */
    public void addButtonClickListener(ClickListener listener) {
        // Remove default listener and add custom one
        removeListener(clickListener);
        addListener(listener);
    }
    
    // ==================== RENDERING ====================
    
    @Override
    public void draw(Batch batch, float parentAlpha) {
        // Ensure font is initialized
        initSharedResources();
        
        float drawX = getX();
        float drawY = getY();
        float width = getWidth();
        float height = getHeight();
        
        // Apply press offset
        if (isPressed && isEnabled) {
            drawX += StickerStyles.PRESS_OFFSET;
            drawY -= StickerStyles.PRESS_OFFSET;
        }
        
        // Determine colors based on state
        Color bgColor = getBackgroundColor();
        Color textColor = getTextColor();
        
        // Draw hard shadow (only when not pressed)
        if (isEnabled && !isPressed) {
            StickerDrawableUtils.drawHardShadow(batch, drawX, drawY, width, height, bgColor);
        }
        
        // Draw background
        StickerDrawableUtils.drawRoundedRect(batch, drawX, drawY, width, height, bgColor);
        
        // Draw border
        if (isEnabled) {
            StickerDrawableUtils.drawRoundedRectBorder(batch, drawX, drawY, width, height, 
                                                       StickerStyles.COLOR_PRIMARY);
        } else {
            StickerDrawableUtils.drawRoundedRectBorder(batch, drawX, drawY, width, height, 
                                                       StickerStyles.CORNER_RADIUS,
                                                       StickerStyles.BORDER_WIDTH - 1f,
                                                       StickerStyles.COLOR_DISABLED_BORDER);
        }
        
        // Draw text (centered)
        StickerDrawableUtils.drawTextCentered(batch, sharedFont, text, 
                                               drawX, drawY, width, height, textColor);
    }
    
    /**
     * Get the appropriate background color based on state.
     */
    private Color getBackgroundColor() {
        if (!isEnabled) {
            return StickerStyles.COLOR_DISABLED;
        }
        if (isPressed) {
            return StickerStyles.COLOR_PRIMARY; // Black when pressed
        }
        if (isHovered) {
            return StickerStyles.COLOR_ACCENT_HOVER;
        }
        return StickerStyles.COLOR_ACCENT; // Yellow default
    }
    
    /**
     * Get the appropriate text color based on state.
     */
    private Color getTextColor() {
        if (!isEnabled) {
            return StickerStyles.COLOR_DISABLED_TEXT;
        }
        if (isPressed) {
            return Color.WHITE; // White text on black when pressed
        }
        return StickerStyles.COLOR_PRIMARY; // Black text otherwise
    }
    
    // ==================== HELPER METHODS ====================
    
    /**
     * Measure text dimensions.
     */
    private float[] measureText(String text) {
        return StickerDrawableUtils.measureText(sharedFont, text);
    }
    
    // ==================== CLICK LISTENER ====================
    
    /**
     * Internal click listener handling touch events.
     * Reused across instances to avoid allocation.
     */
    private class ButtonClickListener extends ClickListener {
        @Override
        public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
            if (isEnabled) {
                isPressed = true;
                return true; // Must return true to receive touchUp
            }
            return false;
        }
        
        @Override
        public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
            isPressed = false;
            if (isEnabled && contains(x, y)) {
                // Click complete - any additional handling can be done here
            }
        }
        
        @Override
        public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
            isHovered = true;
        }
        
        @Override
        public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
            isHovered = false;
        }
    }
}
