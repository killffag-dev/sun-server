package naryn.sun.mixin.minecraft.client.gui.overlay;

import naryn.sun.Sun;
import naryn.sun.mixin.accessors.CameraAccessor;
import naryn.sun.systems.modules.modules.utility.Freelook;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {

   // protected Camera.setRotation(float,float) вынесен в CameraAccessor
   // (Version Adapter, Этап 3) как invoker.

   @Inject(
      method = "update",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setRotation(FF)V", ordinal = 1, shift = At.Shift.AFTER)
   )
   private void onRotationSet(CallbackInfo ci) {
      Freelook freelook = Sun.getInstance().getModuleManager().getModule(Freelook.class);
      if (freelook != null && freelook.isEnabled()) {
         ((CameraAccessor) (Object) this).invokeSetRotation(freelook.getFreelookYaw(), freelook.getFreelookPitch());
      }
   }
}