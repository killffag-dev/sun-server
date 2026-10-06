package naryn.sun.ui.menu.dropdown.components.settings.impl;

import naryn.sun.framework.base.CustomComponent;
import naryn.sun.framework.base.UIContext;
import naryn.sun.framework.msdf.Font;
import naryn.sun.framework.msdf.Fonts;
import naryn.sun.framework.objects.BorderRadius;
import naryn.sun.framework.objects.MouseButton;
import naryn.sun.systems.localization.Localizator;
import naryn.sun.systems.setting.settings.SliderSetting;
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
import naryn.sun.utility.render.DrawUtility;
import naryn.sun.utility.time.Timer;

import java.util.ArrayList;
import java.util.List;

public class SliderSettingComponent extends MenuSettingComponent<SliderSetting> {
   private final Animation animation = new Animation(500L, Easing.BAKEK_PAGES);
   private final Animation moving = new Animation(500L, Easing.FIGMA_EASE_IN_OUT);
   private final Timer timer = new Timer();
   private boolean drag;
   private static SliderSettingComponent current;

   private static final float BASE_HEIGHT     = 29.0F;
   private static final float LEFT_PADDING    = 10.0F;
   private static final float RIGHT_PADDING   = 9.0F;
   private static final float NAME_VALUE_GAP  = 10.0F;
   private static final float LINE_GAP        = 2.0F;

   public SliderSettingComponent(SliderSetting setting, CustomComponent parent) {
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

   private String getValueText() {
      return TextUtility.formatNumberClean(this.animation.getValue()) + this.setting.getSuffix();
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
      this.animation.update(this.setting.getCurrentValue());
      this.hoverAnimation.update(this.isHovered(context.getMouseX(), context.getMouseY()));
      MenuSkin skin = MenuSkin.current();
      float trackY = y + this.height - 12.0F;
      float trackH = 2.0F;
      float fillW = width * GuiUtility.getPercent(this.animation.getValue(), this.setting.getMin(), this.setting.getMax());

      skin.renderSliderTrack(context, x, trackY, width, trackH, BorderRadius.all(0.25F), 1.0F);
      skin.renderSliderFill(context, x, trackY, fillW, trackH, BorderRadius.all(0.25F), Colors.ACCENT, 1.0F);

      if (this.timer.finished(1000L)) {
         DrawUtility.updateBuffer();
         this.timer.reset();
      }

      float thumbCX = x + fillW;
      float thumbCY = y + this.height - 11.0F;
      float thumbW = 9.0F + 6.0F * this.moving.getValue();
      float thumbH = 6.0F + 4.0F * this.moving.getValue();
      skin.renderSliderThumb(context, thumbCX, thumbCY, thumbW, thumbH, this.moving.getValue(), 1.0F);

      String name = Localizator.translate(this.setting.getName());
      String value = getValueText();
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

      this.moving.setDuration(200L);
      this.moving.update(this.drag ? 1.0F : 0.0F);
      if (this.drag) {
         float xValue = GuiUtility.getSliderValue(this.setting.getMin(), this.setting.getMax(), x, width, context.getMouseX());
         this.setting.setCurrentValue(xValue);
         CursorUtility.set(CursorType.ARROW_HORIZONTAL);
         current = this;
      }
   }

   @Override
   public void drawSplit(UIContext context) {
      float separatorHeight = 0.5F;
      context.drawRect(this.x, this.y + this.height, this.width, separatorHeight, Colors.getTextColor().withAlpha(5.1F));
   }

   @Override
   public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
      if (this.isHovered(mouseX, mouseY)) {
         this.drag = true;
      }

      super.onMouseClicked(mouseX, mouseY, button);
   }

   @Override
   public void onMouseReleased(double mouseX, double mouseY, MouseButton button) {
      this.drag = false;
      super.onMouseReleased(mouseX, mouseY, button);
   }

   @Override
   public void onKeyPressed(int keyCode, int scanCode, int modifiers) {
      if ((keyCode == 262 || keyCode == 263) && current == this) {
         current.getSetting().setCurrentValue(current.getSetting().getCurrentValue() + current.getSetting().getStep() * 0.7F * (keyCode == 262 ? 1 : -1));
      }
   }

   @Override
   public float getHeight() {
      Font nameFont = Fonts.REGULAR.getFont(8.0F);
      Font valueFont = Fonts.REGULAR.getFont(7.0F);
      String name = Localizator.translate(this.setting.getName());
      String value = getValueText();
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