package naryn.sun.mixin.minecraft.client.option;

import naryn.sun.Sun;
import naryn.sun.systems.modules.modules.visuals.Fullbright;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.SimpleOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SimpleOption.class)
public class SimpleOptionMixin<T> {
   @Inject(method = "getValue", at = @At("HEAD"), cancellable = true)
   public void getGammaValue(CallbackInfoReturnable<Double> cir) {
      if (Sun.getInstance().getModuleManager() != null) {
         Fullbright fullbrightModule = Sun.getInstance().getModuleManager().getModule(Fullbright.class);
         if (fullbrightModule != null
            && fullbrightModule.isEnabled()
            && MinecraftClient.getInstance().options.getGamma() == (Object)this) {
            cir.setReturnValue(1337.0);
         }
      }
   }
}
