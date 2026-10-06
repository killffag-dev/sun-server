package naryn.sun.systems.modules.listeners;

import naryn.sun.systems.event.EventListener;
import naryn.sun.systems.event.impl.render.HudRenderEvent;

public class ModuleWidgetRenderer implements EventListener<HudRenderEvent> {
   public void onEvent(HudRenderEvent event) {
      naryn.sun.systems.theme.PaletteConfig.getInstance().updatePerFrame();
   }
}
