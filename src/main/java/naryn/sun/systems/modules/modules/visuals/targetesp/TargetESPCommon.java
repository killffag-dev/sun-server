package naryn.sun.systems.modules.modules.visuals.targetesp;

import com.mojang.blaze3d.systems.RenderSystem;
import naryn.sun.utility.interfaces.IMinecraft;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class TargetESPCommon implements IMinecraft {

    private static final int RING_SEGMENTS = 54;
    private static final float[] RING_WX = new float[RING_SEGMENTS + 1];
    private static final float[] RING_WY = new float[RING_SEGMENTS + 1];
    private static final float[] RING_WZ = new float[RING_SEGMENTS + 1];
    private static final float[] RING_HNX = new float[RING_SEGMENTS + 1];
    private static final float[] RING_HNY = new float[RING_SEGMENTS + 1];
    private static final float[] RING_HNZ = new float[RING_SEGMENTS + 1];
    private static final float[] RING_CNX = new float[RING_SEGMENTS + 1];
    private static final float[] RING_CNY = new float[RING_SEGMENTS + 1];
    private static final float[] RING_CNZ = new float[RING_SEGMENTS + 1];
    private static final float[] RING_HBX = new float[RING_SEGMENTS + 1];
    private static final float[] RING_HBY = new float[RING_SEGMENTS + 1];
    private static final float[] RING_HBZ = new float[RING_SEGMENTS + 1];
    private static final float[] RING_CBX = new float[RING_SEGMENTS + 1];
    private static final float[] RING_CBY = new float[RING_SEGMENTS + 1];
    private static final float[] RING_CBZ = new float[RING_SEGMENTS + 1];

    private TargetESPCommon() {
    }

    public static Vec3d getRenderPos(LivingEntity target) {
        float tickDelta = MinecraftClient.getInstance().getRenderTickCounter().getTickDelta(false);
        return new Vec3d(
                MathHelper.lerp(tickDelta, target.prevX, target.getX()),
                MathHelper.lerp(tickDelta, target.prevY, target.getY()),
                MathHelper.lerp(tickDelta, target.prevZ, target.getZ())
        );
    }

    public static void drawGlowingRing3D(
            Matrix4f matrix, float cx, float cy, float cz, float radius,
            float rotX, float rotY, float rotZ, float beamW, float haloW,
            float r, float g, float b, float cr, float cg, float cb,
            float alpha, float glow
    ) {
        if (alpha <= 0.001F) return;

        float cosX = (float) Math.cos(rotX), sinX = (float) Math.sin(rotX);
        float cosY = (float) Math.cos(rotY), sinY = (float) Math.sin(rotY);
        float cosZ = (float) Math.cos(rotZ), sinZ = (float) Math.sin(rotZ);

        float coreAlpha = Math.min(1.0F, alpha * 0.95F);
        float haloAlpha = Math.min(1.0F, alpha * 0.35F * glow);

        // Precompute all 3D ring points and camera-facing normals (View-Aligned Ribbon)
        for (int s = 0; s <= RING_SEGMENTS; s++) {
            float a = (float) (s * 2.0 * Math.PI / RING_SEGMENTS);
            float cosA = (float) Math.cos(a), sinA = (float) Math.sin(a);

            float u = cosA * radius;
            float v = sinA * radius;

            float px = rotateX(u, v, cosX, sinX, cosY, sinY, cosZ, sinZ);
            float py = rotateY(u, v, cosX, sinX, cosZ, sinZ);
            float pz = rotateZ(u, v, cosX, sinX, cosY, sinY, cosZ, sinZ);

            float wx = cx + px;
            float wy = cy + py;
            float wz = cz + pz;

            // Unit tangent vector T to the ring at this point
            float du = -sinA;
            float dv = cosA;
            float tx = rotateX(du, dv, cosX, sinX, cosY, sinY, cosZ, sinZ);
            float ty = rotateY(du, dv, cosX, sinX, cosZ, sinZ);
            float tz = rotateZ(du, dv, cosX, sinX, cosY, sinY, cosZ, sinZ);

            // Vector from camera (0,0,0) to ring point in camera space
            float vx = wx;
            float vy = wy;
            float vz = wz;

            // Normal N = T x V (perpendicular to ring tangent AND line of sight)
            float nx = ty * vz - tz * vy;
            float ny = tz * vx - tx * vz;
            float nz = tx * vy - ty * vx;
            float nLen = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (nLen < 0.0001F) {
                nx = 0.0F; ny = 1.0F; nz = 0.0F;
            } else {
                float inv = 1.0F / nLen;
                nx *= inv; ny *= inv; nz *= inv;
            }

            RING_WX[s] = wx;
            RING_WY[s] = wy;
            RING_WZ[s] = wz;

            RING_HNX[s] = nx * haloW;
            RING_HNY[s] = ny * haloW;
            RING_HNZ[s] = nz * haloW;

            RING_CNX[s] = nx * beamW;
            RING_CNY[s] = ny * beamW;
            RING_CNZ[s] = nz * beamW;

            // Binormal B = T x N (perpendicular to both tangent and screen-normal: provides 3D volumetric depth)
            float bx = ty * nz - tz * ny;
            float by = tz * nx - tx * nz;
            float bz = tx * ny - ty * nx;

            RING_HBX[s] = bx * haloW;
            RING_HBY[s] = by * haloW;
            RING_HBZ[s] = bz * haloW;

            RING_CBX[s] = bx * beamW;
            RING_CBY[s] = by * beamW;
            RING_CBZ[s] = bz * beamW;
        }

        // Pass 1: Diffuse outer halo left side (center -> -hn fading to 0)
        BufferBuilder haloLeft = RenderSystem.renderThreadTesselator().begin(DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        for (int s = 0; s <= RING_SEGMENTS; s++) {
            haloLeft.vertex(matrix, RING_WX[s], RING_WY[s], RING_WZ[s]).color(r, g, b, haloAlpha);
            haloLeft.vertex(matrix, RING_WX[s] - RING_HNX[s], RING_WY[s] - RING_HNY[s], RING_WZ[s] - RING_HNZ[s]).color(r, g, b, 0.0F);
        }
        BufferRenderer.drawWithGlobalProgram(haloLeft.end());

        // Pass 2: Diffuse outer halo right side (center -> +hn fading to 0)
        BufferBuilder haloRight = RenderSystem.renderThreadTesselator().begin(DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        for (int s = 0; s <= RING_SEGMENTS; s++) {
            haloRight.vertex(matrix, RING_WX[s], RING_WY[s], RING_WZ[s]).color(r, g, b, haloAlpha);
            haloRight.vertex(matrix, RING_WX[s] + RING_HNX[s], RING_WY[s] + RING_HNY[s], RING_WZ[s] + RING_HNZ[s]).color(r, g, b, 0.0F);
        }
        BufferRenderer.drawWithGlobalProgram(haloRight.end());

        // Pass 3: Focused intense screen-aligned core laser strip
        BufferBuilder coreBuf = RenderSystem.renderThreadTesselator().begin(DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        for (int s = 0; s <= RING_SEGMENTS; s++) {
            coreBuf.vertex(matrix, RING_WX[s] - RING_CNX[s], RING_WY[s] - RING_CNY[s], RING_WZ[s] - RING_CNZ[s]).color(cr, cg, cb, coreAlpha);
            coreBuf.vertex(matrix, RING_WX[s] + RING_CNX[s], RING_WY[s] + RING_CNY[s], RING_WZ[s] + RING_CNZ[s]).color(cr, cg, cb, coreAlpha);
        }
        BufferRenderer.drawWithGlobalProgram(coreBuf.end());

        // Pass 4: Volumetric 3D Depth core strip (Perpendicular Binormal: gives true 3D tubular thickness!)
        BufferBuilder depthBuf = RenderSystem.renderThreadTesselator().begin(DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        for (int s = 0; s <= RING_SEGMENTS; s++) {
            depthBuf.vertex(matrix, RING_WX[s] - RING_CBX[s], RING_WY[s] - RING_CBY[s], RING_WZ[s] - RING_CBZ[s]).color(cr, cg, cb, coreAlpha);
            depthBuf.vertex(matrix, RING_WX[s] + RING_CBX[s], RING_WY[s] + RING_CBY[s], RING_WZ[s] + RING_CBZ[s]).color(cr, cg, cb, coreAlpha);
        }
        BufferRenderer.drawWithGlobalProgram(depthBuf.end());
    }

    public static float rotateX(float x, float y, float cosX, float sinX, float cosY, float sinY, float cosZ, float sinZ) {
        float rx = x * cosZ - y * sinZ;
        float ry = x * sinZ + y * cosZ;
        return rx * cosY + (ry * sinX) * sinY;
    }

    public static float rotateY(float x, float y, float cosX, float sinX, float cosZ, float sinZ) {
        float ry = x * sinZ + y * cosZ;
        return ry * cosX;
    }

    public static float rotateZ(float x, float y, float cosX, float sinX, float cosY, float sinY, float cosZ, float sinZ) {
        float rx = x * cosZ - y * sinZ;
        float ry = x * sinZ + y * cosZ;
        return -rx * sinY + (ry * sinX) * cosY;
    }

    public static void drawSoftGlow(
            MatrixStack ms, float cx, float cy, float cz, float radius, float r, float g, float b, float centerAlpha, Vector3f right, Vector3f up
    ) {
        Matrix4f matrix = ms.peek().getPositionMatrix();
        BufferBuilder buf = RenderSystem.renderThreadTesselator().begin(DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        buf.vertex(matrix, cx, cy, cz).color(r, g, b, centerAlpha);
        int segments = 16;
        for (int i = 0; i <= segments; i++) {
            float angle = (float) (i * 2.0 * Math.PI / segments);
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            float vx = cx + (right.x * cos + up.x * sin) * radius;
            float vy = cy + (right.y * cos + up.y * sin) * radius;
            float vz = cz + (right.z * cos + up.z * sin) * radius;
            buf.vertex(matrix, vx, vy, vz).color(r, g, b, 0.0F);
        }
        BufferRenderer.drawWithGlobalProgram(buf.end());
    }

    public static void drawBillboardSegment(
            Matrix4f matrix, BufferBuilder buf,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float camX, float camY, float camZ,
            float halfW,
            float r, float g, float b, float a
    ) {
        float dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
        float mx = (x1 + x2) * 0.5F - camX;
        float my = (y1 + y2) * 0.5F - camY;
        float mz = (z1 + z2) * 0.5F - camZ;

        float nx = dy * mz - dz * my;
        float ny = dz * mx - dx * mz;
        float nz = dx * my - dy * mx;
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 0.0001F) {
            nx = 0.0F; ny = halfW; nz = 0.0F;
        } else {
            float inv = halfW / len;
            nx *= inv; ny *= inv; nz *= inv;
        }

        buf.vertex(matrix, x1 - nx, y1 - ny, z1 - nz).color(r, g, b, a);
        buf.vertex(matrix, x1 + nx, y1 + ny, z1 + nz).color(r, g, b, a);
        buf.vertex(matrix, x2 + nx, y2 + ny, z2 + nz).color(r, g, b, a);
        buf.vertex(matrix, x2 - nx, y2 - ny, z2 - nz).color(r, g, b, a);
    }

    public static void drawGlowingBillboardSegment(
            Matrix4f matrix, BufferBuilder buf,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float camX, float camY, float camZ,
            float coreW, float haloW,
            float r, float g, float b,
            float cr, float cg, float cb,
            float a, float glow
    ) {
        float dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
        float mx = (x1 + x2) * 0.5F - camX;
        float my = (y1 + y2) * 0.5F - camY;
        float mz = (z1 + z2) * 0.5F - camZ;

        float nx = dy * mz - dz * my;
        float ny = dz * mx - dx * mz;
        float nz = dx * my - dy * mx;
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 0.0001F) {
            nx = 0.0F; ny = 1.0F; nz = 0.0F;
        } else {
            float inv = 1.0F / len;
            nx *= inv; ny *= inv; nz *= inv;
        }

        float hnx = nx * haloW, hny = ny * haloW, hnz = nz * haloW;
        float cnx = nx * coreW, cny = ny * coreW, cnz = nz * coreW;
        float haloA = Math.min(1.0F, a * 0.35F * glow);

        // Left soft halo (center to -hn) fading to zero
        buf.vertex(matrix, x1, y1, z1).color(r, g, b, haloA);
        buf.vertex(matrix, x1 - hnx, y1 - hny, z1 - hnz).color(r, g, b, 0.0F);
        buf.vertex(matrix, x2 - hnx, y2 - hny, z2 - hnz).color(r, g, b, 0.0F);
        buf.vertex(matrix, x2, y2, z2).color(r, g, b, haloA);

        // Right soft halo (center to +hn) fading to zero
        buf.vertex(matrix, x1, y1, z1).color(r, g, b, haloA);
        buf.vertex(matrix, x1 + hnx, y1 + hny, z1 + hnz).color(r, g, b, 0.0F);
        buf.vertex(matrix, x2 + hnx, y2 + hny, z2 + hnz).color(r, g, b, 0.0F);
        buf.vertex(matrix, x2, y2, z2).color(r, g, b, haloA);

        // Core laser
        buf.vertex(matrix, x1 - cnx, y1 - cny, z1 - cnz).color(cr, cg, cb, a);
        buf.vertex(matrix, x1 + cnx, y1 + cny, z1 + cnz).color(cr, cg, cb, a);
        buf.vertex(matrix, x2 + cnx, y2 + cny, z2 + cnz).color(cr, cg, cb, a);
        buf.vertex(matrix, x2 - cnx, y2 - cny, z2 - cnz).color(cr, cg, cb, a);
    }

    private static final float SQRT2_2 = 0.70710678F;

    public static void drawBillboardNode(
            Matrix4f matrix, BufferBuilder buf,
            float cx, float cy, float cz,
            Vector3f right, Vector3f up,
            float radius,
            float r, float g, float b, float a
    ) {
        if (a <= 0.001F) return;

        float r1x = right.x * radius, r1y = right.y * radius, r1z = right.z * radius;
        float u1x = up.x * radius, u1y = up.y * radius, u1z = up.z * radius;

        float diagR = radius * SQRT2_2;
        float rdX = (right.x + up.x) * diagR;
        float rdY = (right.y + up.y) * diagR;
        float rdZ = (right.z + up.z) * diagR;

        float ldX = (-right.x + up.x) * diagR;
        float ldY = (-right.y + up.y) * diagR;
        float ldZ = (-right.z + up.z) * diagR;

        // 8-point radial circular fan (4 quads with alpha fading to 0 at perimeter)
        // Quad 0: East quadrant
        buf.vertex(matrix, cx, cy, cz).color(r, g, b, a);
        buf.vertex(matrix, cx + r1x, cy + r1y, cz + r1z).color(r, g, b, 0.0F);
        buf.vertex(matrix, cx + rdX, cy + rdY, cz + rdZ).color(r, g, b, 0.0F);
        buf.vertex(matrix, cx + u1x, cy + u1y, cz + u1z).color(r, g, b, 0.0F);

        // Quad 1: North quadrant
        buf.vertex(matrix, cx, cy, cz).color(r, g, b, a);
        buf.vertex(matrix, cx + u1x, cy + u1y, cz + u1z).color(r, g, b, 0.0F);
        buf.vertex(matrix, cx + ldX, cy + ldY, cz + ldZ).color(r, g, b, 0.0F);
        buf.vertex(matrix, cx - r1x, cy - r1y, cz - r1z).color(r, g, b, 0.0F);

        // Quad 2: West quadrant
        buf.vertex(matrix, cx, cy, cz).color(r, g, b, a);
        buf.vertex(matrix, cx - r1x, cy - r1y, cz - r1z).color(r, g, b, 0.0F);
        buf.vertex(matrix, cx - rdX, cy - rdY, cz - rdZ).color(r, g, b, 0.0F);
        buf.vertex(matrix, cx - u1x, cy - u1y, cz - u1z).color(r, g, b, 0.0F);

        // Quad 3: South quadrant
        buf.vertex(matrix, cx, cy, cz).color(r, g, b, a);
        buf.vertex(matrix, cx - u1x, cy - u1y, cz - u1z).color(r, g, b, 0.0F);
        buf.vertex(matrix, cx - ldX, cy - ldY, cz - ldZ).color(r, g, b, 0.0F);
        buf.vertex(matrix, cx + r1x, cy + r1y, cz + r1z).color(r, g, b, 0.0F);
    }

    public static void drawGlowingNode(
            Matrix4f matrix, BufferBuilder buf,
            float cx, float cy, float cz,
            Vector3f right, Vector3f up,
            float coreSize, float haloSize,
            float r, float g, float b,
            float cr, float cg, float cb,
            float a, float glow
    ) {
        // Outer soft colored halo
        drawBillboardNode(matrix, buf, cx, cy, cz, right, up, haloSize, r, g, b, Math.min(1.0F, a * 0.40F * glow));
        // Inner intense hot core
        drawBillboardNode(matrix, buf, cx, cy, cz, right, up, coreSize, cr, cg, cb, a);
    }
}
