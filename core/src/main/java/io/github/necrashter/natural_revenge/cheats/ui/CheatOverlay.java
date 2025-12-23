package io.github.necrashter.natural_revenge.cheats.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import io.github.necrashter.natural_revenge.GameScreen;
import io.github.necrashter.natural_revenge.Main;
import io.github.necrashter.natural_revenge.cheats.CheatDefinition;
import io.github.necrashter.natural_revenge.cheats.CheatManager;

/**
 * Main cheat overlay container implementing the sticker-themed menu.
 * Singleton pattern for easy access from anywhere in the game.
 * 
 * Integration requirements:
 * 1. CheatOverlay.getInstance() for access
 * 2. Stage must be added to InputMultiplexer in GameScreen
 * 3. getStage().act(delta) called in GameScreen.render()
 * 4. getStage().draw() called AFTER world renderer but BEFORE game stage
 */
public class CheatOverlay extends Actor {
    
    // ==================== SINGLETON ====================
    
    private static CheatOverlay instance;
    
    /**
     * Get the singleton instance.
     * Creates the instance if it doesn't exist.
     * @return The CheatOverlay instance
     */
    public static CheatOverlay getInstance() {
        if (instance == null) {
            instance = new CheatOverlay();
        }
        return instance;
    }
    
    /**
     * Check if the overlay has been created.
     * @return true if instance exists
     */
    public static boolean isCreated() {
        return instance != null;
    }
    
    // ==================== INSTANCE PROPERTIES ====================
    
    /** The stage containing all cheat UI elements */
    private final Stage stage;
    
    /** The root table layout */
    private final Table rootTable;
    
    /** Scroll pane for cheat list */
    private StickerScrollPane scrollPane;
    
    /** Content table inside scroll pane */
    private Table contentTable;
    
    /** Whether the overlay is currently visible */
    private boolean visible = false;
    
    /** Whether the overlay has been initialized */
    private boolean initialized = false;
    
    /** Shared fonts for UI */
    private static BitmapFont titleFont;
    private static BitmapFont bodyFont;
    
    // ==================== CONSTRUCTORS ====================
    
    /**
     * Private constructor - use getInstance() instead.
     */
    private CheatOverlay() {
        // Create viewport matching screen dimensions
        Viewport viewport = new FitViewport(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        
        // Create stage
        stage = new Stage(viewport);
        
        // Create root table
        rootTable = new Table();
        rootTable.setFillParent(true);
        
        // Initialize fonts
        initFonts();
        
        // Build the UI
        buildUI();
        
        // Initially hide
        setVisible(false);
    }
    
    /**
     * Initialize shared fonts.
     */
    private void initFonts() {
        try {
            if (Main.skin != null && Main.skin.getFont("default-font") != null) {
                bodyFont = Main.skin.getFont("default-font");
                // Use same font for title, or scale it
                titleFont = bodyFont;
            } else {
                bodyFont = new BitmapFont();
                titleFont = new BitmapFont();
            }
        } catch (Exception e) {
            Gdx.app.error("CheatOverlay", "Failed to load fonts", e);
            bodyFont = new BitmapFont();
            titleFont = new BitmapFont();
        }
        
        // Share fonts with components
        StickerButton.setSharedFont(bodyFont);
        StickerToggle.setSharedFont(bodyFont);
        StickerLabel.setSharedFonts(bodyFont, titleFont);
    }
    
    /**
     * Build the complete cheat menu UI.
     */
    private void buildUI() {
        // Set background color for stage
        Color bgColor = StickerStyles.COLOR_PAGE_BG;
        stage.getRoot().setColor(bgColor);
        
        // Main container with padding
        Table mainContainer = new Table();
        mainContainer.pad(StickerStyles.PADDING_MEDIUM);
        
        // Title label (accent style)
        StickerLabel titleLabel = new StickerLabel("CHEATS v1.0", true);
        mainContainer.add(titleLabel).height(50f).width(StickerStyles.MENU_WIDTH).row();
        
        // Spacing
        mainContainer.add().height(StickerStyles.PADDING_SMALL).row();
        
        // Create scrollable content
        contentTable = new Table();
        contentTable.defaults().padBottom(StickerStyles.PADDING_SMALL);
        
        // Build cheat categories
        buildCheatCategories();
        
        // Create scroll pane
        scrollPane = new StickerScrollPane(contentTable);
        scrollPane.setSize(StickerStyles.MENU_WIDTH, 400f);
        scrollPane.setTouchable(Touchable.enabled);
        
        mainContainer.add(scrollPane).width(StickerStyles.MENU_WIDTH).height(400f).row();
        
        // Spacing
        mainContainer.add().height(StickerStyles.PADDING_MEDIUM).row();
        
        // Close button
        StickerButton closeButton = new StickerButton("CLOSE MENU");
        closeButton.addButtonClickListener(new CloseClickListener());
        mainContainer.add(closeButton).height(StickerStyles.BUTTON_HEIGHT)
                   .width(StickerStyles.MENU_WIDTH).row();
        
        // Add main container to root
        rootTable.add(mainContainer).align(Align.topRight)
                 .padRight(StickerStyles.MENU_MARGIN)
                 .padTop(StickerStyles.MENU_MARGIN);
        
        stage.addActor(rootTable);
        
        initialized = true;
    }
    
    /**
     * Build cheat category cards with toggles.
     */
    private void buildCheatCategories() {
        CheatDefinition[] cheats = CheatDefinition.createAllCheats();
        
        // Group by category
        CheatDefinition.Category currentCategory = null;
        StickerCard currentCard = null;
        
        for (CheatDefinition cheat : cheats) {
            // New category - create new card
            if (cheat.category != currentCategory) {
                currentCategory = cheat.category;
                currentCard = StickerCard.createCategoryCard(currentCategory.displayName);
                contentTable.add(currentCard).width(StickerStyles.MENU_WIDTH - 
                                                     StickerStyles.PADDING_MEDIUM * 2).row();
            }
            
            // Add toggle to current card
            StickerToggle toggle = new StickerToggle(cheat);
            currentCard.add(toggle).height(StickerStyles.TOGGLE_HEIGHT)
                       .width(StickerStyles.MENU_WIDTH - StickerStyles.PADDING_MEDIUM * 2)
                       .left().row();
        }
    }
    
    // ==================== VISIBILITY MANAGEMENT ====================
    
    /**
     * Toggle the overlay visibility.
     */
    public void toggle() {
        setVisible(!visible);
    }
    
    /**
     * Show the overlay.
     */
    public void show() {
        setVisible(true);
    }
    
    /**
     * Hide the overlay.
     */
    public void hide() {
        setVisible(false);
    }
    
    /**
     * Set overlay visibility.
     * @param visible New visibility state
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
        // Stage doesn't have setVisible, so we control visibility through the stage's root actor
        stage.getRoot().setVisible(visible);
        
        if (visible) {
            // Refresh toggle states from CheatManager
            refreshToggleStates();
        }
    }
    
    /**
     * Check if overlay is visible.
     * @return true if visible
     */
    public boolean isVisible() {
        return visible;
    }
    
    /**
     * Refresh all toggle states from CheatManager.
     */
    private void refreshToggleStates() {
        // This can be used to sync UI with cheat states
        // Currently toggles update themselves via CheatManager
    }
    
    // ==================== STAGE ACCESS ====================
    
    /**
     * Get the stage for input processing.
     * @return The stage containing cheat UI
     */
    public Stage getStage() {
        return stage;
    }
    
    // ==================== UPDATE LOOP ====================
    
    @Override
    public void act(float delta) {
        if (visible && initialized) {
            stage.act(delta);
        }
    }
    
    // ==================== RENDERING ====================
    
    @Override
    public void draw(Batch batch, float parentAlpha) {
        if (visible && initialized) {
            stage.draw();
        }
    }
    
    // ==================== RESIZE HANDLING ====================
    
    /**
     * Handle screen resize.
     * @param width New screen width
     * @param height New screen height
     */
    public void resize(int width, int height) {
        if (stage != null) {
            stage.getViewport().update(width, height, true);
        }
    }
    
    // ==================== DISPOSAL ====================
    
    /**
     * Dispose resources.
     * Call when application is closing.
     */
    public void dispose() {
        if (stage != null) {
            stage.dispose();
        }
        
        // Dispose shared resources
        StickerButton.disposeSharedResources();
        StickerDrawableUtils.dispose();
        
        instance = null;
    }
    
    // ==================== CLOSE BUTTON LISTENER ====================
    
    /**
     * Click listener for the close button.
     */
    private class CloseClickListener extends ClickListener {
        @Override
        public void clicked(InputEvent event, float x, float y) {
            hide();
        }
    }
    
    // ==================== HELPER METHODS ====================
    
    /**
     * Get menu width.
     */
    public static float getMenuWidth() {
        return StickerStyles.MENU_WIDTH;
    }
    
    /**
     * Get menu margin.
     */
    public static float getMenuMargin() {
        return StickerStyles.MENU_MARGIN;
    }
    
    /**
     * Get current active cheat count.
     */
    public int getActiveCheatCount() {
        return CheatManager.getInstance().getActiveCheatCount();
    }
    
    /**
     * Check if any cheats are active.
     */
    public boolean hasActiveCheats() {
        return CheatManager.getInstance().hasActiveCheats();
    }
}
