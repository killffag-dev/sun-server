package naryn.sun.ui.menu;

import lombok.Generated;
import naryn.sun.framework.base.CustomScreen;
import naryn.sun.utility.animation.base.Animation;
import naryn.sun.utility.animation.base.Easing;

public abstract class MenuScreen extends CustomScreen {
   protected final Animation menuAnimation = new Animation(500L, Easing.LINEAR);
   protected boolean closing = true;

   @Generated
   public Animation getMenuAnimation() {
      return this.menuAnimation;
   }

   @Generated
   public boolean isClosing() {
      return this.closing;
   }

   @Generated
   public void setClosing(boolean closing) {
      this.closing = closing;
   }
}
