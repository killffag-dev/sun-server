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
import naryn.sun.utility.game.cursor.CursorType;
import naryn.sun.utility.game.cursor.CursorUtility;
import naryn.sun.utility.gui.GuiUtility;
import naryn.sun.utility.render.RenderUtility;

/**
 * Плавающий тулбар "Reset/Done" со встроенной капсулой-«каплей» (Скрыть / Показать)
 * и подсказкой по зажатию клавиши Shift для магнитной сетки.
 *
 * Полностью оформлен в стиле активной темы (Dark / Frost Glass) и физически
 * соединяется с островком в форме плавной капли/присоски.
 */
public final class EditorToolbar {

    public static final float WIDTH = 220.0F;
    public static final float HEIGHT = 26.0F;
    public static final float TOP_OFFSET = 10.0F;

    public static final float SUCKER_W = 60.0F;
    public static final float SUCKER_H = 17.0F;
    public static final float SUCKER_R = 8.5F;

    private static final Animation collapseAnim   = new Animation(240L, 0.0F, Easing.FIGMA_EASE_IN_OUT);
    private static final Animation resetHoverAnim = new Animation(180L, 0.0F, Easing.FIGMA_EASE_IN_OUT);
    private static final Animation doneHoverAnim  = new Animation(180L, 0.0F, Easing.FIGMA_EASE_IN_OUT);

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

    private EditorToolbar() {
    }

    public static void resetState() {
        collapsed = false;
        collapseAnim.setValue(0.0F);
        suckerCustomPos = false;
        suckerDragging = false;
        suckerMoved = false;
        resetHoverAnim.setValue(0.0F);
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

    public static boolean isResetHovered(double mouseX, double mouseY, float screenWidth) {
        if (collapsed || collapseAnim.getValue() > 0.4F) return false;
        float tbX = screenWidth / 2.0F - WIDTH / 2.0F;
        float tbY = TOP_OFFSET;
        float btnW = (WIDTH - 12.0F) / 2.0F;
        return GuiUtility.isHovered(tbX + 4.0F, tbY + 4.0F, btnW, HEIGHT - 8.0F, mouseX, mouseY);
    }

    public static boolean isDoneHovered(double mouseX, double mouseY, float screenWidth) {
        if (collapsed || collapseAnim.getValue() > 0.4F) return false;
        float tbX = screenWidth / 2.0F - WIDTH / 2.0F;
        float tbY = TOP_OFFSET;
        float btnW = (WIDTH - 12.0F) / 2.0F;
        return GuiUtility.isHovered(tbX + 8.0F + btnW, tbY + 4.0F, btnW, HEIGHT - 8.0F, mouseX, mouseY);
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
                // Клик без перемещения — переключаем состояние
                collapsed = !collapsed;
            }
            suckerDragging = false;
        }
    }

    public static void render(UIContext context, float alpha, Font labelFont, float screenWidth, float screenHeight) {
        collapseAnim.update(collapsed ? 1.0F : 0.0F);
        float collapseVal = collapseAnim.getValue();
        MenuSkin skin = MenuSkin.current();

        float tbX = screenWidth / 2.0F - WIDTH / 2.0F;
        float tbY = TOP_OFFSET;
        float btnW = (WIDTH - 12.0F) / 2.0F;

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

            boolean resetHovered = !collapsed && GuiUtility.isHovered(tbX + 4.0F, tbY + 4.0F, btnW, HEIGHT - 8.0F, context);
            boolean doneHovered  = !collapsed && GuiUtility.isHovered(tbX + 8.0F + btnW, tbY + 4.0F, btnW, HEIGHT - 8.0F, context);

            resetHoverAnim.update(resetHovered ? 1.0F : 0.0F);
            doneHoverAnim.update(doneHovered ? 1.0F : 0.0F);

            // Кнопка Сбросить (заливается в красный при наведении)
            float rVal = resetHoverAnim.getValue();
            ColorRGBA resetBg = new ColorRGBA(255, 255, 255, (int) (15 * (1.0F - rVal)))
                    .mix(new ColorRGBA(239, 68, 68, 220), rVal);
            context.drawRoundedRect(tbX + 4.0F, tbY + 4.0F, btnW, HEIGHT - 8.0F, BorderRadius.all(6.0F),
                    resetBg.withAlpha((int) (resetBg.getAlpha() * islandAlpha)));

            if (rVal > 0.05F) {
                context.drawRoundedBorder(tbX + 4.0F, tbY + 4.0F, btnW, HEIGHT - 8.0F, 0.5F, BorderRadius.all(6.0F),
                        new ColorRGBA(239, 68, 68, (int) (180 * rVal * islandAlpha)));
            }

            String resetLabel = Localizator.translate("menu.toolbar.reset");
            context.drawText(labelFont, resetLabel,
                    tbX + 4.0F + (btnW - labelFont.width(resetLabel)) / 2.0F,
                    tbY + (HEIGHT - labelFont.height()) / 2.0F + 0.5F,
                    Colors.getTextColor().withAlpha(255.0F * islandAlpha));

            // Кнопка Готово (заливается в зелёный при наведении)
            float dVal = doneHoverAnim.getValue();
            ColorRGBA doneBg = new ColorRGBA(151, 71, 255, 120)
                    .mix(new ColorRGBA(34, 197, 94, 220), dVal);
            context.drawRoundedRect(tbX + 8.0F + btnW, tbY + 4.0F, btnW, HEIGHT - 8.0F, BorderRadius.all(6.0F),
                    doneBg.withAlpha((int) (doneBg.getAlpha() * islandAlpha)));

            if (dVal > 0.05F) {
                context.drawRoundedBorder(tbX + 8.0F + btnW, tbY + 4.0F, btnW, HEIGHT - 8.0F, 0.5F, BorderRadius.all(6.0F),
                        new ColorRGBA(34, 197, 94, (int) (180 * dVal * islandAlpha)));
            }

            String doneLabel = Localizator.translate("menu.toolbar.done");
            context.drawText(labelFont, doneLabel,
                    tbX + 8.0F + btnW + (btnW - labelFont.width(doneLabel)) / 2.0F,
                    tbY + (HEIGHT - labelFont.height()) / 2.0F + 0.5F,
                    Colors.getTextColor().withAlpha(255.0F * islandAlpha));

            if (resetHovered || doneHovered) {
                CursorUtility.set(CursorType.HAND);
            }

            RenderUtility.end(context.getMatrices());
            context.popMatrix();
        }

        // 2. Кнопка-капля (Скрыть / Показать) — оформлена в теме меню
        boolean suckerHovered = GuiUtility.isHovered(curSuckerX, curSuckerY, SUCKER_W, SUCKER_H, context);
        if (suckerHovered || suckerDragging) {
            CursorUtility.set(CursorType.HAND);
        }

        if (naryn.sun.systems.theme.ClientAppearance.isFrost()) {
            skin.renderPanelShell(context, curSuckerX, curSuckerY, SUCKER_W, SUCKER_H, BorderRadius.all(SUCKER_R), alpha);
        } else {
            context.drawBlurredRect(curSuckerX, curSuckerY, SUCKER_W, SUCKER_H, 15.0F, BorderRadius.all(SUCKER_R),
                    new ColorRGBA(151, 71, 255, (int) (35 * alpha)));
            context.drawRoundedRect(curSuckerX, curSuckerY, SUCKER_W, SUCKER_H, BorderRadius.all(SUCKER_R),
                    new ColorRGBA(13, 15, 25, (int) ((suckerHovered || suckerDragging ? 245 : 225) * alpha)));
            context.drawRoundedBorder(curSuckerX, curSuckerY, SUCKER_W, SUCKER_H, 0.5F, BorderRadius.all(SUCKER_R),
                    new ColorRGBA(151, 71, 255, (int) ((suckerHovered || suckerDragging ? 220 : 130) * alpha)));
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

        // 3. Подсказка по клавише Shift (сворачивается вместе с тулбаром)
        float hintAlpha = alpha * (1.0F - collapseVal);
        if (hintAlpha > 0.02F) {
            renderShiftHint(context, hintAlpha, screenWidth, screenHeight);
        }
    }

    private static void renderShiftHint(UIContext context, float alpha, float screenWidth, float screenHeight) {
        Font hintFont = Fonts.REGULAR.getFont(6.0F);

        String hintText = Localizator.translate("menu.editor.shift_hint");

        float badgeW = hintFont.width(hintText) + 20.0F;
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
                    new ColorRGBA(255, 255, 255, (int) (18 * alpha)));
        }

        float textX = badgeX + (badgeW - hintFont.width(hintText)) / 2.0F;
        float textY = badgeY + (badgeH - hintFont.height()) / 2.0F + 0.5F;

        context.drawText(hintFont, hintText, textX, textY,
                new ColorRGBA(148, 163, 184, (int) (240 * alpha)));
    }
}