package io.github.necrashter.natural_revenge.cheats.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.utils.Array;

/**
 * Drawing utilities for sticker-themed UI components.
 * Provides hard shadow, rounded rectangle, and border drawing methods.
 * Memory efficient: Uses shared white texture and pre-allocated objects.
 */
public final class StickerDrawableUtils {
    private StickerDrawableUtils() {} // Prevent instantiation
    
    // ==================== SHARED RESOURCES (Memory Critical) ====================
    
    /** Shared 1x1 white texture for programmatic drawing */
    private static Texture whiteTexture;
    
    /** Static initializer for texture creation */
    static {
        createWhiteTexture();
    }
    
    /**
     * Create the shared 1x1 white texture.
     * Called once on class loading.
     */
    private static void createWhiteTexture() {
        try {
            Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            pixmap.setColor(Color.WHITE);
            pixmap.fill();
            whiteTexture = new Texture(pixmap);
            pixmap.dispose();
        } catch (Exception e) {
            Gdx.app.error("StickerDrawableUtils", "Failed to create white texture", e);
        }
    }
    
    /**
     * Get the shared white texture.
     * @return The 1x1 white texture
     */
    public static Texture getWhiteTexture() {
        return whiteTexture;
    }
    
    /**
     * Dispose the white texture.
     * Call when application is closing.
     */
    public static void dispose() {
        if (whiteTexture != null) {
            whiteTexture.dispose();
            whiteTexture = null;
        }
    }
    
    // ==================== ROUNDED RECTANGLE DRAWING ====================
    
    /**
     * Draw a filled rounded rectangle.
     * Uses 4 corner arcs and 4 rectangles for efficient drawing.
     * 
     * @param batch The batch to draw with
     * @param x X position
     * @param y Y position
     * @param width Width of the rectangle
     * @param height Height of the rectangle
     * @param radius Corner radius
     * @param color Fill color
     */
    public static void drawRoundedRect(Batch batch, float x, float y, 
                                        float width, float height, float radius, Color color) {
        batch.setColor(color);
        
        float diameter = radius * 2f;
        
        // Top-left arc
        batch.draw(whiteTexture, x, y + height - diameter, diameter, diameter, 
                   0, 0, 1, 1);
        
        // Top-right arc
        batch.draw(whiteTexture, x + width - diameter, y + height - diameter, 
                   diameter, diameter, 0, 0, 1, 1);
        
        // Bottom-left arc
        batch.draw(whiteTexture, x, y, diameter, diameter, 0, 0, 1, 1);
        
        // Bottom-right arc
        batch.draw(whiteTexture, x + width - diameter, y, diameter, diameter, 
                   0, 0, 1, 1);
        
        // Top rectangle
        batch.draw(whiteTexture, x + radius, y + height - diameter, 
                   width - diameter * 2, diameter, 0, 0, 1, 1);
        
        // Bottom rectangle
        batch.draw(whiteTexture, x + radius, y, width - diameter * 2, radius, 
                   0, 0, 1, 1);
        
        // Left rectangle
        batch.draw(whiteTexture, x, y + radius, radius, height - diameter * 2, 
                   0, 0, 1, 1);
        
        // Right rectangle
        batch.draw(whiteTexture, x + width - radius, y + radius, radius, 
                   height - diameter * 2, 0, 0, 1, 1);
        
        // Center rectangle (fills any remaining gaps)
        batch.draw(whiteTexture, x + radius, y + radius, 
                   width - diameter * 2, height - diameter * 2, 0, 0, 1, 1);
    }
    
    /**
     * Draw a simple rounded rectangle with default radius.
     */
    public static void drawRoundedRect(Batch batch, float x, float y, 
                                        float width, float height, Color color) {
        drawRoundedRect(batch, x, y, width, height, StickerStyles.CORNER_RADIUS, color);
    }
    
    // ==================== BORDER DRAWING ====================
    
    /**
     * Draw a rounded rectangle border/outline.
     * Uses 4 rectangles for each side to maintain consistent width.
     * 
     * @param batch The batch to draw with
     * @param x X position
     * @param y Y position
     * @param width Width of the rectangle
     * @param height Height of the rectangle
     * @param radius Corner radius
     * @param borderWidth Width of the border
     * @param color Border color
     */
    public static void drawRoundedRectBorder(Batch batch, float x, float y,
                                              float width, float height,
                                              float radius, float borderWidth, Color color) {
        batch.setColor(color);
        
        float diameter = radius * 2f;
        
        // Top border
        batch.draw(whiteTexture, x + radius, y + height - borderWidth, 
                   width - diameter * 2, borderWidth, 0, 0, 1, 1);
        
        // Bottom border
        batch.draw(whiteTexture, x + radius, y, 
                   width - diameter * 2, borderWidth, 0, 0, 1, 1);
        
        // Left border
        batch.draw(whiteTexture, x, y + radius, 
                   borderWidth, height - diameter * 2, 0, 0, 1, 1);
        
        // Right border
        batch.draw(whiteTexture, x + width - borderWidth, y + radius, 
                   borderWidth, height - diameter * 2, 0, 0, 1, 1);
        
        // Top-left corner arc (outer border)
        drawBorderCornerArc(batch, x, y + height - radius, radius, borderWidth, true, color);
        
        // Top-right corner arc (outer border)
        drawBorderCornerArc(batch, x + width - radius, y + height - radius, 
                           radius, borderWidth, true, color);
        
        // Bottom-left corner arc (outer border)
        drawBorderCornerArc(batch, x, y, radius, borderWidth, false, color);
        
        // Bottom-right corner arc (outer border)
        drawBorderCornerArc(batch, x + width - radius, y, radius, borderWidth, false, color);
    }
    
    /**
     * Draw a corner arc for the border.
     */
    private static void drawBorderCornerArc(Batch batch, float x, float y,
                                             float radius, float borderWidth,
                                             boolean top, Color color) {
        batch.setColor(color);
        float diameter = radius * 2f;
        
        if (top) {
            // Outer arc (top)
            batch.draw(whiteTexture, x, y, diameter, borderWidth, 0, 0, 1, 1);
            // Inner arc part
            batch.draw(whiteTexture, x + borderWidth, y + borderWidth, 
                       radius - borderWidth, borderWidth, 0, 0, 1, 1);
        } else {
            // Bottom
            batch.draw(whiteTexture, x, y + radius - borderWidth, 
                       diameter, borderWidth, 0, 0, 1, 1);
            batch.draw(whiteTexture, x + borderWidth, y + radius - borderWidth, 
                       radius - borderWidth, borderWidth, 0, 0, 1, 1);
        }
    }
    
    /**
     * Draw a rounded border with default dimensions.
     */
    public static void drawRoundedRectBorder(Batch batch, float x, float y,
                                              float width, float height, Color color) {
        drawRoundedRectBorder(batch, x, y, width, height, 
                             StickerStyles.CORNER_RADIUS, 
                             StickerStyles.BORDER_WIDTH, color);
    }
    
    // ==================== HARD SHADOW DRAWING ====================
    
    /**
     * Draw a hard shadow for the sticker effect.
     * Shadow is solid color with offset, no blur.
     * 
     * @param batch The batch to draw with
     * @param x Base X position (shadow will be offset)
     * @param y Base Y position (shadow will be offset)
     * @param width Width of the element
     * @param height Height of the element
     * @param radius Corner radius
     * @param shadowColor Shadow color (usually black)
     * @param offsetX Horizontal offset
     * @param offsetY Vertical offset
     */
    public static void drawHardShadow(Batch batch, float x, float y,
                                       float width, float height, float radius,
                                       Color shadowColor, float offsetX, float offsetY) {
        drawRoundedRect(batch, x + offsetX, y + offsetY, width, height, radius, shadowColor);
    }
    
    /**
     * Draw hard shadow with default style settings.
     */
    public static void drawHardShadow(Batch batch, float x, float y,
                                       float width, float height, Color shadowColor) {
        drawHardShadow(batch, x, y, width, height, StickerStyles.CORNER_RADIUS,
                      shadowColor, StickerStyles.SHADOW_OFFSET_X, 
                      StickerStyles.SHADOW_OFFSET_Y);
    }
    
    // ==================== TEXT DRAWING ====================
    
    /** Reusable GlyphLayout for text measurement (no allocation in render) */
    private static final GlyphLayout glyphLayout = new GlyphLayout();
    
    /**
     * Measure text dimensions without allocation.
     * 
     * @param font Font to measure with
     * @param text Text to measure
     * @return Array containing [width, height]
     */
    public static float[] measureText(BitmapFont font, String text) {
        glyphLayout.setText(font, text);
        return new float[] { glyphLayout.width, glyphLayout.height };
    }
    
    /**
     * Draw centered text.
     * 
     * @param batch Batch to draw with
     * @param font Font to use
     * @param text Text to draw
     * @param x X position of container
     * @param y Y position of container
     * @param width Container width
     * @param height Container height
     * @param color Text color
     */
    public static void drawTextCentered(Batch batch, BitmapFont font, String text,
                                         float x, float y, float width, float height,
                                         Color color) {
        if (text == null || text.isEmpty()) return;
        
        batch.setColor(color);
        glyphLayout.setText(font, text);
        
        float textX = x + (width - glyphLayout.width) / 2f;
        float textY = y + (height - glyphLayout.height) / 2f + font.getCapHeight() / 2f;
        
        font.draw(batch, glyphLayout, textX, textY);
    }
    
    /**
     * Draw left-aligned text with padding.
     * 
     * @param batch Batch to draw with
     * @param font Font to use
     * @param text Text to draw
     * @param x X position
     * @param y Y position
     * @param color Text color
     */
    public static void drawTextLeft(Batch batch, BitmapFont font, String text,
                                     float x, float y, Color color) {
        if (text == null || text.isEmpty()) return;
        
        batch.setColor(color);
        glyphLayout.setText(font, text);
        font.draw(batch, glyphLayout, x, y + font.getCapHeight() / 2f);
    }
    
    // ==================== TOGGLE SWITCH DRAWING ====================
    
    /**
     * Draw a toggle switch track.
     * 
     * @param batch Batch to draw with
     * @param x X position
     * @param y Y position
     * @param width Track width
     * @param height Track height
     * @param on Whether the toggle is on
     * @param colorOn Color for on state
     * @param colorOff Color for off state
     */
    public static void drawToggleTrack(Batch batch, float x, float y,
                                        float width, float height,
                                        boolean on, Color colorOn, Color colorOff) {
        Color trackColor = on ? colorOn : colorOff;
        drawRoundedRect(batch, x, y, width, height, height / 2f, trackColor);
    }
    
    /**
     * Draw a toggle switch knob.
     * 
     * @param batch Batch to draw with
     * @param x X position
     * @param y Y position
     * @param size Knob size
     * @param color Knob color
     * @param hasBorder Whether to draw a border
     */
    public static void drawToggleKnob(Batch batch, float x, float y,
                                       float size, Color color, boolean hasBorder) {
        // Draw knob
        drawRoundedRect(batch, x, y, size, size, size / 2f, color);
        
        // Draw border if needed
        if (hasBorder) {
            drawRoundedRectBorder(batch, x, y, size, size, size / 2f, 
                                 StickerStyles.BORDER_WIDTH - 1f, StickerStyles.COLOR_PRIMARY);
        }
    }
    
    // ==================== ARC DRAWING (For scroll bars) ====================
    
    /**
     * Draw an arc segment for scroll bars.
     * 
     * @param batch Batch to draw with
     * @param x Center X
     * @param y Center Y
     * @param radius Arc radius
     * @param startAngle Start angle in degrees
     * @param sweepAngle Sweep angle in degrees
     * @param width Arc width/thickness
     * @param color Arc color
     */
    public static void drawArc(Batch batch, float x, float y, float radius,
                                float startAngle, float sweepAngle, float width,
                                Color color) {
        batch.setColor(color);
        
        // Approximate arc with rectangles for performance
        int segments = (int) (Math.abs(sweepAngle) / 15f) + 1;
        float anglePerSegment = sweepAngle / segments;
        
        for (int i = 0; i < segments; i++) {
            float angle = (startAngle + i * anglePerSegment) * (float) Math.PI / 180f;
            float nextAngle = (startAngle + (i + 1) * anglePerSegment) * (float) Math.PI / 180f;
            
            float x1 = x + (float) Math.cos(angle) * radius;
            float y1 = y + (float) Math.sin(angle) * radius;
            float x2 = x + (float) Math.cos(nextAngle) * radius;
            float y2 = y + (float) Math.sin(nextAngle) * radius;
            
            // Draw segment as a thin trapezoid
            float innerRadius = radius - width;
            float x3 = x + (float) Math.cos(nextAngle) * innerRadius;
            float y3 = y + (float) Math.sin(nextAngle) * innerRadius;
            float x4 = x + (float) Math.cos(angle) * innerRadius;
            float y4 = y + (float) Math.sin(angle) * innerRadius;
            
            drawPolygon(batch, new float[] { x1, y1, x2, y2, x3, y3, x4, y4 }, color);
        }
    }
    
    /**
     * Draw a filled polygon.
     */
    private static void drawPolygon(Batch batch, float[] vertices, Color color) {
        batch.setColor(color);
        int vertexCount = vertices.length / 2;
        short[] indices = new short[vertexCount];
        for (int i = 0; i < vertexCount; i++) {
            indices[i] = (short) i;
        }
        
        // For simplicity, draw as triangles
        for (int i = 1; i < vertexCount - 1; i++) {
            float[] triVerts = new float[] {
                vertices[0], vertices[1],
                vertices[i * 2], vertices[i * 2 + 1],
                vertices[(i + 1) * 2], vertices[(i + 1) * 2 + 1]
            };
            
            // Simple triangle using texture
            batch.draw(whiteTexture, triVerts[0], triVerts[1], 1, 1, 
                      triVerts[0], triVerts[1], 1, 1);
        }
    }
}
