package naryn.sun.systems.modules.modules.visuals.prediction;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.List;
import naryn.sun.access.MCWorldAccess;
import naryn.sun.utility.colors.ColorRGBA;
import naryn.sun.utility.render.Draw3DUtility;
import naryn.sun.utility.render.RenderUtility;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.joml.Matrix4f;

public final class PredictionLandingRenderer {

    private static final List<BlockFace> FACES = new ArrayList<>(64);
    private static final LongOpenHashSet SEEN_KEYS = new LongOpenHashSet(64);
    private static final List<Box> ENTITY_BOXES = new ArrayList<>(8);

    private PredictionLandingRenderer() {
    }

    public static void render(MatrixStack ms, List<TrajectoryData> trajectories, ColorRGBA userColor) {
        FACES.clear();
        SEEN_KEYS.clear();
        ENTITY_BOXES.clear();

        for (TrajectoryData data : trajectories) {
            // Попадание в сущность
            if (data.collidedEntity() != null) {
                ENTITY_BOXES.add(data.collidedEntity().getBoundingBox());
                continue;
            }

            // Попадание в блок или активная область на земле
            if (data.hitResult() != null && data.hitResult().getType() == HitResult.Type.BLOCK) {
                BlockHitResult bHit = data.hitResult();
                BlockPos hitPos = bHit.getBlockPos();
                Direction hitSide = bHit.getSide();

                if (data.splashRadius() > 0.0F) {
                    collectAoeBlockSurfaces(hitPos, hitSide, data.splashRadius());
                } else {
                    addFace(hitPos, hitSide);
                }
            }
        }

        if (FACES.isEmpty() && ENTITY_BOXES.isEmpty()) {
            return;
        }

        ColorRGBA blockFillColor = userColor.withAlpha(55.0F);
        ColorRGBA blockOutlineColor = userColor.withAlpha(180.0F);
        Matrix4f matrix = ms.peek().getPositionMatrix();

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        // Проход 1: Заливка полупрозрачными квадами
        BufferBuilder quadsBuffer = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        for (int i = 0; i < FACES.size(); i++) {
            BlockFace face = FACES.get(i);
            addBlockFaceQuadVertices(quadsBuffer, matrix, face.pos(), face.side(), blockFillColor);
        }
        for (int i = 0; i < ENTITY_BOXES.size(); i++) {
            Draw3DUtility.renderFilledBox(ms, quadsBuffer, ENTITY_BOXES.get(i), userColor.mulAlpha(0.35F));
        }
        RenderUtility.buildBuffer(quadsBuffer);

        // Проход 2: Четкая контурная обводка
        BufferBuilder linesBuffer = RenderSystem.renderThreadTesselator().begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        for (int i = 0; i < FACES.size(); i++) {
            BlockFace face = FACES.get(i);
            addBlockFaceOutlineVertices(linesBuffer, matrix, face.pos(), face.side(), blockOutlineColor);
        }
        for (int i = 0; i < ENTITY_BOXES.size(); i++) {
            Draw3DUtility.renderOutlinedBox(ms, linesBuffer, ENTITY_BOXES.get(i), userColor);
        }
        RenderUtility.buildBuffer(linesBuffer);
    }

    private static void collectAoeBlockSurfaces(BlockPos centerPos, Direction hitSide, float radius) {
        World world = MCWorldAccess.get();
        if (world == null) return;

        int rInt = Math.max(1, Math.round(radius));
        float rSq = radius * radius;

        if (hitSide == Direction.UP || hitSide == Direction.DOWN) {
            for (int dx = -rInt; dx <= rInt; dx++) {
                for (int dz = -rInt; dz <= rInt; dz++) {
                    if (dx * dx + dz * dz > rSq + 0.5F) continue;

                    int bx = centerPos.getX() + dx;
                    int bz = centerPos.getZ() + dz;

                    for (int dy = 2; dy >= -3; dy--) {
                        BlockPos check = new BlockPos(bx, centerPos.getY() + dy, bz);
                        if (!world.getBlockState(check).isAir()) {
                            BlockPos above = check.up();
                            if (world.getBlockState(above).isAir() || !world.getBlockState(above).isOpaqueFullCube()) {
                                addFace(check, hitSide);
                                break;
                            }
                        }
                    }
                }
            }
        } else {
            for (int d1 = -rInt; d1 <= rInt; d1++) {
                for (int d2 = -rInt; d2 <= rInt; d2++) {
                    if (d1 * d1 + d2 * d2 > rSq + 0.5F) continue;

                    BlockPos targetPos;
                    if (hitSide == Direction.NORTH || hitSide == Direction.SOUTH) {
                        targetPos = centerPos.add(d1, d2, 0);
                    } else {
                        targetPos = centerPos.add(0, d2, d1);
                    }

                    if (!world.getBlockState(targetPos).isAir()) {
                        addFace(targetPos, hitSide);
                    }
                }
            }
        }
    }

    private static void addFace(BlockPos pos, Direction side) {
        long key = (pos.asLong() << 3) | (side.getId() & 0x7);
        if (SEEN_KEYS.add(key)) {
            FACES.add(new BlockFace(pos, side));
        }
    }

    private static void addBlockFaceQuadVertices(
        BufferBuilder buffer,
        Matrix4f matrix,
        BlockPos pos,
        Direction side,
        ColorRGBA color
    ) {
        double offset = 0.002;
        double x = pos.getX();
        double y = pos.getY();
        double z = pos.getZ();

        double x1, y1, z1, x2, y2, z2, x3, y3, z3, x4, y4, z4;

        switch (side) {
            case UP -> {
                double py = y + 1.0 + offset;
                x1 = x;       y1 = py; z1 = z;
                x2 = x + 1.0; y2 = py; z2 = z;
                x3 = x + 1.0; y3 = py; z3 = z + 1.0;
                x4 = x;       y4 = py; z4 = z + 1.0;
            }
            case DOWN -> {
                double py = y - offset;
                x1 = x;       y1 = py; z1 = z + 1.0;
                x2 = x + 1.0; y2 = py; z2 = z + 1.0;
                x3 = x + 1.0; y3 = py; z3 = z;
                x4 = x;       y4 = py; z4 = z;
            }
            case NORTH -> {
                double pz = z - offset;
                x1 = x + 1.0; y1 = y;       z1 = pz;
                x2 = x;       y2 = y;       z2 = pz;
                x3 = x;       y3 = y + 1.0; z3 = pz;
                x4 = x + 1.0; y4 = y + 1.0; z4 = pz;
            }
            case SOUTH -> {
                double pz = z + 1.0 + offset;
                x1 = x;       y1 = y;       z1 = pz;
                x2 = x + 1.0; y2 = y;       z2 = pz;
                x3 = x + 1.0; y3 = y + 1.0; z3 = pz;
                x4 = x;       y4 = y + 1.0; z4 = pz;
            }
            case WEST -> {
                double px = x - offset;
                x1 = px; y1 = y;       z1 = z;
                x2 = px; y2 = y;       z2 = z + 1.0;
                x3 = px; y3 = y + 1.0; z3 = z + 1.0;
                x4 = px; y4 = y + 1.0; z4 = z;
            }
            case EAST -> {
                double px = x + 1.0 + offset;
                x1 = px; y1 = y;       z1 = z + 1.0;
                x2 = px; y2 = y;       z2 = z;
                x3 = px; y3 = y + 1.0; z3 = z;
                x4 = px; y4 = y + 1.0; z4 = z + 1.0;
            }
            default -> {
                return;
            }
        }

        int r = (int) color.getRed();
        int g = (int) color.getGreen();
        int b = (int) color.getBlue();
        int a = (int) color.getAlpha();

        buffer.vertex(matrix, (float) x1, (float) y1, (float) z1).color(r, g, b, a);
        buffer.vertex(matrix, (float) x2, (float) y2, (float) z2).color(r, g, b, a);
        buffer.vertex(matrix, (float) x3, (float) y3, (float) z3).color(r, g, b, a);
        buffer.vertex(matrix, (float) x4, (float) y4, (float) z4).color(r, g, b, a);
    }

    private static void addBlockFaceOutlineVertices(
        BufferBuilder buffer,
        Matrix4f matrix,
        BlockPos pos,
        Direction side,
        ColorRGBA color
    ) {
        double offset = 0.002;
        double x = pos.getX();
        double y = pos.getY();
        double z = pos.getZ();

        double x1, y1, z1, x2, y2, z2, x3, y3, z3, x4, y4, z4;

        switch (side) {
            case UP -> {
                double py = y + 1.0 + offset;
                x1 = x;       y1 = py; z1 = z;
                x2 = x + 1.0; y2 = py; z2 = z;
                x3 = x + 1.0; y3 = py; z3 = z + 1.0;
                x4 = x;       y4 = py; z4 = z + 1.0;
            }
            case DOWN -> {
                double py = y - offset;
                x1 = x;       y1 = py; z1 = z + 1.0;
                x2 = x + 1.0; y2 = py; z2 = z + 1.0;
                x3 = x + 1.0; y3 = py; z3 = z;
                x4 = x;       y4 = py; z4 = z;
            }
            case NORTH -> {
                double pz = z - offset;
                x1 = x + 1.0; y1 = y;       z1 = pz;
                x2 = x;       y2 = y;       z2 = pz;
                x3 = x;       y3 = y + 1.0; z3 = pz;
                x4 = x + 1.0; y4 = y + 1.0; z4 = pz;
            }
            case SOUTH -> {
                double pz = z + 1.0 + offset;
                x1 = x;       y1 = y;       z1 = pz;
                x2 = x + 1.0; y2 = y;       z2 = pz;
                x3 = x + 1.0; y3 = y + 1.0; z3 = pz;
                x4 = x;       y4 = y + 1.0; z4 = pz;
            }
            case WEST -> {
                double px = x - offset;
                x1 = px; y1 = y;       z1 = z;
                x2 = px; y2 = y;       z2 = z + 1.0;
                x3 = px; y3 = y + 1.0; z3 = z + 1.0;
                x4 = px; y4 = y + 1.0; z4 = z;
            }
            case EAST -> {
                double px = x + 1.0 + offset;
                x1 = px; y1 = y;       z1 = z + 1.0;
                x2 = px; y2 = y;       z2 = z;
                x3 = px; y3 = y + 1.0; z3 = z;
                x4 = px; y4 = y + 1.0; z4 = z + 1.0;
            }
            default -> {
                return;
            }
        }

        int r = (int) color.getRed();
        int g = (int) color.getGreen();
        int b = (int) color.getBlue();
        int a = (int) color.getAlpha();

        buffer.vertex(matrix, (float) x1, (float) y1, (float) z1).color(r, g, b, a);
        buffer.vertex(matrix, (float) x2, (float) y2, (float) z2).color(r, g, b, a);

        buffer.vertex(matrix, (float) x2, (float) y2, (float) z2).color(r, g, b, a);
        buffer.vertex(matrix, (float) x3, (float) y3, (float) z3).color(r, g, b, a);

        buffer.vertex(matrix, (float) x3, (float) y3, (float) z3).color(r, g, b, a);
        buffer.vertex(matrix, (float) x4, (float) y4, (float) z4).color(r, g, b, a);

        buffer.vertex(matrix, (float) x4, (float) y4, (float) z4).color(r, g, b, a);
        buffer.vertex(matrix, (float) x1, (float) y1, (float) z1).color(r, g, b, a);
    }

    private record BlockFace(BlockPos pos, Direction side) {
    }
}
