package naryn.sun.utility.inventory.group.impl;

import java.util.List;
import naryn.sun.utility.inventory.group.SlotGroup;
import naryn.sun.utility.inventory.slots.OffhandSlot;

public class OffhandSlotGroup extends SlotGroup<OffhandSlot> {
   public OffhandSlotGroup() {
      super(List.of(new OffhandSlot()));
   }
}
