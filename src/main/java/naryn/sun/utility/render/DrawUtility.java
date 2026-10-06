package naryn.sun.utility.render;

import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Generated;
import naryn.sun.Sun;
import naryn.sun.framework.objects.BorderRadius;
import naryn.sun.framework.objects.gradient.Gradient;
import naryn.sun.framework.shader.GlProgram;
import naryn.sun.framework.shader.impl.BlurProgram;
import naryn.sun.framework.shader.impl.SkyProgram;
import naryn.sun.framework.shader.impl.MotionBlurProgram;
import naryn.sun.utility.colors.ColorRGBA;
import naryn.sun.utility.interfaces.IMinecraft;
import naryn.sun.utility.interfaces.IWindow;
import naryn.sun.utility.render.obj.CustomSprite;
import naryn.sun.utility.render.penis.PenisSprite;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec2f;
import ru.kotopushka.compiler.sdk.annotations.Initialization;

public final class DrawUtility implements IMinecraft, IWindow {
   public static final float DEFAULT_SMOOTHNESS = 0.5F;
   public static final HookLimiter limiter = new HookLimiter(true);

   public static GlProgram rectangleProgram;
   public static GlProgram squircleProgram;
   public static GlProgram roundedTextureProgram;
   public static GlProgram squircleTextureProgram;
   public static GlProgram borderProgram;
   public static GlProgram loadingProgram;
   public static GlProgram glassProgram;
   public static GlProgram gradientRectangleProgram;
   public static GlProgram perimeterShadowProgram;
   public static BlurProgram blurProgram;
   public static MotionBlurProgram motionBlurProgram;
   public static SkyProgram skyProgram;

   private static final CustomRenderTarget buffer = new CustomRenderTarget(false);

   @Initialization
   public static void initializeShaders() {
      rectangleProgram = new GlProgram(Sun.id("rectangle/data"), VertexFormats.POSITION_COLOR);
      squircleProgram = new GlProgram(Sun.id("squircle/data"), VertexFormats.POSITION_COLOR);
      squircleTextureProgram = new GlProgram(Sun.id("squircle_texture/data"), VertexFormats.POSITION_TEXTURE_COLOR);
      roundedTextureProgram = new GlProgram(Sun.id("texture/data"), VertexFormats.POSITION_TEXTURE_COLOR);
      borderProgram = new GlProgram(Sun.id("border/data"), VertexFormats.POSITION_COLOR);
      loadingProgram = new GlProgram(Sun.id("loading/data"), VertexFormats.POSITION_COLOR);
      glassProgram = new GlProgram(Sun.id("liquidglass/data"), VertexFormats.POSITION_TEXTURE_COLOR);
      gradientRectangleProgram = new GlProgram(Sun.id("gradient_rectangle/data"), VertexFormats.POSITION_COLOR);
      perimeterShadowProgram = new GlProgram(Sun.id("perimeter_shadow/data"), VertexFormats.POSITION_COLOR);
      blurProgram = new BlurProgram();
      blurProgram.initShaders();
      motionBlurProgram = new MotionBlurProgram();
	  skyProgram = new SkyProgram();
   }

   public static void updateBuffer() {
      buffer.setClearColor(0.0F, 0.0F, 0.0F, 1.0F);
      buffer.setup();
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      mc.getFramebuffer().beginRead();
      RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
      RenderSystem.setShaderTexture(0, mc.getFramebuffer().getColorAttachment());
      drawQuad(0.0F, 0.0F, mw.getScaledWidth(), mw.getScaledHeight(), true);
      mc.getFramebuffer().endRead();
      RenderSystem.disableBlend();
      mc.getFramebuffer().beginWrite(true);
      buffer.stop();
   }

   private static void drawQuad(float x, float y, float width, float height, boolean flip) {
      BufferBuilder builder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      float vTop = flip ? 0.0F : 1.0F;
      float vBottom = flip ? 1.0F : 0.0F;
      builder.vertex(x, y, 0.0F).texture(0.0F, vBottom).color(-1);
      builder.vertex(x, y + height, 0.0F).texture(0.0F, vTop).color(-1);
      builder.vertex(x + width, y + height, 0.0F).texture(1.0F, vTop).color(-1);
      builder.vertex(x + width, y, 0.0F).texture(1.0F, vBottom).color(-1);
      BufferRenderer.drawWithGlobalProgram(builder.end());
   }

   public static void drawSetup() {
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
   }

   public static void drawEnd() {
      RenderSystem.disableBlend();
   }

   // --- Shape and Primitive Delegates ---

   public static void drawLine(MatrixStack matrices, Vec2f from, Vec2f to, ColorRGBA color) {
      ShapeDrawUtility.drawLine(matrices, from, to, color);
   }

   public static void drawBezier(MatrixStack matrices, Vec2f p0, Vec2f p1, Vec2f p2, Vec2f p3, ColorRGBA color, int resolution) {
      ShapeDrawUtility.drawBezier(matrices, p0, p1, p2, p3, color, resolution);
   }

   public static void drawRect(MatrixStack matrices, float x, float y, float width, float height, ColorRGBA color) {
      ShapeDrawUtility.drawRect(matrices, x, y, width, height, color);
   }

   public static void drawSquircle(MatrixStack matrices, float x, float y, float width, float height, float squirt, BorderRadius borderRadius, ColorRGBA color) {
      ShapeDrawUtility.drawSquircle(matrices, x, y, width, height, squirt, borderRadius, color);
   }

   public static void drawLoadingRect(
      MatrixStack matrices, float x, float y, float width, float height, float progress, BorderRadius borderRadius, ColorRGBA color
   ) {
      ShapeDrawUtility.drawLoadingRect(matrices, x, y, width, height, progress, borderRadius, color);
   }

   public static void drawRoundedRect(MatrixStack matrices, float x, float y, float width, float height, BorderRadius borderRadius, ColorRGBA color) {
      ShapeDrawUtility.drawRoundedRect(matrices, x, y, width, height, borderRadius, color);
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
      ShapeDrawUtility.drawRoundedRect(matrices, x, y, width, height, borderRadius, color1, color2, color3, color4);
   }

   public static void drawRoundedRect(MatrixStack matrices, float x, float y, float width, float height, BorderRadius borderRadius, Gradient gradient) {
      ShapeDrawUtility.drawRoundedRect(matrices, x, y, width, height, borderRadius, gradient);
   }

   public static void drawRoundedBorder(
      MatrixStack matrices, float x, float y, float width, float height, float borderThickness, BorderRadius borderRadius, ColorRGBA borderColor
   ) {
      ShapeDrawUtility.drawRoundedBorder(matrices, x, y, width, height, borderThickness, borderRadius, borderColor);
   }

   public static void drawRoundedBorder(
      MatrixStack matrices, float x, float y, float width, float height, float borderThickness, BorderRadius borderRadius,
      ColorRGBA topLeftColor, ColorRGBA bottomLeftColor, ColorRGBA bottomRightColor, ColorRGBA topRightColor
   ) {
      ShapeDrawUtility.drawRoundedBorder(matrices, x, y, width, height, borderThickness, borderRadius, topLeftColor, bottomLeftColor, bottomRightColor, topRightColor);
   }

   public static void drawStar(MatrixStack matrices, float centerX, float centerY, float outerRadius, float innerRadius, ColorRGBA color) {
      ShapeDrawUtility.drawStar(matrices, centerX, centerY, outerRadius, innerRadius, color);
   }

   public static void drawStarOutline(MatrixStack matrices, float centerX, float centerY, float outerRadius, float innerRadius, ColorRGBA color) {
      ShapeDrawUtility.drawStarOutline(matrices, centerX, centerY, outerRadius, innerRadius, color);
   }

   // --- Texture and Sprite Delegates ---

   public static void drawTexture(MatrixStack matrices, Identifier identifier, float x, float y, float width, float height, ColorRGBA textureColor) {
      TextureDrawUtility.drawTexture(matrices, identifier, x, y, width, height, textureColor);
   }

   public static void drawTexture(
      MatrixStack matrices, Identifier identifier, float x, float y, float width, float height, float u1, float u2, float v1, float v2, ColorRGBA clor
   ) {
      TextureDrawUtility.drawTexture(matrices, identifier, x, y, width, height, u1, u2, v1, v2, clor);
   }

   public static void drawAnimationSprite(MatrixStack matrices, PenisSprite sprite, float x, float y, float width, float height, ColorRGBA color) {
      TextureDrawUtility.drawAnimationSprite(matrices, sprite, x, y, width, height, color);
   }

   public static void drawSprite(MatrixStack matrices, CustomSprite sprite, float x, float y, float width, float height, ColorRGBA color) {
      TextureDrawUtility.drawSprite(matrices, sprite, x, y, width, height, color);
   }

   public static void drawRoundedTexture(MatrixStack matrices, Identifier identifier, float x, float y, float width, float height, BorderRadius borderRadius) {
      TextureDrawUtility.drawRoundedTexture(matrices, identifier, x, y, width, height, borderRadius);
   }

   public static void drawRoundedTexture(
      MatrixStack matrices, Identifier identifier, float x, float y, float width, float height, BorderRadius borderRadius, ColorRGBA color
   ) {
      TextureDrawUtility.drawRoundedTexture(matrices, identifier, x, y, width, height, borderRadius, color);
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
      TextureDrawUtility.drawRoundedTextureWithUV(matrices, identifier, x, y, width, height, borderRadius, color, u1, v1, u2, v2);
   }

   public static void drawImage(MatrixStack matrices, BufferBuilder builder, double x, double y, double z, double width, double height, ColorRGBA color) {
      TextureDrawUtility.drawImage(matrices, builder, x, y, z, width, height, color);
   }

   public static void drawImage(MatrixStack matrices, BufferBuilder builder, double x, double y, double z, double width, double height, int colorRGB) {
      TextureDrawUtility.drawImage(matrices, builder, x, y, z, width, height, colorRGB);
   }

   public static void drawImage(MatrixStack matrices, Identifier identifier, double x, double y, double z, double width, double height, ColorRGBA color) {
      TextureDrawUtility.drawImage(matrices, identifier, x, y, z, width, height, color);
   }

   // --- Effect Delegates ---

   public static void drawShadow(MatrixStack matrices, float x, float y, float width, float height, float softness, BorderRadius borderRadius, ColorRGBA color) {
      EffectDrawUtility.drawShadow(matrices, x, y, width, height, softness, borderRadius, color);
   }

   public static void drawPerimeterShadow(MatrixStack matrices, float x, float y, float width, float height, float softness, BorderRadius borderRadius, ColorRGBA color) {
      EffectDrawUtility.drawPerimeterShadow(matrices, x, y, width, height, softness, borderRadius, color);
   }

   public static void drawBlur(
      MatrixStack matrices, float x, float y, float width, float height, float blurRadius, float squirt, BorderRadius borderRadius, ColorRGBA color
   ) {
      EffectDrawUtility.drawBlur(matrices, x, y, width, height, blurRadius, squirt, borderRadius, color);
   }

   public static void drawBlur(MatrixStack matrices, float x, float y, float width, float height, float blurRadius, BorderRadius borderRadius, ColorRGBA color) {
      EffectDrawUtility.drawBlur(matrices, x, y, width, height, blurRadius, borderRadius, color);
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
      EffectDrawUtility.drawLiquidGlass(
         matrices, x, y, width, height, borderRadius, color,
         globalAlpha, fresnelPower, fresnelColor, baseAlpha,
         fresnelInvert, fresnelMix, distortStrength, squirt, clean
      );
   }

   public static void drawNoiseOverlay(MatrixStack matrices, float x, float y, float width, float height, BorderRadius borderRadius, ColorRGBA color) {
      EffectDrawUtility.drawNoiseOverlay(matrices, x, y, width, height, borderRadius, color);
   }

   // --- Entity Head Delegates ---

   public static void drawPlayerHeadWithHat(
      MatrixStack matrices, AbstractClientPlayerEntity player, float x, float y, float size, BorderRadius borderRadius, ColorRGBA color
   ) {
      EntityHeadDrawUtility.drawPlayerHeadWithHat(matrices, player, x, y, size, borderRadius, color);
   }

   public static <T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>> void drawEntityHeadWithHat(
      MatrixStack matrices, T entity, float x, float y, float size, BorderRadius borderRadius, ColorRGBA color
   ) {
      EntityHeadDrawUtility.drawEntityHeadWithHat(matrices, entity, x, y, size, borderRadius, color);
   }

   public static void drawPlayerHeadWithRoundedShader(
      MatrixStack matrices, Identifier skinTexture, float x, float y, float size, BorderRadius borderRadius, ColorRGBA color
   ) {
      EntityHeadDrawUtility.drawPlayerHeadWithRoundedShader(matrices, skinTexture, x, y, size, borderRadius, color);
   }

   @Generated
   public static GlProgram getSquircleProgram() {
      return squircleProgram;
   }

   public record HeadUV(float u1, float v1, float uSize, float vSize) {
   }

   @Generated
   private DrawUtility() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}	
