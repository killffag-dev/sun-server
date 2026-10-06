package naryn.sun.systems.event.impl.render;

import lombok.Generated;
import naryn.sun.systems.event.Event;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

public class Render3DEvent extends Event {
   public static final Render3DEvent INSTANCE = new Render3DEvent();

   private MatrixStack matrices;
   private Matrix4f positionMatrix;
   private Matrix4f projectionMatrix;
   private Camera camera;
   private float tickDelta;

   public Render3DEvent() {
   }

   @Generated
   public Render3DEvent(MatrixStack matrices, Matrix4f positionMatrix, Matrix4f projectionMatrix, Camera camera, float tickDelta) {
      this.set(matrices, positionMatrix, projectionMatrix, camera, tickDelta);
   }

   public Render3DEvent set(MatrixStack matrices, Matrix4f positionMatrix, Matrix4f projectionMatrix, Camera camera, float tickDelta) {
      this.matrices = matrices;
      this.positionMatrix = positionMatrix;
      this.projectionMatrix = projectionMatrix;
      this.camera = camera;
      this.tickDelta = tickDelta;
      return this;
   }

   @Generated
   public MatrixStack getMatrices() {
      return this.matrices;
   }

   @Generated
   public Matrix4f getPositionMatrix() {
      return this.positionMatrix;
   }

   @Generated
   public Matrix4f getProjectionMatrix() {
      return this.projectionMatrix;
   }

   @Generated
   public Camera getCamera() {
      return this.camera;
   }

   @Generated
   public float getTickDelta() {
      return this.tickDelta;
   }
}
