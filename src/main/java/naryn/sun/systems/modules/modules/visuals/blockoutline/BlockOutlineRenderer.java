package naryn.sun.systems.modules.modules.visuals.blockoutline;

import com.mojang.blaze3d.systems.RenderSystem;
import naryn.sun.utility.colors.ColorRGBA;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import org.joml.Matrix4f;

public final class BlockOutlineRenderer {

    private BlockOutlineRenderer() {
    }

    public static void render(
        MatrixStack ms,
        BlockOutlineState state,
        Camera camera,
        boolean depthBuffer,
        String renderMode,
        String shapeMode,
        float lineWidth,
        float fillAlpha,
        ColorRGBA color
    ) {
        if (!state.shouldRender()) {
            return;
        }

        Box box = state.getCurrentBox();
        Box targetBox = state.getTargetBox();
        if (box == null) {
            return;
        }

        Vec3d camPos = camera.getPos();
        Box relBox = box.offset(-camPos.x, -camPos.y, -camPos.z);

        float animAlpha = state.getAlpha();
        float r = color.getRed() / 255.0F;
        float g = color.getGreen() / 255.0F;
        float b = color.getBlue() / 255.0F;
        float baseAlpha = (color.getAlpha() / 255.0F) * animAlpha;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // Управление Z-буфером (Depth Buffer)
        if (depthBuffer) {
            RenderSystem.enableDepthTest();
            RenderSystem.enablePolygonOffset();
            RenderSystem.polygonOffset(-1.0F, -1.0F);
        } else {
            RenderSystem.disableDepthTest();
        }
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        boolean doFill = renderMode.contains("Fill");
        boolean doOutline = renderMode.contains("Outline");
        boolean doCorners = renderMode.contains("Corners");
        boolean useVoxel = shapeMode.equalsIgnoreCase("Voxel");

        VoxelShape shape = state.getCurrentShape();
        BlockPos pos = state.getCurrentPos();
        boolean canDrawVoxel = useVoxel && shape != null && pos != null && targetBox != null;

        // Предварительные коэффициенты для плавной интерполяции VoxelShape
        double curMinX = box.minX, curMinY = box.minY, curMinZ = box.minZ;
        double curSizeX = box.maxX - curMinX, curSizeY = box.maxY - curMinY, curSizeZ = box.maxZ - curMinZ;

        double tgtMinX = targetBox != null ? targetBox.minX : curMinX;
        double tgtMinY = targetBox != null ? targetBox.minY : curMinY;
        double tgtMinZ = targetBox != null ? targetBox.minZ : curMinZ;
        double tgtSizeX = targetBox != null ? Math.max(0.0001, targetBox.maxX - tgtMinX) : 1.0;
        double tgtSizeY = targetBox != null ? Math.max(0.0001, targetBox.maxY - tgtMinY) : 1.0;
        double tgtSizeZ = targetBox != null ? Math.max(0.0001, targetBox.maxZ - tgtMinZ) : 1.0;

        // 1. Заливка (Fill)
        if (doFill) {
            float alphaF = baseAlpha * (fillAlpha / 100.0F);
            if (alphaF > 0.001F) {
                RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
                BufferBuilder fillBuf = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
                Matrix4f matrix = ms.peek().getPositionMatrix();

                if (canDrawVoxel) {
                    shape.forEachBox((minX, minY, minZ, maxX, maxY, maxZ) -> {
                        float x1 = mapCoord(pos.getX() + minX, tgtMinX, tgtSizeX, curMinX, curSizeX, camPos.x);
                        float y1 = mapCoord(pos.getY() + minY, tgtMinY, tgtSizeY, curMinY, curSizeY, camPos.y);
                        float z1 = mapCoord(pos.getZ() + minZ, tgtMinZ, tgtSizeZ, curMinZ, curSizeZ, camPos.z);
                        float x2 = mapCoord(pos.getX() + maxX, tgtMinX, tgtSizeX, curMinX, curSizeX, camPos.x);
                        float y2 = mapCoord(pos.getY() + maxY, tgtMinY, tgtSizeY, curMinY, curSizeY, camPos.y);
                        float z2 = mapCoord(pos.getZ() + maxZ, tgtMinZ, tgtSizeZ, curMinZ, curSizeZ, camPos.z);
                        solidBox(fillBuf, matrix, x1, y1, z1, x2, y2, z2, r, g, b, alphaF);
                    });
                } else {
                    solidBox(fillBuf, matrix,
                        (float) relBox.minX, (float) relBox.minY, (float) relBox.minZ,
                        (float) relBox.maxX, (float) relBox.maxY, (float) relBox.maxZ,
                        r, g, b, alphaF
                    );
                }

                BuiltBuffer built = fillBuf.endNullable();
                if (built != null) {
                    BufferRenderer.drawWithGlobalProgram(built);
                }
            }
        }

        // 2. Обводка (Outline / Corners)
        if (doOutline || doCorners) {
            float radius = 0.0015F + (lineWidth / 10.0F) * 0.016F;
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
            BufferBuilder lineBuf = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            Matrix4f matrix = ms.peek().getPositionMatrix();

            if (doCorners) {
                addThickCorners(lineBuf, matrix, relBox, radius, r, g, b, baseAlpha);
            } else if (canDrawVoxel) {
                shape.forEachEdge((vx1, vy1, vz1, vx2, vy2, vz2) -> {
                    float x1 = mapCoord(pos.getX() + vx1, tgtMinX, tgtSizeX, curMinX, curSizeX, camPos.x);
                    float y1 = mapCoord(pos.getY() + vy1, tgtMinY, tgtSizeY, curMinY, curSizeY, camPos.y);
                    float z1 = mapCoord(pos.getZ() + vz1, tgtMinZ, tgtSizeZ, curMinZ, curSizeZ, camPos.z);
                    float x2 = mapCoord(pos.getX() + vx2, tgtMinX, tgtSizeX, curMinX, curSizeX, camPos.x);
                    float y2 = mapCoord(pos.getY() + vy2, tgtMinY, tgtSizeY, curMinY, curSizeY, camPos.y);
                    float z2 = mapCoord(pos.getZ() + vz2, tgtMinZ, tgtSizeZ, curMinZ, curSizeZ, camPos.z);
                    addThickLine(lineBuf, matrix, x1, y1, z1, x2, y2, z2, radius, r, g, b, baseAlpha);
                });
            } else {
                addThickBoxEdges(lineBuf, matrix, relBox, radius, r, g, b, baseAlpha);
            }

            BuiltBuffer built = lineBuf.endNullable();
            if (built != null) {
                BufferRenderer.drawWithGlobalProgram(built);
            }
        }

        if (depthBuffer) {
            RenderSystem.polygonOffset(0.0F, 0.0F);
            RenderSystem.disablePolygonOffset();
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private static float mapCoord(double worldVal, double tgtMin, double tgtSize, double curMin, double curSize, double camVal) {
        double frac = (worldVal - tgtMin) / tgtSize;
        return (float) (curMin + frac * curSize - camVal);
    }

    private static void addThickLine(
        BufferBuilder buf, Matrix4f m,
        float x1, float y1, float z1,
        float x2, float y2, float z2,
        float r,
        float red, float green, float blue, float alpha
    ) {
        float minX = Math.min(x1, x2);
        float maxX = Math.max(x1, x2);
        float minY = Math.min(y1, y2);
        float maxY = Math.max(y1, y2);
        float minZ = Math.min(z1, z2);
        float maxZ = Math.max(z1, z2);

        if (maxX - minX <= 0.0001F) {
            minX -= r;
            maxX += r;
        }
        if (maxY - minY <= 0.0001F) {
            minY -= r;
            maxY += r;
        }
        if (maxZ - minZ <= 0.0001F) {
            minZ -= r;
            maxZ += r;
        }

        solidBox(buf, m, minX, minY, minZ, maxX, maxY, maxZ, red, green, blue, alpha);
    }

    private static void addThickBoxEdges(BufferBuilder buf, Matrix4f m, Box box, float r, float red, float green, float blue, float alpha) {
        float x1 = (float) box.minX, y1 = (float) box.minY, z1 = (float) box.minZ;
        float x2 = (float) box.maxX, y2 = (float) box.maxY, z2 = (float) box.maxZ;

        // Нижние 4 ребра
        addThickLine(buf, m, x1, y1, z1, x2, y1, z1, r, red, green, blue, alpha);
        addThickLine(buf, m, x2, y1, z1, x2, y1, z2, r, red, green, blue, alpha);
        addThickLine(buf, m, x2, y1, z2, x1, y1, z2, r, red, green, blue, alpha);
        addThickLine(buf, m, x1, y1, z2, x1, y1, z1, r, red, green, blue, alpha);

        // Верхние 4 ребра
        addThickLine(buf, m, x1, y2, z1, x2, y2, z1, r, red, green, blue, alpha);
        addThickLine(buf, m, x2, y2, z1, x2, y2, z2, r, red, green, blue, alpha);
        addThickLine(buf, m, x2, y2, z2, x1, y2, z2, r, red, green, blue, alpha);
        addThickLine(buf, m, x1, y2, z2, x1, y2, z1, r, red, green, blue, alpha);

        // Вертикальные 4 ребра
        addThickLine(buf, m, x1, y1, z1, x1, y2, z1, r, red, green, blue, alpha);
        addThickLine(buf, m, x2, y1, z1, x2, y2, z1, r, red, green, blue, alpha);
        addThickLine(buf, m, x2, y1, z2, x2, y2, z2, r, red, green, blue, alpha);
        addThickLine(buf, m, x1, y1, z2, x1, y2, z2, r, red, green, blue, alpha);
    }

    private static void addThickCorners(BufferBuilder buf, Matrix4f m, Box box, float r, float red, float green, float blue, float alpha) {
        float x1 = (float) box.minX, y1 = (float) box.minY, z1 = (float) box.minZ;
        float x2 = (float) box.maxX, y2 = (float) box.maxY, z2 = (float) box.maxZ;
        float len = Math.min(x2 - x1, Math.min(y2 - y1, z2 - z1)) * 0.28F;

        // 8 углов (по 3 луча от каждого)
        cornerRay(buf, m, x1, y1, z1, len, len, len, r, red, green, blue, alpha);
        cornerRay(buf, m, x2, y1, z1, -len, len, len, r, red, green, blue, alpha);
        cornerRay(buf, m, x1, y2, z1, len, -len, len, r, red, green, blue, alpha);
        cornerRay(buf, m, x2, y2, z1, -len, -len, len, r, red, green, blue, alpha);
        cornerRay(buf, m, x1, y1, z2, len, len, -len, r, red, green, blue, alpha);
        cornerRay(buf, m, x2, y1, z2, -len, len, -len, r, red, green, blue, alpha);
        cornerRay(buf, m, x1, y2, z2, len, -len, -len, r, red, green, blue, alpha);
        cornerRay(buf, m, x2, y2, z2, -len, -len, -len, r, red, green, blue, alpha);
    }

    private static void cornerRay(
        BufferBuilder buf, Matrix4f m,
        float x, float y, float z,
        float dx, float dy, float dz,
        float r, float red, float green, float blue, float alpha
    ) {
        addThickLine(buf, m, x, y, z, x + dx, y, z, r, red, green, blue, alpha);
        addThickLine(buf, m, x, y, z, x, y + dy, z, r, red, green, blue, alpha);
        addThickLine(buf, m, x, y, z, x, y, z + dz, r, red, green, blue, alpha);
    }

    private static void solidBox(
        BufferBuilder b, Matrix4f m,
        float x1, float y1, float z1,
        float x2, float y2, float z2,
        float r, float g, float bl, float a
    ) {
        // Bottom (Y-)
        quad(b, m, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2, r, g, bl, a);
        // Top (Y+)
        quad(b, m, x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1, r, g, bl, a);
        // North (Z-)
        quad(b, m, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1, r, g, bl, a);
        // South (Z+)
        quad(b, m, x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2, r, g, bl, a);
        // West (X-)
        quad(b, m, x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1, r, g, bl, a);
        // East (X+)
        quad(b, m, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2, r, g, bl, a);
    }

    private static void quad(
        BufferBuilder b, Matrix4f m,
        float x1, float y1, float z1,
        float x2, float y2, float z2,
        float x3, float y3, float z3,
        float x4, float y4, float z4,
        float r, float g, float bl, float a
    ) {
        b.vertex(m, x1, y1, z1).color(r, g, bl, a);
        b.vertex(m, x2, y2, z2).color(r, g, bl, a);
        b.vertex(m, x3, y3, z3).color(r, g, bl, a);
        b.vertex(m, x4, y4, z4).color(r, g, bl, a);
    }
}
