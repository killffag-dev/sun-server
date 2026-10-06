package naryn.sun.protection.client;

import naryn.sun.Sun;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.kotopushka.compiler.sdk.annotations.VMProtect;
import ru.kotopushka.compiler.sdk.enums.VMProtectType;

public class MinecraftClientMixinProtection {
   @VMProtect(type = VMProtectType.MUTATION)
   public static void init() {
      Sun.INSTANCE.initialize();
   }

   @VMProtect(type = VMProtectType.MUTATION)
   public static void shutdown() {
      Sun.INSTANCE.shutdown();
   }

   public static void updateTitle(CallbackInfoReturnable<String> cir) {
      String title = "%s Client %s".formatted(Sun.NAME, Sun.VERSION);
      cir.setReturnValue(title);
   }
}
