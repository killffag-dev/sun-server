package naryn.sun.systems.bbmodel;

import org.joml.Vector3f;

import java.util.EnumMap;
import java.util.Map;

/**
 * Один "куб" (element) из .bbmodel — то, что в Blockbench называется Cube/box.
 * Координаты from/to/origin приходят из файла в "пиксельных" юнитах Blockbench
 * (16 юнитов = 1 блок Minecraft, та же система, что в ванильных ModelPart).
 * Перевод в блоки (/16) делает {@link BbModelRenderer}, здесь хранится сырьё.
 */
public final class BbCube {

    public enum Face { NORTH, SOUTH, EAST, WEST, UP, DOWN }

    public final Vector3f from;
    public final Vector3f to;

    /** Локальный пивот куба (может отличаться от пивота кости — Blockbench это разрешает). Может быть null. */
    public final Vector3f origin;
    /** Локальный поворот куба вокруг origin, в градусах. Может быть null (= без поворота). */
    public final Vector3f rotation;

    /** UV-прямоугольник на текстуре для каждой грани, в пикселях текстуры (x1,y1,x2,y2). Отсутствующая грань не рисуется. */
    public final Map<Face, float[]> faceUv;

    public BbCube(Vector3f from, Vector3f to, Vector3f origin, Vector3f rotation, Map<Face, float[]> faceUv) {
        this.from = from;
        this.to = to;
        this.origin = origin;
        this.rotation = rotation;
        this.faceUv = faceUv == null ? new EnumMap<>(Face.class) : faceUv;
    }
}
