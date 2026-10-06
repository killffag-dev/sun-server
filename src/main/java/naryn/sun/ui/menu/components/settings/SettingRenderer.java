package naryn.sun.ui.menu.components.settings;

import naryn.sun.framework.base.UIContext;
import naryn.sun.framework.msdf.Font;
import naryn.sun.framework.msdf.Fonts;
import naryn.sun.framework.objects.BorderRadius;
import naryn.sun.framework.objects.MouseButton;
import naryn.sun.systems.animation.ClientAnimationConfig;
import naryn.sun.systems.localization.Localizator;
import naryn.sun.systems.theme.ClientAppearance;
import naryn.sun.ui.menu.skin.MenuSkin;
import naryn.sun.utility.animation.base.Animation;
import naryn.sun.utility.animation.base.Easing;
import naryn.sun.utility.animation.types.ColorAnimation;
import naryn.sun.utility.colors.ColorRGBA;
import naryn.sun.utility.colors.Colors;
import naryn.sun.utility.game.cursor.CursorType;
import naryn.sun.utility.game.cursor.CursorUtility;
import naryn.sun.utility.gui.GuiUtility;

import java.util.HashMap;
import java.util.Map;

/**
 * Базовый класс для рендереров настроек GuiSettingCard.
 * Содержит общие методы отрисовки toggle/slider/mode-box,
 * используемые всеми типами настроек.
 */
public abstract class SettingRenderer {

    protected static final float TOGGLE_ROW_H  = 18.0F;
    protected static final float SLIDER_ROW_H  = 29.0F;
    protected static final float BUTTON_ROW_H  = 24.0F;
    protected static final float SETTINGS_SEP_Y = 7.0F;
    protected static final float CARD_BOTTOM_PAD = 10.0F;
    protected static final float MODE_BOX_TITLE_H = 17.0F;
    protected static final float MODE_BOX_PAD     = 8.0F;
    protected static final float MODE_ROW_H       = 19.0F;

    // Анимации для toggle
    protected final Map<String, Animation> toggleAnimMap = new HashMap<>();
    protected final Map<String, Animation> toggleOpacityMap = new HashMap<>();
    protected final Map<String, ColorAnimation> toggleColorMap = new HashMap<>();

    // Анимации для slider
    protected final Map<String, Animation> sliderValAnimMap = new HashMap<>();
    protected final Map<String, Animation> sliderMovingMap = new HashMap<>();
    protected final Map<String, Animation> rowHoverMap = new HashMap<>();

    protected Animation getToggleAnim(String key, boolean initial) {
        return toggleAnimMap.computeIfAbsent(key, k -> new Animation(300L, initial ? 1.0F : 0.0F, Easing.BAKEK));
    }

    protected Animation getToggleOpacity(String key, boolean initial) {
        return toggleOpacityMap.computeIfAbsent(key, k -> new Animation(300L, initial ? 1.0F : 0.75F, Easing.FIGMA_EASE_IN_OUT));
    }

    protected ColorAnimation getToggleColor(String key, boolean initial) {
        return toggleColorMap.computeIfAbsent(key, k -> new ColorAnimation(300L,
                initial ? new ColorRGBA(151.0F, 71.0F, 255.0F) : new ColorRGBA(24.0F, 24.0F, 27.0F),
                Easing.FIGMA_EASE_IN_OUT));
    }

    protected Animation getSliderValAnim(String key, float initialVal) {
        return sliderValAnimMap.computeIfAbsent(key, k -> new Animation(500L, initialVal, Easing.BAKEK_PAGES));
    }

    protected Animation getSliderMovingAnim(String key) {
        return sliderMovingMap.computeIfAbsent(key, k -> new Animation(200L, 0.0F, Easing.FIGMA_EASE_IN_OUT));
    }

    protected Animation getRowHoverAnim(String key) {
        return rowHoverMap.computeIfAbsent(key, k -> new Animation(300L, 0.0F, Easing.FIGMA_EASE_IN_OUT));
    }

    protected static float getModeBoxHeight(int itemsCount) {
        return MODE_BOX_TITLE_H + MODE_BOX_PAD + itemsCount * MODE_ROW_H;
    }

    /**
     * Отрисовка toggle-переключателя (BooleanSettingComponent style).
     */
    protected void renderModulesToggle(UIContext context, float x, float y, float width,
                                       float rowY, String labelKey, boolean enabled, String animKey) {
        Animation enableAnim = getToggleAnim(animKey, enabled);
        enableAnim.update(enabled ? 1.0F : 0.0F);
        Animation circleOpacity = getToggleOpacity(animKey, enabled);
        circleOpacity.update(enabled ? 1.0F : 0.75F);

        boolean hovered = GuiUtility.isHovered(x, rowY, width, TOGGLE_ROW_H, context.getMouseX(), context.getMouseY());
        if (hovered) {
            CursorUtility.set(CursorType.HAND);
        }

        Font nameFont = Fonts.REGULAR.getFont(8.0F);
        float lineY = rowY + (TOGGLE_ROW_H - nameFont.height()) / 2.0F;
        ColorRGBA textColor = Colors.getTextColor().withAlpha(255.0F * (0.75F + 0.25F * enableAnim.getValue()));
        context.drawText(nameFont, Localizator.translate(labelKey), x + 10.0F, lineY, textColor);

        MenuSkin skin = MenuSkin.current();
        float checkW = 13.0F;
        float checkH = 8.0F;
        float trackX = x + width - checkW - 12.0F;
        float trackY = rowY + (TOGGLE_ROW_H - checkH) / 2.0F;
        float eVal = enableAnim.getValue();
        skin.renderToggleTrack(context, trackX, trackY, checkW, checkH,
                BorderRadius.all(3.0F), enabled, eVal, 1.0F);

        float stretch = ClientAnimationConfig.getInstance().isTogglePhysics()
                ? (float) Math.sin(eVal * Math.PI) * 1.5F : 0.0F;
        float thumbSize = 6.0F;
        float thumbW = thumbSize + stretch;
        float thumbX = trackX + 1.0F + 5.0F * eVal - (stretch * (eVal > 0.5F ? 0.7F : 0.3F));
        float thumbY = trackY + 1.0F;
        skin.renderToggleThumb(context, thumbX, thumbY, thumbW, thumbSize,
                eVal, circleOpacity.getValue());
    }

    /**
     * Отрисовка слайдера (SliderSettingComponent style).
     */
    protected void renderModulesSlider(UIContext context, float x, float y, float width,
                                       float rowY, String label, float currentValue,
                                       float min, float max, String animKey, boolean isDragging) {
        float innerX = x + 9.0F;
        float innerW = width - 18.0F;
        Font nameFont = Fonts.REGULAR.getFont(8.0F);
        Font valueFont = Fonts.REGULAR.getFont(7.0F);
        float leftPadding = 10.0F;

        Animation valAnim = getSliderValAnim(animKey, currentValue);
        valAnim.update(currentValue);

        Animation moving = getSliderMovingAnim(animKey);
        moving.setDuration(200L);
        moving.update(isDragging ? 1.0F : 0.0F);

        boolean hovered = GuiUtility.isHovered(x, rowY, width, SLIDER_ROW_H, context.getMouseX(), context.getMouseY());
        Animation rHover = getRowHoverAnim(animKey);
        rHover.update(hovered);
        if (hovered) {
            CursorUtility.set(CursorType.HAND);
        }

        MenuSkin skin = MenuSkin.current();
        float trackY = rowY + SLIDER_ROW_H - 12.0F;
        float trackH = 2.0F;
        float fillW = innerW * GuiUtility.getPercent(valAnim.getValue(), min, max);

        skin.renderSliderTrack(context, innerX, trackY, innerW, trackH, BorderRadius.all(0.25F), 1.0F);
        skin.renderSliderFill(context, innerX, trackY, fillW, trackH, BorderRadius.all(0.25F), Colors.ACCENT, 1.0F);

        float thumbCX = innerX + fillW;
        float thumbCY = rowY + SLIDER_ROW_H - 11.0F;
        float thumbW = 9.0F + 6.0F * moving.getValue();
        float thumbH = 6.0F + 4.0F * moving.getValue();
        skin.renderSliderThumb(context, thumbCX, thumbCY, thumbW, thumbH, moving.getValue(), 1.0F);

        String name = Localizator.translate(label);
        String valText = Math.round(currentValue * 100.0F) + "%";
        ColorRGBA textColor = Colors.getTextColor().withAlpha(255.0F * (0.75F + 0.25F * rHover.getValue()));

        context.drawText(nameFont, name, x + leftPadding, rowY + 11.0F - nameFont.height(), textColor);
        context.drawRightText(valueFont, valText, innerX + innerW, rowY + 11.0F - valueFont.height(), textColor);
    }

    /** Рендерит секцию настроек. */
    public abstract void render(UIContext context, float x, float y, float width, float settY, float expand);

    /** Обрабатывает клик по секции настроек. */
    public abstract void handleClick(float x, float y, float width, float settY, double mouseX, double mouseY);

    /** Возвращает высоту содержимого секции. */
    public abstract float getContentHeight();

    /** Обрабатывает перетаскивание мыши (для слайдеров). */
    public void onMouseDragged(float x, float width, double mouseX, double mouseY) {}

    /** Обрабатывает отпускание мыши. */
    public void onMouseReleased(double mouseX, double mouseY, MouseButton button) {}

    /** Выполняется ли перетаскивание. */
    public boolean isDragging() { return false; }

    /** Обрабатывает нажатие клавиши (для текстовых полей внутри секции). По умолчанию — ничего. */
    public void onKeyPressed(int keyCode, int scanCode, int modifiers) {}

    /** Обрабатывает ввод символа (для текстовых полей внутри секции). По умолчанию — не обработано. */
    public boolean charTyped(char chr, int modifiers) { return false; }
}