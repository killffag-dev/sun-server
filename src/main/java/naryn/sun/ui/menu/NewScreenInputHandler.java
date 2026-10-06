package naryn.sun.ui.menu;

import naryn.sun.Sun;
import naryn.sun.framework.objects.MouseButton;
import naryn.sun.systems.localization.Localizator;
import naryn.sun.systems.modules.impl.PositionableHudModule;
import naryn.sun.ui.components.ColorPicker;
import naryn.sun.ui.menu.components.NewModuleCard;
import naryn.sun.ui.menu.layout.HudModuleEditor;
import naryn.sun.ui.menu.layout.HudModuleLayoutManager;
import naryn.sun.ui.menu.layout.LayoutEditor;
import naryn.sun.ui.menu.layout.MenuLayoutData;
import naryn.sun.ui.menu.layout.MenuLayoutManager;
import naryn.sun.ui.menu.panels.GuiSettingsPanel;
import naryn.sun.ui.menu.panels.WindowPanel;
import naryn.sun.ui.menu.toolbar.EditorToolbar;
import naryn.sun.utility.gui.GuiUtility;
import naryn.sun.utility.sounds.ClientSoundManager;

import java.util.List;

/**
 * Обработчик ввода (кликов/драга/клавиатуры) для NewScreen.
 * Вынесен из NewScreen для снижения размера класса.
 */
public class NewScreenInputHandler {

    private final NewScreen screen;

    public NewScreenInputHandler(NewScreen screen) {
        this.screen = screen;
    }

    public void onMouseClicked(double mouseX, double mouseY, MouseButton button,
                               boolean layoutEditMode, boolean moduleEditMode,
                               boolean guiSettingsVisible,
                               MenuLayoutData layout, MenuLayoutManager layoutManager,
                               LayoutEditor layoutEditor, HudModuleEditor moduleLayoutEditor,
                               HudModuleLayoutManager moduleLayoutManager,
                               List<ColorPicker> colorPickers, ColorPicker accentPicker,
                               NewScreenScrollState scroll,
                               float winW, float winH, float topH,
                               float contentPad, float gridGap, float botW, float botH, float botPad,
                               float topPadX, float topPadY, float tabPadX, float tabPadY,
                               float tabGap, float tabCp, float searchW, float searchH,
                               int screenWidth, int screenHeight,
                               Runnable setLayoutEditMode, Runnable clearLayoutEditMode,
                               Runnable setModuleEditMode, Runnable clearModuleEditMode,
                               Runnable setGuiSettingsVisible, Runnable clearGuiSettings,
                               java.util.function.Consumer<MenuTab> setCurrentTab,
                               SearchEditor searchEditor,
                               MenuTab currentTab, String searchQuery,
                               NewScreen.VisualsFilter visualsFilter,
                               NewScreen.UtilityFilter utilityFilter,
                               java.util.function.Consumer<NewScreen.VisualsFilter> setVisualsFilter,
                               java.util.function.Consumer<NewScreen.UtilityFilter> setUtilityFilter,
                               java.util.function.Supplier<List<NewModuleCard>> getVisibleCards,
                               java.util.function.Supplier<List<PositionableHudModule>> allPositionableModules,
                               java.util.function.Supplier<List<PositionableHudModule>> enabledPositionableModules,
                               String[] tabKeys,
                               naryn.sun.framework.msdf.Font tabFont,
                               naryn.sun.framework.msdf.Font searchFont) {

        if (layoutEditMode) {
            handleEditorClick(mouseX, mouseY, button, layoutManager, layoutEditor, layout, clearLayoutEditMode, screenWidth);
            return;
        }
        if (moduleEditMode) {
            handleModuleEditorClick(mouseX, mouseY, button, moduleLayoutManager, moduleLayoutEditor,
                    allPositionableModules, clearModuleEditMode, screenWidth);
            return;
        }

        if (accentPicker != null && (accentPicker.isShowing() || accentPicker.getAnimation().getValue() > 0.01F)) {
            boolean wasHovered = accentPicker.isHovered(mouseX, mouseY);
            boolean wasPick = accentPicker.isPick();
            if (wasHovered || wasPick) {
                accentPicker.onMouseClicked(mouseX, mouseY, button);
                return;
            } else {
                accentPicker.setShowing(false);
                return;
            }
        }

        for (int i = colorPickers.size() - 1; i >= 0; i--) {
            ColorPicker p = colorPickers.get(i);
            boolean wasShowing = p.isShowing() || p.getAnimation().getValue() > 0.01F;
            boolean wasHovered = p.isHovered(mouseX, mouseY);
            boolean wasPick = p.isPick();
            if (wasShowing && (wasHovered || wasPick)) {
                p.onMouseClicked(mouseX, mouseY, button);
                return;
            } else if (wasShowing) {
                p.setShowing(false);
                return;
            }
        }

        // Нижний островок
        float localBotMouseX = ((float) mouseX - layout.bottomBarX) / layout.bottomBarScale;
        float localBotMouseY = ((float) mouseY - layout.bottomBarY) / layout.bottomBarScale;

        if (GuiUtility.isHovered(0.0F, 0.0F, botW, botH, localBotMouseX, localBotMouseY)) {
            float btnW = (botW - botPad * 2 - 6.0F) / 3.0F;
            float btnX = botPad;
            for (int i = 0; i < 3; i++) {
                boolean hovered = GuiUtility.isHovered(btnX, botPad, btnW, botH - botPad * 2, localBotMouseX, localBotMouseY);
                if (hovered) {
                    ClientSoundManager.getInstance().playButtonClick();
                    if (i == 0) {
                        clearGuiSettings.run();
                    } else if (i == 1) {
                        float pbW = btnW / 2.0F;
                        if (GuiUtility.isHovered(btnX, botPad, pbW, botH - botPad * 2, localBotMouseX, localBotMouseY)) {
                            setModuleEditMode.run();
                        } else {
                            setLayoutEditMode.run();
                        }
                    } else if (i == 2) {
                        setGuiSettingsVisible.run();
                    }
                    return;
                }
                btnX += btnW + 3.0F;
            }
            return;
        }

        float localWinMouseX = ((float) mouseX - layout.windowX) / layout.windowScale;
        float localWinMouseY = ((float) mouseY - layout.windowY) / layout.windowScale;

        if (guiSettingsVisible) {
            handleGuiSettingsClick(localWinMouseX, localWinMouseY, button, scroll,
                    winW, winH, topH, contentPad, layout);
            return;
        }

        // Табы
        MenuTab[] tabs = MenuTab.values();
        float[] tabWidths = new float[tabs.length];
        float totalTabW = tabCp * 2;
        for (int i = 0; i < tabs.length; i++) {
            tabWidths[i] = tabFont.width(Localizator.translate(tabKeys[i])) + tabPadX * 2;
            if (i > 0) totalTabW += tabGap;
            totalTabW += tabWidths[i];
        }
        float tabContH = tabFont.height() + tabPadY * 2 + tabCp * 2;
        float tabX = topPadX + tabCp;
        float tabIY = topPadY + tabCp;
        float tabIH = tabContH - tabCp * 2;
        for (int i = 0; i < tabs.length; i++) {
            if (GuiUtility.isHovered(tabX, tabIY, tabWidths[i], tabIH, localWinMouseX, localWinMouseY)) {
                setCurrentTab.accept(tabs[i]);
                scroll.scrollTarget = 0.0F;
                ClientSoundManager.getInstance().playButtonClick();
                return;
            }
            tabX += tabWidths[i] + tabGap;
        }

        // Search
        float searchX = winW - topPadX - searchW;
        float searchY = topPadY + (tabContH - searchH) / 2.0F;
        if (searchEditor != null) {
            if (searchEditor.mouseClicked(localWinMouseX, localWinMouseY, button, searchX, searchY, searchW, searchH, searchFont)) {
                return;
            }
        }

        float contentX = contentPad;
        float contentY = topH + contentPad;
        float contentW = winW - contentPad * 2;
        float contentH = winH - topH - contentPad * 2;

        // Кнопки сортировки
        float sortChipHeight = WindowPanel.sortChipsHeight(currentTab, searchQuery);
        if (sortChipHeight > 0.0F) {
            float sortY = contentY + 6.0F - scroll.scrollOffset;
            if (localWinMouseY >= contentY && localWinMouseY <= contentY + contentH
                    && localWinMouseY >= sortY && localWinMouseY <= sortY + WindowPanel.SORT_ROW_H) {
                int hit = WindowPanel.hitTestSortChips(currentTab, searchQuery,
                        contentX + 6.0F, sortY, localWinMouseX, localWinMouseY);
                if (hit != -1) {
                    ClientSoundManager.getInstance().playButtonClick();
                    if (currentTab == MenuTab.VISUALS) {
                        setVisualsFilter.accept(NewScreen.VisualsFilter.values()[hit]);
                    } else if (currentTab == MenuTab.UTILITY) {
                        setUtilityFilter.accept(NewScreen.UtilityFilter.values()[hit]);
                    } else if (currentTab == MenuTab.OPTIMIZATION) {
                        naryn.sun.systems.modules.modules.optimization.OptimizationPresets.apply(
                                naryn.sun.systems.modules.modules.optimization.OptimizationPreset.values()[hit]
                        );
                    }
                    scroll.scrollTarget = 0.0F;
                    return;
                }
            }
        }

        List<NewModuleCard> visible = getVisibleCards.get();
        float totalH = WindowPanel.computeTotalHeight(visible, gridGap, sortChipHeight);
        float maxScroll = Math.max(0.0F, totalH - contentH);

        // Скроллбар
        if (maxScroll > 0.0F) {
            float scrollbarW = 3.5F;
            float scrollbarX = contentX + contentW - scrollbarW;
            float scrollbarY = contentY;
            float scrollbarH = contentH;
            float thumbH = Math.max(24.0F, (contentH / totalH) * scrollbarH);
            float thumbY = scrollbarY + (scroll.scrollOffset / maxScroll) * (scrollbarH - thumbH);

            if (GuiUtility.isHovered(scrollbarX - 4.0F, scrollbarY, scrollbarW + 8.0F, scrollbarH, localWinMouseX, localWinMouseY)) {
                if (localWinMouseY >= thumbY && localWinMouseY <= thumbY + thumbH) {
                    scroll.scrollbarDragging = true;
                    scroll.scrollbarDragOffset = localWinMouseY - thumbY;
                } else {
                    scroll.scrollbarDragging = true;
                    scroll.scrollbarDragOffset = thumbH / 2.0F;
                    float newThumbY = localWinMouseY - scroll.scrollbarDragOffset;
                    float progress = Math.max(0.0F, Math.min(1.0F, (newThumbY - scrollbarY) / (scrollbarH - thumbH)));
                    scroll.scrollTarget = progress * maxScroll;
                    scroll.scrollOffset = scroll.scrollTarget;
                }
                return;
            }
        }

        // Карточки
        if (GuiUtility.isHovered(contentX, contentY, contentW, contentH, localWinMouseX, localWinMouseY)) {
            for (NewModuleCard card : visible) {
                if (card.getY() + card.getHeight() >= contentY && card.getY() <= contentY + contentH) {
                    card.onMouseClicked(localWinMouseX, localWinMouseY, button);
                }
            }
        }
    }

    /**
     * Обработка keyPressed для NewScreen. Возвращает true, если событие полностью
     * обработано (NewScreen должен вернуть true и НЕ звать super.keyPressed) и
     * false, если нужно продолжить в NewScreen штатным путём (карточки/color picker'ы
     * уже обработаны здесь, но next-step — вызов super.keyPressed остаётся за NewScreen,
     * т.к. super доступен только из самого подкласса).
     */
    public boolean keyPressed(int keyCode, int scanCode, int modifiers,
                              java.util.function.Supplier<List<NewModuleCard>> getVisibleCards,
                              boolean guiSettingsVisible, Runnable clearGuiSettingsVisible,
                              boolean layoutEditMode, MenuLayoutManager layoutManager, Runnable clearLayoutEditMode,
                              boolean moduleEditMode, HudModuleLayoutManager moduleLayoutManager,
                              java.util.function.Supplier<List<PositionableHudModule>> allPositionableModules,
                              Runnable clearModuleEditMode,
                              long openedAtMs, Runnable setClosing,
                              SearchEditor searchEditor,
                              List<ColorPicker> colorPickers) {

        for (int i = colorPickers.size() - 1; i >= 0; i--) {
            ColorPicker p = colorPickers.get(i);
            if (p.isShowing()) {
                if (p.isInputFocused()) {
                    p.onKeyPressed(keyCode, scanCode, modifiers);
                    return true;
                }
                if (keyCode == 256) {
                    p.setShowing(false);
                    return true;
                }
            }
        }

        for (NewModuleCard c : getVisibleCards.get()) {
            if (c.isBindingMode()) {
                c.onKeyPressed(keyCode, scanCode, modifiers);
                return true;
            }
        }
        if (guiSettingsVisible && keyCode == 256) {
            clearGuiSettingsVisible.run();
            return true;
        }
        if (guiSettingsVisible) {
            GuiSettingsPanel.handleKeyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        if (layoutEditMode) {
            if (keyCode == 256) {
                layoutManager.save();
                clearLayoutEditMode.run();
            }
            return true;
        }
        if (moduleEditMode) {
            if (keyCode == 256) {
                moduleLayoutManager.saveAll(allPositionableModules.get());
                clearModuleEditMode.run();
                PositionableHudModule.setEditingActive(false);
            }
            return true;
        }
        int menuKey = Sun.getInstance().getModuleManager().getModule("Menu").getKey();
        boolean menuKeyPressed = menuKey != -1 && keyCode == menuKey
                && (System.currentTimeMillis() - openedAtMs) > NewScreen.MENU_KEY_CLOSE_DEBOUNCE_MS;
        if (keyCode == 256 || menuKeyPressed) {
            setClosing.run();
            Sun.getInstance().getModuleManager().getModule("Menu").disable();
            return true;
        }
        if (searchEditor != null && searchEditor.isFocused()) {
            if (searchEditor.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        for (NewModuleCard c : getVisibleCards.get()) c.onKeyPressed(keyCode, scanCode, modifiers);
        for (ColorPicker p : colorPickers) p.onKeyPressed(keyCode, scanCode, modifiers);
        return false;
    }

    /**
     * Обработка charTyped для NewScreen. Семантика возврата — как у keyPressed выше.
     */
    public boolean charTyped(char chr, int modifiers,
                             boolean layoutEditMode, boolean moduleEditMode,
                             boolean guiSettingsVisible,
                             SearchEditor searchEditor,
                             java.util.function.Supplier<List<NewModuleCard>> getVisibleCards,
                             List<ColorPicker> colorPickers) {

        for (int i = colorPickers.size() - 1; i >= 0; i--) {
            ColorPicker p = colorPickers.get(i);
            if (p.isShowing() && p.isInputFocused()) {
                if (p.charTyped(chr, modifiers)) {
                    return true;
                }
            }
        }

        if (layoutEditMode || moduleEditMode) {
            return true;
        }
        if (guiSettingsVisible) {
            GuiSettingsPanel.handleCharTyped(chr, modifiers);
            return true;
        }
        if (searchEditor != null && searchEditor.isFocused()) {
            if (searchEditor.charTyped(chr, modifiers)) {
                return true;
            }
        }
        for (NewModuleCard c : getVisibleCards.get()) c.charTyped(chr, modifiers);
        for (ColorPicker p : colorPickers) p.charTyped(chr, modifiers);
        return false;
    }

    private void handleGuiSettingsClick(float localWinMouseX, float localWinMouseY, MouseButton button,
                                        NewScreenScrollState scroll,
                                        float winW, float winH, float topH, float contentPad,
                                        MenuLayoutData layout) {
        float panelX = contentPad;
        float panelY = contentPad + 4.0F;
        float panelW = winW - contentPad * 2.0F;
        float panelH = winH - topH - contentPad * 2.0F + 16.0F;
        float totalH = GuiSettingsPanel.getTotalHeight();
        float maxScroll = Math.max(0.0F, totalH - panelH);

        if (maxScroll > 0.0F) {
            float scrollbarW = 3.5F;
            float scrollbarX = panelX + panelW - scrollbarW;
            float thumbH = Math.max(24.0F, (panelH / totalH) * panelH);
            float thumbY = panelY + (scroll.guiSettingsScrollOffset / maxScroll) * (panelH - thumbH);
            if (GuiUtility.isHovered(scrollbarX - 4.0F, panelY, scrollbarW + 8.0F, panelH, localWinMouseX, localWinMouseY)) {
                scroll.guiSettingsScrollDragging = true;
                scroll.guiSettingsScrollDragOffset = (localWinMouseY >= thumbY && localWinMouseY <= thumbY + thumbH)
                        ? (localWinMouseY - thumbY) : (thumbH / 2.0F);
                return;
            }
        }

        if (GuiUtility.isHovered(panelX, panelY, panelW, panelH, localWinMouseX, localWinMouseY)) {
            GuiSettingsPanel.handleMouseClicked(panelX, panelY - scroll.guiSettingsScrollOffset, panelW,
                    localWinMouseX, localWinMouseY, button);
        }
    }

    private void handleEditorClick(double mouseX, double mouseY, MouseButton button,
                                   MenuLayoutManager layoutManager, LayoutEditor layoutEditor,
                                   MenuLayoutData layout, Runnable clearLayoutEditMode, int screenWidth) {
        if (EditorToolbar.onMouseClicked(mouseX, mouseY, button, (float) screenWidth)) return;
        if (EditorToolbar.isResetHovered(mouseX, mouseY, (float) screenWidth)) {
            layoutManager.resetToDefault();
            ClientSoundManager.getInstance().playButtonClick();
            return;
        }
        if (EditorToolbar.isDoneHovered(mouseX, mouseY, (float) screenWidth)) {
            layoutManager.save();
            clearLayoutEditMode.run();
            EditorToolbar.resetState();
            ClientSoundManager.getInstance().playButtonClick();
            return;
        }
        layoutEditor.onMouseClicked(mouseX, mouseY, button, (float) screenWidth, 0);
    }

    private void handleModuleEditorClick(double mouseX, double mouseY, MouseButton button,
                                         HudModuleLayoutManager moduleLayoutManager,
                                         HudModuleEditor moduleLayoutEditor,
                                         java.util.function.Supplier<List<PositionableHudModule>> allPositionableModules,
                                         Runnable clearModuleEditMode,
                                         int screenWidth) {
        if (EditorToolbar.onMouseClicked(mouseX, mouseY, button, (float) screenWidth)) return;
        if (EditorToolbar.isResetHovered(mouseX, mouseY, (float) screenWidth)) {
            moduleLayoutManager.resetAll(allPositionableModules.get());
            ClientSoundManager.getInstance().playButtonClick();
            return;
        }
        if (EditorToolbar.isDoneHovered(mouseX, mouseY, (float) screenWidth)) {
            moduleLayoutManager.saveAll(allPositionableModules.get());
            clearModuleEditMode.run();
            PositionableHudModule.setEditingActive(false);
            EditorToolbar.resetState();
            ClientSoundManager.getInstance().playButtonClick();
            return;
        }
        moduleLayoutEditor.onMouseClicked(mouseX, mouseY, button);
    }
}