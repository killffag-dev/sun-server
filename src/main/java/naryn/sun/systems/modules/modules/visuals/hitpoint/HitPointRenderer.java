package naryn.sun.systems.modules.modules.visuals.hitpoint;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

public final class HitPointRenderer {

   private HitPointRenderer() {}

   public static void renderMagicCircle(
      MatrixStack ms, float progress, float maxR, float r, float g, float b, float baseAlpha, boolean glow
   ) {
      float appear = Math.min(1.0F, progress / 0.25F);
      float fadeOut = progress > 0.70F ? (1.0F - progress) / 0.30F : 1.0F;
      float alpha = Math.max(0.0F, Math.min(appear, fadeOut)) * baseAlpha;
      if (alpha <= 0.001F) return;

      float popScale = easeOutBack(Math.min(1.0F, progress / 0.30F));
      ms.push();
      ms.scale(popScale, popScale, popScale);

      Matrix4f matrix = ms.peek().getPositionMatrix();
      RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

      // Компактный и аккуратный размер для магического круга (0.65 от maxR)
      float outerR = maxR * 0.65F;
      float innerR = outerR * 0.62F;
      float starR = outerR * 0.40F;

      float rotLeft = progress * (float) (Math.PI * 2.5);
      float rotRight = -progress * (float) (Math.PI * 3.2);

      drawStarXY(matrix, starR, 0.0F, r, g, b, alpha * 0.9F);
      drawCircleOutlineXY(matrix, innerR, 36, r, g, b, alpha * 0.75F);
      drawArcsXY(matrix, innerR, innerR * 1.08F, 8, 0.50F, rotLeft, r, g, b, alpha);
      drawArcsXY(matrix, outerR * 0.92F, outerR, 12, 0.42F, rotRight, r, g, b, alpha);
      drawCircleOutlineXY(matrix, outerR, 44, r, g, b, alpha * 0.85F);
      drawDiamondSparkXY(matrix, starR * 0.35F, starR * 0.35F, r, g, b, alpha);

      if (glow) {
         drawRingXY(matrix, outerR * 0.85F, outerR * 1.30F, 36, r, g, b, alpha * 0.30F);
      }

      ms.pop();
   }

   public static void renderShockwave(
      MatrixStack ms, float progress, float maxR, float r, float g, float b, float baseAlpha, boolean glow
   ) {
      float fade = (float) Math.pow(1.0F - progress, 1.5F);
      float alpha = fade * baseAlpha;
      if (alpha <= 0.001F) return;

      float currentR = maxR * easeOutCubic(progress);
      float thickness = maxR * 0.12F * (1.0F - progress * 0.5F);
      Matrix4f matrix = ms.peek().getPositionMatrix();
      RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

      drawRingXY(matrix, Math.max(0.0F, currentR - thickness), currentR, 40, r, g, b, alpha);
      drawCircleOutlineXY(matrix, currentR, 40, r, g, b, alpha * 0.9F);

      float echoR = Math.max(0.0F, currentR * 0.65F);
      drawRingXY(matrix, Math.max(0.0F, echoR - thickness * 0.6F), echoR, 32, r, g, b, alpha * 0.45F);
      drawDiamondSparkXY(matrix, maxR * 0.15F * (1.0F - progress), maxR * 0.15F * (1.0F - progress), r, g, b, alpha);

      if (glow) {
         drawRingXY(matrix, currentR * 0.85F, currentR * 1.35F, 36, r, g, b, alpha * 0.25F);
      }
   }

   public static void renderCrossSlash(
      MatrixStack ms, float progress, float size, float r, float g, float b, float baseAlpha, boolean glow
   ) {
      float fade = (float) Math.pow(1.0F - progress, 1.4F);
      float alpha = fade * baseAlpha;
      if (alpha <= 0.001F) return;

      float slashProg = Math.min(1.0F, progress / 0.35F);
      float len = size * 2.0F * easeOutCubic(slashProg);
      float thick = size * 0.08F * (1.0F - progress * 0.6F);

      Matrix4f matrix = ms.peek().getPositionMatrix();
      RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

      drawSlashLineXY(matrix, (float) (Math.PI / 4.0), len, thick, r, g, b, alpha);
      drawSlashLineXY(matrix, (float) (-Math.PI / 4.0), len, thick, r, g, b, alpha);
      drawDiamondSparkXY(matrix, size * 0.22F, size * 0.22F, r, g, b, alpha * 1.1F);
      drawCircleOutlineXY(matrix, size * 0.35F * slashProg, 28, r, g, b, alpha * 0.7F);

      if (glow) {
         drawDiamondSparkXY(matrix, size * 0.50F, size * 0.50F, r, g, b, alpha * 0.35F);
      }
   }

   public static void renderNova(
      MatrixStack ms, float progress, float size, float r, float g, float b, float baseAlpha, boolean glow
   ) {
      float fade = (float) Math.pow(1.0F - progress, 1.5F);
      float alpha = fade * baseAlpha;
      if (alpha <= 0.001F) return;

      float popScale = easeOutCubic(Math.min(1.0F, progress / 0.25F));
      ms.push();
      ms.scale(popScale, popScale, popScale);

      float rot = progress * (float) (Math.PI * 2.2);
      Matrix4f matrix = ms.peek().getPositionMatrix();
      RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

      float primaryLen = size * 1.2F * (1.0F - progress * 0.3F);
      float secondaryLen = size * 0.65F * (1.0F - progress * 0.3F);
      float rayThick = size * 0.06F * (1.0F - progress * 0.5F);

      for (int i = 0; i < 4; i++) {
         float angle = rot + i * (float) (Math.PI / 2.0);
         drawSlashLineXY(matrix, angle, primaryLen, rayThick, r, g, b, alpha);
      }
      for (int i = 0; i < 4; i++) {
         float angle = rot + (float) (Math.PI / 4.0) + i * (float) (Math.PI / 2.0);
         drawSlashLineXY(matrix, angle, secondaryLen, rayThick * 0.75F, r, g, b, alpha * 0.8F);
      }

      drawCircleOutlineXY(matrix, size * progress * 1.3F, 36, r, g, b, alpha * 0.6F);
      drawDiamondSparkXY(matrix, size * 0.25F, size * 0.25F, r, g, b, alpha);

      if (glow) {
         drawDiamondSparkXY(matrix, size * 0.60F, size * 0.60F, r, g, b, alpha * 0.30F);
      }

      ms.pop();
   }

   // ===================== ГРАФИЧЕСКИЕ ПРИМИТИВЫ (XY) =====================

   private static void drawRingXY(
      Matrix4f matrix, float innerRadius, float outerRadius, int segments, float r, float g, float b, float alpha
   ) {
      BufferBuilder buf = RenderSystem.renderThreadTesselator().begin(DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
      for (int i = 0; i <= segments; i++) {
         float angle = (float) (i * 2.0 * Math.PI / segments);
         float cos = (float) Math.cos(angle);
         float sin = (float) Math.sin(angle);
         buf.vertex(matrix, cos * outerRadius, sin * outerRadius, 0.0F).color(r, g, b, alpha);
         buf.vertex(matrix, cos * innerRadius, sin * innerRadius, 0.0F).color(r, g, b, alpha);
      }
      BufferRenderer.drawWithGlobalProgram(buf.end());
   }

   private static void drawArcsXY(
      Matrix4f matrix, float innerRadius, float outerRadius, int arcCount, float coverage,
      float rotationOffset, float r, float g, float b, float alpha
   ) {
      int segmentsPerArc = 6;
      float slice = (float) (2.0 * Math.PI / arcCount);
      float arcAngle = slice * coverage;

      for (int a = 0; a < arcCount; a++) {
         float startAngle = rotationOffset + a * slice;
         BufferBuilder buf = RenderSystem.renderThreadTesselator().begin(DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
         for (int i = 0; i <= segmentsPerArc; i++) {
            float angle = startAngle + arcAngle * i / segmentsPerArc;
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            buf.vertex(matrix, cos * outerRadius, sin * outerRadius, 0.0F).color(r, g, b, alpha);
            buf.vertex(matrix, cos * innerRadius, sin * innerRadius, 0.0F).color(r, g, b, alpha);
         }
         BufferRenderer.drawWithGlobalProgram(buf.end());
      }
   }

   private static void drawCircleOutlineXY(
      Matrix4f matrix, float radius, int segments, float r, float g, float b, float alpha
   ) {
      BufferBuilder buf = RenderSystem.renderThreadTesselator().begin(DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
      for (int i = 0; i <= segments; i++) {
         float angle = (float) ((i % segments) * 2.0 * Math.PI / segments);
         buf.vertex(matrix, (float) Math.cos(angle) * radius, (float) Math.sin(angle) * radius, 0.0F).color(r, g, b, alpha);
      }
      BufferRenderer.drawWithGlobalProgram(buf.end());
   }

   private static void drawStarXY(Matrix4f matrix, float radius, float rot, float r, float g, float b, float alpha) {
      for (int t = 0; t < 2; t++) {
         float rotOffset = rot + (t == 0 ? 0.0F : (float) Math.PI);
         BufferBuilder buf = RenderSystem.renderThreadTesselator().begin(DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
         for (int i = 0; i <= 3; i++) {
            float angle = rotOffset - (float) (Math.PI / 2.0) + (i % 3) * (float) (2.0 * Math.PI / 3.0);
            buf.vertex(matrix, (float) Math.cos(angle) * radius, (float) Math.sin(angle) * radius, 0.0F).color(r, g, b, alpha);
         }
         BufferRenderer.drawWithGlobalProgram(buf.end());
      }
   }

   private static void drawSlashLineXY(
      Matrix4f matrix, float angleRad, float length, float thickness, float r, float g, float b, float alpha
   ) {
      float cos = (float) Math.cos(angleRad);
      float sin = (float) Math.sin(angleRad);
      float perpX = -sin * thickness;
      float perpY = cos * thickness;
      float halfL = length * 0.5F;

      BufferBuilder buf = RenderSystem.renderThreadTesselator().begin(DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
      buf.vertex(matrix, -cos * halfL, -sin * halfL, 0.0F).color(r, g, b, 0.0F);
      buf.vertex(matrix, perpX, perpY, 0.0F).color(r, g, b, alpha);
      buf.vertex(matrix, cos * halfL, sin * halfL, 0.0F).color(r, g, b, 0.0F);

      buf.vertex(matrix, -cos * halfL, -sin * halfL, 0.0F).color(r, g, b, 0.0F);
      buf.vertex(matrix, cos * halfL, sin * halfL, 0.0F).color(r, g, b, 0.0F);
      buf.vertex(matrix, -perpX, -perpY, 0.0F).color(r, g, b, alpha);
      BufferRenderer.drawWithGlobalProgram(buf.end());
   }

   private static void drawDiamondSparkXY(
      Matrix4f matrix, float sizeX, float sizeY, float r, float g, float b, float alpha
   ) {
      BufferBuilder buf = RenderSystem.renderThreadTesselator().begin(DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
      buf.vertex(matrix, 0.0F, sizeY, 0.0F).color(r, g, b, alpha);
      buf.vertex(matrix, -sizeX, 0.0F, 0.0F).color(r, g, b, alpha * 0.4F);
      buf.vertex(matrix, sizeX, 0.0F, 0.0F).color(r, g, b, alpha * 0.4F);
      buf.vertex(matrix, 0.0F, -sizeY, 0.0F).color(r, g, b, alpha);
      BufferRenderer.drawWithGlobalProgram(buf.end());
   }

   // ===================== EASING ANIMATIONS =====================

   private static float easeOutBack(float t) {
      float c1 = 1.70158F;
      float c3 = c1 + 1.0F;
      return 1.0F + c3 * (float) Math.pow(t - 1.0F, 3.0) + c1 * (float) Math.pow(t - 1.0F, 2.0);
   }

   private static float easeOutCubic(float t) {
      return 1.0F - (float) Math.pow(1.0F - t, 3.0);
   }
}
