package naryn.sun.ui.menu.layout;

import naryn.sun.framework.base.UIContext;
import naryn.sun.framework.objects.BorderRadius;
import naryn.sun.framework.objects.MouseButton;
import naryn.sun.utility.animation.base.Animation;
import naryn.sun.utility.animation.base.Easing;
import naryn.sun.utility.colors.ColorRGBA;
import naryn.sun.utility.game.cursor.CursorType;
import naryn.sun.utility.game.cursor.CursorUtility;
import naryn.sun.utility.gui.GuiUtility;

import java.util.ArrayList;
import java.util.List;

public class LayoutEditor {

   public enum Element {
      WINDOW(MenuLayoutData.BASE_WINDOW_WIDTH, MenuLayoutData.BASE_WINDOW_HEIGHT, 260.0F, 160.0F),
      BOTTOM_BAR(MenuLayoutData.BASE_BOTTOM_WIDTH, MenuLayoutData.BASE_BOTTOM_HEIGHT, 120.0F, 20.0F),
      TOOLTIP(MenuLayoutData.BASE_TOOLTIP_WIDTH, MenuLayoutData.BASE_TOOLTIP_HEIGHT, 80.0F, 24.0F);

      public final float baseWidth;
      public final float baseHeight;
      public final float minWidth;
      public final float minHeight;

      Element(float baseWidth, float baseHeight, float minWidth, float minHeight) {
         this.baseWidth = baseWidth;
         this.baseHeight = baseHeight;
         this.minWidth = minWidth;
         this.minHeight = minHeight;
      }
   }

   private enum Mode { NONE, MOVE, RESIZE }

   private static final float HANDLE_SIZE = 6.0F;
   private static final float MAX_SCALE = 3.0F;
   private static final float GUIDE_DOT_SPACING = 16.0F;
   private static final float GUIDE_DOT_SIZE = 2.0F;

   private final MenuLayoutData data;
   private final Animation guideDotsAnim = new Animation(150L, 0.0F, Easing.FIGMA_EASE_IN_OUT);

   private Element dragElement;
   private Mode dragMode = Mode.NONE;
   private float dragStartMouseX;
   private float dragStartMouseY;
   private float dragStartX;
   private float dragStartY;
   private float dragStartScale;
   private float dragStartDist;

   private Float activeGuideX = null;
   private Float activeGuideY = null;

   public LayoutEditor(MenuLayoutData data) {
      this.data = data;
   }

   private float getX(Element e) {
      return switch (e) {
         case WINDOW -> this.data.windowX;
         case BOTTOM_BAR -> this.data.bottomBarX;
         case TOOLTIP -> this.data.tooltipX;
      };
   }

   private float getY(Element e) {
      return switch (e) {
         case WINDOW -> this.data.windowY;
         case BOTTOM_BAR -> this.data.bottomBarY;
         case TOOLTIP -> this.data.tooltipY;
      };
   }

   private float getScale(Element e) {
      return switch (e) {
         case WINDOW -> this.data.windowScale;
         case BOTTOM_BAR -> this.data.bottomBarScale;
         case TOOLTIP -> this.data.tooltipScale;
      };
   }

   private void setPos(Element e, float x, float y) {
      switch (e) {
         case WINDOW -> {
            this.data.windowX = x;
            this.data.windowY = y;
         }
         case BOTTOM_BAR -> {
            this.data.bottomBarX = x;
            this.data.bottomBarY = y;
         }
         case TOOLTIP -> {
            this.data.tooltipX = x;
            this.data.tooltipY = y;
         }
      }
   }

   private void setScale(Element e, float scale) {
      switch (e) {
         case WINDOW -> this.data.windowScale = scale;
         case BOTTOM_BAR -> this.data.bottomBarScale = scale;
         case TOOLTIP -> this.data.tooltipScale = scale;
      }
   }

   private float width(Element e) {
      return e.baseWidth * this.getScale(e);
   }

   private float height(Element e) {
      return e.baseHeight * this.getScale(e);
   }

   private float minScaleOf(Element e) {
      return Math.max(e.minWidth / e.baseWidth, e.minHeight / e.baseHeight);
   }

   public boolean onMouseClicked(double mouseX, double mouseY, MouseButton button, float screenW, float screenH) {
      if (button != MouseButton.LEFT) {
         return false;
      }

      for (Element e : Element.values()) {
         float x = this.getX(e);
         float y = this.getY(e);
         float w = this.width(e);
         float h = this.height(e);

         if (this.isNear((float)mouseX, (float)mouseY, x + w, y + h, HANDLE_SIZE)) {
            this.dragElement = e;
            this.dragMode = Mode.RESIZE;
            this.dragStartMouseX = (float)mouseX;
            this.dragStartMouseY = (float)mouseY;
            this.dragStartX = x;
            this.dragStartY = y;
            this.dragStartScale = this.getScale(e);
            this.dragStartDist = this.distance(x, y, x + w, y + h);
            return true;
         }
      }

      for (Element e : Element.values()) {
         float x = this.getX(e);
         float y = this.getY(e);
         float w = this.width(e);
         float h = this.height(e);

         if (GuiUtility.isHovered((double)x, (double)y, (double)w, (double)h, mouseX, mouseY)) {
            this.dragElement = e;
            this.dragMode = Mode.MOVE;
            this.dragStartMouseX = (float)mouseX;
            this.dragStartMouseY = (float)mouseY;
            this.dragStartX = x;
            this.dragStartY = y;
            this.dragStartScale = this.getScale(e);
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
      return (float)Math.sqrt(dx * dx + dy * dy);
   }

   public void onMouseDragged(double mouseX, double mouseY, float screenW, float screenH) {
      if (this.dragElement == null) {
         return;
      }

      this.data.custom = true;
      Element e = this.dragElement;

      if (this.dragMode == Mode.MOVE) {
         float deltaX = (float)mouseX - this.dragStartMouseX;
         float deltaY = (float)mouseY - this.dragStartMouseY;
         float w = this.width(e);
         float h = this.height(e);

         float rawX = this.clamp(this.dragStartX + deltaX, 0.0F, Math.max(0.0F, screenW - w));
         float rawY = this.clamp(this.dragStartY + deltaY, 0.0F, Math.max(0.0F, screenH - h));

         List<SnapGuide.Bounds> otherBounds = new ArrayList<>();
         for (Element other : Element.values()) {
            if (other == e) continue;
            otherBounds.add(new SnapGuide.Bounds(this.getX(other), this.getY(other), this.width(other), this.height(other)));
         }

         SnapGuide.SnapResult snap = SnapGuide.calculateSnap(rawX, rawY, w, h, screenW, screenH, otherBounds);
         this.activeGuideX = snap.guideX;
         this.activeGuideY = snap.guideY;

         float newX = this.clamp(snap.x, 0.0F, Math.max(0.0F, screenW - w));
         float newY = this.clamp(snap.y, 0.0F, Math.max(0.0F, screenH - h));

         this.setPos(e, newX, newY);
      } else if (this.dragMode == Mode.RESIZE) {
         this.activeGuideX = null;
         this.activeGuideY = null;

         float curDist = this.distance(this.dragStartX, this.dragStartY, (float)mouseX, (float)mouseY);
         float ratio = this.dragStartDist > 0.001F ? curDist / this.dragStartDist : 1.0F;
         float newScale = this.dragStartScale * ratio;

         newScale = Math.max(newScale, this.minScaleOf(e));
         newScale = Math.min(newScale, MAX_SCALE);

         float maxScaleW = (screenW - this.dragStartX) / e.baseWidth;
         float maxScaleH = (screenH - this.dragStartY) / e.baseHeight;
         newScale = Math.min(newScale, Math.min(maxScaleW, maxScaleH));

         this.setScale(e, newScale);
      }
   }

   public void onMouseReleased(double mouseX, double mouseY, MouseButton button) {
      if (button == MouseButton.LEFT) {
         this.dragElement = null;
         this.dragMode = Mode.NONE;
         this.activeGuideX = null;
         this.activeGuideY = null;
      }
   }

   public boolean isDragging() {
      return this.dragElement != null;
   }

   private float clamp(float value, float min, float max) {
      if (max < min) {
         return min;
      }
      return Math.max(min, Math.min(max, value));
   }

   public void validate(float screenW, float screenH) {
      if (!this.data.custom) {
         this.data.updateDefaultPositions(screenW, screenH);
         return;
      }

      for (Element e : Element.values()) {
         float scale = Math.max(this.getScale(e), this.minScaleOf(e));
         float maxScaleW = screenW / e.baseWidth;
         float maxScaleH = screenH / e.baseHeight;
         scale = Math.min(scale, Math.min(MAX_SCALE, Math.min(maxScaleW, maxScaleH)));
         this.setScale(e, scale);

         float w = e.baseWidth * scale;
         float h = e.baseHeight * scale;
         float x = this.clamp(this.getX(e), 0.0F, Math.max(0.0F, screenW - w));
         float y = this.clamp(this.getY(e), 0.0F, Math.max(0.0F, screenH - h));
         this.setPos(e, x, y);
      }
   }

   public void render(UIContext context, float screenW, float screenH) {
      boolean isDraggingMove = (this.dragElement != null && this.dragMode == Mode.MOVE);
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

      if (this.dragElement != null && this.dragMode == Mode.MOVE) {
         SnapGuide.renderGuides(context, this.activeGuideX, this.activeGuideY, screenW, screenH, 1.0F);
      }

      for (Element e : Element.values()) {
         float x = this.getX(e);
         float y = this.getY(e);
         float w = this.width(e);
         float h = this.height(e);
         boolean active = e == this.dragElement;

         ColorRGBA border = active ? new ColorRGBA(151, 71, 255, 255) : new ColorRGBA(255, 255, 255, 90);
         context.drawRoundedBorder(x, y, w, h, 1.0F, BorderRadius.all(4.0F), border);

         this.drawHandle(context, x + w, y + h);

         if (GuiUtility.isHovered((double)x, (double)y, (double)w, (double)h, context)) {
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
