package naryn.sun.framework.objects.gradient.impl;

import naryn.sun.framework.objects.gradient.Gradient;
import naryn.sun.utility.colors.ColorRGBA;

public class HorizontalGradient extends Gradient {
   public HorizontalGradient(ColorRGBA startColor, ColorRGBA endColor) {
      super(startColor, startColor, endColor, endColor);
   }

   public HorizontalGradient rotate() {
      return new HorizontalGradient(this.bottomRightColor, this.topLeftColor);
   }
}
