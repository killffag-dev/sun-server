package naryn.sun.systems.modules.modules.visuals.targetesp;

import com.mojang.blaze3d.systems.RenderSystem;
import naryn.sun.utility.colors.ColorRGBA;
import naryn.sun.utility.interfaces.IMinecraft;
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

public final class TargetSonarEchoRenderer implements IMinecraft {

    private static final int WAVES = 4;

    private TargetSonarEchoRenderer() {
    }

    public static void draw(
            MatrixStack ms, LivingEntity target, ColorRGBA color, float animVal,
            float radiusMult, float speed, float glow, float size
    ) {
        if (animVal <= 0.02F) return;

        Camera camera = mc.gameRenderer.getCamera();
        Vec3d targetPos = TargetESPCommon.getRenderPos(target);
        Vec3d camPos = camera.getPos();

        float cx = (float) (targetPos.x - camPos.x);
        float cy = (float) (targetPos.y - camPos.y + target.getHeight() * 0.5F);
        float cz = (float) (targetPos.z - camPos.z);

        float r = color.getRed() / 255.0F;
        float g = color.getGreen() / 255.0F;
        float b = color.getBlue() / 255.0F;

        float cr = Math.min(1.0F, r * 0.25F + 0.75F);
        float cg = Math.min(1.0F, g * 0.25F + 0.75F);
        float cb = Math.min(1.0F, b * 0.25F + 0.75F);

        float hitSurge = target.hurtTime > 0 ? (target.hurtTime / 10.0F) : 0.0F;
        // Keep maxRadius constant to prevent waves from abruptly jerking/resetting on hit
        float maxRadius = (target.getWidth() * 1.4F + 0.7F) * radiusMult;
        float activeGlow = glow * (1.0F + hitSurge * 0.5F);
        float alpha = Math.min(1.0F, animVal * (0.88F + hitSurge * 0.12F));

        if (alpha <= 0.02F) return;

        float tickTime = (System.currentTimeMillis() % 10000000L) / 50.0F;

        Vector3f right = new Vector3f(1.0F, 0.0F, 0.0F).rotate(camera.getRotation());
        Vector3f up = new Vector3f(0.0F, 1.0F, 0.0F).rotate(camera.getRotation());

        Matrix4f matrix = ms.peek().getPositionMatrix();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        // 1. Central Sonic Ping Emitter Node
        BufferBuilder pingBuf = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        float pingPulse = 1.0F + 0.3F * (float) Math.sin(tickTime * speed * 0.3F);
        TargetESPCommon.drawGlowingNode(matrix, pingBuf,
                cx, cy, cz, right, up, 0.035F * pingPulse, 0.08F * pingPulse, r, g, b, cr, cg, cb, alpha, activeGlow);
        BufferRenderer.drawWithGlobalProgram(pingBuf.end());

        // 2. ONLY Horizontal Wavefronts (Clean expanding circular ripples in the XZ plane)
        float coreW = 0.020F * (0.8F + size * 0.4F);
        float haloW = coreW * 2.8F;

        for (int w = 0; w < WAVES; w++) {
            float phase = ((tickTime * speed * 0.025F + (float) w / WAVES) % 1.0F);
            float curR = maxRadius * phase;
            float waveAlpha = alpha * (1.0F - phase) * (float) Math.sin(phase * Math.PI) * 1.2F;
            if (waveAlpha <= 0.01F) continue;

            // Horizontal Wavefront (Pitch 90 deg -> flat XZ plane)
            TargetESPCommon.drawGlowingRing3D(matrix, cx, cy, cz, curR,
                    (float) Math.PI / 2.0F, 0.0F, 0.0F, coreW, haloW, r, g, b, cr, cg, cb, waveAlpha, activeGlow);
        }
    }
}
