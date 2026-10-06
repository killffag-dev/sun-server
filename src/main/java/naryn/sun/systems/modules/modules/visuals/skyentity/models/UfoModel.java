package naryn.sun.systems.modules.modules.visuals.skyentity.models;

import static naryn.sun.systems.modules.modules.visuals.skyentity.SkyEntityGeometry.*;

import naryn.sun.systems.modules.modules.visuals.skyentity.SkyEntityModel;
import net.minecraft.client.render.BufferBuilder;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Процедурная модель НЛО: "лататый" (lathe) корпус диск+купол, крутящийся вокруг своей оси,
 * кольцо пульсирующих тёплых огней по кромке и три опорные ноги снизу.
 * Вся геометрия пересчитывается каждый кадр — ассетов не требует.
 *
 * SCALE подобран так, чтобы общий габарит был сопоставим с DragonModel при том же
 * значении слайдера size в SkyEntityModule (профиль сам по себе компактный, ~3.4 юнита
 * диаметром, поэтому масштабируем его до ~14 юнитов).
 */
public class UfoModel implements SkyEntityModel {

    private static final float SCALE = 4.0F;

    // Профиль корпуса: {радиус, высота}, от макушки купола вниз до нижнего полюса брюха.
    // Значения — в "базовых" юнитах, домножаются на SCALE при построении.
    private static final float[][] PROFILE = {
        {0.00F, 0.95F},   // 0 макушка купола
        {0.30F, 0.75F},   // 1
        {0.44F, 0.52F},   // 2
        {0.50F, 0.32F},   // 3 низ купола / верх диска
        {1.55F, 0.15F},   // 4 верхняя кромка диска
        {1.72F, 0.00F},   // 5 самая широкая точка — здесь огни
        {1.50F, -0.13F},  // 6 нижняя кромка диска
        {0.55F, -0.30F},  // 7 сужение к брюху
        {0.00F, -0.42F},  // 8 нижний полюс
    };
    private static final int LIGHT_ROW = 5;
    private static final int SEGMENTS = 16;
    private static final int LIGHT_COUNT = 10;
    private static final float HULL_SHADE = 0.85F;

    @Override
    public void build(BufferBuilder buf, Matrix4f m, float t, float phase) {
        float spin = t * 0.5F;

        int rows = PROFILE.length;
        Vector3f[][] rings = new Vector3f[rows][];
        for (int r = 0; r < rows; r++) {
            float radius = PROFILE[r][0] * SCALE;
            float y = PROFILE[r][1] * SCALE;
            if (radius <= 0.0001F) {
                rings[r] = null; // полюс — одна точка
                continue;
            }
            Vector3f[] ring = new Vector3f[SEGMENTS];
            for (int i = 0; i < SEGMENTS; i++) {
                float a = spin + (float) (2.0 * Math.PI * i / SEGMENTS);
                ring[i] = v((float) Math.cos(a) * radius, y, (float) Math.sin(a) * radius);
            }
            rings[r] = ring;
        }

        // --- Пояса между соседними рядами профиля ---
        for (int r = 0; r < rows - 1; r++) {
            Vector3f[] a = rings[r];
            Vector3f[] b = rings[r + 1];
            float dy = PROFILE[r + 1][1] - PROFILE[r][1];
            float ny = Math.max(-1F, Math.min(1F, dy * 3F));
            int[] c = col(HULL_SHADE, ny);

            if (a == null) {
                Vector3f pole = v(0, PROFILE[r][1] * SCALE, 0);
                for (int i = 0; i < SEGMENTS; i++) {
                    int j = (i + 1) % SEGMENTS;
                    triD(buf, m, pole, b[i], b[j], c);
                }
            } else if (b == null) {
                Vector3f pole = v(0, PROFILE[r + 1][1] * SCALE, 0);
                for (int i = 0; i < SEGMENTS; i++) {
                    int j = (i + 1) % SEGMENTS;
                    triD(buf, m, a[i], a[j], pole, c);
                }
            } else {
                for (int i = 0; i < SEGMENTS; i++) {
                    int j = (i + 1) % SEGMENTS;
                    quad(buf, m, a[i], a[j], b[j], b[i], c);
                }
            }
        }

        // --- Три опорные ноги ---
        for (int i = 0; i < 3; i++) {
            float a = spin + (float) (2.0 * Math.PI * i / 3.0) + 0.35F;
            float rx = (float) Math.cos(a) * 1.15F * SCALE;
            float rz = (float) Math.sin(a) * 1.15F * SCALE;
            Vector3f hip = v(rx, -0.22F * SCALE, rz);
            Vector3f foot = v(rx * 1.18F, -0.62F * SCALE, rz * 1.18F);
            box(buf, m, hip, foot, 0.09F * SCALE, 0.05F * SCALE, HULL_SHADE);
        }

        // --- Кольцо пульсирующих огней по кромке (бегущая волна) ---
        for (int i = 0; i < LIGHT_COUNT; i++) {
            float a = spin + (float) (2.0 * Math.PI * i / LIGHT_COUNT);
            float r = (PROFILE[LIGHT_ROW][0] + 0.06F) * SCALE;
            float y = PROFILE[LIGHT_ROW][1] * SCALE;
            Vector3f center = v((float) Math.cos(a) * r, y, (float) Math.sin(a) * r);

            float pulse = 0.5F + 0.5F * (float) Math.sin(t * 3.0 - i * 0.9);
            float bright = 0.35F + 0.65F * pulse;
            float lr = bright, lg = bright * 0.85F, lb = bright * 0.25F; // тёплый жёлто-оранжевый

            Vector3f up = off(center, 0, 0.05F * SCALE, 0);
            Vector3f down = off(center, 0, -0.05F * SCALE, 0);
            Vector3f out = off(center, (float) Math.cos(a) * 0.08F * SCALE, 0, (float) Math.sin(a) * 0.08F * SCALE);
            lightTri(buf, m, up, down, out, lr, lg, lb);
        }
    }

    private static void lightTri(BufferBuilder buf, Matrix4f m, Vector3f p1, Vector3f p2, Vector3f p3, float r, float g, float b) {
        buf.vertex(m, p1.x, p1.y, p1.z).color(r, g, b, 1.0F);
        buf.vertex(m, p2.x, p2.y, p2.z).color(r, g, b, 1.0F);
        buf.vertex(m, p3.x, p3.y, p3.z).color(r, g, b, 1.0F);
    }
}