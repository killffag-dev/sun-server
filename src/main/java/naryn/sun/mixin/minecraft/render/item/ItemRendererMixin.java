package naryn.sun.mixin.minecraft.render.item;

import naryn.sun.Sun;
import naryn.sun.systems.modules.modules.optimization.NoRender;
import naryn.sun.systems.modules.modules.optimization.Optimizer;
import net.minecraft.client.render.item.ItemRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {

   @ModifyVariable(
      method = "getArmorGlintConsumer",
      at = @At("HEAD"),
      argsOnly = true,
      ordinal = 0
   )
   private static boolean sun$disableArmorGlint(boolean glint) {
      Optimizer optimizer = Sun.getInstance().getModuleManager().getModule(Optimizer.class);
      if (optimizer != null && optimizer.isEnabled() && optimizer.getDisableGlint().isEnabled()) {
         return false;
      }
      NoRender noRender = Sun.getInstance().getModuleManager().getModule(NoRender.class);
      if (noRender != null && noRender.isEnabled() && noRender.getGlint().isSelected()) {
         return false;
      }
      return glint;
   }

   @ModifyVariable(
      method = "getItemGlintConsumer",
      at = @At("HEAD"),
      argsOnly = true,
      ordinal = 0
   )
   private static boolean sun$disableItemGlint(boolean glint) {
      Optimizer optimizer = Sun.getInstance().getModuleManager().getModule(Optimizer.class);
      if (optimizer != null && optimizer.isEnabled() && optimizer.getDisableGlint().isEnabled()) {
         return false;
      }
      NoRender noRender = Sun.getInstance().getModuleManager().getModule(NoRender.class);
      if (noRender != null && noRender.isEnabled() && noRender.getGlint().isSelected()) {
         return false;
      }
      return glint;
   }
}
