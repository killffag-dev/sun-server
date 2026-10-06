package naryn.sun.ui.menu.components.settings.configs;

import naryn.sun.Sun;
import naryn.sun.framework.base.UIContext;
import naryn.sun.framework.msdf.Font;
import naryn.sun.framework.msdf.Fonts;
import naryn.sun.framework.objects.BorderRadius;
import naryn.sun.framework.objects.MouseButton;
import naryn.sun.systems.config.ConfigFile;
import naryn.sun.systems.localization.Localizator;
import naryn.sun.systems.theme.ClientAppearance;
import naryn.sun.ui.components.textfield.TextField;
import naryn.sun.ui.menu.skin.MenuSkin;
import naryn.sun.utility.animation.base.Animation;
import naryn.sun.utility.colors.ColorRGBA;
import naryn.sun.utility.colors.Colors;
import naryn.sun.utility.game.cursor.CursorType;
import naryn.sun.utility.game.cursor.CursorUtility;
import naryn.sun.utility.gui.GuiUtility;

/**
 * Отрисовка и обработка кликов отдельной строки конфигурации в списке.
 * Полная локализация и понятная индикация состояний (обычный / переименование / подтверждение удаления).
 */
public class ConfigItemRow {

    public static final float ROW_HEIGHT = 28.0F;
    public static final float BTN_HEIGHT = 18.0F;

    public interface RowActionListener {
        void onLoad(ConfigFile cfg);
        void onSave(ConfigFile cfg);
        void onStartRename(ConfigFile cfg);
        void onConfirmRename();
        void onCancelRename();
        void onStartDelete(ConfigFile cfg);
        void onConfirmDelete(ConfigFile cfg);
        void onCancelDelete();
    }

    public static void renderNormal(UIContext context, float bx, float by, float bw, ConfigFile cfg,
                                    boolean isCurrent, Animation hoverAnim, Animation actionHoverAnim) {
        MenuSkin skin = MenuSkin.current();
        skin.renderSettingRow(context, bx, by, bw, ROW_HEIGHT, BorderRadius.all(6.0F), 1.0F);

        float hover = hoverAnim.getValue();
        if (hover > 0.01F) {
            context.drawRoundedRect(bx, by, bw, ROW_HEIGHT, BorderRadius.all(6.0F),
                    skin.hoverBackground(hover));
        }

        if (isCurrent) {
            ColorRGBA accent = ClientAppearance.getAccent();
            context.drawRoundedRect(bx, by, bw, ROW_HEIGHT, BorderRadius.all(6.0F), accent.withAlpha(22.0F));
            context.drawRoundedBorder(bx, by, bw, ROW_HEIGHT, 0.9F, BorderRadius.all(6.0F), accent.withAlpha(160.0F));
        }

        // Правые кнопки управления (Корзина и Карандаш)
        float btnY = by + (ROW_HEIGHT - BTN_HEIGHT) / 2.0F;
        float iconBtnW = 18.0F;
        float trashX = bx + bw - iconBtnW - 5.0F;
        renderIconAction(context, trashX, btnY, iconBtnW, BTN_HEIGHT, Sun.id("icons/trash.png"), false);

        float pencilX = trashX - iconBtnW - 4.0F;
        renderIconAction(context, pencilX, btnY, iconBtnW, BTN_HEIGHT, Sun.id("icons/pencil.png"), false);

        Font nameFont = Fonts.MEDIUM.getFont(7.5F);
        float textX = bx + 9.0F;
        float textY = by + (ROW_HEIGHT - nameFont.height()) / 2.0F;
        float maxNameW = pencilX - textX - 6.0F;

        if (isCurrent) {
            ColorRGBA textCol = Colors.getTextColor();
            if (nameFont.width(cfg.getFileName()) <= maxNameW) {
                context.drawText(nameFont, cfg.getFileName(), textX, textY, textCol);
            } else {
                context.drawFadeoutText(nameFont, cfg.getFileName(), textX, textY, textCol, 0.85F, 1.0F, maxNameW);
            }
        } else {
            if (GuiUtility.isHovered(bx, by, bw, ROW_HEIGHT, context.getMouseX(), context.getMouseY())) {
                CursorUtility.set(CursorType.HAND);
            }
            ColorRGBA textCol = Colors.getTextColor().withAlpha(hover > 0.01F ? 255.0F : 205.0F);
            if (nameFont.width(cfg.getFileName()) <= maxNameW) {
                context.drawText(nameFont, cfg.getFileName(), textX, textY, textCol);
            } else {
                context.drawFadeoutText(nameFont, cfg.getFileName(), textX, textY, textCol, 0.85F, 1.0F, maxNameW);
            }
        }
    }

    public static void renderEditing(UIContext context, float bx, float by, float bw, TextField editField) {
        MenuSkin.current().renderSettingBox(context, bx, by, bw, ROW_HEIGHT, BorderRadius.all(6.0F), 1.0F);

        float btnH = BTN_HEIGHT;
        float btnY = by + (ROW_HEIGHT - btnH) / 2.0F;

        Font btnFont = Fonts.MEDIUM.getFont(6.5F);
        String applyText = Localizator.translate("menu.gui_settings.configs.apply");
        String cancelText = Localizator.translate("menu.gui_settings.configs.cancel");
        float applyW = btnFont.width(applyText) + 12.0F;
        float cancelW = btnFont.width(cancelText) + 12.0F;

        float cancelX = bx + bw - cancelW - 6.0F;
        float applyX = cancelX - applyW - 4.0F;

        float fieldX = bx + 8.0F;
        float fieldW = applyX - fieldX - 8.0F;
        float fieldH = 18.0F;
        float fieldY = by + (ROW_HEIGHT - fieldH) / 2.0F;

        editField.set(fieldX, fieldY, fieldW, fieldH);
        editField.setAlpha(1.0F);
        editField.setTextColor(Colors.getTextColor());
        editField.render(context);

        // Кнопка Применить
        boolean applyHover = GuiUtility.isHovered(applyX, btnY, applyW, btnH, context.getMouseX(), context.getMouseY());
        if (applyHover) CursorUtility.set(CursorType.HAND);
        ColorRGBA accent = ClientAppearance.getAccent();
        context.drawRoundedRect(applyX, btnY, applyW, btnH, BorderRadius.all(4.0F),
                accent.withAlpha((int) ((applyHover ? 230 : 190) * 1.0F)));
        context.drawCenteredText(btnFont, applyText, applyX + applyW / 2.0F, btnY + (btnH - btnFont.height()) / 2.0F + 0.5F, ColorRGBA.WHITE);

        // Кнопка Отмена
        boolean cancelHover = GuiUtility.isHovered(cancelX, btnY, cancelW, btnH, context.getMouseX(), context.getMouseY());
        if (cancelHover) CursorUtility.set(CursorType.HAND);
        MenuSkin.current().renderButtonBox(context, cancelX, btnY, cancelW, btnH, BorderRadius.all(4.0F), cancelHover ? 1.0F : 0.0F, 1.0F);
        context.drawCenteredText(btnFont, cancelText, cancelX + cancelW / 2.0F, btnY + (btnH - btnFont.height()) / 2.0F + 0.5F,
                Colors.getTextColor().withAlpha(cancelHover ? 255.0F : 190.0F));
    }

    public static void renderConfirmDelete(UIContext context, float bx, float by, float bw, ConfigFile cfg) {
        MenuSkin.current().renderSettingBox(context, bx, by, bw, ROW_HEIGHT, BorderRadius.all(6.0F), 1.0F);

        ColorRGBA warnCol = new ColorRGBA(245, 60, 60, 255);
        context.drawRoundedBorder(bx, by, bw, ROW_HEIGHT, 0.8F, BorderRadius.all(6.0F), warnCol.withAlpha(90.0F));

        Font labelFont = Fonts.REGULAR.getFont(7.0F);
        Font btnFont = Fonts.MEDIUM.getFont(6.5F);

        String yesText = Localizator.translate("menu.gui_settings.configs.delete_yes");
        String noText = Localizator.translate("menu.gui_settings.configs.delete_no");
        float yesW = btnFont.width(yesText) + 12.0F;
        float noW = btnFont.width(noText) + 12.0F;

        float btnH = BTN_HEIGHT;
        float btnY = by + (ROW_HEIGHT - btnH) / 2.0F;
        float noX = bx + bw - noW - 6.0F;
        float yesX = noX - yesW - 4.0F;

        String confirmMsg = Localizator.translate("menu.gui_settings.configs.delete_confirm", cfg.getFileName());
        float maxTextW = yesX - (bx + 10.0F) - 6.0F;
        context.drawFadeoutText(labelFont, confirmMsg, bx + 10.0F, by + (ROW_HEIGHT - labelFont.height()) / 2.0F,
                Colors.getTextColor().withAlpha(230.0F), 1.0F, 1.0F, maxTextW);

        // Кнопка подтверждения удаления
        boolean yesHover = GuiUtility.isHovered(yesX, btnY, yesW, btnH, context.getMouseX(), context.getMouseY());
        if (yesHover) CursorUtility.set(CursorType.HAND);
        context.drawRoundedRect(yesX, btnY, yesW, btnH, BorderRadius.all(4.0F),
                warnCol.withAlpha((int) ((yesHover ? 230 : 180) * 1.0F)));
        context.drawCenteredText(btnFont, yesText, yesX + yesW / 2.0F, btnY + (btnH - btnFont.height()) / 2.0F + 0.5F, ColorRGBA.WHITE);

        // Кнопка отмены
        boolean noHover = GuiUtility.isHovered(noX, btnY, noW, btnH, context.getMouseX(), context.getMouseY());
        if (noHover) CursorUtility.set(CursorType.HAND);
        MenuSkin.current().renderButtonBox(context, noX, btnY, noW, btnH, BorderRadius.all(4.0F), noHover ? 1.0F : 0.0F, 1.0F);
        context.drawCenteredText(btnFont, noText, noX + noW / 2.0F, btnY + (btnH - btnFont.height()) / 2.0F + 0.5F,
                Colors.getTextColor().withAlpha(noHover ? 255.0F : 190.0F));
    }

    private static void renderIconAction(UIContext context, float bx, float by, float bw, float bh,
                                         net.minecraft.util.Identifier icon, boolean accent) {
        boolean hovered = GuiUtility.isHovered(bx, by, bw, bh, context.getMouseX(), context.getMouseY());
        if (hovered) CursorUtility.set(CursorType.HAND);

        MenuSkin skin = MenuSkin.current();
        skin.renderButtonBox(context, bx, by, bw, bh, BorderRadius.all(4.0F), hovered ? 1.0F : 0.0F, 1.0F);

        float iconSize = 9.0F;
        float ix = bx + (bw - iconSize) / 2.0F;
        float iy = by + (bh - iconSize) / 2.0F;
        ColorRGBA iconColor = hovered
                ? Colors.getTextColor().withAlpha(255.0F)
                : Colors.getTextColor().withAlpha(170.0F);

        context.drawTexture(icon, ix, iy, iconSize, iconSize, iconColor);
    }

    public static boolean handleClickNormal(float bx, float by, float bw, ConfigFile cfg, boolean isCurrent,
                                            double mouseX, double mouseY, RowActionListener listener) {
        float btnY = by + (ROW_HEIGHT - BTN_HEIGHT) / 2.0F;
        float iconBtnW = 18.0F;
        float trashX = bx + bw - iconBtnW - 5.0F;

        // 1. Клик по корзине
        if (GuiUtility.isHovered(trashX, btnY, iconBtnW, BTN_HEIGHT, mouseX, mouseY)) {
            listener.onStartDelete(cfg);
            return true;
        }

        // 2. Клик по карандашу
        float pencilX = trashX - iconBtnW - 4.0F;
        if (GuiUtility.isHovered(pencilX, btnY, iconBtnW, BTN_HEIGHT, mouseX, mouseY)) {
            listener.onStartRename(cfg);
            return true;
        }

        // 3. Клик по всей плашке для неактивного конфига загружает его (в 1 клик)
        if (!isCurrent && GuiUtility.isHovered(bx, by, bw, ROW_HEIGHT, mouseX, mouseY)) {
            listener.onLoad(cfg);
            return true;
        }

        return false;
    }

    public static boolean handleClickEditing(float bx, float by, float bw, TextField editField,
                                             double mouseX, double mouseY, RowActionListener listener) {
        float btnH = BTN_HEIGHT;
        float btnY = by + (ROW_HEIGHT - btnH) / 2.0F;

        Font btnFont = Fonts.MEDIUM.getFont(6.5F);
        String applyText = Localizator.translate("menu.gui_settings.configs.apply");
        String cancelText = Localizator.translate("menu.gui_settings.configs.cancel");
        float applyW = btnFont.width(applyText) + 12.0F;
        float cancelW = btnFont.width(cancelText) + 12.0F;

        float cancelX = bx + bw - cancelW - 6.0F;
        float applyX = cancelX - applyW - 4.0F;

        if (GuiUtility.isHovered(applyX, btnY, applyW, btnH, mouseX, mouseY)) {
            listener.onConfirmRename();
            return true;
        }
        if (GuiUtility.isHovered(cancelX, btnY, cancelW, btnH, mouseX, mouseY)) {
            listener.onCancelRename();
            return true;
        }

        editField.onMouseClicked(mouseX, mouseY, MouseButton.LEFT);
        return true;
    }

    public static boolean handleClickDelete(float bx, float by, float bw, ConfigFile cfg,
                                            double mouseX, double mouseY, RowActionListener listener) {
        Font btnFont = Fonts.MEDIUM.getFont(6.5F);
        String yesText = Localizator.translate("menu.gui_settings.configs.delete_yes");
        String noText = Localizator.translate("menu.gui_settings.configs.delete_no");
        float yesW = btnFont.width(yesText) + 12.0F;
        float noW = btnFont.width(noText) + 12.0F;

        float btnH = BTN_HEIGHT;
        float btnY = by + (ROW_HEIGHT - btnH) / 2.0F;
        float noX = bx + bw - noW - 6.0F;
        float yesX = noX - yesW - 4.0F;

        if (GuiUtility.isHovered(yesX, btnY, yesW, btnH, mouseX, mouseY)) {
            listener.onConfirmDelete(cfg);
            return true;
        }
        if (GuiUtility.isHovered(noX, btnY, noW, btnH, mouseX, mouseY)) {
            listener.onCancelDelete();
            return true;
        }

        return false;
    }
}
