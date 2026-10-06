package naryn.sun.utility.render;

import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.math.MathHelper;

public final class SkyDomeGeometry {
   private static final int SLICES = 32;
   private static final int STACKS = 16;
   private static final float RADIUS = 100.0F;

   private SkyDomeGeometry() {
   }

   public static VertexBuffer createBuffer() {
      return VertexBuffer.createAndUpload(
         VertexFormat.DrawMode.QUADS,
         VertexFormats.POSITION,
         SkyDomeGeometry::buildDome
      );
   }

   private static void buildDome(VertexConsumer consumer) {
      // Верхний купол
      for (int stack = 0; stack < STACKS; stack++) {
         float phi0 = (float) Math.PI * 0.5F * (1.0F - (float) stack / STACKS);
         float phi1 = (float) Math.PI * 0.5F * (1.0F - (float) (stack + 1) / STACKS);

         float y0 = RADIUS * MathHelper.sin(phi0);
         float r0 = RADIUS * MathHelper.cos(phi0);

         float y1 = RADIUS * MathHelper.sin(phi1);
         float r1 = RADIUS * MathHelper.cos(phi1);

         for (int slice = 0; slice < SLICES; slice++) {
            float theta0 = (float) (2.0 * Math.PI * (float) slice / SLICES);
            float theta1 = (float) (2.0 * Math.PI * (float) (slice + 1) / SLICES);

            float x00 = r0 * MathHelper.sin(theta0);
            float z00 = r0 * MathHelper.cos(theta0);

            float x10 = r0 * MathHelper.sin(theta1);
            float z10 = r0 * MathHelper.cos(theta1);

            float x11 = r1 * MathHelper.sin(theta1);
            float z11 = r1 * MathHelper.cos(theta1);

            float x01 = r1 * MathHelper.sin(theta0);
            float z01 = r1 * MathHelper.cos(theta0);

            consumer.vertex(x00, y0, z00);
            consumer.vertex(x10, y0, z10);
            consumer.vertex(x11, y1, z11);
            consumer.vertex(x01, y1, z01);
         }
      }

      // Нижний купол для горизонта
      for (int stack = 0; stack < STACKS; stack++) {
         float phi0 = -(float) Math.PI * 0.5F * ((float) stack / STACKS);
         float phi1 = -(float) Math.PI * 0.5F * ((float) (stack + 1) / STACKS);

         float y0 = RADIUS * MathHelper.sin(phi0);
         float r0 = RADIUS * MathHelper.cos(phi0);

         float y1 = RADIUS * MathHelper.sin(phi1);
         float r1 = RADIUS * MathHelper.cos(phi1);

         for (int slice = 0; slice < SLICES; slice++) {
            float theta0 = (float) (2.0 * Math.PI * (float) slice / SLICES);
            float theta1 = (float) (2.0 * Math.PI * (float) (slice + 1) / SLICES);

            float x00 = r0 * MathHelper.sin(theta0);
            float z00 = r0 * MathHelper.cos(theta0);

            float x10 = r0 * MathHelper.sin(theta1);
            float z10 = r0 * MathHelper.cos(theta1);

            float x11 = r1 * MathHelper.sin(theta1);
            float z11 = r1 * MathHelper.cos(theta1);

            float x01 = r1 * MathHelper.sin(theta0);
            float z01 = r1 * MathHelper.cos(theta0);

            consumer.vertex(x00, y0, z00);
            consumer.vertex(x10, y0, z10);
            consumer.vertex(x11, y1, z11);
            consumer.vertex(x01, y1, z01);
         }
      }
   }
}
