package naryn.sun.systems.modules.modules.visuals.world;

/**
 * Интерфейс 3D проволочной модели (wireframe) для модуля World.
 */
public interface WorldModel {
    /**
     * Возвращает плоский массив координат рёбер.
     * Каждое ребро состоит из 6 значений: x1, y1, z1, x2, y2, z2.
     */
    float[] getEdges();
}
