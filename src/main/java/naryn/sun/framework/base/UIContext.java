package naryn.sun.framework.base;

import lombok.Generated;
import net.minecraft.client.gui.DrawContext;

public class UIContext extends CustomDrawContext {
   private int mouseX;
   private int mouseY;
   private final float delta;

   protected UIContext(DrawContext originalContext, int mouseX, int mouseY, float delta) {
      super(originalContext);
      this.mouseX = mouseX;
      this.mouseY = mouseY;
      this.delta = delta;
   }

   public static UIContext of(DrawContext originalContext, int mouseX, int mouseY, float delta) {
      return new UIContext(originalContext, mouseX, mouseY, delta);
   }

   @Generated
   public int getMouseX() {
      return this.mouseX;
   }

   @Generated
   public int getMouseY() {
      return this.mouseY;
   }

   @Generated
   public float getDelta() {
      return this.delta;
   }

   /**
    * Временно подменяет координаты мыши на локальные — используется при рендере
    * блоков, обёрнутых в матрицу translate+scale (редактор layout'а меню/HUD),
    * чтобы hover/drag внутри масштабированного блока считался в тех же
    * координатах, что и сами компоненты (которые остаются немасштабированными
    * "локальными" числами, а трансформирует их GPU через матрицу).
    * Обязательно верните исходные значения через эти же сеттеры сразу после
    * рендера блока, иначе собьётся всё, что рисуется после (тултипы, попапы и т.д).
    */
   public void setMouseX(int mouseX) {
      this.mouseX = mouseX;
   }

   public void setMouseY(int mouseY) {
      this.mouseY = mouseY;
   }
}