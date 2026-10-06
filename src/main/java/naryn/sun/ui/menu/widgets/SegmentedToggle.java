package naryn.sun.ui.menu.widgets;

import naryn.sun.framework.base.UIContext;
import naryn.sun.framework.msdf.Font;
import naryn.sun.framework.objects.BorderRadius;
import naryn.sun.utility.colors.ColorRGBA;
import naryn.sun.utility.game.cursor.CursorType;
import naryn.sun.utility.game.cursor.CursorUtility;
import naryn.sun.utility.gui.GuiUtility;

/**
 * Кнопка-переключатель в стиле сегмент-контрола (выбран/не выбран), с
 * акцентной подсветкой активного варианта. Используется для переключателей
 * "Материал интерфейса" (Тёмный/Стекло) и "Язык" в GuiSettingsPanel — раньше
 * это были два почти идентичных copy-paste метода (drawAppearanceMode /
 * drawLanguageButton) прямо в NewScreen.
 */
public final class SegmentedToggle {

    private SegmentedToggle() {
    }

    /**
     * @param drawBorder      исходный drawAppearanceMode рисовал рамку, drawLanguageButton — нет.
     * @param textY           явный Y текста (в оригинале это были разные магические числа:
     *                        y+4.0F для режимов интерфейса, y+5.0F для языков) — передаём как есть,
     *                        чтобы не сдвинуть пиксель при переносе.
     */
    public static void render(UIContext context, Font font, String label,
                               float x, float y, float width, float height,
                               boolean selected, boolean hovered, boolean drawBorder, float textY,
                               ColorRGBA accent, ColorRGBA idleTextColor, float alpha) {
        if (naryn.sun.systems.theme.ClientAppearance.isFrost()) {
            if (selected) {
                context.drawShadow(x, y + 1.0F, width, height, 5.0F, BorderRadius.all(5.0F),
                        accent.withAlpha((int) (70 * alpha)));
            } else if (hovered) {
                context.drawShadow(x, y + 1.0F, width, height, 4.0F, BorderRadius.all(5.0F),
                        ColorRGBA.BLACK.withAlpha((int) (40 * alpha)));
            }
            context.drawRoundedRect(x, y, width, height, BorderRadius.all(5.0F),
                    selected
                            ? accent.withAlpha((int) (130 * alpha))
                            : new ColorRGBA(255, 255, 255, (int) ((hovered ? 16 : 8) * alpha)));
        } else {
            context.drawRoundedRect(x, y, width, height, BorderRadius.all(5.0F),
                    selected
                            ? accent.withAlpha((int) (90 * alpha))
                            : new ColorRGBA(255, 255, 255, (int) ((hovered ? 12 : 6) * alpha)));
            if (drawBorder) {
                context.drawRoundedBorder(x, y, width, height, 0.5F, BorderRadius.all(5.0F),
                        selected
                                ? accent.withAlpha((int) (150 * alpha))
                                : ColorRGBA.WHITE.withAlpha((int) (12 * alpha)));
            }
        }
        context.drawCenteredText(font, label, x + width / 2.0F, textY,
                idleTextColor.withAlpha((int) ((selected ? 255 : 180) * alpha)));
        if (hovered) {
            CursorUtility.set(CursorType.HAND);
        }
    }

    public static boolean isHovered(float x, float y, float width, float height, double mouseX, double mouseY) {
        return GuiUtility.isHovered(x, y, width, height, mouseX, mouseY);
    }
}
