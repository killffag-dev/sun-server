package naryn.sun.systems.modules.modules.visuals;

import lombok.Generated;
import naryn.sun.systems.modules.api.ModuleCategory;
import naryn.sun.systems.modules.api.ModuleInfo;
import naryn.sun.systems.modules.impl.BaseModule;
import naryn.sun.systems.setting.settings.ColorSetting;
import naryn.sun.systems.setting.settings.ModeSetting;
import naryn.sun.utility.colors.ColorRGBA;

@ModuleInfo(name = "Hit Color", desc = "modules.descriptions.hit_color", category = ModuleCategory.VISUALS)
public class HitColor extends BaseModule {
   private final ModeSetting target = new ModeSetting(this, "modules.settings.hit_color.target");
   private final ModeSetting.Value body = new ModeSetting.Value(this.target, "modules.settings.hit_color.target.body");
   private final ModeSetting.Value armor = new ModeSetting.Value(this.target, "modules.settings.hit_color.target.armor");
   private final ModeSetting.Value both = new ModeSetting.Value(this.target, "modules.settings.hit_color.target.both");
   private final ColorSetting colorSetting = new ColorSetting(this, "modules.settings.hit_color.color")
      .color(new ColorRGBA(255.0F, 0.0F, 0.0F).withAlpha(255.0F));

   // Мост между ArmorFeatureRendererMixin (знает state.hurt) и EquipmentRendererMixin
   // (рисует конкретный слой брони, но state там уже недоступен). Однопоточный рендер —
   // гонок нет, флаг выставляется в HEAD render()'а брони и гасится в TAIL того же метода.
   private boolean hurtArmor;

   public int getPackedColor() {
      ColorRGBA c = this.colorSetting.getColor();
      int a = (int) c.getAlpha();
      int r = (int) c.getRed();
      int g = (int) c.getGreen();
      int b = (int) c.getBlue();
      return (a << 24) | (r << 16) | (g << 8) | b;
   }

   @Generated
   public ModeSetting getTarget() {
      return this.target;
   }

   @Generated
   public ModeSetting.Value getBody() {
      return this.body;
   }

   @Generated
   public ModeSetting.Value getArmor() {
      return this.armor;
   }

   @Generated
   public ModeSetting.Value getBoth() {
      return this.both;
   }

   @Generated
   public ColorSetting getColorSetting() {
      return this.colorSetting;
   }

   @Generated
   public boolean isHurtArmor() {
      return this.hurtArmor;
   }

   @Generated
   public void setHurtArmor(boolean hurtArmor) {
      this.hurtArmor = hurtArmor;
   }
}