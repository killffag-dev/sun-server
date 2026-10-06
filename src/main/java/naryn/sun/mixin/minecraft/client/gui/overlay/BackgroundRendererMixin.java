package naryn.sun.mixin.minecraft.client.gui.overlay;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import naryn.sun.Sun;
import naryn.sun.systems.modules.modules.optimization.NoRender;
import naryn.sun.systems.modules.modules.utility.AntiOverlay;
import naryn.sun.systems.modules.modules.visuals.CustomFog;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.BackgroundRenderer.FogType;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Fog;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BackgroundRenderer.class)
public class BackgroundRendererMixin {
   @Inject(
      method = "getFogModifier(Lnet/minecraft/entity/Entity;F)Lnet/minecraft/client/render/BackgroundRenderer$StatusEffectFogModifier;",
      at = @At("HEAD"),
      cancellable = true
   )
   private static void onGetFogModifier(Entity entity, float tickDelta, CallbackInfoReturnable<Object> info) {
      AntiOverlay antiOverlay = Sun.getInstance().getModuleManager().getModule(AntiOverlay.class);
      if (antiOverlay != null && antiOverlay.isEnabled() && antiOverlay.getBlindness().isSelected()) {
         info.setReturnValue(null);
         return;
      }
      NoRender noRender = Sun.getInstance().getModuleManager().getModule(NoRender.class);
      if (noRender != null && noRender.isEnabled() && noRender.getBlindness().isSelected()) {
         info.setReturnValue(null);
      }
   }

   @ModifyReturnValue(method = "applyFog", at = @At("RETURN"))
   private static Fog modifyFogProperties(
      Fog original, @Local(argsOnly = true) Camera camera, @Local(argsOnly = true) FogType fogType, @Local(argsOnly = true, ordinal = 0) float viewDistance
   ) {
      CustomFog customFogModule = Sun.getInstance().getModuleManager().getModule(CustomFog.class);
      if (customFogModule != null && customFogModule.isEnabled()) {
         return customFogModule.modifyFog(original, camera, fogType, viewDistance);
      }
      return original;
   }
}