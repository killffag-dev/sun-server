package naryn.sun.systems.modules.listeners;

import naryn.sun.Sun;
import naryn.sun.systems.event.EventListener;
import naryn.sun.systems.event.impl.player.ClientPlayerTickEvent;
import naryn.sun.systems.modules.Module;

public class ModuleTickListener implements EventListener<ClientPlayerTickEvent> {
   public void onEvent(ClientPlayerTickEvent event) {
      for (Module module : Sun.getInstance().getModuleManager().getActiveModules()) {
         try {
            module.tick();
            Sun.getInstance().getEventManager().reportSuccess(module);
         } catch (Throwable throwable) {
            Sun.getInstance().getEventManager().reportFailure(module, module.getClass().getSimpleName(), "tick()", throwable);
         }
      }
   }
}