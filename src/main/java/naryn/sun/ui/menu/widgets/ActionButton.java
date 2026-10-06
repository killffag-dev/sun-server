package naryn.sun.ui.menu.widgets;

import naryn.sun.framework.base.UIContext;
import naryn.sun.framework.msdf.Font;
import naryn.sun.framework.objects.BorderRadius;
import naryn.sun.utility.colors.ColorRGBA;
import naryn.sun.utility.game.cursor.CursorType;
import naryn.sun.utility.game.cursor.CursorUtility;

/**
 * Обычная кнопка с текстом по центру и подсветкой при наведении.
 * Используется в нижнем островке (HUD Editor/Configs/GUI Settings,
 * Modules/Interface) и в тулбаре редактора (Reset/Done).
 *
 * Только рендер — клик обрабатывается там же, где и раньше (в NewScreen),
 * этот класс ничего не мутирует.
 */
public final class ActionButton {

    private ActionButton() {
    }

    /** Кнопка с подсветкой-заливкой при наведении (как HUD Editor/Configs/GUI Settings, Reset/Done). */
    public static void renderFilled(UIContext context, Font font, String label,
                                     float x, float y, float w, float h,
                                     boolean hovered, ColorRGBA hoverFill,
                                     ColorRGBA textColor, float alpha) {
        if (hovered) {
            context.drawRoundedRect(x, y, w, h, BorderRadius.all(6.0F), hoverFill);
            CursorUtility.set(CursorType.HAND);
        }
        float lw = font.width(label);
        context.drawText(font, label, x + (w - lw) / 2.0F, y + (h - font.height()) / 2.0F,
                textColor.withAlpha((int) (255 * alpha)));
    }

    /** Кнопка без заливки — только смена цвета текста при наведении (как Modules/Interface в попапе). */
    public static void renderTextOnly(UIContext context, Font font, String label,
                                       float x, float y, float w, float h,
                                       boolean hovered, ColorRGBA idleColor, ColorRGBA hoverColor,
                                       float alpha) {
        if (hovered) {
            CursorUtility.set(CursorType.HAND);
        }
        float lw = font.width(label);
        ColorRGBA color = hovered ? hoverColor : idleColor;
        context.drawText(font, label, x + (w - lw) / 2.0F, y + (h - font.height()) / 2.0F,
                color.withAlpha((int) (255 * alpha)));
    }
}
