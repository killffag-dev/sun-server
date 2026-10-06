package naryn.sun.utility.game.cursor;

import org.lwjgl.glfw.GLFW;

/**
 * Флаг + управление режимом "курсор свободен поверх игры" (Ctrl+Shift, только
 * пока играет музыка — см. MusicModule). Никакого отдельного модуля/карточки
 * в ClickGUI — это чисто служебное состояние.
 */
public final class FreeCursorState {
   private static volatile boolean active = false;

   public static boolean isActive() {
      return active;
   }

   public static void activate(long windowHandle) {
      active = true;
      GLFW.glfwSetInputMode(windowHandle, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
   }

   public static void deactivate(long windowHandle) {
      active = false;
      GLFW.glfwSetInputMode(windowHandle, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
   }

   private FreeCursorState() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}