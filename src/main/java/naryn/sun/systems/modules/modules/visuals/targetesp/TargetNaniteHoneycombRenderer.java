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
import naryn.sun.utility.math.MathPool;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class TargetNaniteHoneycombRenderer implements IMinecraft {

    private static final int RINGS = 5;
    private static final int CELLS_PER_RING = 8;

    private TargetNaniteHoneycombRenderer() {
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

        float cr = Math.min(1.0F, r * 0.25F + 0.75F);
        float cg = Math.min(1.0F, g * 0.25F + 0.75F);
        float cb = Math.min(1.0F, b * 0.25F + 0.75F);

        float hitSurge = target.hurtTime > 0 ? (target.hurtTime / 10.0F) : 0.0F;
        float baseShieldRadius = (target.getWidth() * 0.90F + 0.35F) * radiusMult * (1.0F + hitSurge * 0.16F);
        float activeGlow = glow * (1.0F + hitSurge * 0.8F + critSurge * 0.4F);
        float alpha = Math.min(1.0F, animVal * (0.85F + hitSurge * 0.15F));

        float tickTime = (System.currentTimeMillis() % 10000000L) / 50.0F;
        float rotTime = tickTime * speed * (0.035F + critSurge * 0.02F);
        float hexR = 0.115F * (0.8F + size * 0.4F);

        Vector3f right = MathPool.vec3f(1.0F, 0.0F, 0.0F).rotate(camera.getRotation());
        Vector3f up = MathPool.vec3f(0.0F, 1.0F, 0.0F).rotate(camera.getRotation());

        Matrix4f matrix = ms.peek().getPositionMatrix();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        float ringSpacing = (height + 0.3F) / (RINGS + 1);
        float scanPhase = ((tickTime * speed * 0.04F) % 1.0F);
        float scanY = feetY + 0.1F + scanPhase * (height + 0.2F);

        // 1. Semi-transparent Forcefield Hexagonal Fill Quads (Bi-conic Ellipsoid Cocoon)
        BufferBuilder faceBuf = RenderSystem.renderThreadTesselator().begin(DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

        for (int ring = 0; ring < RINGS; ring++) {
            float yNorm = (ring + 0.5F) / RINGS;
            float curvature = 1.0F - 0.28F * (float) Math.pow((yNorm - 0.5F) * 2.0F, 2);
            float currentRadius = baseShieldRadius * curvature;

            float ringY = feetY + 0.15F + (ring + 0.5F) * ringSpacing;
            float stagger = (ring % 2 == 0) ? 0.0F : (float) (Math.PI / CELLS_PER_RING);

            float scanDist = Math.abs(ringY - scanY);
            float scanBoost = Math.max(0.0F, 1.0F - scanDist / 0.40F);

            for (int c = 0; c < CELLS_PER_RING; c++) {
                float angle = rotTime + stagger + (float) (c * 2.0 * Math.PI / CELLS_PER_RING);
                float wave = (float) Math.sin(angle * 2.0F + tickTime * speed * 0.08F + ring * 0.8F);
                float cellA = alpha * Math.max(0.08F, (0.35F + 0.65F * wave)) * (0.65F + hitSurge * 0.35F);

                float centerCellX = cx + (float) Math.cos(angle) * currentRadius;
                float centerCellY = ringY;
                float centerCellZ = cz + (float) Math.sin(angle) * currentRadius;

                float tx = -(float) Math.sin(angle);
                float tz = (float) Math.cos(angle);

                float fillA = Math.min(0.40F, (cellA * 0.18F + scanBoost * 0.25F) * activeGlow);
                for (int v = 0; v < 6; v++) {
                    float va1 = (float) (v * Math.PI / 3.0);
                    float va2 = (float) ((v + 1) * Math.PI / 3.0);

                    float x1 = centerCellX + tx * (float) Math.cos(va1) * hexR;
                    float y1 = centerCellY + (float) Math.sin(va1) * hexR;
                    float z1 = centerCellZ + tz * (float) Math.cos(va1) * hexR;

                    float x2 = centerCellX + tx * (float) Math.cos(va2) * hexR;
                    float y2 = centerCellY + (float) Math.sin(va2) * hexR;
                    float z2 = centerCellZ + tz * (float) Math.cos(va2) * hexR;

                    faceBuf.vertex(matrix, centerCellX, centerCellY, centerCellZ).color(cr, cg, cb, fillA * 1.2F);
                    faceBuf.vertex(matrix, x1, y1, z1).color(r, g, b, fillA * 0.5F);
                    faceBuf.vertex(matrix, x2, y2, z2).color(r, g, b, fillA * 0.5F);
                }
            }
        }
        BufferRenderer.drawWithGlobalProgram(faceBuf.end());

        // 2. Camera-Facing Billboard Hex Struts & Nanite Corner Nodes
        BufferBuilder strutBuf = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        float strutCore = 0.013F * (0.8F + size * 0.4F);
        float strutHalo = strutCore * 2.6F;

        for (int ring = 0; ring < RINGS; ring++) {
            float yNorm = (ring + 0.5F) / RINGS;
            float curvature = 1.0F - 0.28F * (float) Math.pow((yNorm - 0.5F) * 2.0F, 2);
            float currentRadius = baseShieldRadius * curvature;

            float ringY = feetY + 0.15F + (ring + 0.5F) * ringSpacing;
            float stagger = (ring % 2 == 0) ? 0.0F : (float) (Math.PI / CELLS_PER_RING);

            float scanDist = Math.abs(ringY - scanY);
            float scanBoost = Math.max(0.0F, 1.0F - scanDist / 0.40F);

            for (int c = 0; c < CELLS_PER_RING; c++) {
                float angle = rotTime + stagger + (float) (c * 2.0 * Math.PI / CELLS_PER_RING);
                float wave = (float) Math.sin(angle * 2.0F + tickTime * speed * 0.08F + ring * 0.8F);
                float cellA = alpha * Math.min(1.0F, Math.max(0.15F, (0.40F + 0.60F * wave) + scanBoost * 0.5F));

                float centerCellX = cx + (float) Math.cos(angle) * currentRadius;
                float centerCellY = ringY;
                float centerCellZ = cz + (float) Math.sin(angle) * currentRadius;

                float tx = -(float) Math.sin(angle);
                float tz = (float) Math.cos(angle);

                for (int v = 0; v < 6; v++) {
                    float va1 = (float) (v * Math.PI / 3.0);
                    float va2 = (float) ((v + 1) * Math.PI / 3.0);

                    float x1 = centerCellX + tx * (float) Math.cos(va1) * hexR;
                    float y1 = centerCellY + (float) Math.sin(va1) * hexR;
                    float z1 = centerCellZ + tz * (float) Math.cos(va1) * hexR;

                    float x2 = centerCellX + tx * (float) Math.cos(va2) * hexR;
                    float y2 = centerCellY + (float) Math.sin(va2) * hexR;
                    float z2 = centerCellZ + tz * (float) Math.cos(va2) * hexR;

                    TargetESPCommon.drawGlowingBillboardSegment(matrix, strutBuf,
                            x1, y1, z1, x2, y2, z2,
                            (float) camPos.x, (float) camPos.y, (float) camPos.z,
                            strutCore, strutHalo, r, g, b, cr, cg, cb, cellA, activeGlow);

                    if (wave > 0.25F || scanBoost > 0.3F) {
                        float nodeCore = 0.018F + scanBoost * 0.008F;
                        TargetESPCommon.drawGlowingNode(matrix, strutBuf, x1, y1, z1, right, up,
                                nodeCore, nodeCore * 2.2F, r, g, b, cr, cg, cb, cellA, activeGlow);
                    }
                }
            }
        }
        BufferRenderer.drawWithGlobalProgram(strutBuf.end());
    }
}
