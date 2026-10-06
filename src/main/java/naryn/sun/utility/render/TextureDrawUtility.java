package naryn.sun.utility.render;

import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Generated;
import naryn.sun.Sun;
import naryn.sun.framework.objects.BorderRadius;
import naryn.sun.utility.colors.ColorRGBA;
import naryn.sun.utility.colors.Colors;
import naryn.sun.utility.render.batching.Batching;
import naryn.sun.utility.render.batching.impl.IconBatching;
import naryn.sun.utility.render.obj.CustomSprite;
import naryn.sun.utility.render.penis.PenisSprite;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

public final class TextureDrawUtility {

   public static void bindTexture(Identifier identifier) {
      RenderSystem.setShaderTexture(0, identifier);
      RenderSystem.texParameter(3553, 10241, 9729);
      RenderSystem.texParameter(3553, 10240, 9729);
   }

   public static void drawTexture(MatrixStack matrices, Identifier identifier, float x, float y, float width, float height, ColorRGBA textureColor) {
      if (Batching.getActive() instanceof IconBatching batching) {
         BufferBuilder builder = batching.getBuilder();
         Matrix4f matrix4f = batching.getMatrices().peek().getPositionMatrix();
         bindTexture(identifier);
         builder.vertex(matrix4f, x, y, 0.0F).texture(0.0F, 0.0F).color(textureColor.getRGB());
         builder.vertex(matrix4f, x, y + height, 0.0F).texture(0.0F, 1.0F).color(textureColor.getRGB());
         builder.vertex(matrix4f, x + width, y + height, 0.0F).texture(1.0F, 1.0F).color(textureColor.getRGB());
         builder.vertex(matrix4f, x + width, y, 0.0F).texture(1.0F, 0.0F).color(textureColor.getRGB());
      } else {
         matrices.push();
         Matrix4f matrix4f = matrices.peek().getPositionMatrix();
         RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
         bindTexture(identifier);
         DrawUtility.drawSetup();
         BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
         builder.vertex(matrix4f, x, y, 0.0F).texture(0.0F, 0.0F).color(textureColor.getRGB());
         builder.vertex(matrix4f, x, y + height, 0.0F).texture(0.0F, 1.0F).color(textureColor.getRGB());
         builder.vertex(matrix4f, x + width, y + height, 0.0F).texture(1.0F, 1.0F).color(textureColor.getRGB());
         builder.vertex(matrix4f, x + width, y, 0.0F).texture(1.0F, 0.0F).color(textureColor.getRGB());
         BufferRenderer.drawWithGlobalProgram(builder.end());
         DrawUtility.drawEnd();
         RenderSystem.setShaderTexture(0, 0);
         matrices.pop();
      }
   }

   public static void drawTexture(
      MatrixStack matrices, Identifier identifier, float x, float y, float width, float height, float u1, float u2, float v1, float v2, ColorRGBA clor
   ) {
      if (Batching.getActive() instanceof IconBatching batching) {
         BufferBuilder builder = batching.getBuilder();
         Matrix4f matrix4f = batching.getMatrices().peek().getPositionMatrix();
         RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
         bindTexture(identifier);
         int color = clor.getRGB();
         float x2 = x + width;
         float y2 = y + height;
         builder.vertex(matrix4f, x, y, 0.0F).texture(u1, v1).color(color);
         builder.vertex(matrix4f, x, y2, 0.0F).texture(u1, v2).color(color);
         builder.vertex(matrix4f, x2, y2, 0.0F).texture(u2, v2).color(color);
         builder.vertex(matrix4f, x2, y, 0.0F).texture(u2, v1).color(color);
      } else {
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         matrices.push();
         int color = clor.getRGB();
         Matrix4f matrix4f = matrices.peek().getPositionMatrix();
         float x2 = x + width;
         float y2 = y + height;
         RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
         bindTexture(identifier);
         BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
         builder.vertex(matrix4f, x, y, 0.0F).texture(u1, v1).color(color);
         builder.vertex(matrix4f, x, y2, 0.0F).texture(u1, v2).color(color);
         builder.vertex(matrix4f, x2, y2, 0.0F).texture(u2, v2).color(color);
         builder.vertex(matrix4f, x2, y, 0.0F).texture(u2, v1).color(color);
         BufferRenderer.drawWithGlobalProgram(builder.end());
         DrawUtility.drawEnd();
         RenderSystem.setShaderTexture(0, 0);
         matrices.pop();
         RenderSystem.disableBlend();
      }
   }

   public static void drawAnimationSprite(MatrixStack matrices, PenisSprite sprite, float x, float y, float width, float height, ColorRGBA color) {
      if (sprite != null) {
         drawTexture(matrices, sprite.texture(), x, y, width, height, sprite.u1(), sprite.u2(), sprite.v1(), sprite.v2(), color);
      }
   }

   public static void drawSprite(MatrixStack matrices, CustomSprite sprite, float x, float y, float width, float height, ColorRGBA color) {
      drawTexture(
         matrices,
         Sun.id(sprite.getTexture().getTexture()),
         x,
         y,
         width,
         height,
         sprite.x / sprite.getTexture().getWidth(),
         (sprite.x + sprite.getTexture().getStep()) / sprite.getTexture().getWidth(),
         0.0F,
         1.0F,
         color
      );
   }

   public static void drawRoundedTexture(MatrixStack matrices, Identifier identifier, float x, float y, float width, float height, BorderRadius borderRadius) {
      drawRoundedTexture(matrices, identifier, x, y, width, height, borderRadius, Colors.WHITE);
   }

   public static void drawRoundedTexture(
      MatrixStack matrices, Identifier identifier, float x, float y, float width, float height, BorderRadius borderRadius, ColorRGBA color
   ) {
      drawRoundedTextureWithUV(matrices, identifier, x, y, width, height, borderRadius, color, 0.0F, 0.0F, 1.0F, 1.0F);
   }

   public static void drawRoundedTextureWithUV(
      MatrixStack matrices,
      Identifier identifier,
      float x,
      float y,
      float width,
      float height,
      BorderRadius borderRadius,
      ColorRGBA color,
      float u1,
      float v1,
      float u2,
      float v2
   ) {
      matrices.push();
      Matrix4f matrix4f = matrices.peek().getPositionMatrix();
      float smoothness = 0.5F;
      DrawUtility.roundedTextureProgram.use();
      bindTexture(identifier);
      DrawUtility.roundedTextureProgram.findUniform("Size").set(width, height);
      DrawUtility.roundedTextureProgram.findUniform("Radius")
         .set(borderRadius.topLeftRadius(), borderRadius.bottomLeftRadius(), borderRadius.topRightRadius(), borderRadius.bottomRightRadius());
      DrawUtility.roundedTextureProgram.findUniform("Smoothness").set(smoothness);
      DrawUtility.drawSetup();
      emitTexturedQuad(matrix4f, x, y, width, height, smoothness, color.getRGB(), u1, v1, u2, v2);
      DrawUtility.drawEnd();
      RenderSystem.setShaderTexture(0, 0);
      matrices.pop();
   }

   public static void drawImage(MatrixStack matrices, BufferBuilder builder, double x, double y, double z, double width, double height, ColorRGBA color) {
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      builder.vertex(matrix, (float) x, (float) (y + height), (float) z).texture(0.0F, 1.0F).color(color.getRGB());
      builder.vertex(matrix, (float) (x + width), (float) (y + height), (float) z).texture(1.0F, 1.0F).color(color.getRGB());
      builder.vertex(matrix, (float) (x + width), (float) y, (float) z).texture(1.0F, 0.0F).color(color.getRGB());
      builder.vertex(matrix, (float) x, (float) y, (float) z).texture(0.0F, 0.0F).color(color.getRGB());
   }

   public static void drawImage(MatrixStack matrices, Identifier identifier, double x, double y, double z, double width, double height, ColorRGBA color) {
      bindTexture(identifier);
      BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      builder.vertex(matrix, (float) x, (float) (y + height), (float) z).texture(0.0F, 1.0F).color(color.getRGB());
      builder.vertex(matrix, (float) (x + width), (float) (y + height), (float) z).texture(1.0F, 1.0F).color(color.getRGB());
      builder.vertex(matrix, (float) (x + width), (float) y, (float) z).texture(1.0F, 0.0F).color(color.getRGB());
      builder.vertex(matrix, (float) x, (float) y, (float) z).texture(0.0F, 0.0F).color(color.getRGB());
      BufferRenderer.drawWithGlobalProgram(builder.end());
   }

   private static void emitTexturedQuad(
      Matrix4f matrix, float x, float y, float width, float height, float smoothness, int color,
      float u1, float v1, float u2, float v2
   ) {
      float horizontalPadding = -smoothness / 2.0F + smoothness * 2.0F;
      float verticalPadding = smoothness / 2.0F + smoothness;
      float adjustedX = x - horizontalPadding / 2.0F;
      float adjustedY = y - verticalPadding / 2.0F;
      float adjustedWidth = width + horizontalPadding;
      float adjustedHeight = height + verticalPadding;
      BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      builder.vertex(matrix, adjustedX, adjustedY, 0.0F).texture(u1, v1).color(color);
      builder.vertex(matrix, adjustedX, adjustedY + adjustedHeight, 0.0F).texture(u1, v2).color(color);
      builder.vertex(matrix, adjustedX + adjustedWidth, adjustedY + adjustedHeight, 0.0F).texture(u2, v2).color(color);
      builder.vertex(matrix, adjustedX + adjustedWidth, adjustedY, 0.0F).texture(u2, v1).color(color);
      BufferRenderer.drawWithGlobalProgram(builder.end());
   }

   @Generated
   private TextureDrawUtility() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
