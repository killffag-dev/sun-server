package naryn.sun.systems.modules.modules.visuals;

import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import naryn.sun.Sun;
import naryn.sun.access.MCPlayerAccess;
import naryn.sun.access.MCWorldAccess;
import naryn.sun.systems.event.EventListener;
import naryn.sun.systems.event.impl.player.ClientPlayerTickEvent;
import naryn.sun.systems.event.impl.render.Render3DEvent;
import naryn.sun.systems.modules.api.ModuleCategory;
import naryn.sun.systems.modules.api.ModuleInfo;
import naryn.sun.systems.modules.impl.BaseModule;
import naryn.sun.systems.setting.settings.BooleanSetting;
import naryn.sun.systems.setting.settings.ColorSetting;
import naryn.sun.systems.setting.settings.GroupSetting;
import naryn.sun.systems.setting.settings.ModeSetting;
import naryn.sun.systems.setting.settings.SliderSetting;
import naryn.sun.utility.colors.ColorRGBA;
import naryn.sun.utility.colors.Colors;
import naryn.sun.utility.render.DrawUtility;
import naryn.sun.utility.render.RenderUtility;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

@ModuleInfo(name = "Jump Circle", category = ModuleCategory.VISUALS, desc = "Рисует эффект в точке прыжка")
public class JumpCircle extends BaseModule {

   private static final Identifier BLOOM_ID = Sun.id("textures/bloom.png");
   private static final int MAX_EFFECTS = 64;

   // --- Top-Level: Режим эффекта ---
   private final ModeSetting mode = new ModeSetting(this, "modules.settings.jump_circle.mode");
   private final ModeSetting.Value ripple = new ModeSetting.Value(this.mode, "modules.settings.jump_circle.mode.ripple").select();
   private final ModeSetting.Value shockwave = new ModeSetting.Value(this.mode, "modules.settings.jump_circle.mode.shockwave");
   private final ModeSetting.Value magicCircle = new ModeSetting.Value(this.mode, "modules.settings.jump_circle.mode.magic_circle");

   // --- Box 1: Основные ---
   private final GroupSetting generalGroup = new GroupSetting(this, "modules.settings.jump_circle.group.general");
   private final BooleanSetting showOthers = new BooleanSetting(this.generalGroup, "modules.settings.jump_circle.show_others").enabled(false);
   private final SliderSetting duration = new SliderSetting(this.generalGroup, "modules.settings.jump_circle.duration")
      .min(150.0f).max(2500.0f).step(10.0f).currentValue(800.0f);

   // --- Box 2: Внешний вид ---
   private final GroupSetting appearanceGroup = new GroupSetting(this, "modules.settings.jump_circle.group.appearance");
   private final ColorSetting color = new ColorSetting(this.appearanceGroup, "modules.settings.jump_circle.color").color(Colors.ACCENT);
   private final SliderSetting maxRadius = new SliderSetting(this.appearanceGroup, "modules.settings.jump_circle.max_radius")
      .min(0.2f).max(2.5f).step(0.05f).currentValue(0.8f);
   private final SliderSetting thickness = new SliderSetting(this.appearanceGroup, "modules.settings.jump_circle.thickness")
      .min(0.01f).max(0.5f).step(0.01f).currentValue(0.06f);
   private final SliderSetting glowSize = new SliderSetting(this.appearanceGroup, "modules.settings.jump_circle.glow_size")
      .min(0.1f).max(1.5f).step(0.05f).currentValue(0.35f);
   private final SliderSetting glowIntensity = new SliderSetting(this.appearanceGroup, "modules.settings.jump_circle.glow_intensity")
      .min(0.0f).max(3.0f).step(0.1f).currentValue(1.2f);

   private final List<JumpEffect> effects = new ArrayList<>();
   private boolean wasOnGround = true;
   private double lastGroundX;
   private double lastGroundY;
   private double lastGroundZ;

   private final it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet<PlayerEntity> otherWasOnGround = new it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet<>();
   private final it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap<PlayerEntity, double[]> otherLastGroundPos = new it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap<>();

   private final EventListener<ClientPlayerTickEvent> onPlayerTick = event -> {
      if (!MCPlayerAccess.isPresent()) {
         return;
      }

      boolean onGround = MCPlayerAccess.isOnGround();
      if (onGround) {
         this.lastGroundX = MCPlayerAccess.getX();
         this.lastGroundY = MCPlayerAccess.getY();
         this.lastGroundZ = MCPlayerAccess.getZ();
      }

      if (this.wasOnGround && !onGround && MCPlayerAccess.getVelocity().y > 0.0) {
         this.spawnJumpAt(new Vec3d(this.lastGroundX, this.lastGroundY, this.lastGroundZ));
      }

      this.wasOnGround = onGround;

      if (this.showOthers.isEnabled() && MCWorldAccess.isPresent()) {
         for (PlayerEntity player : MCWorldAccess.getPlayers()) {
            if (player == MCPlayerAccess.get()) {
               continue;
            }

            boolean playerOnGround = player.isOnGround();
            double[] lastGround = this.otherLastGroundPos.computeIfAbsent(
               player, p -> new double[]{player.getX(), player.getY(), player.getZ()}
            );

            if (playerOnGround) {
               lastGround[0] = player.getX();
               lastGround[1] = player.getY();
               lastGround[2] = player.getZ();
            }

            boolean prevOnGround = this.otherWasOnGround.contains(player);
            if (prevOnGround && !playerOnGround && player.getVelocity().y > 0.0) {
               this.spawnJumpAt(new Vec3d(lastGround[0], lastGround[1], lastGround[2]));
            }

            if (playerOnGround) {
               this.otherWasOnGround.add(player);
            } else {
               this.otherWasOnGround.remove(player);
            }
         }

         it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet<PlayerEntity> currentPlayers = new it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet<>(MCWorldAccess.getPlayers());
         this.otherWasOnGround.retainAll(currentPlayers);
         this.otherLastGroundPos.keySet().retainAll(currentPlayers);
      }
   };

   private final EventListener<Render3DEvent> onRender3D = event -> {
      if (this.effects.isEmpty() || !MCPlayerAccess.isPresent()) {
         return;
      }

      MatrixStack ms = event.getMatrices();
      Camera camera = event.getCamera();
      Vec3d camPos = camera.getPos();
      ColorRGBA col = this.color.getColor();
      float rr = col.getRed() / 255.0F;
      float gg = col.getGreen() / 255.0F;
      float bb = col.getBlue() / 255.0F;

      long now = System.currentTimeMillis();
      float durationMs = this.duration.getCurrentValue();
      float maxR = this.maxRadius.getCurrentValue();
      float thick = this.thickness.getCurrentValue();
      float gSize = this.glowSize.getCurrentValue();
      float gIntensity = this.glowIntensity.getCurrentValue();

      RenderSystem.enableBlend();
      RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
      RenderSystem.enableDepthTest();
      RenderSystem.disableCull();
      RenderSystem.depthMask(false);

      Iterator<JumpEffect> it = this.effects.iterator();
      while (it.hasNext()) {
         JumpEffect fx = it.next();
         long age = now - fx.spawnTime - fx.startDelay;
         if (age < 0L) {
            continue;
         }

         float progress = age / durationMs;
         if (progress >= 1.0F) {
            it.remove();
            continue;
         }

         float fade = 1.0F - progress;
         float radius = maxR * progress;
         float alpha = fade * (col.getAlpha() / 255.0F);
         if (this.shockwave.isSelected()) {
            alpha *= 1.0F - fx.pulseIndex * 0.25F;
         }

         if (this.magicCircle.isSelected()) {
            this.renderMagicCircle(ms, camera, fx.pos, camPos, maxR, progress, alpha, gSize, gIntensity, rr, gg, bb, col);
         } else {
            ms.push();
            ms.translate((float)(fx.pos.x - camPos.x), (float)(fx.pos.y - camPos.y) + 0.02F, (float)(fx.pos.z - camPos.z));
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
            this.drawRing(ms, Math.max(0.0F, radius - thick), radius, 40, rr, gg, bb, alpha);
            ms.pop();

            RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
            this.drawGlowRing(ms, camera, fx.pos, radius, 20, gSize, col, alpha * gIntensity * 0.6F);
         }
      }

      RenderSystem.depthMask(true);
      RenderSystem.setShaderTexture(0, 0);
      RenderSystem.enableCull();
      RenderSystem.disableDepthTest();
      RenderSystem.disableBlend();
   };

   // ===================== MAGIC CIRCLE =====================
   // Слой 1: статичная гексаграмма в центре (не крутится)
   // Слой 2: внутренняя насечка — крутится влево
   // Слой 3: внешняя насечка — крутится вправо
   private void renderMagicCircle(
      MatrixStack ms, Camera camera, Vec3d worldPos, Vec3d camPos, float maxR, float progress,
      float baseAlpha, float gSize, float gIntensity, float r, float g, float b, ColorRGBA col
   ) {
      float appear = Math.min(1.0F, progress / 0.25F);
      float fadeOut = progress > 0.75F ? (1.0F - progress) / 0.25F : 1.0F;
      float seal = Math.max(0.0F, Math.min(appear, fadeOut));
      float alpha = seal * (col.getAlpha() / 255.0F);

      float outerR = maxR;
      float innerR = maxR * 0.6F;
      float starR = maxR * 0.42F;

      float rotLeft = progress * (float)(Math.PI * 2.0);
      float rotRight = -progress * (float)(Math.PI * 2.6);

      ms.push();
      ms.translate((float)(worldPos.x - camPos.x), (float)(worldPos.y - camPos.y) + 0.02F, (float)(worldPos.z - camPos.z));
      RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

      this.drawStar(ms, starR, r, g, b, alpha);
      this.drawCircleOutline(ms, innerR, 48, r, g, b, alpha * 0.8F);
      this.drawArcs(ms, innerR, innerR * 1.06F, 8, 0.5F, rotLeft, r, g, b, alpha);
      this.drawArcs(ms, outerR * 0.94F, outerR, 12, 0.4F, rotRight, r, g, b, alpha);
      this.drawCircleOutline(ms, outerR, 64, r, g, b, alpha * 0.8F);

      ms.pop();

      RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
      this.drawGlowRing(ms, camera, worldPos, outerR, 18, gSize, col, alpha * gIntensity * 0.5F);
      this.drawGlowPoints(ms, camera, worldPos, 0.0F, 1, gSize * 1.4F, col, alpha * gIntensity * 0.9F, 0.0F, false);
   }

   private void spawnJumpAt(Vec3d pos) {
      long now = System.currentTimeMillis();

      while (this.effects.size() >= MAX_EFFECTS) {
         this.effects.remove(0);
      }

      if (this.shockwave.isSelected()) {
         for (int i = 0; i < 3; i++) {
            this.effects.add(new JumpEffect(pos, now, i * 120L, i));
         }
      } else {
         this.effects.add(new JumpEffect(pos, now, 0L, 0));
      }
   }

   // ===================== ПРИМИТИВЫ =====================

   private void drawRing(MatrixStack ms, float innerRadius, float outerRadius, int segments, float r, float g, float b, float alpha) {
      Matrix4f matrix = ms.peek().getPositionMatrix();
      BufferBuilder buf = RenderSystem.renderThreadTesselator().begin(DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
      for (int i = 0; i <= segments; i++) {
         float angle = (float)(i * 2.0 * Math.PI / segments);
         float cos = (float)Math.cos(angle);
         float sin = (float)Math.sin(angle);
         buf.vertex(matrix, cos * outerRadius, 0.0F, sin * outerRadius).color(r, g, b, alpha);
         buf.vertex(matrix, cos * innerRadius, 0.0F, sin * innerRadius).color(r, g, b, alpha);
      }
      BufferRenderer.drawWithGlobalProgram(buf.end());
   }

   private void drawArcs(
      MatrixStack ms, float innerRadius, float outerRadius, int arcCount, float coverage, float rotationOffset, float r, float g, float b, float alpha
   ) {
      Matrix4f matrix = ms.peek().getPositionMatrix();
      int segmentsPerArc = 8;
      float slice = (float)(2.0 * Math.PI / arcCount);
      float arcAngle = slice * coverage;

      BufferBuilder buf = RenderSystem.renderThreadTesselator().begin(DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
      for (int a = 0; a < arcCount; a++) {
         float startAngle = rotationOffset + a * slice;
         for (int i = 0; i < segmentsPerArc; i++) {
            float a1 = startAngle + arcAngle * i / segmentsPerArc;
            float a2 = startAngle + arcAngle * (i + 1) / segmentsPerArc;
            float c1 = (float)Math.cos(a1), s1 = (float)Math.sin(a1);
            float c2 = (float)Math.cos(a2), s2 = (float)Math.sin(a2);

            float x1_out = c1 * outerRadius, z1_out = s1 * outerRadius;
            float x1_in = c1 * innerRadius, z1_in = s1 * innerRadius;
            float x2_out = c2 * outerRadius, z2_out = s2 * outerRadius;
            float x2_in = c2 * innerRadius, z2_in = s2 * innerRadius;

            buf.vertex(matrix, x1_out, 0.0F, z1_out).color(r, g, b, alpha);
            buf.vertex(matrix, x1_in, 0.0F, z1_in).color(r, g, b, alpha);
            buf.vertex(matrix, x2_out, 0.0F, z2_out).color(r, g, b, alpha);

            buf.vertex(matrix, x2_out, 0.0F, z2_out).color(r, g, b, alpha);
            buf.vertex(matrix, x1_in, 0.0F, z1_in).color(r, g, b, alpha);
            buf.vertex(matrix, x2_in, 0.0F, z2_in).color(r, g, b, alpha);
         }
      }
      BufferRenderer.drawWithGlobalProgram(buf.end());
   }

   private void drawCircleOutline(MatrixStack ms, float radius, int segments, float r, float g, float b, float alpha) {
      Matrix4f matrix = ms.peek().getPositionMatrix();
      BufferBuilder buf = RenderSystem.renderThreadTesselator().begin(DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
      for (int i = 0; i <= segments; i++) {
         float angle = (float)(i % segments) * (float)(2.0 * Math.PI / segments);
         float x = (float)Math.cos(angle) * radius;
         float z = (float)Math.sin(angle) * radius;
         buf.vertex(matrix, x, 0.0F, z).color(r, g, b, alpha);
      }
      BufferRenderer.drawWithGlobalProgram(buf.end());
   }

   private void drawStar(MatrixStack ms, float radius, float r, float g, float b, float alpha) {
      Matrix4f matrix = ms.peek().getPositionMatrix();
      for (int t = 0; t < 2; t++) {
         float rotOffset = t == 0 ? 0.0F : (float)Math.PI;
         BufferBuilder buf = RenderSystem.renderThreadTesselator().begin(DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
         for (int i = 0; i <= 3; i++) {
            float angle = rotOffset - (float)(Math.PI / 2.0) + (i % 3) * (float)(2.0 * Math.PI / 3.0);
            float x = (float)Math.cos(angle) * radius;
            float z = (float)Math.sin(angle) * radius;
            buf.vertex(matrix, x, 0.0F, z).color(r, g, b, alpha);
         }
         BufferRenderer.drawWithGlobalProgram(buf.end());
      }
   }

   private void drawGlowRing(MatrixStack ms, Camera camera, Vec3d center, float radius, int count, float spriteSize, ColorRGBA color, float baseAlpha) {
      this.drawGlowPoints(ms, camera, center, radius, count, spriteSize, color, baseAlpha, 0.0F, false);
   }

   private void drawGlowPoints(
      MatrixStack ms, Camera camera, Vec3d center, float radius, int count, float spriteSize, ColorRGBA color, float baseAlpha, float rotationPhase, boolean comet
   ) {
      RenderSystem.setShaderTexture(0, BLOOM_ID);
      RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
      BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

      int cr = (int) color.getRed();
      int cg = (int) color.getGreen();
      int cb = (int) color.getBlue();

      for (int i = 0; i < count; i++) {
         float angle = (float)(i * 2.0 * Math.PI / count) + rotationPhase;
         float px = (float)Math.cos(angle) * radius;
         float pz = (float)Math.sin(angle) * radius;

         float alpha = baseAlpha;
         if (comet) {
            float head = 0.5F + 0.5F * (float)Math.cos(angle - rotationPhase);
            alpha *= 0.1F + 0.9F * head;
         }

         float clampedAlpha = Math.max(0.0F, Math.min(1.0F, alpha));
         int ca = (int) (255.0F * clampedAlpha);
         int argb = ((ca & 0xFF) << 24) | ((cr & 0xFF) << 16) | ((cg & 0xFF) << 8) | (cb & 0xFF);

         ms.push();
         RenderUtility.prepareMatrices(ms, center.x + px, center.y + 0.02, center.z + pz);
         ms.multiply(camera.getRotation());
         DrawUtility.drawImage(
            ms,
            builder,
            (double)(-spriteSize / 2.0F),
            (double)(-spriteSize / 2.0F),
            0.0,
            (double)spriteSize,
            (double)spriteSize,
            argb
         );
         ms.pop();
      }

      BuiltBuffer built = builder.endNullable();
      if (built != null) {
         BufferRenderer.drawWithGlobalProgram(built);
      }
   }

   @Override
   public void onDisable() {
      super.onDisable();
      this.effects.clear();
      this.wasOnGround = true;
      this.otherWasOnGround.clear();
      this.otherLastGroundPos.clear();
   }

   @Override
   public void tick() {
      super.tick();
   }

   private static class JumpEffect {
      final Vec3d pos;
      final long spawnTime;
      final long startDelay;
      final int pulseIndex;

      JumpEffect(Vec3d pos, long spawnTime, long startDelay, int pulseIndex) {
         this.pos = pos;
         this.spawnTime = spawnTime;
         this.startDelay = startDelay;
         this.pulseIndex = pulseIndex;
      }
   }
}
