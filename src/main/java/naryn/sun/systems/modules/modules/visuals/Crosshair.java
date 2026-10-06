package naryn.sun.systems.modules.modules.visuals;

import naryn.sun.framework.base.CustomDrawContext;
import naryn.sun.framework.objects.BorderRadius;
import naryn.sun.systems.event.EventListener;
import naryn.sun.systems.event.impl.render.HudRenderEvent;
import naryn.sun.systems.modules.api.ModuleCategory;
import naryn.sun.systems.modules.api.ModuleInfo;
import naryn.sun.systems.modules.impl.BaseModule;
import naryn.sun.systems.setting.settings.BooleanSetting;
import naryn.sun.systems.setting.settings.ColorSetting;
import naryn.sun.systems.setting.settings.ModeSetting;
import naryn.sun.systems.setting.settings.SliderSetting;
import naryn.sun.utility.colors.ColorRGBA;
import naryn.sun.utility.colors.Colors;
import naryn.sun.utility.interfaces.IMinecraft;

@ModuleInfo(name = "Crosshair", category = ModuleCategory.VISUALS, desc = "modules.descriptions.crosshair")
public class Crosshair extends BaseModule implements IMinecraft {

    private final ModeSetting mode = new ModeSetting(this, "modules.settings.crosshair.mode");
    private final ModeSetting.Value modeCross = new ModeSetting.Value(this.mode, "modules.settings.crosshair.mode.cross").select();
    private final ModeSetting.Value modeDot = new ModeSetting.Value(this.mode, "modules.settings.crosshair.mode.dot");
    private final ModeSetting.Value modeCircle = new ModeSetting.Value(this.mode, "modules.settings.crosshair.mode.circle");

    private final SliderSetting length = new SliderSetting(this, "modules.settings.crosshair.length", () -> this.modeDot.isSelected())
            .min(1.0F).max(20.0F).step(0.5F).currentValue(4.0F);

    private final SliderSetting gap = new SliderSetting(this, "modules.settings.crosshair.gap", () -> this.modeDot.isSelected())
            .min(0.0F).max(15.0F).step(0.5F).currentValue(2.5F);

    private final SliderSetting thickness = new SliderSetting(this, "modules.settings.crosshair.thickness")
            .min(0.5F).max(4.0F).step(0.5F).currentValue(1.0F);

    private final SliderSetting dotSize = new SliderSetting(this, "modules.settings.crosshair.dot_size", () -> !this.modeDot.isSelected())
            .min(0.5F).max(5.0F).step(0.5F).currentValue(1.5F);

    private final BooleanSetting outline = new BooleanSetting(this, "modules.settings.crosshair.outline").enable();
    private final BooleanSetting dynamicSpread = new BooleanSetting(this, "modules.settings.crosshair.dynamic_spread");

    private final ColorSetting color = new ColorSetting(this, "modules.settings.crosshair.color")
            .color(Colors.WHITE);

    private final EventListener<HudRenderEvent> onHudRender = event -> this.draw(event.getContext());

    private void draw(CustomDrawContext context) {
        if (mc.options.hudHidden || mc.currentScreen != null) {
            return;
        }

        float cx = mc.getWindow().getScaledWidth() / 2.0F;
        float cy = mc.getWindow().getScaledHeight() / 2.0F;

        ColorRGBA mainColor = this.color.getColor();
        ColorRGBA outlineColor = ColorRGBA.BLACK.withAlpha(mainColor.getAlpha() * 0.75F);

        float spread = 0.0F;
        if (this.dynamicSpread.isEnabled() && mc.player != null) {
            if (mc.player.getVelocity().horizontalLength() > 0.05 || mc.player.handSwinging) {
                spread = 2.5F;
            }
        }

        float t = this.thickness.getCurrentValue();
        float g = this.gap.getCurrentValue() + spread;
        float l = this.length.getCurrentValue();

        if (this.modeCross.isSelected()) {
            if (this.outline.isEnabled()) {
                float ot = t + 1.0F;
                float ol = l + 1.0F;
                float og = Math.max(0.0F, g - 0.5F);
                drawCross(context, cx, cy, og, ol, ot, outlineColor);
            }
            drawCross(context, cx, cy, g, l, t, mainColor);
        } else if (this.modeDot.isSelected()) {
            float ds = this.dotSize.getCurrentValue();
            if (this.outline.isEnabled()) {
                context.drawRoundedRect(cx - ds - 0.5F, cy - ds - 0.5F, (ds + 0.5F) * 2.0F, (ds + 0.5F) * 2.0F, BorderRadius.all(ds + 0.5F), outlineColor);
            }
            context.drawRoundedRect(cx - ds, cy - ds, ds * 2.0F, ds * 2.0F, BorderRadius.all(ds), mainColor);
        } else if (this.modeCircle.isSelected()) {
            float radius = g + l;
            if (this.outline.isEnabled()) {
                drawCircle(context, cx, cy, radius - 0.5F, outlineColor);
                drawCircle(context, cx, cy, radius + 0.5F, outlineColor);
            }
            drawCircle(context, cx, cy, radius, mainColor);
        }
    }

    private void drawCircle(CustomDrawContext context, float cx, float cy, float radius, ColorRGBA col) {
        int segments = 24;
        float angleStep = (float) (2.0 * Math.PI / segments);
        for (int i = 0; i < segments; i++) {
            float a1 = i * angleStep;
            float a2 = (i + 1) * angleStep;
            net.minecraft.util.math.Vec2f p1 = new net.minecraft.util.math.Vec2f(
                cx + (float) Math.cos(a1) * radius,
                cy + (float) Math.sin(a1) * radius
            );
            net.minecraft.util.math.Vec2f p2 = new net.minecraft.util.math.Vec2f(
                cx + (float) Math.cos(a2) * radius,
                cy + (float) Math.sin(a2) * radius
            );
            context.drawLine(p1, p2, col);
        }
    }

    private void drawCross(CustomDrawContext context, float cx, float cy, float g, float l, float t, ColorRGBA col) {
        float halfT = t / 2.0F;
        // Top
        context.drawRect(cx - halfT, cy - g - l, t, l, col);
        // Bottom
        context.drawRect(cx - halfT, cy + g, t, l, col);
        // Left
        context.drawRect(cx - g - l, cy - halfT, l, t, col);
        // Right
        context.drawRect(cx + g, cy - halfT, l, t, col);
    }
}
