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

public final class TargetSoulsRenderer implements IMinecraft {

    private static final int WISP_COUNT = 3;
    private static final int TRAIL_SEGMENTS = 18;
    private static final float[] WISP_HEAD_X = new float[WISP_COUNT];
    private static final float[] WISP_HEAD_Y = new float[WISP_COUNT];
    private static final float[] WISP_HEAD_Z = new float[WISP_COUNT];

    private TargetSoulsRenderer() {
    }

    public static void draw(
            MatrixStack ms, LivingEntity target, ColorRGBA color, float animVal, float moveVal,
            float radiusMult, float sizeScale, float speed, float glow
    ) {
        if (animVal <= 0.001F) {
            return;
        }

        Camera camera = mc.gameRenderer.getCamera();
        Vec3d targetPos = TargetESPCommon.getRenderPos(target);
        Vec3d camPos = camera.getPos();

        float r = color.getRed() / 255.0F;
        float g = color.getGreen() / 255.0F;
        float b = color.getBlue() / 255.0F;

        float baseRadius = (target.getWidth() * 0.85F + 0.35F) * radiusMult;
        float targetHeight = target.getHeight();
        float tickTime = (System.currentTimeMillis() % 1000000L) / 50.0F;
        float effectiveSpeed = speed * 1.5F;

        Vector3f right = MathPool.vec3f(1.0F, 0.0F, 0.0F).rotate(camera.getRotation());
        Vector3f up = MathPool.vec3f(0.0F, 1.0F, 0.0F).rotate(camera.getRotation());
        Matrix4f matrix = ms.peek().getPositionMatrix();

        float coreSize = sizeScale * 1.8F;

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        // Pass 1: Draw spectral ribbon trail segments (DrawMode.LINES)
            BufferBuilder lineBuf = RenderSystem.renderThreadTesselator().begin(DrawMode.LINES, VertexFormats.POSITION_COLOR);
            for (int w = 0; w < WISP_COUNT; w++) {
                float wispPhase = (float) (w * 2.0 * Math.PI / WISP_COUNT);

                float prevX = 0.0F;
                float prevY = 0.0F;
                float prevZ = 0.0F;
                float prevAlpha = 0.0F;

                for (int s = 0; s < TRAIL_SEGMENTS; s++) {
                    float t = (tickTime - s * 1.4F) * effectiveSpeed * 0.04F + moveVal * 0.005F;
                    float progress = 1.0F - (float) s / TRAIL_SEGMENTS;
                    float alpha = progress * progress * animVal * 0.75F;

                    float angle = t + wispPhase;
                    float radWave = baseRadius * (1.0F + 0.25F * (float) Math.sin(t * 1.7F + wispPhase));
                    float wx = (float) Math.cos(angle) * radWave;
                    float wz = (float) Math.sin(angle) * radWave;
                    float wy = targetHeight * 0.5F + (float) Math.sin(t * 2.3F + wispPhase) * (targetHeight * 0.38F);

                    float px = (float) (targetPos.x - camPos.x) + wx;
                    float py = (float) (targetPos.y - camPos.y) + wy;
                    float pz = (float) (targetPos.z - camPos.z) + wz;

                    if (s == 0) {
                        WISP_HEAD_X[w] = px;
                        WISP_HEAD_Y[w] = py;
                        WISP_HEAD_Z[w] = pz;
                    } else {
                        lineBuf.vertex(matrix, prevX, prevY, prevZ).color(r, g, b, prevAlpha);
                        lineBuf.vertex(matrix, px, py, pz).color(r, g, b, alpha);
                    }

                    prevX = px;
                    prevY = py;
                    prevZ = pz;
                    prevAlpha = alpha;
                }
            }
            BufferRenderer.drawWithGlobalProgram(lineBuf.end());

            // Pass 2: Draw glowing soul cores and aura
            for (int w = 0; w < WISP_COUNT; w++) {
                float px = WISP_HEAD_X[w];
                float py = WISP_HEAD_Y[w];
                float pz = WISP_HEAD_Z[w];

                // Bright soul core
                TargetESPCommon.drawSoftGlow(ms, px, py, pz, coreSize * 1.2F, r, g, b, Math.min(1.0F, animVal * 0.85F), right, up);

                // Ethereal diffuse halo
                if (glow > 0.0F) {
                    TargetESPCommon.drawSoftGlow(ms, px, py, pz, coreSize * 3.5F, r, g, b, Math.min(1.0F, animVal * 0.25F * glow), right, up);
                    TargetESPCommon.drawSoftGlow(ms, px, py, pz, coreSize * 6.5F, r, g, b, Math.min(1.0F, animVal * 0.08F * glow), right, up);
                }

                // Rising spectral ember motes
                for (int m = 1; m <= 2; m++) {
                    float mt = tickTime * 0.12F + w * 17.0F + m * 31.0F;
                    float mx = px + (float) Math.cos(mt) * (coreSize * 0.9F);
                    float my = py + ((tickTime * 0.04F + m * 0.5F) % 1.0F) * (targetHeight * 0.25F);
                    float mz = pz + (float) Math.sin(mt) * (coreSize * 0.9F);
                    float emberAlpha = (1.0F - ((tickTime * 0.04F + m * 0.5F) % 1.0F)) * animVal * 0.45F;
                    TargetESPCommon.drawSoftGlow(ms, mx, my, mz, coreSize * 0.5F, r, g, b, emberAlpha, right, up);
                }
            }
    }
}
