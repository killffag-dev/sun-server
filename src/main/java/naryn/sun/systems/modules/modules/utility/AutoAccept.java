package naryn.sun.systems.modules.modules.utility;

import naryn.sun.Sun;
import naryn.sun.systems.event.EventListener;
import naryn.sun.systems.event.impl.network.ReceivePacketEvent;
import naryn.sun.systems.modules.api.ModuleCategory;
import naryn.sun.systems.modules.api.ModuleInfo;
import naryn.sun.systems.modules.impl.BaseModule;
import naryn.sun.systems.setting.settings.ModeSetting;
import naryn.sun.utility.game.server.ServerUtility;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;

@ModuleInfo(name = "Auto Accept", category = ModuleCategory.UTILITY, desc = "Автоматически принимает телепортацию")
public class AutoAccept extends BaseModule {
   private final ModeSetting acceptMode = new ModeSetting(this, "modules.settings.auto_accept.mode");
   private final ModeSetting.Value acceptAll = new ModeSetting.Value(this.acceptMode, "modules.settings.auto_accept.mode.all");
   private final ModeSetting.Value friendsOnly = new ModeSetting.Value(this.acceptMode, "modules.settings.auto_accept.mode.friends");
   private final EventListener<ReceivePacketEvent> onReceivePacketEvent = event -> {
      if (event.getPacket() instanceof GameMessageS2CPacket packet
         && mc.player != null
         && packet.content().getString().contains("телепортироваться")
         && !ServerUtility.hasCT
         && this.canAccept(packet.content().getString())) {
         mc.player.networkHandler.sendChatCommand("tpaccept");
      }
   };

   private boolean canAccept(String message) {
      if (this.acceptMode.is(this.acceptAll)) {
         return true;
      } else if (this.acceptMode.is(this.friendsOnly)) {
         String[] parts = message.split(" ");
         if (parts.length >= 2 && Sun.getInstance().getFriendManager().isFriend(parts[1])) {
            return true;
         }
         if (Sun.getInstance()
               .getFriendManager()
               .isFriend(message.replace("\u0a77 просит телепортироваться к Вам.\u0a77§l [ੲ§l✔\u0a77§l]\u0a77§l [\u0a7c§l✗\u0a77§l]", "").replace("੶", ""))
            || Sun.getInstance().getFriendManager().isFriend(message.replace("➝ Ник: ", "").trim())) {
            return true;
         }

         if (message.contains("телепортироваться") && parts.length >= 3) {
            return Sun.getInstance().getFriendManager().isFriend(parts[2]);
         }
      }

      return false;
   }
}
