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
      Sun.getInstance().getEventManager().triggerEvent(new ScreenRenderEvent(customDrawContext, delta));
   }

   @Inject(method = "mouseClicked", at = @At("HEAD"))
   private void onMouseClick(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
      Sun.getInstance().getEventManager().triggerEvent(new ContainerClickEvent((float)mouseX, (float)mouseY, button));
   }

   @Inject(method = "mouseReleased", at = @At("HEAD"))
   public void mouseReleased(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
      Sun.getInstance().getEventManager().triggerEvent(new ContainerReleaseEvent((float)mouseX, (float)mouseY, button));
   }
}
