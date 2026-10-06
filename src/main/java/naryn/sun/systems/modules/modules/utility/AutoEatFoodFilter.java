package naryn.sun.systems.modules.modules.utility;

import naryn.sun.systems.setting.settings.BooleanSetting;
import naryn.sun.systems.setting.settings.SelectSetting;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class AutoEatFoodFilter {
   private final BooleanSetting allowBadFood;
   private final SelectSetting.Value rottenFlesh;
   private final SelectSetting.Value spiderEye;
   private final SelectSetting.Value pufferfish;
   private final SelectSetting.Value poisonousPotato;
   private final SelectSetting.Value chorusFruit;
   private final SelectSetting.Value rawChicken;
   private final SelectSetting.Value suspiciousStew;

   public AutoEatFoodFilter(
      BooleanSetting allowBadFood,
      SelectSetting.Value rottenFlesh,
      SelectSetting.Value spiderEye,
      SelectSetting.Value pufferfish,
      SelectSetting.Value poisonousPotato,
      SelectSetting.Value chorusFruit,
      SelectSetting.Value rawChicken,
      SelectSetting.Value suspiciousStew
   ) {
      this.allowBadFood = allowBadFood;
      this.rottenFlesh = rottenFlesh;
      this.spiderEye = spiderEye;
      this.pufferfish = pufferfish;
      this.poisonousPotato = poisonousPotato;
      this.chorusFruit = chorusFruit;
      this.rawChicken = rawChicken;
      this.suspiciousStew = suspiciousStew;
   }

   public boolean isAllowedFood(ItemStack stack) {
      if (stack == null || stack.isEmpty()) {
         return false;
      }
      if (!stack.contains(DataComponentTypes.FOOD)) {
         return false;
      }

      Item item = stack.getItem();
      // Золотые и зачарованные золотые яблоки строго запрещены
      if (item == Items.GOLDEN_APPLE || item == Items.ENCHANTED_GOLDEN_APPLE) {
         return false;
      }

      if (this.isBadFood(item)) {
         if (!this.allowBadFood.isEnabled()) {
            return false;
         }
         return this.isBadFoodSelected(item);
      }

      return true;
   }

   public boolean isBadFood(Item item) {
      return item == Items.ROTTEN_FLESH
         || item == Items.SPIDER_EYE
         || item == Items.PUFFERFISH
         || item == Items.POISONOUS_POTATO
         || item == Items.CHORUS_FRUIT
         || item == Items.CHICKEN
         || item == Items.SUSPICIOUS_STEW;
   }

   private boolean isBadFoodSelected(Item item) {
      if (item == Items.ROTTEN_FLESH) return this.rottenFlesh.isSelected();
      if (item == Items.SPIDER_EYE) return this.spiderEye.isSelected();
      if (item == Items.PUFFERFISH) return this.pufferfish.isSelected();
      if (item == Items.POISONOUS_POTATO) return this.poisonousPotato.isSelected();
      if (item == Items.CHORUS_FRUIT) return this.chorusFruit.isSelected();
      if (item == Items.CHICKEN) return this.rawChicken.isSelected();
      if (item == Items.SUSPICIOUS_STEW) return this.suspiciousStew.isSelected();
      return false;
   }
}
