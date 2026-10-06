package naryn.sun.systems.modules.modules.visuals.skyentity;

import lombok.Generated;
import net.minecraft.client.render.BufferBuilder;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Общие геометрические примитивы для рисования "небесных сущностей":
 * сужающиеся боксы (кости/тело) и плоские треугольные мембраны (крылья).
 * Вынесено отдельно, чтобы не дублировать в каждой модели (DragonModel, UfoModel, ...).
 */
public final class SkyEntityGeometry {

    // Глобальный множитель альфы для текущей строящейся модели (по умолчанию непрозрачно).
    // Используется сценами вроде похищения коровы для fade-in/fade-out.
    // Обязательно сбрасывать обратно в 1.0F сразу после построения буфера!
    private static float currentAlpha = 1.0F;

    public static void setAlpha(float a) {
        currentAlpha = Math.max(0F, Math.min(1F, a));
    }

    public static void resetAlpha() {
        currentAlpha = 1.0F;
    }

    public static Vector3f v(float x, float y, float z) {
        return new Vector3f(x, y, z);
    }

    public static Vector3f off(Vector3f base, float dx, float dy, float dz) {
        return new Vector3f(base.x + dx, base.y + dy, base.z + dz);
    }

    /** Сужающийся бокс между двумя точками с фейковым освещением по нормалям граней. */
    public static void box(BufferBuilder buf, Matrix4f m, Vector3f a, Vector3f b, float ra, float rb, float shade) {
        Vector3f dir = new Vector3f(b).sub(a);
        float len = dir.length();
        if (len < 1.0e-4f) return;
        dir.div(len);
        Vector3f up0 = Math.abs(dir.y) > 0.9f ? new Vector3f(1, 0, 0) : new Vector3f(0, 1, 0);
        Vector3f right = new Vector3f(dir).cross(up0).normalize();
        Vector3f up = new Vector3f(right).cross(dir).normalize();

        float[][] k = {{1, 1}, {1, -1}, {-1, -1}, {-1, 1}};
        Vector3f[] ca = new Vector3f[4];
        Vector3f[] cb = new Vector3f[4];
        for (int i = 0; i < 4; i++) {
            Vector3f oa = new Vector3f(right).mul(k[i][0] * ra).add(new Vector3f(up).mul(k[i][1] * ra));
            Vector3f ob = new Vector3f(right).mul(k[i][0] * rb).add(new Vector3f(up).mul(k[i][1] * rb));
            ca[i] = new Vector3f(a).add(oa);
            cb[i] = new Vector3f(b).add(ob);
        }
        for (int i = 0; i < 4; i++) {
            int j = (i + 1) % 4;
            float nx = (k[i][0] + k[j][0]) * 0.5f;
            float nyk = (k[i][1] + k[j][1]) * 0.5f;
            float ny = right.y * nx + up.y * nyk;
            quad(buf, m, ca[i], cb[i], cb[j], ca[j], col(shade, ny));
        }
        quad(buf, m, cb[0], cb[1], cb[2], cb[3], col(shade, dir.y));
        quad(buf, m, ca[0], ca[1], ca[2], ca[3], col(shade, -dir.y));
    }

    public static void quad(BufferBuilder buf, Matrix4f m, Vector3f p1, Vector3f p2, Vector3f p3, Vector3f p4, int[] c) {
        triD(buf, m, p1, p2, p3, c);
        triD(buf, m, p1, p3, p4, c);
    }

    public static void triD(BufferBuilder buf, Matrix4f m, Vector3f p1, Vector3f p2, Vector3f p3, int[] c) {
        float r = c[0] / 255.0F;
        float g = c[1] / 255.0F;
        float b = c[2] / 255.0F;
        buf.vertex(m, p1.x, p1.y, p1.z).color(r, g, b, currentAlpha);
        buf.vertex(m, p2.x, p2.y, p2.z).color(r, g, b, currentAlpha);
        buf.vertex(m, p3.x, p3.y, p3.z).color(r, g, b, currentAlpha);
    }

    /** Цвет части тела: оттенок * освещение сверху. */
    public static int[] col(float shade, float ny) {
        float l = 0.55F + 0.45F * Math.max(-1F, Math.min(1F, ny));
        float base = 255F * shade * l;
        int r = (int) Math.min(255, base);
        int g = (int) Math.min(255, base * 1.02F);
        int b = (int) Math.min(255, base * 1.12F);
        return new int[]{r, g, b};
    }

    @Generated
    private SkyEntityGeometry() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}