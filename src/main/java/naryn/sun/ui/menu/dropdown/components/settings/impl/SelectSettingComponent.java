package naryn.sun.ui.menu.dropdown.components.settings.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import naryn.sun.Sun;
import naryn.sun.framework.base.CustomComponent;
import naryn.sun.framework.base.UIContext;
import naryn.sun.framework.msdf.Font;
import naryn.sun.framework.msdf.Fonts;
import naryn.sun.framework.objects.BorderRadius;
import naryn.sun.framework.objects.MouseButton;
import naryn.sun.systems.localization.Localizator;
import naryn.sun.systems.setting.settings.SelectSetting;
import naryn.sun.ui.components.animated.AnimatedNumber;
import naryn.sun.ui.menu.dropdown.components.settings.MenuSettingComponent;
import naryn.sun.ui.menu.skin.MenuSkin;
import naryn.sun.utility.animation.base.Easing;
import naryn.sun.utility.colors.ColorRGBA;
import naryn.sun.utility.colors.Colors;
import naryn.sun.utility.game.cursor.CursorType;
import naryn.sun.utility.game.cursor.CursorUtility;
import naryn.sun.utility.gui.GuiUtility;
import naryn.sun.utility.gui.TextWrapUtility;
import naryn.sun.utility.render.DrawUtility;
import naryn.sun.utility.render.penis.PenisPlayer;
import naryn.sun.utility.time.Timer;

public class SelectSettingComponent extends MenuSettingComponent<SelectSetting> {
   private AnimatedNumber numberAnim;
   private SelectSetting.Value dragging;
   private final Timer sortTimer = new Timer();
   private boolean initialized;

   private static final float TOP_HEADER_BASE = 19.0F;
   private static final float TOP_LINE_GAP    = 2.0F;
   private static final float VALUE_LINE_GAP  = 1.0F;
   private static final float VALUE_ROW_BASE  = 12.0F;
   private static final float VALUE_PADDING   = 7.0F;
   private static final float LIST_PADDING    = 12.0F;

   public SelectSettingComponent(SelectSetting setting, CustomComponent parent) {
      super(setting, parent);
      List<SelectSetting.Value> enabled = new ArrayList<>();
      setting.getValues().forEach(sel -> {
         if (sel.isSelected()) {
            enabled.add(sel);
         }
      });
      setting.getSelectedValues().clear();
      setting.getSelectedValues().addAll(enabled);
   }

   private String getRightText() {
      return String.format(" %s", Localizator.translate("setting_of") + " " + this.setting.getValues().size());
   }

   private List<String> getTopLabelLines(Font nameFont) {
      String rightText = getRightText();
      float reserved = Fonts.REGULAR.getFont(7.0F).width(rightText)
            + (this.numberAnim != null ? this.numberAnim.getWidth() : 10.0F) + 10.0F;
      float maxWidth = this.getParent().getWidth() - 10.0F - reserved;
      if (maxWidth <= 0.0F) maxWidth = 30.0F;
      return TextWrapUtility.wrap(nameFont, Localizator.translate(this.setting.getName()), maxWidth);
   }

   private float getTopExtra(Font nameFont) {
      int lines = Math.max(1, getTopLabelLines(nameFont).size());
      return (lines - 1) * (nameFont.height() + TOP_LINE_GAP);
   }

   private float valueRowHeight(Font font, SelectSetting.Value value, float width) {
      float maxWidth = width - 12.0F - value.getActiveAnimation().getValue() * 10.0F;
      if (maxWidth <= 0.0F) maxWidth = 20.0F;
      List<String> lines = TextWrapUtility.wrap(font, Localizator.translate(value.getName()), maxWidth);
      float h = lines.size() * font.height() + Math.max(0, lines.size() - 1) * VALUE_LINE_GAP + VALUE_PADDING;
      return Math.max(VALUE_ROW_BASE, h);
   }

   private List<SelectSetting.Value> getOrderedValues() {
      List<SelectSetting.Value> vals = this.setting.getValues();
      if (!this.setting.isDraggable() && Localizator.getCurrentLanguage() == naryn.sun.systems.localization.Language.RU_RU) {
         java.text.Collator collator = java.text.Collator.getInstance(new java.util.Locale("ru", "RU"));
         collator.setStrength(java.text.Collator.PRIMARY);
         List<SelectSetting.Value> sorted = new ArrayList<>(vals);
         sorted.sort((a, b) -> collator.compare(Localizator.translate(a.getName()), Localizator.translate(b.getName())));
         return sorted;
      }
      return vals;
   }

   private float totalValuesHeight(Font font, float width) {
      float total = 0.0F;
      for (SelectSetting.Value v : getOrderedValues()) {
         if (!v.isHidden()) total += valueRowHeight(font, v, width);
      }
      return total;
   }

   @Override
   protected void renderComponent(UIContext context) {
      if (!this.initialized) {
         for (SelectSetting.Value value : this.setting.getValues()) {
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
      String rightText = getRightText();
      if (this.numberAnim == null) {
         this.numberAnim = new AnimatedNumber(Fonts.MEDIUM.getFont(7.0F), 5.0F, 500L, Easing.BAKEK);
      }

      float topExtra = getTopExtra(nameFont);
      float headerHeight = TOP_HEADER_BASE + topExtra;

      // Название настройки — переносится на несколько строк вместо затухания.
      // Ширина под "N из M" и счётчик зарезервирована на каждой строке,
      // поэтому наложения быть не может.
      List<String> topLines = getTopLabelLines(nameFont);
      float totalTopHeight = topLines.size() * nameFont.height() + Math.max(0, topLines.size() - 1) * TOP_LINE_GAP;
      float topLineY = y - 1.0F + GuiUtility.getMiddleOfBox(totalTopHeight, headerHeight);
      ColorRGBA topColor = Colors.getTextColor().withAlpha(255.0F * (0.75F + 0.25F * this.hoverAnimation.getValue()));
      for (String line : topLines) {
         context.drawText(nameFont, line, this.x + leftPadding, topLineY, topColor);
         topLineY += nameFont.height() + TOP_LINE_GAP;
      }

      float nameHeight = Fonts.REGULAR.getFont(7.0F).height();
      float firstLineY = y - 1.0F + GuiUtility.getMiddleOfBox(nameHeight, TOP_HEADER_BASE);
      this.numberAnim.settings(false, topColor);
      this.numberAnim.update(this.setting.getSelectedValues().size());
      this.numberAnim
         .pos(
            x + width - Fonts.REGULAR.getFont(7.0F).width(rightText) - this.numberAnim.getWidth(),
            firstLineY
         );
      this.numberAnim.render(context);
      context.drawRightText(
         Fonts.REGULAR.getFont(7.0F),
         rightText,
         x + width,
         firstLineY,
         topColor
      );

      float boxTop = y + 17.0F + topExtra;
      float totalValuesH = totalValuesHeight(valueFont, width);
      MenuSkin.current().renderSettingBox(
         context, x - 1.0F, boxTop, width + 2.0F, 8.0F + totalValuesH, BorderRadius.all(6.0F), 1.0F
      );
      float offset = 0.0F;

      for (SelectSetting.Value valuex : getOrderedValues()) {
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
            float elmtY = this.dragging == valuex
               ? Math.clamp((float)(context.getMouseY() - 2), boxTop + 3.0F, boxTop + 5.0F + totalValuesH)
               : boxTop + 3.0F + offset;

            boolean hover = GuiUtility.isHovered(
               (double)(x - 1.0F), (double)elmtY, (double)(width + 2.0F), (double)rowHeight, context.getMouseX(), context.getMouseY()
            );
            valuex.getYAnim().setEasing(Easing.BAKEK_SMALLER);
            valuex.getYAnim().update(elmtY - y);
            valuex.setYFactor(elmtY);
            if (hover && this.dragging != valuex && !valuex.isAlwaysEnabled()) {
               CursorUtility.set(CursorType.HAND);
            }

            valuex.getHoverAnimation().update(hover);
            valuex.getActiveAnimation().update(valuex.isSelected());
            if (this.setting.isDraggable()) {
               context.drawTexture(Sun.id("icons/hud/drag.png"), x + 7.0F, y + valuex.getYAnim().getValue(), 6.0F, 6.0F, Colors.getTextColor());
            }

            if (GuiUtility.isHovered(x, y + valuex.getYAnim().getValue() - 2.0F, 17.0, rowHeight, context) || valuex == this.dragging) {
               CursorUtility.set(CursorType.ARROW_VERTICAL);
            }

            // Название значения — переносится на несколько строк вместо затухания
            List<String> lines = TextWrapUtility.wrap(
               valueFont,
               Localizator.translate(valuex.getName()),
               width - 12.0F - valuex.getActiveAnimation().getValue() * 10.0F
            );
            ColorRGBA valColor = Colors.getTextColor()
               .withAlpha(255.0F * (0.75F + 0.25F * valuex.getHoverAnimation().getValue() + 0.25F * valuex.getActiveAnimation().getValue()));
            float lineY = y + valuex.getYAnim().getValue() + 0.5F;
            for (String line : lines) {
               context.drawText(valueFont, line, x + (this.setting.isDraggable() ? 18 : 7), lineY, valColor);
               lineY += valueFont.height() + VALUE_LINE_GAP;
            }

            if (valuex.getActiveAnimation().getValue() > 0.0F || valuex.getCurrentPenis().isPlaying()) {
               DrawUtility.drawAnimationSprite(
                  context.getMatrices(),
                  valuex.getCurrentPenis().getCurrentSprite(),
                  x + width - 11.0F - valuex.getActiveAnimation().getValue() * 2.0F,
                  y + valuex.getYAnim().getValue(),
                  6.0F,
                  6.0F,
                  Colors.getTextColor().mulAlpha(0.1F + 0.9F * valuex.getActiveAnimation().getValue())
               );
            }

            offset += rowHeight;
         }
      }

      if (this.sortTimer.finished(100L)) {
         if (this.setting.isDraggable()) {
            this.setting.getValues().sort(Comparator.comparingDouble(SelectSetting.Value::getYFactor));
         }
         this.sortTimer.reset();
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
         float x = this.x + 9.0F;
         float width = this.width - 18.0F;
         float topExtra = getTopExtra(nameFont);
         float boxTop = this.y + 1.0F + 17.0F + topExtra;
         float offset = 0.0F;

         for (SelectSetting.Value value : getOrderedValues()) {
            if (!value.isHidden()) {
               float rowHeight = valueRowHeight(valueFont, value, width);
               float rowTop = boxTop + 3.0F + offset;
               boolean hover = GuiUtility.isHovered((double)(x - 1.0F), (double)rowTop, (double)(this.width - 2.0F), (double)rowHeight, mouseX, mouseY);
               if (GuiUtility.isHovered((double)x, (double)(rowTop + 2.0F), 17.0, rowHeight, mouseX, mouseY) && this.setting.isDraggable()) {
                  this.dragging = value;
               } else if (hover) {
                  value.toggle();
                  naryn.sun.utility.sounds.ClientSoundManager.getInstance().playButtonClick();
               }

               offset += rowHeight;
            }
         }

         super.onMouseClicked(mouseX, mouseY, button);
      }
   }

   @Override
   public void onMouseReleased(double mouseX, double mouseY, MouseButton button) {
      this.dragging = null;
      super.onMouseReleased(mouseX, mouseY, button);
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