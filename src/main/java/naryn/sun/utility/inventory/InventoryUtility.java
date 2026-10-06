package naryn.sun.utility.inventory;

import java.util.function.Predicate;
import lombok.Generated;
import naryn.sun.access.MCClientAccess;
import naryn.sun.access.MCPlayerAccess;
import naryn.sun.utility.inventory.group.impl.HotbarSlotsGroup;
import naryn.sun.utility.inventory.slots.ArmorSlot;
import naryn.sun.utility.inventory.slots.HotbarSlot;
import naryn.sun.utility.inventory.slots.InventorySlot;
import naryn.sun.utility.inventory.slots.OffhandSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import org.jetbrains.annotations.NotNull;

public final class InventoryUtility {
   public static HotbarSlot getHotbarSlot(int slotId) {
      return new HotbarSlot(slotId);
   }

   public static InventorySlot getInventorySlot(int slotId) {
      return new InventorySlot(slotId);
   }

   public static ArmorSlot getArmorSlot(int armorIndex) {
      return new ArmorSlot(armorIndex);
   }

   public static ArmorSlot getHelmetSlot() {
      return getArmorSlot(3);
   }

   public static ArmorSlot getChestplateSlot() {
      return getArmorSlot(2);
   }

   public static ArmorSlot getLeggingsSlot() {
      return getArmorSlot(1);
   }

   public static ArmorSlot getBootsSlot() {
      return getArmorSlot(0);
   }

   public static OffhandSlot getOffHandSlot() {
      return new OffhandSlot();
   }

   public static boolean hasItemInOffHand(Item item) {
      return getOffHandSlot().contains(item);
   }

   public static boolean offHandItemMatches(Predicate<ItemStack> predicate) {
      return getOffHandSlot().matches(predicate);
   }

   public static boolean isOffHandEmpty() {
      return getOffHandSlot().isEmpty();
   }

   public static void moveItem(ItemSlot from, ItemSlot to) {
      if (MCClientAccess.getNetworkHandler() != null) {
         from.click();
         to.click();
         if (!to.isEmpty()) {
            from.click();
         }

         MCClientAccess.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(0));
      }
   }

   public static void quickMove(int from) {
      if (MCClientAccess.getNetworkHandler() != null) {
         MCClientAccess.getInteractionManager().clickSlot(MCPlayerAccess.get().currentScreenHandler.syncId, from, 0, SlotActionType.QUICK_MOVE, MCPlayerAccess.get());
      }
   }

   public static void moveItem(int from, int to) {
      moveItem(from, to, false);
   }

   public static void moveItem(int from, int to, boolean back) {
      if (MCClientAccess.getNetworkHandler() != null) {
         MCClientAccess.getInteractionManager().clickSlot(MCPlayerAccess.get().currentScreenHandler.syncId, from, 0, SlotActionType.PICKUP, MCPlayerAccess.get());
         MCClientAccess.getInteractionManager().clickSlot(MCPlayerAccess.get().currentScreenHandler.syncId, to, 0, SlotActionType.PICKUP, MCPlayerAccess.get());
         if (back) {
            MCClientAccess.getInteractionManager().clickSlot(MCPlayerAccess.get().currentScreenHandler.syncId, from, 0, SlotActionType.PICKUP, MCPlayerAccess.get());
         }
      }
   }

   public static void moveHalf(int from, int to) {
      if (MCClientAccess.getNetworkHandler() != null) {
         MCClientAccess.getInteractionManager().clickSlot(MCPlayerAccess.get().currentScreenHandler.syncId, from, 1, SlotActionType.PICKUP, MCPlayerAccess.get());
         MCClientAccess.getInteractionManager().clickSlot(MCPlayerAccess.get().currentScreenHandler.syncId, to, 0, SlotActionType.PICKUP, MCPlayerAccess.get());
      }
   }

   public static void swapOneItem(int from, int to) {
      if (MCClientAccess.getNetworkHandler() != null) {
         MCClientAccess.getInteractionManager().clickSlot(MCPlayerAccess.get().currentScreenHandler.syncId, from, 0, SlotActionType.PICKUP, MCPlayerAccess.get());
         MCClientAccess.getInteractionManager().clickSlot(MCPlayerAccess.get().currentScreenHandler.syncId, to, 1, SlotActionType.PICKUP, MCPlayerAccess.get());
         MCClientAccess.getInteractionManager().clickSlot(MCPlayerAccess.get().currentScreenHandler.syncId, from, 0, SlotActionType.PICKUP, MCPlayerAccess.get());
      }
   }

   public static void hotbarSwap(int from, int to) {
      if (MCClientAccess.getNetworkHandler() != null) {
         MCClientAccess.getInteractionManager().clickSlot(MCPlayerAccess.get().currentScreenHandler.syncId, from, to, SlotActionType.SWAP, MCPlayerAccess.get());
      }
   }

   public static boolean moveToHotbar(ItemSlot fromSlot, int hotbarSlotId) {
      HotbarSlot hotbarSlot = getHotbarSlot(hotbarSlotId);
      moveItem(fromSlot, hotbarSlot);
      return true;
   }

   public static boolean moveToArmor(ItemSlot fromSlot, int armorIndex) {
      ArmorSlot armorSlot = getArmorSlot(armorIndex);
      moveItem(fromSlot, armorSlot);
      return true;
   }

   public static void moveToOffHand(ItemSlot fromSlot) {
      OffhandSlot offHandSlot = getOffHandSlot();
      moveItem(fromSlot, offHandSlot);
   }

   @NotNull
   public static HotbarSlot getCurrentHotbarSlot() {
      return MCPlayerAccess.isPresent() && MCPlayerAccess.get().getInventory() != null ? getHotbarSlot(MCPlayerAccess.get().getInventory().selectedSlot) : new HotbarSlot(0);
   }

   public static void selectHotbarSlot(int slotId) {
      if (MCPlayerAccess.isPresent() && MCPlayerAccess.get().getInventory() != null && MCClientAccess.getNetworkHandler() != null) {
         if (slotId >= 0 && slotId <= 8) {
            MCPlayerAccess.get().getInventory().selectedSlot = slotId;
            MCClientAccess.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(MCPlayerAccess.get().getInventory().selectedSlot));
         } else {
            throw new IllegalArgumentException("Hotbar slot ID must be between 0 and 8");
         }
      }
   }

   public static void selectHotbarSlot(HotbarSlot slot) {
      selectHotbarSlot(slot.getSlotId());
   }

   public static boolean selectItemInHotbar(Item item) {
      HotbarSlot slot = new HotbarSlotsGroup().findItem(item);
      if (slot != null) {
         selectHotbarSlot(slot);
         return true;
      } else {
         return false;
      }
   }

   public static int findItemInContainer(Predicate<ItemStack> predicate) {
      if (MCPlayerAccess.isPresent() && MCPlayerAccess.get().currentScreenHandler != null) {
         for (int i = 0; i < MCPlayerAccess.get().currentScreenHandler.slots.size(); i++) {
            ItemStack stack = MCPlayerAccess.get().currentScreenHandler.getSlot(i).getStack();
            if (predicate.test(stack)) {
               return i;
            }
         }

         return -1;
      } else {
         return -1;
      }
   }

   public static int findItemInContainer(Item item) {
      return findItemInContainer((Predicate<ItemStack>)(stack -> stack.getItem() == item));
   }

   @Generated
   private InventoryUtility() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
