package naryn.sun.systems.modules.modules.visuals.hitbox;

import naryn.sun.utility.colors.ColorRGBA;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import org.joml.Matrix4f;

/**
 * Общая геометрия для модуля Hitbox:
 * - Заливка (сплошная и вертикальный градиент);
 * - 3D-рёбра (полная обводка из 12 рёбер со сплошным цветом или вертикальным градиентом);
 * - 3D-уголки (8 вершин со сплошным цветом или вертикальным градиентом).
 */
public final class HitboxGeometry {

    // ===================== ЗАЛИВКА =====================

    /** Заливка с вертикальным градиентом между двумя цветами (низ/верх). */
    public static void addGradientBox(BufferBuilder builder, MatrixStack matrices, Box box, ColorRGBA bottomColor, ColorRGBA topColor) {
        float minX = (float) box.minX, minY = (float) box.minY, minZ = (float) box.minZ;
        float maxX = (float) box.maxX, maxY = (float) box.maxY, maxZ = (float) box.maxZ;
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        float br = bottomColor.getRed() / 255.0F, bg = bottomColor.getGreen() / 255.0F, bb = bottomColor.getBlue() / 255.0F, ba = bottomColor.getAlpha() / 255.0F;
        float tr = topColor.getRed() / 255.0F, tg = topColor.getGreen() / 255.0F, tb = topColor.getBlue() / 255.0F, ta = topColor.getAlpha() / 255.0F;

        // низ и верх — каждый сплошным своим цветом
        quad(builder, matrix, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ, br, bg, bb, ba);
        quad(builder, matrix, minX, maxY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, minX, maxY, maxZ, tr, tg, tb, ta);

        // 4 боковые грани — интерполяция bottom -> top
        sideQuad(builder, matrix, minX, minZ, minX, maxZ, minY, maxY, br, bg, bb, ba, tr, tg, tb, ta);
        sideQuad(builder, matrix, maxX, minZ, maxX, maxZ, minY, maxY, br, bg, bb, ba, tr, tg, tb, ta);
        sideQuad(builder, matrix, minX, minZ, maxX, minZ, minY, maxY, br, bg, bb, ba, tr, tg, tb, ta);
        sideQuad(builder, matrix, minX, maxZ, maxX, maxZ, minY, maxY, br, bg, bb, ba, tr, tg, tb, ta);
    }

    // ===================== 3D РЁБРА: ПОЛНАЯ ОБВОДКА (12 РЁБЕР) =====================

    /** Рендерит все 12 рёбер бокса одним цветом через 3D-боксы (QUADS). */
    public static void addThickOutline(BufferBuilder builder, MatrixStack matrices, Box box, ColorRGBA color, float radius) {
        addThickOutlineGradient(builder, matrices, box, color, color, radius);
    }

    /** Рендерит все 12 рёбер бокса вертикальным градиентом через 3D-боксы (QUADS). */
    public static void addThickOutlineGradient(BufferBuilder builder, MatrixStack matrices, Box box, ColorRGBA bottomColor, ColorRGBA topColor, float radius) {
        float minX = (float) box.minX, minY = (float) box.minY, minZ = (float) box.minZ;
        float maxX = (float) box.maxX, maxY = (float) box.maxY, maxZ = (float) box.maxZ;
        float midY = (minY + maxY) / 2.0F;
        Matrix4f m = matrices.peek().getPositionMatrix();

        float br = bottomColor.getRed() / 255.0F, bg = bottomColor.getGreen() / 255.0F, bb = bottomColor.getBlue() / 255.0F, ba = bottomColor.getAlpha() / 255.0F;
        float tr = topColor.getRed() / 255.0F, tg = topColor.getGreen() / 255.0F, tb = topColor.getBlue() / 255.0F, ta = topColor.getAlpha() / 255.0F;
        float r = radius;

        // 1. Нижние 4 ребра (bottomColor)
        solidBox(builder, m, minX - r, minY - r, minZ - r, maxX + r, minY + r, minZ + r, br, bg, bb, ba); // X minZ
        solidBox(builder, m, minX - r, minY - r, maxZ - r, maxX + r, minY + r, maxZ + r, br, bg, bb, ba); // X maxZ
        solidBox(builder, m, minX - r, minY - r, minZ - r, minX + r, minY + r, maxZ + r, br, bg, bb, ba); // Z minX
        solidBox(builder, m, maxX - r, minY - r, minZ - r, maxX + r, minY + r, maxZ + r, br, bg, bb, ba); // Z maxX

        // 2. Верхние 4 ребра (topColor)
        solidBox(builder, m, minX - r, maxY - r, minZ - r, maxX + r, maxY + r, minZ + r, tr, tg, tb, ta); // X minZ
        solidBox(builder, m, minX - r, maxY - r, maxZ - r, maxX + r, maxY + r, maxZ + r, tr, tg, tb, ta); // X maxZ
        solidBox(builder, m, minX - r, maxY - r, minZ - r, minX + r, maxY + r, maxZ + r, tr, tg, tb, ta); // Z minX
        solidBox(builder, m, maxX - r, maxY - r, minZ - r, maxX + r, maxY + r, maxZ + r, tr, tg, tb, ta); // Z maxX

        // 3. Вертикальные 4 ребра (разделены на нижнюю половину bottomColor и верхнюю половину topColor)
        // Ребро (minX, minZ)
        solidBox(builder, m, minX - r, minY - r, minZ - r, minX + r, midY + r, minZ + r, br, bg, bb, ba);
        solidBox(builder, m, minX - r, midY - r, minZ - r, minX + r, maxY + r, minZ + r, tr, tg, tb, ta);

        // Ребро (maxX, minZ)
        solidBox(builder, m, maxX - r, minY - r, minZ - r, maxX + r, midY + r, minZ + r, br, bg, bb, ba);
        solidBox(builder, m, maxX - r, midY - r, minZ - r, maxX + r, maxY + r, minZ + r, tr, tg, tb, ta);

        // Ребро (minX, maxZ)
        solidBox(builder, m, minX - r, minY - r, maxZ - r, minX + r, midY + r, maxZ + r, br, bg, bb, ba);
        solidBox(builder, m, minX - r, midY - r, maxZ - r, minX + r, maxY + r, maxZ + r, tr, tg, tb, ta);

        // Ребро (maxX, maxZ)
        solidBox(builder, m, maxX - r, minY - r, maxZ - r, maxX + r, midY + r, maxZ + r, br, bg, bb, ba);
        solidBox(builder, m, maxX - r, midY - r, maxZ - r, maxX + r, maxY + r, maxZ + r, tr, tg, tb, ta);
    }

    // ===================== 3D УГОЛКИ (ТОЛЬКО ВЕРШИНЫ) =====================

    /** Рендерит 3D-уголки одним цветом через 3D-боксы (QUADS). */
    public static void addThickCorners(BufferBuilder builder, MatrixStack matrices, Box box, ColorRGBA color, float length, float radius) {
        addThickCornersGradient(builder, matrices, box, color, color, length, radius);
    }

    /** Рендерит 3D-уголки вертикальным градиентом через 3D-боксы (QUADS). */
    public static void addThickCornersGradient(BufferBuilder builder, MatrixStack matrices, Box box, ColorRGBA bottomColor, ColorRGBA topColor, float length, float radius) {
        float minX = (float) box.minX, minY = (float) box.minY, minZ = (float) box.minZ;
        float maxX = (float) box.maxX, maxY = (float) box.maxY, maxZ = (float) box.maxZ;
        Matrix4f m = matrices.peek().getPositionMatrix();

        float br = bottomColor.getRed() / 255.0F, bg = bottomColor.getGreen() / 255.0F, bb = bottomColor.getBlue() / 255.0F, ba = bottomColor.getAlpha() / 255.0F;
        float tr = topColor.getRed() / 255.0F, tg = topColor.getGreen() / 255.0F, tb = topColor.getBlue() / 255.0F, ta = topColor.getAlpha() / 255.0F;

        float lenX = Math.min(length, (maxX - minX) / 2.0F);
        float lenY = Math.min(length, (maxY - minY) / 2.0F);
        float lenZ = Math.min(length, (maxZ - minZ) / 2.0F);
        float r = radius;

        float[] xs = {minX, maxX};
        float[] ys = {minY, maxY};
        float[] zs = {minZ, maxZ};

        for (float x : xs) {
            float x1 = (x == minX) ? minX - r : maxX - lenX;
            float x2 = (x == minX) ? minX + lenX : maxX + r;

            for (float y : ys) {
                boolean isBottom = (y == minY);
                float cr = isBottom ? br : tr;
                float cg = isBottom ? bg : tg;
                float cb = isBottom ? bb : tb;
                float ca = isBottom ? ba : ta;

                float y1 = isBottom ? minY - r : maxY - lenY;
                float y2 = isBottom ? minY + lenY : maxY + r;

                for (float z : zs) {
                    float z1 = (z == minZ) ? minZ - r : maxZ - lenZ;
                    float z2 = (z == minZ) ? minZ + lenZ : maxZ + r;

                    // X-сегмент уголка
                    solidBox(builder, m, x1, y - r, z - r, x2, y + r, z + r, cr, cg, cb, ca);
                    // Z-сегмент уголка
                    solidBox(builder, m, x - r, y - r, z1, x + r, y + r, z2, cr, cg, cb, ca);
                    // Y-сегмент уголка (вертикальный)
                    solidBox(builder, m, x - r, y1, z - r, x + r, y2, z + r, cr, cg, cb, ca);
                }
            }
        }
    }

    // ===================== ПРИМИТИВЫ БОКСОВ =====================

    public static void solidBox(
        BufferBuilder b, Matrix4f m,
        float x1, float y1, float z1, float x2, float y2, float z2,
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

    // ===================== ПРИМИТИВЫ ГРАНЕЙ =====================

    private static void quad(
        BufferBuilder b, Matrix4f m,
        float x1, float y1, float z1, float x2, float y2, float z2,
        float x3, float y3, float z3, float x4, float y4, float z4,
        float r, float g, float bl, float a
    ) {
        b.vertex(m, x1, y1, z1).color(r, g, bl, a);
        b.vertex(m, x2, y2, z2).color(r, g, bl, a);
        b.vertex(m, x3, y3, z3).color(r, g, bl, a);
        b.vertex(m, x4, y4, z4).color(r, g, bl, a);
    }

    private static void sideQuad(
        BufferBuilder b, Matrix4f m, float x1, float z1, float x2, float z2, float minY, float maxY,
        float br, float bg, float bb, float ba, float tr, float tg, float tbl, float ta
    ) {
        b.vertex(m, x1, minY, z1).color(br, bg, bb, ba);
        b.vertex(m, x2, minY, z2).color(br, bg, bb, ba);
        b.vertex(m, x2, maxY, z2).color(tr, tg, tbl, ta);
        b.vertex(m, x1, maxY, z1).color(tr, tg, tbl, ta);
    }

    private HitboxGeometry() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}