package naryn.sun.mixin.minecraft.client.render;

import naryn.sun.Sun;
import naryn.sun.systems.modules.modules.utility.AntiOverlay;
import net.minecraft.client.render.Fog;
import net.minecraft.client.render.FrameGraphBuilder;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// This hook must remain a mixin because Fabric has no cancellable weather event.
// Apply before renderer replacements so the cancellation point is established
// before Sodium merges its WorldRenderer changes.
@Mixin(value = WorldRenderer.class, priority = 1100)
public abstract class WeatherRendererMixin {
   @Inject(method = "renderWeather", at = @At("HEAD"), cancellable = true)
   private void onRenderWeather(FrameGraphBuilder frameGraphBuilder, Vec3d pos, float tickDelta, Fog fog, CallbackInfo ci) {
      AntiOverlay antiOverlay = Sun.getInstance().getModuleManager().getModule(AntiOverlay.class);
      if (antiOverlay != null && antiOverlay.isEnabled() && antiOverlay.getWeather().isSelected()) {
         ci.cancel();
      }
   }
}
