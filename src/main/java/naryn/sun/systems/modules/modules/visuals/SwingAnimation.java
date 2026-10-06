package naryn.sun.systems.modules.modules.visuals;

import naryn.sun.Sun;
import naryn.sun.systems.event.EventListener;
import naryn.sun.systems.event.impl.render.HandRenderEvent;
import naryn.sun.systems.modules.api.ModuleCategory;
import naryn.sun.systems.modules.api.ModuleInfo;
import naryn.sun.systems.modules.constructions.swinganim.SwingTransformations;
import naryn.sun.systems.modules.impl.BaseModule;
import naryn.sun.systems.setting.settings.ModeSetting;
import naryn.sun.systems.setting.settings.SliderSetting;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.consume.UseAction;
import net.minecraft.util.Arm;
import org.joml.Quaternionf;

@ModuleInfo(name = "Swing Animation", category = ModuleCategory.VISUALS, desc = "Изменяет анимации рук при взмахе")
public class SwingAnimation extends BaseModule {
   private final ModeSetting mode = new ModeSetting(this, "modules.settings.swing_animation.mode");
   private final ModeSetting.Value blockHit = new ModeSetting.Value(this.mode, "swings.block_hit").select();
   private final ModeSetting.Value bonk = new ModeSetting.Value(this.mode, "swings.bonk");
   private final ModeSetting.Value rotate360 = new ModeSetting.Value(this.mode, "swings.rotate_360");
   private final ModeSetting.Value fromMe = new ModeSetting.Value(this.mode, "swings.from_me");
   private final SliderSetting speed = new SliderSetting(this, "swing.wing_speed").step(0.5F).min(1.0F).max(5.0F).currentValue(2.0F);

   {
      this.mode.onChange(val -> {
         if (val != null) {
            Sun.getInstance().getSwingManager().applyPreset(val.getName());
            this.speed.setCurrentValue(Sun.getInstance().getSwingManager().getSpeed().getCurrentValue());
         }
      });
      this.speed.onChange(s -> Sun.getInstance().getSwingManager().getSpeed().setCurrentValue(this.speed.getCurrentValue()));
   }

   @Override
   public void onEnable() {
      super.onEnable();
      if (this.mode.getValue() != null) {
         Sun.getInstance().getSwingManager().applyPreset(this.mode.getValue().getName());
      }
   }
   private final EventListener<HandRenderEvent> onHandRender = event -> {
      if (this.shouldApplyAnimation(event.getItemStack()) && event.getArm() == Arm.RIGHT) {
         MatrixStack matrices = event.getMatrices();
         float swingProgress = event.getSwingProgress();
         SwingTransformations trans = Sun.getInstance().getSwingManager().transformations(swingProgress);
         matrices.translate(trans.anchorX(), trans.anchorY(), trans.anchorZ());
         matrices.translate(trans.moveX(), trans.moveY(), trans.moveZ());
         matrices.multiply(
            new Quaternionf()
               .rotationXYZ((float)Math.toRadians(trans.rotateX()), (float)Math.toRadians(trans.rotateY()), (float)Math.toRadians(trans.rotateZ()))
         );
         matrices.translate(-trans.anchorX(), -trans.anchorY(), -trans.anchorZ());
         event.cancel();
      }
   };

   public boolean shouldApplyAnimation(ItemStack itemStack) {
      Item item = itemStack.getItem();
      return item != Items.AIR
         && item != Items.FILLED_MAP
         && item != Items.CROSSBOW
         && item != Items.BOW
         && item != Items.TRIDENT
         && item.getUseAction(itemStack) != UseAction.DRINK
         && item.getUseAction(itemStack) != UseAction.EAT;
   }
}