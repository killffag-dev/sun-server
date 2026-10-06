package naryn.sun.ui.menu.layout;

import naryn.sun.framework.base.UIContext;
import naryn.sun.framework.objects.MouseButton;
import naryn.sun.systems.modules.constructions.viewmodel.ViewModelMeshTracker;
import naryn.sun.systems.modules.modules.visuals.ViewModel;
import naryn.sun.systems.setting.settings.SliderSetting;
import naryn.sun.ui.menu.toolbar.ViewModelToolbar;
import naryn.sun.utility.game.cursor.CursorType;
import naryn.sun.utility.game.cursor.CursorUtility;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.math.MathHelper;

public class ViewModelEditor {

   public enum HandElement {
      MAIN_HAND("view_model.main_hand", 0.74F, 0.76F),
      OFF_HAND("view_model.off_hand", 0.26F, 0.76F);

      public final String titleKey;
      public final float defaultRelX;
      public final float defaultRelY;

      HandElement(String titleKey, float defaultRelX, float defaultRelY) {
         this.titleKey = titleKey;
         this.defaultRelX = defaultRelX;
         this.defaultRelY = defaultRelY;
      }
   }

   private enum Mode { NONE, MOVE, ROTATE }

   private final ViewModel viewModel;

   private HandElement activeHand = HandElement.MAIN_HAND;
   private HandElement dragElement;
   private Mode dragMode = Mode.NONE;

   private float dragStartMouseX;
   private float dragStartMouseY;
   private float dragStartTransX;
   private float dragStartTransY;
   private float dragStartRotX;
   private float dragStartRotY;
   private float dragStartRotZ;

   public ViewModelEditor(ViewModel viewModel) {
      this.viewModel = viewModel;
   }

   public static void resetBounds() {
   }

   public HandElement getActiveHand() {
      return this.activeHand;
   }

   public void setActiveHand(HandElement hand) {
      if (hand != null) {
         this.activeHand = hand;
      }
   }

   public float getPixelPerUnit(float screenH) {
      return screenH * 0.65F;
   }

   public float getScale(HandElement e) {
      return e == HandElement.MAIN_HAND
              ? this.viewModel.getMainScale().getCurrentValue()
              : this.viewModel.getOffScale().getCurrentValue();
   }

   public float getCenterX(HandElement e, float screenW, float screenH) {
      float ppu = this.getPixelPerUnit(screenH);
      if (e == HandElement.MAIN_HAND) {
         return screenW * e.defaultRelX + this.viewModel.getMainTranslateX().getCurrentValue() * ppu;
      } else {
         return screenW * e.defaultRelX - this.viewModel.getOffTranslateX().getCurrentValue() * ppu;
      }
   }

   public float getCenterY(HandElement e, float screenW, float screenH) {
      float ppu = this.getPixelPerUnit(screenH);
      if (e == HandElement.MAIN_HAND) {
         return screenH * e.defaultRelY - this.viewModel.getMainTranslateY().getCurrentValue() * ppu;
      } else {
         return screenH * e.defaultRelY - this.viewModel.getOffTranslateY().getCurrentValue() * ppu;
      }
   }

   public boolean isNearItem(HandElement e, double mouseX, double mouseY, float screenW, float screenH) {
      float depth = ViewModelMeshTracker.hitTest(e, mouseX, mouseY);
      if (depth < Float.POSITIVE_INFINITY) {
         return true;
      }
      if (!ViewModelMeshTracker.hasData(e)) {
         float cx = this.getCenterX(e, screenW, screenH);
         float cy = this.getCenterY(e, screenW, screenH);
         float radius = 80.0F * Math.max(0.6F, this.getScale(e));
         float dx = (float) mouseX - cx;
         float dy = (float) mouseY - cy;
         return (dx * dx + dy * dy) <= (radius * radius);
      }
      return false;
   }

   public boolean onMouseClicked(double mouseX, double mouseY, MouseButton button, float screenW, float screenH) {
      if (ViewModelToolbar.isCollapsed() || ViewModelToolbar.getCollapseValue() > 0.3F) {
         return false;
      }

      // 1. Проверяем клик ТОЛЬКО по самим предметам/рукам (точный mesh hit test)
      float mainDepth = ViewModelMeshTracker.hitTest(HandElement.MAIN_HAND, mouseX, mouseY);
      float offDepth = ViewModelMeshTracker.hitTest(HandElement.OFF_HAND, mouseX, mouseY);

      HandElement target = null;
      if (mainDepth < Float.POSITIVE_INFINITY && offDepth < Float.POSITIVE_INFINITY) {
         target = (mainDepth <= offDepth) ? HandElement.MAIN_HAND : HandElement.OFF_HAND;
      } else if (mainDepth < Float.POSITIVE_INFINITY) {
         target = HandElement.MAIN_HAND;
      } else if (offDepth < Float.POSITIVE_INFINITY) {
         target = HandElement.OFF_HAND;
      } else {
         for (HandElement e : HandElement.values()) {
            if (this.isNearItem(e, mouseX, mouseY, screenW, screenH)) {
               target = e;
               break;
            }
         }
      }

      if (target != null) {
         this.activeHand = target;
         this.dragElement = target;
         this.dragStartMouseX = (float) mouseX;
         this.dragStartMouseY = (float) mouseY;

         boolean isMain = (target == HandElement.MAIN_HAND);
         if (button == MouseButton.LEFT) {
            // ЛКМ: перемещение предмета
            this.dragMode = Mode.MOVE;
            this.dragStartTransX = isMain ? this.viewModel.getMainTranslateX().getCurrentValue() : this.viewModel.getOffTranslateX().getCurrentValue();
            this.dragStartTransY = isMain ? this.viewModel.getMainTranslateY().getCurrentValue() : this.viewModel.getOffTranslateY().getCurrentValue();
            return true;
         } else if (button == MouseButton.RIGHT) {
            // ПКМ: 3D вращение предмета
            this.dragMode = Mode.ROTATE;
            this.dragStartRotX = isMain ? this.viewModel.getMainRotateX().getCurrentValue() : this.viewModel.getOffRotateX().getCurrentValue();
            this.dragStartRotY = isMain ? this.viewModel.getMainRotateY().getCurrentValue() : this.viewModel.getOffRotateY().getCurrentValue();
            this.dragStartRotZ = isMain ? this.viewModel.getMainRotateZ().getCurrentValue() : this.viewModel.getOffRotateZ().getCurrentValue();
            return true;
         }
      }

      // Клик в свободное пространство ничего не выбирает и не перемещает
      return false;
   }

   public boolean onMouseScrolled(double mouseX, double mouseY, double verticalAmount, float screenW, float screenH) {
      if (verticalAmount == 0.0) {
         return false;
      }

      // Масштабируем предмет при скролле над ним или активный предмет
      HandElement target = null;
      float mainDepth = ViewModelMeshTracker.hitTest(HandElement.MAIN_HAND, mouseX, mouseY);
      float offDepth = ViewModelMeshTracker.hitTest(HandElement.OFF_HAND, mouseX, mouseY);
      if (mainDepth < Float.POSITIVE_INFINITY && offDepth < Float.POSITIVE_INFINITY) {
         target = (mainDepth <= offDepth) ? HandElement.MAIN_HAND : HandElement.OFF_HAND;
      } else if (mainDepth < Float.POSITIVE_INFINITY) {
         target = HandElement.MAIN_HAND;
      } else if (offDepth < Float.POSITIVE_INFINITY) {
         target = HandElement.OFF_HAND;
      } else {
         for (HandElement e : HandElement.values()) {
            if (this.isNearItem(e, mouseX, mouseY, screenW, screenH)) {
               target = e;
               break;
            }
         }
      }

      if (target == null) {
         target = this.activeHand;
      }

      SliderSetting scaleSetting = (target == HandElement.MAIN_HAND)
              ? this.viewModel.getMainScale()
              : this.viewModel.getOffScale();

      float cur = scaleSetting.getCurrentValue();
      float delta = (float) (verticalAmount > 0 ? 0.05F : -0.05F);
      float next = MathHelper.clamp(cur + delta, scaleSetting.getMin(), scaleSetting.getMax());
      scaleSetting.setCurrentValue(next);
      return true;
   }

   public void onMouseDragged(double mouseX, double mouseY, float screenW, float screenH) {
      if (this.dragElement == null) {
         return;
      }

      HandElement e = this.dragElement;
      boolean isMain = (e == HandElement.MAIN_HAND);

      if (this.dragMode == Mode.MOVE) {
         float deltaMouseX = (float) mouseX - this.dragStartMouseX;
         float deltaMouseY = (float) mouseY - this.dragStartMouseY;

         float ppu = this.getPixelPerUnit(screenH);
         float deltaTransX = (deltaMouseX / ppu) * (isMain ? 1.0F : -1.0F);
         // Инвертируем знак Y, чтобы при движении мышки вверх предмет шел вверх, а вниз — вниз
         float deltaTransY = -(deltaMouseY / ppu);

         float newTransX = MathHelper.clamp(this.dragStartTransX + deltaTransX, -2.0F, 2.0F);
         float newTransY = MathHelper.clamp(this.dragStartTransY + deltaTransY, -2.0F, 2.0F);

         if (isMain) {
            this.viewModel.getMainTranslateX().setCurrentValue(newTransX);
            this.viewModel.getMainTranslateY().setCurrentValue(newTransY);
         } else {
            this.viewModel.getOffTranslateX().setCurrentValue(newTransX);
            this.viewModel.getOffTranslateY().setCurrentValue(newTransY);
         }
      } else if (this.dragMode == Mode.ROTATE) {
         float deltaMouseX = (float) mouseX - this.dragStartMouseX;
         float deltaMouseY = (float) mouseY - this.dragStartMouseY;

         if (Screen.hasShiftDown()) {
            // Shift + ПКМ: вращение по диагонали (Roll / Z)
            float newRotZ = MathHelper.wrapDegrees(this.dragStartRotZ + deltaMouseX * 0.7F);
            if (isMain) {
               this.viewModel.getMainRotateZ().setCurrentValue(newRotZ);
            } else {
               this.viewModel.getOffRotateZ().setCurrentValue(newRotZ);
            }
         } else {
            // ПКМ: вращение по горизонтали (Yaw / Y) и вертикали (Pitch / X)
            float newRotY = MathHelper.wrapDegrees(this.dragStartRotY + deltaMouseX * 0.7F);
            float newRotX = MathHelper.wrapDegrees(this.dragStartRotX - deltaMouseY * 0.7F);
            if (isMain) {
               this.viewModel.getMainRotateY().setCurrentValue(newRotY);
               this.viewModel.getMainRotateX().setCurrentValue(newRotX);
            } else {
               this.viewModel.getOffRotateY().setCurrentValue(newRotY);
               this.viewModel.getOffRotateX().setCurrentValue(newRotX);
            }
         }
      }
   }

   public void onMouseReleased(double mouseX, double mouseY, MouseButton button) {
      this.dragElement = null;
      this.dragMode = Mode.NONE;
   }

   public boolean isDragging() {
      return this.dragElement != null;
   }

   public void render(UIContext context, float screenW, float screenH) {
      float collapseVal = ViewModelToolbar.getCollapseValue();
      float uiAlpha = 1.0F - collapseVal;
      if (uiAlpha <= 0.005F) {
         return;
      }

      double mouseX = context.getMouseX();
      double mouseY = context.getMouseY();

      // Проверка курсора над предметами
      boolean overAnyItem = (ViewModelMeshTracker.hitTest(HandElement.MAIN_HAND, mouseX, mouseY) < Float.POSITIVE_INFINITY)
                         || (ViewModelMeshTracker.hitTest(HandElement.OFF_HAND, mouseX, mouseY) < Float.POSITIVE_INFINITY);
      if (!overAnyItem && (!ViewModelMeshTracker.hasData(HandElement.MAIN_HAND) && !ViewModelMeshTracker.hasData(HandElement.OFF_HAND))) {
         for (HandElement e : HandElement.values()) {
            if (this.isNearItem(e, mouseX, mouseY, screenW, screenH)) {
               overAnyItem = true;
               break;
            }
         }
      }

      if (this.dragMode == Mode.MOVE || this.dragMode == Mode.ROTATE) {
         CursorUtility.set(CursorType.RESIZE_ALL);
      } else if (overAnyItem) {
         CursorUtility.set(CursorType.HAND);
      }

      // Все рамки и обводки полностью удалены - рендерятся только чистые предметы в 3D мире
   }
}
