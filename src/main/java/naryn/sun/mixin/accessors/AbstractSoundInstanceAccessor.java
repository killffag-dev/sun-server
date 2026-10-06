package naryn.sun.mixin.accessors;

import net.minecraft.client.sound.AbstractSoundInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractSoundInstance.class)
public interface AbstractSoundInstanceAccessor {
   @Accessor("volume")
   void setVolume(float volume);

   @Accessor("volume")
   float getVolume();
}
