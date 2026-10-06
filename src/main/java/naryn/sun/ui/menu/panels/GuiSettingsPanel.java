package naryn.sun.ui.menu.panels;

import naryn.sun.framework.base.UIContext;
import naryn.sun.framework.msdf.Font;
import naryn.sun.framework.msdf.Fonts;
import naryn.sun.framework.objects.MouseButton;
import naryn.sun.systems.localization.Localizator;
import naryn.sun.ui.menu.components.GuiSettingCard;
import naryn.sun.ui.menu.components.GuiSettingType;
import naryn.sun.utility.colors.Colors;

import java.util.ArrayList;
import java.util.List;

public final class GuiSettingsPanel {

    private static final List<GuiSettingCard> cards = new ArrayList<>();
    private static final float GRID_GAP = 8.0F;
    private static final float INNER_PAD_X = 6.0F;
    private static final float HEADER_HEIGHT = 20.0F;

    static {
        cards.add(new GuiSettingCard(GuiSettingType.APPEARANCE));
        cards.add(new GuiSettingCard(GuiSettingType.BACKGROUND));
        cards.add(new GuiSettingCard(GuiSettingType.PALETTE));
        cards.add(new GuiSettingCard(GuiSettingType.LANGUAGE));
        cards.add(new GuiSettingCard(GuiSettingType.SOUNDS));
        cards.add(new GuiSettingCard(GuiSettingType.ANIMATIONS));
        cards.add(new GuiSettingCard(GuiSettingType.CONFIGS));
        cards.add(new GuiSettingCard(GuiSettingType.JVM_PRESETS));
        cards.add(new GuiSettingCard(GuiSettingType.CREDITS));
    }

    private GuiSettingsPanel() {
    }

    public static List<GuiSettingCard> getCards() {
        return cards;
    }

    public static float getTotalHeight() {
        float[] colHeights = {0.0F, 0.0F};
        for (int i = 0; i < cards.size(); i++) {
            colHeights[i % 2] += cards.get(i).getHeight() + GRID_GAP;
        }
        return HEADER_HEIGHT + Math.max(colHeights[0], colHeights[1]) + 16.0F;
    }

    public static String render(UIContext context, float alpha, float panelX, float panelY, float panelW, float mouseX, float mouseY) {
        // 1. Заголовок "Настройки GUI" (без описания под ним)
        Font titleFont = Fonts.MEDIUM.getFont(8.0F);
        String title = Localizator.translate("menu.gui_settings.title");
        context.drawText(titleFont, title, panelX + INNER_PAD_X, panelY + 2.0F,
                Colors.getTextColor().withAlpha((int) (255 * alpha)));

        // 2. Двухколоночная сетка карточек с безопасными отступами (чтобы карточки не вылезали за края)
        float startY = panelY + HEADER_HEIGHT;
        float usableW = panelW - INNER_PAD_X * 2.0F;
        float colW = (usableW - GRID_GAP) / 2.0F;
        float col0X = panelX + INNER_PAD_X;
        float col1X = col0X + colW + GRID_GAP;
        float[] colY = {startY, startY};

        String hoveredTooltip = "";

        for (int i = 0; i < cards.size(); i++) {
            GuiSettingCard card = cards.get(i);
            int col = i % 2;
            card.setX(col == 0 ? col0X : col1X);
            card.setY(colY[col]);
            card.setWidth(colW);

            card.render(context);

            if (card.isHovered(mouseX, mouseY) && !card.isExpandedVisually()) {
                hoveredTooltip = card.getType().getDescription();
            }

            colY[col] += card.getHeight() + GRID_GAP;
        }

        return hoveredTooltip;
    }

    public static boolean handleMouseClicked(float panelX, float panelY, float panelW, double mouseX, double mouseY, MouseButton button) {
        for (GuiSettingCard card : cards) {
            if (card.isHovered(mouseX, mouseY)) {
                card.onMouseClicked(mouseX, mouseY, button);
                return true;
            }
        }
        return false;
    }

    public static boolean handleMouseDragged(float panelX, float panelY, float panelW, double mouseX, double mouseY) {
        for (GuiSettingCard card : cards) {
            if (card.isDragging()) {
                card.onMouseDragged(mouseX, mouseY);
                return true;
            }
        }
        return false;
    }

    public static void handleMouseReleased(MouseButton button) {
        for (GuiSettingCard card : cards) {
            card.onMouseReleased(0, 0, button);
        }
    }

    /** Клавиатурный ввод — прокидывается всем карточкам, а не только раскрытым: раскрытых может быть несколько. */
    public static void handleKeyPressed(int keyCode, int scanCode, int modifiers) {
        for (GuiSettingCard card : cards) {
            card.onKeyPressed(keyCode, scanCode, modifiers);
        }
    }

    public static boolean handleCharTyped(char chr, int modifiers) {
        boolean handled = false;
        for (GuiSettingCard card : cards) {
            if (card.charTyped(chr, modifiers)) {
                handled = true;
            }
        }
        return handled;
    }
}