package naryn.sun.systems.modules.modules.utility;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import naryn.sun.systems.event.EventListener;
import naryn.sun.systems.event.impl.network.ReceivePacketEvent;
import naryn.sun.systems.modules.api.ModuleCategory;
import naryn.sun.systems.modules.api.ModuleInfo;
import naryn.sun.systems.modules.impl.BaseModule;
import naryn.sun.systems.setting.settings.BooleanSetting;
import naryn.sun.systems.setting.settings.StringSetting;
import naryn.sun.utility.game.TextUtility;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;

@ModuleInfo(name = "Auto Auth", category = ModuleCategory.UTILITY, desc = "Автоматически регистрирует аккаунт на сервере")
public class AutoAuth extends BaseModule {
   private final BooleanSetting random = new BooleanSetting(this, "modules.settings.auto_auth.random");
   private final StringSetting password = new StringSetting(this, "modules.settings.auto_auth.password", this.random::isEnabled).text("123123");
   private final Map<String, String> nickAndPassword = new HashMap<>();
   private final EventListener<ReceivePacketEvent> onReceivePacketEvent = event -> {
      if (event.getPacket() instanceof GameMessageS2CPacket packet && mc.player != null) {
         String message = packet.content().getString().toLowerCase();
         String randomPass = TextUtility.getRandomNick();
         String password = this.random.isEnabled() ? randomPass : this.password.getText();
         this.nickAndPassword.put(mc.player.getDisplayName().getString(), " " + randomPass);
         if (!message.contains("зарегистрируйтесь") && !message.contains("/reg")) {
            if (message.contains("авторизуйтесь") || message.contains("/login") || message.contains("/l") && message.matches("/l(\\s|$)")) {
               mc.player.networkHandler.sendChatCommand(String.format("l %s", password));
            }
         } else {
            mc.player.networkHandler.sendChatCommand(String.format("reg %s %s", password, password));
         }
      }
   };

   public Map<String, String> listPassword() {
      return Collections.unmodifiableMap(this.nickAndPassword);
   }

   public void put(String key, String value) {
      this.nickAndPassword.put(key, value);
   }
}
