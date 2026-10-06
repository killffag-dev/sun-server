package naryn.sun.protection.client;

import naryn.sun.Sun;
import naryn.sun.mixin.accessors.AbstractSoundInstanceAccessor;
import naryn.sun.systems.event.impl.game.SoundEvent;
import naryn.sun.systems.modules.modules.utility.AntiOverlay;
import net.minecraft.client.sound.AbstractSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class SoundSystemMixinProtection {
   private static final String SUN_NAMESPACE = "sun";

   private static AntiOverlay sun$antiOverlay;
   private static naryn.sun.systems.modules.modules.utility.HitSound sun$hitSound;

   private static AntiOverlay sun$getAntiOverlay() {
      if (sun$antiOverlay == null && Sun.getInstance() != null && Sun.getInstance().getModuleManager() != null) {
         sun$antiOverlay = Sun.getInstance().getModuleManager().getModule(AntiOverlay.class);
      }
      return sun$antiOverlay;
   }

   private static naryn.sun.systems.modules.modules.utility.HitSound sun$getHitSound() {
      if (sun$hitSound == null && Sun.getInstance() != null && Sun.getInstance().getModuleManager() != null) {
         sun$hitSound = Sun.getInstance().getModuleManager().getModule(naryn.sun.systems.modules.modules.utility.HitSound.class);
      }
      return sun$hitSound;
   }

   public static void playSound(SoundInstance sound, CallbackInfo ci) {
      if (sound != null && sound.getId() != null) {
         AntiOverlay antiOverlay = sun$getAntiOverlay();
         if (antiOverlay != null && antiOverlay.isEnabled()) {
            String path = sound.getId().getPath();

            if (antiOverlay.getBeacon().isSelected() && path.startsWith("block.beacon.")) {
               applyVolume(sound, antiOverlay.getBeaconVolume().getCurrentValue(), ci);
               if (ci.isCancelled()) return;
            }
            if (antiOverlay.getWeatherSound().isSelected() && (path.startsWith("weather.rain") || path.equals("weather.rain.above"))) {
               applyVolume(sound, antiOverlay.getWeatherVolume().getCurrentValue(), ci);
               if (ci.isCancelled()) return;
            }
            if (antiOverlay.getThunder().isSelected() && path.startsWith("entity.lightning_bolt.")) {
               applyVolume(sound, antiOverlay.getThunderVolume().getCurrentValue(), ci);
               if (ci.isCancelled()) return;
            }
            if (antiOverlay.getPhantoms().isSelected() && (path.startsWith("entity.phantom.") || path.equals("entity.parrot.imitate.phantom"))) {
               applyVolume(sound, antiOverlay.getPhantomsVolume().getCurrentValue(), ci);
               if (ci.isCancelled()) return;
            }
            if (antiOverlay.getPortalSound().isSelected() && path.startsWith("block.portal.")) {
               applyVolume(sound, antiOverlay.getPortalVolume().getCurrentValue(), ci);
               if (ci.isCancelled()) return;
            }
            if (antiOverlay.getExplosion().isSelected() && path.startsWith("entity.generic.explode")) {
               applyVolume(sound, antiOverlay.getExplosionVolume().getCurrentValue(), ci);
               if (ci.isCancelled()) return;
            }
            if (antiOverlay.getAnvil().isSelected() && path.startsWith("block.anvil.")) {
               applyVolume(sound, antiOverlay.getAnvilVolume().getCurrentValue(), ci);
               if (ci.isCancelled()) return;
            }
            if (antiOverlay.getTotem().isSelected() && path.equals("item.totem.use")) {
               applyVolume(sound, antiOverlay.getTotemVolume().getCurrentValue(), ci);
               if (ci.isCancelled()) return;
            }
            if (antiOverlay.getElytra().isSelected() && path.equals("item.elytra.flying")) {
               applyVolume(sound, antiOverlay.getElytraVolume().getCurrentValue(), ci);
               if (ci.isCancelled()) return;
            }
            if (antiOverlay.getEating().isSelected() && (path.startsWith("entity.generic.eat") || path.startsWith("entity.generic.drink") || path.equals("entity.player.burp") || path.endsWith(".drink"))) {
               applyVolume(sound, antiOverlay.getEatingVolume().getCurrentValue(), ci);
               if (ci.isCancelled()) return;
            }
         }
      }

      naryn.sun.systems.modules.modules.utility.HitSound hitSound = sun$getHitSound();
      if (hitSound != null && hitSound.isEnabled() && sound != null && sound.getId() != null) {
         boolean isOwnSound = sound.getId().getNamespace().equals(SUN_NAMESPACE);

         if (!isOwnSound) {
            String path = sound.getId().getPath();
            boolean isPlayerAttack = path.startsWith("entity.player.attack") || path.startsWith("item.mace.smash");
            boolean isCrit = path.equals("entity.player.attack.crit");
            boolean isPlayerHurt = path.startsWith("entity.player.hurt");
            boolean isHurtOrDamage = path.endsWith(".hurt")
               || path.contains(".hurt_")
               || path.endsWith(".hit")
               || path.endsWith(".damage");

            if (isPlayerAttack) {
               // Всегда отменяем ванильный звук атаки игрока (свои и чужие).
               // Свои звуки атаки на клиенте играются до ответа сервера (в том числе на спавне).
               // Полная отмена здесь гарантирует, что на спавне не проскочит ни ванильный звук, ни ложный хитсаунд.
               ci.cancel();
               if (isCrit) {
                  hitSound.markCrit();
               }
            } else if (isPlayerHurt || (isHurtOrDamage && hitSound.isRecentAttack())) {
               // Сервер подтвердил урон: по игроку (своему или противнику) или по мобу при атаке
               ci.cancel();
               hitSound.onDamageConfirmed(hitSound.isRecentCrit());
            }
         }
      }

      Sun.getInstance().getEventManager().triggerEvent(SoundEvent.INSTANCE.set(sound));
   }

   private static void applyVolume(SoundInstance sound, float volumePercent, CallbackInfo ci) {
      if (volumePercent <= 0.0F) {
         ci.cancel();
         return;
      }
      if (volumePercent < 100.0F && sound instanceof AbstractSoundInstance abstractSound) {
         ((AbstractSoundInstanceAccessor) abstractSound).setVolume(abstractSound.getVolume() * (volumePercent / 100.0F));
      }
   }
}