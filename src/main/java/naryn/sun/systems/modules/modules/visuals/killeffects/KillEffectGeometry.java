package naryn.sun.systems.modules.modules.visuals.killeffects;

import lombok.Generated;
import naryn.sun.utility.math.MathUtility;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/**
 * Общие геометрические примитивы для эффектов смерти (KillEffects):
 * случайная точка в хитбоксе, "толстый" 2D-отрезок (для контуров/прожилок).
 * Вынесено отдельно, чтобы не дублировать в каждом эффекте (LeafEffect, ...).
 */
public final class KillEffectGeometry {

    public static Vec3d randomPointInBox(Box box) {
        double x = box.minX + MathUtility.random(0.0F, 1.0F) * (box.maxX - box.minX);
        double y = box.minY + MathUtility.random(0.0F, 1.0F) * (box.maxY - box.minY);
        double z = box.minZ + MathUtility.random(0.0F, 1.0F) * (box.maxZ - box.minZ);
        return new Vec3d(x, y, z);
    }

    /** "Толстый" отрезок в плоскости XY (2 треугольника) — для контуров и прожилок плоских частиц. */
    public static void addThickSegment(BufferBuilder builder, Matrix4f matrix, float x1, float y1, float x2, float y2, float halfWidth, int rgb) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length < 1.0E-5F) {
            return;
        }

        float nx = -dy / length * halfWidth;
        float ny = dx / length * halfWidth;

        float ax = x1 + nx;
        float ay = y1 + ny;
        float bx = x1 - nx;
        float by = y1 - ny;
        float cx = x2 - nx;
        float cy = y2 - ny;
        float dx2 = x2 + nx;
        float dy2 = y2 + ny;

        builder.vertex(matrix, ax, ay, 0.0F).color(rgb);
        builder.vertex(matrix, bx, by, 0.0F).color(rgb);
        builder.vertex(matrix, cx, cy, 0.0F).color(rgb);

        builder.vertex(matrix, ax, ay, 0.0F).color(rgb);
        builder.vertex(matrix, cx, cy, 0.0F).color(rgb);
        builder.vertex(matrix, dx2, dy2, 0.0F).color(rgb);
    }

    @Generated
    private KillEffectGeometry() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}