package naryn.sun.systems.modules.modules.utility;

import lombok.Generated;
import naryn.sun.systems.modules.api.ModuleCategory;
import naryn.sun.systems.modules.api.ModuleInfo;
import naryn.sun.systems.modules.impl.BaseModule;
import naryn.sun.systems.setting.settings.SelectSetting;
import naryn.sun.systems.setting.settings.SliderSetting;

@ModuleInfo(
   name = "AntiOverlay",
   category = ModuleCategory.UTILITY,
   enabledByDefault = true,
   desc = "modules.descriptions.anti_overlay"
)
public class AntiOverlay extends BaseModule {
   // Визуальные оверлеи и элементы интерфейса
   private final SelectSetting effects = new SelectSetting(this, "modules.settings.anti_overlay.effects");
   private final SelectSetting.Value bossBar = new SelectSetting.Value(this.effects, "modules.settings.anti_overlay.bossBar");
   private final SelectSetting.Value freezing = new SelectSetting.Value(this.effects, "modules.settings.anti_overlay.freezing");
   private final SelectSetting.Value scoreboard = new SelectSetting.Value(this.effects, "modules.settings.anti_overlay.scoreboard");
   private final SelectSetting.Value spyglass = new SelectSetting.Value(this.effects, "modules.settings.anti_overlay.spyglass");
   private final SelectSetting.Value pumpkin = new SelectSetting.Value(this.effects, "modules.settings.anti_overlay.pumpkin");
   private final SelectSetting.Value vignette = new SelectSetting.Value(this.effects, "modules.settings.anti_overlay.vignette");
   private final SelectSetting.Value fire = new SelectSetting.Value(this.effects, "modules.settings.anti_overlay.fire").select();
   private final SelectSetting.Value weather = new SelectSetting.Value(this.effects, "modules.settings.anti_overlay.weather");
   private final SelectSetting.Value portal = new SelectSetting.Value(this.effects, "modules.settings.anti_overlay.portal").select();
   private final SelectSetting.Value blindness = new SelectSetting.Value(this.effects, "modules.settings.anti_overlay.blindness");
   private final SelectSetting.Value nausea = new SelectSetting.Value(this.effects, "modules.settings.anti_overlay.nausea").select();

   // Звуки
   private final SelectSetting sounds = new SelectSetting(this, "modules.settings.anti_overlay.sounds");
   private final SelectSetting.Value elytra = new SelectSetting.Value(this.sounds, "modules.settings.anti_overlay.elytra");
   private final SelectSetting.Value explosion = new SelectSetting.Value(this.sounds, "modules.settings.anti_overlay.explosion");
   private final SelectSetting.Value thunder = new SelectSetting.Value(this.sounds, "modules.settings.anti_overlay.thunder");
   private final SelectSetting.Value eating = new SelectSetting.Value(this.sounds, "modules.settings.anti_overlay.eating");
   private final SelectSetting.Value beacon = new SelectSetting.Value(this.sounds, "modules.settings.anti_overlay.beacon");
   private final SelectSetting.Value anvil = new SelectSetting.Value(this.sounds, "modules.settings.anti_overlay.anvil");
   private final SelectSetting.Value weatherSound = new SelectSetting.Value(this.sounds, "modules.settings.anti_overlay.weatherSound");
   private final SelectSetting.Value portalSound = new SelectSetting.Value(this.sounds, "modules.settings.anti_overlay.portalSound");
   private final SelectSetting.Value totem = new SelectSetting.Value(this.sounds, "modules.settings.anti_overlay.totem");
   private final SelectSetting.Value phantoms = new SelectSetting.Value(this.sounds, "modules.settings.anti_overlay.phantoms");

   // Индивидуальные слайдеры громкости для каждого звука
   private final SliderSetting elytraVolume = new SliderSetting(this, "modules.settings.anti_overlay.volume_elytra", () -> !this.elytra.isSelected())
      .min(0.0F).max(100.0F).step(1.0F).currentValue(0.0F).suffix("%");
   private final SliderSetting explosionVolume = new SliderSetting(this, "modules.settings.anti_overlay.volume_explosion", () -> !this.explosion.isSelected())
      .min(0.0F).max(100.0F).step(1.0F).currentValue(0.0F).suffix("%");
   private final SliderSetting thunderVolume = new SliderSetting(this, "modules.settings.anti_overlay.volume_thunder", () -> !this.thunder.isSelected())
      .min(0.0F).max(100.0F).step(1.0F).currentValue(0.0F).suffix("%");
   private final SliderSetting eatingVolume = new SliderSetting(this, "modules.settings.anti_overlay.volume_eating", () -> !this.eating.isSelected())
      .min(0.0F).max(100.0F).step(1.0F).currentValue(0.0F).suffix("%");
   private final SliderSetting beaconVolume = new SliderSetting(this, "modules.settings.anti_overlay.volume_beacon", () -> !this.beacon.isSelected())
      .min(0.0F).max(100.0F).step(1.0F).currentValue(0.0F).suffix("%");
   private final SliderSetting anvilVolume = new SliderSetting(this, "modules.settings.anti_overlay.volume_anvil", () -> !this.anvil.isSelected())
      .min(0.0F).max(100.0F).step(1.0F).currentValue(0.0F).suffix("%");
   private final SliderSetting weatherVolume = new SliderSetting(this, "modules.settings.anti_overlay.volume_weather", () -> !this.weatherSound.isSelected())
      .min(0.0F).max(100.0F).step(1.0F).currentValue(0.0F).suffix("%");
   private final SliderSetting portalVolume = new SliderSetting(this, "modules.settings.anti_overlay.volume_portal", () -> !this.portalSound.isSelected())
      .min(0.0F).max(100.0F).step(1.0F).currentValue(0.0F).suffix("%");
   private final SliderSetting totemVolume = new SliderSetting(this, "modules.settings.anti_overlay.volume_totem", () -> !this.totem.isSelected())
      .min(0.0F).max(100.0F).step(1.0F).currentValue(0.0F).suffix("%");
   private final SliderSetting phantomsVolume = new SliderSetting(this, "modules.settings.anti_overlay.volume_phantoms", () -> !this.phantoms.isSelected())
      .min(0.0F).max(100.0F).step(1.0F).currentValue(0.0F).suffix("%");

   @Generated
   public SelectSetting getEffects() {
      return this.effects;
   }

   @Generated
   public SelectSetting.Value getBossBar() {
      return this.bossBar;
   }

   @Generated
   public SelectSetting.Value getFreezing() {
      return this.freezing;
   }

   @Generated
   public SelectSetting.Value getScoreboard() {
      return this.scoreboard;
   }

   @Generated
   public SelectSetting.Value getSpyglass() {
      return this.spyglass;
   }

   @Generated
   public SelectSetting.Value getPumpkin() {
      return this.pumpkin;
   }

   @Generated
   public SelectSetting.Value getVignette() {
      return this.vignette;
   }

   @Generated
   public SelectSetting.Value getFire() {
      return this.fire;
   }

   @Generated
   public SelectSetting.Value getWeather() {
      return this.weather;
   }

   @Generated
   public SelectSetting.Value getPortal() {
      return this.portal;
   }

   @Generated
   public SelectSetting.Value getBlindness() {
      return this.blindness;
   }

   @Generated
   public SelectSetting.Value getNausea() {
      return this.nausea;
   }

   @Generated
   public SelectSetting getSounds() {
      return this.sounds;
   }

   @Generated
   public SelectSetting.Value getElytra() {
      return this.elytra;
   }

   @Generated
   public SelectSetting.Value getExplosion() {
      return this.explosion;
   }

   @Generated
   public SelectSetting.Value getThunder() {
      return this.thunder;
   }

   @Generated
   public SelectSetting.Value getEating() {
      return this.eating;
   }

   @Generated
   public SelectSetting.Value getBeacon() {
      return this.beacon;
   }

   @Generated
   public SelectSetting.Value getAnvil() {
      return this.anvil;
   }

   @Generated
   public SelectSetting.Value getWeatherSound() {
      return this.weatherSound;
   }

   @Generated
   public SelectSetting.Value getPortalSound() {
      return this.portalSound;
   }

   @Generated
   public SelectSetting.Value getTotem() {
      return this.totem;
   }

   @Generated
   public SelectSetting.Value getPhantoms() {
      return this.phantoms;
   }

   @Generated
   public SliderSetting getElytraVolume() {
      return this.elytraVolume;
   }

   @Generated
   public SliderSetting getExplosionVolume() {
      return this.explosionVolume;
   }

   @Generated
   public SliderSetting getThunderVolume() {
      return this.thunderVolume;
   }

   @Generated
   public SliderSetting getEatingVolume() {
      return this.eatingVolume;
   }

   @Generated
   public SliderSetting getBeaconVolume() {
      return this.beaconVolume;
   }

   @Generated
   public SliderSetting getAnvilVolume() {
      return this.anvilVolume;
   }

   @Generated
   public SliderSetting getWeatherVolume() {
      return this.weatherVolume;
   }

   @Generated
   public SliderSetting getPortalVolume() {
      return this.portalVolume;
   }

   @Generated
   public SliderSetting getTotemVolume() {
      return this.totemVolume;
   }

   @Generated
   public SliderSetting getPhantomsVolume() {
      return this.phantomsVolume;
   }
}
