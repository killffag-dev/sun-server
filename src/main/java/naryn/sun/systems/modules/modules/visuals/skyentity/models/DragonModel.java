package naryn.sun.systems.modules.modules.visuals.skyentity.models;

import static naryn.sun.systems.modules.modules.visuals.skyentity.SkyEntityGeometry.*;

import naryn.sun.systems.modules.modules.visuals.skyentity.SkyEntityModel;
import net.minecraft.client.render.BufferBuilder;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Процедурная модель дракона — вся геометрия строится из "сужающихся боксов"
 * (тело, шея, хвост, лапы, кости крыльев) и треугольных мембран (крылья, плавник хвоста).
 * Никаких внешних ассетов не требуется.
 */
public class DragonModel implements SkyEntityModel {

    @Override
    public void build(BufferBuilder buf, Matrix4f m, float t, float phase) {
        // --- Корпус: грудь и брюхо ---
        box(buf, m, v(4.8F, 0.2F, 0), v(0.5F, 0.0F, 0), 1.45F, 1.65F, 0.19F);
        box(buf, m, v(0.5F, 0.0F, 0), v(-5.2F, 0.0F, 0), 1.60F, 0.85F, 0.17F);

        // --- Шея: 3 сегмента, лёгкий поворот головы ---
        float look = (float) Math.sin(t * 0.4) * 0.35F;
        Vector3f n0 = v(4.6F, 0.5F, 0);
        Vector3f n1 = off(n0, 1.5F, 0.35F, look * 0.3F);
        Vector3f n2 = off(n1, 1.5F, 0.40F, look * 0.5F);
        Vector3f n3 = off(n2, 1.4F, 0.35F, look * 0.7F);
        box(buf, m, n0, n1, 0.75F, 0.62F, 0.20F);
        box(buf, m, n1, n2, 0.62F, 0.52F, 0.20F);
        box(buf, m, n2, n3, 0.52F, 0.46F, 0.21F);

        // --- Голова: череп, морда, челюсть, рога ---
        Vector3f skullEnd = off(n3, 1.7F, 0.10F, look * 0.2F);
        box(buf, m, n3, skullEnd, 0.55F, 0.45F, 0.24F);
        box(buf, m, skullEnd, off(skullEnd, 1.4F, -0.05F, 0), 0.38F, 0.16F, 0.22F);
        box(buf, m, off(n3, 0.6F, -0.30F, 0), off(n3, 2.1F, -0.55F, 0), 0.20F, 0.08F, 0.20F);
        for (int s = -1; s <= 1; s += 2) {
            box(buf, m, off(n3, -0.1F, 0.35F, s * 0.30F), off(n3, -1.4F, 1.15F, s * 0.65F), 0.13F, 0.03F, 0.30F);
        }

        // --- Хвост: 6 сегментов, волна бежит к кончику ---
        float segLen = 2.2F;
        float rad = 0.72F;
        Vector3f prev = v(-5.2F, 0.05F, 0);
        for (int i = 0; i < 6; i++) {
            float k = (i + 1) / 6F;
            float lateral = (float) Math.sin(t * 2.4 - i * 0.55) * 0.30F * k;
            float vertical = (float) Math.sin(t * 1.3 - i * 0.40) * 0.10F * k;
            Vector3f dir = new Vector3f(-1F, vertical, lateral).normalize().mul(segLen);
            Vector3f next = new Vector3f(prev).add(dir);
            float nextRad = rad * 0.84F;
            box(buf, m, prev, next, rad, nextRad, 0.15F);
            prev = next;
            rad = nextRad;
            segLen *= 0.95F;
        }
        int[] finC = col(0.12F, 0.4F);
        triD(buf, m, prev, off(prev, -1.6F, 1.2F, 0), off(prev, -1.9F, 0.1F, 0), finC);
        triD(buf, m, prev, off(prev, -1.6F, -0.9F, 0), off(prev, -1.9F, 0.1F, 0), finC);

        // --- Лапы (поджаты в полёте) ---
        for (int s = -1; s <= 1; s += 2) {
            Vector3f fh = v(1.4F, -0.9F, s * 1.15F);
            Vector3f fk = off(fh, -0.6F, -1.1F, s * 0.25F);
            box(buf, m, fh, fk, 0.32F, 0.24F, 0.16F);
            box(buf, m, fk, off(fk, 1.1F, -0.35F, 0), 0.22F, 0.10F, 0.16F);

            Vector3f rh = v(-3.6F, -0.7F, s * 1.05F);
            Vector3f rk = off(rh, -0.7F, -1.25F, s * 0.3F);
            box(buf, m, rh, rk, 0.40F, 0.28F, 0.16F);
            box(buf, m, rk, off(rk, 1.3F, -0.3F, 0), 0.26F, 0.11F, 0.16F);
        }

        // --- Крылья: плечо -> предплечье -> 4 пальца, мембрана ---
        float aSh = (float) Math.sin(phase) * 0.65F;
        float aFr = (float) Math.sin(phase - 0.40F) * 0.75F;
        float aFi = (float) Math.sin(phase - 0.80F) * 0.85F;

        for (int s = -1; s <= 1; s += 2) {
            Vector3f S = v(2.2F, 0.6F, s * 1.4F);
            Vector3f E = off(S, -0.6F, 3.6F * (float) Math.sin(aSh), s * 3.6F * (float) Math.cos(aSh));
            Vector3f W = off(E, 0.9F, 4.6F * (float) Math.sin(aFr), s * 4.6F * (float) Math.cos(aFr));
            float sinF = (float) Math.sin(aFi);
            float cosF = (float) Math.cos(aFi);
            Vector3f F1 = off(W, 3.6F, 6.2F * sinF, s * 6.2F * cosF);
            Vector3f F2 = off(W, 0.6F, 6.6F * sinF * 0.98F, s * 6.6F * cosF);
            Vector3f F3 = off(W, -2.4F, 5.6F * sinF * 0.95F, s * 5.6F * cosF * 0.97F);
            Vector3f F4 = off(W, -4.6F, 3.4F * sinF * 0.90F, s * 3.4F * cosF * 0.92F);

            box(buf, m, S, E, 0.42F, 0.30F, 0.22F);
            box(buf, m, E, W, 0.30F, 0.20F, 0.22F);
            box(buf, m, W, F1, 0.13F, 0.04F, 0.22F);
            box(buf, m, W, F2, 0.12F, 0.04F, 0.22F);
            box(buf, m, W, F3, 0.11F, 0.04F, 0.22F);
            box(buf, m, W, F4, 0.10F, 0.04F, 0.22F);

            Vector3f B1 = v(3.2F, 0.3F, s * 1.5F);
            Vector3f B2 = v(-1.5F, 0.2F, s * 1.7F);
            Vector3f B3 = v(-5.0F, 0.1F, s * 1.2F);
            int[] mem = col(0.10F, 0.5F);
            triD(buf, m, S, B1, E, mem);
            triD(buf, m, B1, E, B2, mem);
            triD(buf, m, E, W, B2, mem);
            triD(buf, m, W, F1, F2, mem);
            triD(buf, m, W, F2, F3, mem);
            triD(buf, m, W, F3, F4, mem);
            triD(buf, m, W, F4, B3, mem);
            triD(buf, m, W, B3, B2, mem);
        }
    }
}