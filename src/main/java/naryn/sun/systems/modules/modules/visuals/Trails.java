package naryn.sun.systems.modules.modules.visuals;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import naryn.sun.access.MCClientAccess;
import naryn.sun.access.MCPlayerAccess;
import naryn.sun.access.MCWorldAccess;
import naryn.sun.systems.event.EventListener;
import naryn.sun.systems.event.impl.render.Render3DEvent;
import naryn.sun.systems.modules.api.ModuleCategory;
import naryn.sun.systems.modules.api.ModuleInfo;
import naryn.sun.systems.modules.impl.BaseModule;
import naryn.sun.systems.setting.settings.BooleanSetting;
import naryn.sun.systems.setting.settings.ColorSetting;
import naryn.sun.systems.setting.settings.ModeSetting;
import naryn.sun.systems.setting.settings.SliderSetting;
import naryn.sun.utility.colors.ColorRGBA;
import naryn.sun.utility.colors.Colors;
import naryn.sun.utility.game.FakePlayerEntity;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerModelPart;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.consume.UseAction;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

@ModuleInfo(name = "Trails", category = ModuleCategory.VISUALS, desc = "Оставляет след из прошлых позиций игрока")
public class Trails extends BaseModule {

    private static final int SUBDIVISIONS = 8;
    private static final float TOP_ALPHA_FACTOR = 0.15F;
    private static final double MIN_RENDER_DISTANCE = 1.0;

    private static final double GHOST_CULL_HALF_WIDTH = 0.4;
    private static final double GHOST_CULL_MIN_Y = -0.2;
    private static final double GHOST_CULL_MAX_Y = 2.0;
    private static final float HOLOGRAM_ALPHA = 0.35F;

    private final ModeSetting mode = new ModeSetting(this, "modules.settings.trails.mode");
    private final ModeSetting.Value lineMode = new ModeSetting.Value(this.mode, "modules.settings.trails.mode.line");
    private final ModeSetting.Value projectionMode = new ModeSetting.Value(this.mode, "modules.settings.trails.mode.projection");

    private final naryn.sun.systems.setting.settings.GroupSetting paramsGroup = new naryn.sun.systems.setting.settings.GroupSetting(this, "modules.settings.trails.group.params");
    private final SliderSetting length = new SliderSetting(this.paramsGroup, "modules.settings.trails.length", () -> !this.lineMode.isSelected())
        .min(5.0F).max(60.0F).step(1.0F).currentValue(20.0F);
    private final SliderSetting cloneCount = new SliderSetting(this.paramsGroup, "modules.settings.trails.clone_count", () -> !this.projectionMode.isSelected())
        .min(2.0F).max(15.0F).step(1.0F).currentValue(5.0F);
    private final SliderSetting spawnDistance = new SliderSetting(this.paramsGroup, "modules.settings.trails.spawn_distance", () -> !this.projectionMode.isSelected())
        .min(0.5F).max(6.0F).step(0.1F).currentValue(2.0F);
    private final SliderSetting fade = new SliderSetting(this.paramsGroup, "modules.settings.trails.fade", () -> !this.projectionMode.isSelected())
        .min(0.5F).max(10.0F).step(0.5F).suffix(" sec").currentValue(2.5F);

    private final naryn.sun.systems.setting.settings.GroupSetting generalGroup = new naryn.sun.systems.setting.settings.GroupSetting(this, "modules.settings.trails.group.general");
    private final BooleanSetting onlyThirdPerson = new BooleanSetting(this.generalGroup, "modules.settings.trails.only_third_person");

    private final naryn.sun.systems.setting.settings.GroupSetting colorGroup = new naryn.sun.systems.setting.settings.GroupSetting(this, "modules.settings.trails.group.color");
    private final ColorSetting color = new ColorSetting(this.colorGroup, "modules.settings.trails.color").color(Colors.ACCENT).alpha(true);

    private final Deque<Vec3d> points = new ArrayDeque<>();
    private final Deque<FakePlayerEntity> ghosts = new ArrayDeque<>();
    private final List<Vec3d> rawBuffer = new ArrayList<>(128);
    private double[] smoothCoords = new double[1024 * 3];
    private int smoothCoordCount = 0;
    private Vec3d lastCapturePos;

    private final EventListener<Render3DEvent> on3DRender = event -> {
        if (this.onlyThirdPerson.isEnabled() && mc.options.getPerspective().isFirstPerson()) {
            return;
        }
        if (this.projectionMode.isSelected()) {
            this.renderProjection(event);
        } else {
            this.renderLine(event);
        }
    };

    @Override
    public void tick() {
        if (!MCPlayerAccess.isPresent()) {
            return;
        }

        if (this.projectionMode.isSelected()) {
            long now = System.currentTimeMillis();
            long lifetimeMs = (long) (this.fade.getCurrentValue() * 1000.0F);
            this.ghosts.removeIf(ghost -> now - ghost.spawnTime >= lifetimeMs);

            Vec3d currentPos = MCPlayerAccess.get().getPos();
            if (this.lastCapturePos == null || currentPos.distanceTo(this.lastCapturePos) >= this.spawnDistance.getCurrentValue()) {
                this.lastCapturePos = currentPos;
                this.ghosts.addLast(this.captureGhost(currentPos));
                while (this.ghosts.size() > (int) this.cloneCount.getCurrentValue()) {
                    this.ghosts.removeFirst();
                }
            }
        } else {
            this.points.addLast(MCPlayerAccess.get().getPos());
            while (this.points.size() > (int) this.length.getCurrentValue()) {
                this.points.removeFirst();
            }
        }
    }

    private FakePlayerEntity captureGhost(Vec3d pos) {
        FakePlayerEntity ghost = new FakePlayerEntity(MCWorldAccess.get(), MCPlayerAccess.get().getGameProfile());
        PlayerEntity player = MCPlayerAccess.get();

        float yaw = player.getYaw();
        float pitch = player.getPitch();
        float bodyYaw = player.getBodyYaw();
        float headYaw = player.getHeadYaw();

        ghost.setPos(pos.x, pos.y, pos.z);
        ghost.prevX = pos.x;
        ghost.prevY = pos.y;
        ghost.prevZ = pos.z;
        ghost.lastRenderX = pos.x;
        ghost.lastRenderY = pos.y;
        ghost.lastRenderZ = pos.z;

        ghost.setYaw(yaw);
        ghost.prevYaw = yaw;
        ghost.setPitch(pitch);
        ghost.prevPitch = pitch;
        ghost.setBodyYaw(bodyYaw);
        ghost.prevBodyYaw = bodyYaw;
        ghost.setHeadYaw(headYaw);
        ghost.prevHeadYaw = headYaw;

        ghost.capturedPitch = pitch;
        ghost.capturedYaw = yaw;
        ghost.capturedHeadYaw = headYaw;
        ghost.capturedBodyYaw = bodyYaw;

        float limbPos = player.limbAnimator.getPos(1.0F);
        float limbSpeed = player.limbAnimator.getSpeed(1.0F);

        // Если игрок в воздухе (прыжок на месте или падение без горизонтальной скорости) —
        // задаём естественный размах ног в воздухе, чтобы манекен не висел вытянутой прямой палкой.
        // При спринт-джампе limbSpeed уже >= 0.15F и сохраняется реальный широкий шаг.
        if (!player.isOnGround() && limbSpeed < 0.15F) {
            ghost.capturedLimbFrequency = 1.0F;
            ghost.capturedLimbAmplitude = 0.45F;
        } else {
            ghost.capturedLimbFrequency = limbPos;
            ghost.capturedLimbAmplitude = limbSpeed;
        }

        ghost.capturedHandSwingProgress = player.getHandSwingProgress(1.0F);
        ghost.capturedPose = player.getPose();
        ghost.capturedSneaking = player.isInSneakingPose();
        ghost.capturedGliding = player.isGliding();
        ghost.capturedSwimming = player.isInSwimmingPose();
        ghost.capturedLeaningPitch = player.getLeaningPitch(1.0F);

        ghost.capturedLeftArmPose = this.getArmPose(player, Arm.LEFT);
        ghost.capturedRightArmPose = this.getArmPose(player, Arm.RIGHT);

        ghost.capturedHatVisible = player.isPartVisible(PlayerModelPart.HAT);
        ghost.capturedJacketVisible = player.isPartVisible(PlayerModelPart.JACKET);
        ghost.capturedLeftPantsVisible = player.isPartVisible(PlayerModelPart.LEFT_PANTS_LEG);
        ghost.capturedRightPantsVisible = player.isPartVisible(PlayerModelPart.RIGHT_PANTS_LEG);
        ghost.capturedLeftSleeveVisible = player.isPartVisible(PlayerModelPart.LEFT_SLEEVE);
        ghost.capturedRightSleeveVisible = player.isPartVisible(PlayerModelPart.RIGHT_SLEEVE);
        ghost.capturedCapeVisible = false;

        ghost.spawnTime = System.currentTimeMillis();

        return ghost;
    }

    private BipedEntityModel.ArmPose getArmPose(PlayerEntity player, Arm arm) {
        ItemStack main = player.getMainHandStack();
        ItemStack off = player.getOffHandStack();
        BipedEntityModel.ArmPose mainPose = this.getArmPose(player, main, Hand.MAIN_HAND);
        BipedEntityModel.ArmPose offPose = this.getArmPose(player, off, Hand.OFF_HAND);
        if (mainPose.isTwoHanded()) {
            offPose = off.isEmpty() ? BipedEntityModel.ArmPose.EMPTY : BipedEntityModel.ArmPose.ITEM;
        }
        return player.getMainArm() == arm ? mainPose : offPose;
    }

    private BipedEntityModel.ArmPose getArmPose(PlayerEntity player, ItemStack stack, Hand hand) {
        if (stack.isEmpty()) {
            return BipedEntityModel.ArmPose.EMPTY;
        }
        if (player.getActiveHand() == hand && player.getItemUseTimeLeft() > 0) {
            UseAction action = stack.getUseAction();
            if (action == UseAction.BLOCK) return BipedEntityModel.ArmPose.BLOCK;
            if (action == UseAction.BOW) return BipedEntityModel.ArmPose.BOW_AND_ARROW;
            if (action == UseAction.SPEAR) return BipedEntityModel.ArmPose.THROW_SPEAR;
            if (action == UseAction.CROSSBOW) return BipedEntityModel.ArmPose.CROSSBOW_CHARGE;
            if (action == UseAction.SPYGLASS) return BipedEntityModel.ArmPose.SPYGLASS;
            if (action == UseAction.TOOT_HORN) return BipedEntityModel.ArmPose.TOOT_HORN;
            if (action == UseAction.BRUSH) return BipedEntityModel.ArmPose.BRUSH;
        } else if (!player.handSwinging && stack.isOf(Items.CROSSBOW) && CrossbowItem.isCharged(stack)) {
            return BipedEntityModel.ArmPose.CROSSBOW_HOLD;
        }
        return BipedEntityModel.ArmPose.ITEM;
    }

    @Override
    public void onDisable() {
        this.points.clear();
        this.ghosts.clear();
        this.lastCapturePos = null;
    }

    // ==================== LINE MODE (Vertical 3D Ribbon) ====================

    private void renderLine(Render3DEvent event) {
        if (this.points.size() < 2 || !MCPlayerAccess.isPresent()) {
            return;
        }

        this.rawBuffer.clear();
        this.rawBuffer.addAll(this.points);
        this.rawBuffer.add(MCPlayerAccess.get().getLerpedPos(event.getTickDelta()));

        this.buildSmoothedCoords(this.rawBuffer);
        if (this.smoothCoordCount < 6) {
            return;
        }

        MatrixStack matrices = event.getMatrices();
        Vec3d cameraPos = event.getCamera().getPos();
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        ColorRGBA base = this.color.getColor();
        float height = MCPlayerAccess.get().getHeight();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        this.buildRibbon(cameraPos, matrix, height, base);

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private void buildRibbon(Vec3d cameraPos, Matrix4f matrix, float height, ColorRGBA base) {
        int count = this.smoothCoordCount / 3;
        BufferBuilder buffer = RenderSystem.renderThreadTesselator().begin(DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);

        int r = (int) base.getRed();
        int g = (int) base.getGreen();
        int b = (int) base.getBlue();
        float baseAlpha = base.getAlpha();

        double camX = cameraPos.x;
        double camY = cameraPos.y;
        double camZ = cameraPos.z;

        for (int i = 0; i < count; i++) {
            int idx = i * 3;
            float bx = (float) (this.smoothCoords[idx] - camX);
            float by = (float) (this.smoothCoords[idx + 1] - camY);
            float bz = (float) (this.smoothCoords[idx + 2] - camZ);
            float ty = by + height;

            float lengthFraction = (float) i / (float) (count - 1);
            int bottomA = Math.round(baseAlpha * lengthFraction);
            int topA = Math.round(bottomA * TOP_ALPHA_FACTOR);

            int bottomColor = ((bottomA & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
            int topColor = ((topA & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);

            buffer.vertex(matrix, bx, ty, bz).color(topColor);
            buffer.vertex(matrix, bx, by, bz).color(bottomColor);
        }

        BuiltBuffer built = buffer.endNullable();
        if (built != null) {
            BufferRenderer.drawWithGlobalProgram(built);
        }
    }

    private void buildSmoothedCoords(List<Vec3d> raw) {
        int n = raw.size();
        this.smoothCoordCount = 0;
        if (n < 2) {
            return;
        }

        int maxSteps = (n - 1) * (SUBDIVISIONS + 1) * 3;
        if (this.smoothCoords.length < maxSteps) {
            this.smoothCoords = new double[maxSteps * 2];
        }

        for (int i = 0; i < n - 1; i++) {
            Vec3d p0 = raw.get(Math.max(i - 1, 0));
            Vec3d p1 = raw.get(i);
            Vec3d p2 = raw.get(i + 1);
            Vec3d p3 = raw.get(Math.min(i + 2, n - 1));

            int steps = (i == n - 2) ? SUBDIVISIONS + 1 : SUBDIVISIONS;
            for (int s = 0; s < steps; s++) {
                float t = (float) s / (float) SUBDIVISIONS;
                this.catmullRomToBuffer(p0, p1, p2, p3, t);
            }
        }
    }

    private void catmullRomToBuffer(Vec3d p0, Vec3d p1, Vec3d p2, Vec3d p3, float t) {
        double t2 = t * t;
        double t3 = t2 * t;

        this.smoothCoords[this.smoothCoordCount++] = 0.5 * (2.0 * p1.x + (-p0.x + p2.x) * t + (2.0 * p0.x - 5.0 * p1.x + 4.0 * p2.x - p3.x) * t2 + (-p0.x + 3.0 * p1.x - 3.0 * p2.x + p3.x) * t3);
        this.smoothCoords[this.smoothCoordCount++] = 0.5 * (2.0 * p1.y + (-p0.y + p2.y) * t + (2.0 * p0.y - 5.0 * p1.y + 4.0 * p2.y - p3.y) * t2 + (-p0.y + 3.0 * p1.y - 3.0 * p2.y + p3.y) * t3);
        this.smoothCoords[this.smoothCoordCount++] = 0.5 * (2.0 * p1.z + (-p0.z + p2.z) * t + (2.0 * p0.z - 5.0 * p1.z + 4.0 * p2.z - p3.z) * t2 + (-p0.z + 3.0 * p1.z - 3.0 * p2.z + p3.z) * t3);
    }

    // ==================== PROJECTION MODE (Frozen Ghosts) ====================

    private void renderProjection(Render3DEvent event) {
        if (!MCPlayerAccess.isPresent()) {
            return;
        }

        long now = System.currentTimeMillis();
        long lifetimeMs = (long) (this.fade.getCurrentValue() * 1000.0F);
        this.ghosts.removeIf(ghost -> now - ghost.spawnTime >= lifetimeMs);

        if (this.ghosts.isEmpty()) {
            return;
        }

        MatrixStack matrices = event.getMatrices();
        Vec3d cameraPos = event.getCamera().getPos();
        EntityRenderDispatcher dispatcher = MCClientAccess.getEntityRenderDispatcher();
        VertexConsumerProvider.Immediate immediate = MCClientAccess.getBufferBuilders().getEntityVertexConsumers();
        int light = LightmapTextureManager.pack(15, 15);
        ColorRGBA base = this.color.getColor();
        float baseAlpha = base.getAlpha() / 255.0F;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        RenderSystem.setShaderColor(
            base.getRed() / 255.0F,
            base.getGreen() / 255.0F,
            base.getBlue() / 255.0F,
            HOLOGRAM_ALPHA * baseAlpha
        );

        for (FakePlayerEntity ghost : this.ghosts) {
            Vec3d ghostPos = ghost.getPos();

            if (ghostPos.distanceTo(cameraPos) < MIN_RENDER_DISTANCE) {
                continue;
            }

            if (cameraPos.x >= ghostPos.x - GHOST_CULL_HALF_WIDTH && cameraPos.x <= ghostPos.x + GHOST_CULL_HALF_WIDTH
                && cameraPos.y >= ghostPos.y + GHOST_CULL_MIN_Y && cameraPos.y <= ghostPos.y + GHOST_CULL_MAX_Y
                && cameraPos.z >= ghostPos.z - GHOST_CULL_HALF_WIDTH && cameraPos.z <= ghostPos.z + GHOST_CULL_HALF_WIDTH) {
                continue;
            }

            double x = ghostPos.x - cameraPos.x;
            double y = ghostPos.y - cameraPos.y;
            double z = ghostPos.z - cameraPos.z;

            dispatcher.render(ghost, x, y, z, 1.0F, matrices, immediate, light);
        }
        immediate.draw();

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }
}
