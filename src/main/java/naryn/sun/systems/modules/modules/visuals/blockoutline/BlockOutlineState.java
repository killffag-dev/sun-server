package naryn.sun.systems.modules.modules.visuals.blockoutline;

import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.shape.VoxelShape;

public final class BlockOutlineState {

    private Box currentBox;
    private Box targetBox;
    private BlockPos currentPos;
    private VoxelShape currentShape;
    private float alpha = 0.0F;
    private long lastTime = 0L;

    public void update(MinecraftClient mc, boolean smooth, float smoothSpeed) {
        long now = System.currentTimeMillis();
        float deltaSeconds = this.lastTime > 0L ? (now - this.lastTime) / 1000.0F : 0.016F;
        this.lastTime = now;
        deltaSeconds = MathHelper.clamp(deltaSeconds, 0.001F, 0.1F);

        if (mc.world == null || mc.player == null) {
            this.alpha = 0.0F;
            this.currentBox = null;
            this.targetBox = null;
            return;
        }

        HitResult hit = mc.crosshairTarget;
        boolean hasTarget = false;

        if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = blockHit.getBlockPos();
            BlockState state = mc.world.getBlockState(pos);

            if (!state.isAir() && mc.world.getWorldBorder().contains(pos)) {
                VoxelShape shape = state.getOutlineShape(mc.world, pos, ShapeContext.of(mc.player));
                if (!shape.isEmpty()) {
                    hasTarget = true;
                    this.currentPos = pos;
                    this.currentShape = shape;

                    Box bounding = shape.getBoundingBox().offset(pos);
                    this.targetBox = bounding;

                    if (this.currentBox == null || !smooth) {
                        this.currentBox = bounding;
                    }
                }
            }
        }

        // Плавная интерполяция позиции бокса (Smooth transition)
        if (this.currentBox != null && this.targetBox != null) {
            if (smooth) {
                float factor = 1.0F - (float) Math.exp(-smoothSpeed * deltaSeconds);
                factor = MathHelper.clamp(factor, 0.01F, 1.0F);

                this.currentBox = new Box(
                    MathHelper.lerp(factor, this.currentBox.minX, this.targetBox.minX),
                    MathHelper.lerp(factor, this.currentBox.minY, this.targetBox.minY),
                    MathHelper.lerp(factor, this.currentBox.minZ, this.targetBox.minZ),
                    MathHelper.lerp(factor, this.currentBox.maxX, this.targetBox.maxX),
                    MathHelper.lerp(factor, this.currentBox.maxY, this.targetBox.maxY),
                    MathHelper.lerp(factor, this.currentBox.maxZ, this.targetBox.maxZ)
                );
            } else {
                this.currentBox = this.targetBox;
            }
        }

        // Плавный Fade In / Fade Out
        float fadeSpeed = 8.0F * deltaSeconds;
        if (hasTarget) {
            this.alpha = Math.min(1.0F, this.alpha + fadeSpeed);
        } else {
            this.alpha = Math.max(0.0F, this.alpha - fadeSpeed);
            if (this.alpha <= 0.001F) {
                this.currentBox = null;
                this.targetBox = null;
                this.currentPos = null;
                this.currentShape = null;
            }
        }
    }

    public void reset() {
        this.currentBox = null;
        this.targetBox = null;
        this.currentPos = null;
        this.currentShape = null;
        this.alpha = 0.0F;
        this.lastTime = 0L;
    }

    public boolean shouldRender() {
        return this.alpha > 0.005F && this.currentBox != null;
    }

    public Box getCurrentBox() {
        return this.currentBox;
    }

    public Box getTargetBox() {
        return this.targetBox;
    }

    public BlockPos getCurrentPos() {
        return this.currentPos;
    }

    public VoxelShape getCurrentShape() {
        return this.currentShape;
    }

    public float getAlpha() {
        return this.alpha;
    }
}
