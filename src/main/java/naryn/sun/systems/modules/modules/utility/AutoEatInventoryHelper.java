package naryn.sun.systems.modules.modules.utility;

import naryn.sun.utility.interfaces.IMinecraft;
import naryn.sun.utility.inventory.InventoryUtility;
import naryn.sun.utility.time.Timer;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.ItemStack;

public class AutoEatInventoryHelper implements IMinecraft {
   public enum State {
      IDLE,
      OPEN_FOR_REFILL,
      WAIT_SWAP_REFILL,
      WAIT_CLOSE_REFILL,
      READY_TO_EAT,
      OPEN_FOR_RESTORE,
      WAIT_SWAP_RESTORE,
      WAIT_CLOSE_RESTORE
   }

   private State state = State.IDLE;
   private int savedInvSlot = -1;
   private int savedHotbarSlot = -1;
   private boolean hadItemInHotbar = false;
   private boolean wasRefilled = false;
   private final Timer actionTimer = new Timer();

   public int findFoodInInventory(AutoEatFoodFilter filter) {
      if (mc.player == null || mc.player.playerScreenHandler == null) {
         return -1;
      }
      for (int i = 9; i <= 35; i++) {
         ItemStack stack = mc.player.playerScreenHandler.getSlot(i).getStack();
         if (filter.isAllowedFood(stack)) {
            return i;
         }
      }
      return -1;
   }

   public void startRefill(int invSlot, int hotbarSlot) {
      this.savedInvSlot = invSlot;
      this.savedHotbarSlot = hotbarSlot;
      this.hadItemInHotbar = mc.player != null && !mc.player.getInventory().getStack(hotbarSlot).isEmpty();
      this.wasRefilled = true;
      this.state = State.OPEN_FOR_REFILL;
   }

   public void tickRefill() {
      this.tickRefill(150L);
   }

   public void tickRefill(long delayMs) {
      if (mc.player == null) {
         this.reset();
         return;
      }

      long closeDelay = Math.max(40L, (long) (delayMs * 0.6));

      switch (this.state) {
         case OPEN_FOR_REFILL -> {
            if (mc.currentScreen == null) {
               mc.setScreen(new InventoryScreen(mc.player));
            }
            this.actionTimer.reset();
            this.state = State.WAIT_SWAP_REFILL;
         }
         case WAIT_SWAP_REFILL -> {
            if (this.actionTimer.finished(delayMs)) {
               InventoryUtility.hotbarSwap(this.savedInvSlot, this.savedHotbarSlot);
               this.actionTimer.reset();
               this.state = State.WAIT_CLOSE_REFILL;
            }
         }
         case WAIT_CLOSE_REFILL -> {
            if (this.actionTimer.finished(closeDelay)) {
               if (mc.currentScreen instanceof InventoryScreen) {
                  mc.setScreen(null);
               }
               this.state = State.READY_TO_EAT;
            }
         }
         case OPEN_FOR_RESTORE -> {
            if (mc.currentScreen == null) {
               mc.setScreen(new InventoryScreen(mc.player));
            }
            this.actionTimer.reset();
            this.state = State.WAIT_SWAP_RESTORE;
         }
         case WAIT_SWAP_RESTORE -> {
            if (this.actionTimer.finished(delayMs)) {
               InventoryUtility.hotbarSwap(this.savedInvSlot, this.savedHotbarSlot);
               this.actionTimer.reset();
               this.state = State.WAIT_CLOSE_RESTORE;
            }
         }
         case WAIT_CLOSE_RESTORE -> {
            if (this.actionTimer.finished(closeDelay)) {
               if (mc.currentScreen instanceof InventoryScreen) {
                  mc.setScreen(null);
               }
               this.reset();
            }
         }
         default -> {}
      }
   }

   public void startRestore(boolean shouldRestore) {
      if (this.wasRefilled && shouldRestore && this.hadItemInHotbar && this.savedInvSlot != -1 && this.savedHotbarSlot != -1) {
         this.state = State.OPEN_FOR_RESTORE;
      } else {
         this.reset();
      }
   }

   public boolean isRefilling() {
      return this.state == State.OPEN_FOR_REFILL
         || this.state == State.WAIT_SWAP_REFILL
         || this.state == State.WAIT_CLOSE_REFILL;
   }

   public boolean isReadyToEat() {
      return this.state == State.READY_TO_EAT;
   }

   public boolean isRestoring() {
      return this.state == State.OPEN_FOR_RESTORE
         || this.state == State.WAIT_SWAP_RESTORE
         || this.state == State.WAIT_CLOSE_RESTORE;
   }

   public void clearReadyToEat() {
      if (this.state == State.READY_TO_EAT) {
         this.state = State.IDLE;
      }
   }

   public int getSavedHotbarSlot() {
      return this.savedHotbarSlot;
   }

   public boolean wasRefilled() {
      return this.wasRefilled;
   }

   public void reset() {
      if (mc.currentScreen instanceof InventoryScreen) {
         mc.setScreen(null);
      }
      this.state = State.IDLE;
      this.savedInvSlot = -1;
      this.savedHotbarSlot = -1;
      this.hadItemInHotbar = false;
      this.wasRefilled = false;
   }
}
