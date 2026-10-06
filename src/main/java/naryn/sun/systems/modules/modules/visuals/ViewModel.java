package naryn.sun.systems.modules.modules.visuals;

import lombok.Getter;
import naryn.sun.systems.event.EventListener;
import naryn.sun.systems.event.impl.render.HandRenderEvent;
import naryn.sun.systems.modules.api.ModuleCategory;
import naryn.sun.systems.modules.api.ModuleInfo;
import naryn.sun.systems.modules.constructions.viewmodel.ViewModelScreen;
import naryn.sun.systems.modules.impl.BaseModule;
import naryn.sun.systems.setting.settings.ButtonSetting;
import naryn.sun.systems.setting.settings.SliderSetting;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.math.RotationAxis;

@Getter
@ModuleInfo(name = "View Model", category = ModuleCategory.VISUALS, desc = "modules.descriptions.view_model")
public class ViewModel extends BaseModule {
   private final ButtonSetting openMenu = new ButtonSetting(this, "view_model.open_menu", () -> false)
      .action(() -> mc.setScreen(new ViewModelScreen()));

   private final SliderSetting mainTranslateX = new SliderSetting(this, "modules.settings.view_model.main_translate_x", () -> true)
      .min(-2.0F)
      .max(2.0F)
      .currentValue(0.0F)
      .step(0.02F);
   private final SliderSetting mainTranslateY = new SliderSetting(this, "modules.settings.view_model.main_translate_y", () -> true)
      .min(-2.0F)
      .max(2.0F)
      .currentValue(0.0F)
      .step(0.02F);
   private final SliderSetting mainTranslateZ = new SliderSetting(this, "modules.settings.view_model.main_translate_z", () -> true)
      .min(-2.0F)
      .max(2.0F)
      .currentValue(0.0F)
      .step(0.02F);
   private final SliderSetting mainRotateX = new SliderSetting(this, "modules.settings.view_model.main_rotate_x", () -> true)
      .min(-180.0F)
      .max(180.0F)
      .currentValue(0.0F)
      .step(1.0F);
   private final SliderSetting mainRotateY = new SliderSetting(this, "modules.settings.view_model.main_rotate_y", () -> true)
      .min(-180.0F)
      .max(180.0F)
      .currentValue(0.0F)
      .step(1.0F);
   private final SliderSetting mainRotateZ = new SliderSetting(this, "modules.settings.view_model.main_rotate_z", () -> true)
      .min(-180.0F)
      .max(180.0F)
      .currentValue(0.0F)
      .step(1.0F);
   private final SliderSetting mainScale = new SliderSetting(this, "modules.settings.view_model.main_scale", () -> true)
      .min(0.2F)
      .max(3.0F)
      .currentValue(1.0F)
      .step(0.02F);

   private final SliderSetting offTranslateX = new SliderSetting(this, "modules.settings.view_model.off_translate_x", () -> true)
      .min(-2.0F)
      .max(2.0F)
      .currentValue(0.0F)
      .step(0.02F);
   private final SliderSetting offTranslateY = new SliderSetting(this, "modules.settings.view_model.off_translate_y", () -> true)
      .min(-2.0F)
      .max(2.0F)
      .currentValue(0.0F)
      .step(0.02F);
   private final SliderSetting offTranslateZ = new SliderSetting(this, "modules.settings.view_model.off_translate_z", () -> true)
      .min(-2.0F)
      .max(2.0F)
      .currentValue(0.0F)
      .step(0.02F);
   private final SliderSetting offRotateX = new SliderSetting(this, "modules.settings.view_model.off_rotate_x", () -> true)
      .min(-180.0F)
      .max(180.0F)
      .currentValue(0.0F)
      .step(1.0F);
   private final SliderSetting offRotateY = new SliderSetting(this, "modules.settings.view_model.off_rotate_y", () -> true)
      .min(-180.0F)
      .max(180.0F)
      .currentValue(0.0F)
      .step(1.0F);
   private final SliderSetting offRotateZ = new SliderSetting(this, "modules.settings.view_model.off_rotate_z", () -> true)
      .min(-180.0F)
      .max(180.0F)
      .currentValue(0.0F)
      .step(1.0F);
   private final SliderSetting offScale = new SliderSetting(this, "modules.settings.view_model.off_scale", () -> true)
      .min(0.2F)
      .max(3.0F)
      .currentValue(1.0F)
      .step(0.02F);

   private final EventListener<HandRenderEvent> onHandRender = event -> {
      MatrixStack matrices = event.getMatrices();
      boolean isMain = event.getArm() == Arm.RIGHT;
      float translateX = isMain ? this.mainTranslateX.getCurrentValue() : this.offTranslateX.getCurrentValue();
      float translateY = isMain ? this.mainTranslateY.getCurrentValue() : this.offTranslateY.getCurrentValue();
      float translateZ = isMain ? this.mainTranslateZ.getCurrentValue() : this.offTranslateZ.getCurrentValue();
      float scale = isMain ? this.mainScale.getCurrentValue() : this.offScale.getCurrentValue();
      float rotateX = isMain ? this.mainRotateX.getCurrentValue() : this.offRotateX.getCurrentValue();
      float rotateY = isMain ? this.mainRotateY.getCurrentValue() : this.offRotateY.getCurrentValue();
      float rotateZ = isMain ? this.mainRotateZ.getCurrentValue() : this.offRotateZ.getCurrentValue();

      float direction = isMain ? 1.0F : -1.0F;
      float anchorX = isMain ? 0.56F : -0.56F;
      float anchorY = -0.52F;
      float anchorZ = -0.72F;

      // 1. Move the hand cleanly along screen/camera axes
      matrices.translate(translateX * direction, translateY, translateZ);

      // 2. Rotate and scale locally around the hand's own center
      matrices.translate(anchorX, anchorY, anchorZ);
      matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(rotateX));
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotateY));
      matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotateZ));
      matrices.scale(scale, scale, scale);
      matrices.translate(-anchorX, -anchorY, -anchorZ);
   };

   public void resetMainHand() {
      this.mainTranslateX.setCurrentValue(0.0F);
      this.mainTranslateY.setCurrentValue(0.0F);
      this.mainTranslateZ.setCurrentValue(0.0F);
      this.mainRotateX.setCurrentValue(0.0F);
      this.mainRotateY.setCurrentValue(0.0F);
      this.mainRotateZ.setCurrentValue(0.0F);
      this.mainScale.setCurrentValue(1.0F);
   }

   public void resetOffHand() {
      this.offTranslateX.setCurrentValue(0.0F);
      this.offTranslateY.setCurrentValue(0.0F);
      this.offTranslateZ.setCurrentValue(0.0F);
      this.offRotateX.setCurrentValue(0.0F);
      this.offRotateY.setCurrentValue(0.0F);
      this.offRotateZ.setCurrentValue(0.0F);
      this.offScale.setCurrentValue(1.0F);
   }

   public void resetDefaults() {
      this.resetMainHand();
      this.resetOffHand();
   }
}
