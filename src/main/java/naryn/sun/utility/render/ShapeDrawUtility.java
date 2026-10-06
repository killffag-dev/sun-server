package naryn.sun.utility.render;

import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Generated;
import naryn.sun.framework.objects.BorderRadius;
import naryn.sun.framework.objects.gradient.Gradient;
import naryn.sun.utility.colors.ColorRGBA;
import naryn.sun.utility.math.MathUtility;
import naryn.sun.utility.render.batching.Batching;
import naryn.sun.utility.render.batching.impl.RectBatching;
import naryn.sun.utility.render.batching.impl.RoundedRectBatching;
import naryn.sun.utility.render.batching.impl.SquircleBatching;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec2f;
import org.joml.Matrix4f;

public final class ShapeDrawUtility {

   public static void drawLine(MatrixStack matrices, Vec2f from, Vec2f to, ColorRGBA color) {
      matrices.push();
      try {
         Matrix4f matrix4f = matrices.peek().getPositionMatrix();
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
         RenderSystem.lineWidth(1.0F);
         DrawUtility.drawSetup();
         BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
         builder.vertex(matrix4f, from.x, from.y, 0.0F).color(color.getRGB());
         builder.vertex(matrix4f, to.x, to.y, 0.0F).color(color.getRGB());
         BufferRenderer.drawWithGlobalProgram(builder.end());
         DrawUtility.drawEnd();
      } finally {
         RenderSystem.disableBlend();
         RenderSystem.lineWidth(1.0F);
         matrices.pop();
      }
   }

   public static void drawBezier(MatrixStack matrices, Vec2f p0, Vec2f p1, Vec2f p2, Vec2f p3, ColorRGBA color, int resolution) {
      matrices.push();
      try {
         Matrix4f matrix4f = matrices.peek().getPositionMatrix();
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
         RenderSystem.lineWidth(1.0F);
         DrawUtility.drawSetup();
         BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
         for (int i = 0; i <= resolution; i++) {
            float t = (float) i / resolution;
            float x = (float) MathUtility.cubicBezier(t, p0.x, p1.x, p2.x, p3.x);
            float y = (float) MathUtility.cubicBezier(t, p0.y, p1.y, p2.y, p3.y);
            builder.vertex(matrix4f, x, y, 0.0F).color(color.getRGB());
         }
         BufferRenderer.drawWithGlobalProgram(builder.end());
         DrawUtility.drawEnd();
      } finally {
         RenderSystem.disableBlend();
         RenderSystem.lineWidth(1.0F);
         matrices.pop();
      }
   }

   public static void drawRect(MatrixStack matrices, float x, float y, float width, float height, ColorRGBA color) {
      if (Batching.getActive() instanceof RectBatching batching) {
         BufferBuilder builder = batching.getBuilder();
         Matrix4f matrix4f = batching.getMatrices().peek().getPositionMatrix();
         builder.vertex(matrix4f, x, y + height, 0.0F).color(color.getRGB());
         builder.vertex(matrix4f, x + width, y + height, 0.0F).color(color.getRGB());
         builder.vertex(matrix4f, x + width, y, 0.0F).color(color.getRGB());
         builder.vertex(matrix4f, x, y, 0.0F).color(color.getRGB());
      } else {
         matrices.push();
         Matrix4f matrix4f = matrices.peek().getPositionMatrix();
         RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
         DrawUtility.drawSetup();
         BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
         builder.vertex(matrix4f, x, y + height, 0.0F).color(color.getRGB());
         builder.vertex(matrix4f, x + width, y + height, 0.0F).color(color.getRGB());
         builder.vertex(matrix4f, x + width, y, 0.0F).color(color.getRGB());
         builder.vertex(matrix4f, x, y, 0.0F).color(color.getRGB());
         BufferRenderer.drawWithGlobalProgram(builder.end());
         DrawUtility.drawEnd();
         matrices.pop();
      }
   }

   public static void drawSquircle(MatrixStack matrices, float x, float y, float width, float height, float squirt, BorderRadius borderRadius, ColorRGBA color) {
      matrices.push();
      Matrix4f m = matrices.peek().getPositionMatrix();
      float smoothness = 0.5F;
      if (Batching.getActive() instanceof SquircleBatching sb) {
         sb.add(
            m,
            x,
            y,
            width,
            height,
            borderRadius.topLeftRadius() * squirt / 2.0F,
            borderRadius.bottomLeftRadius() * squirt / 2.0F,
            borderRadius.topRightRadius() * squirt / 2.0F,
            borderRadius.bottomRightRadius() * squirt / 2.0F,
            color.getRGB()
         );
         matrices.pop();
      } else {
         DrawUtility.squircleProgram.use();
         DrawUtility.squircleProgram.findUniform("Size").set(width, height);
         DrawUtility.squircleProgram.findUniform("Radius")
            .set(
               borderRadius.topLeftRadius() * squirt / 2.0F,
               borderRadius.bottomLeftRadius() * squirt / 2.0F,
               borderRadius.topRightRadius() * squirt / 2.0F,
               borderRadius.bottomRightRadius() * squirt / 2.0F
            );
         DrawUtility.squircleProgram.findUniform("Smoothness").set(smoothness);
         DrawUtility.squircleProgram.findUniform("CornerSmoothness").set(squirt);
         DrawUtility.drawSetup();
         emitColorQuad(m, x, y, width, height, smoothness, color.getRGB());
         DrawUtility.drawEnd();
         matrices.pop();
      }
   }

   public static void drawLoadingRect(
      MatrixStack matrices, float x, float y, float width, float height, float progress, BorderRadius borderRadius, ColorRGBA color
   ) {
      matrices.push();
      Matrix4f matrix4f = matrices.peek().getPositionMatrix();
      float smoothness = 0.5F;
      DrawUtility.loadingProgram.use();
      DrawUtility.loadingProgram.findUniform("Size").set(width, height);
      DrawUtility.loadingProgram.findUniform("Radius")
         .set(borderRadius.topLeftRadius(), borderRadius.bottomLeftRadius(), borderRadius.topRightRadius(), borderRadius.bottomRightRadius());
      DrawUtility.loadingProgram.findUniform("Smoothness").set(smoothness);
      DrawUtility.loadingProgram.findUniform("Progress").set(progress);
      DrawUtility.loadingProgram.findUniform("StripeWidth").set(0.0F);
      DrawUtility.loadingProgram.findUniform("Fade").set(0.5F);
      DrawUtility.drawSetup();
      emitColorQuad(matrix4f, x, y, width, height, smoothness, color.getRGB());
      DrawUtility.drawEnd();
      matrices.pop();
   }

   public static void drawRoundedRect(MatrixStack matrices, float x, float y, float width, float height, BorderRadius borderRadius, ColorRGBA color) {
      matrices.push();
      Matrix4f m = matrices.peek().getPositionMatrix();
      float smoothness = 0.5F;
      if (Batching.getActive() instanceof RoundedRectBatching rb) {
         rb.add(
            m,
            x,
            y,
            width,
            height,
            borderRadius.topLeftRadius(),
            borderRadius.bottomLeftRadius(),
            borderRadius.topRightRadius(),
            borderRadius.bottomRightRadius(),
            color.getRGB()
         );
         matrices.pop();
      } else {
         DrawUtility.rectangleProgram.use();
         DrawUtility.rectangleProgram.findUniform("Size").set(width, height);
         DrawUtility.rectangleProgram.findUniform("Radius")
            .set(borderRadius.topLeftRadius(), borderRadius.bottomLeftRadius(), borderRadius.topRightRadius(), borderRadius.bottomRightRadius());
         DrawUtility.rectangleProgram.findUniform("Smoothness").set(smoothness);
         DrawUtility.drawSetup();
         emitColorQuad(m, x, y, width, height, smoothness, color.getRGB());
         DrawUtility.drawEnd();
         matrices.pop();
      }
   }

   public static void drawRoundedRect(
      MatrixStack matrices,
      float x,
      float y,
      float width,
      float height,
      BorderRadius borderRadius,
      ColorRGBA color1,
      ColorRGBA color2,
      ColorRGBA color3,
      ColorRGBA color4
   ) {
      matrices.push();
      Matrix4f matrix4f = matrices.peek().getPositionMatrix();
      float smoothness = 0.5F;
      DrawUtility.gradientRectangleProgram.use();
      DrawUtility.gradientRectangleProgram.findUniform("Size").set(width, height);
      DrawUtility.gradientRectangleProgram.findUniform("Radius")
         .set(borderRadius.topLeftRadius(), borderRadius.bottomLeftRadius(), borderRadius.topRightRadius(), borderRadius.bottomRightRadius());
      DrawUtility.gradientRectangleProgram.findUniform("Smoothness").set(smoothness);
      DrawUtility.gradientRectangleProgram.findUniform("TopLeftColor")
         .set(color1.getRed() / 255.0F, color1.getGreen() / 255.0F, color1.getBlue() / 255.0F, color1.getAlpha() / 255.0F);
      DrawUtility.gradientRectangleProgram.findUniform("BottomLeftColor")
         .set(color2.getRed() / 255.0F, color2.getGreen() / 255.0F, color2.getBlue() / 255.0F, color2.getAlpha() / 255.0F);
      DrawUtility.gradientRectangleProgram.findUniform("BottomRightColor")
         .set(color3.getRed() / 255.0F, color3.getGreen() / 255.0F, color3.getBlue() / 255.0F, color3.getAlpha() / 255.0F);
      DrawUtility.gradientRectangleProgram.findUniform("TopRightColor")
         .set(color4.getRed() / 255.0F, color4.getGreen() / 255.0F, color4.getBlue() / 255.0F, color4.getAlpha() / 255.0F);
      DrawUtility.drawSetup();
      emitGradientQuad(matrix4f, x, y, width, height, smoothness, color1.getRGB(), color2.getRGB(), color3.getRGB(), color4.getRGB());
      DrawUtility.drawEnd();
      matrices.pop();
   }

   public static void drawRoundedRect(MatrixStack matrices, float x, float y, float width, float height, BorderRadius borderRadius, Gradient gradient) {
      drawRoundedRect(
         matrices,
         x,
         y,
         width,
         height,
         borderRadius,
         gradient.getTopLeftColor(),
         gradient.getBottomLeftColor(),
         gradient.getBottomRightColor(),
         gradient.getTopRightColor()
      );
   }

   public static void drawRoundedBorder(
      MatrixStack matrices, float x, float y, float width, float height, float borderThickness, BorderRadius borderRadius, ColorRGBA borderColor
   ) {
      matrices.push();
      Matrix4f matrix4f = matrices.peek().getPositionMatrix();
      float internalSmoothness = 0.5F;
      float externalSmoothness = 1.0F;
      DrawUtility.borderProgram.use();
      DrawUtility.borderProgram.findUniform("Size").set(width, height);
      DrawUtility.borderProgram.findUniform("Radius")
         .set(borderRadius.topLeftRadius(), borderRadius.bottomLeftRadius(), borderRadius.topRightRadius(), borderRadius.bottomRightRadius());
      DrawUtility.borderProgram.findUniform("Smoothness").set(internalSmoothness, externalSmoothness);
      DrawUtility.borderProgram.findUniform("Thickness").set(borderThickness);
      DrawUtility.drawSetup();
      emitColorQuad(matrix4f, x, y, width, height, externalSmoothness, borderColor.getRGB());
      DrawUtility.drawEnd();
      matrices.pop();
   }

   public static void drawRoundedBorder(
      MatrixStack matrices, float x, float y, float width, float height, float borderThickness, BorderRadius borderRadius,
      ColorRGBA topLeftColor, ColorRGBA bottomLeftColor, ColorRGBA bottomRightColor, ColorRGBA topRightColor
   ) {
      matrices.push();
      Matrix4f matrix4f = matrices.peek().getPositionMatrix();
      float internalSmoothness = 0.5F;
      float externalSmoothness = 1.0F;
      DrawUtility.borderProgram.use();
      DrawUtility.borderProgram.findUniform("Size").set(width, height);
      DrawUtility.borderProgram.findUniform("Radius")
         .set(borderRadius.topLeftRadius(), borderRadius.bottomLeftRadius(), borderRadius.topRightRadius(), borderRadius.bottomRightRadius());
      DrawUtility.borderProgram.findUniform("Smoothness").set(internalSmoothness, externalSmoothness);
      DrawUtility.borderProgram.findUniform("Thickness").set(borderThickness);
      DrawUtility.drawSetup();
      emitGradientQuad(
         matrix4f, x, y, width, height, externalSmoothness,
         topLeftColor.getRGB(), bottomLeftColor.getRGB(), bottomRightColor.getRGB(), topRightColor.getRGB()
      );
      DrawUtility.drawEnd();
      matrices.pop();
   }

   public static void drawStar(MatrixStack matrices, float centerX, float centerY, float outerRadius, float innerRadius, ColorRGBA color) {
      matrices.push();
      try {
         Matrix4f matrix4f = matrices.peek().getPositionMatrix();
         RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
         DrawUtility.drawSetup();
         BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
         int rgb = color.getRGB();
         builder.vertex(matrix4f, centerX, centerY, 0.0F).color(rgb);

         int points = 10;
         for (int i = 0; i <= points; i++) {
            int idx = i % points;
            float angle = (float) (Math.PI / 2.0 + idx * (Math.PI / 5.0));
            float radius = (idx % 2 == 0) ? outerRadius : innerRadius;
            float px = centerX + radius * (float) Math.cos(angle);
            float py = centerY - radius * (float) Math.sin(angle);
            builder.vertex(matrix4f, px, py, 0.0F).color(rgb);
         }

         BufferRenderer.drawWithGlobalProgram(builder.end());
         DrawUtility.drawEnd();
      } finally {
         matrices.pop();
      }
   }

   public static void drawStarOutline(MatrixStack matrices, float centerX, float centerY, float outerRadius, float innerRadius, ColorRGBA color) {
      matrices.push();
      try {
         Matrix4f matrix4f = matrices.peek().getPositionMatrix();
         RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
         DrawUtility.drawSetup();
         BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
         int rgb = color.getRGB();

         int points = 10;
         for (int i = 0; i <= points; i++) {
            int idx = i % points;
            float angle = (float) (Math.PI / 2.0 + idx * (Math.PI / 5.0));
            float radius = (idx % 2 == 0) ? outerRadius : innerRadius;
            float px = centerX + radius * (float) Math.cos(angle);
            float py = centerY - radius * (float) Math.sin(angle);
            builder.vertex(matrix4f, px, py, 0.0F).color(rgb);
         }

         BufferRenderer.drawWithGlobalProgram(builder.end());
         DrawUtility.drawEnd();
      } finally {
         matrices.pop();
      }
   }

   private static void emitColorQuad(Matrix4f matrix, float x, float y, float width, float height, float smoothness, int color) {
      emitGradientQuad(matrix, x, y, width, height, smoothness, color, color, color, color);
   }

   private static void emitGradientQuad(
      Matrix4f matrix, float x, float y, float width, float height, float smoothness, int c1, int c2, int c3, int c4
   ) {
      float horizontalPadding = -smoothness / 2.0F + smoothness * 2.0F;
      float verticalPadding = smoothness / 2.0F + smoothness;
      float ax = x - horizontalPadding / 2.0F;
      float ay = y - verticalPadding / 2.0F;
      float aw = width + horizontalPadding;
      float ah = height + verticalPadding;
      BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      builder.vertex(matrix, ax, ay, 0.0F).color(c1);
      builder.vertex(matrix, ax, ay + ah, 0.0F).color(c2);
      builder.vertex(matrix, ax + aw, ay + ah, 0.0F).color(c3);
      builder.vertex(matrix, ax + aw, ay, 0.0F).color(c4);
      BufferRenderer.drawWithGlobalProgram(builder.end());
   }

   @Generated
   private ShapeDrawUtility() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
