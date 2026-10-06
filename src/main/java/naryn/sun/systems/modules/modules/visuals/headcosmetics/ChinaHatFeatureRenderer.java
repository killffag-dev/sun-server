package naryn.sun.systems.modules.modules.visuals.headcosmetics;

import naryn.sun.Sun;
import naryn.sun.mixin.accessors.BipedEntityModelAccessor;
import naryn.sun.systems.modules.modules.visuals.HeadCosmetics;
import naryn.sun.utility.colors.ColorRGBA;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;


public class ChinaHatFeatureRenderer extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {

    private static final float RADIUS = 10.0F / 16.0F;
    private static final float HEIGHT = 6.0F / 16.0F;
    private static final float Y_OFFSET = -8.5F / 16.0F;
    private static final int SEGMENTS = 20;
    private static final float HAT_ALPHA_MULTIPLIER = 0.5F;

    // Направление "света" вокруг оси конуса (в радианах). Не привязано к реальному
    // солнцу/факелам — фиксированный угол в локальном пространстве головы, даёт
    // стабильный градиент вне зависимости от того, куда игрок повернулся.
    private static final float LIGHT_ANGLE = (float) Math.toRadians(-50.0);

    // Минимальная яркость (теневая сторона) и добавка от "направленного света"
    // (освещённая сторона). AMBIENT + DIFFUSE намеренно > 1.0 — избыток яркости
    // обрезается при клампе и даёт естественный "выбитый" блик на светлой стороне,
    // как на референсе, без ручного подмешивания белого цвета.
    private static final float AMBIENT = 0.55F;
    private static final float DIFFUSE = 0.65F;

    // Кончик шляпы — самостоятельный блик, ярче любой точки на ободке.
    private static final float TIP_BRIGHTNESS = 1.35F;

    // Дно шляпы (нижняя грань) — в тени, равномерно темнее верхней поверхности.
    private static final float BOTTOM_AO = 0.55F;

    public ChinaHatFeatureRenderer(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> context) {
        super(context);
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                        PlayerEntityRenderState state, float limbAngle, float limbDistance) {
        HeadCosmetics module = Sun.getInstance().getModuleManager().getModule(HeadCosmetics.class);
        if (module == null || !module.isChinaHatSelected()) {
            return;
        }

        // Режим "только у меня": рисуем конус только если этот render state
        // принадлежит локальному игроку, на всех остальных сущностях — скип.
        if (module.isOnlyMeVisible() && !this.isLocalPlayerState(state)) {
            return;
        }

        ModelPart head = ((BipedEntityModelAccessor) this.getContextModel()).sun$getHead();

        matrices.push();
        head.rotate(matrices);
        matrices.translate(0.0, Y_OFFSET, 0.0);

        this.drawCone(matrices, vertexConsumers, module.getChinaHatColor());

        matrices.pop();
    }

    private boolean isLocalPlayerState(PlayerEntityRenderState state) {
        ClientPlayerEntity clientPlayer = MinecraftClient.getInstance().player;
        return clientPlayer != null && state.id == clientPlayer.getId();
    }

    private void drawCone(MatrixStack matrices, VertexConsumerProvider vertexConsumers, ColorRGBA color) {
        Matrix4f m = matrices.peek().getPositionMatrix();
        float baseR = color.getRed() / 255F;
        float baseG = color.getGreen() / 255F;
        float baseB = color.getBlue() / 255F;
        float a = (color.getAlpha() / 255F) * HAT_ALPHA_MULTIPLIER;

        float[] ringX = new float[SEGMENTS + 1];
        float[] ringZ = new float[SEGMENTS + 1];
        float[] ringBrightness = new float[SEGMENTS + 1];
        for (int i = 0; i <= SEGMENTS; i++) {
            double ang = 2 * Math.PI * i / SEGMENTS;
            ringX[i] = (float) Math.cos(ang) * RADIUS;
            ringZ[i] = (float) Math.sin(ang) * RADIUS;

            // 0..1 в зависимости от того, насколько эта точка обода "смотрит"
            // в сторону LIGHT_ANGLE — даёт плавный переход свет/тень по кругу.
            float diffuse = (float) (Math.cos(ang - LIGHT_ANGLE) * 0.5 + 0.5);
            ringBrightness[i] = AMBIENT + DIFFUSE * diffuse;
        }

        // Боковая поверхность: кончик — отдельный яркий блик, ободок — по кругу
        // светлее/темнее в зависимости от угла (см. ringBrightness выше).
        VertexConsumer side = vertexConsumers.getBuffer(HeadCosmeticsRenderLayers.HAT_CONE_SIDE);
        this.putShadedVertex(side, m, 0, -HEIGHT, 0, baseR, baseG, baseB, a, TIP_BRIGHTNESS);
        for (int i = 0; i <= SEGMENTS; i++) {
            this.putShadedVertex(side, m, ringX[i], 0, ringZ[i], baseR, baseG, baseB, a, ringBrightness[i]);
        }

        // Дно шляпы — равномерно затемнённое (в тени), обход в обратном порядке
        // относительно боковой поверхности (см. references/entity-overlay-rendering.md).
        // ОБЯЗАТЕЛЬНО отдельный RenderLayer (HAT_CONE_BOTTOM), не HAT_CONE_SIDE —
        // см. javadoc HeadCosmeticsRenderLayers.
        VertexConsumer bottom = vertexConsumers.getBuffer(HeadCosmeticsRenderLayers.HAT_CONE_BOTTOM);
        this.putShadedVertex(bottom, m, 0, 0, 0, baseR, baseG, baseB, a, BOTTOM_AO);
        for (int i = SEGMENTS; i >= 0; i--) {
            this.putShadedVertex(bottom, m, ringX[i], 0, ringZ[i], baseR, baseG, baseB, a, BOTTOM_AO);
        }

        if (vertexConsumers instanceof net.minecraft.client.render.OutlineVertexConsumerProvider outlineConsumers) {
            VertexConsumer outlineSide = outlineConsumers.getBuffer(HeadCosmeticsRenderLayers.HAT_CONE_SIDE_OUTLINE);
            this.putShadedVertex(outlineSide, m, 0, -HEIGHT, 0, baseR, baseG, baseB, 1.0F, 1.0F);
            for (int i = 0; i <= SEGMENTS; i++) {
                this.putShadedVertex(outlineSide, m, ringX[i], 0, ringZ[i], baseR, baseG, baseB, 1.0F, 1.0F);
            }

            VertexConsumer outlineBottom = outlineConsumers.getBuffer(HeadCosmeticsRenderLayers.HAT_CONE_BOTTOM_OUTLINE);
            this.putShadedVertex(outlineBottom, m, 0, 0, 0, baseR, baseG, baseB, 1.0F, 1.0F);
            for (int i = SEGMENTS; i >= 0; i--) {
                this.putShadedVertex(outlineBottom, m, ringX[i], 0, ringZ[i], baseR, baseG, baseB, 1.0F, 1.0F);
            }
        }
    }

    private void putShadedVertex(VertexConsumer buffer, Matrix4f m, float x, float y, float z,
                                  float baseR, float baseG, float baseB, float a, float brightness) {
        float r = Math.min(1.0F, baseR * brightness);
        float g = Math.min(1.0F, baseG * brightness);
        float b = Math.min(1.0F, baseB * brightness);
        buffer.vertex(m, x, y, z).color(r, g, b, a);
    }
}