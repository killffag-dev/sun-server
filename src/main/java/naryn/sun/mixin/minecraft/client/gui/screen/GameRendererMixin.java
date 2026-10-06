package naryn.sun.mixin.minecraft.client.gui.screen;

import com.llamalad7.mixinextras.sugar.Local;
import naryn.sun.Sun;
import naryn.sun.systems.modules.modules.utility.Zoom;
import naryn.sun.systems.modules.modules.optimization.NoRender;
import naryn.sun.systems.modules.modules.utility.AntiOverlay;
import naryn.sun.systems.modules.modules.utility.NoHurtCam;
import naryn.sun.utility.render.Utils;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

   @Inject(method = "render", at = @At("HEAD"))
   private void onRenderFrameStart(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci) {
      naryn.sun.utility.profiler.PerformanceProfiler.getInstance().onFrameStart();
   }

   @Inject(method = "render", at = @At("RETURN"))
   private void onRenderFrameEnd(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci) {
      naryn.sun.utility.profiler.PerformanceProfiler.getInstance().onFrameEnd();
   }

   @Inject(
      method = "renderWorld",
      at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiler/Profiler;swap(Ljava/lang/String;)V", args = "ldc=hand")
   )
   private void onRenderWorld(
      RenderTickCounter tickCounter,
      CallbackInfo ci,
      @Local(ordinal = 0) Matrix4f projection,
      @Local(ordinal = 2) Matrix4f view,
      @Local(ordinal = 1) float tickDelta,
      @Local MatrixStack matrices
   ) {
      Utils.onRender(view, projection);
   }

   @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true)
   private void tiltViewWhenHurtHook(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
      NoHurtCam noHurtCam = Sun.getInstance().getModuleManager().getModule(NoHurtCam.class);
      if (noHurtCam != null && noHurtCam.isEnabled() && noHurtCam.getFactor() <= 0.0F) {
         ci.cancel();
         return;
      }
      NoRender noRender = Sun.getInstance().getModuleManager().getModule(NoRender.class);
      if (noRender != null && noRender.isEnabled() && noRender.getHurtCam().isSelected()) {
         ci.cancel();
      }
   }

   @Redirect(method = "renderWorld", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;lerp(FFF)F"))
   private float renderWorldHook(float delta, float first, float second) {
      AntiOverlay antiOverlay = Sun.getInstance().getModuleManager().getModule(AntiOverlay.class);
      if (antiOverlay != null && antiOverlay.isEnabled() && antiOverlay.getNausea().isSelected()) {
         return 0.0F;
      }
      NoRender noRender = Sun.getInstance().getModuleManager().getModule(NoRender.class);
      if (noRender != null && noRender.isEnabled() && noRender.getNausea().isSelected()) {
         return 0.0F;
      }
      return MathHelper.lerp(delta, first, second);
   }

   @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
   private void modifyFov(CallbackInfoReturnable<Float> cir) {
      Zoom zoom = Sun.getInstance().getModuleManager().getModule(Zoom.class);
      if (zoom != null && zoom.isEnabled()) {
         cir.setReturnValue(cir.getReturnValue() * (float) zoom.getCurrentFovMult());
      }
   }
}