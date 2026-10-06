package naryn.sun.mixin.minecraft.client.network;

import naryn.sun.Sun;
import naryn.sun.systems.event.impl.game.PickupEvent;
import naryn.sun.systems.event.impl.game.WorldChangeEvent;
import naryn.sun.utility.interfaces.IMinecraft;
import naryn.sun.utility.rotations.Rotation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.client.network.ClientConnectionState;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.s2c.play.EntityDamageS2CPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.ItemPickupAnimationS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin extends ClientCommonNetworkHandler implements IMinecraft {
   @Unique
   private Rotation oldRotation = Rotation.ZERO;

   protected ClientPlayNetworkHandlerMixin(MinecraftClient client, ClientConnection connection, ClientConnectionState connectionState) {
      super(client, connection, connectionState);
   }

   @Inject(
      method = "onItemPickupAnimation",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/world/ClientWorld;getEntityById(I)Lnet/minecraft/entity/Entity;", ordinal = 0)
   )
   private void onItemPickupAnimation(ItemPickupAnimationS2CPacket packet, CallbackInfo info) {
      Entity itemEntity = this.client.world.getEntityById(packet.getEntityId());
      Entity entity = this.client.world.getEntityById(packet.getCollectorEntityId());
      if (itemEntity instanceof ItemEntity && entity == this.client.player) {
         Sun.getInstance().getEventManager().triggerEvent(new PickupEvent(((ItemEntity)itemEntity).getStack(), packet.getStackAmount()));
      }
   }

   @Inject(method = "onGameJoin", at = @At("TAIL"))
   private void onGameJoin(GameJoinS2CPacket packet, CallbackInfo ci) {
      Sun.getInstance().getEventManager().triggerEvent(new WorldChangeEvent());
   }

   @Inject(method = "onPlayerPositionLook", at = @At("HEAD"))
   public void savePlayerRotation(PlayerPositionLookS2CPacket packet, CallbackInfo ci) {
      if (mc.player != null) {
         this.oldRotation = new Rotation(mc.player.getYaw(), mc.player.getPitch());
      }
   }

   @Inject(method = "onEntityDamage", at = @At("HEAD"))
   private void onEntityDamageHook(EntityDamageS2CPacket packet, CallbackInfo ci) {
      naryn.sun.systems.modules.modules.utility.HitSound hitSound = Sun.getInstance().getModuleManager().getModule(naryn.sun.systems.modules.modules.utility.HitSound.class);
      if (hitSound != null && hitSound.isEnabled() && mc.player != null) {
         boolean isFromMe = packet.sourceCauseId() == mc.player.getId() || hitSound.isRecentAttack();
         boolean isOpponentCombat = packet.sourceCauseId() != -1;
         if (isFromMe || isOpponentCombat) {
            hitSound.onDamageConfirmed(hitSound.isRecentCrit());
         }
      }
      naryn.sun.systems.modules.modules.visuals.hitpoint.HitPoint hitPoint = Sun.getInstance().getModuleManager().getModule(naryn.sun.systems.modules.modules.visuals.hitpoint.HitPoint.class);
      if (hitPoint != null && hitPoint.isEnabled()) {
         hitPoint.onDamagePacket(packet.entityId());
      }
   }
}
