package naryn.sun.utility.render;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Generated;
import naryn.sun.Sun;
import naryn.sun.framework.objects.BorderRadius;
import naryn.sun.framework.shader.impl.BlurProgram;
import naryn.sun.utility.colors.ColorRGBA;
import naryn.sun.utility.interfaces.IMinecraft;
import naryn.sun.utility.interfaces.IWindow;
import naryn.sun.utility.render.batching.Batching;
import naryn.sun.utility.render.batching.impl.IconBatching;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import naryn.sun.utility.math.MathPool;

import java.util.Random;

public final class EffectDrawUtility implements IMinecraft, IWindow {

   private static final Supplier<Identifier> NOISE_TEXTURE = Suppliers.memoize(EffectDrawUtility::generateNoiseTexture);
   private static final int NOISE_TEXTURE_SIZE = 128;

   public static void drawShadow(MatrixStack matrices, float x, float y, float width, float height, float softness, BorderRadius borderRadius, ColorRGBA color) {
      matrices.push();
      Matrix4f matrix4f = matrices.peek().getPositionMatrix();
      if (Batching.getActive() instanceof IconBatching batching) {
         BufferBuilder builder = batching.getBuilder();
         float horizontalPadding = -softness / 2.0F + softness * 2.0F;
         float verticalPadding = softness / 2.0F + softness;
         float adjustedX = x - horizontalPadding / 2.0F;
         float adjustedY = y - verticalPadding / 2.0F;
         float adjustedWidth = width + horizontalPadding;
         float adjustedHeight = height + verticalPadding;
         builder.vertex(matrix4f, adjustedX, adjustedY, 0.0F).color(color.getRGB());
         builder.vertex(matrix4f, adjustedX, adjustedY + adjustedHeight, 0.0F).color(color.getRGB());
         builder.vertex(matrix4f, adjustedX + adjustedWidth, adjustedY + adjustedHeight, 0.0F).color(color.getRGB());
         builder.vertex(matrix4f, adjustedX + adjustedWidth, adjustedY, 0.0F).color(color.getRGB());
      } else {
         DrawUtility.rectangleProgram.use();
         DrawUtility.rectangleProgram.findUniform("Size").set(width, height);
         DrawUtility.rectangleProgram.findUniform("Radius")
            .set(
               borderRadius.topLeftRadius() * 3.0F,
               borderRadius.bottomLeftRadius() * 3.0F,
               borderRadius.topRightRadius() * 3.0F,
               borderRadius.bottomRightRadius() * 3.0F
            );
         DrawUtility.rectangleProgram.findUniform("Smoothness").set(softness);
         DrawUtility.drawSetup();
         float horizontalPadding = -softness / 2.0F + softness * 2.0F;
         float verticalPadding = softness / 2.0F + softness;
         float adjustedX = x - horizontalPadding / 2.0F;
         float adjustedY = y - verticalPadding / 2.0F;
         float adjustedWidth = width + horizontalPadding;
         float adjustedHeight = height + verticalPadding;
         BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
         builder.vertex(matrix4f, adjustedX, adjustedY, 0.0F).color(color.getRGB());
         builder.vertex(matrix4f, adjustedX, adjustedY + adjustedHeight, 0.0F).color(color.getRGB());
         builder.vertex(matrix4f, adjustedX + adjustedWidth, adjustedY + adjustedHeight, 0.0F).color(color.getRGB());
         builder.vertex(matrix4f, adjustedX + adjustedWidth, adjustedY, 0.0F).color(color.getRGB());
         BufferRenderer.drawWithGlobalProgram(builder.end());
         DrawUtility.drawEnd();
         matrices.pop();
      }
   }

   public static void drawPerimeterShadow(MatrixStack matrices, float x, float y, float width, float height, float softness, BorderRadius borderRadius, ColorRGBA color) {
      if (softness <= 0.0F || color.getAlpha() <= 0.0F) {
         return;
      }
      matrices.push();
      Matrix4f matrix4f = matrices.peek().getPositionMatrix();
      DrawUtility.perimeterShadowProgram.use();
      DrawUtility.perimeterShadowProgram.findUniform("Size").set(width, height);
      DrawUtility.perimeterShadowProgram.findUniform("Radius")
         .set(
            borderRadius.topLeftRadius(),
            borderRadius.bottomLeftRadius(),
            borderRadius.topRightRadius(),
            borderRadius.bottomRightRadius()
         );
      DrawUtility.perimeterShadowProgram.findUniform("Softness").set(softness);
      DrawUtility.drawSetup();
      float adjustedX = x - softness;
      float adjustedY = y - softness;
      float adjustedWidth = width + softness * 2.0F;
      float adjustedHeight = height + softness * 2.0F;
      BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      builder.vertex(matrix4f, adjustedX, adjustedY, 0.0F).color(color.getRGB());
      builder.vertex(matrix4f, adjustedX, adjustedY + adjustedHeight, 0.0F).color(color.getRGB());
      builder.vertex(matrix4f, adjustedX + adjustedWidth, adjustedY + adjustedHeight, 0.0F).color(color.getRGB());
      builder.vertex(matrix4f, adjustedX + adjustedWidth, adjustedY, 0.0F).color(color.getRGB());
      BufferRenderer.drawWithGlobalProgram(builder.end());
      DrawUtility.drawEnd();
      matrices.pop();
   }

   public static void drawBlur(
      MatrixStack matrices, float x, float y, float width, float height, float blurRadius, float squirt, BorderRadius borderRadius, ColorRGBA color
   ) {
      matrices.push();
      Matrix4f matrix4f = matrices.peek().getPositionMatrix();
      float smoothness = 0.03F;
      blurRadius /= 22.5F;
      if (!(blurRadius <= 0.0F)) {
         DrawUtility.blurProgram.setBlurOffset(2.0F);
         DrawUtility.squircleTextureProgram.use();
         RenderSystem.setShaderTexture(0, BlurProgram.getTexture());
         DrawUtility.squircleTextureProgram.findUniform("Size").set(width, height);
         DrawUtility.squircleTextureProgram.findUniform("Radius")
            .set(
               borderRadius.topLeftRadius() * squirt / 2.0F,
               borderRadius.bottomLeftRadius() * squirt / 2.0F,
               borderRadius.topRightRadius() * squirt / 2.0F,
               borderRadius.bottomRightRadius() * squirt / 2.0F
            );
         DrawUtility.squircleTextureProgram.findUniform("Smoothness").set(0.1F);
         DrawUtility.squircleTextureProgram.findUniform("CornerSmoothness").set(squirt);
         DrawUtility.drawSetup();
         float horizontalPadding = -smoothness / 2.0F + smoothness * 2.0F;
         float verticalPadding = smoothness / 2.0F + smoothness;
         float adjustedX = x - horizontalPadding / 2.0F;
         float adjustedY = y - verticalPadding / 2.0F;
         float adjustedWidth = width + horizontalPadding;
         float adjustedHeight = height + verticalPadding;
         Vector3f p0 = matrix4f.transformPosition(adjustedX, adjustedY, 0.0F, MathPool.vec3f());
         Vector3f p1 = matrix4f.transformPosition(adjustedX + adjustedWidth, adjustedY + adjustedHeight, 0.0F, MathPool.vec3f());
         int screenWidth = mc.getWindow().getScaledWidth();
         int screenHeight = mc.getWindow().getScaledHeight();
         float u0 = p0.x / screenWidth;
         float u1 = p1.x / screenWidth;
         float v0 = (screenHeight - p0.y) / screenHeight;
         float v1 = (screenHeight - p1.y) / screenHeight;
         BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
         builder.vertex(matrix4f, adjustedX, adjustedY, 0.0F).texture(u0, v0).color(color.getRGB());
         builder.vertex(matrix4f, adjustedX, adjustedY + adjustedHeight, 0.0F).texture(u0, v1).color(color.getRGB());
         builder.vertex(matrix4f, adjustedX + adjustedWidth, adjustedY + adjustedHeight, 0.0F).texture(u1, v1).color(color.getRGB());
         builder.vertex(matrix4f, adjustedX + adjustedWidth, adjustedY, 0.0F).texture(u1, v0).color(color.getRGB());
         BufferRenderer.drawWithGlobalProgram(builder.end());
         DrawUtility.drawEnd();
         RenderSystem.setShaderTexture(0, 0);
         matrices.pop();
      }
   }

   public static void drawBlur(MatrixStack matrices, float x, float y, float width, float height, float blurRadius, BorderRadius borderRadius, ColorRGBA color) {
      matrices.push();
      Matrix4f matrix4f = matrices.peek().getPositionMatrix();
      blurRadius /= 22.5F;
      if (!(blurRadius <= 0.0F)) {
         DrawUtility.blurProgram.setBlurOffset(2.0F);
         DrawUtility.roundedTextureProgram.use();
         RenderSystem.setShaderTexture(0, BlurProgram.getTexture());
         DrawUtility.roundedTextureProgram.findUniform("Size").set(width, height);
         DrawUtility.roundedTextureProgram.findUniform("Radius")
            .set(borderRadius.topLeftRadius(), borderRadius.bottomLeftRadius(), borderRadius.topRightRadius(), borderRadius.bottomRightRadius());
         DrawUtility.roundedTextureProgram.findUniform("Smoothness").set(0.01F);
         DrawUtility.drawSetup();
         Vector3f p0 = matrix4f.transformPosition(x, y, 0.0F, MathPool.vec3f());
         Vector3f p1 = matrix4f.transformPosition(x + width, y + height, 0.0F, MathPool.vec3f());
         int screenWidth = mc.getWindow().getScaledWidth();
         int screenHeight = mc.getWindow().getScaledHeight();
         float u0 = p0.x / screenWidth;
         float u1 = p1.x / screenWidth;
         float v0 = (screenHeight - p0.y) / screenHeight;
         float v1 = (screenHeight - p1.y) / screenHeight;
         BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
         builder.vertex(matrix4f, x, y, 0.0F).texture(u0, v0).color(color.getRGB());
         builder.vertex(matrix4f, x, y + height, 0.0F).texture(u0, v1).color(color.getRGB());
         builder.vertex(matrix4f, x + width, y + height, 0.0F).texture(u1, v1).color(color.getRGB());
         builder.vertex(matrix4f, x + width, y, 0.0F).texture(u1, v0).color(color.getRGB());
         BufferRenderer.drawWithGlobalProgram(builder.end());
         DrawUtility.drawEnd();
         RenderSystem.setShaderTexture(0, 0);
         matrices.pop();
      }
   }

   public static void drawLiquidGlass(
      MatrixStack matrices,
      float x,
      float y,
      float width,
      float height,
      BorderRadius borderRadius,
      ColorRGBA color,
      float globalAlpha,
      float fresnelPower,
      ColorRGBA fresnelColor,
      float baseAlpha,
      boolean fresnelInvert,
      float fresnelMix,
      float distortStrength,
      float squirt,
      boolean clean
   ) {
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      DrawUtility.drawSetup();
      RenderSystem.disableCull();
      RenderSystem.setShaderTexture(0, clean ? mc.getFramebuffer().getColorAttachment() : BlurProgram.getTexture());
      DrawUtility.glassProgram.use();
      DrawUtility.glassProgram.findUniform("GlobalAlpha").set(globalAlpha);
      DrawUtility.glassProgram.findUniform("Size").set(width, height);
      DrawUtility.glassProgram.findUniform("Radius")
         .set(borderRadius.topLeftRadius(), borderRadius.bottomLeftRadius(), borderRadius.topRightRadius(), borderRadius.bottomRightRadius());
      DrawUtility.glassProgram.findUniform("Smoothness").set(0.5F);
      DrawUtility.glassProgram.findUniform("FresnelPower").set(fresnelPower);
      int fRgb = fresnelColor.getRGB();
      DrawUtility.glassProgram.findUniform("FresnelColor").set(ColorUtility.redf(fRgb), ColorUtility.greenf(fRgb), ColorUtility.bluef(fRgb));
      DrawUtility.glassProgram.findUniform("FresnelAlpha").set(ColorUtility.alphaf(fRgb));
      DrawUtility.glassProgram.findUniform("BaseAlpha").set(baseAlpha);
      DrawUtility.glassProgram.findUniform("FresnelInvert").set(fresnelInvert ? 1 : 0);
      DrawUtility.glassProgram.findUniform("FresnelMix").set(fresnelMix);
      DrawUtility.glassProgram.findUniform("DistortStrength").set(distortStrength);
      DrawUtility.glassProgram.findUniform("CornerSmoothness").set(squirt);
      Vector3f p0 = matrix.transformPosition(x, y, 0.0F, MathPool.vec3f());
      Vector3f p1 = matrix.transformPosition(x + width, y + height, 0.0F, MathPool.vec3f());
      int screenWidth = mw.getScaledWidth();
      int screenHeight = mw.getScaledHeight();
      float u0 = p0.x / screenWidth;
      float u1 = p1.x / screenWidth;
      float v0 = (screenHeight - p0.y) / screenHeight;
      float v1 = (screenHeight - p1.y) / screenHeight;
      BufferBuilder builder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      builder.vertex(matrix, x, y, 0.0F).texture(u0, v0).color(color.getRGB());
      builder.vertex(matrix, x, y + height, 0.0F).texture(u0, v1).color(color.getRGB());
      builder.vertex(matrix, x + width, y + height, 0.0F).texture(u1, v1).color(color.getRGB());
      builder.vertex(matrix, x + width, y, 0.0F).texture(u1, v0).color(color.getRGB());
      BufferRenderer.drawWithGlobalProgram(builder.end());
      RenderSystem.setShaderTexture(0, 0);
      RenderSystem.enableCull();
      DrawUtility.drawEnd();
   }

   public static void drawNoiseOverlay(MatrixStack matrices, float x, float y, float width, float height, BorderRadius borderRadius, ColorRGBA color) {
      TextureDrawUtility.drawRoundedTexture(matrices, NOISE_TEXTURE.get(), x, y, width, height, borderRadius, color);
   }

   private static Identifier generateNoiseTexture() {
      int size = NOISE_TEXTURE_SIZE;
      NativeImage image = new NativeImage(size, size, false);
      Random random = new Random(1337L);
      for (int x = 0; x < size; x++) {
         for (int y = 0; y < size; y++) {
            int gray = 96 + random.nextInt(64);
            int argb = (255 << 24) | (gray << 16) | (gray << 8) | gray;
            image.setColor(x, y, argb);
         }
      }
      Identifier id = Sun.id("generated_glass_noise");
      NativeImageBackedTexture texture = new NativeImageBackedTexture(image);
      MinecraftClient.getInstance().getTextureManager().registerTexture(id, texture);
      return id;
   }

   @Generated
   private EffectDrawUtility() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
