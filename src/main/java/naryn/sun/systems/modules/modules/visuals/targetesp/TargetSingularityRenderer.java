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
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class TargetSingularityRenderer implements IMinecraft {

    private static final int DISK_SEGMENTS = 48;
    private static final int INFALL_PARTICLES = 16;

    private TargetSingularityRenderer() {
    }

    public static void draw(
            MatrixStack ms, LivingEntity target, ColorRGBA color, float animVal,
            float radiusMult, float speed, float glow, float size
    ) {
        if (animVal <= 0.001F) return;

        Camera camera = mc.gameRenderer.getCamera();
        Vec3d targetPos = TargetESPCommon.getRenderPos(target);
        Vec3d camPos = camera.getPos();

        float cx = (float) (targetPos.x - camPos.x);
        float cy = (float) (targetPos.y - camPos.y + target.getHeight() * 0.5F);
        float cz = (float) (targetPos.z - camPos.z);

        float hpRatio = target.getMaxHealth() > 0 ? MathHelper.clamp(target.getHealth() / target.getMaxHealth(), 0.0F, 1.0F) : 1.0F;
        float critSurge = hpRatio < 0.35F ? (0.35F - hpRatio) / 0.35F : 0.0F;

        float r = color.getRed() / 255.0F;
        float g = color.getGreen() / 255.0F;
        float b = color.getBlue() / 255.0F;

        if (critSurge > 0.0F) {
            r = MathHelper.lerp(critSurge * 0.65F, r, 1.0F);
            g = MathHelper.lerp(critSurge * 0.65F, g, 0.2F);
            b = MathHelper.lerp(critSurge * 0.65F, b, 0.2F);
        }

        float cr = Math.min(1.0F, r * 0.25F + 0.75F);
        float cg = Math.min(1.0F, g * 0.25F + 0.75F);
        float cb = Math.min(1.0F, b * 0.25F + 0.75F);

        float hitSurge = target.hurtTime > 0 ? (target.hurtTime / 10.0F) : 0.0F;
        float baseRadius = (target.getWidth() * 0.85F + 0.40F) * radiusMult * (1.0F + hitSurge * 0.15F);
        float activeGlow = glow * (1.0F + hitSurge * 0.7F + critSurge * 0.4F);
        float alpha = Math.min(1.0F, animVal * (0.85F + hitSurge * 0.15F));

        float tickTime = (System.currentTimeMillis() % 10000000L) / 50.0F;
        float t = tickTime * speed * (0.04F + critSurge * 0.03F);

        Vector3f right = new Vector3f(1.0F, 0.0F, 0.0F).rotate(camera.getRotation());
        Vector3f up = new Vector3f(0.0F, 1.0F, 0.0F).rotate(camera.getRotation());

        Matrix4f matrix = ms.peek().getPositionMatrix();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        // 1. Swirling Relativistic Accretion Disk (Horizontal Plane with Subtle Tilt)
        BufferBuilder diskBuf = RenderSystem.renderThreadTesselator().begin(DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        float diskInner = baseRadius * 0.35F;
        float diskOuter = baseRadius * 1.30F;
        float tiltX = 0.25F + (float) Math.sin(t * 0.5F) * 0.04F;
        float cosTilt = (float) Math.cos(tiltX), sinTilt = (float) Math.sin(tiltX);

        for (int i = 0; i <= DISK_SEGMENTS; i++) {
            float frac = (float) i / DISK_SEGMENTS;
            float angle = frac * (float) Math.PI * 2.0F - t * 1.6F;
            float cosA = (float) Math.cos(angle);
            float sinA = (float) Math.sin(angle);

            float doppler = 0.60F + 0.40F * sinA;
            float innerA = alpha * 0.95F * doppler;
            float outerA = alpha * 0.12F * doppler * activeGlow;

            float inX = cosA * diskInner;
            float inY = sinA * diskInner * sinTilt;
            float inZ = sinA * diskInner * cosTilt;

            float outX = cosA * diskOuter;
            float outY = sinA * diskOuter * sinTilt;
            float outZ = sinA * diskOuter * cosTilt;

            diskBuf.vertex(matrix, cx + inX, cy + inY, cz + inZ).color(cr, cg, cb, innerA);
            diskBuf.vertex(matrix, cx + outX, cy + outY, cz + outZ).color(r, g, b, outerA);
        }
        BufferRenderer.drawWithGlobalProgram(diskBuf.end());

        // 2. Infalling Spiral Particles (Tracing into the Center of the Accretion Disk)
        BufferBuilder nodeBuf = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        for (int p = 0; p < INFALL_PARTICLES; p++) {
            float phase = ((tickTime * speed * 0.022F + (float) p / INFALL_PARTICLES) % 1.0F);
            float quadPhase = phase * phase;
            float curDist = diskOuter * (1.0F - quadPhase * 0.85F) + diskInner * 0.2F;
            float spiralA = t * 2.4F + phase * (float) Math.PI * 7.0F;

            float px = cx + (float) Math.cos(spiralA) * curDist;
            float py = cy + (float) Math.sin(spiralA) * curDist * sinTilt;
            float pz = cz + (float) Math.sin(spiralA) * curDist * cosTilt;
            float pAlpha = alpha * (float) Math.sin(phase * Math.PI) * (1.0F + critSurge * 0.4F);

            TargetESPCommon.drawGlowingNode(matrix, nodeBuf, px, py, pz, right, up,
                    0.024F, 0.060F, r, g, b, cr, cg, cb, pAlpha, activeGlow);
        }
        BufferRenderer.drawWithGlobalProgram(nodeBuf.end());
    }
}
