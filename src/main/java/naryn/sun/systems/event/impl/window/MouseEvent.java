package naryn.sun.systems.event.impl.window;

import lombok.Generated;
import naryn.sun.systems.event.EventCancellable;

public class MouseEvent extends EventCancellable {
   private final int button;
   private final int action;

   @Generated
   public int getButton() {
      return this.button;
   }

   @Generated
   public int getAction() {
      return this.action;
   }

   @Generated
   public MouseEvent(int button, int action) {
      this.button = button;
      this.action = action;
   }
}
