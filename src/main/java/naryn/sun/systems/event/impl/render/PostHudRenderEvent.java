package naryn.sun.systems.event.impl.render;

import lombok.Generated;
import naryn.sun.framework.base.CustomDrawContext;
import naryn.sun.systems.event.Event;

public class PostHudRenderEvent extends Event {
   public static final PostHudRenderEvent INSTANCE = new PostHudRenderEvent();

   private CustomDrawContext context;
   private float tickDelta;

   public PostHudRenderEvent() {
   }

   @Generated
   public PostHudRenderEvent(CustomDrawContext context, float tickDelta) {
      this.set(context, tickDelta);
   }

   public PostHudRenderEvent set(CustomDrawContext context, float tickDelta) {
      this.context = context;
      this.tickDelta = tickDelta;
      return this;
   }

   @Generated
   public CustomDrawContext getContext() {
      return this.context;
   }

   @Generated
   public float getTickDelta() {
      return this.tickDelta;
   }
}
