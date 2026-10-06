package naryn.sun.systems.modules.modules.visuals;

import naryn.sun.Sun;
import naryn.sun.framework.base.CustomDrawContext;
import naryn.sun.framework.msdf.Font;
import naryn.sun.framework.msdf.Fonts;
import naryn.sun.framework.objects.BorderRadius;
import naryn.sun.mixin.accessors.HandledScreenAccessor;
import naryn.sun.systems.event.EventListener;
import naryn.sun.systems.event.impl.player.ClientPlayerTickEvent;
import naryn.sun.systems.event.impl.render.ScreenRenderEvent;
import naryn.sun.systems.modules.api.ModuleCategory;
import naryn.sun.systems.modules.api.ModuleInfo;
import naryn.sun.systems.modules.impl.BaseModule;
import naryn.sun.utility.colors.ColorRGBA;
import naryn.sun.utility.render.DrawUtility;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.Vec2f;
import org.lwjgl.glfw.GLFW;

import naryn.sun.systems.setting.settings.BooleanSetting;
import net.minecraft.util.collection.DefaultedList;
import java.util.ArrayList;
import java.util.List;

@ModuleInfo(
    name = "Shulker Peek",
    category = ModuleCategory.VISUALS,
    key = GLFW.GLFW_KEY_UNKNOWN,
    enabledByDefault = false,
    holdToActivate = true,
    desc = "Превью содержимого шалкера при наведении"
)
public class ShulkerPeek extends BaseModule {

    private final BooleanSetting holdOnly = new BooleanSetting(this, "modules.settings.shulker_peek.hold_only").enabled(true);

    private static final int COLS = 9;
    private static final int ROWS = 3;
    private static final float SLOT_SIZE = 18f;
    private static final float INNER_PAD = 7f;

    private static final ColorRGBA GRID_LINE_COLOR = ColorRGBA.BLACK.withAlpha(70f);

    @Override
    public boolean isHoldToActivate() {
        return this.holdOnly.isEnabled();
    }

    private final EventListener<ScreenRenderEvent> onScreenRender = event -> {
        if (!isEnabled()) return;
        List<ItemStack> items = getPreviewItems();
        if (items == null) return;

        renderPreview(event.getContext(), items);
    };

    private final EventListener<ClientPlayerTickEvent> onTick = event -> {
        if (isHoldToActivate() && isEnabled() && !(mc.currentScreen instanceof HandledScreen<?>)) {
            setEnabled(false, true);
        }
    };

    // Используется отдельным Mixin-ом, чтобы отключать ванильный тултип предмета,
    // когда наш превью и так показывает содержимое. Считает состояние заново,
    // без привязки к уже отрисованному кадру (иначе будет 1 кадр рассинхрона).
    public boolean isHoveringContainer() {
        return isEnabled() && getPreviewItems() != null;
    }

    private List<ItemStack> getPreviewItems() {
        if (!(mc.currentScreen instanceof HandledScreen<?>)) return null;
        if (mc.player == null || mc.player.currentScreenHandler == null) return null;

        Slot hovered = findHoveredSlot();
        if (hovered == null || !hovered.hasStack()) return null;

        ItemStack stack = hovered.getStack();
        if (!stack.contains(DataComponentTypes.CONTAINER)) return null;

        ContainerComponent container = stack.get(DataComponentTypes.CONTAINER);
        if (container == null) return null;

        DefaultedList<ItemStack> items = DefaultedList.ofSize(ROWS * COLS, ItemStack.EMPTY);
        container.copyTo(items);
        return items;
    }

    private Slot findHoveredSlot() {
        // Координаты курсора в GUI-scaled пространстве (стандартная ваниль-формула)
        double scaledMouseX = mc.mouse.getX() * (double) mc.getWindow().getScaledWidth() / mc.getWindow().getWidth();
        double scaledMouseY = mc.mouse.getY() * (double) mc.getWindow().getScaledHeight() / mc.getWindow().getHeight();

        HandledScreenAccessor accessor = (HandledScreenAccessor) mc.currentScreen;
        int screenX = accessor.getX();
        int screenY = accessor.getY();

        for (Slot slot : mc.player.currentScreenHandler.slots) {
            float sx = screenX + slot.x;
            float sy = screenY + slot.y;
            if (scaledMouseX >= sx && scaledMouseX < sx + 16 && scaledMouseY >= sy && scaledMouseY < sy + 16) {
                return slot;
            }
        }
        return null;
    }

    private void renderPreview(CustomDrawContext context, List<ItemStack> items) {
        // ФИКС: на момент TAIL-инжекта в HandledScreen.render() ванильные элементы
        // (заголовок экрана, слоты и т.д.) могут быть ещё НЕ скомпонованы на экране —
        // они лежат в буфере Immediate vertexConsumers и флашатся позже, во внешнем коде
        // (Screen/GameRenderer). Поэтому просто рисовать "в конце" недостаточно — надо
        // сначала принудительно вытолкнуть всё, что уже поставлено в очередь ДО нас,
        // и только потом рисовать себя. Тогда наш превью гарантированно окажется сверху
        // абсолютно всего, что есть на экране на этот момент.
        context.flushItems();

        double mouseX = mc.mouse.getX() * (double) mc.getWindow().getScaledWidth() / mc.getWindow().getWidth();
        double mouseY = mc.mouse.getY() * (double) mc.getWindow().getScaledHeight() / mc.getWindow().getHeight();

        float bgWidth = COLS * SLOT_SIZE + INNER_PAD * 2;
        float bgHeight = ROWS * SLOT_SIZE + INNER_PAD * 2;

        float startX = (float) mouseX + 14f;
        float startY = (float) mouseY - bgHeight / 2f;

        if (startX + bgWidth > mc.getWindow().getScaledWidth()) startX = (float) mouseX - bgWidth - 14f;
        if (startY < 0) startY = 0f;
        if (startY + bgHeight > mc.getWindow().getScaledHeight()) startY = mc.getWindow().getScaledHeight() - bgHeight;

        context.pushMatrix();
        // Поднимаем себя выше всего остального (инвентарь/тултип рисуются на меньшем Z) —
        // без этого настоящие иконки инвентаря могут оказаться "поверх" наших предметов
        context.getMatrices().translate(0f, 0f, 600f);

        // Обновляем снимок фона для блюра ПРЯМО СЕЙЧАС — иначе блюр возьмёт старый кадр
        // (например, снятый ещё до отрисовки самого инвентаря) и будет "игнорировать" GUI.
        DrawUtility.updateBuffer();

        // Фон в стиле клиента — блюр/стекло, как у Armor/FPS/Ping HUD
        context.drawClientRect(startX, startY, bgWidth, bgHeight, 1.0f, 0.0f, 1.0f);

        float gridX = startX + INNER_PAD;
        float gridY = startY + INNER_PAD;

        Font font = Fonts.REGULAR.getFont(7.0f);

        // минималистичная сетка — просто тонкие линии, без заливки/рамки ячеек
        this.drawGrid(context, gridX, gridY);

        // ФИКС порядка отрисовки: сначала кладём ВСЕ иконки предметов и сразу
        // принудительно флашим их буфер (flushItems). Только после этого рисуем
        // счётчики поверх — иначе буфер предметов (он флашится Minecraft-ом позже)
        // перекрывает уже нарисованный текст, и цифра визуально уходит "под" иконку.
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                float sx = gridX + col * SLOT_SIZE;
                float sy = gridY + row * SLOT_SIZE;

                ItemStack item = items.get(row * COLS + col);
                if (!item.isEmpty()) {
                    context.drawBatchItem(item, (int) sx, (int) sy);
                }
            }
        }

        context.flushItems();

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                float sx = gridX + col * SLOT_SIZE;
                float sy = gridY + row * SLOT_SIZE;

                ItemStack item = items.get(row * COLS + col);
                if (!item.isEmpty() && item.getCount() > 1) {
                    String count = String.valueOf(item.getCount());
                    RenderSystem.disableDepthTest();
                    context.drawRightText(font, count, sx + 16f, sy + 16f - font.height(), ColorRGBA.WHITE);
                    RenderSystem.enableDepthTest();
                }
            }
        }

        context.popMatrix();
    }

    /**
     * Рисует тонкую сетку из линий (без заливки) для блока COLS x ROWS,
     * начиная с левого верхнего угла (blockX, blockY).
     */
    private void drawGrid(CustomDrawContext context, float blockX, float blockY) {
        float width = COLS * SLOT_SIZE;
        float height = ROWS * SLOT_SIZE;

        for (int col = 0; col <= COLS; col++) {
            float lx = blockX + col * SLOT_SIZE;
            context.drawLine(new Vec2f(lx, blockY), new Vec2f(lx, blockY + height), GRID_LINE_COLOR);
        }

        for (int row = 0; row <= ROWS; row++) {
            float ly = blockY + row * SLOT_SIZE;
            context.drawLine(new Vec2f(blockX, ly), new Vec2f(blockX + width, ly), GRID_LINE_COLOR);
        }
    }
}