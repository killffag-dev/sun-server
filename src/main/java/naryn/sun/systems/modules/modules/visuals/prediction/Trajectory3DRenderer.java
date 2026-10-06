package naryn.sun.systems.modules.modules.visuals.prediction;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
import naryn.sun.access.MCCameraAccess;
import naryn.sun.access.MCPlayerAccess;
import naryn.sun.utility.colors.ColorRGBA;
import naryn.sun.utility.render.RenderUtility;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class Trajectory3DRenderer {

    private static final float DOT_SIZE = 0.09F;
    private static final double DOT_SPACING = 0.55;

    private Trajectory3DRenderer() {
    }

    public static void render(
        MatrixStack ms,
        float tickDelta,
        List<TrajectoryData> trajectories,
        boolean renderTrajectory,
        boolean renderLanding,
        ColorRGBA userColor
    ) {
        if (trajectories.isEmpty() || (!renderTrajectory && !renderLanding)) return;

        ms.push();
        RenderUtility.setupRender3D(false);
        RenderUtility.prepareMatrices(ms);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.disableCull();

        // 1. Отрисовка траектории статичными точками (отсчет назад от точки приземления)
        if (renderTrajectory) {
            renderTrajectoryDots(ms, trajectories, userColor);
        }

        // 2. Отрисовка места приземления (блоки на поверхности или сущность)
        if (renderLanding) {
            PredictionLandingRenderer.render(ms, trajectories, userColor);
        }

        RenderUtility.endRender3D();
        ms.pop();
    }

    private static void renderTrajectoryDots(MatrixStack ms, List<TrajectoryData> trajectories, ColorRGBA userColor) {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferBuilder quadsBuffer = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        Matrix4f matrix = ms.peek().getPositionMatrix();

        Quaternionf cameraRot = MCCameraAccess.getRotation();
        float half = DOT_SIZE / 2.0F;
        Vector3f right = new Vector3f(1.0F, 0.0F, 0.0F).rotate(cameraRot).mul(half);
        Vector3f up = new Vector3f(0.0F, 1.0F, 0.0F).rotate(cameraRot).mul(half);

        Vec3d eyePos = MCPlayerAccess.get() != null ? MCPlayerAccess.get().getEyePos() : Vec3d.ZERO;
        ColorRGBA dotColor = userColor.withAlpha(240.0F);

        for (TrajectoryData data : trajectories) {
            List<Vec3d> points = data.positions();
            int n = points.size();
            if (n < 2) continue;

            double totalLength = 0.0;
            for (int i = 0; i < n - 1; i++) {
                totalLength += points.get(i).distanceTo(points.get(i + 1));
            }
            if (totalLength < 0.1) continue;

            double minVisibleFromStart = data.inHand() ? totalLength * 0.45 : 0.0;
            double distFromLanding = 0.0;
            double nextDotDist = DOT_SPACING;

            for (int i = n - 1; i > 0; i--) {
                Vec3d pEnd = points.get(i);
                Vec3d pStart = points.get(i - 1);
                double segLen = pEnd.distanceTo(pStart);
                if (segLen <= 0.0001) continue;

                while (nextDotDist <= distFromLanding + segLen) {
                    double t = (nextDotDist - distFromLanding) / segLen;
                    Vec3d dotPos = pEnd.lerp(pStart, t);

                    double distFromStart = totalLength - nextDotDist;
                    if (distFromStart >= minVisibleFromStart) {
                        if (!data.inHand() || dotPos.distanceTo(eyePos) >= 3.0) {
                            addDotQuad(quadsBuffer, matrix, dotPos, right, up, dotColor);
                        }
                    }

                    nextDotDist += DOT_SPACING;
                }

                distFromLanding += segLen;
            }
        }

        RenderUtility.buildBuffer(quadsBuffer);
    }

    private static void addDotQuad(
        BufferBuilder buffer,
        Matrix4f matrix,
        Vec3d pos,
        Vector3f right,
        Vector3f up,
        ColorRGBA color
    ) {
        float x = (float) pos.x;
        float y = (float) pos.y;
        float z = (float) pos.z;

        int r = (int) color.getRed();
        int g = (int) color.getGreen();
        int b = (int) color.getBlue();
        int a = (int) color.getAlpha();

        buffer.vertex(matrix, x - right.x - up.x, y - right.y - up.y, z - right.z - up.z).color(r, g, b, a);
        buffer.vertex(matrix, x + right.x - up.x, y + right.y - up.y, z + right.z - up.z).color(r, g, b, a);
        buffer.vertex(matrix, x + right.x + up.x, y + right.y + up.y, z + right.z + up.z).color(r, g, b, a);
        buffer.vertex(matrix, x - right.x + up.x, y - right.y + up.y, z - right.z + up.z).color(r, g, b, a);
    }
}
