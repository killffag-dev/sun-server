package naryn.sun.ui.menu.dropdown.components.settings.impl;

import naryn.sun.framework.base.CustomComponent;
import naryn.sun.framework.base.UIContext;
import naryn.sun.framework.msdf.Font;
import naryn.sun.framework.msdf.Fonts;
import naryn.sun.framework.objects.BorderRadius;
import naryn.sun.framework.objects.MouseButton;
import naryn.sun.systems.localization.Localizator;
import naryn.sun.systems.setting.settings.BezierSetting;
import naryn.sun.ui.menu.dropdown.components.settings.MenuSettingComponent;
import naryn.sun.ui.menu.skin.MenuSkin;
import naryn.sun.utility.animation.base.Animation;
import naryn.sun.utility.animation.base.Easing;
import naryn.sun.utility.colors.ColorRGBA;
import naryn.sun.utility.colors.Colors;
import naryn.sun.utility.game.cursor.CursorType;
import naryn.sun.utility.game.cursor.CursorUtility;
import naryn.sun.utility.gui.GuiUtility;
import naryn.sun.utility.gui.TextWrapUtility;
import net.minecraft.util.math.Vec2f;

import java.util.List;

public class BezierSettingComponent extends MenuSettingComponent<BezierSetting> {
   private final Animation startX = new Animation(500L, Easing.BAKEK_PAGES);
   private final Animation startY = new Animation(500L, Easing.BAKEK_PAGES);
   private final Animation endX = new Animation(500L, Easing.BAKEK_PAGES);
   private final Animation endY = new Animation(500L, Easing.BAKEK_PAGES);
   private boolean dragStart;
   private boolean dragEnd;

   private static final float LEFT_PADDING = 10.0F;
   private static final float LINE_GAP     = 2.0F;

   public BezierSettingComponent(BezierSetting setting, CustomComponent parent) {
      super(setting, parent);
   }

   private List<String> getNameLines(Font nameFont) {
      float maxWidth = this.getParent().getWidth() - LEFT_PADDING - 10.0F;
      if (maxWidth <= 0.0F) maxWidth = 40.0F;
      return TextWrapUtility.wrap(nameFont, Localizator.translate(this.setting.getName()), maxWidth);
   }

   private float getExtra(Font nameFont) {
      int lines = Math.max(1, getNameLines(nameFont).size());
      return (lines - 1) * (nameFont.height() + LINE_GAP);
   }

   @Override
   protected void renderComponent(UIContext context) {
      Font nameFont = Fonts.REGULAR.getFont(8.0F);
      float extra = getExtra(nameFont);

      float x = this.x + 9.0F;
      float y = this.y + 2.0F;
      float width = this.width - 18.0F;
      float leftPadding = LEFT_PADDING;
      this.hoverAnimation.update(this.isHovered(context.getMouseX(), context.getMouseY()));

      // Название — переносится на несколько строк вместо затухания.
      // Блок с кривой сдвигается вниз ровно настолько, насколько выросло название.
      List<String> lines = getNameLines(nameFont);
      ColorRGBA textColor = Colors.getTextColor().withAlpha(255.0F * (0.75F + 0.25F * this.hoverAnimation.getValue()));
      float lineTop = y + 11.0F - nameFont.height();
      for (String line : lines) {
         context.drawText(nameFont, line, this.x + leftPadding, lineTop, textColor);
         lineTop += nameFont.height() + LINE_GAP;
      }

      float offset = 3.0F;
      float boxX = x - 1.0F + offset;
      float boxY = lineTop + 4.0F + offset;
      float boxWidth = width + 2.0F - offset * 2.0F;
      float boxHeight = this.height - 27.0F - extra - offset * 2.0F;
      MenuSkin.current().renderSettingBox(
         context,
         boxX - offset,
         boxY - offset,
         boxWidth + offset * 2.0F,
         boxHeight + offset * 2.0F,
         BorderRadius.all(6.0F),
         1.0F
      );
      context.drawRoundedRect(
         boxX + this.startX.getValue() * boxWidth - 3.0F,
         boxY + this.startY.getValue() * boxHeight - 3.0F,
         6.0F,
         6.0F,
         BorderRadius.all(6.0F),
         Colors.WHITE.withAlpha(255.0F)
      );
      context.drawRoundedRect(
         boxX + this.endX.getValue() * boxWidth - 3.0F,
         boxY + this.endY.getValue() * boxHeight - 3.0F,
         6.0F,
         6.0F,
         BorderRadius.all(6.0F),
         Colors.WHITE.withAlpha(255.0F)
      );
      Vec2f anchorStart = new Vec2f(boxX, boxY + boxHeight);
      Vec2f controlStart = new Vec2f(boxX + this.startX.getValue() * boxWidth, boxY + this.startY.getValue() * boxHeight);
      Vec2f controlEnd = new Vec2f(boxX + this.endX.getValue() * boxWidth, boxY + this.endY.getValue() * boxHeight);
      Vec2f anchorEnd = new Vec2f(boxX + boxWidth, boxY);
      context.drawBezier(anchorStart, controlStart, controlEnd, anchorEnd, ColorRGBA.WHITE, 50);
      context.drawLine(anchorStart, controlStart, Colors.WHITE.mulAlpha(0.5F));
      context.drawLine(anchorEnd, controlEnd, Colors.WHITE.mulAlpha(0.5F));

      if (this.isHovered(context.getMouseX(), context.getMouseY())) {
         CursorUtility.set(CursorType.HAND);
      }

      if (this.dragStart) {
         float xValue = GuiUtility.getSliderValue(0.0F, 1.0F, boxX, boxWidth, context.getMouseX());
         float yValue = GuiUtility.getSliderValueWithoutClamp(0.0F, 1.0F, boxY, boxHeight, context.getMouseY());
         this.setting.start(new Vec2f(xValue, Math.clamp(yValue, -0.5F, 1.5F)));
         CursorUtility.set(CursorType.CROSSHAIR);
      } else if (this.dragEnd) {
         float xValue = GuiUtility.getSliderValue(0.0F, 1.0F, boxX, boxWidth, context.getMouseX());
         float yValue = GuiUtility.getSliderValueWithoutClamp(0.0F, 1.0F, boxY, boxHeight, context.getMouseY());
         this.setting.end(new Vec2f(xValue, Math.clamp(yValue, -0.5F, 1.5F)));
         CursorUtility.set(CursorType.CROSSHAIR);
      }

      this.startX.setValue(this.setting.start().x);
      this.startY.setValue(this.setting.start().y);
      this.endX.setValue(this.setting.end().x);
      this.endY.setValue(this.setting.end().y);
   }

   @Override
   public void drawSplit(UIContext context) {
      float separatorHeight = 0.5F;
      context.drawRect(this.x, this.y + this.height, this.width, separatorHeight, Colors.getTextColor().withAlpha(5.1F));
   }

   @Override
   public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
      Font nameFont = Fonts.REGULAR.getFont(8.0F);
      float extra = getExtra(nameFont);

      float x = this.x + 9.0F;
      float y = this.y + 2.0F;
      float width = this.width - 18.0F;
      if (this.isHovered(mouseX, mouseY)) {
         float offset = 3.0F;
         float lineTop = y + 11.0F - nameFont.height() + extra;
         float boxX = x - 1.0F + offset;
         float boxY = lineTop + 4.0F + offset;
         float boxWidth = width + 2.0F - offset * 2.0F;
         float boxHeight = this.height - 27.0F - extra - offset * 2.0F;
         Vec2f mouse = new Vec2f(GuiUtility.getPercent((float)mouseX, boxX, boxX + boxWidth), GuiUtility.getPercent((float)mouseY, boxY, boxY + boxHeight));
         float startDist = this.distance(this.setting.start(), mouse);
         float endDist = this.distance(this.setting.end(), mouse);
         if (startDist < endDist) {
            this.dragStart = true;
         } else {
            this.dragEnd = true;
         }
      }

      super.onMouseClicked(mouseX, mouseY, button);
   }

   public float distance(Vec2f vec, Vec2f vec2) {
      float f = vec.x - vec2.x;
      float g = vec.y - vec2.y;
      return (float)Math.sqrt(f * f + g * g);
   }

   @Override
   public void onMouseReleased(double mouseX, double mouseY, MouseButton button) {
      this.dragStart = false;
      this.dragEnd = false;
      super.onMouseReleased(mouseX, mouseY, button);
   }

   @Override
   public float getHeight() {
      Font nameFont = Fonts.REGULAR.getFont(8.0F);
      return this.height = this.width - 14.0F + getExtra(nameFont);
   }
}