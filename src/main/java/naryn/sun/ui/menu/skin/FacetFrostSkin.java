package naryn.sun.ui.menu.skin;

import naryn.sun.framework.base.UIContext;
import naryn.sun.framework.objects.BorderRadius;
import naryn.sun.systems.theme.ClientAppearance;
import naryn.sun.utility.colors.ColorRGBA;
import naryn.sun.utility.colors.Colors;

/**
 * FACET_FROST ("Стекло") — Чистый нейтральный Glassmorphism без белой заливки и лишних цветов.
 */
final class FacetFrostSkin implements MenuSkin {

    @Override
    public float blurRadius() {
        return 28.0F;
    }

    @Override
    public ColorRGBA blurTint(float alpha) {
        return ColorRGBA.WHITE.withAlpha((int) (255 * alpha));
    }

    @Override
    public ColorRGBA background(float alpha) {
        float darkness = ClientAppearance.getMenuBackgroundDarkness();
        int baseAlpha = (int) ((25 + darkness * 120) * alpha);
        return new ColorRGBA(8, 10, 14, Math.min(255, baseAlpha));
    }

    @Override
    public ColorRGBA border(float alpha) {
        return ColorRGBA.WHITE.withAlpha(0);
    }

    @Override
    public ColorRGBA rowBackground(float alpha) {
        return new ColorRGBA(255, 255, 255, (int) (8 * alpha));
    }

    @Override
    public ColorRGBA hoverBackground(float alpha) {
        return new ColorRGBA(255, 255, 255, (int) (18 * alpha));
    }

    @Override
    public ColorRGBA recessedBackground(float alpha) {
        return new ColorRGBA(0, 0, 0, (int) (40 * alpha));
    }

    @Override
    public ColorRGBA tabTextColor(float anim, float alpha) {
        return new ColorRGBA(120, 135, 155, 255).mix(Colors.ACCENT, anim).withAlpha((int) (255 * alpha));
    }

    @Override
    public void renderPanelShell(UIContext context, float x, float y, float w, float h,
                                 BorderRadius radius, float alpha) {
        // 1. Мягкая периметральная тень под панелью (только снаружи, 0% внутри)
        context.drawPerimeterShadow(x, y + 1.0F, w, h, 14.0F, radius,
                ColorRGBA.BLACK.withAlpha((int) (130 * alpha)));

        // 2. Чистый пиксель-в-пиксель Kawase-блюр фона
        context.drawBlurredRect(x, y, w, h, blurRadius(), radius, blurTint(alpha));

        // 3. Нейтральная стеклянная подложка
        context.drawRoundedRect(x, y, w, h, radius, background(alpha));
    }

    @Override
    public void renderRecessedChip(UIContext context, float x, float y, float w, float h,
                                   BorderRadius radius, float alpha) {
        // Контейнер табов и поиск: периметральная тень строго снаружи + тёмная подложка без белого цвета
        context.drawPerimeterShadow(x, y + 1.0F, w, h, 4.0F, radius,
                ColorRGBA.BLACK.withAlpha((int) (70 * alpha)));
        context.drawRoundedRect(x, y, w, h, radius, recessedBackground(alpha));
    }

    @Override
    public void renderCard(UIContext context, float x, float y, float w, float h,
                           BorderRadius radius, boolean enabled, float enableAnim, float hoverAnim, float alpha) {
        // Мягкая периметральная тень (только снаружи бокса, эффект парения)
        float shadowAlpha = (80.0F + 35.0F * hoverAnim) * alpha;
        context.drawPerimeterShadow(x, y + 1.0F, w, h, 6.0F, radius,
                ColorRGBA.BLACK.withAlpha((int) shadowAlpha));

        if (enableAnim > 0.0F) {
            context.drawPerimeterShadow(x, y + 1.0F, w, h, 5.0F, radius,
                    Colors.ACCENT.withAlpha((int) (90 * enableAnim * alpha)));
        }

        // Прозрачная стеклянная карточка (внутри кристально чистая, тень не просвечивает)
        ColorRGBA cardBg = new ColorRGBA(255, 255, 255, (int) ((8 + 6 * hoverAnim) * alpha));
        if (enableAnim > 0.0F) {
            cardBg = cardBg.mix(Colors.ACCENT.withAlpha((int) (25 * alpha)), enableAnim * 0.4F);
        }
        context.drawRoundedRect(x, y, w, h, radius, cardBg);
    }

    @Override
    public void renderActiveTab(UIContext context, float x, float y, float w, float h,
                                BorderRadius radius, float anim, float alpha) {
        if (anim <= 0.0F) return;

        // Без белой заливки и без свечения — чистый нейтральный стеклянный акцент
        context.drawRoundedRect(x, y, w, h, radius,
                Colors.ACCENT.withAlpha((int) (35 * anim * alpha)));
    }

    @Override
    public ColorRGBA toggleAccent(float alpha) {
        return Colors.ACCENT.withAlpha((int) (230 * alpha));
    }

    @Override
    public void renderToggleTrack(UIContext context,
                                   float x, float y, float w, float h, BorderRadius r,
                                   boolean enabled, float enableAnim, float alpha) {
        // Выключенный трек
        if (enableAnim < 1.0F) {
            float offAnim = 1.0F - enableAnim;
            context.drawPerimeterShadow(x, y + 1.0F, w, h, 3.0F, r,
                    ColorRGBA.BLACK.withAlpha((int) (55 * offAnim * alpha)));
            context.drawRoundedRect(x, y, w, h, r,
                    new ColorRGBA(255, 255, 255, (int) (14 * offAnim * alpha)));
        }

        // Включенный трек
        if (enableAnim > 0.0F) {
            context.drawPerimeterShadow(x, y + 1.0F, w, h, 5.0F, r,
                    Colors.ACCENT.withAlpha((int) (130 * enableAnim * alpha)));
            context.drawRoundedRect(x, y, w, h, r,
                    Colors.ACCENT.withAlpha((int) (220 * enableAnim * alpha)));
        }
    }

    @Override
    public void renderToggleThumb(UIContext context,
                                   float tx, float ty, float size,
                                   float enableAnim, float thumbAlpha) {
        BorderRadius r = BorderRadius.all(size / 2.0F);

        context.drawPerimeterShadow(tx, ty + 1.0F, size, size, 3.0F, r,
                ColorRGBA.BLACK.withAlpha((int) (100 * thumbAlpha)));

        if (enableAnim > 0.0F) {
            context.drawPerimeterShadow(tx, ty + 1.0F, size, size, 4.0F, r,
                    Colors.ACCENT.withAlpha((int) (110 * enableAnim * thumbAlpha)));
        }

        context.drawRoundedRect(tx, ty, size, size, r,
                new ColorRGBA(235, 240, 250, (int) (245 * thumbAlpha)));
    }

    @Override
    public void renderSliderTrack(UIContext context,
                                   float x, float y, float w, float h, BorderRadius r,
                                   float alpha) {
        context.drawRoundedRect(x, y, w, h, r,
                new ColorRGBA(255, 255, 255, (int) (12 * alpha)));
    }

    @Override
    public void renderSliderFill(UIContext context,
                                  float x, float y, float w, float h, BorderRadius r,
                                  ColorRGBA accent, float alpha) {
        if (w <= 0.5F) return;

        context.drawPerimeterShadow(x, y, w, h, 4.0F, r,
                accent.withAlpha((int) (110 * alpha)));
        context.drawRoundedRect(x, y, w, h, r,
                accent.withAlpha((int) (230 * alpha)));
    }

    @Override
    public void renderSliderThumb(UIContext context,
                                   float cx, float cy, float w, float h,
                                   float movingAnim, float alpha) {
        float tx = cx - w / 2.0F;
        float ty = cy - h / 2.0F;
        BorderRadius r = BorderRadius.all(h / 2.0F);

        context.drawPerimeterShadow(tx, ty + 1.0F, w, h, 3.0F, r,
                ColorRGBA.BLACK.withAlpha((int) (100 * alpha)));
        if (movingAnim > 0.0F) {
            context.drawPerimeterShadow(tx, ty, w, h, 5.0F, r,
                    Colors.ACCENT.withAlpha((int) (110 * movingAnim * alpha)));
        }

        context.drawRoundedRect(tx, ty, w, h, r,
                new ColorRGBA(235, 240, 250, (int) (245 * alpha)));
    }

    @Override
    public void renderBindChip(UIContext context,
                                float x, float y, float w, float h, BorderRadius r,
                                float alpha) {
        context.drawPerimeterShadow(x, y + 1.0F, w, h, 3.0F, r,
                ColorRGBA.BLACK.withAlpha((int) (60 * alpha)));
        context.drawRoundedRect(x, y, w, h, r,
                new ColorRGBA(255, 255, 255, (int) (12 * alpha)));
    }

    @Override
    public void renderBindPill(UIContext context,
                                float x, float y, float w, float h, BorderRadius r,
                                boolean binding, float alpha) {
        if (binding) {
            context.drawPerimeterShadow(x, y + 1.0F, w, h, 5.0F, r,
                    Colors.ACCENT.withAlpha((int) (110 * alpha)));
            context.drawRoundedRect(x, y, w, h, r,
                    Colors.ACCENT.withAlpha((int) (130 * alpha)));
        } else {
            renderBindChip(context, x, y, w, h, r, alpha);
        }
    }

    @Override
    public void renderSettingBox(UIContext context,
                                  float x, float y, float w, float h, BorderRadius r,
                                  float alpha) {
        // Единый стиль для всех выпадающих списков/инпутов/боксов настроек: строго наружная тень
        context.drawPerimeterShadow(x, y + 1.0F, w, h, 5.0F, r,
                ColorRGBA.BLACK.withAlpha((int) (65 * alpha)));
        context.drawRoundedRect(x, y, w, h, r,
                new ColorRGBA(255, 255, 255, (int) (10 * alpha)));
    }

    @Override
    public void renderButtonBox(UIContext context,
                                 float x, float y, float w, float h, BorderRadius r,
                                 float hoverAnim, float alpha) {
        context.drawPerimeterShadow(x, y + 1.0F, w, h, 5.0F, r,
                ColorRGBA.BLACK.withAlpha((int) ((55 + 25 * hoverAnim) * alpha)));
        context.drawRoundedRect(x, y, w, h, r,
                new ColorRGBA(255, 255, 255, (int) ((12 + 10 * hoverAnim) * alpha)));
    }

    @Override
    public void renderSettingRow(UIContext context,
                                  float x, float y, float w, float h, BorderRadius r,
                                  float alpha) {
        context.drawPerimeterShadow(x, y + 1.0F, w, h, 5.0F, r,
                ColorRGBA.BLACK.withAlpha((int) (60 * alpha)));
        context.drawRoundedRect(x, y, w, h, r,
                new ColorRGBA(255, 255, 255, (int) (9 * alpha)));
    }

    @Override
    public void renderColorSwatch(UIContext context,
                                   float x, float y, float w, float h, BorderRadius r,
                                   ColorRGBA color, float alpha) {
        context.drawPerimeterShadow(x, y + 1.0F, w, h, 3.0F, r,
                ColorRGBA.BLACK.withAlpha((int) (80 * alpha)));
        context.drawRoundedRect(x, y, w, h, r,
                color.withAlpha((int) (255 * alpha)));
    }

    @Override
    public void renderSearchFocus(UIContext context,
                                   float x, float y, float w, float h, BorderRadius r,
                                   float alpha) {
        context.drawPerimeterShadow(x, y + 1.0F, w, h, 5.0F, r,
                Colors.ACCENT.withAlpha((int) (110 * alpha)));
        context.drawRoundedRect(x, y, w, h, r,
                Colors.ACCENT.withAlpha((int) (35 * alpha)));
    }
}