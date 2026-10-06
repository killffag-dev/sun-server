package naryn.sun.systems.modules.modules.visuals.skyentity.models;

import static naryn.sun.systems.modules.modules.visuals.skyentity.SkyEntityGeometry.*;

import naryn.sun.systems.modules.modules.visuals.skyentity.SkyEntityModel;
import net.minecraft.client.render.BufferBuilder;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Процедурная модель дирижабля — эллипсоидальный баллон из сужающихся боксов
 * (профиль радиуса нос -> хвост), хвостовая крестовина стабилизаторов, гондола
 * на растяжках под брюхом и два вращающихся 2-лопастных винта по бокам гондолы.
 * Анимация: лёгкое покачивание корпуса (тангаж + крен) и вертикальный "бобинг",
 * плюс постоянное вращение винтов.
 */
public class BlimpModel implements SkyEntityModel {

    @Override
    public void build(BufferBuilder buf, Matrix4f m, float t, float phase) {
        // --- Анимация корпуса: лёгкое покачивание целиком ---
        float pitch = (float) Math.sin(t * 0.45) * 0.06F;
        float roll = (float) Math.sin(t * 0.30 + 1.4F) * 0.05F;
        float bob = (float) Math.sin(t * 0.6) * 0.12F;

        // --- Баллон: центральная линия с профилем радиуса нос -> хвост ---
        Vector3f c0 = v(7.6F, 0.00F, 0);    // нос (кончик)
        Vector3f c1 = v(6.1F, 0.05F, 0);
        Vector3f c2 = v(3.9F, 0.08F, 0);
        Vector3f c3 = v(1.2F, 0.08F, 0);    // макс. радиус
        Vector3f c4 = v(-1.7F, 0.06F, 0);
        Vector3f c5 = v(-4.3F, 0.02F, 0);
        Vector3f c6 = v(-6.3F, -0.02F, 0);
        Vector3f c7 = v(-7.6F, -0.05F, 0);  // хвост (кончик)

        float r0 = 0.05F, r1 = 1.35F, r2 = 2.35F, r3 = 2.60F,
              r4 = 2.55F, r5 = 2.05F, r6 = 1.00F, r7 = 0.12F;
        float hullShade = 0.80F;

        box(buf, m, S(c0, pitch, roll, bob), S(c1, pitch, roll, bob), r0, r1, hullShade);
        box(buf, m, S(c1, pitch, roll, bob), S(c2, pitch, roll, bob), r1, r2, hullShade);
        box(buf, m, S(c2, pitch, roll, bob), S(c3, pitch, roll, bob), r2, r3, hullShade);
        box(buf, m, S(c3, pitch, roll, bob), S(c4, pitch, roll, bob), r3, r4, hullShade);
        box(buf, m, S(c4, pitch, roll, bob), S(c5, pitch, roll, bob), r4, r5, hullShade);
        box(buf, m, S(c5, pitch, roll, bob), S(c6, pitch, roll, bob), r5, r6, hullShade);
        box(buf, m, S(c6, pitch, roll, bob), S(c7, pitch, roll, bob), r6, r7, hullShade);

        // --- Хвостовые стабилизаторы: крестовина из 4 плоскостей (верх/низ/лево/право) ---
        int[] finColor = col(0.28F, 0.35F);
        Vector3f[] dirs = {v(0, 1, 0), v(0, -1, 0), v(0, 0, 1), v(0, 0, -1)};
        for (Vector3f dir : dirs) {
            Vector3f tipLocal = off(c6, -1.7F, dir.y * 2.9F, dir.z * 2.9F);
            Vector3f root = S(c5, pitch, roll, bob);
            Vector3f tail = S(c7, pitch, roll, bob);
            Vector3f tip = S(tipLocal, pitch, roll, bob);
            triD(buf, m, root, tail, tip, finColor);
            box(buf, m, S(c6, pitch, roll, bob), tip, 0.12F, 0.03F, 0.32F);
        }

        // --- Гондола под баллоном, сужающаяся к концам ---
        Vector3f gFront = v(2.0F, -3.10F, 0);
        Vector3f gMid = v(0.3F, -3.35F, 0);
        Vector3f gRear = v(-1.6F, -3.10F, 0);
        box(buf, m, S(gFront, pitch, roll, bob), S(gMid, pitch, roll, bob), 0.35F, 0.55F, 0.30F);
        box(buf, m, S(gMid, pitch, roll, bob), S(gRear, pitch, roll, bob), 0.55F, 0.30F, 0.30F);

        // --- Растяжки: брюхо баллона -> гондола (по 2 с каждой стороны) ---
        for (int s = -1; s <= 1; s += 2) {
            Vector3f bellyFront = off(c2, 0, -2.30F, s * 0.9F);
            Vector3f strutFront = off(gFront, 0, 0, s * 0.55F);
            box(buf, m, S(bellyFront, pitch, roll, bob), S(strutFront, pitch, roll, bob), 0.08F, 0.08F, 0.22F);

            Vector3f bellyRear = off(c4, 0, -2.40F, s * 0.9F);
            Vector3f strutRear = off(gRear, 0, 0, s * 0.55F);
            box(buf, m, S(bellyRear, pitch, roll, bob), S(strutRear, pitch, roll, bob), 0.08F, 0.08F, 0.22F);
        }

        // --- Винты: пилоны по бокам гондолы + вращающиеся 2-лопастные пропеллеры ---
        float spin = t * 20.0F;
        for (int s = -1; s <= 1; s += 2) {
            Vector3f pylonBase = off(gMid, 0, 0.05F, s * 0.9F);
            Vector3f hub = off(gMid, 0, 0.15F, s * 2.0F);
            box(buf, m, S(pylonBase, pitch, roll, bob), S(hub, pitch, roll, bob), 0.15F, 0.10F, 0.26F);

            float dir = s > 0 ? 1F : -1F;
            for (int b = 0; b < 2; b++) {
                float theta = spin * dir + b * (float) Math.PI;
                Vector3f bladeTip = off(hub, (float) Math.sin(theta) * 1.4F, (float) Math.cos(theta) * 1.4F, 0);
                box(buf, m, S(hub, pitch, roll, bob), S(bladeTip, pitch, roll, bob), 0.10F, 0.35F, 0.35F);
            }
        }
    }

    /** Поворот точки вокруг центра модели для покачивания (тангаж вокруг Z, крен вокруг X) + вертикальный сдвиг. */
    private static Vector3f S(Vector3f p, float pitch, float roll, float bob) {
        Vector3f r = new Vector3f(p);
        r.rotateZ(pitch);
        r.rotateX(roll);
        r.y += bob;
        return r;
    }
}