package naryn.sun.mixin.minecraft.entity;

import naryn.sun.Sun;
import naryn.sun.systems.event.impl.game.FireworkEvent;
import naryn.sun.utility.interfaces.IMinecraft;
import naryn.sun.utility.rotations.Rotation;
import naryn.sun.utility.rotations.RotationHandler;
import naryn.sun.utility.rotations.RotationState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FireworkRocketEntity.class)
public abstract class FireworkRocketEntityMixin implements IMinecraft {
   @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;setVelocity(Lnet/minecraft/util/math/Vec3d;)V"))
   private void redirectSetVelocity(LivingEntity shooter, Vec3d velocity) {
      FireworkRocketEntity rocketEntity = (FireworkRocketEntity) (Object) this;
      FireworkEvent event = new FireworkEvent(shooter, velocity, rocketEntity);
      Sun.getInstance().getEventManager().triggerEvent(event);
      shooter.setVelocity(event.getVelocity());
   }

   @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getRotationVector()Lnet/minecraft/util/math/Vec3d;"))
   private Vec3d redirectGetRotationVector(LivingEntity instance) {
      if (instance == mc.player) {
         RotationHandler rotationHandler = Sun.getInstance().getRotationHandler();
         if (rotationHandler != null && rotationHandler.getState() != RotationState.IDLE) {
            Rotation currentRotation = rotationHandler.getCurrentRotation();
            return Vec3d.fromPolar(currentRotation.getPitch(), currentRotation.getYaw());
         }
      }

      return instance.getRotationVector();
   }
}
