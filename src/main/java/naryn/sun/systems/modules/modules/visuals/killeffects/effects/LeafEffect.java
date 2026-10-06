package naryn.sun.systems.modules.modules.visuals.killeffects.effects;

import static naryn.sun.systems.modules.modules.visuals.killeffects.KillEffectGeometry.addThickSegment;
import static naryn.sun.systems.modules.modules.visuals.killeffects.KillEffectGeometry.randomPointInBox;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import naryn.sun.systems.modules.modules.visuals.killeffects.KillEffectModel;
import naryn.sun.utility.colors.ColorRGBA;
import naryn.sun.utility.math.MathUtility;
import naryn.sun.utility.render.RenderUtility;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/**
 * Эффект смерти "Лист": частицы-листья разлетаются взрывом от центра хитбокса,
 * гуляют в воздухе (фигуры Лиссажу), падают и оседают на землю, после чего гаснут.
 */
public class LeafEffect implements KillEffectModel {

    private static final long MAX_LIFETIME_MS = 2500L;
    private static final long AIR_FADE_MS = 400L;
    private static final long LAND_FADE_MS = 250L;
    private static final float RAMP_TIME_SEC = 0.35F;

    // Контур листа в локальных координатах (единичный размер, до умножения на size).
    private static final float[][] LEAF_OUTLINE = {
        {0.0F, 1.0F}, {0.35F, 0.65F}, {0.5F, 0.25F}, {0.4F, -0.15F}, {0.15F, -0.45F},
        {0.0F, -0.6F}, {-0.15F, -0.45F}, {-0.4F, -0.15F}, {-0.5F, 0.25F}, {-0.35F, 0.65F}
    };
    private static final float[] LEAF_CENTER = {0.0F, 0.15F};

    private static final float[][][] LEAF_VEINS = {
        {{0.0F, -0.6F}, {0.0F, 1.0F}},
        {{0.0F, 0.6F}, {0.35F, 0.65F}}, {{0.0F, 0.6F}, {-0.35F, 0.65F}},
        {{0.0F, 0.1F}, {0.5F, 0.25F}}, {{0.0F, 0.1F}, {-0.5F, 0.25F}},
        {{0.0F, -0.3F}, {0.4F, -0.15F}}, {{0.0F, -0.3F}, {-0.4F, -0.15F}}
    };

    private final List<Leaf> leaves = new ArrayList<>();

    @Override
    public void onDeath(Box box, ColorRGBA baseColor, int particleCount) {
        double centerX = (box.minX + box.maxX) / 2.0;
        double centerZ = (box.minZ + box.maxZ) / 2.0;

        for (int i = 0; i < particleCount; i++) {
            Vec3d spawnPos = randomPointInBox(box);
            this.leaves.add(new Leaf(spawnPos, centerX, centerZ, baseColor));
        }
    }

    @Override
    public void tick(long now) {
        Iterator<Leaf> leafIterator = this.leaves.iterator();
        while (leafIterator.hasNext()) {
            Leaf leaf = leafIterator.next();
            boolean expiredByAge = now - leaf.spawnTime >= MAX_LIFETIME_MS;
            boolean expiredByLanding = leaf.landed && now - leaf.landTime >= LAND_FADE_MS;
            if (expiredByAge || expiredByLanding) {
                leafIterator.remove();
            }
        }
    }

    @Override
    public void render(BufferBuilder builder, MatrixStack ms, long now) {
        for (Leaf leaf : this.leaves) {
            leaf.update(now);

            float alpha = leaf.calculateAlpha(now);
            if (alpha <= 0.0F) {
                continue;
            }

            ms.push();
            RenderUtility.prepareMatrices(ms, leaf.renderPos);
            ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(leaf.renderRotX));
            ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(leaf.renderRotY));
            ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(leaf.renderRotZ));
            Matrix4f matrix = ms.peek().getPositionMatrix();

            this.addLeafGeometry(builder, matrix, leaf.size, leaf.color.withAlpha(255.0F * alpha), leaf.veinColor.withAlpha(255.0F * alpha), leaf.outlineColor.withAlpha(255.0F * alpha));

            ms.pop();
        }
    }

    @Override
    public boolean isEmpty() {
        return this.leaves.isEmpty();
    }

    @Override
    public long getLifetimeMs() {
        return MAX_LIFETIME_MS;
    }

    private void addLeafGeometry(BufferBuilder builder, Matrix4f matrix, float size, ColorRGBA fillColor, ColorRGBA veinColor, ColorRGBA outlineColor) {
        int fillRgb = fillColor.getRGB();
        int points = LEAF_OUTLINE.length;

        for (int i = 0; i < points; i++) {
            float[] p1 = LEAF_OUTLINE[i];
            float[] p2 = LEAF_OUTLINE[(i + 1) % points];
            builder.vertex(matrix, LEAF_CENTER[0] * size, LEAF_CENTER[1] * size, 0.0F).color(fillRgb);
            builder.vertex(matrix, p1[0] * size, p1[1] * size, 0.0F).color(fillRgb);
            builder.vertex(matrix, p2[0] * size, p2[1] * size, 0.0F).color(fillRgb);
        }

        float veinHalfWidth = size * 0.028F;
        int veinRgb = veinColor.getRGB();
        for (float[][] vein : LEAF_VEINS) {
            addThickSegment(builder, matrix, vein[0][0] * size, vein[0][1] * size, vein[1][0] * size, vein[1][1] * size, veinHalfWidth, veinRgb);
        }

        float outlineHalfWidth = size * 0.022F;
        int outlineRgb = outlineColor.getRGB();
        for (int i = 0; i < points; i++) {
            float[] p1 = LEAF_OUTLINE[i];
            float[] p2 = LEAF_OUTLINE[(i + 1) % points];
            addThickSegment(builder, matrix, p1[0] * size, p1[1] * size, p2[0] * size, p2[1] * size, outlineHalfWidth, outlineRgb);
        }
    }

    private static class Leaf {
        final Vec3d spawnPos;
        final ColorRGBA color;
        final ColorRGBA veinColor;
        final ColorRGBA outlineColor;
        final long spawnTime;
        final float size;

        // "взрыв" - начальный импульс наружу от центра хитбокса, гасится сопротивлением воздуха
        final float burstDirX;
        final float burstDirZ;
        final float burstSpeed;
        final float burstDecay;

        // "гуляние" в воздухе поверх взрыва (сумма двух синусоид на ось - плавная фигура Лиссажу, не зигзаг)
        final float ampAx;
        final float ampBx;
        final float freqAx;
        final float freqBx;
        final float phaseAx;
        final float phaseBx;

        final float ampAz;
        final float ampBz;
        final float freqAz;
        final float freqBz;
        final float phaseAz;
        final float phaseBz;

        final float fallSpeed;
        final float spinRateY;
        final float tiltAmp;
        final float tiltFreq;
        final float tiltPhaseX;
        final float tiltPhaseZ;

        boolean landed = false;
        long landTime = 0L;

        Vec3d renderPos;
        float renderRotX;
        float renderRotY;
        float renderRotZ;

        Leaf(Vec3d pos, double hitboxCenterX, double hitboxCenterZ, ColorRGBA baseColor) {
            this.spawnPos = pos;
            this.renderPos = pos;
            this.color = baseColor;
            this.veinColor = ColorRGBA.fromHSB(baseColor.getHue(), Math.min(1.0F, baseColor.getSaturation() * 1.05F), baseColor.getBrightness() * 0.55F)
                .withAlpha(baseColor.getAlpha());
            this.outlineColor = ColorRGBA.fromHSB(baseColor.getHue(), Math.min(1.0F, baseColor.getSaturation() * 1.1F), baseColor.getBrightness() * 0.3F)
                .withAlpha(baseColor.getAlpha());
            this.spawnTime = System.currentTimeMillis();
            this.size = MathUtility.random(0.14F, 0.24F);

            double dirX = pos.x - hitboxCenterX;
            double dirZ = pos.z - hitboxCenterZ;
            double dirLength = Math.sqrt(dirX * dirX + dirZ * dirZ);
            if (dirLength < 1.0E-4) {
                float randomAngle = MathUtility.random(0.0F, (float) (Math.PI * 2.0));
                dirX = Math.cos(randomAngle);
                dirZ = Math.sin(randomAngle);
            } else {
                dirX /= dirLength;
                dirZ /= dirLength;
            }
            this.burstDirX = (float) dirX;
            this.burstDirZ = (float) dirZ;
            this.burstSpeed = MathUtility.random(2.0F, 4.0F);
            this.burstDecay = MathUtility.random(3.0F, 5.5F);

            this.ampAx = MathUtility.random(0.07F, 0.16F);
            this.ampBx = MathUtility.random(0.04F, 0.10F);
            this.freqAx = MathUtility.random(3.2F, 5.0F);
            this.freqBx = MathUtility.random(1.6F, 2.8F);
            this.phaseAx = MathUtility.random(0.0F, (float) (Math.PI * 2.0));
            this.phaseBx = MathUtility.random(0.0F, (float) (Math.PI * 2.0));

            this.ampAz = MathUtility.random(0.07F, 0.16F);
            this.ampBz = MathUtility.random(0.04F, 0.10F);
            this.freqAz = MathUtility.random(2.8F, 4.6F);
            this.freqBz = MathUtility.random(1.4F, 2.4F);
            this.phaseAz = MathUtility.random(0.0F, (float) (Math.PI * 2.0));
            this.phaseBz = MathUtility.random(0.0F, (float) (Math.PI * 2.0));

            this.fallSpeed = MathUtility.random(0.5F, 0.85F);
            this.spinRateY = MathUtility.random(50.0F, 110.0F) * (MathUtility.random(0.0F, 1.0F) < 0.5F ? -1.0F : 1.0F);
            this.tiltAmp = MathUtility.random(12.0F, 22.0F);
            this.tiltFreq = MathUtility.random(2.0F, 3.2F);
            this.tiltPhaseX = MathUtility.random(0.0F, (float) (Math.PI * 2.0));
            this.tiltPhaseZ = MathUtility.random(0.0F, (float) (Math.PI * 2.0));

            this.renderRotX = MathUtility.random(0.0F, 360.0F);
            this.renderRotY = MathUtility.random(0.0F, 360.0F);
            this.renderRotZ = MathUtility.random(0.0F, 360.0F);
        }

        void update(long now) {
            if (this.landed) {
                return;
            }

            float t = (now - this.spawnTime) / 1000.0F;
            if (t < 0.0F) {
                t = 0.0F;
            }

            float fallDistance;
            if (t < RAMP_TIME_SEC) {
                fallDistance = this.fallSpeed * (t * t) / (2.0F * RAMP_TIME_SEC);
            } else {
                fallDistance = this.fallSpeed * (t - RAMP_TIME_SEC / 2.0F);
            }

            // смещение от взрыва: v0/k * (1 - e^(-k*t)) - быстрый разлёт, потом асимптотически останавливается
            double burstDisplacement = this.burstSpeed / this.burstDecay * (1.0 - Math.exp(-this.burstDecay * t));

            double meanderX = this.ampAx * Math.sin(this.freqAx * t + this.phaseAx) + this.ampBx * Math.sin(this.freqBx * t + this.phaseBx);
            double meanderZ = this.ampAz * Math.sin(this.freqAz * t + this.phaseAz) + this.ampBz * Math.sin(this.freqBz * t + this.phaseBz);

            double candidateX = this.spawnPos.x + this.burstDirX * burstDisplacement + meanderX;
            double candidateY = this.spawnPos.y - fallDistance;
            double candidateZ = this.spawnPos.z + this.burstDirZ * burstDisplacement + meanderZ;

            BlockPos below = BlockPos.ofFloored(candidateX, candidateY - 0.05, candidateZ);
            if (this.isSolid(below)) {
                double top = below.getY() + 1.0;
                if (candidateY <= top) {
                    this.renderPos = new Vec3d(candidateX, top + 0.01, candidateZ);
                    this.landed = true;
                    this.landTime = now;
                    return;
                }
            }

            this.renderPos = new Vec3d(candidateX, candidateY, candidateZ);
            this.renderRotY = (this.spinRateY * t) % 360.0F;
            this.renderRotX = (float) (Math.sin(this.tiltFreq * t + this.tiltPhaseX) * this.tiltAmp);
            this.renderRotZ = (float) (Math.sin(this.tiltFreq * t * 0.85F + this.tiltPhaseZ) * this.tiltAmp);
        }

        float calculateAlpha(long now) {
            if (this.landed) {
                long landAge = now - this.landTime;
                if (landAge >= LAND_FADE_MS) {
                    return 0.0F;
                }
                return 1.0F - (float) landAge / (float) LAND_FADE_MS;
            }

            long age = now - this.spawnTime;
            if (age >= MAX_LIFETIME_MS) {
                return 0.0F;
            } else if (age >= MAX_LIFETIME_MS - AIR_FADE_MS) {
                return 1.0F - (float) (age - (MAX_LIFETIME_MS - AIR_FADE_MS)) / (float) AIR_FADE_MS;
            } else {
                return 1.0F;
            }
        }

        private boolean isSolid(BlockPos pos) {
            MinecraftClient mc = MinecraftClient.getInstance();
            return !mc.world.getBlockState(pos).getCollisionShape(mc.world, pos).isEmpty();
        }
    }
}