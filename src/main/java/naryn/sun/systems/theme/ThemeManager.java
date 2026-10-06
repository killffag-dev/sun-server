package naryn.sun.systems.theme;

import lombok.Generated;

public class ThemeManager {
   private Theme currentTheme = Theme.DARK;

   public void switchTheme() {
      this.currentTheme = this.currentTheme == Theme.DARK ? Theme.LIGHT : Theme.DARK;
   }

   public Theme getCurrentTheme() {
      return this.currentTheme;
   }

   @Generated
   public void setCurrentTheme(Theme currentTheme) {
      this.currentTheme = currentTheme;
   }
}
