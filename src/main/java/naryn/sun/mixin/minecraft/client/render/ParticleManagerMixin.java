package naryn.sun.mixin.minecraft.client.render;

import naryn.sun.Sun;
import naryn.sun.systems.modules.modules.optimization.NoRender;
import naryn.sun.systems.modules.modules.optimization.Optimizer;
import naryn.sun.systems.modules.modules.optimization.PacketFilter;
import naryn.sun.systems.modules.modules.visuals.KillEffects;
import naryn.sun.systems.modules.modules.utility.AntiOverlay;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ParticleManager.class, priority = 1100)
public abstract class ParticleManagerMixin {
   @Inject(method = "addBlockBreakParticles", at = @At("HEAD"), cancellable = true)
   private void onAddBlockBreakParticles(BlockPos blockPos, BlockState state, CallbackInfo info) {
      NoRender noRender = Sun.getInstance().getModuleManager().getModule(NoRender.class);
      if (noRender != null && noRender.isEnabled() && noRender.getBlockBreak().isSelected()) {
         info.cancel();
      }
   }

   @Inject(method = "addBlockBreakingParticles", at = @At("HEAD"), cancellable = true)
   private void onAddBlockBreakingParticles(BlockPos blockPos, Direction direction, CallbackInfo info) {
      NoRender noRender = Sun.getInstance().getModuleManager().getModule(NoRender.class);
      if (noRender != null && noRender.isEnabled() && noRender.getBlockBreak().isSelected()) {
         info.cancel();
      }
   }

   @Inject(method = "addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)Lnet/minecraft/client/particle/Particle;", at = @At("HEAD"), cancellable = true)
   private void onAddParticle(
      ParticleEffect parameters, double x, double y, double z, double velocityX, double velocityY, double velocityZ, CallbackInfoReturnable<Particle> cir
   ) {
      if (parameters == null) return;
      ParticleType<?> type = parameters.getType();

      // PacketFilter: Троттлинг и дроп частиц за препятствиями
      PacketFilter packetFilter = Sun.getInstance().getModuleManager().getModule(PacketFilter.class);
      if (packetFilter != null && packetFilter.shouldDropParticle(x, y, z)) {
         cir.cancel();
         return;
      }

      // NoRender: Глубокая фильтрация частиц
      NoRender noRender = Sun.getInstance().getModuleManager().getModule(NoRender.class);
      if (noRender != null && noRender.isEnabled()) {
         if (noRender.getTotem().isSelected() && type == ParticleTypes.TOTEM_OF_UNDYING) {
            cir.cancel();
            return;
         }
         if (noRender.getPotions().isSelected() && (type == ParticleTypes.ENTITY_EFFECT || type == ParticleTypes.INSTANT_EFFECT || type == ParticleTypes.EFFECT)) {
            cir.cancel();
            return;
         }
         if (noRender.getBubbles().isSelected() && (type == ParticleTypes.BUBBLE || type == ParticleTypes.BUBBLE_COLUMN_UP || type == ParticleTypes.BUBBLE_POP || type == ParticleTypes.CURRENT_DOWN)) {
            cir.cancel();
            return;
         }
         if (noRender.getFireworks().isSelected() && (type == ParticleTypes.FIREWORK || type == ParticleTypes.FLASH)) {
            cir.cancel();
            return;
         }
         if (noRender.getExplosions().isSelected() && (type == ParticleTypes.EXPLOSION || type == ParticleTypes.EXPLOSION_EMITTER || type == ParticleTypes.POOF || type == ParticleTypes.SMOKE || type == ParticleTypes.LARGE_SMOKE || type == ParticleTypes.CAMPFIRE_COSY_SMOKE)) {
            cir.cancel();
            return;
         }
         if (noRender.getCrits().isSelected() && (type == ParticleTypes.CRIT || type == ParticleTypes.ENCHANTED_HIT || type == ParticleTypes.SWEEP_ATTACK)) {
            cir.cancel();
            return;
         }
         if (noRender.getBlockBreak().isSelected() && (type == ParticleTypes.BLOCK || type == ParticleTypes.FALLING_DUST || type == ParticleTypes.DUST_PLUME)) {
            cir.cancel();
            return;
         }
         if (noRender.getRain().isSelected() && (type == ParticleTypes.RAIN || type == ParticleTypes.SPLASH)) {
            cir.cancel();
            return;
         }
         if (noRender.getElderGuardian().isSelected() && type == ParticleTypes.ELDER_GUARDIAN) {
            cir.cancel();
            return;
         }
         if (noRender.getDragonBreath().isSelected() && type == ParticleTypes.DRAGON_BREATH) {
            cir.cancel();
            return;
         }
         if (noRender.getHearts().isSelected() && (type == ParticleTypes.HEART || type == ParticleTypes.DAMAGE_INDICATOR)) {
            cir.cancel();
            return;
         }
      }

      AntiOverlay antiOverlay = Sun.getInstance().getModuleManager().getModule(AntiOverlay.class);
      if (antiOverlay != null && antiOverlay.isEnabled() && antiOverlay.getWeather().isSelected() && type == ParticleTypes.RAIN) {
         cir.cancel();
         return;
      }

      Optimizer optimizer = Sun.getInstance().getModuleManager().getModule(Optimizer.class);
      if (optimizer != null && optimizer.isEnabled()) {
         if (optimizer.getDisableFireworks().isEnabled() && (type == ParticleTypes.FIREWORK || type == ParticleTypes.FLASH)) {
            cir.cancel();
            return;
         }
         if (optimizer.getCullParticles().isEnabled() && MinecraftClient.getInstance().world != null) {
            BlockPos pos = BlockPos.ofFloored(x, y, z);
            if (MinecraftClient.getInstance().world.getBlockState(pos).isOpaqueFullCube()) {
               cir.cancel();
               return;
            }
         }
      }

      KillEffects killEffects = Sun.getInstance().getModuleManager().getModule(KillEffects.class);
      if (killEffects != null && killEffects.isEnabled()) {
         if (type == ParticleTypes.POOF || type == ParticleTypes.EXPLOSION_EMITTER || type == ParticleTypes.SMOKE || type == ParticleTypes.LARGE_SMOKE) {
            cir.cancel();
            return;
         }
      }
   }
}
