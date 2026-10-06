package naryn.sun.systems.event.impl.render;

import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

/**
 * Отдельное от Render3DEvent событие для фоновых full-screen эффектов (сейчас — только
 * Sky), которые должны рисоваться ДО любых сущностей за кадр, а не после всего мира
 * (как Render3DEvent на WorldRenderEvents.END).
 */
public class Render3DBackgroundEvent extends Render3DEvent {
   public static final Render3DBackgroundEvent INSTANCE = new Render3DBackgroundEvent();

   public Render3DBackgroundEvent() {
   }

   public Render3DBackgroundEvent(MatrixStack matrices, Matrix4f positionMatrix, Matrix4f projectionMatrix, Camera camera, float tickDelta) {
      super(matrices, positionMatrix, projectionMatrix, camera, tickDelta);
   }

   @Override
   public Render3DBackgroundEvent set(MatrixStack matrices, Matrix4f positionMatrix, Matrix4f projectionMatrix, Camera camera, float tickDelta) {
      super.set(matrices, positionMatrix, projectionMatrix, camera, tickDelta);
      return this;
   }
}