package naryn.sun.mixin.minecraft.client;

import naryn.sun.Sun;
import naryn.sun.mixin.accessors.MouseAccessor;
import naryn.sun.systems.event.impl.window.MouseEvent;
import naryn.sun.systems.event.impl.window.MouseScrollEvent;
import naryn.sun.systems.modules.modules.utility.Freelook;
import naryn.sun.systems.modules.modules.utility.Zoom;
import naryn.sun.utility.game.cursor.CursorType;
import naryn.sun.utility.game.cursor.CursorUtility;
import naryn.sun.utility.game.cursor.FreeCursorState;
import naryn.sun.utility.interfaces.IMinecraft;
import net.minecraft.client.Mouse;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public abstract class MouseMixin implements IMinecraft {

   // Версия-чувствительные приватные поля Mouse (cursorDeltaX/Y, x, y) вынесены
   // в MouseAccessor (Version Adapter, Этап 3) — при смене маппингов между
   // версиями MC править нужно будет только там, а не здесь.
   // cursorLocked сюда намеренно не включён — этим классом не используется.

   @Unique
   private double sun$lastX;

   @Unique
   private double sun$lastY;

   @Inject(method = "tick()V", at = @At("RETURN"))
   private void tick(CallbackInfo ci) {
      if (CursorUtility.getCurrentType() != CursorUtility.getPrev()) {
         GLFW.glfwSetCursor(mc.getWindow().getHandle(), CursorUtility.getCurrentType().getCode());
      }
      CursorUtility.setPrev(CursorUtility.getCurrentType());
      CursorUtility.set(CursorType.DEFAULT);
   }

   @Inject(method = "onMouseButton", at = @At("HEAD"), cancellable = true)
   private void onMouseButton(long window, int button, int action, int mods, CallbackInfo ci) {
      if (action == 1 || action == 0) {
         Sun.getInstance().getEventManager().triggerEvent(new MouseEvent(button, action));
      }

      // Пока свободный курсор активен (Ctrl+Shift) и никакой реальный Screen не
      // открыт — не даём клику дойти до ванильной обработки кейбиндов (атака/
      // использование). Наш MouseEvent модули (например MusicModule) уже
      // получили клик строчкой выше.
      if (FreeCursorState.isActive() && mc.currentScreen == null) {
         ci.cancel();
      }
   }

   @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
   private void onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
      if (vertical != 0.0) {
         Sun.getInstance().getEventManager().triggerEvent(new MouseScrollEvent(vertical));

         Zoom zoom = Sun.getInstance().getModuleManager().getModule(Zoom.class);
         if (zoom != null && zoom.isEnabled()) {
            ci.cancel();
         }
      }
   }

   @Inject(method = "onCursorPos", at = @At("HEAD"), cancellable = true)
   private void onCursorPos(long window, double x, double y, CallbackInfo ci) {
      Freelook freelook = Sun.getInstance().getModuleManager().getModule(Freelook.class);
      MouseAccessor accessor = (MouseAccessor) (Object) this;
      if (freelook != null && freelook.isEnabled() && mc.currentScreen == null) {
         double deltaX = x - this.sun$lastX;
         double deltaY = y - this.sun$lastY;
         freelook.onMouseMove(deltaX, deltaY);
         this.sun$lastX = x;
         this.sun$lastY = y;
         accessor.setX(x);
         accessor.setY(y);
         ci.cancel();
      } else if (FreeCursorState.isActive() && mc.currentScreen == null) {
         // Свободный курсор поверх игры: сами обновляем позицию (нужна для
         // наведения на кнопки Music и т.п.), но НЕ даём ванильному коду
         // применить это движение к повороту камеры игрока.
         this.sun$lastX = x;
         this.sun$lastY = y;
         accessor.setX(x);
         accessor.setY(y);
         accessor.setCursorDeltaX(0.0);
         accessor.setCursorDeltaY(0.0);
         ci.cancel();
      } else {
         this.sun$lastX = x;
         this.sun$lastY = y;
      }
   }
}