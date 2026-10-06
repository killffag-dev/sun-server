package naryn.sun.systems.modules.constructions.swinganim;

import java.util.ArrayList;
import java.util.List;
import naryn.sun.systems.setting.Setting;
import naryn.sun.systems.setting.SettingsContainer;

public class SwingSettings implements SettingsContainer {
   protected final List<Setting> settings = new ArrayList<>();

   @Override
   public List<Setting> getSettings() {
      return this.settings;
   }
}
