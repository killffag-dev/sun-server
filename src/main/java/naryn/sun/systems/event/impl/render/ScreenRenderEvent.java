package naryn.sun.systems.event.impl.render;

import lombok.Generated;
import naryn.sun.framework.base.CustomDrawContext;
import naryn.sun.systems.event.Event;

public class ScreenRenderEvent extends Event {
   public static final ScreenRenderEvent INSTANCE = new ScreenRenderEvent();

   private CustomDrawContext context;
   private float tickDelta;

   public ScreenRenderEvent() {
   }

   @Generated
   public ScreenRenderEvent(CustomDrawContext context, float tickDelta) {
      this.set(context, tickDelta);
   }

   public ScreenRenderEvent set(CustomDrawContext context, float tickDelta) {
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
