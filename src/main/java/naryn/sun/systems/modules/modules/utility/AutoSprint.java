package naryn.sun.systems.modules.modules.utility;

import naryn.sun.systems.event.EventListener;
import naryn.sun.systems.event.impl.player.ClientPlayerTickEvent;
import naryn.sun.systems.modules.api.ModuleCategory;
import naryn.sun.systems.modules.api.ModuleInfo;
import naryn.sun.systems.modules.impl.BaseModule;

@ModuleInfo(name = "Auto Sprint", category = ModuleCategory.UTILITY, enabledByDefault = true)
public class AutoSprint extends BaseModule {
   private final EventListener<ClientPlayerTickEvent> onUpdateEvent = event -> mc.options.sprintKey.setPressed(true);
}
