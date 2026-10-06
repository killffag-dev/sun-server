package naryn.sun.ui.menu.api;

import naryn.sun.Sun;
import naryn.sun.framework.base.UIContext;
import naryn.sun.systems.event.EventListener;
import naryn.sun.systems.event.impl.render.HudRenderEvent;
import naryn.sun.systems.modules.modules.visuals.MenuModule;
import naryn.sun.ui.menu.MenuScreen;
import naryn.sun.utility.interfaces.IMinecraft;
import net.minecraft.client.MinecraftClient;

public class MenuCloseListener implements IMinecraft {
   private final EventListener<HudRenderEvent> onHudRender = event -> {
      MenuScreen menuScreen = Sun.getInstance().getMenuScreen();
      if (menuScreen != null) {
         menuScreen.getMenuAnimation().update(menuScreen.isClosing() ? 0.0F : 1.0F);
         if (!(mc.currentScreen instanceof MenuScreen) && Sun.getInstance().getModuleManager().getModule(MenuModule.class).isEnabled()) {
            Sun.getInstance().getModuleManager().getModule(MenuModule.class).setEnabled(false);
         }

         if (menuScreen.getMenuAnimation().getValue() > 0.1F && !(mc.currentScreen instanceof MenuScreen) && menuScreen.isClosing()) {
            UIContext context = UIContext.of(event.getContext(), -1, -1, MinecraftClient.getInstance().getRenderTickCounter().getTickDelta(false));
            menuScreen.render(context);
         }
      }
   };

   public MenuCloseListener() {
      Sun.getInstance().getEventManager().subscribe(this);
   }
}
