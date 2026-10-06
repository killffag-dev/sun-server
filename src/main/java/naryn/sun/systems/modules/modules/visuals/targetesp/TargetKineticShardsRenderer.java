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

public final class TargetKineticShardsRenderer implements IMinecraft {

    private static final int SHARDS = 7;
    private static final int TRAIL_STEPS = 20;

    private TargetKineticShardsRenderer() {
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
            g = MathHelper.lerp(critSurge * 0.65F, g, 0.25F);
            b = MathHelper.lerp(critSurge * 0.65F, b, 0.25F);
        }

        // Intense tip color (bright glowing apex, without any separate white spine)
        float cr = Math.min(1.0F, r * 0.30F + 0.70F);
        float cg = Math.min(1.0F, g * 0.30F + 0.70F);
        float cb = Math.min(1.0F, b * 0.30F + 0.70F);

        float hitSurge = target.hurtTime > 0 ? (target.hurtTime / 10.0F) : 0.0F;
        float activeGlow = glow * (1.0F + hitSurge * 0.5F + critSurge * 0.35F);
        float alpha = Math.min(1.0F, animVal * (0.90F + hitSurge * 0.10F)) * activeGlow;

        float orbitR = (target.getWidth() * 0.90F + 0.40F) * radiusMult;
        float tickTime = (System.currentTimeMillis() % 10000000L) / 50.0F;
        float rotTime = tickTime * speed * (0.05F + critSurge * 0.02F);

        float shardLen = 0.30F * (0.8F + size * 0.4F);
        float shardW   = 0.08F * (0.8F + size * 0.4F);
        float shardH   = 0.18F * (0.8F + size * 0.4F);
        float trailSpan = (float) (Math.PI * 0.38);

        Matrix4f matrix = ms.peek().getPositionMatrix();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        BufferBuilder buf = RenderSystem.renderThreadTesselator().begin(DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

        // Subtle facet highlights for 3D depth
        float rTop = Math.min(1.0F, r * 1.15F + 0.03F);
        float gTop = Math.min(1.0F, g * 1.15F + 0.03F);
        float bTop = Math.min(1.0F, b * 1.15F + 0.03F);

        float rBot = Math.max(0.0F, r * 0.85F);
        float gBot = Math.max(0.0F, g * 0.85F);
        float bBot = Math.max(0.0F, b * 0.85F);

        for (int i = 0; i < SHARDS; i++) {
            float headAngle = rotTime + (float) (i * 2.0 * Math.PI / SHARDS);

            // Shard Base Position (junction where shard head seamlessly transitions into the trail)
            float bob = (float) Math.sin(headAngle * 1.5F + i * 2.0F) * 0.22F;
            float baseX = cx + (float) Math.cos(headAngle) * orbitR;
            float baseY = cy + bob;
            float baseZ = cz + (float) Math.sin(headAngle) * orbitR;

            // Velocity tangent along orbit flight direction
            float tanX = -(float) Math.sin(headAngle);
            float tanZ =  (float) Math.cos(headAngle);
            float tanY = (float) (Math.cos(headAngle * 1.5F + i * 2.0F) * 0.22F * 1.5F / orbitR);
            float tanLen = (float) Math.sqrt(tanX * tanX + tanY * tanY + tanZ * tanZ);
            if (tanLen > 0.0001F) {
                tanX /= tanLen;
                tanY /= tanLen;
                tanZ /= tanLen;
            }

            // Normal pointing outward horizontally
            float normX = (float) Math.cos(headAngle);
            float normZ = (float) Math.sin(headAngle);

            // Razor-sharp leading tip of the crystal shard
            float tipX = baseX + tanX * shardLen;
            float tipY = baseY + tanY * shardLen;
            float tipZ = baseZ + tanZ * shardLen;

            // 4 Base Vertices of the shard head (Top, Bottom, Outer, Inner)
            float bTopX = baseX, bTopY = baseY + shardH * 0.5F, bTopZ = baseZ;
            float bBotX = baseX, bBotY = baseY - shardH * 0.5F, bBotZ = baseZ;
            float bOutX = baseX + normX * shardW * 0.5F, bOutY = baseY, bOutZ = baseZ + normZ * shardW * 0.5F;
            float bInX  = baseX - normX * shardW * 0.5F, bInY  = baseY, bInZ  = baseZ - normZ * shardW * 0.5F;

            float headAlpha = Math.min(1.0F, alpha * 0.95F);
            float tipAlpha  = Math.min(1.0F, alpha * 0.98F);

            // --- 1. SHARD HEAD: 4 Crisp Facets meeting at the sharp Tip ---
            // Facet 1: Top -> Outer -> Tip
            addTriangle(buf, matrix, bTopX, bTopY, bTopZ, bOutX, bOutY, bOutZ, tipX, tipY, tipZ,
                    rTop, gTop, bTop, headAlpha, r, g, b, headAlpha, cr, cg, cb, tipAlpha);

            // Facet 2: Outer -> Bottom -> Tip
            addTriangle(buf, matrix, bOutX, bOutY, bOutZ, bBotX, bBotY, bBotZ, tipX, tipY, tipZ,
                    r, g, b, headAlpha, rBot, gBot, bBot, headAlpha, cr, cg, cb, tipAlpha);

            // Facet 3: Bottom -> Inner -> Tip
            addTriangle(buf, matrix, bBotX, bBotY, bBotZ, bInX, bInY, bInZ, tipX, tipY, tipZ,
                    rBot, gBot, bBot, headAlpha, r, g, b, headAlpha, cr, cg, cb, tipAlpha);

            // Facet 4: Inner -> Top -> Tip
            addTriangle(buf, matrix, bInX, bInY, bInZ, bTopX, bTopY, bTopZ, tipX, tipY, tipZ,
                    r, g, b, headAlpha, rTop, gTop, bTop, headAlpha, cr, cg, cb, tipAlpha);

            // --- 2. VOLUMETRIC TRAIL: Seamless continuation from the 4 Base Vertices ---
            float prevTopX = bTopX, prevTopY = bTopY, prevTopZ = bTopZ;
            float prevBotX = bBotX, prevBotY = bBotY, prevBotZ = bBotZ;
            float prevOutX = bOutX, prevOutY = bOutY, prevOutZ = bOutZ;
            float prevInX  = bInX,  prevInY  = bInY,  prevInZ  = bInZ;
            float prevAlpha = headAlpha * 0.85F;

            for (int t = 1; t <= TRAIL_STEPS; t++) {
                float f = (float) t / TRAIL_STEPS;
                float a = headAngle - f * trailSpan;
                float tbob = (float) Math.sin(a * 1.5F + i * 2.0F) * 0.22F;

                float px = cx + (float) Math.cos(a) * orbitR;
                float py = cy + tbob;
                float pz = cz + (float) Math.sin(a) * orbitR;

                float nx = (float) Math.cos(a);
                float nz = (float) Math.sin(a);

                // Smooth aerodynamic taper to tail tip
                float decay = 1.0F - f;
                float curH = shardH * decay;
                float curW = shardW * decay;
                float curAlpha = headAlpha * 0.85F * decay * decay;

                float curTopX = px, curTopY = py + curH * 0.5F, curTopZ = pz;
                float curBotX = px, curBotY = py - curH * 0.5F, curBotZ = pz;
                float curOutX = px + nx * curW * 0.5F, curOutY = py, curOutZ = pz + nz * curW * 0.5F;
                float curInX  = px - nx * curW * 0.5F, curInY  = py, curInZ  = pz - nz * curW * 0.5F;

                // 4 longitudinal facets connecting the previous ring to the current ring
                // Top-Outer strip
                addQuad(buf, matrix,
                        prevTopX, prevTopY, prevTopZ, prevOutX, prevOutY, prevOutZ,
                        curOutX, curOutY, curOutZ, curTopX, curTopY, curTopZ,
                        rTop, gTop, bTop, prevAlpha, curAlpha);

                // Bottom-Outer strip
                addQuad(buf, matrix,
                        prevOutX, prevOutY, prevOutZ, prevBotX, prevBotY, prevBotZ,
                        curBotX, curBotY, curBotZ, curOutX, curOutY, curOutZ,
                        r, g, b, prevAlpha, curAlpha);

                // Bottom-Inner strip
                addQuad(buf, matrix,
                        prevBotX, prevBotY, prevBotZ, prevInX, prevInY, prevInZ,
                        curInX, curInY, curInZ, curBotX, curBotY, curBotZ,
                        rBot, gBot, bBot, prevAlpha, curAlpha);

                // Top-Inner strip
                addQuad(buf, matrix,
                        prevInX, prevInY, prevInZ, prevTopX, prevTopY, prevTopZ,
                        curTopX, curTopY, curTopZ, curInX, curInY, curInZ,
                        r, g, b, prevAlpha, curAlpha);

                prevTopX = curTopX; prevTopY = curTopY; prevTopZ = curTopZ;
                prevBotX = curBotX; prevBotY = curBotY; prevBotZ = curBotZ;
                prevOutX = curOutX; prevOutY = curOutY; prevOutZ = curOutZ;
                prevInX  = curInX;  prevInY  = curInY;  prevInZ  = curInZ;
                prevAlpha = curAlpha;
            }
        }

        BufferRenderer.drawWithGlobalProgram(buf.end());
    }

    private static void addTriangle(
            BufferBuilder buf, Matrix4f m,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            float r1, float g1, float b1, float a1,
            float r2, float g2, float b2, float a2,
            float r3, float g3, float b3, float a3
    ) {
        buf.vertex(m, x1, y1, z1).color(r1, g1, b1, a1);
        buf.vertex(m, x2, y2, z2).color(r2, g2, b2, a2);
        buf.vertex(m, x3, y3, z3).color(r3, g3, b3, a3);
    }

    private static void addQuad(
            BufferBuilder buf, Matrix4f m,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            float x4, float y4, float z4,
            float r, float g, float b,
            float a1, float a2
    ) {
        // Triangle 1: 1 -> 2 -> 3
        buf.vertex(m, x1, y1, z1).color(r, g, b, a1);
        buf.vertex(m, x2, y2, z2).color(r, g, b, a1);
        buf.vertex(m, x3, y3, z3).color(r, g, b, a2);

        // Triangle 2: 1 -> 3 -> 4
        buf.vertex(m, x1, y1, z1).color(r, g, b, a1);
        buf.vertex(m, x3, y3, z3).color(r, g, b, a2);
        buf.vertex(m, x4, y4, z4).color(r, g, b, a2);
    }
}
