package naryn.sun.mixin.minecraft.client.render.block.entity;

import naryn.sun.Sun;
import naryn.sun.systems.modules.modules.optimization.NoRender;
import naryn.sun.systems.modules.modules.optimization.Optimizer;
import naryn.sun.utility.culling.OcclusionCuller;
import net.minecraft.block.entity.BannerBlockEntity;
import net.minecraft.block.entity.BellBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntityRenderDispatcher.class)
public class BlockEntityRenderDispatcherMixin {

    @Shadow
    public Camera camera;

    @Shadow
    public World world;

    @Inject(
        method = "render(Lnet/minecraft/block/entity/BlockEntity;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private <E extends BlockEntity> void sun$cullBlockEntity(
        E blockEntity,
        float tickDelta,
        MatrixStack matrices,
        VertexConsumerProvider vertexConsumers,
        CallbackInfo ci
    ) {
        if (blockEntity == null || this.camera == null || this.world == null) {
            return;
        }

        // NoRender block entity checks
        NoRender noRender = Sun.getInstance().getModuleManager().getModule(NoRender.class);
        if (noRender != null && noRender.isEnabled()) {
            if (noRender.getChests().isSelected() && blockEntity instanceof ChestBlockEntity) {
                ci.cancel();
                return;
            }
            if (noRender.getEnderChests().isSelected() && blockEntity instanceof EnderChestBlockEntity) {
                ci.cancel();
                return;
            }
            if (noRender.getShulkers().isSelected() && blockEntity instanceof ShulkerBoxBlockEntity) {
                ci.cancel();
                return;
            }
            if (noRender.getSigns().isSelected() && blockEntity instanceof SignBlockEntity) {
                ci.cancel();
                return;
            }
            if (noRender.getSpawners().isSelected() && blockEntity instanceof MobSpawnerBlockEntity) {
                ci.cancel();
                return;
            }
            if (noRender.getBanners().isSelected() && blockEntity instanceof BannerBlockEntity) {
                ci.cancel();
                return;
            }
            if (noRender.getBells().isSelected() && blockEntity instanceof BellBlockEntity) {
                ci.cancel();
                return;
            }
        }

        Optimizer optimizer = Sun.getInstance().getModuleManager().getModule(Optimizer.class);
        if (optimizer != null && optimizer.isEnabled()) {
            BlockPos pos = blockEntity.getPos();

            // Дистанционное отсечение сундуков/табличек/спавнеров
            if (optimizer.getBlockEntityDistanceCulling().isEnabled()) {
                Vec3d camPos = this.camera.getPos();
                double dx = (pos.getX() + 0.5) - camPos.x;
                double dy = (pos.getY() + 0.5) - camPos.y;
                double dz = (pos.getZ() + 0.5) - camPos.z;
                double distSq = dx * dx + dy * dy + dz * dz;
                float maxDist = optimizer.getBlockEntityCullDistance().getCurrentValue();
                if (distSq > maxDist * maxDist) {
                    ci.cancel();
                    return;
                }
            }

            // Occlusion culling (отсечение блоков за стенами / под землей)
            if (optimizer.getBlockEntityOcclusionCulling().isEnabled()) {
                if (OcclusionCuller.isBlockEntityOccluded(pos, this.camera, this.world)) {
                    ci.cancel();
                }
            }
        }
    }
}
