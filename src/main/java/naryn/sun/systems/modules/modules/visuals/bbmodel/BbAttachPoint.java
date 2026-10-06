package naryn.sun.systems.modules.modules.visuals.bbmodel;

import naryn.sun.mixin.accessors.BbBipedBodyPartsAccessor;
import naryn.sun.mixin.accessors.BipedEntityModelAccessor;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.PlayerEntityModel;

/** К какой части ванильной модели игрока крепить bbmodel-косметику. */
public enum BbAttachPoint {
    HEAD(model -> ((BipedEntityModelAccessor) model).sun$getHead()),
    BODY(model -> ((BbBipedBodyPartsAccessor) model).sun$bb$getBody()),
    RIGHT_ARM(model -> ((BbBipedBodyPartsAccessor) model).sun$bb$getRightArm()),
    LEFT_ARM(model -> ((BbBipedBodyPartsAccessor) model).sun$bb$getLeftArm()),
    RIGHT_LEG(model -> ((BbBipedBodyPartsAccessor) model).sun$bb$getRightLeg()),
    LEFT_LEG(model -> ((BbBipedBodyPartsAccessor) model).sun$bb$getLeftLeg());

    private final java.util.function.Function<PlayerEntityModel, ModelPart> getter;

    BbAttachPoint(java.util.function.Function<PlayerEntityModel, ModelPart> getter) {
        this.getter = getter;
    }

    public ModelPart get(PlayerEntityModel model) {
        return getter.apply(model);
    }
}
