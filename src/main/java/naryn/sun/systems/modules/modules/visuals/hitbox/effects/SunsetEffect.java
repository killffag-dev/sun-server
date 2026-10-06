package naryn.sun.systems.modules.modules.visuals.hitbox.effects;

import naryn.sun.systems.modules.modules.visuals.hitbox.HitboxFillEffect;
import naryn.sun.systems.modules.modules.visuals.hitbox.HitboxGeometry;
import naryn.sun.utility.colors.ColorRGBA;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;

/**
 * Заливка "Закат": бархатный пурпурно-рубиновый низ и сияющий кораллово-золотой закатный верх.
 * Анимация: медленный теплый перелив солнечного горизонта с дыханием яркости.
 */
public class SunsetEffect implements HitboxFillEffect {

    private static final float CYCLE_SPEED = 0.002F;
    private static final float GLOW_SPEED = 0.0035F;

    @Override
    public void renderFill(BufferBuilder builder, MatrixStack matrices, Box box, float opacity, long now) {
        float cycle = 0.5F + 0.5F * (float) Math.sin(now * CYCLE_SPEED);
        float glow = 0.5F + 0.5F * (float) Math.cos(now * GLOW_SPEED);

        // Нижняя часть: сумеречный пурпур / маджента (0.88..0.96)
        float bottomHue = 0.88F + 0.07F * cycle;
        ColorRGBA bottom = ColorRGBA.fromHSB(bottomHue, 0.85F, 0.35F + 0.15F * glow)
            .withAlpha(255.0F * opacity * (0.65F + 0.15F * glow));

        // Верхняя часть: закатный огонь / неон-коралл -> янтарное золото (0.04..0.12)
        float topHue = 0.04F + 0.08F * (1.0F - cycle);
        ColorRGBA top = ColorRGBA.fromHSB(topHue, 0.85F, 0.85F + 0.15F * (1.0F - glow))
            .withAlpha(255.0F * opacity * (0.80F + 0.20F * (1.0F - glow)));

        HitboxGeometry.addGradientBox(builder, matrices, box, bottom, top);
    }
}
