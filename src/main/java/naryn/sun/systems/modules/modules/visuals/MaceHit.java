package naryn.sun.systems.modules.modules.visuals;

import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import naryn.sun.systems.event.EventListener;
import naryn.sun.systems.event.impl.game.PostAttackEvent;
import naryn.sun.systems.event.impl.render.PreHudRenderEvent;
import naryn.sun.systems.event.impl.render.Render3DEvent;
import naryn.sun.systems.modules.api.ModuleCategory;
import naryn.sun.systems.modules.api.ModuleInfo;
import naryn.sun.systems.modules.impl.BaseModule;
import naryn.sun.systems.setting.settings.BooleanSetting;
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
import net.minecraft.item.Items;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

@ModuleInfo(name = "Mace Hit", category = ModuleCategory.VISUALS, desc = "modules.descriptions.mace_hit")
public class MaceHit extends BaseModule {

   // --- Настройки (Booleans then Sliders) ---
   private final BooleanSetting blackout = new BooleanSetting(this, "modules.settings.mace_hit.blackout").enable();
   private final BooleanSetting throughWalls = new BooleanSetting(this, "modules.settings.mace_hit.through_walls").enable();
   private final SliderSetting starSize = new SliderSetting(this, "modules.settings.mace_hit.star_size")
      .min(0.5F).max(3.0F).step(0.1F).currentValue(1.0F);
   private final SliderSetting minFall = new SliderSetting(this, "modules.settings.mace_hit.min_fall")
      .min(0.5F).max(5.0F).step(0.1F).currentValue(1.5F);

   // --- Состояние эффекта ---
   private boolean effectActive = false;
   private long effectStart = 0L;
   private int tier = 1;
   private float fallHeight = 0F;
   private Entity target;
   private Vec3d targetPos = Vec3d.ZERO;

   @Override
   public void onDisable() {
      this.effectActive = false;
      this.target = null;
   }

   // --- Реакция на удар ---
   private final EventListener<PostAttackEvent> onPostAttack = event -> {
      if (mc.player == null) return;
      if (!mc.player.getMainHandStack().isOf(Items.MACE)) return;
      if (mc.player.isOnGround()) return;
      float fall = mc.player.fallDistance;
      if (fall < this.minFall.getCurrentValue()) return;
      this.trigger(event.getEntity(), fall);
   };

   /** Вызывается при ударе булавой в падении. */
   public void trigger(Entity hitTarget, float fall) {
      this.target = hitTarget;
      this.fallHeight = fall;
      this.tier = tierFor(fall);
      this.effectStart = System.currentTimeMillis();
      this.effectActive = true;
      this.targetPos = hitTarget.getPos().add(0, hitTarget.getHeight() / 2.0, 0);
   }

   private static int tierFor(float fall) {
      if (fall >= 30F) return 5;
      if (fall >= 20F) return 4;
      if (fall >= 10F) return 3;
      if (fall >= 5F) return 2;
      return 1;
   }

   // --- Параметры тиров ---

   private long totalDurationMs() {
      return switch (this.tier) {
         case 1 -> 400L;
         case 2 -> 650L;
         case 3 -> 950L;
         case 4 -> 1250L;
         default -> 1700L;
      };
   }

   private long blackoutDurationMs() {
      return switch (this.tier) {
         case 1 -> 0L;
         case 2 -> 100L;
         case 3 -> 180L;
         case 4 -> 250L;
         default -> 350L;
      };
   }

   private long flashDurationMs() {
      return switch (this.tier) {
         case 1 -> 150L;
         case 2 -> 250L;
         case 3 -> 350L;
         case 4 -> 450L;
         default -> 600L;
      };
   }

   private int flashMaxAlpha() {
      return switch (this.tier) {
         case 1 -> 60;
         case 2 -> 110;
         case 3 -> 160;
         case 4 -> 200;
         default -> 235;
      };
   }

   private float tierScaleMult() {
      return switch (this.tier) {
         case 1 -> 1.0F;
         case 2 -> 1.35F;
         case 3 -> 1.8F;
         case 4 -> 2.3F;
         default -> 2.9F;
      };
   }

   private int ringCount() {
      return switch (this.tier) {
         case 3 -> 1;
         case 4 -> 2;
         case 5 -> 3;
         default -> 0;
      };
   }

   /**
    * ARGB-цвет полноэкранного оверлея.
    * Фаза 1: матовый чёрный blackout. Фаза 3: белая вспышка с затуханием.
    */
   private int getOverlayColor() {
      if (!this.effectActive || !this.isEnabled()) return 0;
      long e = System.currentTimeMillis() - this.effectStart;
      if (e >= this.totalDurationMs()) return 0;

      long bo = this.blackout.isEnabled() ? this.blackoutDurationMs() : 0L;
      if (bo > 0 && e < bo) {
         float in = e < 40 ? e / 40F : 1F;
         int a = (int) (240 * in);
         return (a << 24) | 0x050505;
      }

      long fe = e - bo;
      long fl = this.flashDurationMs();
      if (fe >= 0 && fe < fl) {
         float p = 1F - fe / (float) fl;
         int a = (int) (this.flashMaxAlpha() * p * p);
         if (a > 0) return (a << 24) | 0xFFFFFF;
      }
      return 0;
   }

   // --- Рендер оверлея (под интерфейсом, до отрисовки HUD-виджетов) ---
   private final EventListener<PreHudRenderEvent> onPreHudRender = event -> {
      int color = this.getOverlayColor();
      if (color == 0) return;

      event.getContext().drawRect(
         0, 0,
         event.getContext().getScaledWindowWidth(),
         event.getContext().getScaledWindowHeight(),
         ColorRGBA.fromInt(color)
      );
   };

   // --- Рендер мира: звезда + кольца ---
   private final EventListener<Render3DEvent> onRenderWorld = event -> {
      if (!this.effectActive || !this.isEnabled()) return;
      long elapsed = System.currentTimeMillis() - this.effectStart;
      if (elapsed >= this.totalDurationMs()) {
         this.effectActive = false;
         this.target = null;
         return;
      }

      MatrixStack ms = event.getMatrices();

      float tickDelta = event.getTickDelta();
      if (this.target != null && this.target.isAlive()) {
         this.targetPos = this.target.getLerpedPos(tickDelta).add(0, this.target.getHeight() / 2.0, 0);
      }

      Camera camera = event.getCamera();
      Vec3d cam = camera.getPos();

      ms.push();
      ms.translate(this.targetPos.x - cam.x, this.targetPos.y - cam.y, this.targetPos.z - cam.z);
      ms.multiply(camera.getRotation()); // billboard: плоскость всегда к камере
      Matrix4f m = ms.peek().getPositionMatrix();

      RenderSystem.enableBlend();
      RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE); // аддитивное свечение
      RenderSystem.disableCull();
      RenderSystem.depthMask(false);
      if (this.throughWalls.isEnabled()) {
         RenderSystem.disableDepthTest();
      } else {
         RenderSystem.enableDepthTest();
      }
      RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

      BufferBuilder buf = RenderSystem.renderThreadTesselator().begin(DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

      float size = this.starSize.getCurrentValue() * 0.55F * this.tierScaleMult() * (1.0F + Math.min(this.fallHeight, 40F) * 0.06F);

      float growMs = 120F;
      float scaleP = elapsed < growMs ? easeOutCubic(elapsed / growMs) : 1F;
      float fadeStart = this.totalDurationMs() * 0.55F;
      float alpha = elapsed > fadeStart
         ? 1F - (elapsed - fadeStart) / (this.totalDurationMs() - fadeStart)
         : 1F;
      alpha = clamp01(alpha);
      float s = size * scaleP;
      float rot = this.tier >= 5 ? elapsed * 0.04F : 0F;

      drawStar(m, buf, 4, s, alpha, rot, 0.10F);
      drawStar(m, buf, 4, s * 0.30F, alpha, rot + 45F, 0.45F);
      if (this.tier >= 3) drawStar(m, buf, 4, s * 0.60F, alpha * 0.85F, rot + 45F, 0.10F);
      if (this.tier >= 5) drawStar(m, buf, 4, s * 0.85F, alpha * 0.50F, rot * 2.2F + 22F, 0.07F);

      int rings = this.ringCount();
      long ringDelayBase = this.blackout.isEnabled() ? this.blackoutDurationMs() : 0L;
      for (int i = 0; i < rings; i++) {
         long re = elapsed - (ringDelayBase + i * 90L);
         long rl = 420L;
         if (re > 0 && re < rl) {
            float p = re / (float) rl;
            float radius = 0.3F + easeOutCubic(p) * size * 3.0F;
            float ra = (1F - p) * 0.9F;
            drawRing(m, buf, radius, 0.10F + size * 0.05F, ra);
         }
      }

      BuiltBuffer mesh = buf.endNullable();
      if (mesh != null) BufferRenderer.drawWithGlobalProgram(mesh);

      RenderSystem.depthMask(true);
      RenderSystem.enableCull();
      RenderSystem.disableDepthTest();
      RenderSystem.disableBlend();
      ms.pop();
   };

   /** Четырёхконечная "искра": лучи с прозрачными кончиками. */
   private static void drawStar(Matrix4f m, BufferBuilder buf, int points, float size, float alpha, float rotDeg, float widthFactor) {
      float a = clamp01(alpha);
      if (a <= 0F) return;
      float baseHalf = size * widthFactor;
      for (int i = 0; i < points; i++) {
         double ang = Math.toRadians(rotDeg + (360.0 / points) * i);
         float tx = (float) (Math.cos(ang) * size);
         float ty = (float) (Math.sin(ang) * size);
         float px = (float) (Math.cos(ang + Math.PI / 2.0) * baseHalf);
         float py = (float) (Math.sin(ang + Math.PI / 2.0) * baseHalf);
         buf.vertex(m, px, py, 0F).color(1F, 1F, 1F, a);
         buf.vertex(m, -px, -py, 0F).color(1F, 1F, 1F, a);
         buf.vertex(m, tx, ty, 0F).color(1F, 1F, 1F, 0F);
      }
   }

   /** Кольцо с яркой внутренней кромкой и прозрачной внешней. */
   private static void drawRing(Matrix4f m, BufferBuilder buf, float radius, float width, float alpha) {
      float a = clamp01(alpha);
      if (a <= 0F) return;
      int segs = 48;
      for (int i = 0; i < segs; i++) {
         double a1 = Math.PI * 2.0 * i / segs;
         double a2 = Math.PI * 2.0 * (i + 1) / segs;
         float c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
         float c2 = (float) Math.cos(a2), s2 = (float) Math.sin(a2);
         float ir = radius - width, or = radius + width;
         float ix1 = c1 * ir, iy1 = s1 * ir, ox1 = c1 * or, oy1 = s1 * or;
         float ix2 = c2 * ir, iy2 = s2 * ir, ox2 = c2 * or, oy2 = s2 * or;

         buf.vertex(m, ix1, iy1, 0F).color(1F, 1F, 1F, a);
         buf.vertex(m, ox1, oy1, 0F).color(1F, 1F, 1F, 0F);
         buf.vertex(m, ox2, oy2, 0F).color(1F, 1F, 1F, 0F);

         buf.vertex(m, ix1, iy1, 0F).color(1F, 1F, 1F, a);
         buf.vertex(m, ox2, oy2, 0F).color(1F, 1F, 1F, 0F);
         buf.vertex(m, ix2, iy2, 0F).color(1F, 1F, 1F, a);
      }
   }

   private static float easeOutCubic(float p) {
      float q = 1F - clamp01(p);
      return 1F - q * q * q;
   }

   private static float clamp01(float v) {
      return Math.max(0F, Math.min(1F, v));
   }
}