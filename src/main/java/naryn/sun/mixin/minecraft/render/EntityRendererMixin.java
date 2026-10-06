package naryn.sun.mixin.minecraft.render;

import naryn.sun.utility.mixins.EntityRenderStateAddition;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity, S extends EntityRenderState> {
   @Inject(method = "updateRenderState", at = @At("HEAD"))
   private void updateRenderingEntity(T entity, S state, float tickDelta, CallbackInfo ci) {
      ((EntityRenderStateAddition)state).sun$setEntity(entity);
   }
}
