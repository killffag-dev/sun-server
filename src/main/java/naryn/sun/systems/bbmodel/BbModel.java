package naryn.sun.systems.bbmodel;

import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Полностью распарсенная модель Blockbench: дерево костей + текстура + анимации. */
public final class BbModel {

    public final String name;
    public final int textureWidth;
    public final int textureHeight;
    /** Идентификатор уже зарегистрированной в TextureManager текстуры (см. BbModelLoader). Null, если текстуры не было. */
    public final Identifier textureId;

    public final List<BbBone> rootBones = new ArrayList<>();
    public final Map<String, BbAnimation> animations = new HashMap<>();

    public BbModel(String name, int textureWidth, int textureHeight, Identifier textureId) {
        this.name = name;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.textureId = textureId;
    }

    public BbAnimation animation(String animationName) {
        return animations.get(animationName);
    }
}
