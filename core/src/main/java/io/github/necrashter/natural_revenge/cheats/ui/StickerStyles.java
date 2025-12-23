package io.github.necrashter.natural_revenge.cheats.ui;

import com.badlogic.gdx.graphics.Color;

/**
 * Style constants for sticker-themed UI components.
 * All colors and dimensions are defined here for consistency.
 */
public final class StickerStyles {
    private StickerStyles() {} // Prevent instantiation
    
    // ==================== COLORS ====================
    
    /** Page Background: Cream/Off-White */
    public static final Color COLOR_PAGE_BG = new Color(0xFFFEF9FF);
    
    /** Surface/Cards: Pure White */
    public static final Color COLOR_SURFACE = Color.WHITE;
    
    /** Primary Text/Borders: Jet Black */
    public static final Color COLOR_PRIMARY = Color.BLACK;
    
    /** Accent/Brand: Golden Yellow */
    public static final Color COLOR_ACCENT = new Color(0xFCD34DFF);
    
    /** Secondary Accent: Pastel Red for danger actions */
    public static final Color COLOR_DANGER = new Color(0xFF6B6BFF);
    
    /** Disabled State: Light Gray */
    public static final Color COLOR_DISABLED = new Color(0xEEEEEEFF);
    
    /** Disabled border: Gray */
    public static final Color COLOR_DISABLED_BORDER = new Color(0xCCCCCCFF);
    
    /** Disabled text: Dark Gray */
    public static final Color COLOR_DISABLED_TEXT = new Color(0x999999FF);
    
    /** Hover state: Slightly brighter yellow */
    public static final Color COLOR_ACCENT_HOVER = new Color(0xFDE047FF);
    
    // Toggle colors
    public static final Color COLOR_TOGGLE_OFF_TRACK = new Color(0xCCCCCCFF);
    public static final Color COLOR_TOGGLE_ON_TRACK = COLOR_ACCENT;
    
    // ==================== DIMENSIONS ====================
    
    /** Corner Radius: 16-20 pixels (rounded-xl equivalent) */
    public static final float CORNER_RADIUS = 18f;
    
    /** Border Width: 3-4 pixels solid black */
    public static final float BORDER_WIDTH = 3f;
    
    /** Shadow Offset: 6 pixels down and right */
    public static final float SHADOW_OFFSET_X = 6f;
    public static final float SHADOW_OFFSET_Y = -6f; // Negative for down in libgdx coords
    
    /** Shadow Spread: 0 pixels (hard edge, NO blur) */
    public static final float SHADOW_SPREAD = 0f;
    
    /** Padding between elements: 12-16 pixels */
    public static final float PADDING_SMALL = 12f;
    public static final float PADDING_MEDIUM = 16f;
    public static final float PADDING_LARGE = 20f;
    
    /** Button aspect ratio: Slightly tall (3:2 or 4:3) */
    public static final float BUTTON_HEIGHT = 50f;
    public static final float BUTTON_MIN_WIDTH = 120f;
    
    /** Toggle dimensions */
    public static final float TOGGLE_HEIGHT = 44f;
    public static final float TOGGLE_TRACK_WIDTH = 56f;
    public static final float TOGGLE_KNOB_SIZE = 36f;
    public static final float TOGGLE_KNOB_MARGIN = 4f;
    
    /** Card dimensions */
    public static final float CARD_BORDER_WIDTH = 3f;
    public static final float CARD_CORNER_RADIUS = 16f;
    public static final float CARD_SHADOW_OFFSET = 8f;
    
    /** Label dimensions */
    public static final float LABEL_HEIGHT = 32f;
    public static final float LABEL_PADDING_H = 12f;
    public static final float LABEL_PADDING_V = 6f;
    
    // ==================== ANIMATION ====================
    
    /** Toggle animation duration in seconds */
    public static final float TOGGLE_ANIM_DURATION = 0.15f;
    
    /** Press animation offset */
    public static final float PRESS_OFFSET = 2f;
    
    // ==================== MENU LAYOUT ====================
    
    /** Menu width */
    public static final float MENU_WIDTH = 380f;
    
    /** Menu margin from edges */
    public static final float MENU_MARGIN = 20f;
    
    /** Maximum menu height (will scroll if needed) */
    public static final float MENU_MAX_HEIGHT = 0.9f; // Percentage of screen
    
    /** Scroll bar width */
    public static final float SCROLL_BAR_WIDTH = 8f;
}
