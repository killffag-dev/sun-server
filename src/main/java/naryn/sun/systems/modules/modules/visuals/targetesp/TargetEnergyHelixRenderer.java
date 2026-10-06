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
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class TargetEnergyHelixRenderer implements IMinecraft {

    private static final int COMETS = 2;
    private static final int STEPS = 64;

    // Zero-allocation static buffers for per-frame render loop
    private static final float[] PX = new float[STEPS + 1];
    private static final float[] PY = new float[STEPS + 1];
    private static final float[] PZ = new float[STEPS + 1];
    private static final float[] DX = new float[STEPS + 1];
    private static final float[] DY = new float[STEPS + 1];
    private static final float[] DZ = new float[STEPS + 1];
    private static final float[] TRAIL_ALPHA = new float[STEPS + 1];
    private static final float[] TRAIL_W = new float[STEPS + 1];
    private static final float[] HEAD_X = new float[COMETS];
    private static final float[] HEAD_Y = new float[COMETS];
    private static final float[] HEAD_Z = new float[COMETS];

    private TargetEnergyHelixRenderer() {
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
        float feetY = (float) (targetPos.y - camPos.y);
        float cz = (float) (targetPos.z - camPos.z);
        float height = target.getHeight();

        float hpRatio = target.getMaxHealth() > 0 ? MathHelper.clamp(target.getHealth() / target.getMaxHealth(), 0.0F, 1.0F) : 1.0F;
        float critSurge = hpRatio < 0.35F ? (0.35F - hpRatio) / 0.35F : 0.0F;

        float r = color.getRed() / 255.0F;
        float g = color.getGreen() / 255.0F;
        float b = color.getBlue() / 255.0F;

        if (critSurge > 0.0F) {
            r = MathHelper.lerp(critSurge * 0.65F, r, 1.0F);
            g = MathHelper.lerp(critSurge * 0.65F, g, 0.25F);
            b = MathHelper.lerp(critSurge * 0.65F, b, 0.25F);
        }

        // Intense glowing core color
        float cr = Math.min(1.0F, r * 0.25F + 0.75F);
        float cg = Math.min(1.0F, g * 0.25F + 0.75F);
        float cb = Math.min(1.0F, b * 0.25F + 0.75F);

        float hitSurge = target.hurtTime > 0 ? (target.hurtTime / 10.0F) : 0.0F;
        float baseR = (target.getWidth() * 0.85F + 0.35F) * radiusMult;
        float activeGlow = glow * (1.0F + hitSurge * 0.5F);
        float alpha = Math.min(1.0F, animVal * (0.88F + hitSurge * 0.12F));
        if (alpha <= 0.02F) return;

        float tickTime = (System.currentTimeMillis() % 10000000L) / 50.0F;
        float rotTime = tickTime * speed * (0.05F + critSurge * 0.02F);

        Vector3f right = MathPool.vec3f(1.0F, 0.0F, 0.0F).rotate(camera.getRotation());
        Vector3f up = MathPool.vec3f(0.0F, 1.0F, 0.0F).rotate(camera.getRotation());

        float rx = right.x, ry = right.y, rz = right.z;
        float ux = up.x, uy = up.y, uz = up.z;

        Matrix4f matrix = ms.peek().getPositionMatrix();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        float maxTrailW = 0.14F * (0.8F + size * 0.4F);
        float orbitSpeed = 2.6F;
        float trailDuration = (float) Math.PI / orbitSpeed;

            for (int c = 0; c < COMETS; c++) {
                float offset = (float) (c * Math.PI);

                // 1. Calculate trajectory points from tail (t = 0) to head (t = STEPS)
                for (int t = 0; t <= STEPS; t++) {
                    float f = (float) t / STEPS; // 0.0 = tail tip, 1.0 = comet head
                    float pastTime = rotTime - (1.0F - f) * trailDuration;

                    float cycle = pastTime * (orbitSpeed * 0.5F) + offset;
                    float normY = ((float) Math.sin(cycle) + 1.0F) * 0.5F;
                    float curY = feetY + 0.15F + normY * (height - 0.20F);

                    float curAngle = pastTime * orbitSpeed + offset;
                    float curR = baseR * (0.82F + (float) Math.sin(normY * Math.PI) * 0.22F);

                    PX[t] = cx + (float) Math.cos(curAngle) * curR;
                    PY[t] = curY;
                    PZ[t] = cz + (float) Math.sin(curAngle) * curR;

                    // Strictly linear gradient: 0 at tail tip, 100% at head (no exponential drop-off, no dark gaps)
                    TRAIL_ALPHA[t] = alpha * f * activeGlow;
                    TRAIL_W[t] = maxTrailW * (0.20F + 0.80F * f);
                }

                HEAD_X[c] = PX[STEPS];
                HEAD_Y[c] = PY[STEPS];
                HEAD_Z[c] = PZ[STEPS];

                // 2. Camera-screen aligned billboarding (never edge-on, never twists, zero gap at any camera angle)
                float prevNr = 0.0F;
                float prevNu = 1.0F;

                for (int t = 0; t <= STEPS; t++) {
                    float tx, ty, tz;
                    if (t == 0) {
                        tx = PX[1] - PX[0];
                        ty = PY[1] - PY[0];
                        tz = PZ[1] - PZ[0];
                    } else if (t == STEPS) {
                        tx = PX[STEPS] - PX[STEPS - 1];
                        ty = PY[STEPS] - PY[STEPS - 1];
                        tz = PZ[STEPS] - PZ[STEPS - 1];
                    } else {
                        tx = PX[t + 1] - PX[t - 1];
                        ty = PY[t + 1] - PY[t - 1];
                        tz = PZ[t + 1] - PZ[t - 1];
                    }

                    // Project curve tangent onto camera screen plane
                    float tr = tx * rx + ty * ry + tz * rz;
                    float tu = tx * ux + ty * uy + tz * uz;

                    // Screen normal perpendicular to projected tangent
                    float nr = -tu;
                    float nu = tr;
                    float len = (float) Math.sqrt(nr * nr + nu * nu);

                    if (len > 0.0001F) {
                        float inv = 1.0F / len;
                        nr *= inv;
                        nu *= inv;
                    } else {
                        nr = prevNr;
                        nu = prevNu;
                    }

                    // Maintain normal continuity across adjacent steps
                    if (t > 0 && (nr * prevNr + nu * prevNu) < 0.0F) {
                        nr = -nr;
                        nu = -nu;
                    }
                    prevNr = nr;
                    prevNu = nu;

                    // World offset vector lying strictly in camera screen plane
                    DX[t] = nr * rx + nu * ux;
                    DY[t] = nr * ry + nu * uy;
                    DZ[t] = nr * rz + nu * uz;
                }

                // Pass 1: Soft Outer Neon Ribbon (Full face-on camera alignment)
                BufferBuilder outerBuf = RenderSystem.renderThreadTesselator().begin(DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
                for (int t = 0; t <= STEPS; t++) {
                    float x = PX[t], y = PY[t], z = PZ[t];
                    float hw = TRAIL_W[t] * 0.5F;
                    float a = Math.min(1.0F, TRAIL_ALPHA[t] * 0.50F);

                    float ox = DX[t] * hw;
                    float oy = DY[t] * hw;
                    float oz = DZ[t] * hw;

                    outerBuf.vertex(matrix, x - ox, y - oy, z - oz).color(r, g, b, a);
                    outerBuf.vertex(matrix, x + ox, y + oy, z + oz).color(r, g, b, a);
                }
                BufferRenderer.drawWithGlobalProgram(outerBuf.end());

                // Pass 2: Intense Searing Laser Core Spine
                BufferBuilder coreBuf = RenderSystem.renderThreadTesselator().begin(DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
                for (int t = 0; t <= STEPS; t++) {
                    float x = PX[t], y = PY[t], z = PZ[t];
                    float chw = TRAIL_W[t] * 0.18F;
                    float a = Math.min(1.0F, TRAIL_ALPHA[t] * 0.95F);

                    float ox = DX[t] * chw;
                    float oy = DY[t] * chw;
                    float oz = DZ[t] * chw;

                    coreBuf.vertex(matrix, x - ox, y - oy, z - oz).color(cr, cg, cb, a);
                    coreBuf.vertex(matrix, x + ox, y + oy, z + oz).color(cr, cg, cb, a);
                }
                BufferRenderer.drawWithGlobalProgram(coreBuf.end());
            }

            // 3. Glowing Radiant Comet Head Spheres
            BufferBuilder headBuf = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            for (int c = 0; c < COMETS; c++) {
                TargetESPCommon.drawGlowingNode(matrix, headBuf,
                        HEAD_X[c], HEAD_Y[c], HEAD_Z[c], right, up,
                        0.035F * size, 0.085F * size,
                        r, g, b, cr, cg, cb, alpha, activeGlow);
            }
            BufferRenderer.drawWithGlobalProgram(headBuf.end());
    }
}
