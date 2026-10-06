package naryn.sun.systems.modules.modules.visuals.headcosmetics;

import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import naryn.sun.Sun;
import naryn.sun.mixin.accessors.BipedEntityModelAccessor;
import naryn.sun.systems.modules.modules.visuals.HeadCosmetics;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;


/**
 * ТЕЛО ТОРА (drawTorus): раньше рисовалось напрямую через Tesselator с
 * depthMask(false), по той же причине, что и в ChinaHatFeatureRenderer (не
 * блокировать поздний flush ника игрока) — и с тем же побочным эффектом:
 * модуль Sky стирал кольцо на фоне открытого неба, так как настоящая глубина
 * никогда не записывалась (см. references/entity-overlay-rendering.md, п.6
 * в sun-client skill). Теперь тор рисуется через RenderLayer
 * (HeadCosmeticsRenderLayers.HALO_TORUS) и vertexConsumers — глубина пишется
 * нормально, depthMask-трюк и связанный с ним форс-flush для этой части
 * больше не нужны.
 *
 * СВЕЧЕНИЕ (drawGlow) — ОТКРЫТЫЙ ПУНКТ: всё ещё рисуется старым способом
 * (Tesselator + depthMask(false)), потому что ему нужен текстурированный
 * (bloom.png) аддитивный RenderLayer, а точное имя нужной RenderPhase.
 * ShaderProgram-константы под VertexFormats.POSITION_TEXTURE_COLOR не было
 * подтверждено декомпиляцией — угадывать не стали. Значит свечение всё ещё
 * потенциально стирается модулем Sky на фоне открытого неба. Форс-flush
 * в начале render() оставлен ИМЕННО ради этой части.
 */
public class HaloFeatureRenderer extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {

    // Геометрия тора: MAJOR_RADIUS — радиус самого кольца (до центра "трубки"),
    // MINOR_RADIUS — толщина "трубки" (то, что делает кольцо круглым в сечении).
    private static final float MAJOR_RADIUS = 6.3F / 16.0F;
    private static final float MINOR_RADIUS = 0.75F / 16.0F;
    private static final int MAJOR_SEGMENTS = 32;
    private static final int MINOR_SEGMENTS = 10;

    private static final float Y_OFFSET = -14.0F / 16.0F;

    // Ядро тора — почти белое с тёплым жёлтым оттенком.
    private static final float CORE_R = 1.0F;
    private static final float CORE_G = 0.97F;
    private static final float CORE_B = 0.85F;
    private static final float CORE_ALPHA = 0.92F;

    // Свечение: billboard-точки, расставленные по окружности кольца,
    // каждая развёрнута к камере отдельно (иначе будут видны с ребра
    // под некоторыми углами — см. первую неудачную попытку).
    private static final int GLOW_BLOBS = 40;
    private static final float GLOW_SIZE = 3.4F / 16.0F;
    private static final float GLOW_ALPHA = 0.55F;
    private static final float GLOW_R = 1.0F;
    private static final float GLOW_G = 0.87F;
    private static final float GLOW_B = 0.5F;

    public HaloFeatureRenderer(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> context) {
        super(context);
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                        PlayerEntityRenderState state, float limbAngle, float limbDistance) {
        HeadCosmetics module = Sun.getInstance().getModuleManager().getModule(HeadCosmetics.class);
        if (module == null || !module.isHaloSelected()) {
            return;
        }

        // Режим "только у меня": рисуем нимб только если этот render state
        // принадлежит локальному игроку, на всех остальных сущностях — скип.
        if (module.isOnlyMeVisible() && !this.isLocalPlayerState(state)) {
            return;
        }

        // Форсируем flush уже поставленной в очередь геометрии (броня, предмет
        // в руке и т.д.), чтобы она попала в буфер глубины ДО того, как
        // drawGlow начнёт писать свою (drawTorus теперь через vertexConsumers,
        // в этом флаше больше не нуждается — см. javadoc класса).
        if (vertexConsumers instanceof VertexConsumerProvider.Immediate immediate) {
            immediate.draw();
        }

        ModelPart head = ((BipedEntityModelAccessor) this.getContextModel()).sun$getHead();

        matrices.push();
        head.rotate(matrices);
        matrices.translate(0.0, Y_OFFSET, 0.0);
        // Ни наклона, ни вращения — нимб полностью статичен и всегда
        // горизонтален относительно головы.

        this.drawGlow(matrices);
        this.drawTorus(matrices, vertexConsumers);

        matrices.pop();
    }

    private boolean isLocalPlayerState(PlayerEntityRenderState state) {
        ClientPlayerEntity clientPlayer = MinecraftClient.getInstance().player;
        return clientPlayer != null && state.id == clientPlayer.getId();
    }

    /** Плотное "тело" кольца — настоящая 3D-геометрия тора, не плоский диск. */
    private void drawTorus(MatrixStack matrices, VertexConsumerProvider vertexConsumers) {
        Matrix4f m = matrices.peek().getPositionMatrix();
        VertexConsumer buffer = vertexConsumers.getBuffer(HeadCosmeticsRenderLayers.HALO_TORUS);
        this.emitTorus(buffer, m);

        if (vertexConsumers instanceof net.minecraft.client.render.OutlineVertexConsumerProvider outlineConsumers) {
            VertexConsumer outlineBuffer = outlineConsumers.getBuffer(HeadCosmeticsRenderLayers.HALO_TORUS_OUTLINE);
            this.emitTorus(outlineBuffer, m);
        }
    }

    private void emitTorus(VertexConsumer buffer, Matrix4f m) {
        for (int i = 0; i < MAJOR_SEGMENTS; i++) {
            float u0 = (float) (2 * Math.PI * i / MAJOR_SEGMENTS);
            float u1 = (float) (2 * Math.PI * (i + 1) / MAJOR_SEGMENTS);

            for (int j = 0; j < MINOR_SEGMENTS; j++) {
                float v0 = (float) (2 * Math.PI * j / MINOR_SEGMENTS);
                float v1 = (float) (2 * Math.PI * (j + 1) / MINOR_SEGMENTS);

                this.torusVertex(buffer, m, u0, v0);
                this.torusVertex(buffer, m, u1, v0);
                this.torusVertex(buffer, m, u1, v1);
                this.torusVertex(buffer, m, u0, v1);
            }
        }
    }

    private void torusVertex(VertexConsumer buffer, Matrix4f m, float u, float v) {
        float cu = (float) Math.cos(u);
        float su = (float) Math.sin(u);
        float cv = (float) Math.cos(v);
        float sv = (float) Math.sin(v);

        float ringRadius = MAJOR_RADIUS + MINOR_RADIUS * cv;
        float x = ringRadius * cu;
        float y = MINOR_RADIUS * sv;
        float z = ringRadius * su;

        // Дешёвая имитация освещения без нормального шейдинга: верх "трубки"
        // (sv > 0) чуть светлее, низ чуть темнее — иначе плоская заливка
        // цветом читается глазом как плоский диск, а не круглое сечение.
        float brightness = 0.78F + 0.22F * sv;

        buffer.vertex(m, x, y, z)
            .color(CORE_R * brightness, CORE_G * brightness, CORE_B * brightness, CORE_ALPHA);
    }

    /** Мягкое свечение — billboard-точки на bloom.png, развёрнутые к камере, как в World.java. */
    private void drawGlow(MatrixStack matrices) {
        Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
        RenderSystem.enableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);

        Identifier id = Sun.id("textures/bloom.png");
        RenderSystem.setShaderTexture(0, id);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);

        BufferBuilder buffer = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        for (int i = 0; i < GLOW_BLOBS; i++) {
            float u = (float) (2 * Math.PI * i / GLOW_BLOBS);
            float cu = (float) Math.cos(u);
            float su = (float) Math.sin(u);

            matrices.push();
            matrices.translate(MAJOR_RADIUS * cu, 0.0, MAJOR_RADIUS * su);
            // Разворачиваем квад лицом к камере — именно этого не хватало
            // в предыдущей версии со свечением по касательной.
            matrices.multiply(camera.getRotation());

            Matrix4f m = matrices.peek().getPositionMatrix();
            float half = GLOW_SIZE / 2.0F;

            buffer.vertex(m, -half, -half, 0).texture(0.0F, 1.0F).color(GLOW_R, GLOW_G, GLOW_B, GLOW_ALPHA);
            buffer.vertex(m, half, -half, 0).texture(1.0F, 1.0F).color(GLOW_R, GLOW_G, GLOW_B, GLOW_ALPHA);
            buffer.vertex(m, half, half, 0).texture(1.0F, 0.0F).color(GLOW_R, GLOW_G, GLOW_B, GLOW_ALPHA);
            buffer.vertex(m, -half, half, 0).texture(0.0F, 0.0F).color(GLOW_R, GLOW_G, GLOW_B, GLOW_ALPHA);

            matrices.pop();
        }

        BuiltBuffer built = buffer.endNullable();
        if (built != null) {
            BufferRenderer.drawWithGlobalProgram(built);
        }

        RenderSystem.depthMask(true);
        RenderSystem.setShaderTexture(0, 0);
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
    }
}