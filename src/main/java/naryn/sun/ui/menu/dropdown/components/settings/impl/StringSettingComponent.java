package naryn.sun.ui.menu.dropdown.components.settings.impl;

import naryn.sun.framework.base.CustomComponent;
import naryn.sun.framework.base.UIContext;
import naryn.sun.framework.msdf.Font;
import naryn.sun.framework.msdf.Fonts;
import naryn.sun.framework.objects.BorderRadius;
import naryn.sun.framework.objects.MouseButton;
import naryn.sun.systems.localization.Localizator;
import naryn.sun.systems.setting.settings.StringSetting;
import naryn.sun.ui.components.textfield.TextField;
import naryn.sun.ui.menu.dropdown.components.settings.MenuSettingComponent;
import naryn.sun.ui.menu.skin.MenuSkin;
import naryn.sun.utility.colors.ColorRGBA;
import naryn.sun.utility.colors.Colors;
import naryn.sun.utility.game.cursor.CursorType;
import naryn.sun.utility.game.cursor.CursorUtility;
import naryn.sun.utility.gui.GuiUtility;
import naryn.sun.utility.gui.TextWrapUtility;

import java.util.List;

public class StringSettingComponent extends MenuSettingComponent<StringSetting> {
   private TextField textField;

   private static final float LEFT_PADDING  = 10.0F;
   private static final float LINE_GAP      = 2.0F;
   private static final float BASE_HEIGHT   = 35.0F;
   private static final float FIELD_HEIGHT  = 15.0F;
   private static final float HEADER_BASE   = 19.0F;

   public StringSettingComponent(StringSetting setting, CustomComponent parent) {
      super(setting, parent);
   }

   @Override
   public void onInit() {
      this.width = 13.0F;
      this.height = 8.0F;
      this.textField = new TextField(Fonts.REGULAR.getFont(8.0F));
      this.textField.paste(this.setting.getText());
      this.textField.setPreview(Localizator.translate("type_text"));
      super.onInit();
   }

   @Override
   public void update(UIContext context) {
      super.update(context);
   }

   private List<String> getNameLines() {
      Font nameFont = Fonts.REGULAR.getFont(8.0F);
      float maxWidth = this.width - LEFT_PADDING * 2;
      if (maxWidth <= 0.0F) maxWidth = 40.0F;
      return TextWrapUtility.wrap(nameFont, Localizator.translate(this.setting.getName()), maxWidth);
   }

   private float getExtra() {
      Font nameFont = Fonts.REGULAR.getFont(8.0F);
      int lines = Math.max(1, getNameLines().size());
      return (lines - 1) * (nameFont.height() + LINE_GAP);
   }

   @Override
   protected void renderComponent(UIContext context) {
      float extra = getExtra();
      float x = this.x + 8.0F;
      float y = this.y + 15.0F + extra;
      float width = this.width - 16.0F;
      float height = FIELD_HEIGHT;
      this.hoverAnimation.update(this.isHovered(context.getMouseX(), context.getMouseY()));
      if (this.isHovered(context.getMouseX(), context.getMouseY())) {
         CursorUtility.set(CursorType.HAND);
      }

      Font nameFont = Fonts.REGULAR.getFont(8.0F);
      float headerHeight = HEADER_BASE + extra;

      // Название — переносится на несколько строк вместо затухания
      List<String> lines = getNameLines();
      float totalTextHeight = lines.size() * nameFont.height() + Math.max(0, lines.size() - 1) * LINE_GAP;
      float lineY = this.y + GuiUtility.getMiddleOfBox(totalTextHeight, headerHeight) - 0.5F;
      ColorRGBA textColor = Colors.getTextColor().withAlpha(255.0F * (0.75F + 0.25F * this.hoverAnimation.getValue()));
      for (String line : lines) {
         context.drawText(nameFont, line, this.x + LEFT_PADDING, lineY, textColor);
         lineY += nameFont.height() + LINE_GAP;
      }

      MenuSkin.current().renderSettingBox(context, x, y, width, height, BorderRadius.all(4.0F), 1.0F);
      this.textField.set(x, y, width, height);
      this.textField.setAlpha(1.0F);
      this.textField.setTextColor(Colors.getTextColor());
      this.textField.render(context);
      this.setting.text(this.textField.getBuiltText());
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
   public void onKeyPressed(int keyCode, int scanCode, int modifiers) {
      this.textField.onKeyPressed(keyCode, scanCode, modifiers);
   }

   @Override
   public boolean charTyped(char chr, int modifiers) {
      return this.textField.charTyped(chr, modifiers);
   }

   @Override
   public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
      this.textField.onMouseClicked(mouseX, mouseY, button);
      super.onMouseReleased(mouseX, mouseY, button);
   }

   @Override
   public void onMouseReleased(double mouseX, double mouseY, MouseButton button) {
      this.textField.onMouseReleased(mouseX, mouseY, button);
   }

   @Override
   public float getHeight() {
      return this.height = BASE_HEIGHT + getExtra();
   }
}