package naryn.sun.systems.bbmodel;

import org.joml.Vector3f;

/** Три канала анимации одной кости: смещение позиции, доп. поворот, доп. масштаб. */
public final class BbBoneAnimator {

    public final BbChannelTrack position = new BbChannelTrack();
    public final BbChannelTrack rotation = new BbChannelTrack();
    public final BbChannelTrack scale = new BbChannelTrack();

    /** Итоговая поза кости в конкретный момент времени. */
    public static final class Pose {
        public static final Pose IDENTITY = new Pose(new Vector3f(0, 0, 0), new Vector3f(0, 0, 0), new Vector3f(1, 1, 1));

        public final Vector3f positionOffset;
        public final Vector3f rotationOffset;
        public final Vector3f scale;

        public Pose(Vector3f positionOffset, Vector3f rotationOffset, Vector3f scale) {
            this.positionOffset = positionOffset;
            this.rotationOffset = rotationOffset;
            this.scale = scale;
        }
    }

    public Pose sample(float t) {
        Vector3f pos = position.isEmpty() ? new Vector3f(0, 0, 0) : position.sample(t);
        Vector3f rot = rotation.isEmpty() ? new Vector3f(0, 0, 0) : rotation.sample(t);
        Vector3f scl = scale.isEmpty() ? new Vector3f(1, 1, 1) : scale.sample(t);
        return new Pose(pos, rot, scl);
    }
}
