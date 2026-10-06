package naryn.sun.systems.event.impl.game;

import lombok.Generated;
import naryn.sun.systems.event.Event;
import net.minecraft.client.sound.SoundInstance;

public class SoundEvent extends Event {
   public SoundInstance sound;

   public static final SoundEvent INSTANCE = new SoundEvent(null);

   public SoundEvent(SoundInstance sound) {
      this.sound = sound;
   }

   public SoundEvent set(SoundInstance sound) {
      this.sound = sound;
      return this;
   }

   @Generated
   public SoundInstance getSound() {
      return this.sound;
   }
}
