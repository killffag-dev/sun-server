package naryn.sun.mixin.minecraft.client.render.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import naryn.sun.Sun;
import naryn.sun.mixin.accessors.BipedEntityModelAccessor;
import naryn.sun.mixin.accessors.LivingEntityRendererAccessor;
import naryn.sun.systems.modules.modules.optimization.Optimizer;
import naryn.sun.systems.modules.modules.optimization.SmartCull;
import naryn.sun.systems.modules.modules.visuals.HitColor;
import naryn.sun.systems.modules.modules.visuals.KillEffects;
import naryn.sun.systems.modules.modules.visuals.headcosmetics.ChinaHatFeatureRenderer;
import naryn.sun.systems.modules.modules.visuals.headcosmetics.HaloFeatureRenderer;
import naryn.sun.systems.modules.modules.visuals.bbmodel.BbCosmeticFeatureRenderer;
import naryn.sun.utility.colors.Colors;
import naryn.sun.utility.mixins.EntityRenderStateAddition;
import naryn.sun.utility.game.FakePlayerEntity;
import naryn.sun.utility.render.HitColorVertexConsumer;
import naryn.sun.utility.rotations.RotationHandler;
import naryn.sun.systems.modules.modules.visuals.NameUtility;
import naryn.sun.systems.modules.modules.visuals.nameutility.ServerNameTagFilter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.feature.DolphinHeldItemFeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FoxHeldItemFeatureRenderer;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.client.render.entity.feature.PandaHeldItemFeatureRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>> {

   @Unique private static HitColor sun$hitColor;
   @Unique private static KillEffects sun$killEffects;
   @Unique private static NameUtility sun$nameUtility;
   @Unique private static SmartCull sun$smartCull;
   @Unique private static Optimizer sun$optimizer;

   @Unique
   private static HitColor sun$getHitColor() {
      if (sun$hitColor == null && Sun.getInstance() != null && Sun.getInstance().getModuleManager() != null) {
         sun$hitColor = Sun.getInstance().getModuleManager().getModule(HitColor.class);
      }
      return sun$hitColor;
   }

   @Unique
   private static KillEffects sun$getKillEffects() {
      if (sun$killEffects == null && Sun.getInstance() != null && Sun.getInstance().getModuleManager() != null) {
         sun$killEffects = Sun.getInstance().getModuleManager().getModule(KillEffects.class);
      }
      return sun$killEffects;
   }

   @Unique
   private static NameUtility sun$getNameUtility() {
      if (sun$nameUtility == null && Sun.getInstance() != null && Sun.getInstance().getModuleManager() != null) {
         sun$nameUtility = Sun.getInstance().getModuleManager().getModule(NameUtility.class);
      }
      return sun$nameUtility;
   }

   @Unique
   private static SmartCull sun$getSmartCull() {
      if (sun$smartCull == null && Sun.getInstance() != null && Sun.getInstance().getModuleManager() != null) {
         sun$smartCull = Sun.getInstance().getModuleManager().getModule(SmartCull.class);
      }
      return sun$smartCull;
   }

   @Unique
   private static Optimizer sun$getOptimizer() {
      if (sun$optimizer == null && Sun.getInstance() != null && Sun.getInstance().getModuleManager() != null) {
         sun$optimizer = Sun.getInstance().getModuleManager().getModule(Optimizer.class);
      }
      return sun$optimizer;
   }

   // protected LivingEntityRenderer.getTexture(S) вынесен в
   // LivingEntityRendererAccessor (Version Adapter, Этап 3) как invoker.

   @ModifyExpressionValue(
      method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/LivingEntityRenderer;clampBodyYaw(Lnet/minecraft/entity/LivingEntity;FF)F")
   )
   public float changeYaw(float oldValue, LivingEntity entity) {
      if (entity instanceof ClientPlayerEntity) {
         RotationHandler rotationHandler = Sun.getInstance().getRotationHandler();
         float yaw = rotationHandler.isIdling() ? oldValue : rotationHandler.getRenderRotation().getYaw();
         rotationHandler.getServerRotation().setYaw(yaw);
         return yaw;
      } else {
         return oldValue;
      }
   }

   @ModifyReturnValue(method = "getRenderLayer", at = @At("RETURN"))
   private RenderLayer changeRenderLayer(RenderLayer original, S state, boolean showBody, boolean translucent, boolean showOutline) {
      Entity entity = ((EntityRenderStateAddition) state).sun$getEntity();
      if (entity instanceof FakePlayerEntity ghost && !ghost.isSolidDummy()) {
         // обычный слой кожи (entity_cutout_no_cull) не блендит альфу — поэтому раньше
         // setShaderColor(...alpha...) в Trails не давал прозрачности. Переключаем именно
         // призраков на translucent-слой, чтобы альфа реально применялась.
         Identifier texture = ((LivingEntityRendererAccessor<S>) (Object) this).invokeGetTexture(state);
         return RenderLayer.getEntityTranslucent(texture);
      }
      return original;
   }

   @ModifyExpressionValue(
      method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;lerpAngleDegrees(FFF)F")
   )
   public float changeHeadYaw(float oldValue, LivingEntity entity) {
      if (entity instanceof ClientPlayerEntity ) {
         RotationHandler rotationHandler = Sun.getInstance().getRotationHandler();
         float yaw = rotationHandler.isIdling() ? oldValue : rotationHandler.getRenderRotation().getYaw();
         rotationHandler.getServerRotation().setYaw(yaw);
         return yaw;
      } else {
         return oldValue;
      }
   }

   @ModifyExpressionValue(
      method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getLerpedPitch(F)F")
   )
   public float changePitch(float oldValue, LivingEntity entity) {
      if (entity instanceof ClientPlayerEntity ) {
         RotationHandler rotationHandler = Sun.getInstance().getRotationHandler();
         float pitch = rotationHandler.isIdling() ? oldValue : rotationHandler.getRenderRotation().getPitch();
         rotationHandler.getServerRotation().setPitch(pitch);
         return pitch;
      } else {
         return oldValue;
      }
   }

   @WrapOperation(
      method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/entity/model/EntityModel;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;III)V"
      )
   )
   private void changeModelColor(
      EntityModel<?> instance,
      MatrixStack matrixStack,
      VertexConsumer vertexConsumer,
      int light,
      int overlay,
      int color,
      Operation<Void> original,
      @Local(argsOnly = true) S livingEntityRenderState
   ) {

      Entity entity = ((EntityRenderStateAddition)livingEntityRenderState).sun$getEntity();

      HitColor hitColor = sun$getHitColor();
      if (hitColor != null && hitColor.isEnabled()
         && livingEntityRenderState.hurt
         && (hitColor.getTarget().is(hitColor.getBody()) || hitColor.getTarget().is(hitColor.getBoth()))) {
         // Гасим ванильную бело-красную вспышку урона (она рисуется через overlay, а не color) —
         // иначе она блендится поверх нашего цвета и создаёт эффект "перекрашена только половина".
         overlay = OverlayTexture.DEFAULT_UV;
         color = hitColor.getPackedColor();
      }

      original.call(new Object[]{instance, matrixStack, vertexConsumer, light, overlay, color});
   }

   // Второй слой скина/шерсть/роба/ошейник и т.д. рисуются не базовой моделью, а отдельными
   // FeatureRenderer'ами. FeatureRenderer — сам generic-класс с собственным S extends
   // EntityRenderState (а не LivingEntityRenderState!), поэтому дескриптор его render(...)
   // в байткоде использует именно EntityRenderState — тип hurt-состояния сужаем вручную.
   @WrapOperation(
      method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/entity/feature/FeatureRenderer;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/state/EntityRenderState;FF)V"
      )
   )
   private void sun$wrapFeatureVertexProvider(
      FeatureRenderer<S, M> instance,
      MatrixStack matrices,
      VertexConsumerProvider vertexConsumerProvider,
      int light,
      EntityRenderState state,
      float yawDegrees,
      float pitch,
      Operation<Void> original
   ) {
      boolean hurt = state instanceof LivingEntityRenderState living && living.hurt;

      boolean excluded = instance instanceof ArmorFeatureRenderer
         || instance instanceof HeldItemFeatureRenderer
         || instance instanceof FoxHeldItemFeatureRenderer
         || instance instanceof PandaHeldItemFeatureRenderer
         || instance instanceof DolphinHeldItemFeatureRenderer
         || instance instanceof ChinaHatFeatureRenderer
         || instance instanceof HaloFeatureRenderer
         || instance instanceof BbCosmeticFeatureRenderer;

      HitColor hitColor = sun$getHitColor();
      if (!excluded
         && hitColor != null
         && hitColor.isEnabled()
         && hurt
         && (hitColor.getTarget().is(hitColor.getBody()) || hitColor.getTarget().is(hitColor.getBoth()))) {
         int packedColor = hitColor.getPackedColor();
         VertexConsumerProvider wrapped = layer -> new HitColorVertexConsumer(vertexConsumerProvider.getBuffer(layer), packedColor);
         original.call(instance, matrices, wrapped, light, state, yawDegrees, pitch);
      } else {
         original.call(instance, matrices, vertexConsumerProvider, light, state, yawDegrees, pitch);
      }
   }

   @Inject(
      method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
      at = @At("HEAD"),
      cancellable = true
   )
   private void sun$cancelDissolvingRender(S state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
      Entity entity = ((EntityRenderStateAddition) state).sun$getEntity();
      KillEffects killEffects = sun$getKillEffects();
      if (killEffects != null && killEffects.isEnabled() && killEffects.isDeadOrDissolving(entity)) {
         ci.cancel();
      }
   }

   @Inject(
      method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V",
      at = @At("TAIL")
   )
   private void sun$hideGhostNameLabel(LivingEntity entity, S state, float tickDelta, CallbackInfo ci) {
      if (entity instanceof FakePlayerEntity ghost && !ghost.isSolidDummy()) {
         state.displayName = null;
      }
   }

   @Inject(method = "hasLabel(Lnet/minecraft/entity/LivingEntity;D)Z", at = @At("HEAD"), cancellable = true)
   private void sun$handleNameLabelVisibility(T entity, double d, CallbackInfoReturnable<Boolean> cir) {
      NameUtility nameUtility = sun$getNameUtility();
      if (nameUtility != null && nameUtility.isEnabled()) {
         if (entity instanceof ArmorStandEntity armorStand) {
            if (nameUtility.getNameTag().isEnabled() && ServerNameTagFilter.shouldSuppress(armorStand)) {
               cir.setReturnValue(false);
               return;
            }
         } else if (entity == MinecraftClient.getInstance().player) {
            if (nameUtility.canShowThirdPersonNick()) {
               if (entity.isSneaky() && d >= 1024.0) {
                  cir.setReturnValue(false);
                  return;
               }
               cir.setReturnValue(MinecraftClient.isHudEnabled());
            }
         } else if (entity instanceof PlayerEntity) {
            if (nameUtility.getNameTag().isEnabled()) {
               if (d >= 4096.0) {
                  cir.setReturnValue(false);
                  return;
               }
               cir.setReturnValue(MinecraftClient.isHudEnabled());
            }
         }
      }
   }

   @Inject(method = "shouldRenderFeatures", at = @At("HEAD"), cancellable = true)
   private void sun$adaptiveFeaturesLod(S state, CallbackInfoReturnable<Boolean> cir) {
      SmartCull smartCull = sun$getSmartCull();
      Optimizer optimizer = sun$getOptimizer();
      boolean isAdaptive = (smartCull != null && smartCull.isEnabled() && smartCull.getAdaptiveMobLod().isEnabled())
              || (optimizer != null && optimizer.isEnabled() && optimizer.getAdaptiveMobLod().isEnabled());

      if (isAdaptive) {
         Entity entity = ((EntityRenderStateAddition) state).sun$getEntity();
         if (entity != null && !(entity instanceof PlayerEntity)) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.gameRenderer != null && mc.gameRenderer.getCamera() != null) {
               double distSq = entity.squaredDistanceTo(mc.gameRenderer.getCamera().getPos());
               if (smartCull != null && smartCull.isEnabled()) {
                  if (smartCull.shouldSimplifyMob(distSq)) {
                     cir.setReturnValue(false);
                  }
               } else if (distSq > 40.0 * 40.0) {
                  cir.setReturnValue(false);
               }
            }
         }
      }
   }
}