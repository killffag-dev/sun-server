package naryn.sun.framework.shader.impl;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.mojang.blaze3d.systems.RenderSystem;
import naryn.sun.Sun;
import naryn.sun.framework.shader.GlProgram;
import naryn.sun.utility.interfaces.IMinecraft;
import naryn.sun.utility.interfaces.IWindow;
import naryn.sun.utility.render.CustomRenderTarget;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.VertexFormats;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class MotionBlurProgram extends GlProgram implements IMinecraft, IWindow {
   private static final Supplier<CustomRenderTarget> SWAP = Suppliers.memoize(() -> new CustomRenderTarget(false).setLinear());

   private GlUniform gamePrevModelViewUniform;
   private GlUniform gamePrevProjectionUniform;
   private GlUniform inverseGameModelViewUniform;
   private GlUniform inverseGameProjectionUniform;
   private GlUniform cameraPosUniform;
   private GlUniform prevCameraPosUniform;
   private GlUniform viewResUniform;
   private GlUniform blendFactorUniform;
   private GlUniform motionBlurSamplesUniform;
   private GlUniform blurAlgorithmUniform;
   private GlUniform useDepthUniform;

   private final Matrix4f invModelView = new Matrix4f();
   private final Matrix4f invProjection = new Matrix4f();

   public MotionBlurProgram() {
      super(Sun.id("motion_blur/data"), VertexFormats.POSITION_TEXTURE_COLOR);
   }

   @Override
   protected void setup() {
      this.gamePrevModelViewUniform = this.findUniform("GamePrevModelView");
      this.gamePrevProjectionUniform = this.findUniform("GamePrevProjection");
      this.inverseGameModelViewUniform = this.findUniform("InverseGameModelView");
      this.inverseGameProjectionUniform = this.findUniform("InverseGameProjection");
      this.cameraPosUniform = this.findUniform("CameraPos");
      this.prevCameraPosUniform = this.findUniform("PrevCameraPos");
      this.viewResUniform = this.findUniform("ViewRes");
      this.blendFactorUniform = this.findUniform("BlendFactor");
      this.motionBlurSamplesUniform = this.findUniform("MotionBlurSamples");
      this.blurAlgorithmUniform = this.findUniform("BlurAlgorithm");
      this.useDepthUniform = this.findUniform("UseDepth");
      super.setup();
   }

   public void apply(
      Matrix4f modelView,
      Matrix4f prevModelView,
      Matrix4f projection,
      Matrix4f prevProjection,
      Vector3f cameraPos,
      Vector3f prevCameraPos,
      float blendFactor,
      int samples,
      int algorithmOrdinal,
      boolean useDepth
   ) {
      Framebuffer mainTarget = mc.getFramebuffer();
      CustomRenderTarget swap = SWAP.get();

      int targetWidth = mainTarget.textureWidth;
      int targetHeight = mainTarget.textureHeight;

      if (swap.textureWidth != targetWidth || swap.textureHeight != targetHeight) {
         swap.resize(targetWidth, targetHeight);
      }

      swap.clear();
      swap.beginWrite(false);

      this.use();
      RenderSystem.setShaderTexture(0, mainTarget.getColorAttachment());
      RenderSystem.setShaderTexture(1, mainTarget.getDepthAttachment());

      this.invModelView.set(modelView).invert();
      this.invProjection.set(projection).invert();

      if (this.gamePrevModelViewUniform != null) {
         this.gamePrevModelViewUniform.set(prevModelView);
      }
      if (this.gamePrevProjectionUniform != null) {
         this.gamePrevProjectionUniform.set(prevProjection);
      }
      if (this.inverseGameModelViewUniform != null) {
         this.inverseGameModelViewUniform.set(this.invModelView);
      }
      if (this.inverseGameProjectionUniform != null) {
         this.inverseGameProjectionUniform.set(this.invProjection);
      }
      if (this.cameraPosUniform != null) {
         this.cameraPosUniform.set(cameraPos.x, cameraPos.y, cameraPos.z);
      }
      if (this.prevCameraPosUniform != null) {
         this.prevCameraPosUniform.set(prevCameraPos.x, prevCameraPos.y, prevCameraPos.z);
      }
      if (this.viewResUniform != null) {
         this.viewResUniform.set((float) targetWidth, (float) targetHeight);
      }
      if (this.blendFactorUniform != null) {
         this.blendFactorUniform.set(blendFactor);
      }
      if (this.motionBlurSamplesUniform != null) {
         this.motionBlurSamplesUniform.set(samples);
      }
      if (this.blurAlgorithmUniform != null) {
         this.blurAlgorithmUniform.set(algorithmOrdinal);
      }
      if (this.useDepthUniform != null) {
         this.useDepthUniform.set(useDepth ? 1 : 0);
      }

      RenderSystem.disableDepthTest();
      RenderSystem.disableCull();
      this.drawFullscreenQuad();
      RenderSystem.enableCull();

      swap.endWrite();
      mainTarget.beginWrite(true);
      swap.draw(mainTarget.textureWidth, mainTarget.textureHeight);

      RenderSystem.setShaderTexture(0, 0);
      RenderSystem.setShaderTexture(1, 0);
   }

   private void drawFullscreenQuad() {
      int color = -1;
      BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      builder.vertex(-1.0F, -1.0F, 0.0F).texture(0.0F, 0.0F).color(color);
      builder.vertex(-1.0F, 1.0F, 0.0F).texture(0.0F, 1.0F).color(color);
      builder.vertex(1.0F, 1.0F, 0.0F).texture(1.0F, 1.0F).color(color);
      builder.vertex(1.0F, -1.0F, 0.0F).texture(1.0F, 0.0F).color(color);
      BufferRenderer.drawWithGlobalProgram(builder.end());
   }
}