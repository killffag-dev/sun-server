package naryn.sun.systems.event.impl.window;

import lombok.Generated;
import naryn.sun.systems.event.Event;

public class KeyPressEvent extends Event {
   private final int action;
   private final int key;

   @Generated
   public int getAction() {
      return this.action;
   }

   @Generated
   public int getKey() {
      return this.key;
   }

   @Generated
   public KeyPressEvent(int action, int key) {
      this.action = action;
      this.key = key;
   }
}
