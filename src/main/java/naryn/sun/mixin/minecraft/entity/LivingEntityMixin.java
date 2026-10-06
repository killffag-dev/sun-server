package naryn.sun.mixin.minecraft.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import naryn.sun.Sun;
import naryn.sun.systems.event.impl.game.EntityDeathEvent;
import naryn.sun.systems.event.impl.game.EntityJumpEvent;
import naryn.sun.systems.modules.modules.visuals.SwingAnimation;
import naryn.sun.utility.rotations.MoveCorrection;
import naryn.sun.utility.rotations.RotationHandler;
import naryn.sun.utility.rotations.RotationTask;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Entity.RemovalReason;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
   @Shadow
   private int jumpingCooldown;

   @Shadow
   public abstract void remove(RemovalReason var1);

   @Shadow
   public abstract ItemStack getMainHandStack();

   @ModifyReturnValue(method = "getHandSwingDuration", at = @At("RETURN"))
   public int replaceSwingSpeed(int original) {
      SwingAnimation swingAnimationModule = Sun.getInstance().getModuleManager().getModule(SwingAnimation.class);
      return swingAnimationModule.isEnabled() && swingAnimationModule.shouldApplyAnimation(this.getMainHandStack())
            ? (int) (original * Sun.getInstance().getSwingManager().getSpeed().getCurrentValue())
            : original;
   }

   @Inject(method = "jump", at = @At("HEAD"), cancellable = true)
   public void triggerJumpEvent(CallbackInfo ci) {
      LivingEntity livingEntity = (LivingEntity) (Object) this;
      EntityJumpEvent event = new EntityJumpEvent(livingEntity);
      Sun.getInstance().getEventManager().triggerEvent(event);
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @ModifyExpressionValue(method = "jump", at = @At(value = "NEW", target = "(DDD)Lnet/minecraft/util/math/Vec3d;"))
   public Vec3d movementCorrection(Vec3d original) {
      RotationHandler rotationHandler = Sun.INSTANCE.getRotationHandler();
      RotationTask currentTask = rotationHandler.getCurrentTask();
      if ((Object) this != MinecraftClient.getInstance().player) {
         return original;
      } else if (currentTask != null && currentTask.getMoveCorrection() != MoveCorrection.NONE) {
         float yaw = rotationHandler.getCurrentRotation().getYaw() * (float) (Math.PI / 180.0);
         return new Vec3d(-MathHelper.sin(yaw) * 0.2F, 0.0, MathHelper.cos(yaw) * 0.2F);
      } else {
         return original;
      }
   }



   @Inject(method = "onDeath", at = @At("TAIL"))
   public void triggerEntityDeathEvent(DamageSource damageSource, CallbackInfo ci) {
      LivingEntity entity = (LivingEntity) (Object) this;
      Sun.getInstance().getEventManager().triggerEvent(new EntityDeathEvent(entity, damageSource));
   }

   @Redirect(method = "calcGlidingVelocity(Lnet/minecraft/util/math/Vec3d;)Lnet/minecraft/util/math/Vec3d;", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getPitch()F"))
   private float redirectGetPitch(LivingEntity instance) {
      RotationHandler rotationHandler = Sun.getInstance().getRotationHandler();
      return rotationHandler.isIdling() ? instance.getPitch() : rotationHandler.getCurrentRotation().getPitch();
   }

   @Redirect(method = "calcGlidingVelocity(Lnet/minecraft/util/math/Vec3d;)Lnet/minecraft/util/math/Vec3d;", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getRotationVector()Lnet/minecraft/util/math/Vec3d;"))
   private Vec3d redirectGetRotationVector(LivingEntity instance) {
      RotationHandler rotationHandler = Sun.getInstance().getRotationHandler();
      return rotationHandler.isIdling() ? instance.getRotationVector()
            : rotationHandler.getCurrentRotation().getRotationVector();
   }
}
