package naryn.sun.systems.modules.modules.visuals.targetesp;

import com.mojang.blaze3d.systems.RenderSystem;
import naryn.sun.utility.colors.ColorRGBA;
import naryn.sun.utility.interfaces.IMinecraft;
import naryn.sun.utility.math.MathPool;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class TargetBigCrystalRenderer implements IMinecraft {

    private static final int CRYSTAL_COUNT = 8;
    private static final float[] FACET_SHADING = {1.0F, 0.78F, 0.62F, 0.88F, 0.70F, 0.55F};
    private static final float[] PX = new float[CRYSTAL_COUNT];
    private static final float[] PY = new float[CRYSTAL_COUNT];
    private static final float[] PZ = new float[CRYSTAL_COUNT];
    private static final float[] WAVE_ARR = new float[CRYSTAL_COUNT];

    private TargetBigCrystalRenderer() {
    }

    public static void draw(
            MatrixStack ms, LivingEntity target, ColorRGBA color, float animVal, float moveVal,
            float radiusMult, float size, float speed, float glow
    ) {
        if (animVal <= 0.001F) {
            return;
        }

        Camera camera = mc.gameRenderer.getCamera();
        Vec3d targetPos = TargetESPCommon.getRenderPos(target);
        Vec3d camPos = camera.getPos();
        float baseRadius = (target.getWidth() * 0.9F + 0.4F) * radiusMult;
        float tickTime = (System.currentTimeMillis() % 1000000L) / 50.0F;

        float r = color.getRed() / 255.0F;
        float g = color.getGreen() / 255.0F;
        float b = color.getBlue() / 255.0F;

        Vector3f right = MathPool.vec3f(1.0F, 0.0F, 0.0F).rotate(camera.getRotation());
        Vector3f up = MathPool.vec3f(0.0F, 1.0F, 0.0F).rotate(camera.getRotation());

        float effectiveSpeed = speed * 1.8F;
        float orbitAngleBase = (moveVal * 0.02F + tickTime * 0.03F) * effectiveSpeed;
        float breathe = 1.15F - 0.15F * animVal;

        for (int i = 0; i < CRYSTAL_COUNT; i++) {
            float angle = orbitAngleBase + (float) (i * 2.0 * Math.PI / CRYSTAL_COUNT);
            float wave = (float) Math.sin(angle * 2.0F + tickTime * 0.06F);
            WAVE_ARR[i] = wave;

            float orbitX = (float) Math.cos(angle) * baseRadius * breathe;
            float orbitZ = (float) Math.sin(angle) * baseRadius * breathe;
            float orbitY = target.getHeight() * 0.5F + wave * (target.getHeight() * 0.22F);

            PX[i] = (float) (targetPos.x - camPos.x) + orbitX;
            PY[i] = (float) (targetPos.y - camPos.y) + orbitY;
            PZ[i] = (float) (targetPos.z - camPos.z) + orbitZ;
        }

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        // Pass 1: Shaded crystal facets
        BufferBuilder triBuffer = RenderSystem.renderThreadTesselator().begin(DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        for (int i = 0; i < CRYSTAL_COUNT; i++) {
            ms.push();
            ms.translate(PX[i], PY[i], PZ[i]);
            ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(tickTime * 2.5F * speed + i * 45.0F));
            ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(WAVE_ARR[i] * 22.0F));
            ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(15.0F));

            renderCrystalFacets(ms, triBuffer, size, r, g, b, animVal);
            ms.pop();
        }
        BufferRenderer.drawWithGlobalProgram(triBuffer.end());

        // Pass 2: Sharp contour lines
        BufferBuilder lineBuffer = RenderSystem.renderThreadTesselator().begin(DrawMode.LINES, VertexFormats.POSITION_COLOR);
        for (int i = 0; i < CRYSTAL_COUNT; i++) {
            ms.push();
            ms.translate(PX[i], PY[i], PZ[i]);
            ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(tickTime * 2.5F * speed + i * 45.0F));
            ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(WAVE_ARR[i] * 22.0F));
            ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(15.0F));

            renderCrystalLines(ms, lineBuffer, size, r, g, b, animVal);
            ms.pop();
        }
        BufferRenderer.drawWithGlobalProgram(lineBuffer.end());

        // Pass 3: Soft glow
        if (glow > 0.0F) {
            float glowAlpha = Math.min(1.0F, animVal * 0.22F * glow);
            for (int i = 0; i < CRYSTAL_COUNT; i++) {
                TargetESPCommon.drawSoftGlow(ms, PX[i], PY[i], PZ[i], size * 4.0F, r, g, b, glowAlpha, right, up);
            }
        }
    }

    private static void renderCrystalFacets(
            MatrixStack ms, BufferBuilder triBuf, float size, float r, float g, float b, float animVal
    ) {
        Matrix4f mat = ms.peek().getPositionMatrix();
        float topY = size * 1.7F;
        float botY = -size * 1.5F;
        float midR = size * 0.7F;
        int segments = 6;

        float fillBaseAlpha = 0.55F * animVal;

        for (int i = 0; i < segments; i++) {
            float a1 = (float) (i * 2.0 * Math.PI / segments);
            float a2 = (float) ((i + 1) * 2.0 * Math.PI / segments);
            float e1x = (float) Math.cos(a1) * midR;
            float e1z = (float) Math.sin(a1) * midR;
            float e2x = (float) Math.cos(a2) * midR;
            float e2z = (float) Math.sin(a2) * midR;

            float shade = FACET_SHADING[i % FACET_SHADING.length];

            // Top pyramid
            triBuf.vertex(mat, 0.0F, topY, 0.0F).color(r * shade, g * shade, b * shade, fillBaseAlpha);
            triBuf.vertex(mat, e1x, 0.0F, e1z).color(r * shade * 0.8F, g * shade * 0.8F, b * shade * 0.8F, fillBaseAlpha);
            triBuf.vertex(mat, e2x, 0.0F, e2z).color(r * shade * 0.8F, g * shade * 0.8F, b * shade * 0.8F, fillBaseAlpha);

            // Bottom pyramid
            triBuf.vertex(mat, 0.0F, botY, 0.0F).color(r * shade * 0.6F, g * shade * 0.6F, b * shade * 0.6F, fillBaseAlpha);
            triBuf.vertex(mat, e2x, 0.0F, e2z).color(r * shade * 0.7F, g * shade * 0.7F, b * shade * 0.7F, fillBaseAlpha);
            triBuf.vertex(mat, e1x, 0.0F, e1z).color(r * shade * 0.7F, g * shade * 0.7F, b * shade * 0.7F, fillBaseAlpha);
        }
    }

    private static void renderCrystalLines(
            MatrixStack ms, BufferBuilder lineBuf, float size, float r, float g, float b, float animVal
    ) {
        Matrix4f mat = ms.peek().getPositionMatrix();
        float topY = size * 1.7F;
        float botY = -size * 1.5F;
        float midR = size * 0.7F;
        int segments = 6;
        float lineAlpha = 0.95F * animVal;

        for (int i = 0; i < segments; i++) {
            float a1 = (float) (i * 2.0 * Math.PI / segments);
            float a2 = (float) ((i + 1) * 2.0 * Math.PI / segments);
            float e1x = (float) Math.cos(a1) * midR;
            float e1z = (float) Math.sin(a1) * midR;
            float e2x = (float) Math.cos(a2) * midR;
            float e2z = (float) Math.sin(a2) * midR;

            // From top to equator
            lineBuf.vertex(mat, 0.0F, topY, 0.0F).color(r, g, b, lineAlpha);
            lineBuf.vertex(mat, e1x, 0.0F, e1z).color(r, g, b, lineAlpha);

            // From bottom to equator
            lineBuf.vertex(mat, 0.0F, botY, 0.0F).color(r, g, b, lineAlpha);
            lineBuf.vertex(mat, e1x, 0.0F, e1z).color(r, g, b, lineAlpha);

            // Equator ring
            lineBuf.vertex(mat, e1x, 0.0F, e1z).color(r, g, b, lineAlpha);
            lineBuf.vertex(mat, e2x, 0.0F, e2z).color(r, g, b, lineAlpha);
        }
    }
}
