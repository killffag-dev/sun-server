package naryn.sun.ui.menu.layout;

import java.util.ArrayList;
import java.util.List;
import naryn.sun.framework.base.UIContext;
import naryn.sun.framework.objects.BorderRadius;
import naryn.sun.framework.objects.MouseButton;
import naryn.sun.systems.modules.impl.PositionableHudModule;
import naryn.sun.utility.colors.ColorRGBA;
import naryn.sun.utility.game.cursor.CursorType;
import naryn.sun.utility.game.cursor.CursorUtility;
import naryn.sun.utility.gui.GuiUtility;
import naryn.sun.utility.animation.base.Animation;
import naryn.sun.utility.animation.base.Easing;

public class HudModuleEditor {

   private enum Mode { NONE, MOVE, RESIZE }

   private static final float HANDLE_SIZE = 6.0F;
   private static final float MIN_SCALE = 0.5F;
   private static final float MAX_SCALE = 2.5F;
   private static final float GUIDE_DOT_SPACING = 16.0F;
   private static final float GUIDE_DOT_SIZE = 2.0F;

   private final List<PositionableHudModule> targets;

   private PositionableHudModule dragTarget;
   private Mode dragMode = Mode.NONE;
   private float dragStartMouseX;
   private float dragStartMouseY;
   private float dragStartBoundsX;
   private float dragStartBoundsY;
   private float dragStartScale;
   private float dragStartDist;

   private Float activeGuideX = null;
   private Float activeGuideY = null;
   private final Animation guideDotsAnim = new Animation(150L, 0.0F, Easing.FIGMA_EASE_IN_OUT);

   public HudModuleEditor(List<PositionableHudModule> targets) {
      this.targets = targets;
   }

   public boolean onMouseClicked(double mouseX, double mouseY, MouseButton button) {
      if (button != MouseButton.LEFT) {
         return false;
      }

      for (PositionableHudModule module : this.targets) {
         float x = module.getLastBoundsX();
         float y = module.getLastBoundsY();
         float w = module.getLastBoundsWidth();
         float h = module.getLastBoundsHeight();

         if (this.isNear((float) mouseX, (float) mouseY, x + w, y + h, HANDLE_SIZE)) {
            this.dragTarget = module;
            this.dragMode = Mode.RESIZE;
            this.dragStartMouseX = (float) mouseX;
            this.dragStartMouseY = (float) mouseY;
            this.dragStartBoundsX = x;
            this.dragStartBoundsY = y;
            this.dragStartScale = module.getLayoutScale();
            this.dragStartDist = this.distance(x, y, x + w, y + h);
            return true;
         }
      }

      for (PositionableHudModule module : this.targets) {
         float x = module.getLastBoundsX();
         float y = module.getLastBoundsY();
         float w = module.getLastBoundsWidth();
         float h = module.getLastBoundsHeight();

         if (GuiUtility.isHovered((double) x, (double) y, (double) w, (double) h, mouseX, mouseY)) {
            this.dragTarget = module;
            this.dragMode = Mode.MOVE;
            this.dragStartMouseX = (float) mouseX;
            this.dragStartMouseY = (float) mouseY;
            this.dragStartBoundsX = x;
            this.dragStartBoundsY = y;
            return true;
         }
      }

      return false;
   }

   private boolean isNear(float mouseX, float mouseY, float px, float py, float radius) {
      return Math.abs(mouseX - px) <= radius && Math.abs(mouseY - py) <= radius;
   }

   private float distance(float x1, float y1, float x2, float y2) {
      float dx = x2 - x1;
      float dy = y2 - y1;
      return (float) Math.sqrt(dx * dx + dy * dy);
   }

   public void onMouseDragged(double mouseX, double mouseY, float screenW, float screenH) {
      if (this.dragTarget == null) {
         return;
      }

      PositionableHudModule module = this.dragTarget;

      if (this.dragMode == Mode.MOVE) {
         float deltaX = (float) mouseX - this.dragStartMouseX;
         float deltaY = (float) mouseY - this.dragStartMouseY;

         float curW = module.getLastBoundsWidth();
         float curH = module.getLastBoundsHeight();

         float rawVisualX = this.dragStartBoundsX + deltaX;
         float rawVisualY = this.dragStartBoundsY + deltaY;

         List<SnapGuide.Bounds> others = new ArrayList<>();
         for (PositionableHudModule target : this.targets) {
            if (target == module) continue;
            others.add(new SnapGuide.Bounds(target.getLastBoundsX(), target.getLastBoundsY(),
                    target.getLastBoundsWidth(), target.getLastBoundsHeight()));
         }

         SnapGuide.SnapResult snap = SnapGuide.calculateSnap(rawVisualX, rawVisualY, curW, curH, screenW, screenH, others);
         this.activeGuideX = snap.guideX;
         this.activeGuideY = snap.guideY;

         float snappedVisualX = Math.max(0.0F, Math.min(screenW - curW, snap.x));
         float snappedVisualY = Math.max(0.0F, Math.min(screenH - curH, snap.y));

         float unscaledW = module.getLayoutScale() > 0.001F ? curW / module.getLayoutScale() : curW;
         float unscaledH = module.getLayoutScale() > 0.001F ? curH / module.getLayoutScale() : curH;
         float newAnchorX = snappedVisualX - (unscaledW / 2.0F) * (1.0F - module.getLayoutScale());
         float newAnchorY = snappedVisualY - (unscaledH / 2.0F) * (1.0F - module.getLayoutScale());

         module.setEditorPos(newAnchorX, newAnchorY);
      } else if (this.dragMode == Mode.RESIZE) {
         this.activeGuideX = null;
         this.activeGuideY = null;

         float curDist = this.distance(this.dragStartBoundsX, this.dragStartBoundsY, (float) mouseX, (float) mouseY);
         float ratio = this.dragStartDist > 0.001F ? curDist / this.dragStartDist : 1.0F;
         float newScale = this.dragStartScale * ratio;

         newScale = Math.max(newScale, MIN_SCALE);
         newScale = Math.min(newScale, MAX_SCALE);

         module.setEditorScale(newScale);
      }
   }

   public void onMouseReleased(double mouseX, double mouseY, MouseButton button) {
      if (button == MouseButton.LEFT) {
         this.dragTarget = null;
         this.dragMode = Mode.NONE;
         this.activeGuideX = null;
         this.activeGuideY = null;
      }
   }

   public boolean isDragging() {
      return this.dragTarget != null;
   }

   private float clamp(float value, float min, float max) {
      if (max < min) {
         return min;
      }
      return Math.max(min, Math.min(max, value));
   }

   public void validate(float screenW, float screenH) {
      for (PositionableHudModule module : this.targets) {
         float w = module.getLastBoundsWidth();
         float h = module.getLastBoundsHeight();
         if (w <= 0.0F || h <= 0.0F) {
            continue;
         }

         float x = this.clamp(module.getLastAnchorX(), 0.0F, Math.max(0.0F, screenW - w));
         float y = this.clamp(module.getLastAnchorY(), 0.0F, Math.max(0.0F, screenH - h));
         module.setEditorPos(x, y);
      }
   }

    public void render(UIContext context, float screenW, float screenH) {
        boolean isDraggingMove = (this.dragTarget != null && this.dragMode == Mode.MOVE);
        this.guideDotsAnim.update(isDraggingMove ? 1.0F : 0.0F);
        float guideAlpha = this.guideDotsAnim.getValue();

        // Draw guide dot grid with smooth fade animation
        if (guideAlpha > 0.01F) {
            ColorRGBA dotColor = new ColorRGBA(255, 255, 255, (int) (255 * guideAlpha * 0.35F));
            for (float gx = 0; gx <= screenW; gx += GUIDE_DOT_SPACING) {
                for (float gy = 0; gy <= screenH; gy += GUIDE_DOT_SPACING) {
                    context.drawRoundedRect(gx - GUIDE_DOT_SIZE / 2.0F, gy - GUIDE_DOT_SIZE / 2.0F,
                            GUIDE_DOT_SIZE, GUIDE_DOT_SIZE, BorderRadius.all(GUIDE_DOT_SIZE / 2.0F), dotColor);
                }
            }
        }

        if (this.dragTarget != null && this.dragMode == Mode.MOVE) {
            SnapGuide.renderGuides(context, this.activeGuideX, this.activeGuideY, screenW, screenH, 1.0F);
        }

        for (PositionableHudModule module : this.targets) {
            module.renderPreview(context);

            float x = module.getLastBoundsX();
            float y = module.getLastBoundsY();
            float w = module.getLastBoundsWidth();
            float h = module.getLastBoundsHeight();
            boolean active = module == this.dragTarget;

            ColorRGBA border = active ? new ColorRGBA(151, 71, 255, 255) : new ColorRGBA(255, 255, 255, 90);
            context.drawRoundedBorder(x, y, w, h, 1.0F, BorderRadius.all(4.0F), border);

            this.drawHandle(context, x + w, y + h);

            if (GuiUtility.isHovered((double) x, (double) y, (double) w, (double) h, context)) {
                CursorUtility.set(CursorType.HAND);
            }
        }
    }

   private void drawHandle(UIContext context, float centerX, float centerY) {
      float s = HANDLE_SIZE;
      context.drawRoundedRect(centerX - s / 2.0F, centerY - s / 2.0F, s, s, BorderRadius.all(1.5F), ColorRGBA.WHITE);
      context.drawRoundedBorder(centerX - s / 2.0F, centerY - s / 2.0F, s, s, 1.0F, BorderRadius.all(1.5F), new ColorRGBA(151, 71, 255, 255));
   }
}