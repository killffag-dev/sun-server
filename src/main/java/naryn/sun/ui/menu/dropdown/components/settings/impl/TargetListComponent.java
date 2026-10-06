package naryn.sun.ui.menu.dropdown.components.settings.impl;

import naryn.sun.Sun;
import naryn.sun.framework.base.CustomComponent;
import naryn.sun.framework.base.UIContext;
import naryn.sun.framework.msdf.Font;
import naryn.sun.framework.msdf.Fonts;
import naryn.sun.framework.objects.BorderRadius;
import naryn.sun.framework.objects.MouseButton;
import naryn.sun.systems.localization.Localizator;
import naryn.sun.systems.setting.settings.TargetListSetting;
import naryn.sun.ui.components.textfield.TextField;
import naryn.sun.ui.menu.dropdown.components.settings.MenuSettingComponent;
import naryn.sun.ui.menu.skin.MenuSkin;
import naryn.sun.utility.animation.base.Animation;
import naryn.sun.utility.animation.base.Easing;
import naryn.sun.utility.colors.ColorRGBA;
import naryn.sun.utility.colors.Colors;
import naryn.sun.utility.game.cursor.CursorType;
import naryn.sun.utility.game.cursor.CursorUtility;
import naryn.sun.utility.gui.GuiUtility;
import naryn.sun.utility.sounds.ClientSoundManager;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class TargetListComponent extends MenuSettingComponent<TargetListSetting> {

    private TextField textField;
    private final Animation addHoverAnim = new Animation(200L, Easing.FIGMA_EASE_IN_OUT);

    private static final float MARGIN_X     = 7.0F;
    private static final float MARGIN_Y     = 3.0F;
    private static final float INNER_PAD_X  = 7.0F;
    private static final float FIELD_H      = 15.0F;
    private static final float ADD_BTN_W    = 18.0F;
    private static final float SEP_GAP      = 5.0F;
    private static final float ITEM_H       = 16.0F;
    private static final float DEL_BTN_SIZE = 12.0F;
    private static final float BOTTOM_PAD   = 5.0F;

    public TargetListComponent(TargetListSetting setting, CustomComponent parent) {
        super(setting, parent);
    }

    @Override
    public void onInit() {
        this.width = 13.0F;
        this.height = 70.0F;
        this.textField = new TextField(Fonts.REGULAR.getFont(7.5F));
        this.textField.setPreview(Localizator.translate("modules.settings.target.placeholder"));
        super.onInit();
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

    private float calculateBoxHeight(int count) {
        float contentH = count == 0 ? 14.0F : (count * ITEM_H);
        return (19.0F + FIELD_H + SEP_GAP * 2) + contentH + BOTTOM_PAD;
    }

    @Override
    protected void renderComponent(UIContext context) {
        List<String> targets = Sun.getInstance().getTargetManager().getTarget();
        int targetCount = targets.size();

        float boxX = getBoxX();
        float boxY = getBoxY();
        float boxW = getBoxW();
        float boxH = calculateBoxHeight(targetCount);

        // 1. Фон и рамка бокса
        ColorRGBA boxBg = new ColorRGBA(255, 255, 255, 8);
        ColorRGBA boxBorder = new ColorRGBA(255, 255, 255, 16);
        context.drawRoundedRect(boxX, boxY, boxW, boxH, BorderRadius.all(6.0F), boxBg);
        context.drawRoundedBorder(boxX, boxY, boxW, boxH, 0.5F, BorderRadius.all(6.0F), boxBorder);

        // 2. Заголовок и счетчик
        Font titleFont = Fonts.REGULAR.getFont(7.0F);
        String title = Localizator.translate("modules.settings.target.list");
        context.drawText(titleFont, title, boxX + INNER_PAD_X, boxY + 6.0F, Colors.getTextColor().withAlpha(200));

        Font countFont = Fonts.REGULAR.getFont(6.0F);
        String countText = String.valueOf(targetCount);
        float countW = countFont.width(countText) + 8.0F;
        float countH = 11.0F;
        float countX = boxX + boxW - INNER_PAD_X - countW;
        float countY = boxY + 5.0F;
        MenuSkin.current().renderBindPill(context, countX, countY, countW, countH, BorderRadius.all(3.0F), false, 1.0F);
        context.drawCenteredText(countFont, countText, countX + countW / 2.0F,
                countY + GuiUtility.getMiddleOfBox(countFont.height(), countH) - 0.5F,
                Colors.getTextColor().withAlpha(190));

        // 3. Поле ввода никнейма и кнопка [+]
        float fieldX = boxX + INNER_PAD_X;
        float fieldY = boxY + 19.0F;
        float fieldW = boxW - INNER_PAD_X * 2 - ADD_BTN_W - 4.0F;
        float addBtnX = fieldX + fieldW + 4.0F;

        this.textField.setPreview(Localizator.translate("modules.settings.target.placeholder"));
        MenuSkin.current().renderSettingBox(context, fieldX, fieldY, fieldW, FIELD_H, BorderRadius.all(4.0F), 1.0F);
        this.textField.set(fieldX, fieldY, fieldW, FIELD_H);
        this.textField.setAlpha(1.0F);
        this.textField.setTextColor(Colors.getTextColor());
        this.textField.render(context);

        boolean addHover = GuiUtility.isHovered(addBtnX, fieldY, ADD_BTN_W, FIELD_H, context.getMouseX(), context.getMouseY());
        this.addHoverAnim.update(addHover);
        float addHoverVal = this.addHoverAnim.getValue();

        MenuSkin.current().renderButtonBox(context, addBtnX, fieldY, ADD_BTN_W, FIELD_H, BorderRadius.all(4.0F), addHoverVal, 1.0F);
        Font plusFont = Fonts.MEDIUM.getFont(8.0F);
        ColorRGBA plusColor = Colors.getTextColor().withAlpha((int)(200 + 55 * addHoverVal));
        if (addHoverVal > 0.01F) {
            plusColor = plusColor.mix(Colors.ACCENT, addHoverVal * 0.7F);
        }
        context.drawCenteredText(plusFont, "+", addBtnX + ADD_BTN_W / 2.0F,
                fieldY + GuiUtility.getMiddleOfBox(plusFont.height(), FIELD_H) - 0.5F, plusColor);

        // 4. Разделитель
        float sepY = fieldY + FIELD_H + SEP_GAP;
        context.drawRect(boxX + INNER_PAD_X, sepY, boxW - INNER_PAD_X * 2, 0.5F, new ColorRGBA(255, 255, 255, 16));

        // 5. Список целей
        float listStartY = sepY + SEP_GAP;
        if (targetCount == 0) {
            Font emptyFont = Fonts.REGULAR.getFont(6.5F);
            String emptyText = Localizator.translate("modules.settings.target.empty");
            context.drawCenteredText(emptyFont, emptyText, boxX + boxW / 2.0F, listStartY + 3.0F, Colors.getTextColor().withAlpha(120));
        } else {
            Font itemFont = Fonts.REGULAR.getFont(7.0F);
            Font crossFont = Fonts.REGULAR.getFont(7.0F);

            for (int i = 0; i < targetCount; i++) {
                String targetName = targets.get(i);
                float itemY = listStartY + i * ITEM_H;
                float delX = boxX + boxW - INNER_PAD_X - DEL_BTN_SIZE;
                float delY = itemY + (ITEM_H - DEL_BTN_SIZE) / 2.0F;

                boolean delHover = GuiUtility.isHovered(delX, delY, DEL_BTN_SIZE, DEL_BTN_SIZE, context.getMouseX(), context.getMouseY());
                if (delHover) {
                    CursorUtility.set(CursorType.HAND);
                }

                // Имя цели
                context.drawText(itemFont, targetName, boxX + INNER_PAD_X,
                        itemY + (ITEM_H - itemFont.height()) / 2.0F,
                        Colors.getTextColor().withAlpha(220));

                // Кнопка удаления [×]
                ColorRGBA delBg = delHover ? new ColorRGBA(239, 68, 68, 45) : new ColorRGBA(255, 255, 255, 8);
                ColorRGBA delBorder = delHover ? new ColorRGBA(239, 68, 68, 110) : new ColorRGBA(255, 255, 255, 18);
                context.drawRoundedRect(delX, delY, DEL_BTN_SIZE, DEL_BTN_SIZE, BorderRadius.all(3.0F), delBg);
                context.drawRoundedBorder(delX, delY, DEL_BTN_SIZE, DEL_BTN_SIZE, 0.5F, BorderRadius.all(3.0F), delBorder);

                ColorRGBA crossColor = delHover ? new ColorRGBA(252, 165, 165, 255) : Colors.getTextColor().withAlpha(170);
                context.drawCenteredText(crossFont, "\u00d7", delX + DEL_BTN_SIZE / 2.0F,
                        delY + GuiUtility.getMiddleOfBox(crossFont.height(), DEL_BTN_SIZE) - 0.5F, crossColor);
            }
        }

        // Курсор
        if (addHover || GuiUtility.isHovered(fieldX, fieldY, fieldW, FIELD_H, context.getMouseX(), context.getMouseY())) {
            CursorUtility.set(CursorType.HAND);
        }
    }

    @Override
    public void drawSplit(UIContext context) {
    }

    private void addTargetFromField() {
        String nick = this.textField.getBuiltText().trim();
        if (!nick.isEmpty()) {
            ClientSoundManager.getInstance().playButtonClick();
            Sun.getInstance().getTargetManager().addTarget(nick);
            this.textField.clear();
        }
    }

    @Override
    public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
        float boxX = getBoxX();
        float boxY = getBoxY();
        float boxW = getBoxW();

        float fieldX = boxX + INNER_PAD_X;
        float fieldY = boxY + 19.0F;
        float fieldW = boxW - INNER_PAD_X * 2 - ADD_BTN_W - 4.0F;
        float addBtnX = fieldX + fieldW + 4.0F;

        if (button == MouseButton.LEFT) {
            // Клик по кнопке [+]
            if (GuiUtility.isHovered(addBtnX, fieldY, ADD_BTN_W, FIELD_H, mouseX, mouseY)) {
                addTargetFromField();
                return;
            }

            // Клик по кнопке удаления цели [×]
            float sepY = fieldY + FIELD_H + SEP_GAP;
            float listStartY = sepY + SEP_GAP;
            List<String> targets = Sun.getInstance().getTargetManager().getTarget();
            for (int i = 0; i < targets.size(); i++) {
                float itemY = listStartY + i * ITEM_H;
                float delX = boxX + boxW - INNER_PAD_X - DEL_BTN_SIZE;
                float delY = itemY + (ITEM_H - DEL_BTN_SIZE) / 2.0F;
                if (GuiUtility.isHovered(delX, delY, DEL_BTN_SIZE, DEL_BTN_SIZE, mouseX, mouseY)) {
                    ClientSoundManager.getInstance().playButtonClick();
                    Sun.getInstance().getTargetManager().removeTarget(targets.get(i));
                    return;
                }
            }
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
        if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) && this.textField.isFocused()) {
            addTargetFromField();
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
        List<String> targets = Sun.getInstance().getTargetManager().getTarget();
        float boxH = calculateBoxHeight(targets.size());
        return this.height = boxH + MARGIN_Y * 2;
    }
}
