package naryn.sun.systems.modules.modules.visuals.prediction;

import java.util.List;
import naryn.sun.access.MCPlayerAccess;
import naryn.sun.framework.base.CustomDrawContext;
import naryn.sun.framework.msdf.Font;
import naryn.sun.framework.msdf.Fonts;
import naryn.sun.framework.objects.BorderRadius;
import naryn.sun.utility.colors.ColorRGBA;
import naryn.sun.utility.colors.Colors;
import naryn.sun.utility.render.Utils;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;

public final class PredictionHudRenderer {

    // Цвета стиля «Стекло» (соответствуют NameTagStyle / Glass HUD)
    private static final ColorRGBA GLASS_BG = new ColorRGBA(10, 12, 16, 144);
    private static final ColorRGBA GLASS_BORDER = new ColorRGBA(255, 255, 255, 32);
    private static final ColorRGBA GLASS_BORDER_TOP = new ColorRGBA(255, 255, 255, 64);
    private static final BorderRadius CARD_RADIUS = BorderRadius.all(4.0F);

    private PredictionHudRenderer() {
    }

    public static void render(
        CustomDrawContext context,
        List<TrajectoryData> trajectories
    ) {
        if (trajectories.isEmpty()) return;

        AbstractClientPlayerEntity player = MCPlayerAccess.get();
        if (player == null) return;

        MatrixStack ms = context.getMatrices();
        Font font = Fonts.MEDIUM.getFont(12.0F);

        // Отрисовка плашек у точек приземления снарядов ТОЛЬКО для уже выпущенных снарядов
        for (TrajectoryData data : trajectories) {
            // Для предметов в руках (натягивание лука, прицеливание) и статических облаков на земле плашку НЕ рисуем
            if (data.inHand() || data.entity() instanceof AreaEffectCloudEntity) continue;

            Vec2f screenPos = Utils.worldToScreen(data.getLandingPos());
            if (screenPos == null) continue;

            float distance = (float) data.getLandingPos().distanceTo(player.getEyePos());
            float scale = MathHelper.clamp(1.0F - distance / 25.0F, 0.65F, 1.0F);

            ms.push();
            ms.translate(screenPos.x, screenPos.y, 0.0F);
            ms.scale(scale, scale, 1.0F);

            renderLandingBadge(context, font, data);

            ms.pop();
        }
    }

    private static void renderLandingBadge(CustomDrawContext context, Font font, TrajectoryData data) {
        float textWidth = font.width(data.displayName());
        float iconSize = 14.0F;
        float padX = 5.0F;
        float cardWidth = padX + iconSize + 4.0F + textWidth + padX;
        float cardHeight = font.height() + 8.0F;
        float cardX = -cardWidth / 2.0F;
        float cardY = -cardHeight - 2.0F;

        // Фон стиля «Стекло»
        context.drawRoundedRect(cardX, cardY, cardWidth, cardHeight, CARD_RADIUS, GLASS_BG);
        context.drawRoundedBorder(cardX, cardY, cardWidth, cardHeight, 0.75F, CARD_RADIUS, GLASS_BORDER);
        context.drawRect(cardX + 2.0F, cardY, cardWidth - 4.0F, 0.75F, GLASS_BORDER_TOP);

        // Иконка снаряда
        float iconY = cardY + (cardHeight - 16.0F * 0.85F) / 2.0F;
        context.drawItem(data.itemStack(), cardX + padX, iconY, 0.85F);

        // Название и оставшееся время
        float textX = cardX + padX + 16.0F * 0.85F + 3.0F;
        float textY = cardY + (cardHeight - font.height()) / 2.0F + 1.0F;
        context.drawText(font, data.displayName(), textX, textY, Colors.WHITE);

        // Эффекты зелий (если применимо)
        if (data.effectLabels() != null && !data.effectLabels().isEmpty()) {
            float effY = cardY + cardHeight + 3.0F;
            List<String> labels = data.effectLabels();
            List<StatusEffectInstance> effects = data.potionEffects();
            for (int i = 0; i < labels.size(); i++) {
                String fullEffText = labels.get(i);
                float effWidth = font.width(fullEffText) + 8.0F;
                float effHeight = font.height() + 4.0F;
                float effX = -effWidth / 2.0F;

                context.drawRoundedRect(effX, effY, effWidth, effHeight, BorderRadius.all(3.0F), GLASS_BG);
                context.drawRoundedBorder(effX, effY, effWidth, effHeight, 0.5F, BorderRadius.all(3.0F), GLASS_BORDER);

                ColorRGBA effColor = ColorRGBA.WHITE;
                if (effects != null && i < effects.size()) {
                    effColor = ColorRGBA.fromInt(((StatusEffect) effects.get(i).getEffectType().value()).getColor()).withAlpha(255.0F);
                }
                context.drawText(font, fullEffText, effX + 4.0F, effY + 2.0F, effColor);

                effY += effHeight + 2.0F;
            }
        }

        context.flushItems();
    }
}
