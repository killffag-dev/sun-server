package naryn.sun.systems.modules.modules.visuals.hitbox.effects;

import naryn.sun.systems.modules.modules.visuals.hitbox.HitboxFillEffect;
import naryn.sun.systems.modules.modules.visuals.hitbox.HitboxGeometry;
import naryn.sun.utility.colors.ColorRGBA;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;

/**
 * Заливка "Аврора": изумрудно-неоновый низ и переливающееся полярное фиолетовое сияние вверху.
 * Анимация: гармонический волновой танец лент северного сияния.
 */
public class AuroraEffect implements HitboxFillEffect {

    private static final float AURORA_SPEED = 0.0022F;
    private static final float SHIMMER_SPEED = 0.004F;

    @Override
    public void renderFill(BufferBuilder builder, MatrixStack matrices, Box box, float opacity, long now) {
        float wave = 0.5F + 0.5F * (float) Math.sin(now * AURORA_SPEED);
        float shimmer = 0.5F + 0.5F * (float) Math.sin(now * SHIMMER_SPEED + 1.2F);

        // Нижняя часть: изумрудно-мятный неоновый свет северного сияния (0.36..0.44)
        float bottomHue = 0.36F + 0.08F * wave;
        ColorRGBA bottom = ColorRGBA.fromHSB(bottomHue, 0.85F, 0.40F + 0.20F * shimmer)
            .withAlpha(255.0F * opacity * (0.65F + 0.15F * shimmer));

        // Верхняя часть: электрический фиолетово-маджентовый полярный свет (0.75..0.85)
        float topHue = 0.75F + 0.10F * (1.0F - wave);
        ColorRGBA top = ColorRGBA.fromHSB(topHue, 0.75F, 0.75F + 0.20F * (1.0F - shimmer))
            .withAlpha(255.0F * opacity * (0.80F + 0.20F * (1.0F - shimmer)));

        HitboxGeometry.addGradientBox(builder, matrices, box, bottom, top);
    }
}
