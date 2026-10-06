package naryn.sun.systems.setting.impl;

import java.util.function.BooleanSupplier;
import lombok.Generated;
import naryn.sun.systems.setting.Setting;
import naryn.sun.systems.setting.SettingsContainer;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractSetting implements Setting {
   private final String name;
   @NotNull
   private final BooleanSupplier hideCondition;

   public AbstractSetting(@NotNull SettingsContainer parent, String name, @NotNull BooleanSupplier hideCondition) {
      this.name = name;
      this.hideCondition = hideCondition;
      this.register(parent);
   }

   public AbstractSetting(@NotNull SettingsContainer parent, String name) {
      this(parent, name, () -> false);
   }

   @Override
   public void register(SettingsContainer parent) {
      parent.getSettings().add(this);
   }

   @Override
   public String getDescription() {
      String descKey = this.getName() + ".desc";
      String trans = naryn.sun.systems.localization.Localizator.translate(descKey);
      if (!trans.equals(descKey)) {
         return trans;
      }
      String longDescKey = this.getName() + ".description";
      trans = naryn.sun.systems.localization.Localizator.translate(longDescKey);
      if (!trans.equals(longDescKey)) {
         return trans;
      }
      return "";
   }

   @Generated
   @Override
   public String getName() {
      return this.name;
   }

   @NotNull
   @Generated
   @Override
   public BooleanSupplier getHideCondition() {
      return this.hideCondition;
   }
}
