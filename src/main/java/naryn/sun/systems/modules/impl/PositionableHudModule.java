package naryn.sun.systems.modules.impl;

import lombok.Generated;
import naryn.sun.framework.base.CustomDrawContext;
import naryn.sun.framework.objects.BorderRadius;
import naryn.sun.systems.setting.settings.BooleanSetting;
import naryn.sun.ui.menu.layout.HudModuleLayoutManager;
import naryn.sun.utility.colors.Colors;
import naryn.sun.utility.render.RenderUtility;

/**
 * Базовый класс для модулей, рисующих HUD-элемент на экране, которые должны
 * быть доступны для перетаскивания/масштабирования во вкладке "Modules"
 * HUD-редактора.
 *
 * Чтобы новый модуль появился в редакторе автоматически — унаследуй его от
 * этого класса вместо BaseModule, и в рендере:
 *  1. посчитай defaultX/defaultY и width/height как обычно;
 *  2. замени присвоение x/y на this.resolveX(defaultX) / this.resolveY(defaultY);
 *  3. вызови this.beginScaledRender(context, width, height) ПЕРЕД первой отрисовкой;
 *  4. для фона можно использовать this.drawBackground(context, backgroundSetting, x, y, width, height);
 *  5. вызови this.endScaledRender(context) ПОСЛЕ последней отрисовки;
 *  6. вынеси тело отрисовки в приватный метод draw(CustomDrawContext context)
 *     и переопредели renderPreview(context), чтобы он вызывал этот же draw(...).
 *     Это позволит редактору рисовать чёткую (без ванильного блюра фона)
 *     копию модуля поверх экрана во время редактирования.
 * Дальше ничего регистрировать не нужно — редактор найдёт модуль сам.
 */
public abstract class PositionableHudModule extends BaseModule {

   private static volatile boolean editingActive = false;

   public static void setEditingActive(boolean active) {
      editingActive = active;
   }

   public static boolean isEditingActive() {
      return editingActive;
   }

   private Float layoutX;
   private Float layoutY;
   private float layoutScale = 1.0F;

   private float lastAnchorX;
   private float lastAnchorY;

   private float lastBoundsX;
   private float lastBoundsY;
   private float lastBoundsWidth;
   private float lastBoundsHeight;

   public PositionableHudModule() {
      HudModuleLayoutManager.getInstance().applyTo(this);
   }

   public float resolveX(float defaultX) {
      this.lastAnchorX = this.layoutX != null ? this.layoutX : defaultX;
      return this.lastAnchorX;
   }

   public float resolveY(float defaultY) {
      this.lastAnchorY = this.layoutY != null ? this.layoutY : defaultY;
      return this.lastAnchorY;
   }

   public void beginScaledRender(CustomDrawContext context, float width, float height) {
      float cx = this.lastAnchorX + width / 2.0F;
      float cy = this.lastAnchorY + height / 2.0F;

      this.lastBoundsWidth = width * this.layoutScale;
      this.lastBoundsHeight = height * this.layoutScale;
      this.lastBoundsX = cx - this.lastBoundsWidth / 2.0F;
      this.lastBoundsY = cy - this.lastBoundsHeight / 2.0F;

      RenderUtility.scale(context.getMatrices(), cx, cy, this.layoutScale);
   }

   public void endScaledRender(CustomDrawContext context) {
      RenderUtility.end(context.getMatrices());
   }

   /**
    * Единая точка отрисовки фона HUD-элемента. Во время редактирования (когда
    * открыта вкладка "Modules") всегда рисует плоский фон без блюра/стекла,
    * даже если в настройках Interface включён Glass/Minimalizm. Вне редактора
    * ведёт себя как обычно — рисует стеклянный фон, только если включена
    * переданная настройка background.
    */
   public void drawBackground(CustomDrawContext context, BooleanSetting background, float x, float y, float width, float height) {
      if (isEditingActive()) {
         context.drawRoundedRect(x, y, width, height, BorderRadius.all(6.0F), Colors.getBackgroundColor().withAlpha(200.0F));
      } else if (background.isEnabled()) {
         context.drawClientRect(x, y, width, height, 1.0F, 0.0F, 1.0F);
      }
   }

   /**
    * Вызывается редактором HUD-модулей (вкладка "Modules") каждый кадр, пока
    * идёт редактирование — рисует чёткую (без ванильного блюра фона экрана)
    * копию модуля прямо в контексте экрана, поверх размытого игрового мира.
    * По умолчанию ничего не делает — переопредели в модуле, вызвав тот же
    * приватный метод отрисовки, что и в обычном onHudRender.
    */
   public void renderPreview(CustomDrawContext context) {
   }

   public void setEditorPos(float x, float y) {
      this.layoutX = x;
      this.layoutY = y;
   }

   public void setEditorScale(float scale) {
      this.layoutScale = scale;
   }

   public void resetLayout() {
      this.layoutX = null;
      this.layoutY = null;
      this.layoutScale = 1.0F;
   }

   public boolean hasCustomPosition() {
      return this.layoutX != null;
   }

   @Generated
   public float getLayoutScale() {
      return this.layoutScale;
   }

   @Generated
   public Float getLayoutX() {
      return this.layoutX;
   }

   @Generated
   public Float getLayoutY() {
      return this.layoutY;
   }

   @Generated
   public float getLastAnchorX() {
      return this.lastAnchorX;
   }

   @Generated
   public float getLastAnchorY() {
      return this.lastAnchorY;
   }

   @Generated
   public float getLastBoundsX() {
      return this.lastBoundsX;
   }

   @Generated
   public float getLastBoundsY() {
      return this.lastBoundsY;
   }

   @Generated
   public float getLastBoundsWidth() {
      return this.lastBoundsWidth;
   }

   @Generated
   public float getLastBoundsHeight() {
      return this.lastBoundsHeight;
   }
}