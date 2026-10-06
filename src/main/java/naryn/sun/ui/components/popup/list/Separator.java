package naryn.sun.ui.components.popup.list;

import naryn.sun.framework.base.UIContext;
import naryn.sun.ui.components.popup.PopupComponent;
import naryn.sun.utility.colors.Colors;

public class Separator extends PopupComponent {
   @Override
   protected void renderComponent(UIContext context) {
      context.drawRect(this.x, this.y, this.width, this.height, Colors.getSeparatorColor());
   }

   @Override
   public float getHeight() {
      return this.height = 4.0F;
   }
}
