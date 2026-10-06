package naryn.sun.ui.menu.panels;

import naryn.sun.framework.base.UIContext;
import naryn.sun.framework.msdf.Font;
import naryn.sun.framework.msdf.Fonts;
import naryn.sun.framework.objects.BorderRadius;
import naryn.sun.systems.localization.Localizator;
import naryn.sun.systems.theme.ClientAppearance;
import naryn.sun.ui.menu.MenuTab;
import naryn.sun.ui.menu.NewScreen;
import naryn.sun.ui.menu.SearchEditor;
import naryn.sun.ui.menu.components.NewModuleCard;
import naryn.sun.ui.menu.skin.MenuSkin;
import naryn.sun.ui.menu.widgets.SegmentedToggle;
import naryn.sun.utility.animation.base.Animation;
import naryn.sun.utility.colors.ColorRGBA;
import naryn.sun.utility.colors.Colors;
import naryn.sun.utility.game.cursor.CursorType;
import naryn.sun.utility.game.cursor.CursorUtility;
import naryn.sun.utility.gui.GuiUtility;
import naryn.sun.utility.render.ScissorUtility;

import naryn.sun.utility.animation.base.Easing;
import naryn.sun.systems.animation.ClientAnimationConfig;

import java.util.List;
import java.util.Map;

/**
 * Главное окно ClickGUI: рамка, табы категорий (слева), поисковая строка (справа)
 * и сетка карточек модулей с плавным скроллбаром.
 */
public final class WindowPanel {

    private static final Animation tabIndicatorX = new Animation(240L, 0.0F, Easing.QUARTIC_OUT);
    private static final Animation tabIndicatorW = new Animation(240L, 0.0F, Easing.QUARTIC_OUT);
    private static boolean tabIndicatorInit = false;

    private WindowPanel() {
    }

    /** Фон и рамка окна (skin-зависимые). */
    public static void renderFrame(UIContext context, float alpha, float winW, float winH, float winR, float topH, boolean showHeaderDivider) {
        MenuSkin skin = MenuSkin.current();
        skin.renderPanelShell(context, 0.0F, 0.0F, winW, winH, BorderRadius.all(winR), alpha);
        if (showHeaderDivider) {
            context.drawRect(0.0F, topH, winW, 0.5F, new ColorRGBA(255, 255, 255, (int) (14 * alpha)));
        }
    }

    /**
     * Слайдер прокрутки (скроллбар) справа от контента.
     */
    public static void renderScrollbar(UIContext context, float alpha, float mouseX, float mouseY,
                                       float scrollbarX, float scrollbarY, float scrollbarW, float scrollbarH,
                                       float scrollOffset, float maxScroll, float totalH, float contentH,
                                       boolean isDragging) {
        if (maxScroll <= 0.0F) return;

        float thumbH = Math.max(24.0F, (contentH / totalH) * scrollbarH);
        float progress = Math.max(0.0F, Math.min(1.0F, scrollOffset / maxScroll));
        float thumbY = scrollbarY + progress * (scrollbarH - thumbH);

        boolean hovered = GuiUtility.isHovered(scrollbarX - 3.0F, scrollbarY, scrollbarW + 6.0F, scrollbarH, mouseX, mouseY);
        if (hovered || isDragging) {
            CursorUtility.set(CursorType.HAND);
        }

        // Фон дорожки скролла
        context.drawRoundedRect(scrollbarX, scrollbarY, scrollbarW, scrollbarH,
                BorderRadius.all(scrollbarW / 2.0F),
                new ColorRGBA(255, 255, 255, (int) ((hovered || isDragging ? 14 : 7) * alpha)));

        // Ползунок скроллбара
        ColorRGBA thumbColor = isDragging
                ? ColorRGBA.WHITE.withAlpha((int) (230 * alpha))
                : (hovered
                ? ColorRGBA.WHITE.withAlpha((int) (180 * alpha))
                : new ColorRGBA(255, 255, 255, (int) (70 * alpha)));

        context.drawRoundedRect(scrollbarX, thumbY, scrollbarW, thumbH,
                BorderRadius.all(scrollbarW / 2.0F), thumbColor);
    }

    public static float computeTotalTabW(Font tabFont, String[] tabNames, float tabPadX, float tabGap, float tabCp) {
        float totalTabW = tabCp * 2;
        for (int i = 0; i < tabNames.length; i++) {
            if (i > 0) totalTabW += tabGap;
            totalTabW += tabFont.width(tabNames[i]) + tabPadX * 2;
        }
        return totalTabW;
    }

    /** Табы категорий слева + поисковая строка справа. */
    public static void renderHeader(UIContext context, float alpha, float mouseX, float mouseY,
                                     Font tabFont, Font searchFont,
                                     MenuTab currentTab, Map<MenuTab, Animation> tabAnimations, String[] tabNames,
                                     SearchEditor searchEditor,
                                     boolean layoutEditMode, boolean moduleEditMode,
                                     float winW, float topPadX, float topPadY,
                                     float tabPadX, float tabPadY, float tabGap, float tabCp, float tabR,
                                     float searchW, float searchH) {
        MenuSkin skin = MenuSkin.current();

        float tabContX = topPadX;
        float tabContY = topPadY;
        MenuTab[] tabs = MenuTab.values();
        float[] tabWidths = new float[tabs.length];
        float totalTabW = tabCp * 2;
        for (int i = 0; i < tabs.length; i++) {
            tabWidths[i] = tabFont.width(tabNames[i]) + tabPadX * 2;
            if (i > 0) totalTabW += tabGap;
            totalTabW += tabWidths[i];
        }
        float tabContH = tabFont.height() + tabPadY * 2 + tabCp * 2;

        // Вдавленный контейнер табов
        skin.renderRecessedChip(context, tabContX, tabContY, totalTabW, tabContH,
                BorderRadius.all(6.0F), alpha);

        float tabX = tabContX + tabCp;
        float tabIY = tabContY + tabCp;
        float tabIH = tabContH - tabCp * 2;

        float activeTabX = tabX;
        float activeTabW = tabWidths.length > 0 ? tabWidths[0] : 0.0F;

        // Определяем целевую позицию активного таба
        float curScanX = tabX;
        for (int i = 0; i < tabs.length; i++) {
            if (tabs[i] == currentTab) {
                activeTabX = curScanX;
                activeTabW = tabWidths[i];
                break;
            }
            curScanX += tabWidths[i] + tabGap;
        }

        boolean smoothTabs = ClientAnimationConfig.getInstance().isSmoothTabs();
        if (!tabIndicatorInit || !smoothTabs) {
            tabIndicatorX.setValue(activeTabX);
            tabIndicatorW.setValue(activeTabW);
            tabIndicatorInit = true;
        } else {
            tabIndicatorX.update(activeTabX);
            tabIndicatorW.update(activeTabW);
        }

        // Плавная скользящая светящаяся капсула
        skin.renderActiveTab(context, tabIndicatorX.getValue(), tabIY, tabIndicatorW.getValue(), tabIH,
                BorderRadius.all(tabR), 1.0F, alpha);

        for (int i = 0; i < tabs.length; i++) {
            MenuTab t = tabs[i];
            Animation anim = tabAnimations.get(t);
            anim.update(currentTab == t ? 1.0F : 0.0F);
            float animV = anim.getValue();

            ColorRGBA tc = skin.tabTextColor(animV, alpha);
            context.drawText(tabFont, tabNames[i], tabX + tabPadX, tabIY + tabCp + 1.0F, tc);
            if (!layoutEditMode && !moduleEditMode && GuiUtility.isHovered(tabX, tabIY, tabWidths[i], tabIH, mouseX, mouseY)) {
                CursorUtility.set(CursorType.HAND);
            }
            tabX += tabWidths[i] + tabGap;
        }

        // Поисковая строка справа через полноценный SearchEditor
        float searchX = winW - topPadX - searchW;
        float searchY = topPadY + (tabContH - searchH) / 2.0F;

        if (searchEditor != null) {
            searchEditor.render(context, searchX, searchY, searchW, searchH, searchFont, alpha, mouseX, mouseY);
        }
    }

    /**
     * Сетка карточек модулей и привязанный к верхнему краю контента бокс сортировки.
     * Возвращает текст описания наведённой карточки (или "" если наведения нет).
     */
    public static String renderCardGrid(UIContext context, float mouseX, float mouseY,
                                         float contentX, float contentY, float contentW, float contentH,
                                         float gridGap, float scrollOffset,
                                         List<NewModuleCard> visible,
                                         MenuTab currentTab, String searchQuery,
                                         NewScreen.VisualsFilter visualsFilter,
                                         NewScreen.UtilityFilter utilityFilter,
                                         float alpha,
                                         boolean layoutEditMode, boolean moduleEditMode) {
        String tooltip = "";

        ScissorUtility.push(context.getMatrices(), contentX, contentY, contentW, contentH);

        float innerPadX = 6.0F;
        float innerPadY = 6.0F;
        float usableW = contentW - innerPadX * 2;
        float colW = (usableW - gridGap) / 2.0F;
        float col0X = contentX + innerPadX;
        float col1X = col0X + colW + gridGap;

        // Бокс сортировки скроллится вместе со всем списком модулей
        float sortH = sortChipsHeight(currentTab, searchQuery);
        if (sortH > 0.0F) {
            float sortY = contentY + innerPadY - scrollOffset;
            if (sortY + SORT_ROW_H >= contentY - 6.0F && sortY <= contentY + contentH + 6.0F) {
                renderSortChips(context, alpha, mouseX, mouseY,
                        currentTab, searchQuery, visualsFilter, utilityFilter,
                        col0X, sortY, usableW,
                        layoutEditMode, moduleEditMode);
            }
        }

        float startY = contentY + innerPadY + sortH - scrollOffset;
        float[] colY = {startY, startY};

        for (int i = 0; i < visible.size(); i++) {
            NewModuleCard card = visible.get(i);
            int col = i % 2;
            card.setX(col == 0 ? col0X : col1X);
            card.setY(colY[col]);
            card.setWidth(colW);
            if (colY[col] + card.getHeight() >= contentY - 6.0F && colY[col] <= contentY + contentH + 6.0F) {
                card.render(context);
                if (!layoutEditMode && !moduleEditMode) {
                    String cardTooltip = card.getHoveredTooltip(mouseX, mouseY);
                    if (!cardTooltip.isEmpty()) {
                        tooltip = cardTooltip;
                    }
                }
            }
            colY[col] += card.getHeight() + gridGap;
        }
        ScissorUtility.pop();

        return tooltip;
    }

    public static final float SORT_ROW_H = 34.0F;
    public static final float SORT_ROW_GAP = 8.0F;
    private static final float SORT_BTN_H = 16.0F;
    private static final float SORT_BTN_GAP = 6.0F;
    private static final float SORT_START_X_OFFSET = 68.0F;

    /**
     * Вычисляет высоту бокса сортировки/пресетов вместе с отступом до карточек.
     * Если фильтры не применяются (Favorites / Search) — возвращает 0.
     */
    public static float sortChipsHeight(MenuTab tab, String searchQuery) {
        if (!searchQuery.isEmpty() || tab == MenuTab.FAVORITES) return 0.0F;
        if (tab == MenuTab.VISUALS || tab == MenuTab.UTILITY || tab == MenuTab.OPTIMIZATION) return SORT_ROW_H + SORT_ROW_GAP;
        return 0.0F;
    }

    private static String[] getSortLabels(MenuTab tab) {
        if (tab == MenuTab.VISUALS) {
            NewScreen.VisualsFilter[] values = NewScreen.VisualsFilter.values();
            String[] labels = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                labels[i] = values[i].getLabel();
            }
            return labels;
        } else if (tab == MenuTab.UTILITY) {
            NewScreen.UtilityFilter[] values = NewScreen.UtilityFilter.values();
            String[] labels = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                labels[i] = values[i].getLabel();
            }
            return labels;
        } else if (tab == MenuTab.OPTIMIZATION) {
            naryn.sun.systems.modules.modules.optimization.OptimizationPreset[] presets = naryn.sun.systems.modules.modules.optimization.OptimizationPreset.values();
            String[] labels = new String[presets.length];
            for (int i = 0; i < presets.length; i++) {
                labels[i] = presets[i].getLabel();
            }
            return labels;
        }
        return new String[0];
    }

    private static float getSortButtonWidth(String label) {
        return Math.max(44.0F, Fonts.MEDIUM.getFont(6.0F).width(label) + 16.0F);
    }

    /**
     * Рисует длинный бокс сортировки / пресетов над карточками модулей.
     */
    public static void renderSortChips(UIContext context, float alpha, float mouseX, float mouseY,
                                       MenuTab tab, String searchQuery,
                                       NewScreen.VisualsFilter visualsFilter,
                                       NewScreen.UtilityFilter utilityFilter,
                                       float contentX, float contentY, float contentW,
                                       boolean layoutEditMode, boolean moduleEditMode) {
        if (!searchQuery.isEmpty() || tab == MenuTab.FAVORITES) return;
        if (tab != MenuTab.VISUALS && tab != MenuTab.UTILITY && tab != MenuTab.OPTIMIZATION) return;

        MenuSkin skin = MenuSkin.current();
        ColorRGBA accent = ClientAppearance.getAccent();

        // 1. Длинный бокс как в GUI Settings (renderSettingRow)
        skin.renderSettingRow(context, contentX, contentY, contentW, SORT_ROW_H, BorderRadius.all(7.0F), alpha);

        // 2. Текст "Сортировка" или "Пресет" слева
        String title = tab == MenuTab.OPTIMIZATION ? Localizator.translate("modules.settings.optimizer.preset") : Localizator.translate("menu.sort");
        context.drawText(Fonts.MEDIUM.getFont(7.0F), title,
                contentX + 10.0F, contentY + 12.0F, Colors.getTextColor().withAlpha((int) (225 * alpha)));

        // 3. Сегментные кнопки режимов сортировки / пресетов
        String[] labels = getSortLabels(tab);
        int activeIdx;
        if (tab == MenuTab.VISUALS) {
            activeIdx = visualsFilter.ordinal();
        } else if (tab == MenuTab.UTILITY) {
            activeIdx = utilityFilter.ordinal();
        } else {
            activeIdx = naryn.sun.systems.modules.modules.optimization.OptimizationPresets.getCurrentPreset().ordinal();
        }

        float btnY = contentY + 9.0F;
        float btnX = contentX + 14.0F + Fonts.MEDIUM.getFont(7.0F).width(title);

        for (int i = 0; i < labels.length; i++) {
            float btnW = getSortButtonWidth(labels[i]);
            boolean selected = (i == activeIdx);
            boolean hovered = !layoutEditMode && !moduleEditMode && GuiUtility.isHovered(btnX, btnY, btnW, SORT_BTN_H, mouseX, mouseY);

            SegmentedToggle.render(context, Fonts.MEDIUM.getFont(6.0F), labels[i],
                    btnX, btnY, btnW, SORT_BTN_H, selected, hovered, false, btnY + 5.0F,
                    accent, Colors.getTextColor(), alpha);

            btnX += btnW + SORT_BTN_GAP;
        }
    }

    /**
     * Обработка кликов по кнопкам сортировки / пресетов.
     */
    public static int hitTestSortChips(MenuTab tab, String searchQuery,
                                       float contentX, float contentY,
                                       float mouseX, float mouseY) {
        if (!searchQuery.isEmpty() || tab == MenuTab.FAVORITES) return -1;
        if (tab != MenuTab.VISUALS && tab != MenuTab.UTILITY && tab != MenuTab.OPTIMIZATION) return -1;

        String[] labels = getSortLabels(tab);
        float btnY = contentY + 9.0F;
        String title = tab == MenuTab.OPTIMIZATION ? Localizator.translate("modules.settings.optimizer.preset") : Localizator.translate("menu.sort");
        float btnX = contentX + 14.0F + Fonts.MEDIUM.getFont(7.0F).width(title);

        for (int i = 0; i < labels.length; i++) {
            float btnW = getSortButtonWidth(labels[i]);
            if (GuiUtility.isHovered(btnX, btnY, btnW, SORT_BTN_H, mouseX, mouseY)) {
                return i;
            }
            btnX += btnW + SORT_BTN_GAP;
        }
        return -1;
    }

    /** Суммарная высота сетки (для клампа скролла). */
    public static float computeTotalHeight(List<NewModuleCard> cards, float gridGap) {
        return computeTotalHeight(cards, gridGap, 0.0F);
    }

    /** Суммарная высота сетки с учётом сортировки (для клампа скролла). */
    public static float computeTotalHeight(List<NewModuleCard> cards, float gridGap, float sortH) {
        float[] h = {0.0F, 0.0F};
        for (int i = 0; i < cards.size(); i++) {
            h[i % 2] += cards.get(i).getHeight() + gridGap;
        }
        return Math.max(h[0], h[1]) + sortH + 12.0F;
    }
}