package naryn.sun.mixin.minecraft.render.entity;

import naryn.sun.systems.modules.modules.visuals.headcosmetics.ChinaHatFeatureRenderer;
import naryn.sun.systems.modules.modules.visuals.headcosmetics.HaloFeatureRenderer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import naryn.sun.utility.game.FakePlayerEntity;
import naryn.sun.utility.mixins.EntityRenderStateAddition;
import naryn.sun.Sun;
import net.minecraft.entity.Entity;
import naryn.sun.systems.modules.modules.visuals.NameUtility;
import naryn.sun.systems.modules.modules.visuals.nameutility.NameTagRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin
    extends LivingEntityRenderer<AbstractClientPlayerEntity, PlayerEntityRenderState, PlayerEntityModel> {

    // Этот конструктор никогда реально не выполняется (Mixin сливает @Inject в настоящий конструктор цели),
    // он нужен только чтобы класс скомпилировался как валидный наследник LivingEntityRenderer.
    private PlayerEntityRendererMixin(EntityRendererFactory.Context ctx, PlayerEntityModel model, float shadowRadius) {
        super(ctx, model, shadowRadius);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void sun$registerHeadCosmetics(EntityRendererFactory.Context ctx, boolean slim, CallbackInfo ci) {
        this.addFeature(new ChinaHatFeatureRenderer((FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel>) this));
        this.addFeature(new HaloFeatureRenderer((FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel>) this));
    }

    @Inject(method = "updateRenderState(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;F)V", at = @At("TAIL"))
    private void sun$applyTrailsGhostState(AbstractClientPlayerEntity entity, PlayerEntityRenderState state, float tickDelta, CallbackInfo ci) {
        if (entity instanceof FakePlayerEntity ghost && !ghost.isSolidDummy()) {
            state.bodyYaw = ghost.capturedBodyYaw;
            state.yawDegrees = MathHelper.wrapDegrees(ghost.capturedHeadYaw - ghost.capturedBodyYaw);
            state.pitch = ghost.capturedPitch;
            state.limbFrequency = ghost.capturedLimbFrequency;
            state.limbAmplitudeMultiplier = ghost.capturedLimbAmplitude;
            state.handSwingProgress = ghost.capturedHandSwingProgress;
            state.pose = ghost.capturedPose;
            state.isInSneakingPose = ghost.capturedSneaking;
            state.isGliding = ghost.capturedGliding;
            state.isSwimming = ghost.capturedSwimming;
            state.leaningPitch = ghost.capturedLeaningPitch;
            state.leftArmPose = ghost.capturedLeftArmPose;
            state.rightArmPose = ghost.capturedRightArmPose;
            state.hatVisible = ghost.capturedHatVisible;
            state.jacketVisible = ghost.capturedJacketVisible;
            state.leftPantsLegVisible = ghost.capturedLeftPantsVisible;
            state.rightPantsLegVisible = ghost.capturedRightPantsVisible;
            state.leftSleeveVisible = ghost.capturedLeftSleeveVisible;
            state.rightSleeveVisible = ghost.capturedRightSleeveVisible;
            state.capeVisible = ghost.capturedCapeVisible;
            state.playerName = null;
        }

        NameUtility nameUtility = Sun.getInstance().getModuleManager().getModule(NameUtility.class);
        if (nameUtility != null && nameUtility.isEnabled() && nameUtility.getNameTag().isEnabled()) {
            if (state.nameLabelPos == null) {
                state.nameLabelPos = new Vec3d(0.0, entity.getHeight() + 0.5, 0.0);
            }
            if (state.name == null || state.name.isEmpty()) {
                state.name = entity.getGameProfile().getName();
            }
        }
    }

    @Inject(method = "shouldRenderFeatures(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;)Z", at = @At("HEAD"), cancellable = true)
    private void sun$noFeaturesForTrailsGhost(PlayerEntityRenderState state, CallbackInfoReturnable<Boolean> cir) {
        Entity entity = ((EntityRenderStateAddition) state).sun$getEntity();
        if (entity instanceof FakePlayerEntity ghost && !ghost.isSolidDummy()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(
        method = "renderLabelIfPresent(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;Lnet/minecraft/text/Text;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void sun$renderCustomNameTag(PlayerEntityRenderState state, Text text, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        NameUtility nameUtility = Sun.getInstance().getModuleManager().getModule(NameUtility.class);
        if (nameUtility != null && nameUtility.isEnabled() && nameUtility.getNameTag().isEnabled()) {
            if (NameTagRenderer.render(nameUtility, state, text, matrices, vertexConsumers, light, this.dispatcher)) {
                ci.cancel();
            }
        }
    }
}