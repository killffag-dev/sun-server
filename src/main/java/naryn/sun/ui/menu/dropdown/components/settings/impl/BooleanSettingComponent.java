package naryn.sun.ui.menu.dropdown.components.settings.impl;

import naryn.sun.Sun;
import naryn.sun.framework.base.CustomComponent;
import naryn.sun.framework.base.UIContext;
import naryn.sun.framework.msdf.Font;
import naryn.sun.framework.msdf.Fonts;
import naryn.sun.framework.objects.BorderRadius;
import naryn.sun.framework.objects.MouseButton;
import naryn.sun.systems.localization.Localizator;
import naryn.sun.systems.setting.settings.BooleanSetting;
import naryn.sun.ui.menu.dropdown.components.settings.MenuSettingComponent;
import naryn.sun.ui.menu.skin.MenuSkin;
import naryn.sun.utility.animation.base.Animation;
import naryn.sun.utility.animation.base.Easing;
import naryn.sun.utility.animation.types.ColorAnimation;
import naryn.sun.utility.colors.ColorRGBA;
import naryn.sun.utility.colors.Colors;
import naryn.sun.utility.game.cursor.CursorType;
import naryn.sun.utility.game.cursor.CursorUtility;
import naryn.sun.utility.gui.GuiUtility;
import naryn.sun.systems.animation.ClientAnimationConfig;

import java.util.ArrayList;
import java.util.List;

public class BooleanSettingComponent extends MenuSettingComponent<BooleanSetting> {
   private Animation circleOpacityAnimation;
   private Animation enableAnimation;
   private ColorAnimation backgroundColorAnimation;

   private static final float CHECK_WIDTH  = 13.0F;
   private static final float CHECK_HEIGHT = 8.0F;
   private static final float LEFT_PADDING = 10.0F;
   private static final float LINE_GAP     = 2.0F;
   private static final float BASE_HEIGHT  = 18.0F;

   public BooleanSettingComponent(BooleanSetting setting, CustomComponent parent) {
      super(setting, parent);
   }

   @Override
   public void onInit() {
      this.circleOpacityAnimation = new Animation(300L, 0.0F, Easing.FIGMA_EASE_IN_OUT);
      this.enableAnimation = new Animation(300L, Easing.BAKEK);
      this.backgroundColorAnimation = new ColorAnimation(300L, new ColorRGBA(24.0F, 24.0F, 27.0F), Easing.FIGMA_EASE_IN_OUT);
      this.width = 13.0F;
      this.height = 8.0F;
      super.onInit();
   }

   @Override
   public void update(UIContext context) {
      super.update(context);
   }

   private static List<String> wrapText(Font font, String text, float maxWidth) {
      List<String> lines = new ArrayList<>();
      if (text == null || text.isEmpty()) {
         lines.add("");
         return lines;
      }
      String[] words = text.split(" ");
      StringBuilder current = new StringBuilder();
      for (String word : words) {
         String candidate = current.length() == 0 ? word : current + " " + word;
         if (font.width(candidate) > maxWidth && current.length() > 0) {
            lines.add(current.toString());
            current = new StringBuilder(word);
         } else {
            current = new StringBuilder(candidate);
         }
      }
      lines.add(current.toString());
      return lines;
   }

   private List<String> getLabelLines() {
      Font nameFont = Fonts.REGULAR.getFont(8.0F);
      float maxWidth = this.width - CHECK_WIDTH - 20.0F;
      if (maxWidth <= 0.0F) maxWidth = 40.0F;
      return wrapText(nameFont, Localizator.translate(this.setting.getName()), maxWidth);
   }

   @Override
   protected void renderComponent(UIContext context) {
      this.circleOpacityAnimation.update(this.setting.isEnabled() ? 1.0F : 0.75F);
      this.enableAnimation.update(this.setting.isEnabled() ? 1.0F : 0.0F);
      this.backgroundColorAnimation
         .update(
            this.setting.isEnabled() ? new ColorRGBA(151.0F, 71.0F, 255.0F) : Sun.getInstance().getThemeManager().getCurrentTheme().getAdditionalColor()
         );
      this.hoverAnimation.update(this.isHovered(context.getMouseX(), context.getMouseY()));
      if (this.isHovered(context.getMouseX(), context.getMouseY())) {
         CursorUtility.set(CursorType.HAND);
      }

      Font nameFont = Fonts.REGULAR.getFont(8.0F);

      List<String> lines = getLabelLines();
      float totalTextHeight = lines.size() * nameFont.height() + Math.max(0, lines.size() - 1) * LINE_GAP;
      float rowHeight = getHeight();
      float lineY = this.y + GuiUtility.getMiddleOfBox(totalTextHeight, rowHeight) - 0.5F;
      ColorRGBA textColor = Colors.getTextColor()
            .withAlpha(255.0F * (0.75F + 0.25F * this.enableAnimation.getValue() + 0.25F * this.hoverAnimation.getValue()));
      for (String line : lines) {
         context.drawText(nameFont, line, this.x + LEFT_PADDING, lineY, textColor);
         lineY += nameFont.height() + LINE_GAP;
      }

      MenuSkin skin = MenuSkin.current();
      float trackX = this.x + this.width - CHECK_WIDTH - 9.0F;
      float trackY = this.y + 5.0F;
      float eVal = this.enableAnimation.getValue();
      skin.renderToggleTrack(context, trackX, trackY, CHECK_WIDTH, CHECK_HEIGHT,
              BorderRadius.all(3.0F), this.setting.isEnabled(), eVal, 1.0F);

      float stretch = ClientAnimationConfig.getInstance().isTogglePhysics()
              ? (float) Math.sin(eVal * Math.PI) * 1.5F : 0.0F;
      float thumbW = 6.0F + stretch;
      float thumbX = trackX + 1.0F + 5.0F * eVal - (stretch * (eVal > 0.5F ? 0.7F : 0.3F));
      float thumbY = this.y + 6.0F;
      float thumbSize = 6.0F;
      skin.renderToggleThumb(context, thumbX, thumbY, thumbW, thumbSize,
              eVal, this.circleOpacityAnimation.getValue());
   }

   @Override
   public void drawRegular8(UIContext context) {
   }

   @Override
   public void drawSplit(UIContext context) {
      float separatorHeight = 0.5F;
      context.drawRect(this.x, this.y + this.height, this.width, separatorHeight, Colors.getTextColor().withAlpha(5.1F));
   }

   @Override
   public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
      if (this.isHovered(mouseX, mouseY) && button == MouseButton.LEFT) {
         this.setting.toggle();
         naryn.sun.utility.sounds.ClientSoundManager.getInstance().playButtonClick();
      }

      super.onMouseReleased(mouseX, mouseY, button);
   }

   @Override
   public float getHeight() {
      int lines = Math.max(1, getLabelLines().size());
      float extra = (lines - 1) * (Fonts.REGULAR.getFont(8.0F).height() + LINE_GAP);
      return this.height = BASE_HEIGHT + extra;
   }
}