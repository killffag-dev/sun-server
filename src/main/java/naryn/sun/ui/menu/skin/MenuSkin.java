package naryn.sun.ui.menu.skin;

import naryn.sun.framework.base.UIContext;
import naryn.sun.framework.objects.BorderRadius;
import naryn.sun.systems.theme.ClientAppearance;
import naryn.sun.utility.colors.ColorRGBA;
import naryn.sun.utility.colors.Colors;

/**
 * Набор визуальных параметров (блюр/фон/рамка) для панелей ClickGUI
 * (окно, нижний островок, тулбар редактора, GUI Settings).
 */
public interface MenuSkin {

    MenuSkin DARK = new FacetDarkSkin();
    MenuSkin FROST = new FacetFrostSkin();

    static MenuSkin current() {
        return ClientAppearance.isFrost() ? FROST : DARK;
    }

    /** Радиус блюра фона панели (drawBlurredRect). Игнорируется, если neumorphic() == true. */
    float blurRadius();

    /** Цвет+альфа блюр-подсветки (обычно WHITE с небольшой альфой). Игнорируется, если neumorphic() == true. */
    ColorRGBA blurTint(float alpha);

    /** Заливка панели (окно, нижний островок, тулбар). */
    ColorRGBA background(float alpha);

    /** Рамка панели. */
    ColorRGBA border(float alpha);

    /** Фон строки внутри панели (например, строки в GUI Settings). */
    ColorRGBA rowBackground(float alpha);

    /** Подсветка при наведении на кликабельный элемент. */
    ColorRGBA hoverBackground(float alpha);

    // ================= Неоморфизм =================

    /**
     * true -> renderPanelShell()/renderRecessedChip() рисуют двойную мягкую
     * тень (soft-UI) вместо блюра+рамки.
     */
    default boolean neumorphic() {
        return false;
    }

    /** Мягкая светлая тень (имитация источника света сверху-слева) для "приподнятых" поверхностей. */
    default ColorRGBA neumorphLight(float alpha) {
        return ColorRGBA.WHITE.withAlpha(0.0F);
    }

    /** Мягкая тёмная тень (противоположный угол) — даёт эффект "отрыва" панели от игрового мира. */
    default ColorRGBA neumorphDark(float alpha) {
        return ColorRGBA.BLACK.withAlpha(0.0F);
    }

    /** Мягкость (blur) неоморфных теней, передаётся в drawShadow(). */
    default float neumorphSoftness() {
        return 22.0F;
    }

    /** Смещение каждой из двух теней относительно формы, в пикселях. */
    default float neumorphOffset() {
        return 7.0F;
    }

    /** Заливка "вдавленной" внутренней поверхности (контейнер табов/поиск) — темнее, чем background(). */
    default ColorRGBA recessedBackground(float alpha) {
        return rowBackground(alpha);
    }

    /**
     * Оболочка панели верхнего уровня.
     */
    default void renderPanelShell(UIContext context, float x, float y, float w, float h,
                                   BorderRadius radius, float alpha) {
        float darkness = ClientAppearance.getMenuBackgroundDarkness();
        if (darkness > 0.005F) {
            context.drawRoundedRect(x, y, w, h, radius,
                    new ColorRGBA(8, 10, 18, (int) (darkness * 255.0F * alpha)));
        }
        if (neumorphic()) {
            float off = neumorphOffset();
            float soft = neumorphSoftness();
            context.drawShadow(x + off, y + off, w, h, soft, radius, neumorphDark(alpha));
            context.drawShadow(x - off, y - off, w, h, soft, radius, neumorphLight(alpha));
            context.drawRoundedRect(x, y, w, h, radius, background(alpha));
            context.drawRoundedBorder(x, y, w, h, 0.5F, radius, border(alpha));
        } else {
            context.drawBlurredRect(x, y, w, h, blurRadius(), radius, blurTint(alpha));
            context.drawRoundedRect(x, y, w, h, radius, background(alpha));
            context.drawRoundedBorder(x, y, w, h, 0.5F, radius, border(alpha));
        }
    }

    /**
     * "Вдавленная" внутренняя поверхность.
     */
    default void renderRecessedChip(UIContext context, float x, float y, float w, float h,
                                     BorderRadius radius, float alpha) {
        if (neumorphic()) {
            float off = neumorphOffset() * 0.5F;
            float soft = neumorphSoftness() * 0.6F;
            context.drawShadow(x - off, y - off, w, h, soft, radius, neumorphDark(alpha));
            context.drawRoundedRect(x, y, w, h, radius, recessedBackground(alpha));
            context.drawShadow(x + off, y + off, w, h, soft, radius, neumorphLight(alpha * 0.6F));
            context.drawRoundedRect(x, y, w, h, radius, recessedBackground(alpha));
            context.drawRoundedBorder(x, y, w, h, 0.5F, radius, border(alpha * 0.5F));
        } else {
            context.drawRoundedRect(x, y, w, h, radius, recessedBackground(alpha));
        }
    }

    /**
     * Карточка модуля.
     */
    default void renderCard(UIContext context, float x, float y, float w, float h,
                            BorderRadius radius, boolean enabled, float enableAnim, float hoverAnim, float alpha) {
        if (hoverAnim > 0.01F && naryn.sun.systems.animation.ClientAnimationConfig.getInstance().isButtonHoverLift()) {
            context.drawShadow(x, y + 2.0F, w, h, 8.0F * hoverAnim, radius, new ColorRGBA(0, 0, 0, (int)(45 * hoverAnim * alpha)));
        }
        ColorRGBA cardBg = new ColorRGBA(255, 255, 255, (int)((4 + 4 * hoverAnim) * alpha));
        context.drawRoundedRect(x, y, w, h, radius, cardBg);
        ColorRGBA borderColor = Colors.ACCENT.withAlpha((int)((8 + 55 * hoverAnim * enableAnim) * alpha));
        context.drawRoundedBorder(x, y, w, h, 0.5F, radius, borderColor);
    }

    /**
     * Цвет текста таба (с анимацией активного состояния).
     */
    default ColorRGBA tabTextColor(float anim, float alpha) {
        return new ColorRGBA(130, 145, 165, 255).mix(ColorRGBA.WHITE, anim).withAlpha((int) (255 * alpha));
    }

    /**
     * Активный таб.
     */
    default void renderActiveTab(UIContext context, float x, float y, float w, float h,
                                 BorderRadius radius, float anim, float alpha) {
        context.drawRoundedRect(x, y, w, h, radius, Colors.ACCENT.withAlpha((int) (51 * anim * alpha)));
        context.drawRoundedBorder(x, y, w, h, 0.5F, radius, Colors.ACCENT.withAlpha((int) (51 * anim * alpha)));
    }

    // ================= Контролы =================

    default ColorRGBA toggleAccent(float alpha) {
        return Colors.ACCENT.withAlpha((int) (255 * alpha));
    }

    default void renderToggleTrack(UIContext context,
                                    float x, float y, float w, float h, BorderRadius r,
                                    boolean enabled, float enableAnim, float alpha) {
        ColorRGBA offBg = rowBackground(alpha);
        ColorRGBA onBg = toggleAccent(alpha);
        ColorRGBA trackColor = offBg.mix(onBg, enableAnim);
        context.drawRoundedRect(x, y, w, h, r, trackColor);
    }

    default void renderToggleThumb(UIContext context,
                                    float tx, float ty, float size,
                                    float enableAnim, float thumbAlpha) {
        renderToggleThumb(context, tx, ty, size, size, enableAnim, thumbAlpha);
    }

    default void renderToggleThumb(UIContext context,
                                    float tx, float ty, float w, float h,
                                    float enableAnim, float thumbAlpha) {
        context.drawRoundedRect(tx, ty, w, h, BorderRadius.all(h / 2.0F),
                new ColorRGBA(255, 255, 255, (int) (220 * thumbAlpha)));
    }

    default void renderSliderTrack(UIContext context,
                                    float x, float y, float w, float h, BorderRadius r,
                                    float alpha) {
        context.drawRoundedRect(x, y, w, h, r,
                rowBackground(alpha).withAlpha((int) (178 * alpha)));
    }

    default void renderSliderFill(UIContext context,
                                   float x, float y, float w, float h, BorderRadius r,
                                   ColorRGBA accent, float alpha) {
        context.drawRoundedRect(x, y, w, h, r, accent.withAlpha((int) (255 * alpha)));
    }

    default void renderSliderThumb(UIContext context,
                                    float cx, float cy, float w, float h,
                                    float movingAnim, float alpha) {
        float tx = cx - w / 2.0F;
        float ty = cy - h / 2.0F;
        context.drawRoundedRect(tx, ty, w, h, BorderRadius.all(h / 2.0F),
                new ColorRGBA(255, 255, 255, (int) (220 * alpha)));
    }

    default void renderBindChip(UIContext context,
                                 float x, float y, float w, float h, BorderRadius r,
                                 float alpha) {
        context.drawRoundedRect(x, y, w, h, r, rowBackground(alpha));
    }

    default void renderBindPill(UIContext context,
                                float x, float y, float w, float h, BorderRadius r,
                                boolean binding, float alpha) {
        if (binding) {
            context.drawRoundedRect(x, y, w, h, r, Colors.ACCENT.withAlpha((int) (60 * alpha)));
            context.drawRoundedBorder(x, y, w, h, 0.5F, r, Colors.ACCENT.withAlpha((int) (100 * alpha)));
        } else {
            renderBindChip(context, x, y, w, h, r, alpha);
        }
    }

    default void renderSettingBox(UIContext context,
                                  float x, float y, float w, float h, BorderRadius r,
                                  float alpha) {
        context.drawRoundedRect(x, y, w, h, r, Colors.getBackgroundColor().withAlpha(76.5F * alpha));
    }

    default void renderButtonBox(UIContext context,
                                 float x, float y, float w, float h, BorderRadius r,
                                 float hoverAnim, float alpha) {
        context.drawRoundedRect(x, y, w, h, r,
                Colors.getBackgroundColor().withAlpha(255.0F * (0.3F + 0.2F * hoverAnim) * alpha));
    }

    default void renderSettingRow(UIContext context,
                                  float x, float y, float w, float h, BorderRadius r,
                                  float alpha) {
        context.drawRoundedRect(x, y, w, h, r, rowBackground(alpha));
    }

    default void renderColorSwatch(UIContext context,
                                   float x, float y, float w, float h, BorderRadius r,
                                   ColorRGBA color, float alpha) {
        context.drawRoundedRect(x, y, w, h, r, color.withAlpha((int) (255 * alpha)));
        context.drawRoundedBorder(x, y, w, h, 0.5F, r, ColorRGBA.WHITE.withAlpha((int) (70 * alpha)));
    }

    default void renderSearchFocus(UIContext context,
                                   float x, float y, float w, float h, BorderRadius r,
                                   float alpha) {
        context.drawRoundedBorder(x, y, w, h, 0.5F, r, Colors.ACCENT.withAlpha((int) (160 * alpha)));
    }
}