package naryn.sun.mixin.minecraft.client.render.entity;

import naryn.sun.Sun;
import naryn.sun.systems.modules.modules.optimization.NoRender;
import naryn.sun.systems.modules.modules.optimization.Optimizer;
import naryn.sun.systems.modules.modules.visuals.Hitbox;
import naryn.sun.systems.modules.modules.visuals.KillEffects;
import naryn.sun.systems.modules.modules.visuals.nameutility.ServerNameTagFilter;
import naryn.sun.utility.culling.OcclusionCuller;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.WorldView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {

    @Shadow
    public Camera camera;

    @Inject(
        method = "render(Lnet/minecraft/entity/Entity;DDDFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/EntityRenderer;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private <E extends Entity, S extends EntityRenderState> void sun$cullEntity(
        E entity,
        double x,
        double y,
        double z,
        float tickDelta,
        MatrixStack matrices,
        VertexConsumerProvider vertexConsumers,
        int light,
        EntityRenderer<? super E, S> renderer,
        CallbackInfo ci
    ) {
        // Подавление серверных сущностей-голограмм над игроками при активных неймтегах NameUtility
        if (ServerNameTagFilter.shouldSuppress(entity)) {
            ci.cancel();
            return;
        }

        // NoRender checks
        NoRender noRender = Sun.getInstance().getModuleManager().getModule(NoRender.class);
        if (noRender != null && noRender.isEnabled()) {
            if (noRender.getPlayers().isSelected() && entity instanceof PlayerEntity) {
                ci.cancel();
                return;
            }
            if (noRender.getMobs().isSelected() && entity instanceof MobEntity) {
                ci.cancel();
                return;
            }
            if (noRender.getItems().isSelected() && entity instanceof ItemEntity) {
                ci.cancel();
                return;
            }
            if (noRender.getArmorStands().isSelected() && entity instanceof ArmorStandEntity) {
                ci.cancel();
                return;
            }
            if (noRender.getFallingBlocks().isSelected() && entity instanceof FallingBlockEntity) {
                ci.cancel();
                return;
            }
            if (noRender.getTnt().isSelected() && entity instanceof TntEntity) {
                ci.cancel();
                return;
            }
        }

        Optimizer optimizer = Sun.getInstance().getModuleManager().getModule(Optimizer.class);
        if (optimizer != null && optimizer.isEnabled()) {
            if (optimizer.getHideArmorStands().isEnabled() && entity instanceof ArmorStandEntity) {
                ci.cancel();
                return;
            }
            if (optimizer.getDisableFireworks().isEnabled() && entity instanceof FireworkRocketEntity) {
                ci.cancel();
                return;
            }

            double distSq = x * x + y * y + z * z;
            if (entity instanceof PlayerEntity) {
                if (optimizer.getPlayerDistanceCulling().isEnabled()) {
                    float maxDist = optimizer.getPlayerCullDistance().getCurrentValue();
                    if (distSq > maxDist * maxDist) {
                        ci.cancel();
                        return;
                    }
                }
            } else if (optimizer.getEntityDistanceCulling().isEnabled()) {
                float maxDist = optimizer.getCullDistance().getCurrentValue();
                if (distSq > maxDist * maxDist) {
                    ci.cancel();
                    return;
                }
            }

            if (optimizer.getEntityOcclusionCulling().isEnabled() && this.camera != null) {
                if (!net.minecraft.client.MinecraftClient.getInstance().hasOutline(entity) && OcclusionCuller.isEntityOccluded(entity, this.camera)) {
                    ci.cancel();
                    return;
                }
            }
        }
    }

    @Inject(method = "renderShadow", at = @At("HEAD"), cancellable = true)
    private static void sun$cancelShadow(MatrixStack matrices, VertexConsumerProvider vertexConsumers, EntityRenderState state, float opacity, float tickDelta, WorldView world, float radius, CallbackInfo ci) {
        Optimizer optimizer = Sun.getInstance().getModuleManager().getModule(Optimizer.class);
        if (optimizer != null && optimizer.isEnabled() && !optimizer.getEntityShadows().isEnabled()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderHitbox", at = @At("HEAD"), cancellable = true)
    private static void sun$cancelVanillaHitbox(
        MatrixStack matrices,
        VertexConsumer vertices,
        Entity entity,
        float tickDelta,
        float red,
        float green,
        float blue,
        CallbackInfo ci
    ) {
        KillEffects killEffects = Sun.getInstance().getModuleManager().getModule(KillEffects.class);
        if (killEffects != null && killEffects.isEnabled() && killEffects.isDeadOrDissolving(entity)) {
            ci.cancel();
            return;
        }
        Hitbox hitbox = Sun.getInstance().getModuleManager().getModule(Hitbox.class);
        if (hitbox != null && hitbox.isEnabled()) {
            ci.cancel();
        }
    }
}
