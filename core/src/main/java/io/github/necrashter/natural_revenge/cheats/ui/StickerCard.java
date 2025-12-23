package io.github.necrashter.natural_revenge.cheats.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Table;

/**
 * Sticker-styled card container for grouping related UI elements.
 * Features white background, black border, and hard shadow.
 * 
 * Usage:
 * - Group related toggles together
 * - Visual separation of cheat categories
 */
public class StickerCard extends Table {
    
    // ==================== INSTANCE PROPERTIES ====================
    
    /** Card title (optional) */
    private String title;
    
    /** Whether card has a title */
    private boolean hasTitle = false;
    
    // ==================== CONSTRUCTORS ====================
    
    /**
     * Create an empty sticker card with default padding.
     */
    public StickerCard() {
        super();
        setupDefaults();
    }
    
    /**
     * Create a sticker card with the specified title.
     * @param title Card title text
     */
    public StickerCard(String title) {
        super();
        this.title = title;
        this.hasTitle = true;
        setupDefaults();
        addTitle();
    }
    
    /**
     * Set up default styling.
     */
    private void setupDefaults() {
        // Set internal padding
        pad(StickerStyles.PADDING_MEDIUM);
        
        // Set background color (will be drawn in draw())
        setColor(Color.WHITE);
    }
    
    /**
     * Add title to the card.
     */
    private void addTitle() {
        if (title != null && !title.isEmpty()) {
            StickerLabel titleLabel = new StickerLabel(title, StickerLabel.Style.ACCENT);
            add(titleLabel).left().padBottom(StickerStyles.PADDING_MEDIUM).row();
        }
    }
    
    // ==================== STATE MANAGEMENT ====================
    
    /**
     * Set the card title.
     * @param title New title text
     */
    public void setTitle(String title) {
        this.title = title;
        this.hasTitle = title != null && !title.isEmpty();
        
        // Rebuild the card content
        clearChildren();
        if (hasTitle) {
            addTitle();
        }
    }
    
    /**
     * Get the card title.
     * @return Current title or null
     */
    public String getTitle() {
        return title;
    }
    
    // ==================== RENDERING ====================
    
    @Override
    public void draw(Batch batch, float parentAlpha) {
        float x = getX();
        float y = getY();
        float width = getWidth();
        float height = getHeight();
        
        // Draw hard shadow
        Color shadowColor = new Color(0, 0, 0, 0.25f);
        StickerDrawableUtils.drawRoundedRect(batch, 
                                              x + StickerStyles.CARD_SHADOW_OFFSET,
                                              y - StickerStyles.CARD_SHADOW_OFFSET,
                                              width, height, 
                                              StickerStyles.CARD_CORNER_RADIUS,
                                              shadowColor);
        
        // Draw background
        StickerDrawableUtils.drawRoundedRect(batch, x, y, width, height, 
                                              StickerStyles.CARD_CORNER_RADIUS,
                                              StickerStyles.COLOR_SURFACE);
        
        // Draw border
        StickerDrawableUtils.drawRoundedRectBorder(batch, x, y, width, height, 
                                                    StickerStyles.CARD_CORNER_RADIUS,
                                                    StickerStyles.CARD_BORDER_WIDTH,
                                                    StickerStyles.COLOR_PRIMARY);
        
        // Draw children
        super.drawChildren(batch, parentAlpha);
    }
    
    // ==================== FACTORY METHODS ====================
    
    /**
     * Create a card for a cheat category.
     * @param categoryName Category display name
     * @return Configured StickerCard
     */
    public static StickerCard createCategoryCard(String categoryName) {
        return new StickerCard(categoryName);
    }
    
    /**
     * Create an empty card for custom content.
     * @return Configured StickerCard
     */
    public static StickerCard createEmptyCard() {
        return new StickerCard();
    }
    
    // ==================== HELPER METHODS ====================
    
    /**
     * Get the recommended corner radius for cards.
     */
    public static float getCornerRadius() {
        return StickerStyles.CARD_CORNER_RADIUS;
    }
    
    /**
     * Get the recommended border width for cards.
     */
    public static float getBorderWidth() {
        return StickerStyles.CARD_BORDER_WIDTH;
    }
}
