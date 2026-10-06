package naryn.sun.mixin.minecraft.render.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import naryn.sun.Sun;
import naryn.sun.systems.modules.modules.visuals.HitColor;
import net.minecraft.client.model.Model;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.equipment.EquipmentRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EquipmentRenderer.class)
public abstract class EquipmentRendererMixin {

   // Именно этот render(...) с (matrices, vertexConsumer, light, overlay, color) — 5 аргументов,
   // это ветка ОБЫЧНОГО слоя брони (getDyeColor -> j). У trim'а другой render(...) с 4 аргументами
   // (без color), поэтому WrapOperation по этому дескриптору его физически не заденет.
   @WrapOperation(
      method = "render(Lnet/minecraft/client/render/entity/equipment/EquipmentModel$LayerType;Lnet/minecraft/registry/RegistryKey;Lnet/minecraft/client/model/Model;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/util/Identifier;)V",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/model/Model;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;III)V"
      )
   )
   private void sun$changeArmorColor(
      Model instance,
      MatrixStack matrices,
      VertexConsumer vertexConsumer,
      int light,
      int overlay,
      int color,
      Operation<Void> original
   ) {
      HitColor hitColor = Sun.getInstance().getModuleManager().getModule(HitColor.class);
      if (hitColor.isEnabled()
         && hitColor.isHurtArmor()
         && (hitColor.getTarget().is(hitColor.getArmor()) || hitColor.getTarget().is(hitColor.getBoth()))) {
         color = hitColor.getPackedColor();
      }
      original.call(instance, matrices, vertexConsumer, light, overlay, color);
   }
}