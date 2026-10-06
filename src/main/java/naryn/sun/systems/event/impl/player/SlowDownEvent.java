package naryn.sun.systems.event.impl.player;

import naryn.sun.systems.event.EventCancellable;

public class SlowDownEvent extends EventCancellable {
   public static final SlowDownEvent INSTANCE = new SlowDownEvent();

   public SlowDownEvent set() {
      this.setCancelled(false);
      return this;
   }
}
