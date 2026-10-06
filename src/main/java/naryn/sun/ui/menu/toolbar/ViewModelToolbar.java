package naryn.sun.ui.menu.toolbar;

import naryn.sun.framework.base.UIContext;
import naryn.sun.framework.msdf.Font;
import naryn.sun.framework.msdf.Fonts;
import naryn.sun.framework.objects.BorderRadius;
import naryn.sun.framework.objects.MouseButton;
import naryn.sun.systems.localization.Localizator;
import naryn.sun.ui.menu.skin.MenuSkin;
import naryn.sun.utility.animation.base.Animation;
import naryn.sun.utility.animation.base.Easing;
import naryn.sun.utility.colors.ColorRGBA;
import naryn.sun.utility.colors.Colors;
import naryn.sun.utility.gui.GuiUtility;
import naryn.sun.utility.render.RenderUtility;

public final class ViewModelToolbar {

    public static final float WIDTH = 296.0F;
    public static final float HEIGHT = 26.0F;
    public static final float TOP_OFFSET = 10.0F;

    public static final float SUCKER_W = 60.0F;
    public static final float SUCKER_H = 17.0F;
    public static final float SUCKER_R = 8.5F;

    private static final Animation collapseAnim         = new Animation(240L, 0.0F, Easing.FIGMA_EASE_IN_OUT);
    private static final Animation resetBothHoverAnim   = new Animation(180L, 0.0F, Easing.FIGMA_EASE_IN_OUT);
    private static final Animation resetHandHoverAnim   = new Animation(180L, 0.0F, Easing.FIGMA_EASE_IN_OUT);
    private static final Animation doneHoverAnim        = new Animation(180L, 0.0F, Easing.FIGMA_EASE_IN_OUT);

    private static boolean collapsed = false;
    private static boolean suckerCustomPos = false;
    private static float suckerX = 0.0F;
    private static float suckerY = 0.0F;

    private static boolean suckerDragging = false;
    private static boolean suckerMoved = false;
    private static float suckerDragPressX = 0.0F;
    private static float suckerDragPressY = 0.0F;
    private static float suckerDragOffsetStartX = 0.0F;
    private static float suckerDragOffsetStartY = 0.0F;

    private ViewModelToolbar() {
    }

    public static void resetState() {
        collapsed = false;
        collapseAnim.setValue(0.0F);
        suckerCustomPos = false;
        suckerDragging = false;
        suckerMoved = false;
        resetBothHoverAnim.setValue(0.0F);
        resetHandHoverAnim.setValue(0.0F);
        doneHoverAnim.setValue(0.0F);
    }

    public static boolean isCollapsed() {
        return collapsed;
    }

    public static float getCollapseValue() {
        return collapseAnim.getValue();
    }

    public static float getSuckerX(float screenWidth) {
        if (!suckerCustomPos) {
            float tbX = screenWidth / 2.0F - WIDTH / 2.0F;
            return tbX + (WIDTH - SUCKER_W) / 2.0F;
        }
        return suckerX;
    }

    public static float getSuckerY() {
        if (!suckerCustomPos) {
            return TOP_OFFSET + HEIGHT - 2.0F;
        }
        return suckerY;
    }

    public static boolean isSuckerHovered(double mouseX, double mouseY, float screenWidth) {
        float sx = getSuckerX(screenWidth);
        float sy = getSuckerY();
        return GuiUtility.isHovered(sx, sy, SUCKER_W, SUCKER_H, mouseX, mouseY);
    }

    private static float getBtnW() {
        return (WIDTH - 16.0F) / 3.0F;
    }

    public static boolean isResetBothHovered(double mouseX, double mouseY, float screenWidth) {
        if (collapsed || collapseAnim.getValue() > 0.4F) return false;
        float tbX = screenWidth / 2.0F - WIDTH / 2.0F;
        float tbY = TOP_OFFSET;
        float btnW = getBtnW();
        return GuiUtility.isHovered(tbX + 4.0F, tbY + 4.0F, btnW, HEIGHT - 8.0F, mouseX, mouseY);
    }

    public static boolean isResetHandHovered(double mouseX, double mouseY, float screenWidth) {
        if (collapsed || collapseAnim.getValue() > 0.4F) return false;
        float tbX = screenWidth / 2.0F - WIDTH / 2.0F;
        float tbY = TOP_OFFSET;
        float btnW = getBtnW();
        return GuiUtility.isHovered(tbX + 6.0F + btnW, tbY + 4.0F, btnW, HEIGHT - 8.0F, mouseX, mouseY);
    }

    public static boolean isDoneHovered(double mouseX, double mouseY, float screenWidth) {
        if (collapsed || collapseAnim.getValue() > 0.4F) return false;
        float tbX = screenWidth / 2.0F - WIDTH / 2.0F;
        float tbY = TOP_OFFSET;
        float btnW = getBtnW();
        return GuiUtility.isHovered(tbX + 8.0F + btnW * 2.0F, tbY + 4.0F, btnW, HEIGHT - 8.0F, mouseX, mouseY);
    }

    public static boolean onMouseClicked(double mouseX, double mouseY, MouseButton button, float screenWidth) {
        if (button == MouseButton.LEFT) {
            float sx = getSuckerX(screenWidth);
            float sy = getSuckerY();
            if (GuiUtility.isHovered(sx, sy, SUCKER_W, SUCKER_H, mouseX, mouseY)) {
                suckerDragging = true;
                suckerMoved = false;
                suckerDragPressX = (float) mouseX;
                suckerDragPressY = (float) mouseY;
                suckerDragOffsetStartX = (float) mouseX - sx;
                suckerDragOffsetStartY = (float) mouseY - sy;
                return true;
            }
        }
        return false;
    }

    public static void onMouseDragged(double mouseX, double mouseY, float screenWidth, float screenHeight) {
        if (suckerDragging) {
            float dx = (float) mouseX - suckerDragPressX;
            float dy = (float) mouseY - suckerDragPressY;
            if (Math.abs(dx) > 3.0F || Math.abs(dy) > 3.0F) {
                suckerMoved = true;
            }
            if (suckerMoved) {
                suckerCustomPos = true;
                suckerX = Math.max(0.0F, Math.min((float) screenWidth - SUCKER_W, (float) mouseX - suckerDragOffsetStartX));
                suckerY = Math.max(0.0F, Math.min((float) screenHeight - SUCKER_H, (float) mouseY - suckerDragOffsetStartY));
            }
        }
    }

    public static void onMouseReleased(double mouseX, double mouseY, MouseButton button) {
        if (button == MouseButton.LEFT && suckerDragging) {
            if (!suckerMoved) {
                collapsed = !collapsed;
            }
            suckerDragging = false;
        }
    }

    public static void render(UIContext context, float alpha, float screenWidth, float screenHeight) {
        collapseAnim.update(collapsed ? 1.0F : 0.0F);
        float collapseVal = collapseAnim.getValue();
        MenuSkin skin = MenuSkin.current();

        float tbX = screenWidth / 2.0F - WIDTH / 2.0F;
        float tbY = TOP_OFFSET;
        float btnW = getBtnW();
        float btnH = HEIGHT - 8.0F;

        float curSuckerX = getSuckerX(screenWidth);
        float curSuckerY = getSuckerY();

        // 1. Отрисовка основного островка
        if (collapseVal < 1.0F) {
            float islandAlpha = alpha * (1.0F - collapseVal);
            float islandScale = 1.0F - collapseVal * 0.95F;
            float targetCenterX = curSuckerX + SUCKER_W / 2.0F;
            float targetCenterY = curSuckerY + SUCKER_H / 2.0F;
            float defaultCenterX = tbX + WIDTH / 2.0F;
            float defaultCenterY = tbY + HEIGHT / 2.0F;

            float curCenterX = defaultCenterX + (targetCenterX - defaultCenterX) * collapseVal;
            float curCenterY = defaultCenterY + (targetCenterY - defaultCenterY) * collapseVal;

            context.pushMatrix();
            RenderUtility.scale(context.getMatrices(), curCenterX, curCenterY, islandScale);

            // Соединяющий язычок капли (если в исходной позиции)
            if (!suckerCustomPos && collapseVal < 0.2F) {
                float stemW = SUCKER_W - 12.0F;
                float stemX = tbX + (WIDTH - stemW) / 2.0F;
                float stemY = tbY + HEIGHT - 3.0F;
                if (naryn.sun.systems.theme.ClientAppearance.isFrost()) {
                    skin.renderPanelShell(context, stemX, stemY, stemW, 6.0F, BorderRadius.all(2.0F), islandAlpha);
                } else {
                    context.drawRoundedRect(stemX, stemY, stemW, 6.0F, BorderRadius.all(2.0F),
                            new ColorRGBA(15, 15, 27, (int) (235 * islandAlpha)));
                }
            }

            if (naryn.sun.systems.theme.ClientAppearance.isFrost()) {
                skin.renderPanelShell(context, tbX, tbY, WIDTH, HEIGHT, BorderRadius.all(8.0F), islandAlpha);
            } else {
                context.drawBlurredRect(tbX, tbY, WIDTH, HEIGHT, 20.0F, BorderRadius.all(8.0F),
                        new ColorRGBA(151, 71, 255, (int) (40 * islandAlpha)));
                context.drawRoundedRect(tbX, tbY, WIDTH, HEIGHT, BorderRadius.all(8.0F),
                        new ColorRGBA(15, 15, 27, (int) (235 * islandAlpha)));
                context.drawRoundedBorder(tbX, tbY, WIDTH, HEIGHT, 0.5F, BorderRadius.all(8.0F),
                        new ColorRGBA(151, 71, 255, (int) (150 * islandAlpha)));
            }

            float btn1X = tbX + 4.0F;
            float btn2X = tbX + 6.0F + btnW;
            float btn3X = tbX + 8.0F + btnW * 2.0F;
            float btnY  = tbY + 4.0F;

            boolean resetBothHovered = !collapsed && GuiUtility.isHovered(btn1X, btnY, btnW, btnH, context);
            boolean resetHandHovered = !collapsed && GuiUtility.isHovered(btn2X, btnY, btnW, btnH, context);
            boolean doneHovered      = !collapsed && GuiUtility.isHovered(btn3X, btnY, btnW, btnH, context);

            resetBothHoverAnim.update(resetBothHovered ? 1.0F : 0.0F);
            resetHandHoverAnim.update(resetHandHovered ? 1.0F : 0.0F);
            doneHoverAnim.update(doneHovered ? 1.0F : 0.0F);

            Font btnFont = Fonts.MEDIUM.getFont(6.0F);

            // Кнопка 1: "Сбросить обе"
            float rbVal = resetBothHoverAnim.getValue();
            ColorRGBA rbBg = new ColorRGBA(255, 255, 255, (int) (15 * (1.0F - rbVal)))
                    .mix(new ColorRGBA(239, 68, 68, 220), rbVal);
            context.drawRoundedRect(btn1X, btnY, btnW, btnH, BorderRadius.all(6.0F),
                    rbBg.withAlpha((int) (rbBg.getAlpha() * islandAlpha)));
            if (rbVal > 0.05F) {
                context.drawRoundedBorder(btn1X, btnY, btnW, btnH, 0.5F, BorderRadius.all(6.0F),
                        new ColorRGBA(239, 68, 68, (int) (180 * rbVal * islandAlpha)));
            }
            context.drawCenteredText(btnFont, Localizator.translate("view_model.reset_both"),
                    btn1X + btnW / 2.0F, btnY + (btnH - btnFont.height()) / 2.0F + 0.5F,
                    Colors.getTextColor().withAlpha(255.0F * islandAlpha));

            // Кнопка 2: "Сбросить руку"
            float rhVal = resetHandHoverAnim.getValue();
            ColorRGBA rhBg = new ColorRGBA(255, 255, 255, (int) (15 * (1.0F - rhVal)))
                    .mix(new ColorRGBA(239, 68, 68, 220), rhVal);
            context.drawRoundedRect(btn2X, btnY, btnW, btnH, BorderRadius.all(6.0F),
                    rhBg.withAlpha((int) (rhBg.getAlpha() * islandAlpha)));
            if (rhVal > 0.05F) {
                context.drawRoundedBorder(btn2X, btnY, btnW, btnH, 0.5F, BorderRadius.all(6.0F),
                        new ColorRGBA(239, 68, 68, (int) (180 * rhVal * islandAlpha)));
            }
            context.drawCenteredText(btnFont, Localizator.translate("view_model.reset_hand"),
                    btn2X + btnW / 2.0F, btnY + (btnH - btnFont.height()) / 2.0F + 0.5F,
                    Colors.getTextColor().withAlpha(255.0F * islandAlpha));

            // Кнопка 3: "Готово"
            float dVal = doneHoverAnim.getValue();
            ColorRGBA doneBg = new ColorRGBA(151, 71, 255, (int) (180 + 75 * dVal));
            context.drawRoundedRect(btn3X, btnY, btnW, btnH, BorderRadius.all(6.0F),
                    doneBg.withAlpha((int) (doneBg.getAlpha() * islandAlpha)));
            if (dVal > 0.05F) {
                context.drawRoundedBorder(btn3X, btnY, btnW, btnH, 0.5F, BorderRadius.all(6.0F),
                        new ColorRGBA(255, 255, 255, (int) (160 * dVal * islandAlpha)));
            }
            context.drawCenteredText(btnFont, Localizator.translate("view_model.done"),
                    btn3X + btnW / 2.0F, btnY + (btnH - btnFont.height()) / 2.0F + 0.5F,
                    ColorRGBA.WHITE.withAlpha(255.0F * islandAlpha));

            context.popMatrix();
        }

        // 2. Отрисовка сворачивающего островка (капли)
        if (naryn.sun.systems.theme.ClientAppearance.isFrost()) {
            skin.renderPanelShell(context, curSuckerX, curSuckerY, SUCKER_W, SUCKER_H, BorderRadius.all(SUCKER_R), alpha);
        } else {
            context.drawShadow(curSuckerX, curSuckerY, SUCKER_W, SUCKER_H, 10.0F, BorderRadius.all(SUCKER_R),
                    ColorRGBA.BLACK.withAlpha(80.0F * alpha));
            context.drawBlurredRect(curSuckerX, curSuckerY, SUCKER_W, SUCKER_H, 16.0F, BorderRadius.all(SUCKER_R),
                    new ColorRGBA(151, 71, 255, (int) (35 * alpha)));
            context.drawRoundedRect(curSuckerX, curSuckerY, SUCKER_W, SUCKER_H, BorderRadius.all(SUCKER_R),
                    new ColorRGBA(15, 15, 27, (int) (240 * alpha)));
            context.drawRoundedBorder(curSuckerX, curSuckerY, SUCKER_W, SUCKER_H, 0.5F, BorderRadius.all(SUCKER_R),
                    new ColorRGBA(151, 71, 255, (int) (130 * alpha)));
        }

        // Мини-индикатор активности внутри капли
        float dotR = 2.5F;
        float dotX = curSuckerX + 8.0F;
        float dotY = curSuckerY + SUCKER_H / 2.0F;
        ColorRGBA dotColor = collapsed
                ? new ColorRGBA(34, 197, 94, (int) (240 * alpha))
                : Colors.ACCENT.withAlpha((int) (240 * alpha));
        context.drawRoundedRect(dotX - dotR, dotY - dotR, dotR * 2.0F, dotR * 2.0F, BorderRadius.all(dotR), dotColor);

        Font suckerFont = Fonts.MEDIUM.getFont(5.5F);
        String suckerLabel = collapsed
                ? Localizator.translate("menu.editor.show")
                : Localizator.translate("menu.editor.hide");

        context.drawText(suckerFont, suckerLabel,
                curSuckerX + 17.0F,
                curSuckerY + (SUCKER_H - suckerFont.height()) / 2.0F + 0.5F,
                Colors.getTextColor().withAlpha(255.0F * alpha));

        // 3. Подсказка по управлению внизу экрана (сворачивается вместе с тулбаром)
        float hintAlpha = alpha * (1.0F - collapseVal);
        if (hintAlpha > 0.02F) {
            renderControlsHint(context, hintAlpha, screenWidth, screenHeight);
        }
    }

    private static void renderControlsHint(UIContext context, float alpha, float screenWidth, float screenHeight) {
        Font hintFont = Fonts.REGULAR.getFont(5.5F);
        String hintText = Localizator.translate("view_model.hint");

        float badgeW = hintFont.width(hintText) + 24.0F;
        float badgeH = 17.0F;
        float badgeX = screenWidth / 2.0F - badgeW / 2.0F;
        float badgeY = screenHeight - badgeH - 12.0F;

        if (naryn.sun.systems.theme.ClientAppearance.isFrost()) {
            MenuSkin.current().renderPanelShell(context, badgeX, badgeY, badgeW, badgeH, BorderRadius.all(8.5F), alpha);
        } else {
            context.drawBlurredRect(badgeX, badgeY, badgeW, badgeH, 15.0F, BorderRadius.all(8.5F),
                    new ColorRGBA(0, 0, 0, (int) (60 * alpha)));
            context.drawRoundedRect(badgeX, badgeY, badgeW, badgeH, BorderRadius.all(8.5F),
                    new ColorRGBA(10, 12, 20, (int) (210 * alpha)));
            context.drawRoundedBorder(badgeX, badgeY, badgeW, badgeH, 0.5F, BorderRadius.all(8.5F),
                    new ColorRGBA(151, 71, 255, (int) (90 * alpha)));
        }

        float textX = badgeX + (badgeW - hintFont.width(hintText)) / 2.0F;
        float textY = badgeY + (badgeH - hintFont.height()) / 2.0F + 0.5F;

        context.drawText(hintFont, hintText, textX, textY,
                new ColorRGBA(220, 225, 240, (int) (250 * alpha)));
    }
}
