package naryn.sun.mixin.minecraft.text;

import naryn.sun.Sun;
import naryn.sun.systems.modules.modules.utility.NameProtect;
import naryn.sun.systems.modules.modules.visuals.NameUtility;
import net.minecraft.text.TextVisitFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(TextVisitFactory.class)
public class TextVisitFactoryMixin {
   @ModifyArg(
      method = "visitFormatted(Ljava/lang/String;ILnet/minecraft/text/Style;Lnet/minecraft/text/CharacterVisitor;)Z",
      index = 0,
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/text/TextVisitFactory;visitFormatted(Ljava/lang/String;ILnet/minecraft/text/Style;Lnet/minecraft/text/Style;Lnet/minecraft/text/CharacterVisitor;)Z",
         ordinal = 0
      )
   )
   private static String patchName(String text) {
      NameProtect nameProtectModule = Sun.getInstance().getModuleManager().getModule(NameProtect.class);
      if (nameProtectModule != null && nameProtectModule.isEnabled()) {
         text = nameProtectModule.patchName(text);
      }
      NameUtility nameUtility = Sun.getInstance().getModuleManager().getModule(NameUtility.class);
      if (nameUtility != null && nameUtility.isEnabled() && nameUtility.getHideNick().isEnabled()) {
         text = nameUtility.patchName(text);
      }
      return text;
   }
}
