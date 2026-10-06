package naryn.sun.systems.modules.modules.visuals.bbmodel;

import naryn.sun.systems.bbmodel.BbAnimation;
import naryn.sun.systems.bbmodel.BbBoneAnimator;
import naryn.sun.systems.bbmodel.BbModel;
import naryn.sun.systems.bbmodel.BbModelRenderer;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;

import java.util.Collections;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Универсальный рендерер bbmodel-косметики на игроке — аналог твоего
 * ChinaHatFeatureRenderer, но модель грузится из .bbmodel вместо того, чтобы
 * быть закодированной руками, и умеет проигрывать анимацию.
 *
 * ИСПОЛЬЗОВАНИЕ (пример — крылья дракона на спине):
 * <pre>
 *   this.addFeature(new BbCosmeticFeatureRenderer(
 *       (FeatureRendererContext&lt;PlayerEntityRenderState, PlayerEntityModel&gt;) this,
 *       () -&gt; MyCosmeticsModule.INSTANCE.isDragonWingsSelected(),
 *       () -&gt; DragonWingsBbModel.MODEL,       // BbModel, загруженный один раз при старте
 *       BbAttachPoint.BODY,
 *       0.0, -6.0/16.0, -2.0/16.0,             // смещение относительно тела, если модель не по центру
 *       "flap"                                 // имя анимации внутри .bbmodel, или null для статичной позы
 *   ));
 * </pre>
 *
 * ВАЖНО про глубину: как и в ChinaHatFeatureRenderer, мы рисуем напрямую
 * через Tesselator в обход батчинга vertexConsumers — форсируем flush уже
 * поставленной в очередь геометрии (брони и т.д.) ДО своей отрисовки, иначе
 * она "провалится" за нашей моделью по тесту глубины.
 */
public class BbCosmeticFeatureRenderer extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {

    private final Supplier<Boolean> enabled;
    private final Supplier<BbModel> modelSupplier;
    private final BbAttachPoint attachPoint;
    private final double offsetX, offsetY, offsetZ;
    private final String animationName; // может быть null

    private long startTimeMs = -1L;

    public BbCosmeticFeatureRenderer(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> context,
                                      Supplier<Boolean> enabled, Supplier<BbModel> modelSupplier,
                                      BbAttachPoint attachPoint,
                                      double offsetX, double offsetY, double offsetZ,
                                      String animationName) {
        super(context);
        this.enabled = enabled;
        this.modelSupplier = modelSupplier;
        this.attachPoint = attachPoint;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.animationName = animationName;
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                        PlayerEntityRenderState state, float limbAngle, float limbDistance) {
        if (!Boolean.TRUE.equals(enabled.get())) return;
        BbModel model = modelSupplier.get();
        if (model == null) return;

        if (vertexConsumers instanceof VertexConsumerProvider.Immediate immediate) {
            immediate.draw();
        }

        if (startTimeMs < 0L) {
            startTimeMs = System.currentTimeMillis();
        }

        ModelPart part = attachPoint.get(this.getContextModel());

        matrices.push();
        part.rotate(matrices);
        matrices.translate(offsetX, offsetY, offsetZ);

        Map<String, BbBoneAnimator.Pose> pose = Collections.emptyMap();
        if (animationName != null) {
            BbAnimation anim = model.animation(animationName);
            if (anim != null) {
                float t = (System.currentTimeMillis() - startTimeMs) / 1000.0F;
                pose = anim.sampleAll(t);
            }
        }

        BbModelRenderer.render(model, matrices, pose, 1.0F, 1.0F, 1.0F, 1.0F);

        matrices.pop();
    }
}
