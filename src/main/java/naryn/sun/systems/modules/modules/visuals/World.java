package naryn.sun.systems.modules.modules.visuals;

import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import naryn.sun.Sun;
import naryn.sun.systems.event.EventListener;
import naryn.sun.systems.event.impl.render.Render3DEvent;
import naryn.sun.systems.modules.api.ModuleCategory;
import naryn.sun.systems.modules.api.ModuleInfo;
import naryn.sun.systems.modules.impl.BaseModule;
import naryn.sun.systems.modules.modules.visuals.world.WorldGeometry;
import naryn.sun.systems.modules.modules.visuals.world.WorldModel;
import naryn.sun.systems.modules.modules.visuals.world.WorldModelType;
import naryn.sun.systems.setting.settings.BooleanSetting;
import naryn.sun.systems.setting.settings.ColorSetting;
import naryn.sun.systems.setting.settings.ModeSetting;
import naryn.sun.systems.setting.settings.SliderSetting;
import naryn.sun.utility.animation.base.Animation;
import naryn.sun.utility.animation.base.Easing;
import naryn.sun.utility.colors.ColorRGBA;
import naryn.sun.utility.colors.Colors;
import naryn.sun.utility.math.MathUtility;
import naryn.sun.utility.render.DrawUtility;
import naryn.sun.utility.render.RenderUtility;
import naryn.sun.utility.render.Utils;
import naryn.sun.utility.time.Timer;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;

@ModuleInfo(name = "World", category = ModuleCategory.VISUALS, desc = "Визуальные дополнения мира")
public class World extends BaseModule {
   private final List<World.Particle> particles = new ArrayList<>();

   private final ModeSetting model = new ModeSetting(this, "modules.settings.world.model");
   private final ModeSetting.Value totem = new ModeSetting.Value(this.model, "modules.settings.world.model.totem").select();
   private final ModeSetting.Value sword = new ModeSetting.Value(this.model, "modules.settings.world.model.sword");
   private final ModeSetting.Value pickaxe = new ModeSetting.Value(this.model, "modules.settings.world.model.pickaxe");
   private final ModeSetting.Value crystal = new ModeSetting.Value(this.model, "modules.settings.world.model.crystal");
   private final ModeSetting.Value heart = new ModeSetting.Value(this.model, "modules.settings.world.model.heart");
   private final ModeSetting.Value random = new ModeSetting.Value(this.model, "modules.settings.world.model.random");

   private final naryn.sun.systems.setting.settings.GroupSetting generalGroup = new naryn.sun.systems.setting.settings.GroupSetting(this, "modules.settings.world.group.general");
   private final BooleanSetting bloom = new BooleanSetting(this.generalGroup, "modules.settings.world.bloom").enabled(true);
   private final SliderSetting size = new SliderSetting(this.generalGroup, "modules.settings.world.size").min(0.1F).max(0.6F).step(0.05F).currentValue(0.25F);
   private final SliderSetting count = new SliderSetting(this.generalGroup, "modules.settings.world.count").min(20.0F).max(120.0F).step(5.0F).currentValue(75.0F);

   private final naryn.sun.systems.setting.settings.GroupSetting colorGroup = new naryn.sun.systems.setting.settings.GroupSetting(this, "modules.settings.world.group.color");
   private final ColorSetting color = new ColorSetting(this.colorGroup, "modules.settings.world.color").color(Colors.ACCENT);

   private final EventListener<Render3DEvent> on3DRender = event -> {
      MatrixStack ms = event.getMatrices();
      Camera camera = mc.gameRenderer.getCamera();
      Vec3d cameraPos = camera.getPos();
      ColorRGBA mainColor = this.color.getColor();
      float r = mainColor.getRed() / 255.0F;
      float g = mainColor.getGreen() / 255.0F;
      float b = mainColor.getBlue() / 255.0F;
      float baseAlpha = mainColor.getAlpha() / 255.0F;

      // 1. Bloom billboard pass
      if (this.bloom.isEnabled()) {
         ms.push();
         RenderSystem.enableBlend();
         RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
         RenderSystem.enableDepthTest();
         RenderSystem.disableCull();
         RenderSystem.depthMask(false);
         Identifier id = Sun.id("textures/bloom.png");
         RenderSystem.setShaderTexture(0, id);
         RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
         BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

         for (World.Particle particle : this.particles) {
            Vec3d pos = Utils.getInterpolatedPos(particle.prev, particle.pos, event.getTickDelta());
            float bigSize = 4.0F * particle.size * (this.size.getCurrentValue() / 0.25F);
            ms.push();
            RenderUtility.prepareMatrices(ms, pos);
            ms.multiply(camera.getRotation());
            DrawUtility.drawImage(
               ms,
               builder,
               (double)(-bigSize / 2.0F),
               (double)(-bigSize / 2.0F),
               0.0,
               (double)bigSize,
               (double)bigSize,
               mainColor.withAlpha(255.0F * particle.alpha.getValue() * 0.4F)
            );
            ms.pop();
         }

         BuiltBuffer builtQuadBuffer = builder.endNullable();
         if (builtQuadBuffer != null) {
            BufferRenderer.drawWithGlobalProgram(builtQuadBuffer);
         }

         RenderSystem.depthMask(true);
         RenderSystem.setShaderTexture(0, 0);
         RenderSystem.disableBlend();
         RenderSystem.enableCull();
         RenderSystem.disableDepthTest();
         ms.pop();
      }

      // 2. Wireframe 3D model pass (строго DEBUG_LINES без сплошной заливки)
      RenderSystem.enableBlend();
      RenderSystem.disableDepthTest();
      RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
      RenderSystem.enableDepthTest();
      RenderSystem.disableCull();
      RenderSystem.depthMask(false);
      RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
      BufferBuilder linesBuffer = RenderSystem.renderThreadTesselator().begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

      WorldModel selectedModel = getSelectedModel();
      boolean isRandomMode = this.random.isSelected();
      float sizeMul = this.size.getCurrentValue() / 0.25F;

      for (World.Particle particle : this.particles) {
         particle.alpha.update(!particle.timer.finished(particle.liveTicks));
         Vec3d pos = Utils.getInterpolatedPos(particle.prev, particle.pos, event.getTickDelta());
         Vec3d rot = Utils.getInterpolatedPos(particle.prevRot, particle.rotate, event.getTickDelta());
         ms.push();
         ms.translate(pos.add(-cameraPos.getX(), -cameraPos.getY(), -cameraPos.getZ()));
         ms.multiply(new Quaternionf().rotationXYZ((float)rot.x, (float)rot.y, (float)rot.z));
         float particleScale = particle.size * sizeMul;
         ms.scale(particleScale, particleScale, particleScale);

         WorldModel currentModel = isRandomMode ? particle.modelType.getModel() : selectedModel;
         float a = baseAlpha * particle.alpha.getValue();
         WorldGeometry.renderModel(ms, linesBuffer, currentModel, r, g, b, a);
         ms.pop();
      }

      BuiltBuffer builtLinesBuffer = linesBuffer.endNullable();
      if (builtLinesBuffer != null) {
         BufferRenderer.drawWithGlobalProgram(builtLinesBuffer);
      }

      RenderSystem.depthMask(true);
      RenderSystem.defaultBlendFunc();
      RenderSystem.enableCull();
      RenderSystem.enableDepthTest();
      RenderSystem.disableBlend();
   };

   @Override
   public void tick() {
      if (mc.player == null) return;
      this.particles.removeIf(particlex -> particlex.alpha.getValue() == 0.0F && particlex.timer.finished(particlex.liveTicks));

      for (World.Particle particle : this.particles) {
         particle.tick();
      }

      int targetCount = (int) this.count.getCurrentValue();
      if (this.particles.size() < targetCount) {
         this.particles.add(
            new World.Particle(
               mc.player.getPos().add(MathUtility.random(-20.0, 20.0), MathUtility.random(0.0, 5.0), MathUtility.random(-20.0, 20.0)),
               Vec3d.ZERO,
               new Vec3d(MathUtility.random(-1.0, 1.0), MathUtility.random(0.0, 2.0), MathUtility.random(-1.0, 1.0)),
               new Vec3d(MathUtility.random(-1.0, 1.0), MathUtility.random(-1.0, 1.0), MathUtility.random(-1.0, 1.0)),
               (long)MathUtility.random(1500.0, 4500.0),
               MathUtility.random(0.18F, 0.35F),
               pickRandomModelType()
            )
         );
      }
   }

   private WorldModel getSelectedModel() {
      if (this.sword.isSelected()) return WorldModelType.SWORD.getModel();
      if (this.pickaxe.isSelected()) return WorldModelType.PICKAXE.getModel();
      if (this.crystal.isSelected()) return WorldModelType.CRYSTAL.getModel();
      if (this.heart.isSelected()) return WorldModelType.HEART.getModel();
      return WorldModelType.TOTEM.getModel();
   }

   private WorldModelType pickRandomModelType() {
      WorldModelType[] types = WorldModelType.values();
      return types[(int)(Math.random() * types.length)];
   }

   static class Particle {
      Vec3d prev;
      Vec3d prevRot;
      Vec3d pos;
      Vec3d rotate;
      Vec3d motion;
      Vec3d rotateMotion;
      final long liveTicks;
      float size;
      final WorldModelType modelType;
      final Timer timer = new Timer();
      final Animation alpha = new Animation(300L, Easing.FIGMA_EASE_IN_OUT);

      public Particle(Vec3d pos, Vec3d rotate, Vec3d motion, Vec3d rotateMotion, long liveTicks, float size, WorldModelType modelType) {
         this.pos = pos;
         this.rotate = rotate;
         this.motion = motion.multiply(0.04F);
         this.rotateMotion = rotateMotion.multiply(0.04F);
         this.liveTicks = liveTicks;
         this.size = size;
         this.modelType = modelType;
         this.prevRot = rotate;
         this.prev = pos;
         this.alpha.setDuration(1000L);
      }

      void tick() {
         this.prev = this.pos;
         this.prevRot = this.rotate;
         this.pos = this.pos.add(this.motion);
         this.rotate = this.rotate.add(this.rotateMotion);
         this.motion = this.motion.multiply(0.98);
         this.rotateMotion = this.rotateMotion.multiply(0.98);
      }
   }
}
