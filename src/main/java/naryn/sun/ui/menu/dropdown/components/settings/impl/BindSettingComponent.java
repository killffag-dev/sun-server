package naryn.sun.ui.menu.dropdown.components.settings.impl;

import lombok.Generated;
import naryn.sun.framework.base.CustomComponent;
import naryn.sun.framework.base.UIContext;
import naryn.sun.framework.msdf.Font;
import naryn.sun.framework.msdf.Fonts;
import naryn.sun.framework.objects.BorderRadius;
import naryn.sun.framework.objects.MouseButton;
import naryn.sun.systems.localization.Localizator;
import naryn.sun.systems.setting.settings.BindSetting;
import naryn.sun.ui.menu.dropdown.components.settings.MenuSettingComponent;
import naryn.sun.ui.menu.skin.MenuSkin;
import naryn.sun.utility.animation.base.Animation;
import naryn.sun.utility.animation.base.Easing;
import naryn.sun.utility.animation.types.ColorAnimation;
import naryn.sun.utility.colors.ColorRGBA;
import naryn.sun.utility.colors.Colors;
import naryn.sun.utility.game.TextUtility;
import naryn.sun.utility.game.cursor.CursorType;
import naryn.sun.utility.game.cursor.CursorUtility;
import naryn.sun.utility.gui.GuiUtility;
import naryn.sun.utility.gui.TextWrapUtility;
import naryn.sun.utility.render.ScissorUtility;

import java.util.List;

public class BindSettingComponent extends MenuSettingComponent<BindSetting> {
    private final ColorAnimation bindColorAnimation = new ColorAnimation(300L, new ColorRGBA(24.0F, 24.0F, 27.0F), Easing.FIGMA_EASE_IN_OUT);
    private final Animation widthAnimation = new Animation(300L, Easing.FIGMA_EASE_IN_OUT);
    private Animation changeAnimation = new Animation(300L, 1.0F, Easing.FIGMA_EASE_IN_OUT);
    private int prevKey;
    private boolean bindingMode;

    private static final float LEFT_PADDING = 10.0F;
    private static final float LINE_GAP     = 2.0F;
    private static final float BASE_HEIGHT  = 19.0F;

    public BindSettingComponent(BindSetting setting, CustomComponent parent) {
        super(setting, parent);
    }

    public boolean isBindingMode() {
        return this.bindingMode;
    }

    // Ширина резервируется под таблетку бинда на КАЖДОЙ строке — так название
    // никогда не наедет на неё, независимо от количества строк.
    private List<String> getNameLines(Font nameFont) {
        float maxWidth = this.width - this.widthAnimation.getValue() - 20.0F;
        if (maxWidth <= 0.0F) maxWidth = 40.0F;
        return TextWrapUtility.wrap(nameFont, Localizator.translate(this.setting.getName()), maxWidth);
    }

    private float getExtra(Font nameFont) {
        int lines = Math.max(1, getNameLines(nameFont).size());
        return (lines - 1) * (nameFont.height() + LINE_GAP);
    }

    @Override
    protected void renderComponent(UIContext context) {
        Font nameFont = Fonts.REGULAR.getFont(8.0F);
        Font keyFont = Fonts.REGULAR.getFont(7.0F);
        float leftPadding = LEFT_PADDING;

        this.bindColorAnimation.update(this.bindingMode ? Colors.ACCENT : Colors.getTextColor());
        this.changeAnimation.setDuration(500L);
        this.changeAnimation.update(1.0F);

        String bindLabel = this.bindingMode
            ? Localizator.translate("menu.module.keybind.press")
            : (this.setting.getKey() == -1 ? Localizator.translate("menu.module.keybind.none") : TextUtility.getKeyName(this.setting.getKey()));

        float targetWidth = keyFont.width(bindLabel) + 12.0F;
        this.widthAnimation.update(targetWidth);

        float extra = getExtra(nameFont);
        float headerHeight = BASE_HEIGHT + extra;
        float pillW = this.widthAnimation.getValue();
        float pillH = 13.0F;
        float pillX = this.x + this.width - 9.0F - pillW;
        float pillY = this.y + (headerHeight - pillH) / 2.0F;

        MenuSkin.current().renderBindPill(
            context,
            pillX,
            pillY,
            pillW,
            pillH,
            BorderRadius.all(3.5F),
            this.bindingMode,
            1.0F
        );

        ColorRGBA textColor = this.bindingMode
            ? Colors.ACCENT.mix(ColorRGBA.WHITE, 0.3F)
            : Colors.getTextColor().withAlpha(255.0F * (0.8F + 0.2F * this.hoverAnimation.getValue()));
        float textX = pillX + (pillW - keyFont.width(bindLabel)) / 2.0F;
        float textY = pillY + (pillH - keyFont.height()) / 2.0F;
        context.drawText(keyFont, bindLabel, textX, textY, textColor);

        // Название — переносится на несколько строк вместо затухания
        List<String> lines = getNameLines(nameFont);
        float totalTextHeight = lines.size() * nameFont.height() + Math.max(0, lines.size() - 1) * LINE_GAP;
        float lineY = this.y + GuiUtility.getMiddleOfBox(totalTextHeight, headerHeight);
        ColorRGBA nameColor = Colors.getTextColor().withAlpha(255.0F * (0.75F + 0.25F * this.hoverAnimation.getValue()));
        for (String line : lines) {
            context.drawText(nameFont, line, this.x + leftPadding, lineY, nameColor);
            lineY += nameFont.height() + LINE_GAP;
        }

        if (this.isHovered(context)) {
            CursorUtility.set(CursorType.HAND);
        }
    }

    @Override
    public void drawSplit(UIContext context) {
        float separatorHeight = 0.5F;
        context.drawRect(this.x, this.y + this.height, this.width, separatorHeight, Colors.getTextColor().withAlpha(5.1F));
    }

    @Override
    public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
        float pillW = this.widthAnimation.getValue();
        float pillH = 13.0F;
        float headerHeight = this.getHeight();
        float pillX = this.x + this.width - 9.0F - pillW;
        float pillY = this.y + (headerHeight - pillH) / 2.0F;
        boolean pillHovered = GuiUtility.isHovered(pillX, pillY, pillW, pillH, mouseX, mouseY);
        boolean rowHovered = this.isHovered(mouseX, mouseY);

        if (button == MouseButton.LEFT) {
            if (pillHovered || (rowHovered && !this.bindingMode)) {
                this.bindingMode = !this.bindingMode;
                return;
            } else if (this.bindingMode) {
                this.bindingMode = false;
                return;
            }
        }

        if (this.bindingMode && button != MouseButton.LEFT) {
            this.prevKey = this.setting.getKey();
            this.setting.setKey(button.getButtonIndex());
            this.changeAnimation = new Animation(500L, 0.0F, Easing.FIGMA_EASE_IN_OUT);
            this.bindingMode = false;
            return;
        }

        super.onMouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void onKeyPressed(int keyCode, int scanCode, int modifiers) {
        if (!this.bindingMode) {
            super.onKeyPressed(keyCode, scanCode, modifiers);
            return;
        }

        // Esc (256) — сбросить бинд
        if (keyCode == 256 || keyCode == 261) {
            this.prevKey = this.setting.getKey();
            this.setting.setKey(-1);
            this.changeAnimation = new Animation(500L, 0.0F, Easing.FIGMA_EASE_IN_OUT);
            this.bindingMode = false;
            return;
        }

        this.prevKey = this.setting.getKey();
        this.setting.setKey(keyCode);
        this.changeAnimation = new Animation(500L, 0.0F, Easing.FIGMA_EASE_IN_OUT);
        this.bindingMode = false;
    }

    @Override
    public float getHeight() {
        Font nameFont = Fonts.REGULAR.getFont(8.0F);
        return this.height = BASE_HEIGHT + getExtra(nameFont);
    }

    @Generated
    public void setBindingMode(boolean bindingMode) {
        this.bindingMode = bindingMode;
    }
}