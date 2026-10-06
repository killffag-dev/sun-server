package naryn.sun.systems.bbmodel;

import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Один канал анимации (position ИЛИ rotation ИЛИ scale) для одной кости —
 * отсортированный по времени список ключевых кадров.
 *
 * ВАЖНО про упрощение: поддерживается только линейная интерполяция.
 * Если в Blockbench у кадра стоит "Catmull-Rom" или "Step" — они здесь
 * тоже интерполируются линейно (Step не будет резким, Catmull-Rom не
 * будет сглаженным сплайном). Для взмахов крыльев/полёта по кругу разницы
 * почти не видно, но для сложных плавных анимаций может быть заметно —
 * если понадобится точность, это первое место, куда добавлять сплайны.
 */
public final class BbChannelTrack {

    public static final class Keyframe {
        public final float time;
        public final Vector3f value;

        public Keyframe(float time, Vector3f value) {
            this.time = time;
            this.value = value;
        }
    }

    private final List<Keyframe> keyframes = new ArrayList<>();

    public void add(float time, Vector3f value) {
        keyframes.add(new Keyframe(time, value));
    }

    public void sortByTime() {
        keyframes.sort(Comparator.comparingDouble(k -> k.time));
    }

    public boolean isEmpty() {
        return keyframes.isEmpty();
    }

    /** Линейно интерполирует значение канала на момент времени t (секунды). */
    public Vector3f sample(float t) {
        if (keyframes.isEmpty()) return new Vector3f(0, 0, 0);
        if (keyframes.size() == 1 || t <= keyframes.get(0).time) {
            return new Vector3f(keyframes.get(0).value);
        }
        Keyframe last = keyframes.get(keyframes.size() - 1);
        if (t >= last.time) {
            return new Vector3f(last.value);
        }
        for (int i = 0; i < keyframes.size() - 1; i++) {
            Keyframe a = keyframes.get(i);
            Keyframe b = keyframes.get(i + 1);
            if (t >= a.time && t <= b.time) {
                float span = b.time - a.time;
                float f = span < 1.0e-6f ? 0F : (t - a.time) / span;
                return new Vector3f(a.value).lerp(b.value, f);
            }
        }
        return new Vector3f(last.value);
    }
}
