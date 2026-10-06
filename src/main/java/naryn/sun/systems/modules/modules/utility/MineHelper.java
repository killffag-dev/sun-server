package naryn.sun.systems.modules.modules.utility;

import naryn.sun.Sun;
import naryn.sun.systems.event.EventListener;
import naryn.sun.systems.event.impl.game.StartBreakBlockEvent;
import naryn.sun.systems.localization.Localizator;
import naryn.sun.systems.modules.api.ModuleCategory;
import naryn.sun.systems.modules.api.ModuleInfo;
import naryn.sun.systems.modules.impl.BaseModule;
import naryn.sun.systems.notifications.NotificationType;
import naryn.sun.systems.setting.settings.BooleanSetting;
import naryn.sun.systems.setting.settings.SliderSetting;
import naryn.sun.utility.inventory.InventoryUtility;
import naryn.sun.utility.inventory.slots.HotbarSlot;
import naryn.sun.utility.time.Timer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PickaxeItem;

@ModuleInfo(name = "Mine Helper", category = ModuleCategory.UTILITY, desc = "modules.descriptions.mine_helper")
public class MineHelper extends BaseModule {
   private final SliderSetting percent = new SliderSetting(this, this.getSettingName("percent"))
      .step(1.0F)
      .min(1.0F)
      .max(70.0F)
      .currentValue(10.0F)
      .suffix("%");
   private final BooleanSetting autoReplace = new BooleanSetting(this, this.getSettingName("auto_replace"));

   private final Timer timer = new Timer();

   private final EventListener<StartBreakBlockEvent> onStartBreakBlockEvent = event -> {
      if (mc.player == null) {
         return;
      }

      ItemStack currentStack = mc.player.getMainHandStack();
      if (this.isValidPickaxe(currentStack)) {
         double durabilityPercent = this.getDurabilityPercent(currentStack);
         if (durabilityPercent < this.percent.getCurrentValue()) {
            event.cancel();
            this.handleLowDurability(currentStack);
         }
      }
   };

   private void handleLowDurability(ItemStack currentStack) {
      boolean switched = false;
      if (this.autoReplace.isEnabled()) {
         switched = this.trySwitchPickaxe(currentStack);
      }

      if (!switched && this.timer.finished(800L)) {
         Sun.getInstance()
            .getNotificationManager()
            .addNotificationOther(
               NotificationType.ERROR,
               Localizator.translate("modules.names.mine_helper"),
               Localizator.translate("modules.settings.mine_helper.no_pickaxe")
            );
         this.timer.reset();
      }
   }

   private boolean trySwitchPickaxe(ItemStack currentStack) {
      HotbarSlot bestSlot = this.findBestPickaxeSlot(currentStack);
      if (bestSlot == null) {
         return false;
      }

      InventoryUtility.selectHotbarSlot(bestSlot);
      if (this.timer.finished(800L)) {
         ItemStack newStack = bestSlot.itemStack();
         Sun.getInstance()
            .getNotificationManager()
            .addNotificationOther(
               NotificationType.SUCCESS,
               Localizator.translate("modules.names.mine_helper"),
               Localizator.translate("modules.settings.mine_helper.switched", this.getDurabilityPercent(currentStack), this.getDurabilityPercent(newStack))
            );
         this.timer.reset();
      }

      return true;
   }

   private HotbarSlot findBestPickaxeSlot(ItemStack currentStack) {
      double currentDurability = this.getDurabilityPercent(currentStack);
      HotbarSlot bestSlot = null;
      double bestDurability = currentDurability;

      for (int i = 0; i < 9; i++) {
         HotbarSlot slot = InventoryUtility.getHotbarSlot(i);
         ItemStack stack = slot.itemStack();
         if (this.isValidPickaxe(stack)) {
            double durability = this.getDurabilityPercent(stack);
            if (durability > bestDurability) {
               bestDurability = durability;
               bestSlot = slot;
            }
         }
      }

      return bestSlot;
   }

   private boolean isValidPickaxe(ItemStack stack) {
      return stack != null && stack.isDamageable() && stack.getItem() instanceof PickaxeItem;
   }

   private double getDurabilityPercent(ItemStack stack) {
      return (double)(stack.getMaxDamage() - stack.getDamage()) / stack.getMaxDamage() * 100.0;
   }
}
