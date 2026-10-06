package naryn.sun.ui.menu.layout;

import naryn.sun.framework.base.UIContext;
import naryn.sun.utility.colors.ColorRGBA;
import net.minecraft.client.gui.screen.Screen;

import java.util.ArrayList;
import java.util.List;

/**
 * Умная магнитная сетка для редакторов HUD (LayoutEditor и HudModuleEditor).
 * Срабатывает в радиусе 6px при перетаскивании элементов:
 * - к безопасным краям экрана (16px)
 * - к центрам экрана по X и Y
 * - к граням (лево, центр, право, верх, середина, низ) всех остальных элементов.
 *
 * При зажатом Shift магнитная привязка отключается (свободное позиционирование).
 */
public final class SnapGuide {

    public static final float SNAP_THRESHOLD = 6.0F;
    public static final float SAFE_PADDING   = 16.0F;

    private SnapGuide() {
    }

    public static class SnapResult {
        public final float x;
        public final float y;
        public final Float guideX;
        public final Float guideY;

        public SnapResult(float x, float y, Float guideX, Float guideY) {
            this.x = x;
            this.y = y;
            this.guideX = guideX;
            this.guideY = guideY;
        }
    }

    public static class Bounds {
        public final float x;
        public final float y;
        public final float width;
        public final float height;

        public Bounds(float x, float y, float width, float height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }

    public static boolean isShiftDown() {
        return Screen.hasShiftDown();
    }

    public static SnapResult calculateSnap(float currentX, float currentY, float width, float height,
                                           float screenW, float screenH, List<Bounds> otherElements) {
        if (isShiftDown()) {
            return new SnapResult(currentX, currentY, null, null);
        }

        float dragLeft   = currentX;
        float dragCenter = currentX + width / 2.0F;
        float dragRight  = currentX + width;
        float dragTop    = currentY;
        float dragMiddle = currentY + height / 2.0F;
        float dragBottom = currentY + height;

        List<Float> targetsX = new ArrayList<>();
        targetsX.add(SAFE_PADDING);
        targetsX.add(screenW / 2.0F);
        targetsX.add(screenW - SAFE_PADDING);

        List<Float> targetsY = new ArrayList<>();
        targetsY.add(SAFE_PADDING);
        targetsY.add(screenH / 2.0F);
        targetsY.add(screenH - SAFE_PADDING);

        if (otherElements != null) {
            for (Bounds b : otherElements) {
                targetsX.add(b.x);
                targetsX.add(b.x + b.width / 2.0F);
                targetsX.add(b.x + b.width);

                targetsY.add(b.y);
                targetsY.add(b.y + b.height / 2.0F);
                targetsY.add(b.y + b.height);
            }
        }

        float snappedX = currentX;
        float snappedY = currentY;
        Float guideX = null;
        Float guideY = null;

        float minDiffX = SNAP_THRESHOLD;
        float minDiffY = SNAP_THRESHOLD;

        for (float tx : targetsX) {
            // 1. Левый край
            float dLeft = Math.abs(dragLeft - tx);
            if (dLeft < minDiffX) {
                minDiffX = dLeft;
                snappedX = tx;
                guideX = tx;
            }
            // 2. Центр
            float dCenter = Math.abs(dragCenter - tx);
            if (dCenter < minDiffX) {
                minDiffX = dCenter;
                snappedX = tx - width / 2.0F;
                guideX = tx;
            }
            // 3. Правый край
            float dRight = Math.abs(dragRight - tx);
            if (dRight < minDiffX) {
                minDiffX = dRight;
                snappedX = tx - width;
                guideX = tx;
            }
        }

        for (float ty : targetsY) {
            // 1. Верхний край
            float dTop = Math.abs(dragTop - ty);
            if (dTop < minDiffY) {
                minDiffY = dTop;
                snappedY = ty;
                guideY = ty;
            }
            // 2. Середина
            float dMiddle = Math.abs(dragMiddle - ty);
            if (dMiddle < minDiffY) {
                minDiffY = dMiddle;
                snappedY = ty - height / 2.0F;
                guideY = ty;
            }
            // 3. Нижний край
            float dBottom = Math.abs(dragBottom - ty);
            if (dBottom < minDiffY) {
                minDiffY = dBottom;
                snappedY = ty - height;
                guideY = ty;
            }
        }

        return new SnapResult(snappedX, snappedY, guideX, guideY);
    }

    public static void renderGuides(UIContext context, Float guideX, Float guideY, float screenW, float screenH, float alpha) {
        if (guideX == null && guideY == null) return;

        ColorRGBA lineCol = new ColorRGBA(56, 189, 248, (int) (230 * alpha));
        ColorRGBA glowCol = new ColorRGBA(56, 189, 248, (int) (65 * alpha));
        ColorRGBA softCol = new ColorRGBA(56, 189, 248, (int) (25 * alpha));

        if (guideX != null) {
            context.drawRect(guideX - 2.5F, 0.0F, 5.0F, screenH, softCol);
            context.drawRect(guideX - 1.0F, 0.0F, 2.0F, screenH, glowCol);
            context.drawRect(guideX - 0.5F, 0.0F, 1.0F, screenH, lineCol);
        }
        if (guideY != null) {
            context.drawRect(0.0F, guideY - 2.5F, screenW, 5.0F, softCol);
            context.drawRect(0.0F, guideY - 1.0F, screenW, 2.0F, glowCol);
            context.drawRect(0.0F, guideY - 0.5F, screenW, 1.0F, lineCol);
        }
    }
}
