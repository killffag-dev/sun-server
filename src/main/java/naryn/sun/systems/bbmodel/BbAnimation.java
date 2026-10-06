package naryn.sun.systems.bbmodel;

import java.util.HashMap;
import java.util.Map;

/** Одна именованная анимация из блока "animations" в .bbmodel (например "fly", "idle"). */
public final class BbAnimation {

    public final String name;
    public final float lengthSeconds;
    public final boolean loop;
    public final Map<String, BbBoneAnimator> animatorsByBoneName = new HashMap<>();

    public BbAnimation(String name, float lengthSeconds, boolean loop) {
        this.name = name;
        this.lengthSeconds = lengthSeconds;
        this.loop = loop;
    }

    /**
     * Считает время внутри анимации с учётом loop/clamp и возвращает позу
     * КАЖДОЙ анимируемой кости на этот момент. Кости, для которых в этой
     * анимации нет своего трека, в карту не попадают — рендерер в таком
     * случае просто использует базовую (статическую) позу кости.
     */
    public Map<String, BbBoneAnimator.Pose> sampleAll(float animTimeSeconds) {
        float t = lengthSeconds <= 0F ? 0F
            : (loop ? animTimeSeconds % lengthSeconds : Math.min(animTimeSeconds, lengthSeconds));
        Map<String, BbBoneAnimator.Pose> result = new HashMap<>(animatorsByBoneName.size());
        for (Map.Entry<String, BbBoneAnimator> e : animatorsByBoneName.entrySet()) {
            result.put(e.getKey(), e.getValue().sample(t));
        }
        return result;
    }
}
