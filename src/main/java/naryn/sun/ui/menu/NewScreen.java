package naryn.sun.ui.menu;

import naryn.sun.Sun;
import naryn.sun.framework.base.UIContext;
import naryn.sun.framework.msdf.Font;
import naryn.sun.framework.msdf.Fonts;
import naryn.sun.framework.objects.BorderRadius;
import naryn.sun.framework.objects.MouseButton;
import naryn.sun.utility.render.DrawUtility;
import naryn.sun.systems.localization.Localizator;
import naryn.sun.systems.modules.Module;
import naryn.sun.systems.modules.api.ModuleCategory;
import naryn.sun.systems.modules.impl.PositionableHudModule;
import naryn.sun.systems.theme.ClientAppearance;
import naryn.sun.ui.components.ColorPicker;
import naryn.sun.ui.menu.components.NewModuleCard;
import naryn.sun.ui.menu.layout.HudModuleEditor;
import naryn.sun.ui.menu.layout.HudModuleLayoutManager;
import naryn.sun.ui.menu.layout.LayoutEditor;
import naryn.sun.ui.menu.layout.MenuLayoutData;
import naryn.sun.ui.menu.layout.MenuLayoutManager;
import naryn.sun.ui.menu.panels.BottomBarPanel;
import naryn.sun.ui.menu.panels.GuiSettingsPanel;
import naryn.sun.ui.menu.panels.TooltipBoxPanel;
import naryn.sun.ui.menu.panels.WindowPanel;
import naryn.sun.ui.menu.toolbar.EditorToolbar;
import naryn.sun.utility.animation.base.Animation;
import naryn.sun.utility.animation.base.Easing;
import naryn.sun.utility.colors.ColorRGBA;
import naryn.sun.utility.gui.GuiUtility;
import naryn.sun.utility.interfaces.IMinecraft;
import naryn.sun.utility.interfaces.IScaledResolution;
import naryn.sun.utility.render.ScissorUtility;
import naryn.sun.utility.sounds.ClientSoundManager;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Экран ClickGUI. Держит состояние (layout, табы, поиск, тултип, режимы
 * редактирования) и обрабатывает ввод (клики/клавиши). Вся отрисовка
 * вынесена в:
 *  - ui.menu.panels.WindowPanel       — окно (рамка, табы, поиск, карточки)
 *  - ui.menu.panels.BottomBarPanel    — нижний островок
 *  - ui.menu.panels.GuiSettingsPanel  — экран GUI Settings
 *  - ui.menu.panels.TooltipBoxPanel   — бокс описания модуля
 *  - ui.menu.toolbar.EditorToolbar    — тулбар Reset/Done
 *
 * Поиск/фильтрация — {@link ModuleSearchHelper}.
 * Скролл — {@link NewScreenScrollState}.
 * Обработка кликов/клавиатуры — {@link NewScreenInputHandler}.
 */
public class NewScreen extends MenuScreen implements IMinecraft, IScaledResolution {

    private final Map<MenuTab, List<NewModuleCard>> tabCards = new LinkedHashMap<>();
    private final List<ColorPicker> colorPickers = new ArrayList<>();
    private final Map<MenuTab, Animation> tabAnimations = new LinkedHashMap<>();
    private MenuTab currentTab = MenuTab.VISUALS;

    private final SearchEditor searchEditor = new SearchEditor();
    private boolean guiSettingsVisible = false;
    private ColorPicker accentPicker;

    private String tooltipText = "";
    private final Animation tooltipAnim = new Animation(200L, 0.0F, Easing.FIGMA_EASE_IN_OUT);
    private String tooltipShownText = "";
    private String tooltipPrevText = "";
    private Animation tooltipSwitchAnim = new Animation(220L, 1.0F, Easing.FIGMA_EASE_IN_OUT);

    private static final NewScreenScrollState scroll = new NewScreenScrollState();

    private long openedAtMs = 0L;
    static final long MENU_KEY_CLOSE_DEBOUNCE_MS = 150L;

    // ---- Layout ----
    private MenuLayoutManager layoutManager = MenuLayoutManager.getInstance();
    private MenuLayoutData layout = layoutManager.getData();
    private LayoutEditor layoutEditor = new LayoutEditor(layout);
    private boolean layoutEditMode = false;

    private HudModuleLayoutManager moduleLayoutManager = HudModuleLayoutManager.getInstance();
    private HudModuleEditor moduleLayoutEditor = new HudModuleEditor(new ArrayList<>());
    private boolean moduleEditMode = false;

    // ---- Геометрия окна ----
    private static final float WIN_W       = MenuLayoutData.BASE_WINDOW_WIDTH;
    private static final float WIN_H       = MenuLayoutData.BASE_WINDOW_HEIGHT;
    private static final float WIN_R       = 10.0F;
    private static final float TOP_H       = 30.0F;
    private static final float TOP_PAD_X   = 14.0F;
    private static final float TOP_PAD_Y   = 7.0F;
    private static final float TAB_PAD_X   = 9.0F;
    private static final float TAB_PAD_Y   = 3.5F;
    private static final float TAB_GAP     = 3.0F;
    private static final float TAB_CP      = 2.0F;
    private static final float TAB_R       = 4.5F;
    private static final float CONTENT_PAD = 12.0F;
    private static final float GRID_GAP    = 8.0F;
    private static final float BOT_PAD     = 3.0F;
    private static final float BOT_R       = 8.0F;
    private static final float SEARCH_W    = 110.0F;
    private static final float SEARCH_H    = 16.0F;
    private static final float BOT_W       = MenuLayoutData.BASE_BOTTOM_WIDTH;
    private static final float BOT_H       = MenuLayoutData.BASE_BOTTOM_HEIGHT;

    public enum VisualsFilter {
        ALL("menu.visuals_filter.all"),
        HUD("menu.visuals_filter.hud"),
        RENDER("menu.visuals_filter.render");
        private final String key;
        VisualsFilter(String key) { this.key = key; }
        public String getLabel() { return Localizator.translate(key); }
    }

    public enum UtilityFilter {
        ALL("menu.utility_filter.all"),
        MOVEMENT("menu.utility_filter.movement");
        private final String key;
        UtilityFilter(String key) { this.key = key; }
        public String getLabel() { return Localizator.translate(key); }
    }

    private VisualsFilter visualsFilter = VisualsFilter.ALL;
    private UtilityFilter utilityFilter = UtilityFilter.ALL;

    private static final String[] TAB_KEYS = {"menu.tab.visuals", "menu.tab.utility", "menu.tab.optimization", "menu.tab.favorites"};

    private Font tabFont;
    private Font botFont;
    private Font searchFont;
    private Font subFont;

    private NewScreenInputHandler inputHandler;

    public NewScreen() {
        for (MenuTab t : MenuTab.values()) {
            tabCards.put(t, new ArrayList<>());
            tabAnimations.put(t, new Animation(250L, 0.0F, Easing.FIGMA_EASE_IN_OUT));
        }
    }

    @Override
    protected void init() {
        closing = false;
        menuAnimation.reset(0.0F);
        openedAtMs = System.currentTimeMillis();
        EditorToolbar.resetState();
        scroll.resetDragging();
        searchEditor.setFocused(false);

        tabFont    = Fonts.MEDIUM.getFont(6.0F);
        botFont    = Fonts.MEDIUM.getFont(6.5F);
        searchFont = Fonts.REGULAR.getFont(6.0F);
        subFont    = Fonts.MEDIUM.getFont(6.0F);

        inputHandler = new NewScreenInputHandler(this);

        if (!layout.custom) {
            layout.updateDefaultPositions((float) this.width, (float) this.height);
        }

        int totalCards = 0;
        for (java.util.List<NewModuleCard> list : tabCards.values()) {
            totalCards += list.size();
        }
        boolean cardsInitialized = totalCards == Sun.getInstance().getModuleManager().getModules().size();
        if (!cardsInitialized) {
            for (MenuTab t : MenuTab.values()) tabCards.get(t).clear();

            for (Module module : Sun.getInstance().getModuleManager().getModules()) {
                MenuTab tab = categoryToTab(module.getInfo().category());
                if (tab == null) continue;
                NewModuleCard card = new NewModuleCard(module);
                card.onInit();
                tabCards.get(tab).add(card);
            }
            scroll.resetScroll();
        }
        super.init();
    }

    private MenuTab categoryToTab(ModuleCategory cat) {
        return switch (cat) {
            case VISUALS -> MenuTab.VISUALS;
            case OPTIMIZATION -> MenuTab.OPTIMIZATION;
            case UTILITY -> MenuTab.UTILITY;
            default -> null;
        };
    }

    @Deprecated
    public static boolean isHudModule(Module module) {
        return ModuleSearchHelper.isHudModule(module);
    }

    @Deprecated
    public static boolean isMovementModule(Module module) {
        return ModuleSearchHelper.isMovementModule(module);
    }

    private List<PositionableHudModule> allPositionableModules() {
        List<PositionableHudModule> result = new ArrayList<>();
        for (Module module : Sun.getInstance().getModuleManager().getModules()) {
            if (module instanceof PositionableHudModule positionable) {
                result.add(positionable);
            }
        }
        return result;
    }

    private List<PositionableHudModule> enabledPositionableModules() {
        List<PositionableHudModule> result = new ArrayList<>();
        for (Module module : Sun.getInstance().getModuleManager().getModules()) {
            if (module instanceof PositionableHudModule positionable && positionable.isEnabled()) {
                result.add(positionable);
            }
        }
        return result;
    }

    private List<NewModuleCard> getVisibleCards() {
        return ModuleSearchHelper.getVisibleCards(tabCards, currentTab, searchEditor.getText(), visualsFilter, utilityFilter);
    }

    @Override
    public void render(UIContext context) {
        naryn.sun.systems.theme.PaletteConfig.getInstance().updatePerFrame();
        menuAnimation.update(!closing);
        if (closing && menuAnimation.getValue() == 0.0F) {
            mc.setScreen(null);
            return;
        }
        float alpha = menuAnimation.getValue();
        scroll.smoothUpdate();

        if (layoutEditMode && !layoutEditor.isDragging()) {
            layoutEditor.validate((float) this.width, (float) this.height);
        }
        if (moduleEditMode && !moduleLayoutEditor.isDragging()) {
            moduleLayoutEditor.validate((float) this.width, (float) this.height);
        }

        if (moduleEditMode) {
            moduleLayoutEditor.render(context, this.width, this.height);
            EditorToolbar.render(context, alpha, botFont, (float) this.width, (float) this.height);
            return;
        }

        float bgBlur = ClientAppearance.getMenuBackgroundBlur();
        if (bgBlur > 0.01F) {
            DrawUtility.blurProgram.draw();
            context.drawBlurredRect(0.0F, 0.0F, (float) this.width, (float) this.height,
                    bgBlur * 35.0F, BorderRadius.ZERO,
                    ColorRGBA.WHITE.withAlpha((int) (255 * alpha * Math.min(1.0F, bgBlur * 1.5F))));
        }

        float bgDarkness = ClientAppearance.getMenuBackgroundDarkness();
        if (bgDarkness > 0.005F) {
            context.drawRect(0, 0, this.width, this.height,
                    new ColorRGBA(8, 10, 18, (int) (bgDarkness * 255.0F * alpha)));
        }

        naryn.sun.ui.menu.background.MenuBackgroundRenderer.getInstance().render(
                context, (float) this.width, (float) this.height, alpha);

        tooltipText = "";

        int origMouseX = context.getMouseX();
        int origMouseY = context.getMouseY();

        boolean mouseOverPicker = false;
        if (accentPicker != null && (accentPicker.isShowing() || accentPicker.getAnimation().getValue() > 0.01F)) {
            if (accentPicker.isHovered(origMouseX, origMouseY) || accentPicker.isPick() || accentPicker.isDragging()) {
                mouseOverPicker = true;
            }
        }
        if (!mouseOverPicker) {
            for (ColorPicker p : colorPickers) {
                if (p.isShowing() || p.getAnimation().getValue() > 0.01F) {
                    if (p.isHovered(origMouseX, origMouseY) || p.isPick() || p.isDragging()) {
                        mouseOverPicker = true;
                        break;
                    }
                }
            }
        }

        float localWinMouseX = mouseOverPicker ? -9999.0F : ((float) origMouseX - layout.windowX) / layout.windowScale;
        float localWinMouseY = mouseOverPicker ? -9999.0F : ((float) origMouseY - layout.windowY) / layout.windowScale;

        context.pushMatrix();
        context.getMatrices().translate(layout.windowX, layout.windowY, 0.0F);
        context.getMatrices().scale(layout.windowScale, layout.windowScale, 1.0F);
        context.setMouseX(Math.round(localWinMouseX));
        context.setMouseY(Math.round(localWinMouseY));
        renderWindow(context, alpha, localWinMouseX, localWinMouseY);
        context.setMouseX(origMouseX);
        context.setMouseY(origMouseY);
        context.popMatrix();

        float localBotMouseX = mouseOverPicker ? -9999.0F : ((float) origMouseX - layout.bottomBarX) / layout.bottomBarScale;
        float localBotMouseY = mouseOverPicker ? -9999.0F : ((float) origMouseY - layout.bottomBarY) / layout.bottomBarScale;

        context.pushMatrix();
        context.getMatrices().translate(layout.bottomBarX, layout.bottomBarY, 0.0F);
        context.getMatrices().scale(layout.bottomBarScale, layout.bottomBarScale, 1.0F);
        context.setMouseX(Math.round(localBotMouseX));
        context.setMouseY(Math.round(localBotMouseY));
        BottomBarPanel.render(context, alpha, localBotMouseX, localBotMouseY, botFont,
                layoutEditMode, moduleEditMode, guiSettingsVisible,
                BOT_W, BOT_H, BOT_PAD, BOT_R);
        context.setMouseX(origMouseX);
        context.setMouseY(origMouseY);
        context.popMatrix();

        if (mouseOverPicker) {
            tooltipText = "";
            tooltipShownText = "";
        }

        if (layoutEditMode && tooltipText.isEmpty()) {
            tooltipText = Localizator.translate("menu.gui_settings.tooltip_placeholder");
        }

        if (!tooltipText.equals(tooltipShownText)) {
            tooltipPrevText = tooltipShownText;
            tooltipShownText = tooltipText;
            tooltipSwitchAnim = new Animation(220L, 0.0F, Easing.FIGMA_EASE_IN_OUT);
        }
        tooltipSwitchAnim.update(1.0F);

        tooltipAnim.update(!tooltipShownText.isEmpty() ? 1.0F : 0.0F);
        TooltipBoxPanel.render(context, alpha, tooltipAnim.getValue(), layoutEditMode,
                layout, tooltipSwitchAnim.getValue(), tooltipShownText, tooltipPrevText);

        colorPickers.removeIf(p -> !p.isShowing() && p.getAnimation().getValue() == 0.0F);
        for (ColorPicker p : colorPickers) p.render(context);
        if (accentPicker != null && accentPicker.isShowing()) {
            ClientAppearance.setAccent(accentPicker.built());
        }

        if (layoutEditMode) {
            layoutEditor.render(context, this.width, this.height);
            EditorToolbar.render(context, alpha, botFont, (float) this.width, (float) this.height);
        }
    }

    private void renderWindow(UIContext context, float alpha, float mouseX, float mouseY) {
        WindowPanel.renderFrame(context, alpha, WIN_W, WIN_H, WIN_R, TOP_H, !guiSettingsVisible);

        if (guiSettingsVisible) {
            float panelX = CONTENT_PAD;
            float panelY = CONTENT_PAD + 4.0F;
            float panelW = WIN_W - CONTENT_PAD * 2.0F;
            float panelH = WIN_H - TOP_H - CONTENT_PAD * 2.0F + 16.0F;

            float totalH = GuiSettingsPanel.getTotalHeight();
            float maxScroll = Math.max(0.0F, totalH - panelH);
            scroll.clampGuiSettingsScroll(maxScroll);
            scroll.smoothUpdateGuiSettings();

            float scrollbarW = 3.5F;
            float contentW = maxScroll > 0.0F ? panelW - scrollbarW - 4.0F : panelW;

            ScissorUtility.push(context.getMatrices(), panelX, panelY, panelW, panelH);
            tooltipText = GuiSettingsPanel.render(context, alpha, panelX, panelY - scroll.guiSettingsScrollOffset, contentW, mouseX, mouseY);
            ScissorUtility.pop();

            if (maxScroll > 0.0F) {
                float scrollbarX = panelX + panelW - scrollbarW;
                WindowPanel.renderScrollbar(context, alpha, mouseX, mouseY,
                        scrollbarX, panelY, scrollbarW, panelH, scroll.guiSettingsScrollOffset, maxScroll, totalH, panelH, scroll.guiSettingsScrollDragging);
            }
            return;
        }

        String[] tabNamesLocalized = new String[TAB_KEYS.length];
        for (int i = 0; i < TAB_KEYS.length; i++) {
            tabNamesLocalized[i] = Localizator.translate(TAB_KEYS[i]);
        }
        WindowPanel.renderHeader(context, alpha, mouseX, mouseY,
                tabFont, searchFont, currentTab, tabAnimations, tabNamesLocalized,
                searchEditor, layoutEditMode, moduleEditMode,
                WIN_W, TOP_PAD_X, TOP_PAD_Y,
                TAB_PAD_X, TAB_PAD_Y, TAB_GAP, TAB_CP, TAB_R,
                SEARCH_W, SEARCH_H);

        float contentX = CONTENT_PAD;
        float contentY = TOP_H + CONTENT_PAD;
        float contentW = WIN_W - CONTENT_PAD * 2;
        float contentH = WIN_H - TOP_H - CONTENT_PAD * 2;

        float sortChipHeight = WindowPanel.sortChipsHeight(currentTab, searchEditor.getText());
        List<NewModuleCard> visible = getVisibleCards();
        float totalH = WindowPanel.computeTotalHeight(visible, GRID_GAP, sortChipHeight);
        float maxScroll = Math.max(0.0F, totalH - contentH);
        scroll.clampScroll(maxScroll);

        float scrollbarW = 3.5F;
        float scrollbarPad = 6.0F;
        float gridW = maxScroll > 0.0F ? contentW - scrollbarW - scrollbarPad : contentW;

        tooltipText = WindowPanel.renderCardGrid(context, mouseX, mouseY, contentX, contentY,
                gridW, contentH, GRID_GAP, scroll.scrollOffset, visible,
                currentTab, searchEditor.getText(), visualsFilter, utilityFilter,
                alpha, layoutEditMode, moduleEditMode);

        if (maxScroll > 0.0F) {
            float scrollbarX = contentX + contentW - scrollbarW;
            WindowPanel.renderScrollbar(context, alpha, mouseX, mouseY, scrollbarX, contentY,
                    scrollbarW, contentH, scroll.scrollOffset, maxScroll, totalH, contentH, scroll.scrollbarDragging);
        }
    }

    // FIX БАГА 2: хелпер для создания ColorPicker по центру экрана
    public ColorPicker createCenteredColorPicker(boolean enableAlpha, ColorRGBA color, String title) {
        float pickerW = ColorPicker.PICKER_W;
        float pickerH = ColorPicker.PICKER_H;
        float cx = this.width / 2.0F - pickerW / 2.0F;
        float cy = this.height / 2.0F - pickerH / 2.0F;
        ColorPicker picker = new ColorPicker(cx, cy, 2.0F, enableAlpha, color, title);
        colorPickers.add(picker);
        return picker;
    }

    @Override
    public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
        boolean overWindow = GuiUtility.isHovered(layout.windowX, layout.windowY, WIN_W * layout.windowScale, WIN_H * layout.windowScale, mouseX, mouseY);
        float localBotX = ((float) mouseX - layout.bottomBarX) / layout.bottomBarScale;
        float localBotY = ((float) mouseY - layout.bottomBarY) / layout.bottomBarScale;
        boolean overBottomBar = GuiUtility.isHovered(0.0F, 0.0F, BOT_W, BOT_H, localBotX, localBotY);
        boolean overPicker = false;
        if (accentPicker != null && (accentPicker.isShowing() || accentPicker.getAnimation().getValue() > 0.01F)) {
            overPicker = true;
        }
        if (!overPicker) {
            for (ColorPicker p : colorPickers) {
                if (p.isShowing() || p.getAnimation().getValue() > 0.01F) {
                    overPicker = true;
                    break;
                }
            }
        }

        if (!overWindow && !overBottomBar && !overPicker && !layoutEditMode && !moduleEditMode) {
            naryn.sun.ui.menu.background.MenuBackgroundRenderer.getInstance().onBackgroundClick(mouseX, mouseY);
        }

        inputHandler.onMouseClicked(mouseX, mouseY, button,
                layoutEditMode, moduleEditMode, guiSettingsVisible,
                layout, layoutManager, layoutEditor, moduleLayoutEditor, moduleLayoutManager,
                colorPickers, accentPicker, scroll,
                WIN_W, WIN_H, TOP_H, CONTENT_PAD, GRID_GAP, BOT_W, BOT_H, BOT_PAD,
                TOP_PAD_X, TOP_PAD_Y, TAB_PAD_X, TAB_PAD_Y, TAB_GAP, TAB_CP, SEARCH_W, SEARCH_H,
                this.width, this.height,
                () -> { layoutEditMode = true; },
                () -> { layoutEditMode = false; },
                () -> {
                    moduleLayoutEditor = new HudModuleEditor(enabledPositionableModules());
                    moduleEditMode = true;
                    PositionableHudModule.setEditingActive(true);
                },
                () -> {
                    moduleEditMode = false;
                    PositionableHudModule.setEditingActive(false);
                },
                () -> { guiSettingsVisible = true; },
                () -> { guiSettingsVisible = false; },
                tab -> { currentTab = tab; },
                searchEditor,
                currentTab, searchEditor.getText(), visualsFilter, utilityFilter,
                vf -> { visualsFilter = vf; },
                uf -> { utilityFilter = uf; },
                this::getVisibleCards,
                this::allPositionableModules,
                this::enabledPositionableModules,
                TAB_KEYS, tabFont, searchFont);
    }

    @Override
    public void onMouseDragged(double mouseX, double mouseY, MouseButton button, double deltaX, double deltaY) {
        if (layoutEditMode) {
            EditorToolbar.onMouseDragged(mouseX, mouseY, (float) this.width, (float) this.height);
            layoutEditor.onMouseDragged(mouseX, mouseY, (float) this.width, (float) this.height);
            return;
        }
        if (moduleEditMode) {
            EditorToolbar.onMouseDragged(mouseX, mouseY, (float) this.width, (float) this.height);
            moduleLayoutEditor.onMouseDragged(mouseX, mouseY, (float) this.width, (float) this.height);
            return;
        }
        if (guiSettingsVisible) {
            if (scroll.guiSettingsScrollDragging) {
                float panelY = CONTENT_PAD + 4.0F;
                float panelH = WIN_H - TOP_H - CONTENT_PAD * 2.0F + 16.0F;
                float totalH = GuiSettingsPanel.getTotalHeight();
                float maxScroll = Math.max(0.0F, totalH - panelH);
                if (maxScroll > 0.0F) {
                    float thumbH = Math.max(24.0F, (panelH / totalH) * panelH);
                    float localWinMouseY = ((float) mouseY - layout.windowY) / layout.windowScale;
                    float newThumbY = localWinMouseY - scroll.guiSettingsScrollDragOffset;
                    float progress = Math.max(0.0F, Math.min(1.0F, (newThumbY - panelY) / (panelH - thumbH)));
                    scroll.guiSettingsScrollTarget = progress * maxScroll;
                    scroll.guiSettingsScrollOffset = scroll.guiSettingsScrollTarget;
                }
                return;
            }
            float panelX = CONTENT_PAD;
            float panelY = CONTENT_PAD + 4.0F;
            float panelW = WIN_W - CONTENT_PAD * 2.0F;
            float localWinMouseX = ((float) mouseX - layout.windowX) / layout.windowScale;
            float localWinMouseY = ((float) mouseY - layout.windowY) / layout.windowScale;
            GuiSettingsPanel.handleMouseDragged(panelX, panelY - scroll.guiSettingsScrollOffset, panelW, localWinMouseX, localWinMouseY);
            return;
        }
        if (scroll.scrollbarDragging) {
            float contentY = TOP_H + CONTENT_PAD;
            float contentH = WIN_H - TOP_H - CONTENT_PAD * 2;
            float sortChipHeight = WindowPanel.sortChipsHeight(currentTab, searchEditor.getText());
            List<NewModuleCard> visible = getVisibleCards();
            float totalH = WindowPanel.computeTotalHeight(visible, GRID_GAP, sortChipHeight);
            float maxScroll = Math.max(0.0F, totalH - contentH);
            if (maxScroll > 0.0F) {
                float scrollbarH = contentH;
                float thumbH = Math.max(24.0F, (contentH / totalH) * scrollbarH);
                float localWinMouseY = ((float) mouseY - layout.windowY) / layout.windowScale;
                float newThumbY = localWinMouseY - scroll.scrollbarDragOffset;
                float progress = Math.max(0.0F, Math.min(1.0F, (newThumbY - contentY) / (scrollbarH - thumbH)));
                scroll.scrollTarget = progress * maxScroll;
                scroll.scrollOffset = scroll.scrollTarget;
            }
            return;
        }
        if (searchEditor.isFocused()) {
            float tabContH = tabFont.height() + TAB_PAD_Y * 2 + TAB_CP * 2;
            float searchX = WIN_W - TOP_PAD_X - SEARCH_W;
            float searchY = TOP_PAD_Y + (tabContH - SEARCH_H) / 2.0F;
            float localWinMouseX = ((float) mouseX - layout.windowX) / layout.windowScale;
            float localWinMouseY = ((float) mouseY - layout.windowY) / layout.windowScale;
            searchEditor.mouseDragged(localWinMouseX, localWinMouseY, searchX, searchY, SEARCH_W, SEARCH_H, searchFont);
            return;
        }
        super.onMouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public void onMouseReleased(double mouseX, double mouseY, MouseButton button) {
        for (ColorPicker p : colorPickers) p.onMouseReleased(mouseX, mouseY, button);
        if (accentPicker != null) {
            accentPicker.onMouseReleased(mouseX, mouseY, button);
            ClientAppearance.setAccent(accentPicker.built());
            Sun.getInstance().getFileManager().writeFile("client");
        }
        scroll.scrollbarDragging = false;
        EditorToolbar.onMouseReleased(mouseX, mouseY, button);
        if (guiSettingsVisible) {
            scroll.guiSettingsScrollDragging = false;
            GuiSettingsPanel.handleMouseReleased(button);
            return;
        }
        if (layoutEditMode) {
            layoutEditor.onMouseReleased(mouseX, mouseY, button);
            layoutManager.save();
            return;
        }
        if (moduleEditMode) {
            moduleLayoutEditor.onMouseReleased(mouseX, mouseY, button);
            moduleLayoutManager.saveAll(allPositionableModules());
            return;
        }
        float localWinMouseX = ((float) mouseX - layout.windowX) / layout.windowScale;
        float localWinMouseY = ((float) mouseY - layout.windowY) / layout.windowScale;
        searchEditor.mouseReleased(localWinMouseX, localWinMouseY, button);
        for (NewModuleCard c : getVisibleCards()) c.onMouseReleased(localWinMouseX, localWinMouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double hAmount, double vAmount) {
        if (layoutEditMode || moduleEditMode) return true;

        for (ColorPicker p : colorPickers) {
            if ((p.isShowing() || p.getAnimation().getValue() > 0.01F) && p.isHovered(mouseX, mouseY)) {
                p.onScroll(mouseX, mouseY, hAmount, vAmount);
                return true;
            }
        }
        if (accentPicker != null && (accentPicker.isShowing() || accentPicker.getAnimation().getValue() > 0.01F) && accentPicker.isHovered(mouseX, mouseY)) {
            accentPicker.onScroll(mouseX, mouseY, hAmount, vAmount);
            return true;
        }

        if (guiSettingsVisible) {
            float panelH = WIN_H - TOP_H - CONTENT_PAD * 2.0F + 16.0F;
            float maxScroll = Math.max(0.0F, GuiSettingsPanel.getTotalHeight() - panelH);
            scroll.scrollGuiSettings((float) vAmount, maxScroll);
            ClientSoundManager.getInstance().playScroll();
            return true;
        }
        float localWinMouseX = ((float) mouseX - layout.windowX) / layout.windowScale;
        float localWinMouseY = ((float) mouseY - layout.windowY) / layout.windowScale;
        if (GuiUtility.isHovered(0.0F, TOP_H, WIN_W, WIN_H - TOP_H, localWinMouseX, localWinMouseY)) {
            scroll.scroll((float) vAmount, Float.MAX_VALUE);
            ClientSoundManager.getInstance().playScroll();
        }
        return super.mouseScrolled(mouseX, mouseY, hAmount, vAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean handled = inputHandler.keyPressed(keyCode, scanCode, modifiers,
                this::getVisibleCards,
                guiSettingsVisible, () -> { guiSettingsVisible = false; },
                layoutEditMode, layoutManager, () -> { layoutEditMode = false; },
                moduleEditMode, moduleLayoutManager, this::allPositionableModules, () -> { moduleEditMode = false; },
                openedAtMs, () -> { closing = true; },
                searchEditor,
                colorPickers);
        if (handled) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        boolean handled = inputHandler.charTyped(chr, modifiers,
                layoutEditMode, moduleEditMode, guiSettingsVisible,
                searchEditor,
                this::getVisibleCards, colorPickers);
        if (handled) return true;
        return super.charTyped(chr, modifiers);
    }

    @Override public boolean shouldPause() { return false; }
    @Override public boolean shouldCloseOnEsc() { return false; }

    public List<ColorPicker> getColorPickers() { return colorPickers; }
}