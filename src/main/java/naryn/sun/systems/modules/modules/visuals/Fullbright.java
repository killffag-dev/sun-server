package naryn.sun.systems.modules.modules.visuals;

import lombok.Generated;
import naryn.sun.systems.event.EventListener;
import naryn.sun.systems.event.impl.network.ReceivePacketEvent;
import naryn.sun.systems.modules.api.ModuleCategory;
import naryn.sun.systems.modules.api.ModuleInfo;
import naryn.sun.systems.modules.impl.BaseModule;
import naryn.sun.systems.setting.settings.BooleanSetting;
import naryn.sun.systems.setting.settings.ModeSetting;
import naryn.sun.systems.setting.settings.SliderSetting;
import naryn.sun.utility.game.EntityUtility;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;

@ModuleInfo(name = "Fullbright", category = ModuleCategory.VISUALS, desc = "modules.descriptions.fullbright")
public class Fullbright extends BaseModule {
   private final BooleanSetting customTime = new BooleanSetting(this, "modules.settings.fullbright.custom_time");
   private final ModeSetting timeMode = new ModeSetting(this, "modules.settings.fullbright.time_mode", () -> !this.customTime.isEnabled());
   private final ModeSetting.Value sunrise = new ModeSetting.Value(this.timeMode, "modules.settings.fullbright.time_mode.sunrise");
   private final ModeSetting.Value morning = new ModeSetting.Value(this.timeMode, "modules.settings.fullbright.time_mode.morning");
   private final ModeSetting.Value noon = new ModeSetting.Value(this.timeMode, "modules.settings.fullbright.time_mode.noon").select();
   private final ModeSetting.Value sunset = new ModeSetting.Value(this.timeMode, "modules.settings.fullbright.time_mode.sunset");
   private final ModeSetting.Value midnight = new ModeSetting.Value(this.timeMode, "modules.settings.fullbright.time_mode.midnight");
   private final ModeSetting.Value custom = new ModeSetting.Value(this.timeMode, "modules.settings.fullbright.time_mode.custom");

   private final SliderSetting time = new SliderSetting(
      this,
      "modules.settings.fullbright.time",
      () -> !this.customTime.isEnabled() || !this.timeMode.is(this.custom)
   )
      .step(1000.0F)
      .min(0.0F)
      .max(24000.0F)
      .currentValue(6000.0F);

   private long oldTime;
   private final EventListener<ReceivePacketEvent> onReceivePacket = event -> {
      if (event.getPacket() instanceof WorldTimeUpdateS2CPacket && this.customTime.isEnabled()) {
         event.cancel();
      }
   };

   @Override
   public void tick() {
      if (mc.world != null) {
         if (this.customTime.isEnabled()) {
            long targetTime;
            if (this.timeMode.is(this.sunrise)) {
               targetTime = 23000L;
            } else if (this.timeMode.is(this.morning)) {
               targetTime = 1000L;
            } else if (this.timeMode.is(this.noon)) {
               targetTime = 6000L;
            } else if (this.timeMode.is(this.sunset)) {
               targetTime = 12000L;
            } else if (this.timeMode.is(this.midnight)) {
               targetTime = 18000L;
            } else {
               targetTime = (long)this.time.getCurrentValue();
            }
            mc.world.getLevelProperties().setTimeOfDay(targetTime);
         }

         super.tick();
      }
   }

   @Override
   public void onEnable() {
      if (EntityUtility.isInGame() && mc.world != null) {
         this.oldTime = mc.world.getTimeOfDay();
         super.onEnable();
      }
   }

   @Override
   public void onDisable() {
      if (EntityUtility.isInGame() && mc.world != null) {
         mc.world.getLevelProperties().setTimeOfDay(this.oldTime);
         super.onDisable();
      }
   }

   @Generated
   public BooleanSetting getCustomTime() {
      return this.customTime;
   }

   @Generated
   public ModeSetting getTimeMode() {
      return this.timeMode;
   }

   @Generated
   public SliderSetting getTime() {
      return this.time;
   }

   @Generated
   public long getOldTime() {
      return this.oldTime;
   }

   @Generated
   public EventListener<ReceivePacketEvent> getOnReceivePacket() {
      return this.onReceivePacket;
   }
}
