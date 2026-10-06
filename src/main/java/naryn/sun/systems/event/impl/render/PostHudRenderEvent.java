package naryn.sun.systems.event.impl.render;

import lombok.Generated;
import naryn.sun.framework.base.CustomDrawContext;
import naryn.sun.systems.event.Event;

public class PostHudRenderEvent extends Event {
   private final CustomDrawContext context;
   private final float tickDelta;

   @Generated
   public CustomDrawContext getContext() {
      return this.context;
   }

   @Generated
   public float getTickDelta() {
      return this.tickDelta;
   }

   @Generated
   public PostHudRenderEvent(CustomDrawContext context, float tickDelta) {
      this.context = context;
      this.tickDelta = tickDelta;
   }
}
