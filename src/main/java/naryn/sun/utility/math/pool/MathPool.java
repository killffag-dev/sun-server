package naryn.sun.utility.math.pool;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * Централизованный пул математических объектов для предотвращения аллокаций
 * в горячих путях рендера (3D события, шейдеры, партиклы, геометрия).
 */
public final class MathPool {

    private static final ObjectPool<Vector3f> VECTOR3F_POOL = new ObjectPool<>(
            Vector3f::new,
            v -> v.set(0.0F, 0.0F, 0.0F),
            128
    );

    private static final ObjectPool<Vector4f> VECTOR4F_POOL = new ObjectPool<>(
            Vector4f::new,
            v -> v.set(0.0F, 0.0F, 0.0F, 0.0F),
            64
    );

    private static final ObjectPool<Matrix4f> MATRIX4F_POOL = new ObjectPool<>(
            Matrix4f::new,
            Matrix4f::identity,
            64
    );

    private static final ObjectPool<Quaternionf> QUATERNIONF_POOL = new ObjectPool<>(
            Quaternionf::new,
            Quaternionf::identity,
            32
    );

    private MathPool() {
    }

    public static Vector3f vec3() {
        return VECTOR3F_POOL.obtain();
    }

    public static Vector3f vec3(float x, float y, float z) {
        Vector3f v = VECTOR3F_POOL.obtain();
        v.set(x, y, z);
        return v;
    }

    public static void release(Vector3f vector) {
        VECTOR3F_POOL.release(vector);
    }

    public static Vector4f vec4() {
        return VECTOR4F_POOL.obtain();
    }

    public static Vector4f vec4(float x, float y, float z, float w) {
        Vector4f v = VECTOR4F_POOL.obtain();
        v.set(x, y, z, w);
        return v;
    }

    public static void release(Vector4f vector) {
        VECTOR4F_POOL.release(vector);
    }

    public static Matrix4f mat4() {
        return MATRIX4F_POOL.obtain();
    }

    public static Matrix4f mat4(Matrix4f src) {
        Matrix4f m = MATRIX4F_POOL.obtain();
        m.set(src);
        return m;
    }

    public static void release(Matrix4f matrix) {
        MATRIX4F_POOL.release(matrix);
    }

    public static Quaternionf quat() {
        return QUATERNIONF_POOL.obtain();
    }

    public static void release(Quaternionf quat) {
        QUATERNIONF_POOL.release(quat);
    }
}
