package naryn.sun.mixin.minecraft.client.gui.screen;

import naryn.sun.Sun;
import naryn.sun.framework.base.CustomDrawContext;
import naryn.sun.systems.event.impl.render.ScreenRenderEvent;
import naryn.sun.systems.event.impl.window.ContainerClickEvent;
import naryn.sun.systems.event.impl.window.ContainerReleaseEvent;
import naryn.sun.utility.interfaces.IMinecraft;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin implements IMinecraft {
   @Inject(method = "render", at = @At("TAIL"))
   private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      CustomDrawContext customDrawContext = CustomDrawContext.of(context);
      Sun.getInstance().getEventManager().triggerEvent(ScreenRenderEvent.INSTANCE.set(customDrawContext, delta));
   }

   @Inject(method = "mouseClicked", at = @At("HEAD"))
   private void onMouseClick(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
      Sun.getInstance().getEventManager().triggerEvent(new ContainerClickEvent((float)mouseX, (float)mouseY, button));
   }

   @Inject(method = "mouseReleased", at = @At("HEAD"))
   public void mouseReleased(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
      Sun.getInstance().getEventManager().triggerEvent(new ContainerReleaseEvent((float)mouseX, (float)mouseY, button));
   }

   @org.spongepowered.asm.mixin.Unique
   private static naryn.sun.systems.modules.modules.utility.BlockSlot sun$blockSlot;

   @org.spongepowered.asm.mixin.Unique
   private static naryn.sun.systems.modules.modules.utility.BlockSlot sun$getBlockSlot() {
      if (sun$blockSlot == null && Sun.getInstance() != null && Sun.getInstance().getModuleManager() != null) {
         sun$blockSlot = Sun.getInstance().getModuleManager().getModule(naryn.sun.systems.modules.modules.utility.BlockSlot.class);
      }
      return sun$blockSlot;
   }

   @Inject(method = "drawSlot", at = @At("HEAD"))
   private void onDrawSlotHead(DrawContext context, net.minecraft.screen.slot.Slot slot, CallbackInfo ci) {
      naryn.sun.systems.modules.modules.utility.BlockSlot blockSlot = sun$getBlockSlot();
      if (blockSlot != null && blockSlot.isEnabled() && blockSlot.isSlotLocked(slot)) {
         context.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, 0x80505050);
      }
   }

   @Inject(method = "drawSlot", at = @At("TAIL"))
   private void onDrawSlotTail(DrawContext context, net.minecraft.screen.slot.Slot slot, CallbackInfo ci) {
      naryn.sun.systems.modules.modules.utility.BlockSlot blockSlot = sun$getBlockSlot();
      if (blockSlot != null && blockSlot.isEnabled() && blockSlot.isSlotLocked(slot)) {
         context.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, 0x25303030);
      }
   }

   @org.spongepowered.asm.mixin.Shadow
   @org.jetbrains.annotations.Nullable
   protected net.minecraft.screen.slot.Slot getSlotAt(double x, double y) {
      return null;
   }

   @org.spongepowered.asm.mixin.Shadow
   @org.jetbrains.annotations.Nullable
   protected net.minecraft.screen.slot.Slot focusedSlot;

   @Inject(method = "handleHotbarKeyPressed", at = @At("HEAD"), cancellable = true)
   private void onHandleHotbarKeyPressed(int keyCode, int scanCode, CallbackInfoReturnable<Boolean> cir) {
      naryn.sun.systems.modules.modules.utility.BlockSlot blockSlot = sun$getBlockSlot();
      if (blockSlot != null && blockSlot.isEnabled() && this.focusedSlot != null) {
         if (mc.options.swapHandsKey.matchesKey(keyCode, scanCode)) {
            if (blockSlot.isSlotIndexLocked(40) || blockSlot.isSlotLocked(this.focusedSlot)) {
               cir.setReturnValue(true);
               return;
            }
         }

         for (int i = 0; i < 9; i++) {
            if (mc.options.hotbarKeys[i].matchesKey(keyCode, scanCode)) {
               if (blockSlot.isSlotIndexLocked(i) || blockSlot.isSlotLocked(this.focusedSlot)) {
                  cir.setReturnValue(true);
                  return;
               }
            }
         }
      }
   }

   @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
   private void onMouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY, CallbackInfoReturnable<Boolean> cir) {
      naryn.sun.systems.modules.modules.utility.BlockSlot blockSlot = sun$getBlockSlot();
      if (blockSlot != null && blockSlot.isEnabled()) {
         net.minecraft.screen.slot.Slot slot = this.getSlotAt(mouseX, mouseY);
         if (slot != null && blockSlot.isSlotLocked(slot)) {
            cir.setReturnValue(true);
         }
      }
   }

   @Inject(method = "onMouseClick(Lnet/minecraft/screen/slot/Slot;IILnet/minecraft/screen/slot/SlotActionType;)V", at = @At("HEAD"), cancellable = true)
   private void onHandledScreenSlotClick(net.minecraft.screen.slot.Slot slot, int slotId, int button, net.minecraft.screen.slot.SlotActionType actionType, CallbackInfo ci) {
      naryn.sun.systems.modules.modules.utility.BlockSlot blockSlot = sun$getBlockSlot();
      if (blockSlot != null && blockSlot.isEnabled() && blockSlot.shouldBlockSlotClick(slot, slotId, button, actionType)) {
         ci.cancel();
      }
   }
}
