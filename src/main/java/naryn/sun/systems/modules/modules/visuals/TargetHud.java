package naryn.sun.systems.modules.modules.visuals;

import com.mojang.blaze3d.systems.RenderSystem;
import naryn.sun.Sun;
import naryn.sun.framework.base.CustomDrawContext;
import naryn.sun.framework.msdf.Font;
import naryn.sun.framework.msdf.Fonts;
import naryn.sun.framework.objects.BorderRadius;
import naryn.sun.systems.event.EventListener;
import naryn.sun.systems.event.impl.game.InternalAttackEvent;
import naryn.sun.systems.event.impl.render.HudRenderEvent;
import naryn.sun.systems.modules.api.ModuleCategory;
import naryn.sun.systems.modules.api.ModuleInfo;
import naryn.sun.systems.modules.impl.PositionableHudModule;
import naryn.sun.systems.setting.settings.BooleanSetting;
import naryn.sun.systems.setting.settings.ModeSetting;
import naryn.sun.systems.setting.settings.SliderSetting;
import naryn.sun.systems.target.TargetSettings;
import naryn.sun.systems.theme.Theme;
import naryn.sun.utility.animation.base.Animation;
import naryn.sun.utility.animation.base.Easing;
import naryn.sun.utility.colors.ColorRGBA;
import naryn.sun.utility.colors.Colors;
import naryn.sun.utility.game.EntityUtility;
import naryn.sun.utility.game.TextUtility;
import naryn.sun.utility.interfaces.IMinecraft;
import naryn.sun.utility.time.Timer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

@ModuleInfo(name = "Target Hud", category = ModuleCategory.VISUALS, desc = "Отображает здоровье и снаряжение текущей цели")
public class TargetHud extends PositionableHudModule implements IMinecraft {

    private static final float PAD = 6.0F;
    private static final float ICON = 16.0F;
    private static final float GAP = 4.0F;
    private static final float NAME_FADE_BUFFER = 8.0F;
    private static final float MAX_RANGE = 6.0F;

    private static final float HEART_SIZE = 8.0F;
    private static final float HEART_GAP = -1.0F;
    private static final float HEART_ROW_GAP = 1.0F;
    private static final int HEARTS_PER_ROW = 10;
    private static final float HEART_OUTLINE = 1.0F;

    private static final Identifier HEART_FULL = Identifier.of("minecraft", "textures/gui/sprites/hud/heart/full.png");
    private static final Identifier HEART_HALF = Identifier.of("minecraft", "textures/gui/sprites/hud/heart/half.png");
    private static final Identifier HEART_ABSORBING_FULL = Identifier.of("minecraft", "textures/gui/sprites/hud/heart/absorbing_full.png");
    private static final Identifier HEART_ABSORBING_HALF = Identifier.of("minecraft", "textures/gui/sprites/hud/heart/absorbing_half.png");

    private static final ColorRGBA EMPTY_HEART_TINT = new ColorRGBA(20.0F, 20.0F, 20.0F, 255.0F);
    private static final ColorRGBA HEART_OUTLINE_COLOR = new ColorRGBA(0.0F, 0.0F, 0.0F, 255.0F);

    private final ModeSetting visibility = new ModeSetting(this, "modules.settings.target_hud.visibility");
    private final ModeSetting.Value visibilityAlways = new ModeSetting.Value(this.visibility, "modules.settings.target_hud.visibility.always").select();
    private final ModeSetting.Value visibilityHover = new ModeSetting.Value(this.visibility, "modules.settings.target_hud.visibility.hover");

    private final BooleanSetting hideBackground = new BooleanSetting(this, "modules.settings.target_hud.hide_background");

    private final SliderSetting hideDelay = new SliderSetting(this, "modules.settings.target_hud.hidedelay", () -> !this.visibilityHover.isSelected())
        .min(1.0F).max(5.0F).step(1.0F).currentValue(3.0F);

    private final Animation health = new Animation(300L, 0.0F, Easing.BAKEK);
    private final Animation golden = new Animation(300L, 0.0F, Easing.BAKEK);
    private final Animation number = new Animation(300L, 0.0F, Easing.FIGMA_EASE_IN_OUT);
    private final Animation eatingPulse = new Animation(150L, 0.0F, Easing.BAKEK);
    private final Animation pulseIntensity = new Animation(50L, 0.0F, Easing.SINE_IN_OUT);

    private LivingEntity target;
    private final Timer lostTimer = new Timer();

    private final EventListener<HudRenderEvent> onHudRender = event -> this.draw(event.getContext());

    private final EventListener<InternalAttackEvent> onAttack = event -> {
        if (event.getEntity() instanceof LivingEntity living) {
            this.target = living;
            this.lostTimer.reset();
        }
    };

    @Override
    public void renderPreview(CustomDrawContext context) {
        this.draw(context);
    }

    private void draw(CustomDrawContext context) {
        boolean editing = PositionableHudModule.isEditingActive();

        LivingEntity live = this.getTarget();
        if (live != null) {
            this.target = live;
            this.lostTimer.reset();
        } else if (this.target != null) {
            long delayMs = (long) (this.hideDelay.getCurrentValue() * 1000L);
            if (!this.visibilityHover.isSelected() || this.lostTimer.finished(delayMs)) {
                this.target = null;
            }
        }

        LivingEntity shown = this.target != null ? this.target : (editing ? mc.player : null);
        if (shown == null) {
            return;
        }

        Font font7 = Fonts.REGULAR.getFont(7.0F);
        Font semibold6 = Fonts.SEMIBOLD.getFont(6.0F);
        Font semibold7 = Fonts.SEMIBOLD.getFont(7.0F);

        boolean isEating = shown.isUsingItem() && shown.getActiveItem().contains(DataComponentTypes.FOOD);
        this.eatingPulse.update(isEating);
        if (isEating) {
            float pulse = (float) Math.sin(System.currentTimeMillis() / 100.0) * 0.5F + 0.5F;
            this.pulseIntensity.setValue(pulse);
        }

        float maxHealth = shown.getMaxHealth();
        float healthNum = shown instanceof PlayerEntity playerx ? EntityUtility.getHealth(playerx) : shown.getHealth();

        this.health.update(healthNum / maxHealth);
        this.golden.update(shown.getAbsorptionAmount());
        this.number.update(healthNum);

        List<ItemStack> gear = new ArrayList<>();
        for (ItemStack stack : shown.getArmorItems()) {
            if (!stack.isEmpty()) gear.add(stack);
        }
        ItemStack mainHand = shown.getMainHandStack();
        ItemStack offHand = shown.getOffHandStack();
        if (!mainHand.isEmpty()) gear.add(mainHand);
        if (!offHand.isEmpty()) gear.add(offHand);

        String name = shown.getName().getString();
        String hpText = healthNum == 1000.0F ? "?" : TextUtility.formatNumber(this.number.getValue()).replace(",", ".");

        int heartsTotal = (int) Math.ceil(maxHealth / 2.0F);
        int filledHalfSteps = Math.clamp(
            (int) Math.ceil(Math.clamp(this.health.getValue(), 0.0F, 1.0F) * maxHealth),
            0, heartsTotal * 2
        );
        if (healthNum > 0.0F && filledHalfSteps == 0) {
            filledHalfSteps = 1;
        }

        int absorptionHalfSteps = Math.max(0, Math.round(this.golden.getValue()));
        int absorptionHearts = absorptionHalfSteps > 0 ? (int) Math.ceil(absorptionHalfSteps / 2.0F) : 0;

        int totalHearts = heartsTotal + absorptionHearts;
        int heartRows = Math.max(1, (int) Math.ceil(totalHearts / (float) HEARTS_PER_ROW));

        float nameWidth = font7.width(name);
        float headerWidth = PAD + ICON + 6.0F + nameWidth + 10.0F + semibold7.width(hpText) + PAD;
        float rowWidth = gear.isEmpty() ? 0.0F : PAD * 2 + gear.size() * (ICON + GAP) - GAP;
        float heartsRowWidth = PAD * 2 + HEARTS_PER_ROW * HEART_SIZE + (HEARTS_PER_ROW - 1) * HEART_GAP;
        float width = Math.max(headerWidth, Math.max(rowWidth, heartsRowWidth));

        float heartsAreaHeight = heartRows * HEART_SIZE + (heartRows - 1) * HEART_ROW_GAP;
        float height = PAD + ICON + 6.0F + heartsAreaHeight + PAD;
        if (!gear.isEmpty()) {
            height += ICON + font7.height() + 4.0F;
        }

        float sw = mc.getWindow().getScaledWidth();
        float x = this.resolveX(sw / 2.0F - width / 2.0F);
        float y = this.resolveY(40.0F);

        this.beginScaledRender(context, width, height);

        if (editing) {
            context.drawRoundedRect(x, y, width, height, BorderRadius.all(6.0F), Colors.getBackgroundColor().withAlpha(200.0F));
        } else if (!this.hideBackground.isEnabled()) {
            context.drawClientRect(x, y, width, height, 1.0F, 0.0F, 1.0F);
        }

        float rowY = y + PAD;

        if (shown instanceof AbstractClientPlayerEntity playerHead) {
            context.drawHead(playerHead, x + PAD, rowY, ICON, BorderRadius.all(3.0F), Colors.WHITE);
        } else {
            context.drawRoundedTexture(
                Sun.id(
                    Interface.glassSelected()
                        ? "icons/hud/whoglass.png"
                        : (Sun.getInstance().getThemeManager().getCurrentTheme() == Theme.DARK ? "icons/hud/whodark.png" : "icons/hud/who.png")
                ),
                x + PAD, rowY, ICON, ICON, BorderRadius.all(3.0F), Colors.WHITE
            );
        }

        context.drawFadeoutText(
            font7, name,
            x + PAD + ICON + 6.0F, rowY + (ICON - font7.height()) / 2.0F,
            Colors.getTextColor(), 0.7F, 1.0F,
            nameWidth + NAME_FADE_BUFFER
        );
        context.drawRightText(semibold7, hpText, x + width - PAD, rowY + (ICON - semibold7.height()) / 2.0F, Colors.ACCENT);

        float heartsY = rowY + ICON + 6.0F;

        for (int i = 0; i < heartsTotal; i++) {
            int steps = Math.clamp(filledHalfSteps - i * 2, 0, 2);
            this.drawHealthHeart(context, x, width, heartsY, i, totalHearts, steps);
        }

        for (int i = 0; i < absorptionHearts; i++) {
            int index = heartsTotal + i;
            int steps = Math.clamp(absorptionHalfSteps - i * 2, 0, 2);
            this.drawAbsorptionHeart(context, x, width, heartsY, index, totalHearts, steps);
        }

        if (!gear.isEmpty()) {
            float itemsY = heartsY + heartsAreaHeight + 6.0F;
            float itemX = x + PAD;
            for (ItemStack stack : gear) {
                boolean pulsing = isEating && stack == shown.getActiveItem();
                float alpha = pulsing ? 0.5F + 0.7F * this.pulseIntensity.getValue() : 1.0F;
                float prev = RenderSystem.getShaderColor()[3];
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, prev * alpha);
                context.drawItem(stack, itemX, itemsY, 1.0F);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, prev);
                if (stack.isDamageable()) {
                    int max = stack.getMaxDamage();
                    int remaining = max - stack.getDamage();
                    String text = String.valueOf(remaining);
                    ColorRGBA color = this.getDurabilityColor(remaining, max);
                    context.drawCenteredText(semibold6, text, itemX + ICON / 2.0F, itemsY + ICON + 2.0F, color);
                }
                itemX += ICON + GAP;
            }
        }

        this.endScaledRender(context);
    }

    private void drawHealthHeart(CustomDrawContext context, float boxX, float boxWidth, float startY, int index, int totalHearts, int steps) {
        float hx = this.heartX(boxX, boxWidth, index, totalHearts);
        float hy = this.heartY(startY, index);

        context.drawTexture(
            HEART_FULL,
            hx - HEART_OUTLINE, hy - HEART_OUTLINE,
            HEART_SIZE + HEART_OUTLINE * 2.0F, HEART_SIZE + HEART_OUTLINE * 2.0F,
            HEART_OUTLINE_COLOR
        );
        context.drawTexture(HEART_FULL, hx, hy, HEART_SIZE, HEART_SIZE, EMPTY_HEART_TINT);

        if (steps >= 2) {
            context.drawTexture(HEART_FULL, hx, hy, HEART_SIZE, HEART_SIZE, Colors.WHITE);
        } else if (steps == 1) {
            context.drawTexture(HEART_HALF, hx, hy, HEART_SIZE, HEART_SIZE, Colors.WHITE);
        }
    }

    private void drawAbsorptionHeart(CustomDrawContext context, float boxX, float boxWidth, float startY, int index, int totalHearts, int steps) {
        float hx = this.heartX(boxX, boxWidth, index, totalHearts);
        float hy = this.heartY(startY, index);

        context.drawTexture(
            HEART_FULL,
            hx - HEART_OUTLINE, hy - HEART_OUTLINE,
            HEART_SIZE + HEART_OUTLINE * 2.0F, HEART_SIZE + HEART_OUTLINE * 2.0F,
            HEART_OUTLINE_COLOR
        );

        Identifier texture = steps >= 2 ? HEART_ABSORBING_FULL : HEART_ABSORBING_HALF;
        context.drawTexture(texture, hx, hy, HEART_SIZE, HEART_SIZE, Colors.WHITE);
    }

    /**
     * X-координата сердца с индексом index: ряд, которому оно принадлежит, центрируется
     * внутри всей ширины бокса (boxWidth) по фактическому кол-ву сердец именно в этом ряду
     * (последний неполный ряд не растягивается на всю ширину, а тоже центрируется по своей длине).
     */
    private float heartX(float boxX, float boxWidth, int index, int totalHearts) {
        int row = index / HEARTS_PER_ROW;
        int col = index % HEARTS_PER_ROW;

        int rowStartIndex = row * HEARTS_PER_ROW;
        int heartsInRow = Math.min(HEARTS_PER_ROW, totalHearts - rowStartIndex);
        float rowWidth = heartsInRow * HEART_SIZE + Math.max(0, heartsInRow - 1) * HEART_GAP;
        float rowStartX = boxX + (boxWidth - rowWidth) / 2.0F;

        return rowStartX + col * (HEART_SIZE + HEART_GAP);
    }

    private float heartY(float startY, int index) {
        int row = index / HEARTS_PER_ROW;
        return startY + row * (HEART_SIZE + HEART_ROW_GAP);
    }

    private ColorRGBA getDurabilityColor(int remaining, int max) {
        float ratio = (float) remaining / max;
        if (ratio > 0.5F) return new ColorRGBA(80, 220, 120, 255);
        if (ratio > 0.2F) return new ColorRGBA(230, 200, 70, 255);
        return new ColorRGBA(220, 70, 70, 255);
    }

    private LivingEntity getTarget() {
        if (mc.player == null) {
            return null;
        }
        Entity current = Sun.getInstance().getTargetManager().getCurrentTarget();
        if (current == null || !(current instanceof LivingEntity) || mc.player.distanceTo(current) > MAX_RANGE) {
            TargetSettings settings = new TargetSettings.Builder()
                    .targetPlayers(true)
                    .targetMobs(true)
                    .targetAnimals(true)
                    .requiredRange(MAX_RANGE)
                    .build();
            Entity crosshair = Sun.getInstance().getTargetManager().getCrosshairTarget(settings, MAX_RANGE);
            if (crosshair != null) {
                current = crosshair;
            } else if (mc.targetedEntity instanceof LivingEntity living && mc.player.distanceTo(living) <= MAX_RANGE) {
                current = mc.targetedEntity;
            }
        }
        if (!(current instanceof LivingEntity living)) {
            return null;
        }
        return mc.player.distanceTo(living) <= MAX_RANGE ? living : null;
    }
}