package naryn.sun.systems.modules.modules.utility;

import java.util.Map;
import java.util.TreeMap;
import lombok.Generated;
import naryn.sun.systems.event.EventListener;
import naryn.sun.systems.event.impl.player.ClientPlayerTickEvent;
import naryn.sun.systems.modules.api.ModuleCategory;
import naryn.sun.systems.modules.api.ModuleInfo;
import naryn.sun.systems.modules.impl.BaseModule;
import naryn.sun.systems.setting.settings.BooleanSetting;
import naryn.sun.utility.game.PotionUtility;
import naryn.sun.utility.inventory.InventoryUtility;
import naryn.sun.utility.inventory.ItemSlot;
import naryn.sun.utility.inventory.group.SlotGroup;
import naryn.sun.utility.inventory.group.SlotGroups;
import naryn.sun.utility.inventory.slots.OffhandSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

@ModuleInfo(name = "Auto Invisible", category = ModuleCategory.UTILITY, desc = "Автоматически пьет зелье невидимости")
public class AutoInvisible extends BaseModule {
   private final Map<String, StatusEffectInstance> effects = new TreeMap<>();
   private boolean isUsingPotion;
   private final BooleanSetting preDrink = new BooleanSetting(this, "modules.settings.auto_invisible.pre_drink");
   private final EventListener<ClientPlayerTickEvent> onClientPlayerTickEvent = event -> {
      boolean hasInvisibility = mc.player.hasStatusEffect(StatusEffects.INVISIBILITY);
      StatusEffectInstance effect = hasInvisibility ? mc.player.getStatusEffect(StatusEffects.INVISIBILITY) : null;
      boolean shouldDrink = !hasInvisibility;
      if (this.preDrink.isEnabled() && effect != null && effect.getDuration() <= 200) {
         shouldDrink = true;
      }

      if (shouldDrink) {
         ItemStack offhandItem = mc.player.getOffHandStack();
         boolean hasPotionInOffhand = this.isInvisibilityPotion(offhandItem);
         SlotGroup<ItemSlot> slotsToSearch = SlotGroups.inventory().and(SlotGroups.hotbar());
         ItemSlot potionSlot = slotsToSearch.findItem(this::isInvisibilityPotion);
         OffhandSlot offhandSlot = new OffhandSlot();
         if (potionSlot != null && !hasPotionInOffhand) {
            InventoryUtility.moveItem(potionSlot, offhandSlot);
         }

         if (hasPotionInOffhand) {
            this.isUsingPotion = true;
            mc.options.useKey.setPressed(true);
         }
      } else if (this.isUsingPotion) {
         mc.options.useKey.setPressed(false);
         this.isUsingPotion = false;
         ItemStack offhandItemx = mc.player.getOffHandStack();
         if (offhandItemx.getItem() == Items.GLASS_BOTTLE) {
            mc.interactionManager.clickSlot(0, 45, 1, SlotActionType.THROW, mc.player);
         }
      }
   };

   private boolean isInvisibilityPotion(ItemStack stack) {
      return PotionUtility.hasEffect(stack, StatusEffects.INVISIBILITY);
   }

   @Generated
   public Map<String, StatusEffectInstance> getEffects() {
      return this.effects;
   }
}
