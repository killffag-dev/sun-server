package naryn.sun.systems.modules.modules.visuals.hitbox.effects;

import naryn.sun.systems.modules.modules.visuals.hitbox.HitboxFillEffect;
import naryn.sun.systems.modules.modules.visuals.hitbox.HitboxGeometry;
import naryn.sun.utility.colors.ColorRGBA;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;

/**
 * Заливка "Аква": глубокая океаническая синева внизу и переливающийся бирюзово-лазурный верх.
 * Анимация: бегущая волна каустики и пульсация водной толщи.
 */
public class AquaEffect implements HitboxFillEffect {

    private static final float WAVE_SPEED = 0.0025F;
    private static final float RIPPLE_SPEED = 0.005F;

    @Override
    public void renderFill(BufferBuilder builder, MatrixStack matrices, Box box, float opacity, long now) {
        float wave = 0.5F + 0.5F * (float) Math.sin(now * WAVE_SPEED);
        float ripple = 0.5F + 0.5F * (float) Math.sin(now * RIPPLE_SPEED);

        // Нижняя часть: глубокий ультрамарин / бездна
        float bottomHue = 0.58F + 0.04F * wave;
        ColorRGBA bottom = ColorRGBA.fromHSB(bottomHue, 0.85F, 0.30F + 0.15F * wave)
            .withAlpha(255.0F * opacity * (0.65F + 0.15F * wave));

        // Верхняя часть: яркий бирюзово-циановый блик
        float topHue = 0.48F + 0.05F * ripple;
        ColorRGBA top = ColorRGBA.fromHSB(topHue, 0.70F, 0.75F + 0.20F * (1.0F - wave))
            .withAlpha(255.0F * opacity * (0.80F + 0.20F * (1.0F - ripple)));

        HitboxGeometry.addGradientBox(builder, matrices, box, bottom, top);
    }
}