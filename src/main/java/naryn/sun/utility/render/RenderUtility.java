package naryn.sun.utility.render;

import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Generated;
import naryn.sun.utility.interfaces.IMinecraft;
import naryn.sun.utility.interfaces.IWindow;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

public final class RenderUtility implements IMinecraft, IWindow {
   public static void rotate(MatrixStack ms, float x, float y, float value) {
      ms.push();
      ms.translate(x, y, 0.0F);
      ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(value));
      ms.translate(-x, -y, 0.0F);
   }

   public static void scale(MatrixStack ms, float x, float y, float scale) {
      ms.push();
      ms.translate(x, y, 0.0F);
      ms.scale(scale, scale, 1.0F);
      ms.translate(-x, -y, 0.0F);
   }

   public static void end(MatrixStack ms) {
      ms.pop();
   }

   public static void prepareMatrices(MatrixStack matrices) {
      Camera camera = mc.gameRenderer.getCamera();
      Vec3d cameraPos = camera.getPos();
      matrices.translate(-cameraPos.getX(), -cameraPos.getY(), -cameraPos.getZ());
   }

   public static void prepareMatrices(MatrixStack matrices, Vec3d pos) {
      prepareMatrices(matrices, pos.getX(), pos.getY(), pos.getZ());
   }

   public static void prepareMatrices(MatrixStack matrices, double x, double y, double z) {
      Camera camera = mc.gameRenderer.getCamera();
      Vec3d cameraPos = camera.getPos();
      matrices.translate(x - cameraPos.getX(), y - cameraPos.getY(), z - cameraPos.getZ());
   }

   public static void setupRender3D(boolean bloomColor) {
      RenderSystem.enableBlend();
      RenderSystem.disableCull();
      RenderSystem.disableDepthTest();
      RenderSystem.defaultBlendFunc();
      RenderSystem.depthMask(false);
      if (bloomColor) {
         RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
      } else {
         RenderSystem.defaultBlendFunc();
      }
   }

   public static void endRender3D() {
      RenderSystem.depthMask(true);
      RenderSystem.disableBlend();
      RenderSystem.enableDepthTest();
      RenderSystem.enableCull();
   }

   public static void buildBuffer(BufferBuilder builder) {
      BuiltBuffer builtBuffer = builder.endNullable();
      if (builtBuffer != null) {
         BufferRenderer.drawWithGlobalProgram(builtBuffer);
      }
   }

   @Generated
   private RenderUtility() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
