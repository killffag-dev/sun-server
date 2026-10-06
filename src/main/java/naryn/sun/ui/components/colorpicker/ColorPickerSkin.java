package naryn.sun.ui.components.colorpicker;

import naryn.sun.framework.base.UIContext;
import naryn.sun.framework.objects.BorderRadius;
import naryn.sun.systems.theme.ClientAppearance;
import naryn.sun.utility.colors.ColorRGBA;
import naryn.sun.utility.colors.Colors;

/**
 * Адаптивный рендерер материалов для ColorPicker:
 * 1. FACET_FROST ("Стекло") — Настоящее матовое стекло с Kawase Blur, наружной тенью
 *    и прозрачной подложкой в тон общему стилю меню (без сплошной белой глины);
 * 2. FACET_DARK ("Тёмный") — Глубокий тёмный неоморфизм с матовыми поверхностями и спекулярными тенями.
 */
public final class ColorPickerSkin {

    public static final float CARD_RADIUS = 14.0F;
    private static final BorderRadius WINDOW_RADIUS = BorderRadius.all(CARD_RADIUS);

    private ColorPickerSkin() {}

    public static boolean isFrost() {
        return ClientAppearance.isFrost();
    }

    /**
     * Отрисовка оболочки окна ColorPicker.
     */
    public static void renderWindowShell(UIContext context, float x, float y, float w, float h, float alpha) {
        if (isFrost()) {
            // Настоящее стекло (Glassmorphism): периметральная наружная тень + Kawase-блюр + прозрачная стеклянная подложка
            context.drawPerimeterShadow(x, y + 2.0F, w, h, 18.0F, WINDOW_RADIUS,
                    ColorRGBA.BLACK.withAlpha((int) (140 * alpha)));
            context.drawBlurredRect(x, y, w, h, 28.0F, WINDOW_RADIUS,
                    ColorRGBA.WHITE.withAlpha((int) (255 * alpha)));
            context.drawRoundedRect(x, y, w, h, WINDOW_RADIUS,
                    new ColorRGBA(10, 12, 16, (int) (38 * alpha)));
            context.drawRoundedBorder(x, y, w, h, 0.5F, WINDOW_RADIUS,
                    ColorRGBA.WHITE.withAlpha((int) (28 * alpha)));
        } else {
            // Тёмный глубокий неоморфизм
            float off = 5.0F;
            float soft = 18.0F;
            context.drawShadow(x + off, y + off + 2.0F, w, h, soft, WINDOW_RADIUS,
                    new ColorRGBA(0, 0, 0, (int) (180 * alpha)));
            context.drawShadow(x - off * 0.6F, y - off * 0.6F, w, h, soft * 0.7F, WINDOW_RADIUS,
                    new ColorRGBA(255, 255, 255, (int) (16 * alpha)));
            context.drawRoundedRect(x, y, w, h, WINDOW_RADIUS,
                    new ColorRGBA(22, 23, 30, (int) (250 * alpha)));
            context.drawRoundedBorder(x, y, w, h, 0.5F, WINDOW_RADIUS,
                    new ColorRGBA(255, 255, 255, (int) (10 * alpha)));
        }
    }

    /**
     * Отрисовка вдавленного поля (инпут значения, кнопка дропдауна).
     */
    public static void renderRecessedWell(UIContext context, float x, float y, float w, float h, BorderRadius radius, float alpha) {
        if (isFrost()) {
            // Стеклянная вдавленная плашка
            context.drawPerimeterShadow(x, y + 1.0F, w, h, 4.0F, radius,
                    ColorRGBA.BLACK.withAlpha((int) (65 * alpha)));
            context.drawRoundedRect(x, y, w, h, radius,
                    new ColorRGBA(0, 0, 0, (int) (45 * alpha)));
            context.drawRoundedBorder(x, y, w, h, 0.5F, radius,
                    ColorRGBA.WHITE.withAlpha((int) (15 * alpha)));
        } else {
            // Тёмный неоморфный инсет
            float off = 1.8F;
            float soft = 4.0F;
            context.drawShadow(x - off, y - off, w, h, soft, radius,
                    new ColorRGBA(0, 0, 0, (int) (165 * alpha)));
            context.drawRoundedRect(x, y, w, h, radius,
                    new ColorRGBA(14, 15, 20, (int) (245 * alpha)));
            context.drawShadow(x + off, y + off, w, h, soft, radius,
                    new ColorRGBA(255, 255, 255, (int) (14 * alpha)));
            context.drawRoundedRect(x, y, w, h, radius,
                    new ColorRGBA(14, 15, 20, (int) (245 * alpha)));
            context.drawRoundedBorder(x, y, w, h, 0.5F, radius,
                    new ColorRGBA(255, 255, 255, (int) (8 * alpha)));
        }
    }

    /**
     * Отрисовка кнопки (пипетка).
     */
    public static void renderRaisedButton(UIContext context, float x, float y, float w, float h,
                                          BorderRadius radius, boolean active, float hover, float alpha) {
        if (active) {
            renderRecessedWell(context, x, y, w, h, radius, alpha);
            context.drawRoundedRect(x, y, w, h, radius, Colors.ACCENT.withAlpha((int) (40 * alpha)));
            context.drawRoundedBorder(x, y, w, h, 0.5F, radius, Colors.ACCENT.withAlpha((int) (180 * alpha)));
            return;
        }

        if (isFrost()) {
            // Стеклянная кнопка
            context.drawPerimeterShadow(x, y + 1.0F, w, h, 5.0F, radius,
                    ColorRGBA.BLACK.withAlpha((int) ((50 + 25 * hover) * alpha)));
            context.drawRoundedRect(x, y, w, h, radius,
                    new ColorRGBA(255, 255, 255, (int) ((12 + 15 * hover) * alpha)));
            context.drawRoundedBorder(x, y, w, h, 0.5F, radius,
                    ColorRGBA.WHITE.withAlpha((int) ((18 + 18 * hover) * alpha)));
        } else {
            // Тёмная неоморфная кнопка
            float off = 2.0F + 0.8F * hover;
            float soft = 5.0F + 2.0F * hover;
            context.drawShadow(x + off, y + off, w, h, soft, radius,
                    new ColorRGBA(0, 0, 0, (int) ((140 + 40 * hover) * alpha)));
            context.drawShadow(x - off, y - off, w, h, soft, radius,
                    new ColorRGBA(255, 255, 255, (int) (18 * alpha)));
            context.drawRoundedRect(x, y, w, h, radius,
                    new ColorRGBA(28, 30, 39, (int) (255 * alpha)));
            context.drawRoundedBorder(x, y, w, h, 0.5F, radius,
                    new ColorRGBA(255, 255, 255, (int) ((12 + 15 * hover) * alpha)));
        }
    }

    /**
     * Отрисовка выпадающей шторки выбора формата (Hex / RGB / HSL).
     */
    public static void renderDropdownCurtain(UIContext context, float x, float y, float w, float h, BorderRadius radius, float alpha) {
        if (isFrost()) {
            context.drawPerimeterShadow(x, y + 2.0F, w, h, 12.0F, radius,
                    ColorRGBA.BLACK.withAlpha((int) (140 * alpha)));
            context.drawBlurredRect(x, y, w, h, 20.0F, radius,
                    ColorRGBA.WHITE.withAlpha((int) (255 * alpha)));
            context.drawRoundedRect(x, y, w, h, radius,
                    new ColorRGBA(14, 16, 22, (int) (230 * alpha)));
            context.drawRoundedBorder(x, y, w, h, 0.5F, radius,
                    ColorRGBA.WHITE.withAlpha((int) (35 * alpha)));
        } else {
            context.drawShadow(x, y + 3.0F, w, h, 14.0F, radius,
                    new ColorRGBA(0, 0, 0, (int) (190 * alpha)));
            context.drawRoundedRect(x, y, w, h, radius,
                    new ColorRGBA(24, 26, 34, (int) (252 * alpha)));
            context.drawRoundedBorder(x, y, w, h, 0.5F, radius,
                    new ColorRGBA(255, 255, 255, (int) (16 * alpha)));
        }
    }

    public static ColorRGBA getTitleColor(float alpha) {
        return new ColorRGBA(245, 246, 250, (int) (245 * alpha));
    }

    public static ColorRGBA getShadeTitleColor(float alpha) {
        return new ColorRGBA(235, 238, 246, (int) (240 * alpha));
    }

    public static ColorRGBA getValueTextColor(float alpha) {
        return new ColorRGBA(240, 242, 248, (int) (240 * alpha));
    }

    public static ColorRGBA getDropdownTextColor(float alpha) {
        return new ColorRGBA(240, 242, 248, (int) (245 * alpha));
    }

    public static ColorRGBA getIconColor(boolean active, boolean hover, float alpha) {
        if (active) {
            return Colors.ACCENT.withAlpha((int) (255 * alpha));
        }
        return hover ? new ColorRGBA(255, 255, 255, (int) (255 * alpha))
                     : new ColorRGBA(215, 220, 232, (int) (220 * alpha));
    }

    public static ColorRGBA getPaletteBorderColor(float alpha) {
        return new ColorRGBA(255, 255, 255, (int) (15 * alpha));
    }
}
