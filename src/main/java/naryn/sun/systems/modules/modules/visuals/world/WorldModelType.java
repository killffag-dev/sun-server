package naryn.sun.systems.modules.modules.visuals.world;

/**
 * Перечисление поддерживаемых типов моделей в модуле World.
 */
public enum WorldModelType {
    TOTEM("totem", TotemWorldModel.INSTANCE),
    SWORD("sword", SwordWorldModel.INSTANCE),
    PICKAXE("pickaxe", PickaxeWorldModel.INSTANCE),
    CRYSTAL("crystal", CrystalWorldModel.INSTANCE),
    HEART("heart", HeartWorldModel.INSTANCE);

    private final String id;
    private final WorldModel model;

    WorldModelType(String id, WorldModel model) {
        this.id = id;
        this.model = model;
    }

    public String getId() {
        return id;
    }

    public WorldModel getModel() {
        return model;
    }
}
