package naryn.sun.systems.modules.modules.visuals;

import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import naryn.sun.Sun;
import naryn.sun.systems.event.EventListener;
import naryn.sun.systems.event.impl.render.Render3DEvent;
import naryn.sun.systems.modules.api.ModuleCategory;
import naryn.sun.systems.modules.api.ModuleInfo;
import naryn.sun.systems.modules.impl.BaseModule;
import naryn.sun.systems.modules.modules.visuals.targetesp.TargetEnergyHelixRenderer;
import naryn.sun.systems.modules.modules.visuals.targetesp.TargetKineticShardsRenderer;
import naryn.sun.systems.modules.modules.visuals.targetesp.TargetNaniteHoneycombRenderer;

import naryn.sun.systems.modules.modules.visuals.targetesp.TargetSingularityRenderer;
import naryn.sun.systems.modules.modules.visuals.targetesp.TargetSonarEchoRenderer;
import naryn.sun.systems.setting.settings.BooleanSetting;
import naryn.sun.systems.setting.settings.ColorSetting;
import naryn.sun.systems.setting.settings.ModeSetting;
import naryn.sun.systems.setting.settings.SliderSetting;
import naryn.sun.systems.target.TargetSettings;
import naryn.sun.utility.animation.base.Animation;
import naryn.sun.utility.animation.base.Easing;
import naryn.sun.utility.colors.Colors;
import naryn.sun.utility.game.EntityUtility;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;

@ModuleInfo(name = "Target ESP", category = ModuleCategory.VISUALS, desc = "Помечает активную цель")
public class TargetESP extends BaseModule {
    private final ModeSetting mode = new ModeSetting(this, "modules.settings.target_esp.mode");
    private final ModeSetting.Value singularity = new ModeSetting.Value(this.mode, "modules.settings.target_esp.mode.singularity").select();
    private final ModeSetting.Value naniteHoneycomb = new ModeSetting.Value(this.mode, "modules.settings.target_esp.mode.nanite_honeycomb");
    private final ModeSetting.Value dnaHelix = new ModeSetting.Value(this.mode, "modules.settings.target_esp.mode.dna_helix");
    private final ModeSetting.Value sonarEcho = new ModeSetting.Value(this.mode, "modules.settings.target_esp.mode.sonar_echo");
    private final ModeSetting.Value kineticShards = new ModeSetting.Value(this.mode, "modules.settings.target_esp.mode.kinetic_shards");

    private final BooleanSetting targetMobs = new BooleanSetting(this, "modules.settings.target_esp.target_mobs").enabled(false);
    private final SliderSetting range = new SliderSetting(this, "modules.settings.target_esp.range").min(1.0f).max(50.0f).step(0.5f).currentValue(3.0f);

    private final ColorSetting color = new ColorSetting(this, "modules.settings.target_esp.color").color(Colors.ACCENT);
    private final SliderSetting radius = new SliderSetting(this, "modules.settings.target_esp.radius").min(0.5f).max(2.5f).step(0.05f).currentValue(1.0f);
    private final SliderSetting speed = new SliderSetting(this, "modules.settings.target_esp.speed").min(0.2f).max(3.0f).step(0.1f).currentValue(1.0f);
    private final SliderSetting glow = new SliderSetting(this, "modules.settings.target_esp.glow").min(0.1f).max(3.0f).step(0.1f).currentValue(1.0f);
    private final SliderSetting size = new SliderSetting(this, "modules.settings.target_esp.size").min(0.3f).max(2.0f).step(0.05f).currentValue(1.0f);

    private final Animation animation = new Animation(300L, 0.0F, Easing.BOTH_CUBIC);
    private LivingEntity prevTarget;
    private long lastTargetTime;

    public TargetESP() {
    }

    private final EventListener<Render3DEvent> onRender3D = event -> {
        if (EntityUtility.isInGame()) {
            TargetSettings settings = new TargetSettings.Builder()
                    .targetPlayers(true)
                    .targetMobs(this.targetMobs.isEnabled())
                    .targetAnimals(this.targetMobs.isEnabled())
                    .requiredRange(this.range.getCurrentValue())
                    .build();
            Sun.getInstance().getTargetManager().updateCrosshair(settings, this.range.getCurrentValue());

            LivingEntity target = Sun.getInstance().getTargetManager().getCurrentTarget() instanceof LivingEntity target2 ? target2 : null;
            long now = System.currentTimeMillis();

            if (target != null) {
                this.prevTarget = target;
                this.lastTargetTime = now;
            } else if (this.prevTarget != null) {
                if (now - this.lastTargetTime < 400L && this.prevTarget.isAlive()
                        && mc.player != null && mc.player.distanceTo(this.prevTarget) <= this.range.getCurrentValue() + 2.0F) {
                    target = this.prevTarget;
                }
            }

            this.animation.setEasing(Easing.FIGMA_EASE_IN_OUT);
            this.animation.update(target != null);

            if (this.prevTarget != null && this.animation.getValue() > 0.01F) {
                MatrixStack ms = event.getMatrices();
                ms.push();
                RenderSystem.enableBlend();
                RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
                RenderSystem.enableDepthTest();
                RenderSystem.disableCull();
                RenderSystem.depthMask(false);

                float animVal = this.animation.getValue();
                float rVal = this.radius.getCurrentValue();
                float sVal = this.speed.getCurrentValue();
                float gVal = this.glow.getCurrentValue();
                float szVal = this.size.getCurrentValue();

                if (this.singularity.isSelected()) {
                    TargetSingularityRenderer.draw(ms, this.prevTarget, this.color.getColor(), animVal, rVal, sVal, gVal, szVal);
                } else if (this.naniteHoneycomb.isSelected()) {
                    TargetNaniteHoneycombRenderer.draw(ms, this.prevTarget, this.color.getColor(), animVal, rVal, sVal, gVal, szVal);
                } else if (this.dnaHelix.isSelected()) {
                    TargetEnergyHelixRenderer.draw(ms, this.prevTarget, this.color.getColor(), animVal, rVal, sVal, gVal, szVal);
                } else if (this.sonarEcho.isSelected()) {
                    TargetSonarEchoRenderer.draw(ms, this.prevTarget, this.color.getColor(), animVal, rVal, sVal, gVal, szVal);
                } else if (this.kineticShards.isSelected()) {
                    TargetKineticShardsRenderer.draw(ms, this.prevTarget, this.color.getColor(), animVal, rVal, sVal, gVal, szVal);
                }

                RenderSystem.depthMask(true);
                RenderSystem.setShaderTexture(0, 0);
                RenderSystem.disableBlend();
                RenderSystem.enableCull();
                RenderSystem.disableDepthTest();
                ms.pop();
            }
        }
    };
}