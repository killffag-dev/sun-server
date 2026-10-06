package naryn.sun.systems.modules.modules.visuals.targetesp;

import com.mojang.blaze3d.systems.RenderSystem;
import naryn.sun.utility.colors.ColorRGBA;
import naryn.sun.utility.interfaces.IMinecraft;
import naryn.sun.utility.math.pool.MathPool;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class TargetShieldRenderer implements IMinecraft {

    private static final int MAX_CELLS = 36;
    private static final float GOLDEN_RATIO = (float) ((1.0 + Math.sqrt(5.0)) / 2.0);
    private static final float GOLDEN_ANGLE = (float) (2.0 * Math.PI * (1.0 - 1.0 / GOLDEN_RATIO));

    private TargetShieldRenderer() {
    }

    public static void draw(
            MatrixStack ms,
            LivingEntity target,
            ColorRGBA color,
            float animVal,
            float radiusMult,
            float hexSize,
            float speed,
            float density,
            float glow
    ) {
        if (animVal <= 0.001F) {
            return;
        }

        Vec3d targetPos = TargetESPCommon.getRenderPos(target);
        Vec3d camPos = mc.gameRenderer.getCamera().getPos();
        Camera camera = mc.gameRenderer.getCamera();

        float r = color.getRed() / 255.0F;
        float g = color.getGreen() / 255.0F;
        float b = color.getBlue() / 255.0F;
        float tickTime = (System.currentTimeMillis() % 1000000L) / 50.0F;

        float radiusX = (target.getWidth() * 0.85F + 0.35F) * radiusMult;
        float radiusY = (target.getHeight() * 0.60F + 0.25F) * radiusMult;
        float radiusZ = radiusX;

        float centerX = (float) (targetPos.x - camPos.x);
        float centerY = (float) (targetPos.y - camPos.y + target.getHeight() * 0.5F);
        float centerZ = (float) (targetPos.z - camPos.z);

        Vector3f right = MathPool.vec3(1.0F, 0.0F, 0.0F).rotate(camera.getRotation());
        Vector3f up = MathPool.vec3(0.0F, 1.0F, 0.0F).rotate(camera.getRotation());
        Vector3f normal = MathPool.vec3();
        Vector3f worldUp = MathPool.vec3();
        Vector3f tangentU = MathPool.vec3();
        Vector3f tangentV = MathPool.vec3();

        int cellCount = Math.max(16, Math.min(MAX_CELLS, Math.round(MAX_CELLS * (density / 100.0F))));
        float threshold = 0.25F;
        float rotOffset = tickTime * speed * 0.015F;

        float[] cellPx = new float[MAX_CELLS];
        float[] cellPy = new float[MAX_CELLS];
        float[] cellPz = new float[MAX_CELLS];
        float[] cellFillA = new float[MAX_CELLS];
        float[] cellLineA = new float[MAX_CELLS];
        float[] cellGlowA = new float[MAX_CELLS];
        float[][] cellHexX = new float[MAX_CELLS][6];
        float[][] cellHexY = new float[MAX_CELLS][6];
        float[][] cellHexZ = new float[MAX_CELLS][6];
        int activeCount = 0;

        try {
            for (int i = 0; i < cellCount; i++) {
                float seed = i * 137.5F + (i % 7) * 23.1F;
                float t = tickTime * speed * 0.04F + seed;
                float wave = (float) (Math.sin(t) * 0.65 + Math.sin(t * 1.9 + seed * 0.5) * 0.35);

                if (wave <= threshold) {
                    continue;
                }

                float rawVisibility = (wave - threshold) / (1.0F - threshold);
                float visibility = rawVisibility * rawVisibility * (3.0F - 2.0F * rawVisibility);
                float cellAlpha = visibility * animVal;
                if (cellAlpha <= 0.01F) {
                    continue;
                }

                float flicker = 0.85F + 0.15F * (float) Math.sin(tickTime * 0.8F + i * 5.7F);
                float fillAlpha = cellAlpha * 0.28F;
                float lineAlpha = cellAlpha * 0.95F * flicker;

                float yUnit = 1.0F - (i + 0.5F) / cellCount * 2.0F;
                float rUnit = (float) Math.sqrt(Math.max(0.0F, 1.0F - yUnit * yUnit));
                float phi = i * GOLDEN_ANGLE + rotOffset;

                float nx = rUnit * (float) Math.cos(phi);
                float ny = yUnit;
                float nz = rUnit * (float) Math.sin(phi);

                float px = centerX + nx * radiusX;
                float py = centerY + ny * radiusY;
                float pz = centerZ + nz * radiusZ;

                normal.set(nx, ny, nz).normalize();
                worldUp.set(0.0F, 1.0F, 0.0F);
                if (Math.abs(normal.dot(worldUp)) > 0.9F) {
                    worldUp.set(1.0F, 0.0F, 0.0F);
                }
                tangentU.set(worldUp).cross(normal).normalize();
                tangentV.set(normal).cross(tangentU).normalize();

                cellPx[activeCount] = px;
                cellPy[activeCount] = py;
                cellPz[activeCount] = pz;
                cellFillA[activeCount] = fillAlpha;
                cellLineA[activeCount] = lineAlpha;
                cellGlowA[activeCount] = Math.min(1.0F, cellAlpha * 0.18F * glow);

                for (int k = 0; k < 6; k++) {
                    float angle = (float) (k * Math.PI / 3.0);
                    float cosA = (float) Math.cos(angle) * hexSize;
                    float sinA = (float) Math.sin(angle) * hexSize;

                    cellHexX[activeCount][k] = px + tangentU.x * cosA + tangentV.x * sinA;
                    cellHexY[activeCount][k] = py + tangentU.y * cosA + tangentV.y * sinA;
                    cellHexZ[activeCount][k] = pz + tangentU.z * cosA + tangentV.z * sinA;
                }
                activeCount++;
            }

            if (activeCount == 0) {
                return;
            }

            Matrix4f matrix = ms.peek().getPositionMatrix();
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

            // Pass 1: Hex interior fills
            BufferBuilder fillBuffer = RenderSystem.renderThreadTesselator().begin(DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
            for (int a = 0; a < activeCount; a++) {
                float fa = cellFillA[a];
                float px = cellPx[a];
                float py = cellPy[a];
                float pz = cellPz[a];
                float[] hx = cellHexX[a];
                float[] hy = cellHexY[a];
                float[] hz = cellHexZ[a];

                for (int k = 0; k < 6; k++) {
                    int next = (k + 1) % 6;
                    fillBuffer.vertex(matrix, px, py, pz).color(r, g, b, fa);
                    fillBuffer.vertex(matrix, hx[k], hy[k], hz[k]).color(r, g, b, fa);
                    fillBuffer.vertex(matrix, hx[next], hy[next], hz[next]).color(r, g, b, fa);
                }
            }
            BufferRenderer.drawWithGlobalProgram(fillBuffer.end());

            // Pass 2: Hex contour lines
            BufferBuilder lineBuffer = RenderSystem.renderThreadTesselator().begin(DrawMode.LINES, VertexFormats.POSITION_COLOR);
            for (int a = 0; a < activeCount; a++) {
                float la = cellLineA[a];
                float[] hx = cellHexX[a];
                float[] hy = cellHexY[a];
                float[] hz = cellHexZ[a];

                for (int k = 0; k < 6; k++) {
                    int next = (k + 1) % 6;
                    lineBuffer.vertex(matrix, hx[k], hy[k], hz[k]).color(r, g, b, la);
                    lineBuffer.vertex(matrix, hx[next], hy[next], hz[next]).color(r, g, b, la);
                }
            }
            BufferRenderer.drawWithGlobalProgram(lineBuffer.end());

            // Pass 3: Soft localized glow
            if (glow > 0.0F) {
                for (int a = 0; a < activeCount; a++) {
                    if (cellGlowA[a] > 0.02F) {
                        TargetESPCommon.drawSoftGlow(ms, cellPx[a], cellPy[a], cellPz[a], hexSize * 2.2F, r, g, b, cellGlowA[a], right, up);
                    }
                }
            }
        } finally {
            MathPool.release(right);
            MathPool.release(up);
            MathPool.release(normal);
            MathPool.release(worldUp);
            MathPool.release(tangentU);
            MathPool.release(tangentV);
        }
    }
}
