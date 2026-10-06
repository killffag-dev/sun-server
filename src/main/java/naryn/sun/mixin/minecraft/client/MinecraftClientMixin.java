package naryn.sun.mixin.minecraft.client;

import naryn.sun.Sun;
import naryn.sun.protection.client.MinecraftClientMixinProtection;
import naryn.sun.systems.event.impl.game.GameTickEvent;
import naryn.sun.utility.render.penis.PenisAtlas;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
   @Shadow
   private int itemUseCooldown;

   @Inject(method = "tick", at = @At("HEAD"))
   public void tick(CallbackInfo ci) {
      Sun.getInstance().getEventManager().triggerEvent(new GameTickEvent());
   }

   @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;onResolutionChanged()V"))
   public void initializeClient(RunArgs args, CallbackInfo ci) {
      MinecraftClientMixinProtection.init();
   }

   @Inject(method = "<init>", at = @At("RETURN"))
   public void endInitialize(RunArgs args, CallbackInfo ci) {
      PenisAtlas atlas = PenisAtlas.getOrCreateAtlasFor(16, 16);
      atlas.registerAnimationFromPenisFile(Sun.id("penises/combat.penis"));
      atlas.registerAnimationFromPenisFile(Sun.id("penises/movement.penis"));
      atlas.registerAnimationFromPenisFile(Sun.id("penises/visuals.penis"));
      atlas.registerAnimationFromPenisFile(Sun.id("penises/player.penis"));
      atlas.registerAnimationFromPenisFile(Sun.id("penises/other.penis"));
      atlas.registerAnimationFromPenisFile(Sun.id("penises/search.penis"));
      atlas.buildAtlas();
      PenisAtlas atlas12 = PenisAtlas.getOrCreateAtlasFor(12, 12);
      atlas12.registerAnimationFromPenisFile(Sun.id("penises/check_enable.penis"));
      atlas12.registerAnimationFromPenisFile(Sun.id("penises/check_disable.penis"));
      atlas12.buildAtlas();
   }

   @Inject(method = "stop", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;close()V", shift = Shift.AFTER))
   public void shutdownClient(CallbackInfo ci) {
      MinecraftClientMixinProtection.shutdown();
   }

   @Inject(method = "getWindowTitle", at = @At("HEAD"), cancellable = true)
   public void changeWindowTitle(CallbackInfoReturnable<String> cir) {
      MinecraftClientMixinProtection.updateTitle(cir);
   }

      @Inject(method = "doAttack", at = @At("HEAD"))
    private void onDoAttack(CallbackInfoReturnable<Boolean> cir) {
      MinecraftClient client = (MinecraftClient) (Object) this;
      if (client.player != null && client.crosshairTarget != null) {
         if (client.crosshairTarget.getType() != net.minecraft.util.hit.HitResult.Type.ENTITY) {
            naryn.sun.systems.modules.modules.utility.HitSound hitSound = Sun.getInstance().getModuleManager().getModule(naryn.sun.systems.modules.modules.utility.HitSound.class);
            if (hitSound != null && hitSound.isEnabled()) {
               hitSound.playMissSound();
            }
         }
      }
   }

   @Inject(method = "hasOutline", at = @At("HEAD"), cancellable = true)
   private void sun$friendHasOutline(net.minecraft.entity.Entity entity, CallbackInfoReturnable<Boolean> cir) {
      if (entity instanceof net.minecraft.entity.player.PlayerEntity player) {
         naryn.sun.systems.modules.modules.visuals.Friends friends = Sun.getInstance().getModuleManager().getModule(naryn.sun.systems.modules.modules.visuals.Friends.class);
         if (friends != null && friends.isEnabled() && friends.isOutline()) {
            if (Sun.getInstance().getFriendManager().isFriend(player.getName().getString())) {
               cir.setReturnValue(true);
               return;
            }
         }
         naryn.sun.systems.modules.modules.visuals.Target target = Sun.getInstance().getModuleManager().getModule(naryn.sun.systems.modules.modules.visuals.Target.class);
         if (target != null && target.isEnabled() && target.isOutline()) {
            if (Sun.getInstance().getTargetManager().isTarget(player.getName().getString())) {
               if (naryn.sun.systems.modules.modules.visuals.Target.shouldShowOutline(player)) {
                  cir.setReturnValue(true);
               }
            }
         }
      }
   }
}
