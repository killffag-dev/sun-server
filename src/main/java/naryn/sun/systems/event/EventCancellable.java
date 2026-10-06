package naryn.sun.systems.event;

import lombok.Generated;

public class EventCancellable extends Event {
   private boolean cancelled;

   public void cancel() {
      this.cancelled = true;
   }

   public void setCancelled(boolean cancelled) {
      this.cancelled = cancelled;
   }

   @Generated
   public boolean isCancelled() {
      return this.cancelled;
   }
}
