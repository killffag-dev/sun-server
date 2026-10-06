package naryn.sun.mixin.minecraft.client;

import naryn.sun.Sun;
import naryn.sun.systems.event.impl.window.KeyPressEvent;
import naryn.sun.utility.game.cursor.FreeCursorState;
import naryn.sun.utility.interfaces.IMinecraft;
import net.minecraft.client.Keyboard;
import net.minecraft.client.gui.screen.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public class KeyboardMixin implements IMinecraft {
   @Inject(method = "onKey", at = @At("HEAD"), cancellable = true)
   public void triggerKeyEvent(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
      if (key != -1) {
         KeyPressEvent event = new KeyPressEvent(action, key);
         Sun.getInstance().getEventManager().triggerEvent(event);
         if (event.isCancelled()) {
            ci.cancel();
            return;
         }

         // Пока активен режим свободного курсора (Ctrl+Shift, только во время
         // проигрывания музыки — см. MusicModule) — повторный ESC просто
         // выключает режим, а не открывает ванильную паузу.
         if (key == 256 && action == 1 && FreeCursorState.isActive()) {
            if (mc.getWindow() != null) {
               FreeCursorState.deactivate(mc.getWindow().getHandle());
            }
            ci.cancel();
            return;
         }

         if (mc.currentScreen == null) {
            if (key == 46 && action == 1) {
               mc.setScreen(new ChatScreen(""));
            }
         }
      }
   }
}