package naryn.sun.ui.menu.dropdown.components.settings.impl;

import naryn.sun.Sun;
import naryn.sun.framework.base.CustomComponent;
import naryn.sun.framework.base.UIContext;
import naryn.sun.framework.msdf.Font;
import naryn.sun.framework.msdf.Fonts;
import naryn.sun.framework.objects.BorderRadius;
import naryn.sun.framework.objects.MouseButton;
import naryn.sun.systems.localization.Localizator;
import naryn.sun.systems.setting.settings.ModeSetting;
import naryn.sun.ui.menu.dropdown.components.settings.MenuSettingComponent;
import naryn.sun.ui.menu.skin.MenuSkin;
import naryn.sun.utility.colors.ColorRGBA;
import naryn.sun.utility.colors.Colors;
import naryn.sun.utility.game.cursor.CursorType;
import naryn.sun.utility.game.cursor.CursorUtility;
import naryn.sun.utility.gui.GuiUtility;
import naryn.sun.utility.gui.TextWrapUtility;
import naryn.sun.utility.render.DrawUtility;
import naryn.sun.utility.render.penis.PenisPlayer;

import java.util.List;

public class ModeSettingComponent extends MenuSettingComponent<ModeSetting> {
   private boolean initialized;

   private static final float TOP_HEADER_BASE = 19.0F;
   private static final float TOP_LINE_GAP    = 2.0F;
   private static final float VALUE_LINE_GAP  = 1.0F;
   private static final float VALUE_ROW_BASE  = 12.0F;
   private static final float VALUE_PADDING   = 7.0F;
   private static final float LIST_PADDING    = 12.0F;

   public ModeSettingComponent(ModeSetting setting, CustomComponent parent) {
      super(setting, parent);
   }

   private List<String> getTopLabelLines(Font font) {
      float maxWidth = this.getParent().getWidth() - 10.0F;
      if (maxWidth <= 0.0F) maxWidth = 40.0F;
      return TextWrapUtility.wrap(font, Localizator.translate(this.getSetting().getName()), maxWidth);
   }

   private float getTopExtra(Font font) {
      int lines = Math.max(1, getTopLabelLines(font).size());
      return (lines - 1) * (font.height() + TOP_LINE_GAP);
   }

   private float valueRowHeight(Font font, ModeSetting.Value value, float width) {
      float maxWidth = width - 12.0F - value.getActiveAnimation().getValue() * 10.0F;
      if (maxWidth <= 0.0F) maxWidth = 20.0F;
      List<String> lines = TextWrapUtility.wrap(font, Localizator.translate(value.getName()), maxWidth);
      float h = lines.size() * font.height() + Math.max(0, lines.size() - 1) * VALUE_LINE_GAP + VALUE_PADDING;
      return Math.max(VALUE_ROW_BASE, h);
   }

   private float totalValuesHeight(Font font, float width) {
      float total = 0.0F;
      for (ModeSetting.Value v : this.setting.getValues()) {
         if (!v.isHidden()) total += valueRowHeight(font, v, width);
      }
      return total;
   }

   @Override
   protected void renderComponent(UIContext context) {
      if (!this.initialized) {
         for (ModeSetting.Value value : this.setting.getValues()) {
            value.setEnablePenis(new PenisPlayer(Sun.id("penises/check_enable.penis")));
            value.setDisablePenis(new PenisPlayer(Sun.id("penises/check_disable.penis")));
            value.setLastState(value.isSelected());
            value.setCurrentPenis(value.isLastState() ? value.getEnablePenis() : value.getDisablePenis());
            if (value.isLastState()) {
               value.getEnablePenis().playOnce();
            } else {
               value.getDisablePenis().setFrame(0);
               value.getDisablePenis().stop();
            }
         }

         this.initialized = true;
      }

      float x = this.x + 9.0F;
      float y = this.y + 1.0F;
      float width = this.width - 18.0F;
      Font nameFont = Fonts.REGULAR.getFont(8.0F);
      Font valueFont = Fonts.REGULAR.getFont(7.0F);
      float leftPadding = 10.0F;
      this.hoverAnimation.update(this.isHovered(context.getMouseX(), context.getMouseY()));

      float topExtra = getTopExtra(nameFont);
      float headerHeight = TOP_HEADER_BASE + topExtra;

      // Заголовок настройки — переносится на несколько строк вместо затухания
      List<String> topLines = getTopLabelLines(nameFont);
      float totalTopHeight = topLines.size() * nameFont.height() + Math.max(0, topLines.size() - 1) * TOP_LINE_GAP;
      float topLineY = y - 1.0F + GuiUtility.getMiddleOfBox(totalTopHeight, headerHeight);
      ColorRGBA topColor = Colors.getTextColor().withAlpha(255.0F * (0.75F + 0.25F * this.hoverAnimation.getValue()));
      for (String line : topLines) {
         context.drawText(nameFont, line, this.x + leftPadding, topLineY, topColor);
         topLineY += nameFont.height() + TOP_LINE_GAP;
      }

      float boxTop = y + 17.0F + topExtra;
      float totalValuesH = totalValuesHeight(valueFont, width);
      MenuSkin.current().renderSettingBox(
         context, x - 1.0F, boxTop, width + 2.0F, 8.0F + totalValuesH, BorderRadius.all(6.0F), 1.0F
      );
      float offset = 0.0F;

      for (ModeSetting.Value valuex : this.setting.getValues()) {
         if (!valuex.isHidden()) {
            boolean currentState = valuex.isSelected();
            if (currentState != valuex.isLastState()) {
               if (currentState) {
                  valuex.setCurrentPenis(valuex.getEnablePenis());
               } else {
                  valuex.setCurrentPenis(valuex.getDisablePenis());
               }

               valuex.getCurrentPenis().playOnce();
               valuex.setLastState(currentState);
            }

            valuex.getCurrentPenis().update();

            float rowHeight = valueRowHeight(valueFont, valuex, width);
            float rowTop = boxTop + 3.0F + offset;

            boolean hover = GuiUtility.isHovered(
               (double)(x - 1.0F), (double)rowTop, (double)(width + 2.0F), (double)rowHeight, context.getMouseX(), context.getMouseY()
            );
            if (hover) {
               CursorUtility.set(CursorType.HAND);
            }

            valuex.getHoverAnimation().update(hover);
            valuex.getActiveAnimation().update(valuex.isSelected());

            // Название режима — переносится на несколько строк вместо затухания
            List<String> lines = TextWrapUtility.wrap(
               valueFont,
               Localizator.translate(valuex.getName()),
               width - 12.0F - valuex.getActiveAnimation().getValue() * 10.0F
            );
            ColorRGBA valColor = Colors.getTextColor()
               .withAlpha(255.0F * (0.75F + 0.25F * valuex.getHoverAnimation().getValue() + 0.25F * valuex.getActiveAnimation().getValue()));
            float lineY = rowTop + valueFont.height() + 0.5F;
            for (String line : lines) {
               context.drawText(valueFont, line, x + 7.0F, lineY, valColor);
               lineY += valueFont.height() + VALUE_LINE_GAP;
            }

            if (valuex.getActiveAnimation().getValue() > 0.0F || valuex.getCurrentPenis().isPlaying()) {
               DrawUtility.drawAnimationSprite(
                  context.getMatrices(),
                  valuex.getCurrentPenis().getCurrentSprite(),
                  x + width - 11.0F - valuex.getActiveAnimation().getValue() * 2.0F,
                  rowTop + valueFont.height() - 1.0F,
                  6.0F,
                  6.0F,
                  Colors.getTextColor().mulAlpha(0.1F + 0.9F * valuex.getActiveAnimation().getValue())
               );
            }

            offset += rowHeight;
         }
      }
   }

   @Override
   public void drawSplit(UIContext context) {
      float separatorHeight = 0.5F;
      context.drawRect(this.x, this.y + this.height, this.width, separatorHeight, Colors.getTextColor().withAlpha(5.1F));
   }

   @Override
   public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
      if (button == MouseButton.LEFT) {
         Font nameFont = Fonts.REGULAR.getFont(8.0F);
         Font valueFont = Fonts.REGULAR.getFont(7.0F);
         float width = this.width - 18.0F;
         float topExtra = getTopExtra(nameFont);
         float boxTop = this.y + 1.0F + 17.0F + topExtra;
         float offset = 0.0F;

         for (ModeSetting.Value value : this.setting.getValues()) {
            if (!value.isHidden()) {
               float rowHeight = valueRowHeight(valueFont, value, width);
               float rowTop = boxTop + 3.0F + offset;
               boolean hover = GuiUtility.isHovered(
                  (double)(this.x - 1.0F), (double)rowTop, (double)(this.width - 2.0F), (double)rowHeight, mouseX, mouseY
               );
               if (hover) {
                  value.select();
                  naryn.sun.utility.sounds.ClientSoundManager.getInstance().playButtonClick();
               }

               offset += rowHeight;
            }
         }

         super.onMouseClicked(mouseX, mouseY, button);
      }
   }

   @Override
   public float getHeight() {
      Font nameFont = Fonts.REGULAR.getFont(8.0F);
      Font valueFont = Fonts.REGULAR.getFont(7.0F);
      float width = this.width - 18.0F;
      float topExtra = getTopExtra(nameFont);
      float totalValuesH = totalValuesHeight(valueFont, width);
      return this.height = TOP_HEADER_BASE + topExtra + LIST_PADDING + totalValuesH;
   }
}