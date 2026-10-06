package naryn.sun.ui.menu.layout;

import naryn.sun.utility.interfaces.IScaledResolution;

public class MenuLayoutData {

   public static final float BASE_WINDOW_WIDTH  = 460.0F;
   public static final float BASE_WINDOW_HEIGHT = 280.0F;
   public static final float BASE_BOTTOM_WIDTH  = 220.0F;
   public static final float BASE_BOTTOM_HEIGHT = 28.0F;
   public static final float BASE_BOTTOM_GAP    = 8.0F;
   public static final float BASE_TOOLTIP_WIDTH  = 200.0F;
   public static final float BASE_TOOLTIP_HEIGHT = 40.0F;

   public float windowX;
   public float windowY;
   public float windowScale = 1.0F;

   public float bottomBarX;
   public float bottomBarY;
   public float bottomBarScale = 1.0F;

   public float tooltipX;
   public float tooltipY;
   public float tooltipScale = 1.0F;
   public boolean custom = false;

   public void updateDefaultPositions(float screenW, float screenH) {
      if (this.custom) return;

      float fitScaleW = screenW < BASE_WINDOW_WIDTH ? screenW / BASE_WINDOW_WIDTH : 1.0F;
      float fitScaleH = screenH < (BASE_WINDOW_HEIGHT + BASE_BOTTOM_HEIGHT + BASE_BOTTOM_GAP + 20.0F)
              ? screenH / (BASE_WINDOW_HEIGHT + BASE_BOTTOM_HEIGHT + BASE_BOTTOM_GAP + 20.0F) : 1.0F;
      float autoScale = Math.min(1.0F, Math.min(fitScaleW, fitScaleH));

      this.windowScale = autoScale;
      this.bottomBarScale = autoScale;
      this.tooltipScale = autoScale;

      float winW = BASE_WINDOW_WIDTH * autoScale;
      float winH = BASE_WINDOW_HEIGHT * autoScale;
      float botW = BASE_BOTTOM_WIDTH * autoScale;
      float botH = BASE_BOTTOM_HEIGHT * autoScale;
      float tipW = BASE_TOOLTIP_WIDTH * autoScale;
      float tipH = BASE_TOOLTIP_HEIGHT * autoScale;

      this.windowX = (screenW - winW) / 2.0F;
      this.windowY = (screenH - winH) / 2.0F;

      this.bottomBarX = this.windowX + (winW - botW) / 2.0F;
      this.bottomBarY = Math.min(screenH - botH - 4.0F, this.windowY + winH + BASE_BOTTOM_GAP * autoScale);

      this.tooltipX = this.windowX + (winW - tipW) / 2.0F;
      this.tooltipY = Math.max(4.0F, this.windowY - tipH - 10.0F * autoScale);
   }

   public void reset(float screenW, float screenH) {
      this.custom = false;
      this.updateDefaultPositions(screenW, screenH);
   }

   public static MenuLayoutData createDefault() {
      MenuLayoutData data = new MenuLayoutData();
      data.custom = false;
      data.updateDefaultPositions(IScaledResolution.sr.getScaledWidth(), IScaledResolution.sr.getScaledHeight());
      return data;
   }
}