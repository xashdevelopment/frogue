package io.github.necrashter.natural_revenge.cheats.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;

import io.github.necrashter.natural_revenge.Main;
import io.github.necrashter.natural_revenge.cheats.CheatDefinition;
import io.github.necrashter.natural_revenge.cheats.CheatManager;

/**
 * Sticker-styled toggle switch component with card-like layout.
 * Combines a toggle switch on the left with a label on the right.
 * Binds to CheatManager for state management.
 * 
 * Memory efficient: Uses shared resources, no allocation in render loop.
 */
public class StickerToggle extends Actor {
    
    // ==================== SHARED RESOURCES (Memory Critical) ====================
    
    /** Shared body font instance */
    private static BitmapFont sharedFont;
    
    /** Static font initializer */
    static {
        initSharedResources();
    }
    
    /**
     * Initialize shared font. Called once on first toggle creation.
     */
    private static void initSharedResources() {
        if (sharedFont == null) {
            try {
                if (Main.skin != null && Main.skin.getFont("default-font") != null) {
                    sharedFont = Main.skin.getFont("default-font");
                } else {
                    sharedFont = new BitmapFont();
                }
            } catch (Exception e) {
                Gdx.app.error("StickerToggle", "Failed to load font, using default", e);
                sharedFont = new BitmapFont();
            }
        }
    }
    
    /**
     * Set the shared font instance.
     * @param font Font to share across all toggles
     */
    public static void setSharedFont(BitmapFont font) {
        sharedFont = font;
    }
    
    // ==================== INSTANCE PROPERTIES ====================
    
    /** The cheat definition this toggle controls */
    private final CheatDefinition cheat;
    
    /** Current toggle state */
    private boolean isOn = false;
    
    /** Animation progress (0 = off, 1 = on) */
    private float animationProgress = 0f;
    
    /** Target animation progress */
    private float targetProgress = 0f;
    
    /** Current pressed state */
    private boolean isPressed = false;
    
    /** Current hover state */
    private boolean isHovered = false;
    
    /** Toggle listener (reused, no allocation in render) */
    private final ToggleClickListener clickListener;
    
    // ==================== LAYOUT CONSTANTS ====================
    
    private static final float TOGGLE_HEIGHT = StickerStyles.TOGGLE_HEIGHT;
    private static final float TOGGLE_TRACK_WIDTH = StickerStyles.TOGGLE_TRACK_WIDTH;
    private static final float TOGGLE_KNOB_SIZE = StickerStyles.TOGGLE_KNOB_SIZE;
    private static final float LABEL_PADDING = StickerStyles.PADDING_MEDIUM;
    
    // ==================== CONSTRUCTORS ====================
    
    /**
     * Create a toggle for the specified cheat.
     * @param cheat The cheat definition to control
     */
    public StickerToggle(CheatDefinition cheat) {
        this.cheat = cheat;
        this.clickListener = new ToggleClickListener();
        
        // Initialize state from CheatManager
        Boolean state = CheatManager.getInstance().getCheatState(cheat.fieldName);
        if (state != null) {
            isOn = state;
            targetProgress = isOn ? 1f : 0f;
            animationProgress = targetProgress;
        }
        
        setupLayout();
        addListener(clickListener);
    }
    
    /**
     * Set up the component layout.
     */
    private void setupLayout() {
        initSharedResources();
        
        // Calculate dimensions
        float[] textDims = measureText(cheat.displayName);
        float textWidth = textDims[0] + LABEL_PADDING * 2;
        float totalWidth = TOGGLE_TRACK_WIDTH + textWidth;
        
        setSize(totalWidth, TOGGLE_HEIGHT);
    }
    
    // ==================== STATE MANAGEMENT ====================
    
    /**
     * Toggle the state.
     */
    public void toggle() {
        isOn = !isOn;
        targetProgress = isOn ? 1f : 0f;
        
        // Update CheatManager
        CheatManager.getInstance().setCheatState(cheat.fieldName, isOn);
    }
    
    /**
     * Set the toggle state programmatically.
     * @param on New state
     */
    public void setOn(boolean on) {
        if (isOn != on) {
            isOn = on;
            targetProgress = isOn ? 1f : 0f;
            CheatManager.getInstance().setCheatState(cheat.fieldName, isOn);
        }
    }
    
    /**
     * Get current state.
     * @return true if toggle is on
     */
    public boolean isOn() {
        return isOn;
    }
    
    /**
     * Get the cheat this toggle controls.
     * @return The cheat definition
     */
    public CheatDefinition getCheat() {
        return cheat;
    }
    
    // ==================== UPDATE LOOP ====================
    
    @Override
    public void act(float delta) {
        // Smooth animation interpolation
        float animSpeed = StickerStyles.TOGGLE_ANIM_DURATION > 0 
            ? delta / StickerStyles.TOGGLE_ANIM_DURATION 
            : 1f;
        
        if (animationProgress < targetProgress) {
            animationProgress = Math.min(1f, animationProgress + animSpeed);
        } else if (animationProgress > targetProgress) {
            animationProgress = Math.max(0f, animationProgress - animSpeed);
        }
        
        super.act(delta);
    }
    
    // ==================== RENDERING ====================
    
    @Override
    public void draw(Batch batch, float parentAlpha) {
        initSharedResources();
        
        float x = getX();
        float y = getY();
        float width = getWidth();
        float height = getHeight();
        
        // Apply press offset
        float drawX = x;
        float drawY = y;
        if (isPressed) {
            drawX += StickerStyles.PRESS_OFFSET;
            drawY -= StickerStyles.PRESS_OFFSET;
        }
        
        // Interpolate between off and on colors for track
        Color trackColor = StickerStyles.COLOR_TOGGLE_OFF_TRACK.cpy().lerp(
            StickerStyles.COLOR_TOGGLE_ON_TRACK, animationProgress);
        
        // Draw shadow (only when not pressed)
        if (!isPressed) {
            Color shadowColor = new Color(0, 0, 0, 0.3f);
            StickerDrawableUtils.drawRoundedRect(batch, drawX + StickerStyles.SHADOW_OFFSET_X, 
                                                  drawY + StickerStyles.SHADOW_OFFSET_Y,
                                                  TOGGLE_TRACK_WIDTH, height, 
                                                  height / 2f, shadowColor);
        }
        
        // Draw track background
        StickerDrawableUtils.drawRoundedRect(batch, drawX, drawY, 
                                              TOGGLE_TRACK_WIDTH, height, 
                                              height / 2f, trackColor);
        
        // Draw track border
        StickerDrawableUtils.drawRoundedRectBorder(batch, drawX, drawY, 
                                                    TOGGLE_TRACK_WIDTH, height, 
                                                    height / 2f, 2f, 
                                                    StickerStyles.COLOR_PRIMARY);
        
        // Calculate knob position
        float knobMargin = (height - TOGGLE_KNOB_SIZE) / 2f;
        float trackInnerWidth = TOGGLE_TRACK_WIDTH - knobMargin * 2;
        float knobX = drawX + knobMargin + (trackInnerWidth - TOGGLE_KNOB_SIZE) * animationProgress;
        float knobY = drawY + knobMargin;
        
        // Draw knob shadow (offset based on animation)
        if (!isPressed) {
            Color knobShadow = new Color(0, 0, 0, 0.2f);
            StickerDrawableUtils.drawRoundedRect(batch, knobX + 2, knobY - 2, 
                                                  TOGGLE_KNOB_SIZE, TOGGLE_KNOB_SIZE,
                                                  TOGGLE_KNOB_SIZE / 2f, knobShadow);
        }
        
        // Draw knob
        StickerDrawableUtils.drawRoundedRect(batch, knobX, knobY, 
                                              TOGGLE_KNOB_SIZE, TOGGLE_KNOB_SIZE, 
                                              TOGGLE_KNOB_SIZE / 2f, Color.WHITE);
        
        // Draw knob border
        StickerDrawableUtils.drawRoundedRectBorder(batch, knobX, knobY, 
                                                    TOGGLE_KNOB_SIZE, TOGGLE_KNOB_SIZE,
                                                    TOGGLE_KNOB_SIZE / 2f, 2f,
                                                    StickerStyles.COLOR_PRIMARY);
        
        // Draw label
        float labelX = drawX + TOGGLE_TRACK_WIDTH + LABEL_PADDING;
        float labelY = drawY + (height - sharedFont.getCapHeight()) / 2f;
        
        batch.setColor(isEnabled() ? StickerStyles.COLOR_PRIMARY : StickerStyles.COLOR_DISABLED_TEXT);
        sharedFont.draw(batch, cheat.displayName, labelX, labelY + sharedFont.getCapHeight() / 2f);
    }
    
    /**
     * Check if toggle is enabled.
     */
    public boolean isEnabled() {
        return cheat != null;
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
     * Internal click listener for toggle interactions.
     */
    private class ToggleClickListener extends ClickListener {
        @Override
        public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
            isPressed = true;
            return true;
        }
        
        @Override
        public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
            isPressed = false;
            // Use hit() method to check if point is inside the actor
            Actor actor = StickerToggle.this;
            if (actor.hit(x, y, false) != null) {
                toggle();
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
