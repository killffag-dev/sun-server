package naryn.sun.mixin.minecraft.text;

import naryn.sun.Sun;
import naryn.sun.systems.modules.modules.visuals.NameUtility;
import net.minecraft.text.TextVisitFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(TextVisitFactory.class)
public class TextVisitFactoryMixin {
   @org.spongepowered.asm.mixin.Unique private static NameUtility sun$nameUtility;

   @org.spongepowered.asm.mixin.Unique
   private static NameUtility sun$getNameUtility() {
      if (sun$nameUtility == null && Sun.getInstance() != null && Sun.getInstance().getModuleManager() != null) {
         sun$nameUtility = Sun.getInstance().getModuleManager().getModule(NameUtility.class);
      }
      return sun$nameUtility;
   }

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
      NameUtility nameUtility = sun$getNameUtility();
      if (nameUtility != null && nameUtility.isEnabled() && nameUtility.getHideNick().isEnabled()) {
         text = nameUtility.patchName(text);
      }
      return text;
   }
}
