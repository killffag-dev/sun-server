package naryn.sun.ui.menu.skin;

import naryn.sun.framework.base.UIContext;
import naryn.sun.framework.objects.BorderRadius;
import naryn.sun.systems.theme.ClientAppearance;
import naryn.sun.utility.colors.ColorRGBA;
import naryn.sun.utility.colors.Colors;

/**
 * FACET_DARK ("Тёмный") — soft-UI / неоморфизм.
 */
final class FacetDarkSkin implements MenuSkin {

    private static final ColorRGBA SURFACE  = new ColorRGBA(23, 24, 32, 255);
    private static final ColorRGBA RECESSED = new ColorRGBA(15, 16, 22, 255);

    private static final float C_OFF  = 3.0F;
    private static final float C_SOFT = 10.0F;

    @Override
    public float blurRadius() {
        return 14.0F;
    }

    @Override
    public ColorRGBA blurTint(float alpha) {
        return ColorRGBA.WHITE.withAlpha((int) (20 * alpha));
    }

    @Override
    public ColorRGBA background(float alpha) {
        float darkness = ClientAppearance.getMenuBackgroundDarkness();
        float factor = 1.0F - (darkness * 0.65F);
        int r = Math.max(6, (int) (SURFACE.getRed() * factor));
        int g = Math.max(7, (int) (SURFACE.getGreen() * factor));
        int b = Math.max(10, (int) (SURFACE.getBlue() * factor));
        return new ColorRGBA(r, g, b, (int) (246 * alpha));
    }

    @Override
    public ColorRGBA border(float alpha) {
        return new ColorRGBA(255, 255, 255, (int) (6 * alpha));
    }

    @Override
    public ColorRGBA rowBackground(float alpha) {
        return new ColorRGBA(255, 255, 255, (int) (6 * alpha));
    }

    @Override
    public ColorRGBA hoverBackground(float alpha) {
        return new ColorRGBA(255, 255, 255, (int) (10 * alpha));
    }

    @Override
    public boolean neumorphic() {
        return true;
    }

    @Override
    public ColorRGBA neumorphLight(float alpha) {
        return new ColorRGBA(255, 255, 255, (int) (16 * alpha));
    }

    @Override
    public ColorRGBA neumorphDark(float alpha) {
        return new ColorRGBA(0, 0, 0, (int) (140 * alpha));
    }

    @Override
    public float neumorphSoftness() {
        return 22.0F;
    }

    @Override
    public float neumorphOffset() {
        return 7.0F;
    }

    @Override
    public ColorRGBA recessedBackground(float alpha) {
        return RECESSED.withAlpha((int) (210 * alpha));
    }

    @Override
    public void renderCard(UIContext context, float x, float y, float w, float h,
                           BorderRadius radius, boolean enabled, float enableAnim, float hoverAnim, float alpha) {
        float off = 2.5F + 1.5F * hoverAnim;
        float soft = 8.0F + 4.0F * hoverAnim;
        context.drawShadow(x + off, y + off, w, h, soft, radius, neumorphDark((0.5F + 0.3F * hoverAnim) * alpha));
        context.drawShadow(x - off, y - off, w, h, soft, radius, neumorphLight((0.2F + 0.2F * hoverAnim) * alpha));
        if (enableAnim > 0.0F) {
            context.drawShadow(x, y, w, h, 8.0F, radius, Colors.ACCENT.withAlpha((int) (20 * enableAnim * alpha)));
        }
        context.drawRoundedRect(x, y, w, h, radius, rowBackground(alpha).withAlpha((int) ((20 + 15 * hoverAnim) * alpha)));
        ColorRGBA rim = border((0.6F + 0.4F * hoverAnim) * alpha);
        if (enableAnim > 0.0F) {
            rim = rim.mix(Colors.ACCENT.withAlpha((int) (120 * alpha)), enableAnim * 0.4F);
        }
        context.drawRoundedBorder(x, y, w, h, 0.5F, radius, rim);
    }

    @Override
    public void renderActiveTab(UIContext context, float x, float y, float w, float h,
                                BorderRadius radius, float anim, float alpha) {
        if (anim <= 0.0F) return;
        float off = C_OFF * 0.7F;
        float soft = C_SOFT * 0.7F;
        context.drawShadow(x + off, y + off, w, h, soft, radius, neumorphDark(anim * alpha * 0.5F));
        context.drawShadow(x - off, y - off, w, h, soft, radius, neumorphLight(anim * alpha * 0.25F));
        context.drawRoundedRect(x, y, w, h, radius, Colors.ACCENT.withAlpha((int) (160 * anim * alpha)));
        context.drawRoundedBorder(x, y, w, h, 0.5F, radius, Colors.ACCENT.withAlpha((int) (180 * anim * alpha)));
    }

    @Override
    public ColorRGBA toggleAccent(float alpha) {
        return Colors.ACCENT.withAlpha((int) (255 * alpha));
    }

    @Override
    public void renderToggleTrack(UIContext context,
                                   float x, float y, float w, float h, BorderRadius r,
                                   boolean enabled, float enableAnim, float alpha) {
        if (enabled) {
            context.drawShadow(x + C_OFF, y + C_OFF, w, h, C_SOFT, r, neumorphDark(alpha * 0.65F));
            context.drawShadow(x - C_OFF, y - C_OFF, w, h, C_SOFT, r, neumorphLight(alpha * 0.35F));
            context.drawRoundedRect(x, y, w, h, r, Colors.ACCENT.withAlpha((int) (255 * alpha)));
        } else {
            context.drawShadow(x - C_OFF, y - C_OFF, w, h, C_SOFT, r, neumorphDark(alpha * 0.75F));
            context.drawRoundedRect(x, y, w, h, r, RECESSED.withAlpha((int) (220 * alpha)));
            context.drawShadow(x + C_OFF, y + C_OFF, w, h, C_SOFT, r, neumorphLight(alpha * 0.32F));
            context.drawRoundedRect(x, y, w, h, r, RECESSED.withAlpha((int) (220 * alpha)));
            context.drawRoundedBorder(x, y, w, h, 0.5F, r, border(alpha * 0.55F));
        }
    }

    @Override
    public void renderToggleThumb(UIContext context,
                                   float tx, float ty, float size,
                                   float enableAnim, float thumbAlpha) {
        BorderRadius r = BorderRadius.all(size / 2.0F);
        context.drawShadow(tx + C_OFF, ty + C_OFF, size, size, C_SOFT, r, neumorphDark(thumbAlpha * 0.9F));
        context.drawShadow(tx - C_OFF, ty - C_OFF, size, size, C_SOFT, r, neumorphLight(thumbAlpha * 0.45F));
        context.drawRoundedRect(tx, ty, size, size, r, new ColorRGBA(218, 220, 227, (int) (245 * thumbAlpha)));
    }

    @Override
    public void renderSliderTrack(UIContext context,
                                   float x, float y, float w, float h, BorderRadius r,
                                   float alpha) {
        context.drawShadow(x - C_OFF, y - C_OFF, w, h, C_SOFT, r, neumorphDark(alpha * 0.65F));
        context.drawRoundedRect(x, y, w, h, r, RECESSED.withAlpha((int) (200 * alpha)));
        context.drawShadow(x + C_OFF, y + C_OFF, w, h, C_SOFT, r, neumorphLight(alpha * 0.28F));
        context.drawRoundedRect(x, y, w, h, r, RECESSED.withAlpha((int) (200 * alpha)));
    }

    @Override
    public void renderSliderFill(UIContext context,
                                  float x, float y, float w, float h, BorderRadius r,
                                  ColorRGBA accent, float alpha) {
        context.drawRoundedRect(x, y, w, h, r, accent.withAlpha((int) (255 * alpha)));
    }

    @Override
    public void renderSliderThumb(UIContext context,
                                   float cx, float cy, float w, float h,
                                   float movingAnim, float alpha) {
        float tx = cx - w / 2.0F;
        float ty = cy - h / 2.0F;
        BorderRadius r = BorderRadius.all(h / 2.0F);
        context.drawShadow(tx + C_OFF, ty + C_OFF, w, h, C_SOFT, r, neumorphDark(alpha * 0.85F));
        context.drawShadow(tx - C_OFF, ty - C_OFF, w, h, C_SOFT, r, neumorphLight(alpha * 0.42F));
        context.drawRoundedRect(tx, ty, w, h, r, new ColorRGBA(218, 220, 227, (int) (245 * alpha)));
    }

    @Override
    public void renderBindChip(UIContext context,
                                float x, float y, float w, float h, BorderRadius r,
                                float alpha) {
        context.drawShadow(x - C_OFF, y - C_OFF, w, h, C_SOFT, r, neumorphDark(alpha * 0.65F));
        context.drawRoundedRect(x, y, w, h, r, RECESSED.withAlpha((int) (200 * alpha)));
        context.drawShadow(x + C_OFF, y + C_OFF, w, h, C_SOFT, r, neumorphLight(alpha * 0.28F));
        context.drawRoundedRect(x, y, w, h, r, RECESSED.withAlpha((int) (200 * alpha)));
        context.drawRoundedBorder(x, y, w, h, 0.5F, r, border(alpha * 0.48F));
    }
}