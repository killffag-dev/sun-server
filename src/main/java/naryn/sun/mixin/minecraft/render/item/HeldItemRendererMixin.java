package naryn.sun.mixin.minecraft.render.item;

import naryn.sun.Sun;
import naryn.sun.systems.event.impl.render.HandRenderEvent;
import naryn.sun.systems.modules.constructions.viewmodel.ViewModelMeshTracker;
import naryn.sun.systems.modules.constructions.viewmodel.ViewModelScreen;
import naryn.sun.ui.menu.layout.ViewModelEditor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {
   @Shadow
   @Final
   private ItemRenderer itemRenderer;

   @Shadow
   protected abstract void applyEatOrDrinkTransformation(MatrixStack var1, float var2, Arm var3, ItemStack var4,
         PlayerEntity var5);

   @Shadow
   protected abstract void applyBrushTransformation(MatrixStack var1, float var2, Arm var3, ItemStack var4,
         PlayerEntity var5, float var6);

   @ModifyVariable(
      method = "renderFirstPersonItem",
      at = @At("HEAD"),
      argsOnly = true,
      ordinal = 0
   )
   private VertexConsumerProvider sun$wrapVertexConsumers(
         VertexConsumerProvider vertexConsumers,
         AbstractClientPlayerEntity player,
         float tickDelta,
         float pitch,
         Hand hand) {
      if (MinecraftClient.getInstance().currentScreen instanceof ViewModelScreen) {
         ViewModelEditor.HandElement element = (hand == Hand.MAIN_HAND)
               ? ViewModelEditor.HandElement.MAIN_HAND
               : ViewModelEditor.HandElement.OFF_HAND;
         return ViewModelMeshTracker.wrap(vertexConsumers, element);
      }
      return vertexConsumers;
   }

   @Inject(method = "renderFirstPersonItem", at = @At("HEAD"), cancellable = true)
   private void onRenderFirstPersonItem(
         AbstractClientPlayerEntity player,
         float tickDelta,
         float pitch,
         Hand hand,
         float swingProgress,
         ItemStack item,
         float equipProgress,
         MatrixStack matrices,
         VertexConsumerProvider vertexConsumers,
         int light,
         CallbackInfo ci) {
      boolean isMainHand = hand == Hand.MAIN_HAND;
      Arm arm = isMainHand ? player.getMainArm() : player.getMainArm().getOpposite();
      boolean isRightArm = arm == Arm.RIGHT;
      matrices.push();
      HandRenderEvent event = HandRenderEvent.INSTANCE.set(arm, swingProgress, item, equipProgress, matrices);
      Sun.getInstance().getEventManager().triggerEvent(event);

      if (event.isCancelled()) {
         ci.cancel();
         float f = -0.4F * MathHelper.sin(MathHelper.sqrt(0.0F) * (float) Math.PI);
         float g = 0.2F * MathHelper.sin(MathHelper.sqrt(0.0F) * (float) (Math.PI * 2));
         float h = -0.2F * MathHelper.sin(0.0F);
         matrices.translate((arm == Arm.RIGHT ? 1 : -1) * f, g, h);
         int i = arm == Arm.RIGHT ? 1 : -1;
         matrices.translate(i * 0.56F, -0.52F, -0.72F);

         if (!item.isEmpty()) {
            VertexConsumerProvider activeConsumers = vertexConsumers;
            if (MinecraftClient.getInstance().currentScreen instanceof ViewModelScreen) {
               ViewModelEditor.HandElement element = isMainHand
                     ? ViewModelEditor.HandElement.MAIN_HAND
                     : ViewModelEditor.HandElement.OFF_HAND;
               activeConsumers = ViewModelMeshTracker.wrap(activeConsumers, element);
            }
            HeldItemRenderer rendererInstance = (HeldItemRenderer) (Object) this;
            rendererInstance.renderItem(
                  player,
                  item,
                  isRightArm ? ModelTransformationMode.FIRST_PERSON_RIGHT_HAND
                        : ModelTransformationMode.FIRST_PERSON_LEFT_HAND,
                  !isRightArm,
                  matrices,
                  activeConsumers,
                  light);
         }

         matrices.pop();
         if (MinecraftClient.getInstance().currentScreen instanceof ViewModelScreen) {
            ViewModelEditor.HandElement element = isMainHand
                  ? ViewModelEditor.HandElement.MAIN_HAND
                  : ViewModelEditor.HandElement.OFF_HAND;
            ViewModelMeshTracker.endTracking(element);
         }
         return;
      }
   }

   @Inject(method = "renderFirstPersonItem", at = @At("RETURN"))
   private void onRenderFirstPersonItemEnd(
         AbstractClientPlayerEntity player,
         float tickDelta,
         float pitch,
         Hand hand,
         float swingProgress,
         ItemStack item,
         float equipProgress,
         MatrixStack matrices,
         VertexConsumerProvider vertexConsumers,
         int light,
         CallbackInfo ci) {
      matrices.pop();
      if (MinecraftClient.getInstance().currentScreen instanceof ViewModelScreen) {
         ViewModelEditor.HandElement element = (hand == Hand.MAIN_HAND)
               ? ViewModelEditor.HandElement.MAIN_HAND
               : ViewModelEditor.HandElement.OFF_HAND;
         ViewModelMeshTracker.endTracking(element);
      }
   }
}
