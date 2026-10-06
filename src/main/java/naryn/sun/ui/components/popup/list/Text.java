package naryn.sun.ui.components.popup.list;

import com.mojang.blaze3d.systems.RenderSystem;
import naryn.sun.framework.base.UIContext;
import naryn.sun.framework.msdf.Font;
import naryn.sun.framework.msdf.Fonts;
import naryn.sun.ui.components.popup.PopupComponent;
import naryn.sun.utility.colors.Colors;
import naryn.sun.utility.gui.GuiUtility;

public class Text extends PopupComponent {
   private final String text;

   public Text(String text) {
      this.text = text;
   }

   @Override
   protected void renderComponent(UIContext context) {
      Font nameFont = Fonts.REGULAR.getFont(8.0F);
      float nameLeftPadding = 8.0F;
      float nameHeight = nameFont.height();
      context.drawFadeoutText(
         nameFont,
         this.text,
         this.x + nameLeftPadding,
         this.y + GuiUtility.getMiddleOfBox(nameHeight, this.height),
         Colors.getTextColor().withAlpha(RenderSystem.getShaderColor()[3] * 255.0F * 0.75F),
         0.8F,
         1.0F,
         this.width - 12.0F
      );
   }

   @Override
   public float getHeight() {
      return this.height = 18.0F;
   }
}
