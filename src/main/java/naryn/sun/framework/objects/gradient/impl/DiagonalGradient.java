package naryn.sun.framework.objects.gradient.impl;

import naryn.sun.framework.objects.gradient.Gradient;
import naryn.sun.utility.colors.ColorRGBA;

class DiagonalGradient extends Gradient {
   public DiagonalGradient(ColorRGBA startColor, ColorRGBA endColor) {
      super(startColor, endColor, endColor, startColor);
   }

   public DiagonalGradient rotate() {
      return new DiagonalGradient(this.topRightColor, this.bottomLeftColor);
   }
}
