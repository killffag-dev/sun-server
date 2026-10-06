package naryn.sun.systems.modules.modules.visuals;

import lombok.Generated;
import naryn.sun.Sun;
import naryn.sun.systems.modules.api.ModuleCategory;
import naryn.sun.systems.modules.api.ModuleInfo;
import naryn.sun.systems.modules.impl.BaseModule;
import naryn.sun.systems.setting.settings.SliderSetting;
import naryn.sun.ui.menu.MenuScreen;
import naryn.sun.ui.menu.NewScreen;
import naryn.sun.ui.menu.api.MenuCloseListener;
import naryn.sun.utility.sounds.ClientSoundManager;


@ModuleInfo(name = "Menu", category = ModuleCategory.VISUALS, key = 344, desc = "modules.descriptions.menu")
public class MenuModule extends BaseModule {
   private static final MenuCloseListener menuCloseListener = new MenuCloseListener();
   private final SliderSetting scrollSpeed = new SliderSetting(this, "modules.settings.menu.scroll_speed")
      .step(0.05F)
      .min(0.2F)
      .max(3.0F)
      .currentValue(naryn.sun.systems.animation.ClientAnimationConfig.getInstance().getMenuScrollSpeed());

   {
      this.scrollSpeed.onChange(s -> {
         naryn.sun.systems.animation.ClientAnimationConfig.getInstance().setMenuScrollSpeed(this.scrollSpeed.getCurrentValue());
         Sun.getInstance().getFileManager().writeFile("client");
      });
   }

    @Override
    public void onEnable() {
        MenuScreen screen = Sun.getInstance().getMenuScreen();
        if (!(screen instanceof NewScreen)) {
            screen = new NewScreen();
            Sun.getInstance().setMenuScreen(screen);
        }
        mc.setScreen(screen);
        ClientSoundManager.getInstance().playMenuOpen();
    }


   @Override
   public void onDisable() {
      if (mc.currentScreen instanceof MenuScreen) {
         mc.setScreen(null);
         Sun.getInstance().getMenuScreen().setClosing(true);
      }

      super.onDisable();
   }

   public naryn.sun.systems.setting.settings.SliderSetting getScrollSpeedSetting() {
      return scrollSpeed;
   }
}
