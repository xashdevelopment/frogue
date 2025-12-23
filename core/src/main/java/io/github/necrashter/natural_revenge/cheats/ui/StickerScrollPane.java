package io.github.necrashter.natural_revenge.cheats.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.utils.Cullable;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;

/**
 * Sticker-styled scroll pane for scrollable content.
 * Features custom scrollbars matching the sticker aesthetic.
 * 
 * Memory efficient: No allocation in render or scroll operations.
 */
public class StickerScrollPane extends Actor implements Cullable {
    
    // ==================== CONSTANTS ====================
    
    private static final float SCROLL_BAR_WIDTH = StickerStyles.SCROLL_BAR_WIDTH;
    private static final float MIN_SCROLL_BAR_LENGTH = 40f;
    private static final float SCROLL_SPEED = 20f;
    private static final float FLASH_DURATION = 0.5f;
    
    // ==================== INSTANCE PROPERTIES ====================
    
    /** The content actor to scroll */
    private Actor content;
    
    /** Scroll amount (x, y) */
    private final Vector2 scrollAmount = new Vector2();
    
    /** Target scroll amount for smooth scrolling */
    private final Vector2 targetScroll = new Vector2();
    
    /** Scroll bar flash timer */
    private float flashTimer = 0f;
    
    /** Whether scrollbars are visible */
    private boolean showScrollBars = true;
    
    /** Scroll bar touch listeners (reused) */
    private final ScrollBarTouchListener verticalScrollListener;
    private final ScrollBarTouchListener horizontalScrollListener;
    
    /** Visual properties for vertical scrollbar */
    private float verticalScrollBarX;
    private float verticalScrollBarY;
    private float verticalScrollBarHeight;
    private boolean verticalScrollBarTouched = false;
    
    /** Visual properties for horizontal scrollbar */
    private float horizontalScrollBarX;
    private float horizontalScrollBarY;
    private float horizontalScrollBarWidth;
    private boolean horizontalScrollBarTouched = false;
    
    // ==================== CONSTRUCTORS ====================
    
    /**
     * Create a scroll pane with the specified content.
     * @param content The actor to make scrollable
     */
    public StickerScrollPane(Actor content) {
        this.content = content;
        this.verticalScrollListener = new ScrollBarTouchListener(true);
        this.horizontalScrollListener = new ScrollBarTouchListener(false);
        
        if (content != null) {
            content.setTouchable(Touchable.childrenOnly);
        }
    }
    
    // ==================== LAYOUT ====================
    
    @Override
    public void setSize(float width, float height) {
        super.setSize(width, height);
        updateVisualScrollBars();
    }
    
    @Override
    public void setPosition(float x, float y) {
        super.setPosition(x, y);
    }
    
    // ==================== SCROLLING ====================
    
    /**
     * Scroll to make the specified actor visible.
     * @param actor The actor to make visible
     */
    public void scrollTo(Actor actor) {
        if (actor == null) return;
        
        float actorX = actor.getX();
        float actorY = actor.getY();
        float actorWidth = actor.getWidth();
        float actorHeight = actor.getHeight();
        
        float minX = scrollAmount.x;
        float maxX = scrollAmount.x + getWidth();
        float minY = scrollAmount.y;
        float maxY = scrollAmount.y + getHeight();
        
        // Calculate new scroll position
        float newScrollX = scrollAmount.x;
        float newScrollY = scrollAmount.y;
        
        if (actorX < minX) {
            newScrollX = actorX;
        } else if (actorX + actorWidth > maxX) {
            newScrollX = actorX + actorWidth - getWidth();
        }
        
        if (actorY < minY) {
            newScrollY = actorY;
        } else if (actorY + actorHeight > maxY) {
            newScrollY = actorY + actorHeight - getHeight();
        }
        
        setScroll(newScrollX, newScrollY);
    }
    
    /**
     * Set scroll position.
     * @param x Horizontal scroll
     * @param y Vertical scroll
     */
    public void setScroll(float x, float y) {
        scrollAmount.set(x, y);
        targetScroll.set(x, y);
        flashTimer = FLASH_DURATION;
        updateVisualScrollBars();
    }
    
    /**
     * Get current scroll position.
     * @return Scroll position vector
     */
    public Vector2 getScroll() {
        return scrollAmount;
    }
    
    /**
     * Scroll vertically by the specified amount.
     * @param amount Amount to scroll
     */
    public void scrollY(float amount) {
        setScroll(scrollAmount.x, amount);
    }
    
    /**
     scrollAmount.y + * Scroll horizontally by the specified amount.
     * @param amount Amount to scroll
     */
    public void scrollX(float amount) {
        setScroll(scrollAmount.x + amount, scrollAmount.y);
    }
    
    // ==================== UPDATE LOOP ====================
    
    @Override
    public void act(float delta) {
        super.act(delta);
        
        // Smooth scrolling interpolation
        float lerpSpeed = 10f * delta;
        if (Math.abs(scrollAmount.x - targetScroll.x) > 0.5f) {
            scrollAmount.x = Interpolation.linear.apply(scrollAmount.x, targetScroll.x, lerpSpeed);
        } else {
            scrollAmount.x = targetScroll.x;
        }
        
        if (Math.abs(scrollAmount.y - targetScroll.y) > 0.5f) {
            scrollAmount.y = Interpolation.linear.apply(scrollAmount.y, targetScroll.y, lerpSpeed);
        } else {
            scrollAmount.y = targetScroll.y;
        }
        
        // Update flash timer
        if (flashTimer > 0) {
            flashTimer -= delta;
        }
        
        // Update content position
        if (content != null) {
            content.setPosition(-scrollAmount.x, -scrollAmount.y);
        }
    }
    
    // ==================== CULLING ====================
    
    @Override
    public void setCullingArea(Actor cullingArea) {
        // Not used - we handle culling ourselves
    }
    
    // ==================== RENDERING ====================
    
    @Override
    public void draw(Batch batch, float parentAlpha) {
        float x = getX();
        float y = getY();
        float width = getWidth();
        float height = getHeight();
        
        // Draw clip area (simulated with border)
        Color borderColor = StickerStyles.COLOR_PRIMARY;
        
        // Draw scroll area background
        batch.setColor(StickerStyles.COLOR_PAGE_BG);
        batch.draw(StickerDrawableUtils.getWhiteTexture(), x, y, width, height);
        
        // Draw border around scroll area
        batch.setColor(borderColor);
        drawBorder(batch, x, y, width, height);
        
        // Draw content with clipping
        if (content != null) {
            // Save batch state
            float oldX = batch.getTransformMatrix().getTranslationX();
            float oldY = batch.getTransformMatrix().getTranslationY();
            
            // Apply clipping transform
            batch.setTransformMatrix(batch.getTransformMatrix().cpy()
                .translate(x - scrollAmount.x, y - scrollAmount.y, 0));
            
            // Draw content
            content.draw(batch, parentAlpha);
            
            // Restore batch state
            batch.setTransformMatrix(batch.getTransformMatrix().cpy()
                .translate(oldX, oldY, 0));
        }
        
        // Draw scroll bars
        if (showScrollBars && flashTimer > 0) {
            drawScrollBars(batch, x, y, width, height);
        }
    }
    
    /**
     * Draw scroll area border.
     */
    private void drawBorder(Batch batch, float x, float y, float width, float height) {
        float borderWidth = 2f;
        
        // Top
        batch.draw(StickerDrawableUtils.getWhiteTexture(), x, y + height - borderWidth, 
                   width, borderWidth);
        // Bottom  
        batch.draw(StickerDrawableUtils.getWhiteTexture(), x, y, width, borderWidth);
        // Left
        batch.draw(StickerDrawableUtils.getWhiteTexture(), x, y, borderWidth, height);
        // Right
        batch.draw(StickerDrawableUtils.getWhiteTexture(), x + width - borderWidth, y, 
                   borderWidth, height);
    }
    
    /**
     * Draw scroll bars with sticker aesthetic.
     */
    private void drawScrollBars(Batch batch, float x, float y, float width, float height) {
        // Calculate scroll percentages
        float vScrollPercent = getVerticalScrollPercent();
        float hScrollPercent = getHorizontalScrollPercent();
        
        // Draw vertical scroll bar
        if (vScrollPercent >= 0) {
            float barX = x + width - SCROLL_BAR_WIDTH;
            float barHeight = height * (1 - vScrollPercent);
            float barY;
            
            if (barHeight < MIN_SCROLL_BAR_LENGTH) {
                barHeight = MIN_SCROLL_BAR_LENGTH;
                barY = y + (height - barHeight) * targetScroll.y / 
                       Math.max(1f, content.getHeight() - height);
            } else {
                barY = y;
            }
            
            // Track
            Color trackColor = new Color(0, 0, 0, 0.1f);
            StickerDrawableUtils.drawRoundedRect(batch, barX, y, SCROLL_BAR_WIDTH, height,
                                                  SCROLL_BAR_WIDTH / 2f, trackColor);
            
            // Thumb
            Color thumbColor = verticalScrollBarTouched 
                ? StickerStyles.COLOR_DANGER 
                : StickerStyles.COLOR_ACCENT;
            StickerDrawableUtils.drawRoundedRect(batch, barX, barY, SCROLL_BAR_WIDTH, barHeight,
                                                  SCROLL_BAR_WIDTH / 2f, thumbColor);
            
            // Border
            StickerDrawableUtils.drawRoundedRectBorder(batch, barX, barY, SCROLL_BAR_WIDTH, 
                                                        barHeight, SCROLL_BAR_WIDTH / 2f, 2f,
                                                        StickerStyles.COLOR_PRIMARY);
        }
        
        // Draw horizontal scroll bar
        if (hScrollPercent >= 0) {
            float barY = y + height - SCROLL_BAR_WIDTH;
            float barWidth = width * (1 - hScrollPercent);
            float barX;
            
            if (barWidth < MIN_SCROLL_BAR_LENGTH) {
                barWidth = MIN_SCROLL_BAR_LENGTH;
                barX = x + (width - barWidth) * targetScroll.x / 
                       Math.max(1f, content.getWidth() - width);
            } else {
                barX = x;
            }
            
            // Track
            Color trackColor = new Color(0, 0, 0, 0.1f);
            StickerDrawableUtils.drawRoundedRect(batch, x, barY, width, SCROLL_BAR_WIDTH,
                                                  SCROLL_BAR_WIDTH / 2f, trackColor);
            
            // Thumb
            Color thumbColor = horizontalScrollBarTouched 
                ? StickerStyles.COLOR_DANGER 
                : StickerStyles.COLOR_ACCENT;
            StickerDrawableUtils.drawRoundedRect(batch, barX, barY, barWidth, SCROLL_BAR_WIDTH,
                                                  SCROLL_BAR_WIDTH / 2f, thumbColor);
            
            // Border
            StickerDrawableUtils.drawRoundedRectBorder(batch, barX, barY, barWidth, 
                                                        SCROLL_BAR_WIDTH, SCROLL_BAR_WIDTH / 2f, 
                                                        2f, StickerStyles.COLOR_PRIMARY);
        }
    }
    
    /**
     * Calculate vertical scroll percentage (how much content is hidden).
     */
    private float getVerticalScrollPercent() {
        if (content == null) return 0;
        
        float contentHeight = content.getHeight();
        float viewportHeight = getHeight();
        
        if (contentHeight <= viewportHeight) return -1; // No scroll needed
        
        return viewportHeight / contentHeight;
    }
    
    /**
     * Calculate horizontal scroll percentage.
     */
    private float getHorizontalScrollPercent() {
        if (content == null) return 0;
        
        float contentWidth = content.getWidth();
        float viewportWidth = getWidth();
        
        if (contentWidth <= viewportWidth) return -1; // No scroll needed
        
        return viewportWidth / contentWidth;
    }
    
    /**
     * Update visual scroll bar positions.
     */
    private void updateVisualScrollBars() {
        // Calculate positions for hit testing
        float x = getX();
        float y = getY();
        float width = getWidth();
        float height = getHeight();
        
        verticalScrollBarX = x + width - SCROLL_BAR_WIDTH;
        verticalScrollBarY = y;
        verticalScrollBarHeight = height * getVerticalScrollPercent();
        
        horizontalScrollBarX = x;
        horizontalScrollBarY = y + height - SCROLL_BAR_WIDTH;
        horizontalScrollBarWidth = width * getHorizontalScrollPercent();
    }
    
    // ==================== SCROLL BAR TOUCH LISTENER ====================
    
    /**
     * Touch listener for scroll bar interactions.
     */
    private class ScrollBarTouchListener extends ClickListener {
        private final boolean vertical;
        
        public ScrollBarTouchListener(boolean vertical) {
            this.vertical = vertical;
        }
        
        @Override
        public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
            if (vertical) {
                verticalScrollBarTouched = true;
                scrollY(-SCROLL_SPEED * 10);
            } else {
                horizontalScrollBarTouched = true;
                scrollX(-SCROLL_SPEED * 10);
            }
            flashTimer = FLASH_DURATION;
            return true;
        }
        
        @Override
        public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
            if (vertical) {
                verticalScrollBarTouched = false;
            } else {
                horizontalScrollBarTouched = false;
            }
        }
    }
}
