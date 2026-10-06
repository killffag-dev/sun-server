package naryn.sun.ui.menu.api;

import lombok.Generated;
import naryn.sun.systems.modules.api.ModuleCategory;
import naryn.sun.utility.render.obj.CustomSprite;
import naryn.sun.systems.localization.Localizator;

public enum MenuCategory {
   VISUALS("menu.tab.visuals", ModuleCategory.VISUALS, CustomSprite.VISUALS, CustomSprite.BIG_VISUALS),
   UTILITY("menu.tab.utility", ModuleCategory.UTILITY, CustomSprite.OTHER, CustomSprite.BIG_OTHER),
   OPTIMIZATION("menu.tab.optimization", ModuleCategory.OPTIMIZATION, CustomSprite.CHECK, CustomSprite.CHECK);

   private final String nameKey;
   private final ModuleCategory category;
   private final CustomSprite menuSprite;
   private final CustomSprite bigMenuSprite;

   @Generated
   public String getName() {
      return Localizator.translate(this.nameKey);
   }

   @Generated
   public ModuleCategory getCategory() {
      return this.category;
   }

   @Generated
   public CustomSprite getMenuSprite() {
      return this.menuSprite;
   }

   @Generated
   public CustomSprite getBigMenuSprite() {
      return this.bigMenuSprite;
   }

   @Generated
   private MenuCategory(final String nameKey, final ModuleCategory category, final CustomSprite menuSprite, final CustomSprite bigMenuSprite) {
      this.nameKey = nameKey;
      this.category = category;
      this.menuSprite = menuSprite;
      this.bigMenuSprite = bigMenuSprite;
   }
}
