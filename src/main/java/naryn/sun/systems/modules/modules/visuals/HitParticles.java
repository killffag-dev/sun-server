package naryn.sun.systems.modules.modules.visuals;

import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import naryn.sun.systems.event.EventListener;
import naryn.sun.systems.event.impl.game.EntityDamageEvent;
import naryn.sun.systems.event.impl.player.ClientPlayerTickEvent;
import naryn.sun.systems.event.impl.render.Render3DEvent;
import naryn.sun.systems.modules.api.ModuleCategory;
import naryn.sun.systems.modules.api.ModuleInfo;
import naryn.sun.systems.modules.impl.BaseModule;
import naryn.sun.systems.modules.modules.visuals.particle.HitParticle;
import naryn.sun.systems.modules.modules.visuals.particle.HitParticleRenderer;
import naryn.sun.systems.setting.settings.BooleanSetting;
import naryn.sun.systems.setting.settings.ColorSetting;
import naryn.sun.systems.setting.settings.ModeSetting;
import naryn.sun.systems.setting.settings.SliderSetting;
import naryn.sun.utility.colors.ColorRGBA;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

@ModuleInfo(name = "Particles", category = ModuleCategory.VISUALS, desc = "modules.descriptions.particles")
public class HitParticles extends BaseModule {

   private static final int MAX_PARTICLES = 300;

   // --- 1. Modes First (Top-Level Root) ---
   private final ModeSetting shape = new ModeSetting(this, "modules.settings.particles.shape");
   private final ModeSetting.Value stars = new ModeSetting.Value(this.shape, "modules.settings.particles.shape.stars").select();
   private final ModeSetting.Value hearts = new ModeSetting.Value(this.shape, "modules.settings.particles.shape.hearts");
   private final ModeSetting.Value orbs = new ModeSetting.Value(this.shape, "modules.settings.particles.shape.orbs");
   private final ModeSetting.Value rhombs = new ModeSetting.Value(this.shape, "modules.settings.particles.shape.rhombs");
   private final ModeSetting.Value sun = new ModeSetting.Value(this.shape, "modules.settings.particles.shape.sun");
   private final ModeSetting.Value shuriken = new ModeSetting.Value(this.shape, "modules.settings.particles.shape.shuriken");
   private final ModeSetting.Value snowflake = new ModeSetting.Value(this.shape, "modules.settings.particles.shape.snowflake");
   private final ModeSetting.Value sakura = new ModeSetting.Value(this.shape, "modules.settings.particles.shape.sakura");
   private final ModeSetting.Value randomMode = new ModeSetting.Value(this.shape, "modules.settings.particles.shape.random");

   // --- 2. General & Logic Box ---
   private final naryn.sun.systems.setting.settings.GroupSetting generalGroup = new naryn.sun.systems.setting.settings.GroupSetting(this, "modules.settings.particles.group.general");
   private final BooleanSetting gravity = new BooleanSetting(this.generalGroup, "modules.settings.particles.gravity").enable();
   private final BooleanSetting physics = new BooleanSetting(this.generalGroup, "modules.settings.particles.physics").enable();
   private final BooleanSetting glow = new BooleanSetting(this.generalGroup, "modules.settings.particles.glow").enable();
   private final BooleanSetting throughWalls = new BooleanSetting(this.generalGroup, "modules.settings.particles.through_walls").enabled(false);
   private final SliderSetting amount = new SliderSetting(this.generalGroup, "modules.settings.particles.amount")
      .min(5.0F).max(40.0F).step(1.0F).currentValue(16.0F);
   private final SliderSetting size = new SliderSetting(this.generalGroup, "modules.settings.particles.size")
      .min(0.05F).max(0.40F).step(0.01F).currentValue(0.15F);
   private final SliderSetting speed = new SliderSetting(this.generalGroup, "modules.settings.particles.speed")
      .min(0.05F).max(0.60F).step(0.01F).currentValue(0.22F);
   private final SliderSetting lifetime = new SliderSetting(this.generalGroup, "modules.settings.particles.lifetime")
      .min(300.0F).max(2000.0F).step(50.0F).currentValue(850.0F);

   // --- 3. Colors at the Very Bottom ---
   private final naryn.sun.systems.setting.settings.GroupSetting colorGroup = new naryn.sun.systems.setting.settings.GroupSetting(this, "modules.settings.particles.group.color");
   private final ModeSetting colorMode = new ModeSetting(this.colorGroup, "modules.settings.particles.color_mode");
   private final ModeSetting.Value customColor = new ModeSetting.Value(this.colorMode, "modules.settings.particles.color_mode.custom").select();
   private final ModeSetting.Value rainbowColor = new ModeSetting.Value(this.colorMode, "modules.settings.particles.color_mode.rainbow");
   private final ColorSetting color = new ColorSetting(this.colorGroup, "modules.settings.particles.color", () -> !this.customColor.isSelected())
      .color(new ColorRGBA(255.0F, 120.0F, 210.0F, 255.0F));

   // --- Список активных частиц (потокобезопасная очередь для спавна + синхронизированный список) ---
   private static final long HIT_DEBOUNCE_MS = 400L;
   private final java.util.Map<Integer, Long> lastSpawnTimes = new java.util.concurrent.ConcurrentHashMap<>();
   private final Queue<HitParticle> pendingParticles = new ConcurrentLinkedQueue<>();
   private final List<HitParticle> particles = new ArrayList<>();
   private final Random random = new Random();
   private int particleCounter = 0;

   @Override
   public void onDisable() {
      this.lastSpawnTimes.clear();
      this.pendingParticles.clear();
      synchronized (this.particles) {
         this.particles.clear();
      }
   }

   // --- Спавн частиц только при получении сущностью реального урона (не срабатывает при блоке щитом) ---
   private final EventListener<EntityDamageEvent> onEntityDamage = event -> {
      if (event.getEntity() != null) {
         this.spawnParticles(event.getEntity());
      }
   };

   public void spawnParticles(Entity target) {
      if (!this.isEnabled()) return;
      if (mc.player == null || mc.world == null || target == null) return;
      if (target.isRemoved()) return;
      if (target == mc.player) return;
      if (mc.player.squaredDistanceTo(target) > 48.0 * 48.0) return;

      if (target instanceof net.minecraft.entity.LivingEntity living) {
         if (living.isDead() && living.deathTime > 5) return;
         if (living.isBlocking() && this.isShieldBlocked(living, mc.player)) return;
      }

      int targetId = target.getId();
      long now = System.currentTimeMillis();
      Long lastSpawn = this.lastSpawnTimes.get(targetId);
      if (lastSpawn != null && now - lastSpawn < HIT_DEBOUNCE_MS) {
         return;
      }
      this.lastSpawnTimes.put(targetId, now);

      Box box = target.getBoundingBox();
      Vec3d center = box.getCenter();
      int count = (int) this.amount.getCurrentValue();
      float baseSpeed = this.speed.getCurrentValue();
      float baseSize = this.size.getCurrentValue();
      long baseLife = (long) this.lifetime.getCurrentValue();
      boolean isRandomShape = this.randomMode.isSelected();

      for (int i = 0; i < count; i++) {
         HitParticleRenderer.Shape pShape;
         if (isRandomShape) {
            pShape = HitParticleRenderer.getRandomShape(this.random);
         } else if (this.hearts.isSelected()) {
            pShape = HitParticleRenderer.Shape.HEARTS;
         } else if (this.orbs.isSelected()) {
            pShape = HitParticleRenderer.Shape.ORBS;
         } else if (this.rhombs.isSelected()) {
            pShape = HitParticleRenderer.Shape.RHOMBS;
         } else if (this.sun.isSelected()) {
            pShape = HitParticleRenderer.Shape.SUN;
         } else if (this.shuriken.isSelected()) {
            pShape = HitParticleRenderer.Shape.SHURIKEN;
         } else if (this.snowflake.isSelected()) {
            pShape = HitParticleRenderer.Shape.SNOWFLAKE;
         } else if (this.sakura.isSelected()) {
            pShape = HitParticleRenderer.Shape.SAKURA;
         } else {
            pShape = HitParticleRenderer.Shape.STARS;
         }

         double offsetX = (this.random.nextDouble() - 0.5) * box.getLengthX() * 0.6;
         double offsetY = (this.random.nextDouble() - 0.5) * box.getLengthY() * 0.6;
         double offsetZ = (this.random.nextDouble() - 0.5) * box.getLengthZ() * 0.6;

         double angle = this.random.nextDouble() * Math.PI * 2.0;
         double elevation = (this.random.nextDouble() - 0.25) * Math.PI * 0.5;
         double spd = baseSpeed * (0.6 + this.random.nextDouble() * 0.8);

         double mx = Math.cos(angle) * Math.cos(elevation) * spd;
         double my = (Math.sin(elevation) + 0.3) * spd * 1.25;
         double mz = Math.sin(angle) * Math.cos(elevation) * spd;

         float rot = this.random.nextFloat() * 360.0F;
         float rotSpd = (this.random.nextFloat() - 0.5F) * 14.0F;
         long life = (long) (baseLife * (0.75 + this.random.nextDouble() * 0.5));
         float s = baseSize * (0.8F + this.random.nextFloat() * 0.4F);

         this.pendingParticles.add(new HitParticle(
            this.particleCounter++,
            pShape,
            center.x + offsetX,
            center.y + offsetY,
            center.z + offsetZ,
            mx, my, mz,
            rot, rotSpd, life, s
         ));
      }
   }

   // --- Обновление физики частиц ---
   private final EventListener<ClientPlayerTickEvent> onTick = event -> {
      if (mc.world == null) return;

      HitParticle pending;
      while ((pending = this.pendingParticles.poll()) != null) {
         synchronized (this.particles) {
            if (this.particles.size() >= MAX_PARTICLES) {
               this.particles.removeFirst();
            }
            this.particles.add(pending);
         }
      }

      synchronized (this.particles) {
         if (this.particles.isEmpty()) return;

         long now = System.currentTimeMillis();
         if (!this.lastSpawnTimes.isEmpty() && (now % 2000L < 50L)) {
            this.lastSpawnTimes.entrySet().removeIf(e -> now - e.getValue() > 3000L);
         }
         boolean grav = this.gravity.isEnabled();
         boolean phys = this.physics.isEnabled();

         Iterator<HitParticle> it = this.particles.iterator();
         while (it.hasNext()) {
            HitParticle p = it.next();
            if (p.isDead(now)) {
               it.remove();
               continue;
            }
            p.tick(grav, phys, mc.world);
         }
      }
   };

   // --- Отрисовка частиц ---
   private final EventListener<Render3DEvent> onRender3D = event -> {
      synchronized (this.particles) {
         if (this.particles.isEmpty()) return;

         Camera camera = event.getCamera();
         Vec3d camPos = camera.getPos();
         MatrixStack ms = event.getMatrices();
         long now = System.currentTimeMillis();
         float tickDelta = event.getTickDelta();

         RenderSystem.enableBlend();
         if (this.glow.isEnabled()) {
            RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
         } else {
            RenderSystem.defaultBlendFunc();
         }

         RenderSystem.disableCull();
         RenderSystem.depthMask(false);
         if (this.throughWalls.isEnabled()) {
            RenderSystem.disableDepthTest();
         } else {
            RenderSystem.enableDepthTest();
         }

         RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
         BufferBuilder buf = RenderSystem.renderThreadTesselator().begin(DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

         boolean isRainbow = this.rainbowColor.isSelected();
         ColorRGBA baseCol = this.color.getColor();

         for (HitParticle p : this.particles) {
            float progress = p.getProgress(now);
            if (progress >= 1.0F) continue;

            float alpha = 1.0F - progress;
            float scale = p.getSize() * (1.0F - progress * 0.45F);

            float r;
            float g;
            float b;
            if (isRainbow) {
               float hue = ((now + p.getId() * 120L) % 3600L) / 3600.0F;
               int rgb = java.awt.Color.HSBtoRGB(hue, 0.8F, 1.0F);
               r = ((rgb >> 16) & 0xFF) / 255.0F;
               g = ((rgb >> 8) & 0xFF) / 255.0F;
               b = (rgb & 0xFF) / 255.0F;
            } else {
               r = baseCol.getRed() / 255.0F;
               g = baseCol.getGreen() / 255.0F;
               b = baseCol.getBlue() / 255.0F;
            }

            double px = p.getInterpolatedX(tickDelta) - camPos.x;
            double py = p.getInterpolatedY(tickDelta) - camPos.y;
            double pz = p.getInterpolatedZ(tickDelta) - camPos.z;

            ms.push();
            ms.translate(px, py, pz);
            ms.multiply(camera.getRotation());
            ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(p.getRotation()));

            HitParticleRenderer.renderParticle(ms.peek().getPositionMatrix(), buf, p.getShape(), scale, r, g, b, alpha);

            ms.pop();
         }

         BuiltBuffer mesh = buf.endNullable();
         if (mesh != null) {
            BufferRenderer.drawWithGlobalProgram(mesh);
         }

         RenderSystem.depthMask(true);
         RenderSystem.enableCull();
         RenderSystem.enableDepthTest();
         RenderSystem.defaultBlendFunc();
         RenderSystem.disableBlend();
      }
   };

   private boolean isShieldBlocked(net.minecraft.entity.LivingEntity target, net.minecraft.entity.player.PlayerEntity attacker) {
      if (!target.isBlocking()) return false;
      Vec3d targetFacing = target.getRotationVector();
      Vec3d toAttacker = attacker.getPos().subtract(target.getPos()).normalize();
      double dot = targetFacing.x * toAttacker.x + targetFacing.z * toAttacker.z;
      return dot > 0.0;
   }
}
