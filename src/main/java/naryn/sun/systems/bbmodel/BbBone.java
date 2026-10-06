package naryn.sun.systems.bbmodel;

import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * "Кость" — соответствует группе (Group) в дереве outliner у Blockbench.
 * Имя кости = имя группы в Blockbench, оно же используется как ключ
 * анимационных треков (animators) в файле анимации, поэтому имена костей
 * и групп в Blockbench должны совпадать 1:1 с тем, что ты хочешь анимировать.
 */
public final class BbBone {

    public final String name;
    /** Точка вращения кости, в "пиксельных" юнитах Blockbench (см. BbCube). */
    public final Vector3f pivot;
    /** Базовый (статический, заданный в самом Blockbench) поворот кости в градусах. */
    public final Vector3f baseRotation;

    public final List<BbCube> cubes = new ArrayList<>();
    public final List<BbBone> children = new ArrayList<>();

    public BbBone(String name, Vector3f pivot, Vector3f baseRotation) {
        this.name = name;
        this.pivot = pivot;
        this.baseRotation = baseRotation;
    }
}
