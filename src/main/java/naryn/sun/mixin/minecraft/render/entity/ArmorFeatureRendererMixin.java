package naryn.sun.mixin.minecraft.render.entity;

import naryn.sun.Sun;
import naryn.sun.systems.modules.modules.visuals.HitColor;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ArmorFeatureRenderer.class)
public abstract class ArmorFeatureRendererMixin<S extends BipedEntityRenderState, M extends BipedEntityModel<S>, A extends BipedEntityModel<S>> {

   // Ставим флаг "сейчас рисуем броню, сущность в hurt-состоянии" ДО того как render()
   // пройдётся по CHEST/LEGS/FEET/HEAD (каждый слот внутри вызывает renderArmor -> equipmentRenderer.render).
   @Inject(
      method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/state/BipedEntityRenderState;FF)V",
      at = @At("HEAD")
   )
   private void sun$markHurtArmor(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, S state, float limbAngle, float limbDistance, CallbackInfo ci) {
      Sun.getInstance().getModuleManager().getModule(HitColor.class).setHurtArmor(state.hurt);
   }

   // Гасим флаг сразу после того, как ВСЯ броня сущности отрисована — чтобы он не "утёк"
   // в следующий FeatureRenderer в этом же кадре (например ElytraFeatureRenderer тоже
   // дёргает тот же EquipmentRenderer.render()).
   @Inject(
      method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/state/BipedEntityRenderState;FF)V",
      at = @At("TAIL")
   )
   private void sun$unmarkHurtArmor(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, S state, float limbAngle, float limbDistance, CallbackInfo ci) {
      Sun.getInstance().getModuleManager().getModule(HitColor.class).setHurtArmor(false);
   }
}