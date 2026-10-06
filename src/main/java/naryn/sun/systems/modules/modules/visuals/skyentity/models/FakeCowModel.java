package naryn.sun.systems.modules.modules.visuals.skyentity.models;

import static naryn.sun.systems.modules.modules.visuals.skyentity.SkyEntityGeometry.*;

import naryn.sun.systems.modules.modules.visuals.skyentity.SkyEntityModel;
import net.minecraft.client.render.BufferBuilder;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Фейковая процедурная корова для сцены похищения НЛО.
 * Не связана с настоящим CowEntity — чисто визуальный элемент, использует те же
 * хелперы (box()), что и остальные модели, поэтому цвет получается в общей
 * сине-серой палитре col() — визуально это скорее "призрачная" корова в луче,
 * что для темы похищения даже уместно.
 */
public class FakeCowModel implements SkyEntityModel {

    @Override
    public void build(BufferBuilder buf, Matrix4f m, float t, float phase) {
        float shade = 0.55F;

        // --- Тело ---
        Vector3f bodyBack = v(-0.9F, 0, 0);
        Vector3f bodyFront = v(0.9F, 0, 0);
        box(buf, m, bodyBack, bodyFront, 0.55F, 0.55F, shade);

        // --- Шея и голова ---
        Vector3f neckEnd = off(bodyFront, 0.55F, 0.15F, 0);
        box(buf, m, bodyFront, neckEnd, 0.30F, 0.24F, shade);
        Vector3f headEnd = off(neckEnd, 0.45F, -0.05F, 0);
        box(buf, m, neckEnd, headEnd, 0.24F, 0.18F, shade);

        // --- Рога ---
        for (int s = -1; s <= 1; s += 2) {
            box(buf, m, off(neckEnd, 0.10F, 0.20F, s * 0.15F), off(neckEnd, 0.25F, 0.45F, s * 0.30F), 0.05F, 0.02F, shade);
        }

        // --- Уши ---
        for (int s = -1; s <= 1; s += 2) {
            box(buf, m, off(neckEnd, 0.05F, 0.10F, s * 0.20F), off(neckEnd, 0.05F, 0.10F, s * 0.45F), 0.09F, 0.02F, shade);
        }

        // --- Хвост, слегка помахивает ---
        Vector3f tailEnd = off(bodyBack, -0.5F, -0.2F + (float) Math.sin(t * 3.0) * 0.08F, 0);
        box(buf, m, bodyBack, tailEnd, 0.06F, 0.10F, shade);

        // --- Ноги: болтаются, пока корову тащит луч ---
        float kick = (float) Math.sin(t * 6.0) * 0.35F;
        float[] legX = {0.6F, 0.6F, -0.6F, -0.6F};
        float[] legZ = {0.32F, -0.32F, 0.32F, -0.32F};
        for (int i = 0; i < 4; i++) {
            float k = kick * (i % 2 == 0 ? 1F : -1F);
            Vector3f hip = v(legX[i], -0.5F, legZ[i]);
            Vector3f foot = off(hip, k * 0.3F, -0.65F, k * 0.15F);
            box(buf, m, hip, foot, 0.14F, 0.10F, shade);
        }
    }
}