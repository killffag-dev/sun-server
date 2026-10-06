package naryn.sun.mixin.minecraft.entity;

import naryn.sun.Sun;
import naryn.sun.systems.event.impl.game.AttackEvent;
import naryn.sun.systems.event.impl.game.PostAttackEvent;
import naryn.sun.utility.rotations.RotationHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {
   @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
   private void attackAHook2(Entity target, CallbackInfo ci) {
      if ((Object) this != MinecraftClient.getInstance().player) {
         return;
      }
      AttackEvent event = new AttackEvent(target);
      Sun.getInstance().getEventManager().triggerEvent(event);
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @Inject(method = "attack", at = @At("RETURN"), cancellable = true)
   private void attackAHook(Entity target, CallbackInfo ci) {
      if ((Object) this != MinecraftClient.getInstance().player) {
         return;
      }
      PostAttackEvent event = new PostAttackEvent(target);
      Sun.getInstance().getEventManager().triggerEvent(event);
   }



   @Redirect(method = "travel(Lnet/minecraft/util/math/Vec3d;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;getRotationVector()Lnet/minecraft/util/math/Vec3d;"))
   private Vec3d redirectGetRotationVectorInTravel(PlayerEntity instance) {
      RotationHandler rotationHandler = Sun.getInstance().getRotationHandler();
      return rotationHandler.isIdling() ? instance.getRotationVector()
            : rotationHandler.getCurrentRotation().getRotationVector();
   }
}
