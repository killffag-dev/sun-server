package naryn.sun.ui.menu.dropdown.components.settings.impl;

import naryn.sun.framework.base.CustomComponent;
import naryn.sun.framework.base.UIContext;
import naryn.sun.framework.msdf.Font;
import naryn.sun.framework.msdf.Fonts;
import naryn.sun.framework.objects.BorderRadius;
import naryn.sun.framework.objects.MouseButton;
import naryn.sun.systems.localization.Localizator;
import naryn.sun.systems.setting.settings.CommandBindEntrySetting;
import naryn.sun.ui.components.textfield.TextField;
import naryn.sun.ui.menu.dropdown.components.settings.MenuSettingComponent;
import naryn.sun.ui.menu.skin.MenuSkin;
import naryn.sun.utility.animation.base.Animation;
import naryn.sun.utility.animation.base.Easing;
import naryn.sun.utility.colors.ColorRGBA;
import naryn.sun.utility.colors.Colors;
import naryn.sun.utility.game.TextUtility;
import naryn.sun.utility.game.cursor.CursorType;
import naryn.sun.utility.game.cursor.CursorUtility;
import naryn.sun.utility.gui.GuiUtility;
import naryn.sun.utility.sounds.ClientSoundManager;

public class CommandBindEntryComponent extends MenuSettingComponent<CommandBindEntrySetting> {

    private TextField textField;
    private final Animation widthAnimation = new Animation(250L, Easing.FIGMA_EASE_IN_OUT);
    private final Animation deleteHoverAnim = new Animation(200L, Easing.FIGMA_EASE_IN_OUT);
    private boolean bindingMode = false;

    private static final float MARGIN_X    = 7.0F;
    private static final float MARGIN_Y    = 3.0F;
    private static final float INNER_PAD_X = 7.0F;
    private static final float BOX_HEIGHT  = 75.0F;
    private static final float PILL_HEIGHT = 13.0F;
    private static final float FIELD_H     = 15.0F;
    private static final float DELETE_H    = 15.0F;

    public CommandBindEntryComponent(CommandBindEntrySetting setting, CustomComponent parent) {
        super(setting, parent);
    }

    @Override
    public void onInit() {
        this.width = 13.0F;
        this.height = BOX_HEIGHT + MARGIN_Y * 2;
        this.textField = new TextField(Fonts.REGULAR.getFont(7.5F));
        this.textField.paste(this.setting.getCommand());
        this.textField.setPreview(Localizator.translate("type_text"));
        super.onInit();
    }

    @Override
    public void update(UIContext context) {
        super.update(context);
    }

    private float getBoxX() {
        return this.x + MARGIN_X;
    }

    private float getBoxY() {
        return this.y + MARGIN_Y;
    }

    private float getBoxW() {
        return this.width - MARGIN_X * 2;
    }

    private float getPillW(Font bindFont, String bindLabel) {
        float target = bindFont.width(bindLabel) + 12.0F;
        this.widthAnimation.update(target);
        return this.widthAnimation.getValue();
    }

    private float getPillX(float boxX, float boxW, float pillW) {
        return boxX + boxW - INNER_PAD_X - pillW;
    }

    private float getPillY(float boxY) {
        return boxY + 6.0F;
    }

    private float getFieldX(float boxX) {
        return boxX + INNER_PAD_X;
    }

    private float getFieldY(float boxY) {
        return boxY + 34.0F;
    }

    private float getFieldW(float boxW) {
        return boxW - INNER_PAD_X * 2;
    }

    private float getDeleteX(float boxX) {
        return boxX + INNER_PAD_X;
    }

    private float getDeleteY(float boxY) {
        return boxY + 54.0F;
    }

    private float getDeleteW(float boxW) {
        return boxW - INNER_PAD_X * 2;
    }

    @Override
    protected void renderComponent(UIContext context) {
        float boxX = getBoxX();
        float boxY = getBoxY();
        float boxW = getBoxW();
        float boxH = BOX_HEIGHT;

        // Фон и рамка отдельного бокса бинда
        ColorRGBA boxBg = new ColorRGBA(255, 255, 255, 8);
        ColorRGBA boxBorder = new ColorRGBA(255, 255, 255, 16);
        context.drawRoundedRect(boxX, boxY, boxW, boxH, BorderRadius.all(6.0F), boxBg);
        context.drawRoundedBorder(boxX, boxY, boxW, boxH, 0.5F, BorderRadius.all(6.0F), boxBorder);

        Font labelFont = Fonts.REGULAR.getFont(7.0F);
        Font subFont = Fonts.REGULAR.getFont(6.5F);
        Font bindFont = Fonts.REGULAR.getFont(6.0F);

        // 1. Строка клавиши: метка "Клавиша" и плашка бинда
        String keyLabel = Localizator.translate("modules.settings.command_bind.entry_key");
        context.drawText(labelFont, keyLabel, boxX + INNER_PAD_X, boxY + 8.5F, Colors.getTextColor().withAlpha(210));

        String bindText = this.bindingMode
                ? Localizator.translate("menu.module.keybind.press")
                : (this.setting.getKey() == -1 ? Localizator.translate("menu.module.keybind.none") : TextUtility.getKeyName(this.setting.getKey()));

        float pillW = getPillW(bindFont, bindText);
        float pillH = PILL_HEIGHT;
        float pillX = getPillX(boxX, boxW, pillW);
        float pillY = getPillY(boxY);

        MenuSkin.current().renderBindPill(context, pillX, pillY, pillW, pillH, BorderRadius.all(3.5F), this.bindingMode, 1.0F);

        ColorRGBA bindTextColor = this.bindingMode
                ? Colors.ACCENT.mix(ColorRGBA.WHITE, 0.3F)
                : Colors.getTextColor().withAlpha(235);
        float bindTextX = pillX + (pillW - bindFont.width(bindText)) / 2.0F;
        float bindTextY = pillY + (pillH - bindFont.height()) / 2.0F;
        context.drawText(bindFont, bindText, bindTextX, bindTextY, bindTextColor);

        // 2. Строка команды: заголовок "Команда" и поле ввода
        String cmdLabel = Localizator.translate("modules.settings.command_bind.entry_command");
        context.drawText(subFont, cmdLabel, boxX + INNER_PAD_X, boxY + 24.0F, Colors.getTextColor().withAlpha(165));

        float fieldX = getFieldX(boxX);
        float fieldY = getFieldY(boxY);
        float fieldW = getFieldW(boxW);
        float fieldH = FIELD_H;

        MenuSkin.current().renderSettingBox(context, fieldX, fieldY, fieldW, fieldH, BorderRadius.all(4.0F), 1.0F);
        this.textField.set(fieldX, fieldY, fieldW, fieldH);
        this.textField.setAlpha(1.0F);
        this.textField.setTextColor(Colors.getTextColor());
        this.textField.render(context);
        this.setting.setCommand(this.textField.getBuiltText());

        // 3. Кнопка "Удалить"
        float delX = getDeleteX(boxX);
        float delY = getDeleteY(boxY);
        float delW = getDeleteW(boxW);
        float delH = DELETE_H;

        boolean delHovered = GuiUtility.isHovered(delX, delY, delW, delH, context.getMouseX(), context.getMouseY());
        this.deleteHoverAnim.update(delHovered);
        float delHover = this.deleteHoverAnim.getValue();

        MenuSkin.current().renderButtonBox(context, delX, delY, delW, delH, BorderRadius.all(4.0F), delHover, 1.0F);
        if (delHover > 0.01F) {
            context.drawRoundedRect(delX, delY, delW, delH, BorderRadius.all(4.0F),
                    new ColorRGBA(239, 68, 68, (int)(32 * delHover)));
            context.drawRoundedBorder(delX, delY, delW, delH, 0.5F, BorderRadius.all(4.0F),
                    new ColorRGBA(239, 68, 68, (int)(85 * delHover)));
        }

        String delLabel = Localizator.translate("modules.settings.command_bind.entry_delete");
        ColorRGBA delTextColor = Colors.getTextColor().withAlpha((int)(200 + 55 * delHover));
        if (delHover > 0.01F) {
            delTextColor = delTextColor.mix(new ColorRGBA(252, 165, 165, 255), delHover * 0.8F);
        }
        context.drawCenteredText(subFont, delLabel, delX + delW / 2.0F,
                delY + GuiUtility.getMiddleOfBox(subFont.height(), delH) - 0.5F, delTextColor);

        // Курсор
        boolean pillHovered = GuiUtility.isHovered(pillX, pillY, pillW, pillH, context.getMouseX(), context.getMouseY());
        if (pillHovered || delHovered || GuiUtility.isHovered(fieldX, fieldY, fieldW, fieldH, context.getMouseX(), context.getMouseY())) {
            CursorUtility.set(CursorType.HAND);
        }
    }

    @Override
    public void drawSplit(UIContext context) {
    }

    @Override
    public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
        float boxX = getBoxX();
        float boxY = getBoxY();
        float boxW = getBoxW();

        Font bindFont = Fonts.REGULAR.getFont(6.0F);
        String bindText = this.bindingMode
                ? Localizator.translate("menu.module.keybind.press")
                : (this.setting.getKey() == -1 ? Localizator.translate("menu.module.keybind.none") : TextUtility.getKeyName(this.setting.getKey()));
        float pillW = getPillW(bindFont, bindText);
        float pillH = PILL_HEIGHT;
        float pillX = getPillX(boxX, boxW, pillW);
        float pillY = getPillY(boxY);

        boolean pillHovered = GuiUtility.isHovered(pillX, pillY, pillW, pillH, mouseX, mouseY);

        if (button == MouseButton.LEFT) {
            if (pillHovered) {
                this.bindingMode = !this.bindingMode;
                return;
            } else if (this.bindingMode) {
                this.bindingMode = false;
                return;
            }

            // Клик по кнопке удалить
            float delX = getDeleteX(boxX);
            float delY = getDeleteY(boxY);
            float delW = getDeleteW(boxW);
            float delH = DELETE_H;
            if (GuiUtility.isHovered(delX, delY, delW, delH, mouseX, mouseY)) {
                ClientSoundManager.getInstance().playButtonClick();
                this.setting.delete();
                return;
            }
        }

        if (this.bindingMode && button != MouseButton.LEFT) {
            this.setting.setKey(button.getButtonIndex());
            this.bindingMode = false;
            return;
        }

        this.textField.onMouseClicked(mouseX, mouseY, button);
        super.onMouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void onMouseReleased(double mouseX, double mouseY, MouseButton button) {
        this.textField.onMouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void onKeyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.bindingMode) {
            // Esc (256) или Delete (261) — сброс бинда
            if (keyCode == 256 || keyCode == 261) {
                this.setting.setKey(-1);
                this.bindingMode = false;
                return;
            }

            this.setting.setKey(keyCode);
            this.bindingMode = false;
            return;
        }

        this.textField.onKeyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        return this.textField.charTyped(chr, modifiers);
    }

    @Override
    public float getHeight() {
        return this.height = BOX_HEIGHT + MARGIN_Y * 2;
    }
}
