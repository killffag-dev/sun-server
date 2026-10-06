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
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.List;

public final class TargetSpheresRenderer implements IMinecraft {

    private TargetSpheresRenderer() {
    }

    public static void draw(
            MatrixStack ms,
            LivingEntity target,
            ColorRGBA sphereColor,
            float animVal,
            float orbitRad,
            float size,
            float rotSpeed,
            float bobSpeed,
            float bobHeight,
            float glow,
            int trailLength,
            List<Vec3d>[] trails
    ) {
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d targetPos = TargetESPCommon.getRenderPos(target);
        double cx = targetPos.x;
        double cy = targetPos.y + target.getHeight() * 0.5;
        double cz = targetPos.z;
        Vec3d camPos = camera.getPos();
        float tickTime = (System.currentTimeMillis() % 1000000L) / 50.0F;

        Vector3f right = MathPool.vec3f(1.0F, 0.0F, 0.0F).rotate(camera.getRotation());
        Vector3f up = MathPool.vec3f(0.0F, 1.0F, 0.0F).rotate(camera.getRotation());

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        for (int i = 0; i < 3; i++) {
            float phase = (float) (i * 2.0 * Math.PI / 3.0);
            double angle = Math.toRadians(tickTime * rotSpeed) + phase;
            double ox = Math.cos(angle) * orbitRad;
            double oz = Math.sin(angle) * orbitRad;
            double bob = Math.sin(Math.toRadians(tickTime * bobSpeed) + phase) * bobHeight;
            Vec3d pos = new Vec3d(cx + ox, cy + bob, cz + oz);

            trails[i].add(0, pos);
            if (trails[i].size() > trailLength) {
                trails[i].remove(trails[i].size() - 1);
            }
            renderTrail(ms, camPos, trails[i], sphereColor, animVal);
            renderGlowSphere(ms, camPos, pos, right, up, sphereColor, size, glow, animVal);
        }
    }

    private static void renderTrail(MatrixStack ms, Vec3d cam, List<Vec3d> trail, ColorRGBA color, float animVal) {
        if (trail.size() < 2) {
            return;
        }
        Matrix4f matrix = ms.peek().getPositionMatrix();
        float r = color.getRed() / 255.0F;
        float g = color.getGreen() / 255.0F;
        float b = color.getBlue() / 255.0F;
        BufferBuilder buf = RenderSystem.renderThreadTesselator().begin(DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i = 0; i < trail.size(); i++) {
            Vec3d pos = trail.get(i);
            float alpha = (1.0F - (float) i / trail.size()) * 0.5F * animVal;
            buf.vertex(matrix, (float) (pos.x - cam.x), (float) (pos.y - cam.y), (float) (pos.z - cam.z)).color(r, g, b, alpha);
        }
        BufferRenderer.drawWithGlobalProgram(buf.end());
    }

    private static void renderGlowSphere(
            MatrixStack ms, Vec3d cam, Vec3d pos, Vector3f right, Vector3f up,
            ColorRGBA color, float size, float glow, float animVal
    ) {
        ms.push();
        ms.translate(pos.x - cam.x, pos.y - cam.y, pos.z - cam.z);
        drawSphereMesh(ms, size * 1.0F, animVal * 0.8F, color);
        drawSphereMesh(ms, size * 1.3F, animVal * 0.3F, color);
        ms.pop();

        float r = color.getRed() / 255.0F;
        float g = color.getGreen() / 255.0F;
        float b = color.getBlue() / 255.0F;

        float px = (float) (pos.x - cam.x);
        float py = (float) (pos.y - cam.y);
        float pz = (float) (pos.z - cam.z);
        TargetESPCommon.drawSoftGlow(ms, px, py, pz, size * 3.5F, r, g, b, Math.min(1.0F, animVal * 0.18F * glow), right, up);
        TargetESPCommon.drawSoftGlow(ms, px, py, pz, size * 6.0F, r, g, b, Math.min(1.0F, animVal * 0.08F * glow), right, up);
        TargetESPCommon.drawSoftGlow(ms, px, py, pz, size * 10.0F, r, g, b, Math.min(1.0F, animVal * 0.03F * glow), right, up);
    }

    private static void drawSphereMesh(MatrixStack ms, float size, float alpha, ColorRGBA color) {
        Matrix4f matrix = ms.peek().getPositionMatrix();
        float r = color.getRed() / 255.0F;
        float g = color.getGreen() / 255.0F;
        float b = color.getBlue() / 255.0F;
        int lat = 8;
        int lon = 8;
        BufferBuilder buf = RenderSystem.renderThreadTesselator().begin(DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        for (int la = 0; la < lat; la++) {
            float t1 = (float) (la * Math.PI / lat);
            float t2 = (float) ((la + 1) * Math.PI / lat);
            for (int lo = 0; lo <= lon; lo++) {
                float phi = (float) (lo * 2.0 * Math.PI / lon);
                buf.vertex(matrix, (float) (size * Math.sin(t1) * Math.cos(phi)), (float) (size * Math.cos(t1)), (float) (size * Math.sin(t1) * Math.sin(phi)))
                        .color(r, g, b, alpha);
                buf.vertex(matrix, (float) (size * Math.sin(t2) * Math.cos(phi)), (float) (size * Math.cos(t2)), (float) (size * Math.sin(t2) * Math.sin(phi)))
                        .color(r, g, b, alpha);
            }
        }
        BufferRenderer.drawWithGlobalProgram(buf.end());
    }
}
