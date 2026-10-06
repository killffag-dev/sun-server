package naryn.sun.mixin.minecraft.render;

import naryn.sun.Sun;
import naryn.sun.systems.modules.modules.visuals.ItemPhysics;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.ItemEntityRenderer;
import net.minecraft.client.render.entity.state.ItemEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ItemEntityRenderer.class)
public class ItemEntityRendererMixin {

    // Компенсация зазора для 3D-блоков: 1 пиксель = 1/16 блока
    private static final float GROUND_OFFSET = -0.0625F;

    // Подъём для 2D-предметов: 0.5 пикселя = 0.5/16 блока
    private static final float ITEM_LIFT = 0.03125F;

    @Redirect(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/util/math/MatrixStack;translate(FFF)V",
            ordinal = 0
        )
    )
    private void sun$noBobTranslate(MatrixStack matrices, float x, float y, float z,
                                          ItemEntityRenderState state, MatrixStack matrices2,
                                          VertexConsumerProvider vertexConsumers, int light) {
        if (!getItemPhysics().isEnabled()) {
            matrices.translate(x, y, z);
            return;
        }

        boolean is3d = state.itemRenderState.hasDepth();
        if (is3d) {
            matrices.translate(0.0F, GROUND_OFFSET, 0.0F);
        } else {
            matrices.translate(0.0F, ITEM_LIFT, 0.0F);
        }
    }

    @Redirect(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/util/math/MatrixStack;multiply(Lorg/joml/Quaternionf;)V",
            ordinal = 0
        )
    )
    private void sun$noSpinRotate(MatrixStack matrices, Quaternionf rotation,
                                        ItemEntityRenderState state, MatrixStack matrices2,
                                        VertexConsumerProvider vertexConsumers, int light) {
        if (!getItemPhysics().isEnabled()) {
            matrices.multiply(rotation);
            return;
        }

        boolean is3d = state.itemRenderState.hasDepth();
        if (is3d) {
            // 3D: поворот не трогаем — родная ориентация модели уже правильная
            return;
        }
        // 2D: фиксированный поворот на 90° по X
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F));
    }

    private static ItemPhysics getItemPhysics() {
        return Sun.getInstance().getModuleManager().getModule(ItemPhysics.class);
    }
}