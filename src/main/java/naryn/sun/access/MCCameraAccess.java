package naryn.sun.access;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;

/**
 * Тонкий слой доступа к текущей камере рендера (mc.gameRenderer.getCamera()).
 *
 * Стандарт пакета access: методы никогда не возвращают null,
 * при отсутствии камеры/рендерера отдаются безопасные дефолты.
 */
public final class MCCameraAccess {

   public static Camera get() {
      GameRenderer renderer = MinecraftClient.getInstance().gameRenderer;
      return renderer != null ? renderer.getCamera() : null;
   }

   public static Vec3d getPos() {
      Camera camera = get();
      return camera != null ? camera.getPos() : Vec3d.ZERO;
   }

   public static Quaternionf getRotation() {
      Camera camera = get();
      return camera != null ? camera.getRotation() : new Quaternionf();
   }

   private MCCameraAccess() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}