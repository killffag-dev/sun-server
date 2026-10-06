package naryn.sun.systems.modules;

import naryn.sun.systems.localization.Localizator;
import naryn.sun.systems.modules.api.ModuleCategory;
import naryn.sun.systems.modules.api.ModuleInfo;
import naryn.sun.systems.setting.SettingsContainer;
import naryn.sun.utility.animation.base.Animation;
import naryn.sun.utility.interfaces.IMinecraft;
import naryn.sun.utility.interfaces.IScaledResolution;
import naryn.sun.utility.interfaces.Toggleable;

public interface Module extends Toggleable, IMinecraft, IScaledResolution, SettingsContainer {
   void disable();

   void enable();

   void tick();

   ModuleInfo getInfo();

   String getName();

   default String getDisplayName() {
      String translationKey = "modules.names.%s".formatted(this.getName().toLowerCase().replace(" ", "_"));
      return Localizator.translateOrDefault(translationKey, this.getName());
   }

   default String getDescription() {
      if (this.getInfo() != null && !this.getInfo().desc().isEmpty()) {
         String descKey = this.getInfo().desc();
         String translated = Localizator.translate(descKey);
         if (!translated.equals(descKey)) {
            return translated;
         }
         return descKey;
      }
      String translationKey = "modules.descriptions.%s".formatted(this.getName().toLowerCase().replace(" ", "_"));
      return Localizator.translate(translationKey);
   }

   int getKey();

   ModuleCategory getCategory();

   boolean isEnabled();

   boolean isHidden();

   boolean isFavorite();

   void setFavorite(boolean var1);

   Animation getKeybindsAnimation();

   void setKey(int var1);

   void setEnabled(boolean var1, boolean var2);

   default boolean isHoldToActivate() {
      return this.getInfo() != null && this.getInfo().holdToActivate();
   }
}