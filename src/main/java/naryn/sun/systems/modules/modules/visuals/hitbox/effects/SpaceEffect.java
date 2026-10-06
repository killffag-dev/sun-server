package naryn.sun.systems.modules.modules.visuals.hitbox.effects;

import naryn.sun.systems.modules.modules.visuals.hitbox.HitboxFillEffect;
import naryn.sun.systems.modules.modules.visuals.hitbox.HitboxGeometry;
import naryn.sun.utility.colors.ColorRGBA;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;

/**
 * Заливка "Космос": глубокий индиго-фиолетовый низ и переливающийся неоновый верх (космическая туманность/звёздная пыль).
 * Анимация: многофазный дрейф оттенка и гармоническая пульсация яркости туманности.
 */
public class SpaceEffect implements HitboxFillEffect {

    private static final float HUE_CYCLE_MS = 8000.0F;
    private static final float PULSE_SPEED = 0.003F;

    @Override
    public void renderFill(BufferBuilder builder, MatrixStack matrices, Box box, float opacity, long now) {
        float hueShift = (now % HUE_CYCLE_MS) / HUE_CYCLE_MS;
        float pulse = 0.5F + 0.5F * (float) Math.sin(now * PULSE_SPEED);
        float secondaryPulse = 0.5F + 0.5F * (float) Math.cos(now * (PULSE_SPEED * 0.7F));

        // Нижняя часть: глубокий космический индиго с плавающим оттенком 0.72..0.80
        float bottomHue = (0.72F + hueShift * 0.08F) % 1.0F;
        ColorRGBA bottom = ColorRGBA.fromHSB(bottomHue, 0.90F, 0.20F + 0.15F * pulse)
            .withAlpha(255.0F * opacity * (0.60F + 0.20F * pulse));

        // Верхняя часть: яркий неоновый фиолетово-циан градиент (туманность) 0.82..0.95
        float topHue = (0.80F + hueShift * 0.15F + 0.05F * secondaryPulse) % 1.0F;
        ColorRGBA top = ColorRGBA.fromHSB(topHue, 0.65F, 0.70F + 0.25F * (1.0F - pulse))
            .withAlpha(255.0F * opacity * (0.75F + 0.25F * (1.0F - pulse)));

        HitboxGeometry.addGradientBox(builder, matrices, box, bottom, top);
    }
}