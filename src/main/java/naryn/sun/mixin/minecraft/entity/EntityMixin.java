package naryn.sun.mixin.minecraft.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import java.util.ArrayList;
import java.util.List;
import naryn.sun.Sun;
import naryn.sun.utility.interfaces.IMinecraft;
import naryn.sun.utility.mixins.BacktrackableEntity;
import naryn.sun.utility.rotations.MoveCorrection;
import naryn.sun.utility.rotations.RotationHandler;
import naryn.sun.utility.rotations.RotationTask;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin implements IMinecraft, BacktrackableEntity {
   @Unique
   private final List<Object> backTracks = new ArrayList<>();

   @ModifyExpressionValue(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;isControlledByPlayer()Z"))
   public boolean fixFalldistanceValue(boolean original) {
      return (Object) this == mc.player ? false : original;
   }

   @Redirect(method = "updateVelocity", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getYaw()F"))
   public float movementCorrection(Entity instance) {
      RotationHandler rotationHandler = Sun.INSTANCE.getRotationHandler();
      RotationTask currentTask = rotationHandler.getCurrentTask();
      return currentTask != null && currentTask.getMoveCorrection() != MoveCorrection.NONE
            && instance instanceof ClientPlayerEntity
                  ? rotationHandler.getCurrentRotation().getYaw()
                  : instance.getYaw();
   }

   @Override
   public List<Object> sun2_0$getBackTracks() {
      return this.backTracks;
   }

   @Inject(method = "getTeamColorValue", at = @At("HEAD"), cancellable = true)
   private void sun$friendTeamColor(CallbackInfoReturnable<Integer> cir) {
      if ((Object) this instanceof net.minecraft.entity.player.PlayerEntity player) {
         naryn.sun.systems.modules.modules.visuals.Friends friends = Sun.getInstance().getModuleManager().getModule(naryn.sun.systems.modules.modules.visuals.Friends.class);
         if (friends != null && friends.isEnabled() && friends.isOutline()) {
            if (Sun.getInstance().getFriendManager().isFriend(player.getName().getString())) {
               cir.setReturnValue(0x00FF00);
               return;
            }
         }
         naryn.sun.systems.modules.modules.visuals.Target target = Sun.getInstance().getModuleManager().getModule(naryn.sun.systems.modules.modules.visuals.Target.class);
         if (target != null && target.isEnabled() && target.isOutline()) {
            if (Sun.getInstance().getTargetManager().isTarget(player.getName().getString())) {
               if (naryn.sun.systems.modules.modules.visuals.Target.shouldShowOutline(player)) {
                  cir.setReturnValue(0xFF0000);
               }
            }
         }
      }
   }
}