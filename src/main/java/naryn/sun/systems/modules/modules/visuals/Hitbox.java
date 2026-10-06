package naryn.sun.systems.modules.modules.visuals;

import com.mojang.blaze3d.systems.RenderSystem;
import naryn.sun.systems.event.EventListener;
import naryn.sun.systems.event.impl.render.Render3DEvent;
import naryn.sun.systems.modules.api.ModuleCategory;
import naryn.sun.systems.modules.api.ModuleInfo;
import naryn.sun.systems.modules.impl.BaseModule;
import naryn.sun.systems.modules.modules.visuals.hitbox.HitboxFillEffect;
import naryn.sun.systems.modules.modules.visuals.hitbox.HitboxGeometry;
import naryn.sun.systems.modules.modules.visuals.hitbox.effects.AquaEffect;
import naryn.sun.systems.modules.modules.visuals.hitbox.effects.AuroraEffect;
import naryn.sun.systems.modules.modules.visuals.hitbox.effects.SpaceEffect;
import naryn.sun.systems.modules.modules.visuals.hitbox.effects.SunsetEffect;
import naryn.sun.systems.setting.settings.BooleanSetting;
import naryn.sun.systems.setting.settings.ColorSetting;
import naryn.sun.systems.setting.settings.ModeSetting;
import naryn.sun.systems.setting.settings.SliderSetting;
import naryn.sun.utility.colors.ColorRGBA;
import naryn.sun.utility.colors.Colors;
import naryn.sun.utility.game.EntityUtility;
import naryn.sun.utility.render.Draw3DUtility;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

@ModuleInfo(name = "Hitbox", category = ModuleCategory.VISUALS, desc = "Перекрашивает и заливает хитбоксы сущностей")
public class Hitbox extends BaseModule {

    private static final double OUTLINE_EXPAND = 0.002;

    // ===== Цвет (общий для обводки и статичной заливки) =====
    private final ModeSetting colorMode = new ModeSetting(this, "modules.settings.hitbox.color_mode");
    private final ModeSetting.Value colorSolid = new ModeSetting.Value(this.colorMode, "modules.settings.hitbox.color_mode.solid").select();
    private final ModeSetting.Value colorGradient = new ModeSetting.Value(this.colorMode, "modules.settings.hitbox.color_mode.gradient");
    private final ModeSetting.Value colorRainbow = new ModeSetting.Value(this.colorMode, "modules.settings.hitbox.color_mode.rainbow");

    private final ColorSetting solidColor = new ColorSetting(this, "modules.settings.hitbox.solid_color", () -> !this.colorMode.is(this.colorSolid)).color(Colors.ACCENT);
    private final ColorSetting gradientColorA = new ColorSetting(this, "modules.settings.hitbox.gradient_color_a", () -> !this.colorMode.is(this.colorGradient)).color(Colors.ACCENT);
    private final ColorSetting gradientColorB = new ColorSetting(this, "modules.settings.hitbox.gradient_color_b", () -> !this.colorMode.is(this.colorGradient)).color(Colors.WHITE);
    private final SliderSetting rainbowSpeed = new SliderSetting(this, "modules.settings.hitbox.rainbow_speed", () -> !this.colorMode.is(this.colorRainbow))
        .min(0.1F).max(5.0F).step(0.1F).currentValue(1.0F).suffix("x");

    // ===== Обводка =====
    private final BooleanSetting outline = new BooleanSetting(this, "modules.settings.hitbox.outline").enabled(true);
    private final SliderSetting outlineWidth = new SliderSetting(this, "modules.settings.hitbox.outline_width", () -> !this.outline.isEnabled())
        .min(0.0F).max(100.0F).step(1.0F).currentValue(50.0F);
    private final BooleanSetting cornersOnly = new BooleanSetting(this, "modules.settings.hitbox.corners_only", () -> !this.outline.isEnabled() || this.outlineWidth.getCurrentValue() <= 0.0F).enabled(false);
    private final SliderSetting cornerSize = new SliderSetting(this, "modules.settings.hitbox.corner_size", () -> !this.outline.isEnabled() || this.outlineWidth.getCurrentValue() <= 0.0F || !this.cornersOnly.isEnabled())
        .min(0.05F).max(0.5F).step(0.01F).currentValue(0.2F);

    // ===== Заливка =====
    private final BooleanSetting fill = new BooleanSetting(this, "modules.settings.hitbox.fill").enabled(false);
    private final SliderSetting fillOpacity = new SliderSetting(this, "modules.settings.hitbox.fill_opacity", () -> !this.fill.isEnabled())
        .min(0.0F).max(100.0F).step(5.0F).currentValue(50.0F).suffix("%");
    private final ModeSetting fillStyle = new ModeSetting(this, "modules.settings.hitbox.fill_style", () -> !this.fill.isEnabled());
    private final ModeSetting.Value fillStatic = new ModeSetting.Value(this.fillStyle, "modules.settings.hitbox.fill_style.static").select();
    private final ModeSetting.Value fillAnimated = new ModeSetting.Value(this.fillStyle, "modules.settings.hitbox.fill_style.animated");
    private final ModeSetting fillEffect = new ModeSetting(this, "modules.settings.hitbox.fill_effect", () -> !this.fill.isEnabled() || !this.fillStyle.is(this.fillAnimated));
    private final ModeSetting.Value spaceEffectMode = new ModeSetting.Value(this.fillEffect, "modules.settings.hitbox.fill_effect.space").select();
    private final ModeSetting.Value aquaEffectMode = new ModeSetting.Value(this.fillEffect, "modules.settings.hitbox.fill_effect.aqua");
    private final ModeSetting.Value sunsetEffectMode = new ModeSetting.Value(this.fillEffect, "modules.settings.hitbox.fill_effect.sunset");
    private final ModeSetting.Value auroraEffectMode = new ModeSetting.Value(this.fillEffect, "modules.settings.hitbox.fill_effect.aurora");

    private final SpaceEffect spaceEffect = new SpaceEffect();
    private final AquaEffect aquaEffect = new AquaEffect();
    private final SunsetEffect sunsetEffect = new SunsetEffect();
    private final AuroraEffect auroraEffect = new AuroraEffect();

    private final EventListener<Render3DEvent> onRender3D = event -> {
        if (!EntityUtility.isInGame() || mc.world == null) {
            return;
        }

        MatrixStack ms = event.getMatrices();
        Camera camera = event.getCamera();
        Vec3d camPos = camera.getPos();
        float tickDelta = event.getTickDelta();
        long now = System.currentTimeMillis();
        boolean isFirstPerson = mc.options.getPerspective().isFirstPerson();
        Entity cameraEntity = mc.getCameraEntity();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.disableCull();
        naryn.sun.systems.modules.modules.visuals.KillEffects killEffects = naryn.sun.Sun.getInstance().getModuleManager().getModule(naryn.sun.systems.modules.modules.visuals.KillEffects.class);
        boolean killEffectsActive = killEffects != null && killEffects.isEnabled();

        for (Entity entity : mc.world.getEntities()) {
            if (entity == null) {
                continue;
            }
            if (killEffectsActive && killEffects.isDeadOrDissolving(entity)) {
                continue;
            }
            if (entity instanceof net.minecraft.entity.LivingEntity living && (living.isDead() || living.getHealth() <= 0.0F || living.deathTime > 0)) {
                continue;
            }
            // В режиме от 1 лица не рендерим хитбокс локального игрока/камеры,
            // чтобы грани и градиенты не закрывали экран игроку
            if (isFirstPerson && (entity == cameraEntity || entity == mc.player)) {
                continue;
            }

            Box relativeBox = entity.getBoundingBox().offset(-entity.getX(), -entity.getY(), -entity.getZ());

            double renderX = MathHelper.lerp((double) tickDelta, entity.lastRenderX, entity.getX()) - camPos.x;
            double renderY = MathHelper.lerp((double) tickDelta, entity.lastRenderY, entity.getY()) - camPos.y;
            double renderZ = MathHelper.lerp((double) tickDelta, entity.lastRenderZ, entity.getZ()) - camPos.z;

            Box box = relativeBox.offset(renderX, renderY, renderZ);

            // Сначала рисуем заливку, затем обводку — чтобы рёбра и уголки рисовались поверх заливки
            if (this.fill.isEnabled()) {
                this.renderFill(ms, box, now);
            }

            if (this.outline.isEnabled() && this.outlineWidth.getCurrentValue() > 0.0F) {
                this.renderOutline(ms, box, now);
            }
        }

        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
    };

    private ColorRGBA getFlatColor(long now) {
        if (this.colorMode.is(this.colorRainbow)) {
            float hue = now * this.rainbowSpeed.getCurrentValue() * 0.00005F % 1.0F;
            return ColorRGBA.fromHSB(hue, 1.0F, 1.0F);
        }
        return this.solidColor.getColor();
    }

    private void renderOutline(MatrixStack ms, Box box, long now) {
        float widthVal = this.outlineWidth.getCurrentValue();
        if (widthVal <= 0.0F) {
            return;
        }

        // Вычисляем реальный 3D-радиус для рёбер/уголков в зависимости от слайдера 0..100
        float radius = 0.002F + (widthVal / 100.0F) * 0.012F;
        Box outlineBox = box.expand(OUTLINE_EXPAND);

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferBuilder buf = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        if (this.colorMode.is(this.colorGradient)) {
            ColorRGBA bottom = this.gradientColorA.getColor();
            ColorRGBA top = this.gradientColorB.getColor();
            if (this.cornersOnly.isEnabled()) {
                HitboxGeometry.addThickCornersGradient(buf, ms, outlineBox, bottom, top, this.cornerSize.getCurrentValue(), radius);
            } else {
                HitboxGeometry.addThickOutlineGradient(buf, ms, outlineBox, bottom, top, radius);
            }
        } else {
            ColorRGBA color = this.getFlatColor(now);
            if (this.cornersOnly.isEnabled()) {
                HitboxGeometry.addThickCorners(buf, ms, outlineBox, color, this.cornerSize.getCurrentValue(), radius);
            } else {
                HitboxGeometry.addThickOutline(buf, ms, outlineBox, color, radius);
            }
        }

        BufferRenderer.drawWithGlobalProgram(buf.end());
    }

    private void renderFill(MatrixStack ms, Box box, long now) {
        float opacity = this.fillOpacity.getCurrentValue() / 100.0F;
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferBuilder buf = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        if (this.fillStyle.is(this.fillAnimated)) {
            this.getCurrentFillEffect().renderFill(buf, ms, box, opacity, now);
        } else if (this.colorMode.is(this.colorGradient)) {
            ColorRGBA bottom = this.gradientColorA.getColor();
            ColorRGBA top = this.gradientColorB.getColor();
            HitboxGeometry.addGradientBox(
                buf, ms, box,
                bottom.withAlpha(bottom.getAlpha() * opacity),
                top.withAlpha(top.getAlpha() * opacity)
            );
        } else {
            ColorRGBA color = this.getFlatColor(now);
            Draw3DUtility.renderFilledBox(ms, buf, box, color.withAlpha(color.getAlpha() * opacity));
        }

        BufferRenderer.drawWithGlobalProgram(buf.end());
    }

    private HitboxFillEffect getCurrentFillEffect() {
        if (this.fillEffect.is(this.aquaEffectMode)) {
            return this.aquaEffect;
        }
        if (this.fillEffect.is(this.sunsetEffectMode)) {
            return this.sunsetEffect;
        }
        if (this.fillEffect.is(this.auroraEffectMode)) {
            return this.auroraEffect;
        }
        return this.spaceEffect;
    }
}