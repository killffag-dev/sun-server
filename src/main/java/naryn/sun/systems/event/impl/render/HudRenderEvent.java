package naryn.sun.systems.event.impl.render;

import lombok.Generated;
import naryn.sun.framework.base.CustomDrawContext;
import naryn.sun.systems.event.Event;

public class HudRenderEvent extends Event {
   public static final HudRenderEvent INSTANCE = new HudRenderEvent();

   private CustomDrawContext context;
   private float tickDelta;

   public HudRenderEvent() {
   }

   @Generated
   public HudRenderEvent(CustomDrawContext context, float tickDelta) {
      this.set(context, tickDelta);
   }

   public HudRenderEvent set(CustomDrawContext context, float tickDelta) {
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
