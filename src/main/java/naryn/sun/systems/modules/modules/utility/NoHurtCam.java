package naryn.sun.systems.modules.modules.utility;

import lombok.Generated;
import naryn.sun.systems.modules.api.ModuleCategory;
import naryn.sun.systems.modules.api.ModuleInfo;
import naryn.sun.systems.modules.impl.BaseModule;
import naryn.sun.systems.setting.settings.SliderSetting;

@ModuleInfo(
   name = "NoHurtCam",
   category = ModuleCategory.UTILITY,
   desc = "modules.descriptions.no_hurt_cam"
)
public class NoHurtCam extends BaseModule {
   private final SliderSetting strength = new SliderSetting(this, "modules.settings.no_hurt_cam.strength")
      .min(0.0F)
      .max(100.0F)
      .step(1.0F)
      .currentValue(0.0F)
      .suffix("%");

   @Generated
   public SliderSetting getStrength() {
      return this.strength;
   }

   public float getFactor() {
      return this.strength.getCurrentValue() / 100.0F;
   }
}
