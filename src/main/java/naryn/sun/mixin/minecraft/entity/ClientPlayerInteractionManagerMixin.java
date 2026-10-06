package naryn.sun.mixin.minecraft.entity;

import naryn.sun.Sun;
import naryn.sun.systems.event.impl.game.BlockBreakEvent;
import naryn.sun.systems.event.impl.game.BlockPlaceEvent;
import naryn.sun.systems.event.impl.game.InternalAttackEvent;
import naryn.sun.systems.event.impl.game.StartBreakBlockEvent;
import naryn.sun.systems.modules.modules.utility.NoInteract;
import naryn.sun.utility.game.FakePlayerEntity;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {
   @Shadow
   @Final
   private MinecraftClient client;

   @Inject(method = "attackEntity", at = @At("HEAD"), cancellable = true)
   private void sun$critPre(PlayerEntity player, Entity target, CallbackInfo ci) {
      InternalAttackEvent event = new InternalAttackEvent(target);
      Sun.getInstance().getEventManager().triggerEvent(event);
      if (event.isCancelled() || target instanceof FakePlayerEntity) {
         ci.cancel();
      }
   }

   @Inject(method = "breakBlock", at = @At("RETURN"), cancellable = true)
   public void breakBlockHook(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
      BlockBreakEvent event = new BlockBreakEvent(pos);
      Sun.getInstance().getEventManager().triggerEvent(event);
      if (event.isCancelled()) {
         cir.setReturnValue(false);
      }
   }

   @Inject(method = "attackBlock", at = @At("HEAD"), cancellable = true)
   private void onAttackBlock(BlockPos blockPos, Direction direction, CallbackInfoReturnable<Boolean> info) {
      StartBreakBlockEvent event = new StartBreakBlockEvent(blockPos);
      Sun.getInstance().getEventManager().triggerEvent(event);
      if (event.isCancelled()) {
         info.cancel();
      }
   }

   @Inject(method = "interactBlock", at = @At("HEAD"), cancellable = true)
   public void preventInteraction(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir) {
      if (player.getStackInHand(hand).getItem() instanceof BlockItem) {
         Sun.getInstance().getEventManager().triggerEvent(new BlockPlaceEvent(hitResult));
      }

      if (this.client.world != null) {
         NoInteract noInteractModule = Sun.getInstance().getModuleManager().getModule(NoInteract.class);
         Block block = this.client.world.getBlockState(hitResult.getBlockPos()).getBlock();
         if (noInteractModule.isEnabled() && noInteractModule.shouldPreventInteract(block)) {
            cir.setReturnValue(ActionResult.PASS);
         }
      }
   }
}