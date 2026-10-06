package naryn.sun.systems.modules.constructions.viewmodel;

import com.mojang.blaze3d.systems.RenderSystem;
import naryn.sun.ui.menu.layout.ViewModelEditor.HandElement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import org.joml.Matrix4f;

import java.util.Arrays;

public class ViewModelMeshTracker {

   private static final HandTracker MAIN_TRACKER = new HandTracker();
   private static final HandTracker OFF_TRACKER = new HandTracker();

   public static HandTracker getTracker(HandElement element) {
      return element == HandElement.MAIN_HAND ? MAIN_TRACKER : OFF_TRACKER;
   }

   public static VertexConsumerProvider wrap(VertexConsumerProvider original, HandElement element) {
      if (original instanceof TrackingVertexConsumerProvider) {
         return original;
      }
      HandTracker tracker = getTracker(element);
      MinecraftClient mc = MinecraftClient.getInstance();
      float screenW = (float) mc.getWindow().getScaledWidth();
      float screenH = (float) mc.getWindow().getScaledHeight();
      tracker.beginFrame(RenderSystem.getProjectionMatrix(), RenderSystem.getModelViewMatrix(), screenW, screenH);
      return new TrackingVertexConsumerProvider(original, tracker);
   }

   public static void endTracking(HandElement element) {
      getTracker(element).endFrame();
   }

   public static float hitTest(HandElement element, double mouseX, double mouseY) {
      return getTracker(element).hitTest(mouseX, mouseY);
   }

   public static boolean hasData(HandElement element) {
      return getTracker(element).hasData();
   }

   public static void reset() {
      MAIN_TRACKER.reset();
      OFF_TRACKER.reset();
   }

   private static boolean isGlintLayer(RenderLayer layer) {
      if (layer == null) return false;
      String name = layer.toString();
      return name.contains("glint") || name.contains("Glint");
   }

   public static class TrackingVertexConsumerProvider implements VertexConsumerProvider {
      private final VertexConsumerProvider delegate;
      private final HandTracker tracker;

      public TrackingVertexConsumerProvider(VertexConsumerProvider delegate, HandTracker tracker) {
         this.delegate = delegate;
         this.tracker = tracker;
      }

      @Override
      public VertexConsumer getBuffer(RenderLayer layer) {
         VertexConsumer original = this.delegate.getBuffer(layer);
         if (isGlintLayer(layer)) {
            return original;
         }
         return new TrackingVertexConsumer(original, this.tracker);
      }
   }

   public static class TrackingVertexConsumer implements VertexConsumer {
      private final VertexConsumer delegate;
      private final HandTracker tracker;

      public TrackingVertexConsumer(VertexConsumer delegate, HandTracker tracker) {
         this.delegate = delegate;
         this.tracker = tracker;
      }

      @Override
      public VertexConsumer vertex(float x, float y, float z) {
         this.tracker.addVertex(x, y, z);
         this.delegate.vertex(x, y, z);
         return this;
      }

      @Override
      public void vertex(float x, float y, float z, int color, float u, float v, int overlay, int light, float normalX, float normalY, float normalZ) {
         this.tracker.addVertex(x, y, z);
         this.delegate.vertex(x, y, z, color, u, v, overlay, light, normalX, normalY, normalZ);
      }

      @Override
      public VertexConsumer color(int red, int green, int blue, int alpha) {
         this.delegate.color(red, green, blue, alpha);
         return this;
      }

      @Override
      public VertexConsumer texture(float u, float v) {
         this.delegate.texture(u, v);
         return this;
      }

      @Override
      public VertexConsumer overlay(int u, int v) {
         this.delegate.overlay(u, v);
         return this;
      }

      @Override
      public VertexConsumer light(int u, int v) {
         this.delegate.light(u, v);
         return this;
      }

      @Override
      public VertexConsumer normal(float x, float y, float z) {
         this.delegate.normal(x, y, z);
         return this;
      }
   }

   public static class HandTracker {
      private final Matrix4f combinedMatrix = new Matrix4f();
      private float screenW = 1.0F;
      private float screenH = 1.0F;

      private float minX = Float.POSITIVE_INFINITY;
      private float minY = Float.POSITIVE_INFINITY;
      private float maxX = Float.NEGATIVE_INFINITY;
      private float maxY = Float.NEGATIVE_INFINITY;

      private float[] triangles = new float[1024 * 6];
      private float[] triangleDepths = new float[1024];
      private int triangleCount = 0;

      private final float[] quadX = new float[4];
      private final float[] quadY = new float[4];
      private final float[] quadZ = new float[4];
      private int vertexIndex = 0;

      private boolean hasData = false;

      public void beginFrame(Matrix4f projectionMatrix, Matrix4f modelViewMatrix, float screenW, float screenH) {
         this.combinedMatrix.set(projectionMatrix).mul(modelViewMatrix);
         this.screenW = screenW;
         this.screenH = screenH;
         this.triangleCount = 0;
         this.vertexIndex = 0;
         this.minX = Float.POSITIVE_INFINITY;
         this.minY = Float.POSITIVE_INFINITY;
         this.maxX = Float.NEGATIVE_INFINITY;
         this.maxY = Float.NEGATIVE_INFINITY;
         this.hasData = true;
      }

      public void endFrame() {
         this.vertexIndex = 0;
      }

      public void reset() {
         this.triangleCount = 0;
         this.vertexIndex = 0;
         this.hasData = false;
         this.minX = Float.POSITIVE_INFINITY;
         this.minY = Float.POSITIVE_INFINITY;
         this.maxX = Float.NEGATIVE_INFINITY;
         this.maxY = Float.NEGATIVE_INFINITY;
      }

      public boolean hasData() {
         return this.hasData && this.triangleCount > 0;
      }

      public void addVertex(float x, float y, float z) {
         float m00 = this.combinedMatrix.m00(), m10 = this.combinedMatrix.m10(), m20 = this.combinedMatrix.m20(), m30 = this.combinedMatrix.m30();
         float m01 = this.combinedMatrix.m01(), m11 = this.combinedMatrix.m11(), m21 = this.combinedMatrix.m21(), m31 = this.combinedMatrix.m31();
         float m02 = this.combinedMatrix.m02(), m12 = this.combinedMatrix.m12(), m22 = this.combinedMatrix.m22(), m32 = this.combinedMatrix.m32();
         float m03 = this.combinedMatrix.m03(), m13 = this.combinedMatrix.m13(), m23 = this.combinedMatrix.m23(), m33 = this.combinedMatrix.m33();

         float w = m03 * x + m13 * y + m23 * z + m33;
         if (w <= 0.0001F) {
            this.vertexIndex = (this.vertexIndex + 1) % 4;
            return;
         }

         float invW = 1.0F / w;
         float ndcX = (m00 * x + m10 * y + m20 * z + m30) * invW;
         float ndcY = (m01 * x + m11 * y + m21 * z + m31) * invW;
         float ndcZ = (m02 * x + m12 * y + m22 * z + m32) * invW;

         float sx = (ndcX + 1.0F) * 0.5F * this.screenW;
         float sy = (1.0F - ndcY) * 0.5F * this.screenH;

         this.quadX[this.vertexIndex] = sx;
         this.quadY[this.vertexIndex] = sy;
         this.quadZ[this.vertexIndex] = ndcZ;
         this.vertexIndex++;

         if (this.vertexIndex == 4) {
            // Квад разбит на два 2D-треугольника
            addTriangle(
               this.quadX[0], this.quadY[0], this.quadZ[0],
               this.quadX[1], this.quadY[1], this.quadZ[1],
               this.quadX[2], this.quadY[2], this.quadZ[2]
            );
            addTriangle(
               this.quadX[0], this.quadY[0], this.quadZ[0],
               this.quadX[2], this.quadY[2], this.quadZ[2],
               this.quadX[3], this.quadY[3], this.quadZ[3]
            );
            this.vertexIndex = 0;
         }
      }

      private void addTriangle(float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3) {
         if (this.triangleCount * 6 + 6 > this.triangles.length) {
            int newCap = this.triangles.length * 2;
            this.triangles = Arrays.copyOf(this.triangles, newCap);
            this.triangleDepths = Arrays.copyOf(this.triangleDepths, newCap / 6);
         }

         int base = this.triangleCount * 6;
         this.triangles[base] = x1;
         this.triangles[base + 1] = y1;
         this.triangles[base + 2] = x2;
         this.triangles[base + 3] = y2;
         this.triangles[base + 4] = x3;
         this.triangles[base + 5] = y3;
         this.triangleDepths[this.triangleCount] = (z1 + z2 + z3) / 3.0F;
         this.triangleCount++;

         if (x1 < this.minX) this.minX = x1;
         if (x2 < this.minX) this.minX = x2;
         if (x3 < this.minX) this.minX = x3;

         if (x1 > this.maxX) this.maxX = x1;
         if (x2 > this.maxX) this.maxX = x2;
         if (x3 > this.maxX) this.maxX = x3;

         if (y1 < this.minY) this.minY = y1;
         if (y2 < this.minY) this.minY = y2;
         if (y3 < this.minY) this.minY = y3;

         if (y1 > this.maxY) this.maxY = y1;
         if (y2 > this.maxY) this.maxY = y2;
         if (y3 > this.maxY) this.maxY = y3;
      }

      public float hitTest(double mouseX, double mouseY) {
         if (this.triangleCount == 0) {
            return Float.POSITIVE_INFINITY;
         }

         float px = (float) mouseX;
         float py = (float) mouseY;

         float margin = 4.0F;
         if (px < this.minX - margin || px > this.maxX + margin || py < this.minY - margin || py > this.maxY + margin) {
            return Float.POSITIVE_INFINITY;
         }

         // 1. Точная проверка: внутри полигона
         float closestDepth = Float.POSITIVE_INFINITY;
         boolean hitDirect = false;

         for (int i = 0; i < this.triangleCount; i++) {
            int base = i * 6;
            float x1 = this.triangles[base];
            float y1 = this.triangles[base + 1];
            float x2 = this.triangles[base + 2];
            float y2 = this.triangles[base + 3];
            float x3 = this.triangles[base + 4];
            float y3 = this.triangles[base + 5];

            if (pointInTriangle(px, py, x1, y1, x2, y2, x3, y3)) {
               hitDirect = true;
               float depth = this.triangleDepths[i];
               if (depth < closestDepth) {
                  closestDepth = depth;
               }
            }
         }

         if (hitDirect) {
            return closestDepth;
         }

         // 2. Толерантность к ультратонким граням (лезвие меча толщиной в 1 пиксель)
         float edgeThresholdSq = 3.5F * 3.5F;
         float minEdgeDistSq = Float.POSITIVE_INFINITY;
         float edgeClosestDepth = Float.POSITIVE_INFINITY;

         for (int i = 0; i < this.triangleCount; i++) {
            int base = i * 6;
            float x1 = this.triangles[base];
            float y1 = this.triangles[base + 1];
            float x2 = this.triangles[base + 2];
            float y2 = this.triangles[base + 3];
            float x3 = this.triangles[base + 4];
            float y3 = this.triangles[base + 5];

            float d1 = distSqToSegment(px, py, x1, y1, x2, y2);
            float d2 = distSqToSegment(px, py, x2, y2, x3, y3);
            float d3 = distSqToSegment(px, py, x3, y3, x1, y1);
            float dMin = Math.min(d1, Math.min(d2, d3));

            if (dMin <= edgeThresholdSq) {
               if (dMin < minEdgeDistSq) {
                  minEdgeDistSq = dMin;
                  edgeClosestDepth = this.triangleDepths[i];
               }
            }
         }

         if (minEdgeDistSq <= edgeThresholdSq) {
            return edgeClosestDepth;
         }

         return Float.POSITIVE_INFINITY;
      }

      private static boolean pointInTriangle(float px, float py, float x1, float y1, float x2, float y2, float x3, float y3) {
         float d1 = (px - x2) * (y1 - y2) - (x1 - x2) * (py - y2);
         float d2 = (px - x3) * (y2 - y3) - (x2 - x3) * (py - y3);
         float d3 = (px - x1) * (y3 - y1) - (x3 - x1) * (py - y1);
         boolean hasNeg = (d1 < -1e-4F) || (d2 < -1e-4F) || (d3 < -1e-4F);
         boolean hasPos = (d1 > 1e-4F) || (d2 > 1e-4F) || (d3 > 1e-4F);
         return !(hasNeg && hasPos);
      }

      private static float distSqToSegment(float px, float py, float x1, float y1, float x2, float y2) {
         float dx = x2 - x1;
         float dy = y2 - y1;
         float l2 = dx * dx + dy * dy;
         if (l2 < 1e-5F) {
            float ex = px - x1;
            float ey = py - y1;
            return ex * ex + ey * ey;
         }
         float t = Math.max(0.0F, Math.min(1.0F, ((px - x1) * dx + (py - y1) * dy) / l2));
         float projX = x1 + t * dx;
         float projY = y1 + t * dy;
         float ex = px - projX;
         float ey = py - projY;
         return ex * ex + ey * ey;
      }
   }
}
