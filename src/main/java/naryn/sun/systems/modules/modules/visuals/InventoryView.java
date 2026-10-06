package naryn.sun.systems.modules.modules.visuals;

import naryn.sun.framework.base.CustomDrawContext;
import naryn.sun.framework.msdf.Font;
import naryn.sun.framework.msdf.Fonts;
import naryn.sun.systems.event.EventListener;
import naryn.sun.systems.event.impl.render.HudRenderEvent;
import naryn.sun.systems.modules.api.ModuleCategory;
import naryn.sun.systems.modules.api.ModuleInfo;
import naryn.sun.systems.modules.impl.PositionableHudModule;
import naryn.sun.systems.setting.settings.BooleanSetting;
import naryn.sun.utility.colors.ColorRGBA;
import naryn.sun.utility.interfaces.IMinecraft;
import naryn.sun.utility.render.DrawUtility;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec2f;

@ModuleInfo(name = "Inventory View", category = ModuleCategory.VISUALS, desc = "Отображает содержимое инвентаря на экране")
public class InventoryView extends PositionableHudModule implements IMinecraft {

    private final BooleanSetting background = new BooleanSetting(this, "hud.background").enable();

    private static final int COLS = 9;
    private static final int MAIN_ROWS = 3;

    private static final float SLOT_SIZE = 18.0F;
    private static final float INNER_PAD = 7.0F;
    private static final float ROW_GAP = 4.0F; // зазор между основным инвентарём и хотбаром, как в ваниле

    private static final float GRID_WIDTH = COLS * SLOT_SIZE;
    private static final float GRID_HEIGHT = MAIN_ROWS * SLOT_SIZE + ROW_GAP + SLOT_SIZE;

    private static final float PANEL_W = GRID_WIDTH + INNER_PAD * 2;
    private static final float PANEL_H = GRID_HEIGHT + INNER_PAD * 2;

    private static final ColorRGBA GRID_LINE_COLOR = ColorRGBA.BLACK.withAlpha(70.0F);

    private final EventListener<HudRenderEvent> onHudRender = event -> this.draw(event.getContext());

    @Override
    public void renderPreview(CustomDrawContext context) {
        this.draw(context);
    }

    private void draw(CustomDrawContext context) {
        if (mc.player == null) return;

        float sh = mc.getWindow().getScaledHeight();

        float x = this.resolveX(20.0F);
        float y = this.resolveY(sh / 2.0F - PANEL_H / 2.0F);

        this.beginScaledRender(context, PANEL_W, PANEL_H);

        this.drawBackground(context, this.background, x, y, PANEL_W, PANEL_H);

        float gridX = x + INNER_PAD;
        float gridY = y + INNER_PAD;
        float hotbarY = gridY + MAIN_ROWS * SLOT_SIZE + ROW_GAP;

        // минималистичная сетка — просто тонкие линии, без заливки ячеек
        this.drawGrid(context, gridX, gridY, MAIN_ROWS);
        this.drawGrid(context, gridX, hotbarY, 1);

        // сначала все иконки предметов пачкой, потом принудительный flush —
        // иначе счётчики (текст) окажутся под буфером предметов
        for (int row = 0; row < MAIN_ROWS + 1; row++) {
            float rowY = row < MAIN_ROWS ? gridY + row * SLOT_SIZE : hotbarY;
            for (int col = 0; col < COLS; col++) {
                ItemStack stack = this.getSlotStack(row, col);
                if (!stack.isEmpty()) {
                    float sx = gridX + col * SLOT_SIZE + 1.0F;
                    context.drawItem(stack, (int) sx, (int) (rowY + 1.0F));
                }
            }
        }

        Font font = Fonts.REGULAR.getFont(7.0F);
        for (int row = 0; row < MAIN_ROWS + 1; row++) {
            float rowY = row < MAIN_ROWS ? gridY + row * SLOT_SIZE : hotbarY;
            for (int col = 0; col < COLS; col++) {
                ItemStack stack = this.getSlotStack(row, col);
                if (!stack.isEmpty() && stack.getCount() > 1) {
                    float sx = gridX + col * SLOT_SIZE + 1.0F;
                    String count = String.valueOf(stack.getCount());
                    context.drawRightText(font, count, sx + 16.0F, rowY + 16.0F - font.height(), ColorRGBA.WHITE);
                }
            }
        }

        this.endScaledRender(context);
    }

    /**
     * Рисует тонкую сетку из линий (без заливки) для блока размером COLS x rows,
     * начиная с левого верхнего угла (blockX, blockY).
     */
    private void drawGrid(CustomDrawContext context, float blockX, float blockY, int rows) {
        float width = COLS * SLOT_SIZE;
        float height = rows * SLOT_SIZE;

        for (int col = 0; col <= COLS; col++) {
            float lx = blockX + col * SLOT_SIZE;
            DrawUtility.drawLine(context.getMatrices(), new Vec2f(lx, blockY), new Vec2f(lx, blockY + height), GRID_LINE_COLOR);
        }

        for (int row = 0; row <= rows; row++) {
            float ly = blockY + row * SLOT_SIZE;
            DrawUtility.drawLine(context.getMatrices(), new Vec2f(blockX, ly), new Vec2f(blockX + width, ly), GRID_LINE_COLOR);
        }
    }

    /**
     * row 0..2 — основной инвентарь (слоты 9..35), row 3 — хотбар (слоты 0..8).
     */
    private ItemStack getSlotStack(int row, int col) {
        int slotIndex = row < MAIN_ROWS ? 9 + row * COLS + col : col;
        return mc.player.getInventory().getStack(slotIndex);
    }
}