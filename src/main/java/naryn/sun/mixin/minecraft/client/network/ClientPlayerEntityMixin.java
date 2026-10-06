package naryn.sun.mixin.minecraft.client.network;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import naryn.sun.Sun;
import naryn.sun.systems.event.impl.game.CloseScreenEvent;
import naryn.sun.systems.event.impl.player.ClientPlayerTickEndEvent;
import naryn.sun.systems.event.impl.player.ClientPlayerTickEvent;
import naryn.sun.systems.event.impl.player.SlowDownEvent;
import naryn.sun.utility.interfaces.IMinecraft;
import naryn.sun.utility.mixins.ClientPlayerEntityAddition;
import naryn.sun.utility.rotations.RotationHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin implements ClientPlayerEntityAddition, IMinecraft {
   @Unique
   private int groundTicks = 0;

   @Redirect(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z"), require = 0)
   private boolean onIsUsingItemRedirect(ClientPlayerEntity player) {
      SlowDownEvent slowDownEvent = SlowDownEvent.INSTANCE.set();
      Sun.getInstance().getEventManager().triggerEvent(slowDownEvent);
      return player.isUsingItem() && player.getVehicle() == null && !slowDownEvent.isCancelled();
   }

   @WrapWithCondition(
      method = "closeScreen",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;setScreen(Lnet/minecraft/client/gui/screen/Screen;)V")
   )
   private boolean preventCloseScreen(MinecraftClient instance, Screen screen) {
      Sun.getInstance().getEventManager().triggerEvent(new CloseScreenEvent(screen));
      return true;
   }

   @Inject(method = "tick", at = @At("HEAD"))
   public void triggerTickEvent(CallbackInfo ci) {
      Sun.getInstance().getEventManager().triggerEvent(ClientPlayerTickEvent.INSTANCE);
   }

   @Inject(method = "tick", at = @At("RETURN"))
   public void triggerTickEndEvent(CallbackInfo ci) {
      Sun.getInstance().getEventManager().triggerEvent(ClientPlayerTickEndEvent.INSTANCE);
   }

   @Inject(method = "tickMovement", at = @At("HEAD"))
   public void updateOnGroundTicks(CallbackInfo ci) {
      if (mc.player != null && mc.player.isOnGround()) {
         this.groundTicks++;
      } else {
         this.groundTicks = 0;
      }
   }

   @Redirect(method = "sendMovementPackets", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getYaw()F"))
   public float replaceMovePacketYaw(ClientPlayerEntity instance) {
      RotationHandler rotationHandler = Sun.getInstance().getRotationHandler();
      float yaw = rotationHandler.isIdling() ? instance.getYaw() : rotationHandler.getCurrentRotation().getYaw();
      rotationHandler.getServerRotation().setYaw(yaw);
      return yaw;
   }

   @Redirect(method = "sendMovementPackets", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getPitch()F"))
   public float replaceMovePacketPitch(ClientPlayerEntity instance) {
      RotationHandler rotationHandler = Sun.getInstance().getRotationHandler();
      float pitch = rotationHandler.isIdling() ? instance.getPitch() : rotationHandler.getCurrentRotation().getPitch();
      rotationHandler.getServerRotation().setPitch(pitch);
      return pitch;
   }

   @Override
   public int sun$getOnGroundTicks() {
      return this.groundTicks;
   }

   @Inject(method = "dropSelectedItem", at = @At("HEAD"), cancellable = true)
   public void onDropSelectedItem(boolean entireStack, CallbackInfoReturnable<Boolean> cir) {
      naryn.sun.systems.modules.modules.utility.BlockSlot blockSlot = Sun.getInstance().getModuleManager().getModule(naryn.sun.systems.modules.modules.utility.BlockSlot.class);
      if (blockSlot != null && blockSlot.isEnabled() && mc.player != null) {
         if (blockSlot.isSlotIndexLocked(mc.player.getInventory().selectedSlot)) {
            cir.setReturnValue(false);
         }
      }
   }
}