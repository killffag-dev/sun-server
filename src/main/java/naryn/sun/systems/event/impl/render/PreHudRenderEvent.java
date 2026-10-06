package naryn.sun.systems.event.impl.render;

import lombok.Generated;
import naryn.sun.framework.base.CustomDrawContext;
import naryn.sun.systems.event.Event;

public class PreHudRenderEvent extends Event {
   public static final PreHudRenderEvent INSTANCE = new PreHudRenderEvent();

   private CustomDrawContext context;
   private float tickDelta;

   public PreHudRenderEvent() {
   }

   @Generated
   public PreHudRenderEvent(CustomDrawContext context, float tickDelta) {
      this.set(context, tickDelta);
   }

   public PreHudRenderEvent set(CustomDrawContext context, float tickDelta) {
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
