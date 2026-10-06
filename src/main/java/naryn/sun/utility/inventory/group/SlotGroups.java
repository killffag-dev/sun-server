package naryn.sun.utility.inventory.group;

import naryn.sun.utility.inventory.group.impl.ArmorSlotsGroup;
import naryn.sun.utility.inventory.group.impl.HotbarSlotsGroup;
import naryn.sun.utility.inventory.group.impl.InventorySlotsGroup;
import naryn.sun.utility.inventory.group.impl.OffhandSlotGroup;
import naryn.sun.utility.inventory.slots.ArmorSlot;
import naryn.sun.utility.inventory.slots.HotbarSlot;
import naryn.sun.utility.inventory.slots.InventorySlot;
import naryn.sun.utility.inventory.slots.OffhandSlot;

public class SlotGroups {
   private SlotGroups() {
   }

   public static SlotGroup<HotbarSlot> hotbar() {
      return new HotbarSlotsGroup();
   }

   public static SlotGroup<InventorySlot> inventory() {
      return new InventorySlotsGroup();
   }

   public static SlotGroup<ArmorSlot> armor() {
      return new ArmorSlotsGroup();
   }

   public static SlotGroup<OffhandSlot> offhand() {
      return new OffhandSlotGroup();
   }
}
