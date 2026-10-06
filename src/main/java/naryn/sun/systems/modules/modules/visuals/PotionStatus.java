package naryn.sun.systems.modules.modules.visuals;

import java.util.Collection;
import naryn.sun.framework.base.CustomDrawContext;
import naryn.sun.framework.msdf.Font;
import naryn.sun.framework.msdf.Fonts;
import naryn.sun.framework.objects.BorderRadius;
import naryn.sun.systems.event.EventListener;
import naryn.sun.systems.event.impl.render.HudRenderEvent;
import naryn.sun.systems.modules.api.ModuleCategory;
import naryn.sun.systems.modules.api.ModuleInfo;
import naryn.sun.systems.modules.impl.PositionableHudModule;
import naryn.sun.systems.setting.settings.BooleanSetting;
import naryn.sun.utility.colors.ColorRGBA;
import naryn.sun.utility.colors.Colors;
import naryn.sun.utility.interfaces.IMinecraft;
import net.minecraft.client.texture.Sprite;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;

@ModuleInfo(name = "Potion Status", category = ModuleCategory.VISUALS, desc = "Отображает активные эффекты зелий")
public class PotionStatus extends PositionableHudModule implements IMinecraft {

    private final BooleanSetting background = new BooleanSetting(this, "hud.background").enable();

    private static final float ICON = 18.0F;
    private static final float GAP  = 4.0F;
    private static final float PAD  = 6.0F;

    private final EventListener<HudRenderEvent> onHudRender = event -> this.draw(event.getContext());

    @Override
    public void renderPreview(CustomDrawContext context) {
        this.draw(context);
    }

    private void draw(CustomDrawContext context) {
        if (mc.player == null) return;

        Collection<StatusEffectInstance> effects = mc.player.getStatusEffects();
        boolean editing = PositionableHudModule.isEditingActive();
        if (effects.isEmpty() && !editing) return;

        Font font = Fonts.REGULAR.getFont(7.0F);
        int rows = Math.max(effects.size(), 1);
        float width  = PAD * 2 + ICON + 30.0F;
        float height = PAD * 2 + rows * (ICON + GAP) - GAP;

        float sw = mc.getWindow().getScaledWidth();
        float x = this.resolveX(sw - width - 20.0F);
        float y = this.resolveY(100.0F);

        this.beginScaledRender(context, width, height);

        if (editing) {
            context.drawRoundedRect(x, y, width, height, BorderRadius.all(6.0F), Colors.getBackgroundColor().withAlpha(200.0F));
        } else if (this.background.isEnabled()) {
            context.drawClientRect(x, y, width, height, 1.0F, 0.0F, 1.0F);
        }

        if (effects.isEmpty()) {
            context.drawText(
                font, "Potion Status", x + PAD, y + PAD,
                Colors.getTextColor().withAlpha(150.0F)
            );
        } else {
            float rowY = y + PAD;
            for (StatusEffectInstance effect : effects) {
                StatusEffect potion = (StatusEffect) effect.getEffectType().value();
                Sprite sprite = mc.getStatusEffectSpriteManager().getSprite(effect.getEffectType());

                context.drawTexture(
                    sprite.getAtlasId(), x + PAD, rowY, ICON, ICON,
                    sprite.getMinU(), sprite.getMaxU(), sprite.getMinV(), sprite.getMaxV(),
                    ColorRGBA.WHITE
                );

                if (effect.getAmplifier() > 0) {
                    String lvl = String.valueOf(effect.getAmplifier() + 1);
                    context.drawText(font, lvl, x + PAD + ICON - font.width(lvl), rowY, ColorRGBA.WHITE);
                }

                String timer;
                if (effect.isInfinite() || effect.getDuration() >= 20 * 3600) {
                    timer = "**:**";
                } else {
                    int totalSec = effect.getDuration() / 20;
                    timer = String.format("%d:%02d", totalSec / 60, totalSec % 60);
                }

                ColorRGBA timerColor = (effect.getDuration() < 200 && (effect.getDuration() / 20) % 2 == 0)
                    ? new ColorRGBA(220, 70, 70, 255)
                    : Colors.getTextColor().withAlpha(255.0F);

                context.drawText(font, timer, x + PAD + ICON + 6.0F, rowY + (ICON - font.height()) / 2.0F, timerColor);

                rowY += ICON + GAP;
            }
        }

        this.endScaledRender(context);
    }
}