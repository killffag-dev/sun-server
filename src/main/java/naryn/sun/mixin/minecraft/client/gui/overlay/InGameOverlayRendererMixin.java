package naryn.sun.mixin.minecraft.client.gui.overlay;

import naryn.sun.Sun;
import naryn.sun.systems.modules.modules.optimization.NoRender;
import naryn.sun.systems.modules.modules.utility.AntiOverlay;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameOverlayRenderer.class)
public class InGameOverlayRendererMixin {
   @Inject(method = "renderFireOverlay", at = @At("HEAD"), cancellable = true)
   private static void renderFireOverlayHook(MatrixStack matrices, VertexConsumerProvider vertexConsumers, CallbackInfo ci) {
      AntiOverlay antiOverlay = Sun.getInstance().getModuleManager().getModule(AntiOverlay.class);
      if (antiOverlay != null && antiOverlay.isEnabled() && antiOverlay.getFire().isSelected()) {
         ci.cancel();
         return;
      }
      NoRender noRender = Sun.getInstance().getModuleManager().getModule(NoRender.class);
      if (noRender != null && noRender.isEnabled() && noRender.getFire().isSelected()) {
         ci.cancel();
      }
   }
}
