package naryn.sun.ui.menu.panels;

import naryn.sun.framework.base.UIContext;
import naryn.sun.framework.msdf.Fonts;
import naryn.sun.framework.objects.BorderRadius;
import naryn.sun.ui.menu.layout.MenuLayoutData;
import naryn.sun.utility.colors.ColorRGBA;
import naryn.sun.utility.colors.Colors;
import naryn.sun.utility.gui.TextFitUtility;
import naryn.sun.utility.render.ScissorUtility;

/**
 * Позиционируемый бокс описания модуля (тултип) — позиция/масштаб
 * настраиваются в LayoutEditor (режим GUI Settings), как окно и нижняя
 * панель. Реализует "рулетку" смены текста (старый уезжает вверх, новый
 * заезжает снизу).
 *
 * Только рендер, состояние (tooltipAnim/tooltipSwitchAnim/tooltipShownText/
 * tooltipPrevText) по-прежнему хранится и обновляется в NewScreen — сюда
 * передаются уже готовые значения.
 */
public final class TooltipBoxPanel {

    private TooltipBoxPanel() {
    }

    public static void render(UIContext context, float alpha, float tooltipAlpha, boolean layoutEditMode,
                               MenuLayoutData layout, float switchProgress,
                               String shownText, String prevText) {
        float ta = tooltipAlpha * alpha;
        if (ta <= 0.0F) {
            return;
        }

        float boxX = layout.tooltipX;
        float boxY = layout.tooltipY;
        float boxW = MenuLayoutData.BASE_TOOLTIP_WIDTH * layout.tooltipScale;
        float boxH = MenuLayoutData.BASE_TOOLTIP_HEIGHT * layout.tooltipScale;

        // Фон/рамка видны только во время редактирования GUI Settings —
        // именно тогда его двигают/ресайзят. В обычном режиме бокс
        // полностью невидим, виден только текст внутри него.
        if (layoutEditMode) {
            naryn.sun.ui.menu.skin.MenuSkin.current().renderSettingBox(context, boxX, boxY, boxW, boxH, BorderRadius.all(6.0F), ta);
        }

        float pad = 6.0F * layout.tooltipScale;
        float innerW = Math.max(1.0F, boxW - pad * 2.0F);
        float innerH = Math.max(1.0F, boxH - pad * 2.0F);
        float lineGap = 2.0F * layout.tooltipScale;

        // Скиссор держим ВСЕГДА, даже когда бокс невидим — иначе на
        // "рулетке" старый/новый текст будет на мгновение вылезать за
        // пределы области.
        ScissorUtility.push(context.getMatrices(), boxX, boxY, boxW, boxH);

        boolean smooth = naryn.sun.systems.animation.ClientAnimationConfig.getInstance().isSmoothTooltip();
        if (smooth) {
            if (switchProgress < 1.0F && !prevText.isEmpty()) {
                drawLines(context, prevText, boxX, boxY, pad, innerW, innerH, lineGap, 0.0F, ta * (1.0F - switchProgress), layout.tooltipScale);
            }
            if (!shownText.isEmpty()) {
                drawLines(context, shownText, boxX, boxY, pad, innerW, innerH, lineGap, 0.0F, ta * switchProgress, layout.tooltipScale);
            }
        } else {
            if (!shownText.isEmpty()) {
                drawLines(context, shownText, boxX, boxY, pad, innerW, innerH, lineGap, 0.0F, ta, layout.tooltipScale);
            }
        }

        ScissorUtility.pop();
    }

    private static void drawLines(UIContext context, String text, float boxX, float boxY,
                                   float pad, float innerW, float innerH, float lineGap,
                                   float offsetY, float alphaMul, float tooltipScale) {
        if (alphaMul <= 0.0F || text.isEmpty()) {
            return;
        }

        TextFitUtility.Result fit = TextFitUtility.fit(
                size -> Fonts.REGULAR.getFont(size),
                text, innerW, innerH,
                8.5F * tooltipScale, 5.0F * tooltipScale, lineGap);

        float ty = boxY + pad + offsetY;
        for (String line : fit.lines()) {
            // Каждая строка центрируется по ширине внутренней области.
            float lineW = fit.font().width(line);
            float lineX = boxX + pad + Math.max(0.0F, (innerW - lineW) / 2.0F);
            context.drawText(fit.font(), line, lineX, ty, Colors.getTextColor().withAlpha((int) (200 * alphaMul)));
            ty += fit.font().height() + lineGap;
        }
    }
}
