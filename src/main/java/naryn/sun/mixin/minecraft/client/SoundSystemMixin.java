package naryn.sun.mixin.minecraft.client;

import naryn.sun.Sun;
import naryn.sun.protection.client.SoundSystemMixinProtection;
import naryn.sun.systems.modules.modules.optimization.PacketFilter;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundSystem.class)
public class SoundSystemMixin {
   @Inject(method = "play(Lnet/minecraft/client/sound/SoundInstance;)V", at = @At("HEAD"), cancellable = true)
   private void onPlaySound(SoundInstance sound, CallbackInfo ci) {
      if (sound != null && sound.getId() != null) {
         PacketFilter packetFilter = Sun.getInstance().getModuleManager().getModule(PacketFilter.class);
         if (packetFilter != null && packetFilter.shouldDropSound(sound.getId().toString())) {
            ci.cancel();
            return;
         }
      }
      SoundSystemMixinProtection.playSound(sound, ci);
   }
}
