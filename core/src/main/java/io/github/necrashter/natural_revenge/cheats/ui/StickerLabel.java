package io.github.necrashter.natural_revenge.cheats.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Actor;

/**
 * Sticker-styled label component for text display.
 * Supports plain and accent (highlighted) variants.
 * 
 * Memory efficient: Uses shared font instance.
 */
public class StickerLabel extends Actor {
    
    // ==================== LABEL VARIANTS ====================
    
    /** Label style variants */
    public enum Style {
        /** Plain text with no background */
        PLAIN,
        
        /** Yellow background with black border (accent/highlighted) */
        ACCENT,
        
        /** White background with border (card-like) */
        CARD
    }
    
    // ==================== SHARED RESOURCES (Memory Critical) ====================
    
    /** Shared title font for accent labels */
    private static BitmapFont titleFont;
    
    /** Shared body font for plain labels */
    private static BitmapFont bodyFont;
    
    /** Static font initializers */
    static {
        initSharedResources();
    }
    
    /**
     * Initialize shared fonts.
     */
    private static void initSharedResources() {
        if (titleFont == null || bodyFont == null) {
            try {
                if (Main.skin != null) {
                    // Try to get different fonts from skin
                    if (Main.skin.getFont("default-font") != null) {
                        bodyFont = Main.skin.getFont("default-font");
                        // For title, use the same or scale up
                        titleFont = bodyFont;
                    }
                } else {
                    bodyFont = new BitmapFont();
                    titleFont = new BitmapFont();
                }
            } catch (Exception e) {
                Gdx.app.error("StickerLabel", "Failed to load fonts, using default", e);
                bodyFont = new BitmapFont();
                titleFont = new BitmapFont();
            }
        }
    }
    
    /**
     * Set the shared font instances.
     * @param body Font for plain labels
     * @param title Font for accent/title labels
     */
    public static void setSharedFonts(BitmapFont body, BitmapFont title) {
        bodyFont = body;
        titleFont = title != null ? title : body;
    }
    
    // ==================== INSTANCE PROPERTIES ====================
    
    /** Label text */
    private String text;
    
    /** Current style variant */
    private Style style;
    
    /** Whether text is centered */
    private boolean centered = false;
    
    // ==================== CONSTRUCTORS ====================
    
    /**
     * Create a plain label with the specified text.
     * @param text Label text
     */
    public StickerLabel(String text) {
        this(text, Style.PLAIN);
    }
    
    /**
     * Create a label with the specified text and style.
     * @param text Label text
     * @param style Label style variant
     */
    public StickerLabel(String text, Style style) {
        this.text = text;
        this.style = style;
        setupDimensions();
    }
    
    /**
     * Create a label with custom style.
     * @param text Label text
     * @param centered Whether text is centered
     */
    public StickerLabel(String text, boolean centered) {
        this.text = text;
        this.style = centered ? Style.ACCENT : Style.PLAIN;
        this.centered = centered;
        setupDimensions();
    }
    
    /**
     * Set dimensions based on text and style.
     */
    private void setupDimensions() {
        initSharedResources();
        
        BitmapFont font = (style == Style.ACCENT) ? titleFont : bodyFont;
        float[] dims = measureText(font, text);
        
        setHeight(dims[1] + StickerStyles.LABEL_PADDING_V * 2);
        
        if (style == Style.ACCENT) {
            setWidth(dims[0] + StickerStyles.LABEL_PADDING_H * 2);
        } else {
            setWidth(dims[0]);
        }
    }
    
    // ==================== STATE MANAGEMENT ====================
    
    /**
     * Set the label text.
     * @param text New text
     */
    public void setText(String text) {
        this.text = text;
        setupDimensions();
    }
    
    /**
     * Get the label text.
     * @return Current text
     */
    public String getText() {
        return text;
    }
    
    /**
     * Set the label style.
     * @param style New style variant
     */
    public void setStyle(Style style) {
        this.style = style;
        setupDimensions();
    }
    
    /**
     * Set whether text is centered.
     * @param centered Centered state
     */
    public void setCentered(boolean centered) {
        this.centered = centered;
    }
    
    // ==================== RENDERING ====================
    
    @Override
    public void draw(Batch batch, float parentAlpha) {
        initSharedResources();
        
        float x = getX();
        float y = getY();
        float width = getWidth();
        float height = getHeight();
        
        BitmapFont font = (style == Style.ACCENT) ? titleFont : bodyFont;
        
        switch (style) {
            case ACCENT:
                drawAccentLabel(batch, x, y, width, height, font);
                break;
            case CARD:
                drawCardLabel(batch, x, y, width, height, font);
                break;
            case PLAIN:
            default:
                drawPlainLabel(batch, x, y, width, height, font);
                break;
        }
    }
    
    /**
     * Draw plain label (text only, no background).
     */
    private void drawPlainLabel(Batch batch, float x, float y, float width, float height, 
                                 BitmapFont font) {
        batch.setColor(StickerStyles.COLOR_PRIMARY);
        
        if (centered) {
            float[] dims = measureText(font, text);
            float textX = x + (width - dims[0]) / 2f;
            float textY = y + (height - dims[1]) / 2f + font.getCapHeight() / 2f;
            font.draw(batch, text, textX, textY);
        } else {
            font.draw(batch, text, x, y + height - StickerStyles.LABEL_PADDING_V);
        }
    }
    
    /**
     * Draw accent label (yellow background, black border).
     */
    private void drawAccentLabel(Batch batch, float x, float y, float width, float height,
                                  BitmapFont font) {
        // Draw background
        StickerDrawableUtils.drawRoundedRect(batch, x, y, width, height, 
                                              StickerStyles.CORNER_RADIUS, 
                                              StickerStyles.COLOR_ACCENT);
        
        // Draw border
        StickerDrawableUtils.drawRoundedRectBorder(batch, x, y, width, height, 
                                                    StickerStyles.CORNER_RADIUS, 2f,
                                                    StickerStyles.COLOR_PRIMARY);
        
        // Draw text (centered)
        batch.setColor(StickerStyles.COLOR_PRIMARY);
        float[] dims = measureText(font, text);
        float textX = x + (width - dims[0]) / 2f;
        float textY = y + (height - dims[1]) / 2f + font.getCapHeight() / 2f;
        font.draw(batch, text, textX, textY);
    }
    
    /**
     * Draw card label (white background, black border).
     */
    private void drawCardLabel(Batch batch, float x, float y, float width, float height,
                                BitmapFont font) {
        // Draw shadow
        StickerDrawableUtils.drawHardShadow(batch, x, y, width, height, 
                                             StickerStyles.COLOR_SURFACE);
        
        // Draw background
        StickerDrawableUtils.drawRoundedRect(batch, x, y, width, height, 
                                              StickerStyles.CARD_CORNER_RADIUS, 
                                              StickerStyles.COLOR_SURFACE);
        
        // Draw border
        StickerDrawableUtils.drawRoundedRectBorder(batch, x, y, width, height, 
                                                    StickerStyles.CARD_CORNER_RADIUS,
                                                    StickerStyles.CARD_BORDER_WIDTH,
                                                    StickerStyles.COLOR_PRIMARY);
        
        // Draw text (left aligned with padding)
        batch.setColor(StickerStyles.COLOR_PRIMARY);
        font.draw(batch, text, x + StickerStyles.PADDING_SMALL, 
                  y + height - StickerStyles.PADDING_SMALL);
    }
    
    // ==================== HELPER METHODS ====================
    
    /**
     * Measure text dimensions.
     */
    private float[] measureText(BitmapFont font, String text) {
        return StickerDrawableUtils.measureText(font, text);
    }
    
    /**
     * Get recommended height for a label style.
     */
    public static float getHeightForStyle(Style style) {
        if (style == Style.ACCENT) {
            return StickerStyles.LABEL_HEIGHT + 8f;
        }
        return StickerStyles.LABEL_HEIGHT;
    }
}
