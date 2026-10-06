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

public final class TargetOrbitCrystalsRenderer implements IMinecraft {

    private TargetOrbitCrystalsRenderer() {
    }

    public static void draw(
            MatrixStack ms,
            LivingEntity target,
            ColorRGBA crystalColor,
            float animVal,
            float radius,
            float size,
            float speed,
            float glow
    ) {
        Vec3d targetPos = TargetESPCommon.getRenderPos(target);
        Vec3d camPos = mc.gameRenderer.getCamera().getPos();
        Camera camera = mc.gameRenderer.getCamera();
        float r = crystalColor.getRed() / 255.0F;
        float g = crystalColor.getGreen() / 255.0F;
        float b = crystalColor.getBlue() / 255.0F;

        float centerY = (float) (targetPos.y - camPos.y + target.getHeight() * 0.5);
        float speedDeg = speed * 0.3F;
        float tickTime = (System.currentTimeMillis() % 1000000L) / 50.0F;

        Vector3f right = MathPool.vec3(1.0F, 0.0F, 0.0F).rotate(camera.getRotation());
        Vector3f up = MathPool.vec3(0.0F, 1.0F, 0.0F).rotate(camera.getRotation());

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        try {
            for (int i = 0; i < 6; i++) {
                float phase = (float) (i * 2.0 * Math.PI / 6.0);
                float angle = (float) Math.toRadians(tickTime * speedDeg) + phase;
                float ox = (float) (Math.cos(angle) * radius);
                float oz = (float) (Math.sin(angle) * radius);
                float yOff = (float) (Math.sin(angle * 0.7 + phase) * 0.18);

                float px = (float) (targetPos.x - camPos.x) + ox;
                float py = centerY + yOff;
                float pz = (float) (targetPos.z - camPos.z) + oz;

                ms.push();
                ms.translate(px, py, pz);
                drawCrystalShape(ms, size, r, g, b, animVal * 0.90F);
                ms.pop();

                TargetESPCommon.drawSoftGlow(ms, px, py, pz, size * 3.5F, r, g, b, Math.min(1.0F, animVal * 0.18F * glow), right, up);
                TargetESPCommon.drawSoftGlow(ms, px, py, pz, size * 6.0F, r, g, b, Math.min(1.0F, animVal * 0.08F * glow), right, up);
                TargetESPCommon.drawSoftGlow(ms, px, py, pz, size * 10.0F, r, g, b, Math.min(1.0F, animVal * 0.03F * glow), right, up);
            }
        } finally {
            MathPool.release(right);
            MathPool.release(up);
        }
    }

    private static void drawCrystalShape(MatrixStack ms, float size, float r, float g, float b, float alpha) {
        Matrix4f matrix = ms.peek().getPositionMatrix();
        BufferBuilder buf = RenderSystem.renderThreadTesselator().begin(DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        float h = size * 1.8F;
        float hb = size * 1.0F;
        float[][] ring = {{size, 0, 0}, {0, 0, size}, {-size, 0, 0}, {0, 0, -size}};
        for (int i = 0; i < 4; i++) {
            float[] aa = ring[i];
            float[] c = ring[(i + 1) % 4];
            buf.vertex(matrix, 0, h, 0).color(r, g, b, alpha);
            buf.vertex(matrix, aa[0], 0, aa[2]).color(r, g, b, alpha * 0.45F);
            buf.vertex(matrix, c[0], 0, c[2]).color(r, g, b, alpha * 0.45F);
        }
        for (int i = 0; i < 4; i++) {
            float[] aa = ring[i];
            float[] c = ring[(i + 1) % 4];
            buf.vertex(matrix, 0, -hb, 0).color(r, g, b, alpha * 0.85F);
            buf.vertex(matrix, c[0], 0, c[2]).color(r, g, b, alpha * 0.35F);
            buf.vertex(matrix, aa[0], 0, aa[2]).color(r, g, b, alpha * 0.35F);
        }
        BufferRenderer.drawWithGlobalProgram(buf.end());
    }
}
