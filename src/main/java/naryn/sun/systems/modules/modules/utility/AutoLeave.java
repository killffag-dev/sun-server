package naryn.sun.systems.modules.modules.utility;

import naryn.sun.Sun;
import naryn.sun.access.MCPlayerAccess;
import naryn.sun.access.MCWorldAccess;
import naryn.sun.systems.event.EventListener;
import naryn.sun.systems.event.impl.network.ReceivePacketEvent;
import naryn.sun.systems.event.impl.player.ClientPlayerTickEvent;
import naryn.sun.systems.localization.Localizator;
import naryn.sun.systems.modules.api.ModuleCategory;
import naryn.sun.systems.modules.api.ModuleInfo;
import naryn.sun.systems.modules.impl.BaseModule;
import naryn.sun.systems.setting.settings.ModeSetting;
import naryn.sun.systems.setting.settings.SliderSetting;
import naryn.sun.utility.game.TextUtility;
import naryn.sun.utility.game.server.ServerUtility;
import naryn.sun.utility.time.Timer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.text.Text;

@ModuleInfo(name = "Auto Leave", category = ModuleCategory.UTILITY)
public class AutoLeave extends BaseModule {
   private final ModeSetting leave = new ModeSetting(this, "modules.settings.auto_leave.leave");
   private final ModeSetting.Value distLeave = new ModeSetting.Value(this.leave, "modules.settings.auto_leave.leave.distance");
   private final ModeSetting.Value healthLeave = new ModeSetting.Value(this.leave, "modules.settings.auto_leave.leave.health");
   private final ModeSetting.Value banLeave = new ModeSetting.Value(this.leave, "modules.settings.auto_leave.leave.ban");
   private final SliderSetting dist = new SliderSetting(
         this, "modules.settings.auto_leave.distance", () -> this.healthLeave.isSelected() || this.banLeave.isSelected()
      )
      .suffix(number -> " %s".formatted(Localizator.translate("block")) + TextUtility.makeCountTranslated(number))
      .step(1.0F)
      .min(1.0F)
      .max(150.0F)
      .currentValue(30.0F);
   private final SliderSetting health = new SliderSetting(
         this, "modules.settings.auto_leave.health", () -> this.distLeave.isSelected() || this.banLeave.isSelected()
      )
      .step(1.0F)
      .min(1.0F)
      .max(20.0F)
      .currentValue(10.0F);
   private final SliderSetting delay = new SliderSetting(
         this, "modules.settings.auto_leave.delay", () -> !this.banLeave.isSelected() || this.distLeave.isSelected() || this.healthLeave.isSelected()
      )
      .suffix(Localizator.translate("sec") + ".")
      .step(1.0F)
      .min(1.0F)
      .max(60.0F)
      .currentValue(40.0F);
   private final Timer timer = new Timer();
   private boolean waiting;
   private final ModeSetting mode = new ModeSetting(this, "modules.settings.auto_leave.mode");
   private final ModeSetting.Value hub = new ModeSetting.Value(this.mode, "modules.settings.auto_leave.mode.hub");
   private final ModeSetting.Value serverLeave = new ModeSetting.Value(this.mode, "modules.settings.auto_leave.mode.server");
   private final ModeSetting.Value spawn = new ModeSetting.Value(this.mode, "modules.settings.auto_leave.mode.spawn");
   private final EventListener<ClientPlayerTickEvent> onClientPlayerTickEvent = event -> {
      if (!MCPlayerAccess.isPresent()) return;
      var player = MCPlayerAccess.get();

      if (this.distLeave.isSelected()) {
         for (PlayerEntity e : MCWorldAccess.getPlayers()) {
            if (e != null
               && e != player
               && player.distanceTo(e) <= this.dist.getCurrentValue()
               && !ServerUtility.hasCT
               && !Sun.getInstance().getFriendManager().isFriend(e.getName().getString())) {
               if (this.hub.isSelected()) {
                  player.networkHandler.sendChatCommand("hub");
               } else if (this.serverLeave.isSelected()) {
                  player.networkHandler.getConnection().disconnect(Text.of(Localizator.translate("modules.auto_leave.near_player")));
               } else if (this.spawn.isSelected()) {
                  player.networkHandler.sendChatCommand("spawn");
               }

               this.toggle();
               break;
            }
         }
      }

      if (this.healthLeave.isSelected() && MCPlayerAccess.isPresent() && MCPlayerAccess.get().getHealth() + MCPlayerAccess.get().getAbsorptionAmount() <= this.health.getCurrentValue()) {
         if (this.hub.isSelected()) {
            MCPlayerAccess.get().networkHandler.sendChatCommand("hub");
         } else if (this.serverLeave.isSelected()) {
            MCPlayerAccess.get().networkHandler.getConnection().disconnect(Text.of(Localizator.translate("modules.auto_leave.low_health")));
         } else if (this.spawn.isSelected()) {
            MCPlayerAccess.get().networkHandler.sendChatCommand("spawn");
         }

         this.toggle();
      }

      if (this.waiting) {
         if (this.timer.finished((long)this.delay.getCurrentValue() * 1000L)) {
            MCPlayerAccess.get().networkHandler.sendChatCommand("an" + ServerUtility.ftAn);
            this.waiting = false;
         }
      }
   };
   private final EventListener<ReceivePacketEvent> onReceivePacketEvent = event -> {
      if (event.getPacket() instanceof GameMessageS2CPacket packet
         && packet.content().getString().contains(Localizator.translate("modules.auto_leave.banned_word"))
         && this.banLeave.isSelected()) {
         MCPlayerAccess.get().networkHandler.sendChatCommand("hub");
         this.timer.reset();
         this.waiting = true;
      }
   };
}
