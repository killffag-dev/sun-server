package naryn.sun.systems.event.impl.game;

import lombok.Generated;
import naryn.sun.systems.event.Event;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import org.jetbrains.annotations.Nullable;

public class EntityDamageEvent extends Event {
   private final LivingEntity entity;
   private final DamageSource source;

   public EntityDamageEvent(LivingEntity entity, @Nullable DamageSource source) {
      this.entity = entity;
      this.source = source;
   }

   @Generated
   public LivingEntity getEntity() {
      return this.entity;
   }

   @Nullable
   @Generated
   public DamageSource getSource() {
      return this.source;
   }
}
