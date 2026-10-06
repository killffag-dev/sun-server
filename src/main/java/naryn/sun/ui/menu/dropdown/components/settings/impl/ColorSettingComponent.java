package naryn.sun.ui.menu.dropdown.components.settings.impl;

import naryn.sun.Sun;
import naryn.sun.framework.base.CustomComponent;
import naryn.sun.framework.base.UIContext;
import naryn.sun.framework.msdf.Font;
import naryn.sun.framework.msdf.Fonts;
import naryn.sun.framework.objects.BorderRadius;
import naryn.sun.framework.objects.MouseButton;
import naryn.sun.systems.localization.Localizator;
import naryn.sun.systems.setting.settings.ColorSetting;
import naryn.sun.ui.components.ColorPicker;
import naryn.sun.ui.menu.NewScreen;
import naryn.sun.ui.menu.dropdown.components.settings.MenuSettingComponent;
import naryn.sun.ui.menu.skin.MenuSkin;
import naryn.sun.utility.colors.ColorRGBA;
import naryn.sun.utility.colors.Colors;
import naryn.sun.utility.game.cursor.CursorType;
import naryn.sun.utility.game.cursor.CursorUtility;
import naryn.sun.utility.gui.GuiUtility;
import naryn.sun.utility.gui.TextWrapUtility;
import net.minecraft.client.MinecraftClient;
import ru.kotopushka.compiler.sdk.annotations.Compile;

import java.util.List;

public class ColorSettingComponent extends MenuSettingComponent<ColorSetting> {
   private ColorPicker picker;

   private static final float CHECK_WIDTH = 13.0F;
   private static final float LEFT_PADDING = 10.0F;
   private static final float LINE_GAP = 2.0F;
   private static final float BASE_HEIGHT = 18.0F;

   public ColorSettingComponent(ColorSetting setting, CustomComponent parent) {
      super(setting, parent);
   }

   @Override
   public void onInit() {
      this.width = 13.0F;
      this.height = 8.0F;
      super.onInit();
   }

   @Override
   public void update(UIContext context) {
      super.update(context);
   }

   private List<String> getNameLines() {
      Font nameFont = Fonts.REGULAR.getFont(8.0F);
      float maxWidth = this.width - CHECK_WIDTH - 20.0F;
      if (maxWidth <= 0.0F) maxWidth = 40.0F;
      return TextWrapUtility.wrap(nameFont, Localizator.translate(this.setting.getName()), maxWidth);
   }

   @Override
   protected void renderComponent(UIContext context) {
      this.hoverAnimation.update(this.isHovered(context.getMouseX(), context.getMouseY()));
      if (this.isHovered(context.getMouseX(), context.getMouseY())) {
         CursorUtility.set(CursorType.HAND);
      }

      Font nameFont = Fonts.REGULAR.getFont(8.0F);
      float leftPadding = LEFT_PADDING;

      // Название — переносится на несколько строк вместо затухания
      List<String> lines = getNameLines();
      float totalTextHeight = lines.size() * nameFont.height() + Math.max(0, lines.size() - 1) * LINE_GAP;
      float rowHeight = getHeight();
      float lineY = this.y + GuiUtility.getMiddleOfBox(totalTextHeight, rowHeight) - 0.5F;
      ColorRGBA textColor = Colors.getTextColor().withAlpha(255.0F * (0.75F + 0.25F * this.hoverAnimation.getValue()));
      for (String line : lines) {
         context.drawText(nameFont, line, this.x + leftPadding, lineY, textColor);
         lineY += nameFont.height() + LINE_GAP;
      }

      // Свотч цвета остаётся у верхней части строки, независимо от числа строк текста
      MenuSkin.current().renderColorSwatch(context, this.x + this.width - leftPadding - 8.0F, this.y + 5.0F, 8.0F, 8.0F, BorderRadius.all(4.5F), this.setting.getColor(), 1.0F);
      if (this.picker != null) {
         this.setting.color(this.picker.built());
         if (!this.picker.isShowing() && this.picker.getAnimation().getValue() == 0.0F) {
            this.picker = null;
         }
      }
   }

   @Override
   public void drawSplit(UIContext context) {
      float separatorHeight = 0.5F;
      context.drawRect(this.x, this.y + this.height, this.width, separatorHeight, Colors.getTextColor().withAlpha(5.1F));
   }

   @Compile
   @Override
   public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
      if (this.isHovered(mouseX, mouseY) && button == MouseButton.LEFT) {
         if (Sun.getInstance().getMenuScreen() instanceof NewScreen newScreen) {
            for (ColorPicker existing : newScreen.getColorPickers()) {
               existing.setShowing(false);
            }
            String title = Localizator.translate(this.setting.getName());
            this.picker = newScreen.createCenteredColorPicker(this.setting.isAlpha(), this.setting.getColor(), title);
         }
      }

      super.onMouseClicked(mouseX, mouseY, button);
   }

   @Override
   public float getHeight() {
      int lines = Math.max(1, getNameLines().size());
      float extra = (lines - 1) * (Fonts.REGULAR.getFont(8.0F).height() + LINE_GAP);
      return this.height = BASE_HEIGHT + extra;
   }
}