package naryn.sun.ui.menu.dropdown.components.settings.impl;

import com.mojang.blaze3d.systems.RenderSystem;
import naryn.sun.framework.base.CustomComponent;
import naryn.sun.framework.base.UIContext;
import naryn.sun.framework.msdf.Font;
import naryn.sun.framework.msdf.Fonts;
import naryn.sun.framework.objects.BorderRadius;
import naryn.sun.framework.objects.MouseButton;
import naryn.sun.systems.localization.Localizator;
import naryn.sun.systems.setting.settings.BlockSlotSetting;
import naryn.sun.ui.menu.dropdown.components.settings.MenuSettingComponent;
import naryn.sun.ui.menu.skin.MenuSkin;
import naryn.sun.utility.animation.base.Animation;
import naryn.sun.utility.animation.base.Easing;
import naryn.sun.utility.colors.ColorRGBA;
import naryn.sun.utility.colors.Colors;
import naryn.sun.utility.game.cursor.CursorType;
import naryn.sun.utility.game.cursor.CursorUtility;
import naryn.sun.utility.gui.GuiUtility;
import naryn.sun.utility.interfaces.IMinecraft;
import naryn.sun.utility.render.DrawUtility;
import naryn.sun.utility.sounds.ClientSoundManager;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec2f;

public class BlockSlotComponent extends MenuSettingComponent<BlockSlotSetting> implements IMinecraft {

    private static final float SLOT_SIZE = 18.0F;
    private static final int COLS = 9;
    private static final int MAIN_ROWS = 3;
    private static final float ROW_GAP = 4.0F;
    private static final float MARGIN_X = 7.0F;
    private static final float MARGIN_Y = 3.0F;
    private static final float INNER_PAD_X = 7.0F;
    private static final float BOX_HEIGHT = 140.0F;

    private static final ColorRGBA GRID_LINE_COLOR = ColorRGBA.BLACK.withAlpha(70.0F);

    private final Animation[] slotAnimations = new Animation[41];

    public BlockSlotComponent(BlockSlotSetting setting, CustomComponent parent) {
        super(setting, parent);
    }

    @Override
    public void onInit() {
        this.width = 13.0F;
        this.height = BOX_HEIGHT + MARGIN_Y * 2.0F;
        for (int i = 0; i < slotAnimations.length; i++) {
            float initial = setting.isLocked(i) ? 1.0F : 0.0F;
            slotAnimations[i] = new Animation(240L, initial, Easing.BACK_OUT);
        }
        super.onInit();
    }

    @Override
    public float getHeight() {
        return this.height = BOX_HEIGHT + MARGIN_Y * 2.0F;
    }

    @Override
    protected void renderComponent(UIContext context) {
        float boxX = this.x + MARGIN_X;
        float boxY = this.y + MARGIN_Y;
        float boxW = this.width - MARGIN_X * 2.0F;
        float boxH = BOX_HEIGHT;

        // 1. Фон и обводка бокса
        ColorRGBA boxBg = new ColorRGBA(255, 255, 255, 8);
        ColorRGBA boxBorder = new ColorRGBA(255, 255, 255, 16);
        context.drawRoundedRect(boxX, boxY, boxW, boxH, BorderRadius.all(6.0F), boxBg);
        context.drawRoundedBorder(boxX, boxY, boxW, boxH, 0.5F, BorderRadius.all(6.0F), boxBorder);

        // 2. Заголовок и бейдж счетчика
        Font titleFont = Fonts.REGULAR.getFont(7.0F);
        String title = Localizator.translate("modules.settings.blockslot.slots");
        context.drawText(titleFont, title, boxX + INNER_PAD_X, boxY + 6.0F, Colors.getTextColor().withAlpha(200));

        Font countFont = Fonts.REGULAR.getFont(6.0F);
        String countText = String.valueOf(setting.getLockedCount());
        float countW = countFont.width(countText) + 8.0F;
        float countH = 11.0F;
        float countX = boxX + boxW - INNER_PAD_X - countW;
        float countY = boxY + 5.0F;
        MenuSkin.current().renderBindPill(context, countX, countY, countW, countH, BorderRadius.all(3.0F), false, 1.0F);
        context.drawCenteredText(countFont, countText, countX + countW / 2.0F, countY + 3.0F, ColorRGBA.WHITE.withAlpha(220));

        // 3. Разделитель
        context.drawRect(boxX + INNER_PAD_X, boxY + 19.0F, boxW - INNER_PAD_X * 2.0F, 0.5F, new ColorRGBA(255, 255, 255, 12));

        // 4. Координаты сетки
        float gridW = COLS * SLOT_SIZE;
        float gridStartX = boxX + (boxW - gridW) / 2.0F;
        double mouseX = context.getMouseX();
        double mouseY = context.getMouseY();

        // 5. Подписи секций брони и левой руки
        Font secFont = Fonts.REGULAR.getFont(5.5F);
        String armorLabel = Localizator.translate("modules.settings.blockslot.armor");
        String offhandLabel = Localizator.translate("modules.settings.blockslot.offhand");
        float labelY = boxY + 23.0F;
        context.drawText(secFont, armorLabel, gridStartX, labelY, Colors.getTextColor().withAlpha(150));
        float offhandW = secFont.width(offhandLabel);
        context.drawText(secFont, offhandLabel, gridStartX + gridW - offhandW, labelY, Colors.getTextColor().withAlpha(150));

        // 6. Отрисовка рамок и ячеек
        float armorY = boxY + 31.0F;
        drawGridFrame(context, gridStartX, armorY, 1, 4);

        float offhandX = gridStartX + gridW - SLOT_SIZE;
        drawGridFrame(context, offhandX, armorY, 1, 1);

        float mainY = armorY + SLOT_SIZE + 5.0F;
        drawGridFrame(context, gridStartX, mainY, MAIN_ROWS, COLS);

        float hotbarY = mainY + MAIN_ROWS * SLOT_SIZE + ROW_GAP;
        drawGridFrame(context, gridStartX, hotbarY, 1, COLS);

        // 7. Отрисовка ховера и батч-предметов
        int[] armorSlots = {39, 38, 37, 36};
        for (int i = 0; i < 4; i++) {
            renderSlotItem(context, gridStartX + i * SLOT_SIZE, armorY, armorSlots[i], mouseX, mouseY);
        }
        renderSlotItem(context, offhandX, armorY, 40, mouseX, mouseY);

        for (int row = 0; row < MAIN_ROWS; row++) {
            float rowY = mainY + row * SLOT_SIZE;
            for (int col = 0; col < COLS; col++) {
                renderSlotItem(context, gridStartX + col * SLOT_SIZE, rowY, 9 + row * COLS + col, mouseX, mouseY);
            }
        }

        for (int col = 0; col < COLS; col++) {
            renderSlotItem(context, gridStartX + col * SLOT_SIZE, hotbarY, col, mouseX, mouseY);
        }

        // Восстанавливаем GL-состояние после рендера предметов
        context.draw();
        DiffuseLighting.disableGuiDepthLighting();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        // 8. Отрисовка текста счетчиков, плейсхолдеров и анимированных замочков поверх предметов
        String[] armorHolders = {
            Localizator.translateOrDefault("modules.settings.blockslot.slot_helmet", "H"),
            Localizator.translateOrDefault("modules.settings.blockslot.slot_chestplate", "C"),
            Localizator.translateOrDefault("modules.settings.blockslot.slot_leggings", "L"),
            Localizator.translateOrDefault("modules.settings.blockslot.slot_boots", "B")
        };
        for (int i = 0; i < 4; i++) {
            renderSlotOverlay(context, gridStartX + i * SLOT_SIZE, armorY, armorSlots[i], armorHolders[i]);
        }

        String offhandHolder = Localizator.translateOrDefault("modules.settings.blockslot.slot_offhand", "O");
        renderSlotOverlay(context, offhandX, armorY, 40, offhandHolder);

        for (int row = 0; row < MAIN_ROWS; row++) {
            float rowY = mainY + row * SLOT_SIZE;
            for (int col = 0; col < COLS; col++) {
                renderSlotOverlay(context, gridStartX + col * SLOT_SIZE, rowY, 9 + row * COLS + col, null);
            }
        }

        for (int col = 0; col < COLS; col++) {
            renderSlotOverlay(context, gridStartX + col * SLOT_SIZE, hotbarY, col, null);
        }

        // Финальная очистка состояний рендера
        context.draw();
        DiffuseLighting.disableGuiDepthLighting();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void renderSlotItem(UIContext context, float x, float y, int slotIndex, double mouseX, double mouseY) {
        boolean hovered = GuiUtility.isHovered(x, y, SLOT_SIZE, SLOT_SIZE, mouseX, mouseY);
        if (hovered) {
            context.drawRoundedRect(x + 1.0F, y + 1.0F, 16.0F, 16.0F, BorderRadius.all(2.0F), new ColorRGBA(255, 255, 255, 22));
            CursorUtility.set(CursorType.HAND);
        }

        ItemStack stack = getSlotStack(slotIndex);
        if (!stack.isEmpty()) {
            context.drawItem(stack, (int) (x + 1.0F), (int) (y + 1.0F));
        }
    }

    private void renderSlotOverlay(UIContext context, float x, float y, int slotIndex, String placeholder) {
        ItemStack stack = getSlotStack(slotIndex);
        if (!stack.isEmpty()) {
            if (stack.getCount() > 1) {
                Font countFont = Fonts.REGULAR.getFont(7.0F);
                context.drawRightText(countFont, String.valueOf(stack.getCount()), x + 16.0F, y + 16.0F - countFont.height(), ColorRGBA.WHITE);
            }
        } else if (placeholder != null) {
            Font phFont = Fonts.REGULAR.getFont(6.0F);
            context.drawCenteredText(phFont, placeholder, x + 9.0F, y + 6.0F, new ColorRGBA(255, 255, 255, 50));
        }

        // Анимация блокировки
        boolean locked = setting.isLocked(slotIndex);
        Animation anim = slotAnimations[slotIndex];
        anim.setEasing(locked ? Easing.BACK_OUT : Easing.QUARTIC_OUT);
        anim.update(locked ? 1.0F : 0.0F);
        float progress = anim.getValue();

        if (progress > 0.001F) {
            context.drawRoundedRect(x + 1.0F, y + 1.0F, 16.0F, 16.0F, BorderRadius.all(2.0F), new ColorRGBA(245, 158, 11, (int) (50 * progress)));
            context.drawRoundedBorder(x + 1.0F, y + 1.0F, 16.0F, 16.0F, 0.8F, BorderRadius.all(2.0F), new ColorRGBA(245, 158, 11, (int) (140 * progress)));

            context.getMatrices().push();
            context.getMatrices().translate(x + 9.0F, y + 9.0F, 0.0F);
            context.getMatrices().scale(progress, progress, 1.0F);
            drawPadlock(context, progress);
            context.getMatrices().pop();
        }
    }

    private void drawPadlock(UIContext context, float alpha) {
        float shackleW = 5.2F;
        float shackleH = 4.8F;
        float shackleX = -shackleW / 2.0F;
        float shackleY = -4.5F;
        ColorRGBA shackleColor = new ColorRGBA(254, 240, 138, (int) (255 * alpha));
        context.drawRoundedBorder(shackleX, shackleY, shackleW, shackleH, 1.1F, BorderRadius.top(2.6F, 2.6F), shackleColor);

        float bodyW = 7.6F;
        float bodyH = 5.6F;
        float bodyX = -bodyW / 2.0F;
        float bodyY = -0.5F;
        ColorRGBA bodyColor = new ColorRGBA(245, 158, 11, (int) (255 * alpha));
        context.drawRoundedRect(bodyX, bodyY, bodyW, bodyH, BorderRadius.all(1.5F), bodyColor);

        ColorRGBA keyholeColor = new ColorRGBA(30, 20, 10, (int) (220 * alpha));
        context.drawRoundedRect(-0.7F, bodyY + 1.5F, 1.4F, 1.4F, BorderRadius.all(0.7F), keyholeColor);
        context.drawRect(-0.35F, bodyY + 2.2F, 0.7F, 1.6F, keyholeColor);
    }

    private void drawGridFrame(UIContext context, float blockX, float blockY, int rows, int cols) {
        float width = cols * SLOT_SIZE;
        float height = rows * SLOT_SIZE;

        context.drawRoundedRect(blockX, blockY, width, height, BorderRadius.all(2.0F), new ColorRGBA(0, 0, 0, 35));

        for (int col = 0; col <= cols; col++) {
            float lx = blockX + col * SLOT_SIZE;
            DrawUtility.drawLine(context.getMatrices(), new Vec2f(lx, blockY), new Vec2f(lx, blockY + height), GRID_LINE_COLOR);
        }

        for (int row = 0; row <= rows; row++) {
            float ly = blockY + row * SLOT_SIZE;
            DrawUtility.drawLine(context.getMatrices(), new Vec2f(blockX, ly), new Vec2f(blockX + width, ly), GRID_LINE_COLOR);
        }
    }

    private ItemStack getSlotStack(int slotIndex) {
        if (mc.player == null || mc.player.getInventory() == null) return ItemStack.EMPTY;
        PlayerInventory inv = mc.player.getInventory();
        if (slotIndex >= 0 && slotIndex <= 35) {
            return inv.getStack(slotIndex);
        } else if (slotIndex >= 36 && slotIndex <= 39) {
            return inv.getArmorStack(slotIndex - 36);
        } else if (slotIndex == 40) {
            return inv.offHand.getFirst();
        }
        return ItemStack.EMPTY;
    }

    private int hitTestSlot(double mouseX, double mouseY) {
        float boxX = this.x + MARGIN_X;
        float boxY = this.y + MARGIN_Y;
        float boxW = this.width - MARGIN_X * 2.0F;
        float gridW = COLS * SLOT_SIZE;
        float gridStartX = boxX + (boxW - gridW) / 2.0F;

        // Броня
        float armorY = boxY + 31.0F;
        if (mouseX >= gridStartX && mouseX < gridStartX + 4 * SLOT_SIZE && mouseY >= armorY && mouseY < armorY + SLOT_SIZE) {
            int col = (int) ((mouseX - gridStartX) / SLOT_SIZE);
            int[] armorSlots = {39, 38, 37, 36};
            if (col >= 0 && col < 4) return armorSlots[col];
        }

        // Левая рука
        float offhandX = gridStartX + gridW - SLOT_SIZE;
        if (mouseX >= offhandX && mouseX < offhandX + SLOT_SIZE && mouseY >= armorY && mouseY < armorY + SLOT_SIZE) {
            return 40;
        }

        // Основной инвентарь
        float mainY = armorY + SLOT_SIZE + 5.0F;
        if (mouseX >= gridStartX && mouseX < gridStartX + gridW && mouseY >= mainY && mouseY < mainY + MAIN_ROWS * SLOT_SIZE) {
            int col = (int) ((mouseX - gridStartX) / SLOT_SIZE);
            int row = (int) ((mouseY - mainY) / SLOT_SIZE);
            if (col >= 0 && col < COLS && row >= 0 && row < MAIN_ROWS) {
                return 9 + row * COLS + col;
            }
        }

        // Хотбар
        float hotbarY = mainY + MAIN_ROWS * SLOT_SIZE + ROW_GAP;
        if (mouseX >= gridStartX && mouseX < gridStartX + gridW && mouseY >= hotbarY && mouseY < hotbarY + SLOT_SIZE) {
            int col = (int) ((mouseX - gridStartX) / SLOT_SIZE);
            if (col >= 0 && col < COLS) {
                return col;
            }
        }

        return -1;
    }

    @Override
    public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
        if (button == MouseButton.LEFT) {
            int slot = hitTestSlot(mouseX, mouseY);
            if (slot != -1) {
                setting.toggleSlot(slot);
                ClientSoundManager.getInstance().playButtonClick();
                return;
            }
        }
        super.onMouseClicked(mouseX, mouseY, button);
    }
}
