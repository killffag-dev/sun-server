package naryn.sun.systems.modules.modules.visuals;

import lombok.Generated;
import naryn.sun.systems.modules.api.ModuleCategory;
import naryn.sun.systems.modules.api.ModuleInfo;
import naryn.sun.systems.modules.impl.BaseModule;
import naryn.sun.systems.setting.settings.BooleanSetting;
import naryn.sun.systems.setting.settings.ColorSetting;
import naryn.sun.systems.setting.settings.GroupSetting;
import naryn.sun.systems.setting.settings.ModeSetting;
import naryn.sun.systems.setting.settings.RangeSetting;
import naryn.sun.utility.colors.ColorRGBA;
import naryn.sun.utility.colors.Colors;
import naryn.sun.utility.compatibility.IrisCompatibility;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Fog;
import net.minecraft.client.render.FogShape;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.MathHelper;

@ModuleInfo(name = "Custom Fog", category = ModuleCategory.VISUALS)
public class CustomFog extends BaseModule {
   // === 1. Mode First (Top-Level) ===
   private final ModeSetting mode = new ModeSetting(this, "modules.settings.custom_fog.mode");
   private final ModeSetting.Value custom = new ModeSetting.Value(this.mode, "modules.settings.custom_fog.mode.custom").select();
   private final ModeSetting.Value noFog = new ModeSetting.Value(this.mode, "modules.settings.custom_fog.mode.no_fog");

   // === 2. Mode-Specific Settings Box ===
   private final GroupSetting customGroup = new GroupSetting(this, "modules.settings.custom_fog.group.custom", () -> !this.custom.isSelected());
   private final RangeSetting distance = new RangeSetting(this.customGroup, "modules.settings.custom_fog.distance")
      .min(1.0F)
      .max(100.0F)
      .step(1.0F)
      .firstValue(10.0F)
      .secondValue(50.0F);

   // === 3. General & Logic Box ===
   private final GroupSetting generalGroup = new GroupSetting(this, "modules.settings.custom_fog.group.general");
   private final BooleanSetting removeLiquidFog = new BooleanSetting(this.generalGroup, "modules.settings.custom_fog.remove_liquid_fog", () -> !this.noFog.isSelected());

   // === 4. Colors at the Very Bottom ===
   private final GroupSetting visualGroup = new GroupSetting(this, "modules.settings.custom_fog.group.visual", () -> !this.custom.isSelected());
   private final ColorSetting fogColor = new ColorSetting(this.visualGroup, "modules.settings.custom_fog.color")
      .color(Colors.ACCENT)
      .alpha(true);

   public boolean isNoFog() {
      return this.isEnabled() && this.noFog.isSelected();
   }

   public boolean isCustom() {
      return this.isEnabled() && this.custom.isSelected();
   }

   public Fog modifyFog(Fog original, Camera camera, BackgroundRenderer.FogType fogType, float viewDistance) {
      if (!this.isEnabled() || mc.world == null || mc.player == null) {
         return original;
      }
      if (IrisCompatibility.isShadersActive()) {
         return original;
      }

      if (this.noFog.isSelected()) {
         CameraSubmersionType submersionType = camera.getSubmersionType();
         boolean inLiquid = submersionType == CameraSubmersionType.WATER
            || submersionType == CameraSubmersionType.LAVA
            || submersionType == CameraSubmersionType.POWDER_SNOW;

         if (!inLiquid || this.removeLiquidFog.isEnabled()) {
            float far = Math.max(viewDistance, 1000.0F) * 4.0F;
            return new Fog(far, far * 2.0F, FogShape.SPHERE, 0.0F, 0.0F, 0.0F, 0.0F);
         }
         return original;
      }

      if (fogType == BackgroundRenderer.FogType.FOG_TERRAIN && this.shouldModifyFog(camera)) {
         float start = MathHelper.clamp(this.distance.getFirstValue(), -8.0F, viewDistance);
         float end = MathHelper.clamp(this.distance.getSecondValue(), 0.0F, viewDistance);
         ColorRGBA color = this.fogColor.getColor();
         FogShape shape = FogShape.SPHERE;
         float r = color.getRed() / 255.0F;
         float g = color.getGreen() / 255.0F;
         float b = color.getBlue() / 255.0F;
         float a = color.getAlpha() / 255.0F;
         return new Fog(start, end, shape, r, g, b, a);
      }

      return original;
   }

   public boolean shouldModifyFog(Camera camera) {
      if (IrisCompatibility.isShadersActive()) {
         return false;
      }
      if (this.isEnabled() && mc.world != null && mc.player != null) {
         Entity entity = camera.getFocusedEntity();
         if (camera.getSubmersionType() == CameraSubmersionType.WATER) {
            return false;
         } else if (camera.getSubmersionType() == CameraSubmersionType.LAVA) {
            return false;
         } else if (camera.getSubmersionType() == CameraSubmersionType.POWDER_SNOW) {
            return false;
         } else {
            if (entity instanceof LivingEntity livingEntity) {
               if (livingEntity.hasStatusEffect(StatusEffects.BLINDNESS)) {
                  return false;
               }

               if (livingEntity.hasStatusEffect(StatusEffects.NIGHT_VISION)) {
                  return false;
               }
            }

            return true;
         }
      } else {
         return false;
      }
   }

   @Generated
   public ModeSetting getMode() {
      return this.mode;
   }

   @Generated
   public ModeSetting.Value getCustom() {
      return this.custom;
   }

   @Generated
   public ModeSetting.Value getNoFog() {
      return this.noFog;
   }

   @Generated
   public RangeSetting getDistance() {
      return this.distance;
   }

   @Generated
   public ColorSetting getFogColor() {
      return this.fogColor;
   }

   @Generated
   public BooleanSetting getRemoveLiquidFog() {
      return this.removeLiquidFog;
   }
}
