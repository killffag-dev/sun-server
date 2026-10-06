package naryn.sun.mixin.minecraft.render.entity;

import naryn.sun.utility.mixins.EntityRenderStateAddition;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EntityRenderState.class)
public abstract class EntityRenderStateMixin implements EntityRenderStateAddition {
   @Unique
   private Entity sun$entity;

   @Unique
   @Override
   public void sun$setEntity(Entity entity) {
      this.sun$entity = entity;
   }

   @Unique
   @Override
   public Entity sun$getEntity() {
      return this.sun$entity;
   }
}
