package naryn.sun.mixin.minecraft.render.entity;

import naryn.sun.Sun;
import naryn.sun.systems.modules.modules.visuals.HeadCosmetics;
import naryn.sun.utility.mixins.EntityRenderStateAddition;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Приподнимает ник (nameLabelPos) над стандартной позицией, когда на сущности
 * отрисована косметика HeadCosmetics (China Hat/Halo) — иначе шляпа/нимб рисуются
 * поверх текста ника, т.к. ChinaHatFeatureRenderer/HaloFeatureRenderer ничего не
 * знают про позицию ника и не могут "подвинуть" его сами.
 *
 * Хук на TAIL updateRenderState: к этому моменту вызов уже прошёл через
 * EntityRenderer#updateRenderState (см. EntityRendererMixin), который проставляет
 * sun$entity через EntityRenderStateAddition — поэтому здесь уже можно получить
 * реальную сущность из стейта.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class HeadCosmeticsNameLabelMixin<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>> {

    @Inject(method = "updateRenderState", at = @At("TAIL"))
    private void sun$offsetNameLabelForHeadCosmetics(T entity, S state, float tickDelta, CallbackInfo ci) {
        HeadCosmetics module = Sun.getInstance().getModuleManager().getModule(HeadCosmetics.class);
        if (module == null) {
            return;
        }

        Entity renderedEntity = ((EntityRenderStateAddition) state).sun$getEntity();
        if (renderedEntity == null) {
            return;
        }

        float extraOffset = module.getNameLabelExtraOffset(renderedEntity);
        if (extraOffset <= 0.0F || state.nameLabelPos == null) {
            return;
        }

        state.nameLabelPos = state.nameLabelPos.add(0.0, extraOffset, 0.0);
    }
}