package naryn.sun.ui.menu.dropdown.components.settings.impl;

import naryn.sun.framework.base.CustomComponent;
import naryn.sun.framework.base.UIContext;
import naryn.sun.framework.msdf.Font;
import naryn.sun.framework.msdf.Fonts;
import naryn.sun.framework.objects.BorderRadius;
import naryn.sun.framework.objects.MouseButton;
import naryn.sun.systems.localization.Localizator;
import naryn.sun.systems.setting.settings.RangeSetting;
import naryn.sun.ui.menu.dropdown.components.settings.MenuSettingComponent;
import naryn.sun.ui.menu.skin.MenuSkin;
import naryn.sun.utility.animation.base.Animation;
import naryn.sun.utility.animation.base.Easing;
import naryn.sun.utility.colors.ColorRGBA;
import naryn.sun.utility.colors.Colors;
import naryn.sun.utility.game.TextUtility;
import naryn.sun.utility.game.cursor.CursorType;
import naryn.sun.utility.game.cursor.CursorUtility;
import naryn.sun.utility.gui.GuiUtility;

import java.util.ArrayList;
import java.util.List;

public class RangeSettingComponent extends MenuSettingComponent<RangeSetting> {
   private final Animation xAnim = new Animation(500L, Easing.BAKEK_PAGES);
   private final Animation widthAnim = new Animation(500L, Easing.BAKEK_PAGES);
   private boolean dragFirst;
   private boolean dragSecond;

   private static final float BASE_HEIGHT     = 29.0F;
   private static final float LEFT_PADDING    = 10.0F;
   private static final float RIGHT_PADDING   = 9.0F;
   private static final float NAME_VALUE_GAP  = 10.0F;
   private static final float LINE_GAP        = 2.0F;

   public RangeSettingComponent(RangeSetting setting, CustomComponent parent) {
      super(setting, parent);
   }

   // ---- Перенос текста лейбла по словам ----
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

   private float getAvailableTextWidth() {
      float w = this.width - LEFT_PADDING - RIGHT_PADDING;
      return w > 0.0F ? w : 40.0F;
   }

   private String getValueText(float first, float second) {
      return String.format("от %s до %s", TextUtility.formatNumber(first), TextUtility.formatNumber(second));
   }

   private boolean fitsSingleLine(Font nameFont, Font valueFont, String name, String value) {
      return nameFont.width(name) + NAME_VALUE_GAP + valueFont.width(value) <= getAvailableTextWidth();
   }

   private List<String> getNameLines(Font nameFont, Font valueFont, String name, String value) {
      if (fitsSingleLine(nameFont, valueFont, name, value)) {
         List<String> single = new ArrayList<>();
         single.add(name);
         return single;
      }
      return wrapText(nameFont, name, getAvailableTextWidth());
   }

   @Override
   protected void renderComponent(UIContext context) {
      float x = this.x + 9.0F;
      float y = this.y + 2.0F;
      float width = this.width - 18.0F;
      Font nameFont = Fonts.REGULAR.getFont(8.0F);
      Font valueFont = Fonts.REGULAR.getFont(7.0F);
      float leftPadding = 10.0F;
      float nameHeight = valueFont.height();
      float first = this.setting.getFirstValue();
      float second = this.setting.getSecondValue();
      if (first >= second) {
         first = this.setting.getSecondValue();
         second = this.setting.getFirstValue();
      }

      this.xAnim.update(first);
      this.widthAnim.update(second);
      this.hoverAnimation.update(this.isHovered(context.getMouseX(), context.getMouseY()));
      MenuSkin skin = MenuSkin.current();

      float firstW = width * GuiUtility.getPercent(this.xAnim.getValue(), this.setting.getMin(), this.setting.getMax());
      float secondW = width * GuiUtility.getPercent(this.widthAnim.getValue(), this.setting.getMin(), this.setting.getMax());

      skin.renderSliderTrack(context, x, y + this.height - 12.0F, width, 2.0F, BorderRadius.all(0.25F), 1.0F);
      skin.renderSliderFill(context, x + firstW, y + this.height - 12.0F, secondW - firstW, 2.0F, BorderRadius.all(0.25F), Colors.ACCENT, 1.0F);

      skin.renderSliderThumb(context, x + firstW, y + this.height - 11.0F, 6.0F, 6.0F, 0.0F, 1.0F);
      skin.renderSliderThumb(context, x + secondW, y + this.height - 11.0F, 6.0F, 6.0F, 0.0F, 1.0F);

      String name = Localizator.translate(this.setting.getName());
      String value = getValueText(this.xAnim.getValue(), this.widthAnim.getValue());
      List<String> lines = getNameLines(nameFont, valueFont, name, value);
      boolean singleLine = lines.size() == 1 && fitsSingleLine(nameFont, valueFont, name, value);
      ColorRGBA textColor = Colors.getTextColor().withAlpha(255.0F * (0.75F + 0.25F * this.hoverAnimation.getValue()));

      if (singleLine) {
         context.drawText(nameFont, name, this.x + leftPadding, y + 11.0F - nameFont.height(), textColor);
         context.drawRightText(valueFont, value, x + width, y + 11.0F - nameHeight, textColor);
      } else {
         float lineTop = y + 11.0F - nameFont.height();
         for (String line : lines) {
            context.drawText(nameFont, line, this.x + leftPadding, lineTop, textColor);
            lineTop += nameFont.height() + LINE_GAP;
         }
         context.drawRightText(valueFont, value, x + width, lineTop, textColor);
      }

      if (this.isHovered(context.getMouseX(), context.getMouseY())) {
         CursorUtility.set(CursorType.HAND);
      }

      if (this.dragFirst) {
         float xValue = GuiUtility.getSliderValue(this.setting.getMin(), this.setting.getMax(), x, width, context.getMouseX());
         this.setting.setFirstValue(xValue);
         CursorUtility.set(CursorType.ARROW_HORIZONTAL);
      } else if (this.dragSecond) {
         float xValue = GuiUtility.getSliderValue(this.setting.getMin(), this.setting.getMax(), x, width, context.getMouseX());
         this.setting.setSecondValue(xValue);
         CursorUtility.set(CursorType.ARROW_HORIZONTAL);
      }
   }

   @Override
   public void drawSplit(UIContext context) {
      float separatorHeight = 0.5F;
      context.drawRect(this.x, this.y + this.height, this.width, separatorHeight, Colors.getTextColor().withAlpha(5.1F));
   }

   @Override
   public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
      float x = this.x + 9.0F;
      float width = this.width - 18.0F;
      if (this.isHovered(mouseX, mouseY)) {
         float firstDist = (float)Math.abs(
            mouseX - (x + width * GuiUtility.getPercent(this.setting.getFirstValue(), this.setting.getMin(), this.setting.getMax()))
         );
         float secondDist = (float)Math.abs(
            mouseX - (x + width * GuiUtility.getPercent(this.setting.getSecondValue(), this.setting.getMin(), this.setting.getMax()))
         );
         if (firstDist < secondDist) {
            this.dragFirst = true;
         } else {
            this.dragSecond = true;
         }
      }

      super.onMouseClicked(mouseX, mouseY, button);
   }

   @Override
   public void onMouseReleased(double mouseX, double mouseY, MouseButton button) {
      this.dragFirst = false;
      this.dragSecond = false;
      super.onMouseReleased(mouseX, mouseY, button);
   }

   @Override
   public float getHeight() {
      Font nameFont = Fonts.REGULAR.getFont(8.0F);
      Font valueFont = Fonts.REGULAR.getFont(7.0F);
      String name = Localizator.translate(this.setting.getName());
      float first = this.setting.getFirstValue();
      float second = this.setting.getSecondValue();
      if (first >= second) {
         float tmp = first;
         first = second;
         second = tmp;
      }
      String value = getValueText(first, second);
      List<String> lines = getNameLines(nameFont, valueFont, name, value);
      boolean singleLine = lines.size() == 1 && fitsSingleLine(nameFont, valueFont, name, value);
      float extra = 0.0F;
      if (!singleLine) {
         extra += (lines.size() - 1) * (nameFont.height() + LINE_GAP);
         extra += valueFont.height() + LINE_GAP;
      }
      return this.height = BASE_HEIGHT + extra;
   }
}