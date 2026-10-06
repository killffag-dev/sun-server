package naryn.sun.utility.inventory.slots;

import naryn.sun.utility.inventory.ItemSlot;
import net.minecraft.item.ItemStack;

public class OffhandSlot extends ItemSlot {
   @Override
   public ItemStack itemStack() {
      return mc.player != null && mc.player.getInventory() != null ? (ItemStack)mc.player.getInventory().offHand.getFirst() : ItemStack.EMPTY;
   }

   @Override
   public int getIdForServer() {
      return 45;
   }
}
