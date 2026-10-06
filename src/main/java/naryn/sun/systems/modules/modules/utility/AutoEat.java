package naryn.sun.systems.modules.modules.utility;

import naryn.sun.systems.event.EventListener;
import naryn.sun.systems.event.impl.game.BlockPlaceEvent;
import naryn.sun.systems.event.impl.game.InternalAttackEvent;
import naryn.sun.systems.event.impl.player.ClientPlayerTickEvent;
import naryn.sun.systems.modules.api.ModuleCategory;
import naryn.sun.systems.modules.api.ModuleInfo;
import naryn.sun.systems.modules.impl.BaseModule;
import naryn.sun.systems.setting.settings.BooleanSetting;
import naryn.sun.systems.setting.settings.GroupSetting;
import naryn.sun.systems.setting.settings.SelectSetting;
import naryn.sun.systems.setting.settings.SliderSetting;
import naryn.sun.utility.inventory.InventoryUtility;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;

@ModuleInfo(name = "Auto Eat", category = ModuleCategory.UTILITY, desc = "modules.descriptions.auto_eat")
public class AutoEat extends BaseModule {
   private final SliderSetting food = new SliderSetting(this, "modules.settings.auto_eat.food", "modules.settings.auto_eat.food.desc")
      .min(1.0F)
      .max(19.0F)
      .step(1.0F)
      .currentValue(15.0F);

   // --- Пауза при атаке ---
   private final BooleanSetting pauseOnAttack = new BooleanSetting(this, "modules.settings.auto_eat.pause_on_attack", "modules.settings.auto_eat.pause_on_attack.desc")
      .enable();
   private final GroupSetting attackGroup = new GroupSetting(this, "modules.settings.auto_eat.group.attack", () -> !this.pauseOnAttack.isEnabled());
   private final SliderSetting attackDelay = new SliderSetting(this.attackGroup, "modules.settings.auto_eat.attack_delay", "modules.settings.auto_eat.attack_delay.desc")
      .min(0.5F)
      .max(10.0F)
      .step(0.5F)
      .currentValue(3.0F);

   // --- Пауза при установке блоков ---
   private final BooleanSetting pauseOnBlock = new BooleanSetting(this, "modules.settings.auto_eat.pause_on_block", "modules.settings.auto_eat.pause_on_block.desc")
      .enable();
   private final GroupSetting blockGroup = new GroupSetting(this, "modules.settings.auto_eat.group.block", () -> !this.pauseOnBlock.isEnabled());
   private final SliderSetting blockDelay = new SliderSetting(this.blockGroup, "modules.settings.auto_eat.block_delay", "modules.settings.auto_eat.block_delay.desc")
      .min(0.5F)
      .max(10.0F)
      .step(0.5F)
      .currentValue(3.0F);

   // --- Вредная еда ---
   private final BooleanSetting allowBadFood = new BooleanSetting(this, "modules.settings.auto_eat.allow_bad_food", "modules.settings.auto_eat.allow_bad_food.desc");
   private final SelectSetting badFoodList = new SelectSetting(this, "modules.settings.auto_eat.bad_food_list", () -> !this.allowBadFood.isEnabled());
   private final SelectSetting.Value rottenFlesh = new SelectSetting.Value(this.badFoodList, "modules.settings.auto_eat.rotten_flesh");
   private final SelectSetting.Value spiderEye = new SelectSetting.Value(this.badFoodList, "modules.settings.auto_eat.spider_eye");
   private final SelectSetting.Value pufferfish = new SelectSetting.Value(this.badFoodList, "modules.settings.auto_eat.pufferfish");
   private final SelectSetting.Value poisonousPotato = new SelectSetting.Value(this.badFoodList, "modules.settings.auto_eat.poisonous_potato");
   private final SelectSetting.Value chorusFruit = new SelectSetting.Value(this.badFoodList, "modules.settings.auto_eat.chorus_fruit");
   private final SelectSetting.Value rawChicken = new SelectSetting.Value(this.badFoodList, "modules.settings.auto_eat.raw_chicken");
   private final SelectSetting.Value suspiciousStew = new SelectSetting.Value(this.badFoodList, "modules.settings.auto_eat.suspicious_stew");

   // --- Пополнение хотбара ---
   private final BooleanSetting refillHotbar = new BooleanSetting(this, "modules.settings.auto_eat.refill_hotbar", "modules.settings.auto_eat.refill_hotbar.desc");
   private final GroupSetting refillGroup = new GroupSetting(this, "modules.settings.auto_eat.group.refill", () -> !this.refillHotbar.isEnabled());
   private final BooleanSetting restoreSlot = new BooleanSetting(this.refillGroup, "modules.settings.auto_eat.restore_slot", "modules.settings.auto_eat.restore_slot.desc")
      .enable();
   private final SliderSetting hotbarSlot = new SliderSetting(this.refillGroup, "modules.settings.auto_eat.hotbar_slot", "modules.settings.auto_eat.hotbar_slot.desc")
      .min(1.0F)
      .max(9.0F)
      .step(1.0F)
      .currentValue(3.0F);

   private final AutoEatFoodFilter foodFilter = new AutoEatFoodFilter(
      this.allowBadFood,
      this.rottenFlesh,
      this.spiderEye,
      this.pufferfish,
      this.poisonousPotato,
      this.chorusFruit,
      this.rawChicken,
      this.suspiciousStew
   );
   private final AutoEatInventoryHelper inventoryHelper = new AutoEatInventoryHelper();

   private boolean eating;
   private boolean wasUsingItem;
   private Hand eatingHand = Hand.MAIN_HAND;
   private int originalSelectedSlot = -1;
   private long lastAttackTime = 0L;
   private long lastBlockPlaceTime = 0L;

   private final EventListener<InternalAttackEvent> onAttack = event -> {
      this.lastAttackTime = System.currentTimeMillis();
   };

   private final EventListener<BlockPlaceEvent> onBlockPlace = event -> {
      this.lastBlockPlaceTime = System.currentTimeMillis();
   };

   private final EventListener<ClientPlayerTickEvent> onUpdateEvent = event -> {
      if (mc.player == null || mc.world == null) {
         if (this.eating) this.stopEating();
         return;
      }

      if (mc.player.isCreative() || mc.player.isSpectator() || !mc.player.isAlive()) {
         if (this.eating) this.stopEating();
         return;
      }

      if (this.pauseOnAttack.isEnabled() && System.currentTimeMillis() - this.lastAttackTime < (long) (this.attackDelay.getCurrentValue() * 1000.0F)) {
         if (this.eating) this.stopEating();
         return;
      }

      if (this.pauseOnBlock.isEnabled() && System.currentTimeMillis() - this.lastBlockPlaceTime < (long) (this.blockDelay.getCurrentValue() * 1000.0F)) {
         if (this.eating) this.stopEating();
         return;
      }

      if (this.inventoryHelper.isRestoring()) {
         this.inventoryHelper.tickRefill();
         return;
      }

      if (this.inventoryHelper.isRefilling()) {
         this.inventoryHelper.tickRefill();
         if (this.inventoryHelper.isReadyToEat()) {
            this.startEating(Hand.MAIN_HAND, this.inventoryHelper.getSavedHotbarSlot());
            this.inventoryHelper.clearReadyToEat();
         }
         return;
      }

      if (mc.currentScreen != null) {
         if (this.eating) this.stopEating();
         return;
      }

      if (this.eating) {
         if (this.wasUsingItem && !mc.player.isUsingItem()) {
            if (mc.player.getHungerManager().getFoodLevel() > this.food.getCurrentValue() || mc.player.getHungerManager().getFoodLevel() >= 20) {
               this.stopEating();
               return;
            }
            if (!this.hasFoodInCurrentHand()) {
               this.stopEating();
               return;
            }
         }

         if (!this.hasFoodInCurrentHand()) {
            this.stopEating();
            return;
         }

         mc.options.useKey.setPressed(true);
         this.wasUsingItem = mc.player.isUsingItem();
         return;
      }

      if (mc.player.getHungerManager().getFoodLevel() <= this.food.getCurrentValue()) {
         if (this.foodFilter.isAllowedFood(mc.player.getOffHandStack())) {
            this.startEating(Hand.OFF_HAND, -1);
            return;
         }

         int bestHotbarSlot = this.findBestFoodInHotbar();
         if (bestHotbarSlot != -1) {
            this.startEating(Hand.MAIN_HAND, bestHotbarSlot);
            return;
         }

         if (this.refillHotbar.isEnabled()) {
            int invSlot = this.inventoryHelper.findFoodInInventory(this.foodFilter);
            if (invSlot != -1) {
               int targetSlot = (int) this.hotbarSlot.getCurrentValue() - 1;
               this.inventoryHelper.startRefill(invSlot, targetSlot);
            }
         }
      }
   };

   private void startEating(Hand hand, int slot) {
      if (hand == Hand.MAIN_HAND && slot != -1) {
         if (mc.player != null && mc.player.getInventory().selectedSlot != slot) {
            this.originalSelectedSlot = mc.player.getInventory().selectedSlot;
            InventoryUtility.selectHotbarSlot(slot);
         }
      }
      this.eatingHand = hand;
      this.eating = true;
      this.wasUsingItem = false;
      if (mc.interactionManager != null && mc.player != null) {
         mc.interactionManager.interactItem(mc.player, hand);
      }
      mc.options.useKey.setPressed(true);
   }

   private void stopEating() {
      this.eating = false;
      this.wasUsingItem = false;
      mc.options.useKey.setPressed(false);
      if (this.originalSelectedSlot != -1) {
         InventoryUtility.selectHotbarSlot(this.originalSelectedSlot);
         this.originalSelectedSlot = -1;
      }
      if (this.inventoryHelper.wasRefilled()) {
         this.inventoryHelper.startRestore(this.restoreSlot.isEnabled());
      } else {
         this.inventoryHelper.reset();
      }
   }

   private int findBestFoodInHotbar() {
      if (mc.player == null) return -1;
      int bestSlot = -1;
      int bestNutrition = -1;
      for (int i = 0; i < 9; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (this.foodFilter.isAllowedFood(stack)) {
            var foodComp = stack.get(DataComponentTypes.FOOD);
            int nutrition = foodComp != null ? foodComp.nutrition() : 1;
            if (nutrition > bestNutrition) {
               bestNutrition = nutrition;
               bestSlot = i;
            }
         }
      }
      return bestSlot;
   }

   private boolean hasFoodInCurrentHand() {
      if (mc.player == null) return false;
      ItemStack stack = this.eatingHand == Hand.OFF_HAND ? mc.player.getOffHandStack() : mc.player.getMainHandStack();
      return this.foodFilter.isAllowedFood(stack);
   }

   @Override
   public void onDisable() {
      if (this.eating) {
         this.stopEating();
      }
      this.inventoryHelper.reset();
      super.onDisable();
   }
}
