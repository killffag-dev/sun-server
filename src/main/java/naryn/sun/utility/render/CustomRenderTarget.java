package naryn.sun.utility.render;

import com.mojang.blaze3d.systems.RenderSystem;
import naryn.sun.utility.interfaces.IMinecraft;
import naryn.sun.utility.interfaces.IWindow;
import net.minecraft.client.gl.Framebuffer;

public class CustomRenderTarget extends Framebuffer implements IMinecraft, IWindow {
   private boolean linear;
   private float downscale = 1.0F;

   public CustomRenderTarget(boolean useDepth) {
      super(useDepth);
   }

   public CustomRenderTarget(int width, int height, boolean useDepth) {
      super(useDepth);
      this.resize(width, height);
   }

   public CustomRenderTarget setLinear() {
      if (!this.linear) {
         this.linear = true;
         RenderSystem.recordRenderCall(() -> this.setTexFilter(9729));
      }
      return this;
   }

   public CustomRenderTarget setDownscale(float factor) {
      this.downscale = Math.max(0.1F, Math.min(1.0F, factor));
      return this;
   }

   public void setTexFilter(int texFilter) {
      super.setTexFilter(this.linear ? 9729 : texFilter);
   }

   private int getTargetWidth() {
      int fbWidth = mc.getWindow() != null ? mc.getWindow().getFramebufferWidth() : (int) mw.getScaledWidth();
      return Math.max((int) Math.floor(fbWidth * this.downscale), 1);
   }

   private int getTargetHeight() {
      int fbHeight = mc.getWindow() != null ? mc.getWindow().getFramebufferHeight() : (int) mw.getScaledHeight();
      return Math.max((int) Math.floor(fbHeight * this.downscale), 1);
   }

   private void resizeFramebuffer() {
      int targetWidth = getTargetWidth();
      int targetHeight = getTargetHeight();
      if (this.textureWidth != targetWidth || this.textureHeight != targetHeight) {
         this.initFbo(targetWidth, targetHeight);
      }
   }

   public void setup(boolean clear) {
      this.resizeFramebuffer();
      if (clear) {
         this.clear();
      }

      this.beginWrite(false);
   }

   public void setup() {
      this.setup(true);
   }

   public void stop() {
      this.endWrite();
      mc.getFramebuffer().beginWrite(true);
   }

   private boolean needsNewFramebuffer() {
      return this.textureWidth != getTargetWidth() || this.textureHeight != getTargetHeight();
   }
}
