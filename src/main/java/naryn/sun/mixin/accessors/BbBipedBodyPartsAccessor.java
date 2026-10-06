package naryn.sun.mixin.accessors;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * У тебя в проекте уже есть BipedEntityModelAccessor (head/hat) — этот
 * аксессор ДОПОЛНЯЕТ его остальными частями тела, чтобы косметику можно
 * было крепить не только на голову. Два @Mixin-интерфейса на один и тот же
 * таргет-класс — это нормально, Mixin спокойно объединяет несколько
 * accessor-миксинов на одном классе. Просто добавь эту строку в
 * sun.mixins.json рядом с существующей записью про BipedEntityModelAccessor.
 */
@Mixin(BipedEntityModel.class)
public interface BbBipedBodyPartsAccessor {

    @Accessor("body")
    ModelPart sun$bb$getBody();

    @Accessor("rightArm")
    ModelPart sun$bb$getRightArm();

    @Accessor("leftArm")
    ModelPart sun$bb$getLeftArm();

    @Accessor("rightLeg")
    ModelPart sun$bb$getRightLeg();

    @Accessor("leftLeg")
    ModelPart sun$bb$getLeftLeg();
}
