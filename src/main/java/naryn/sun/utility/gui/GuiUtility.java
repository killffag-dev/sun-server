package naryn.sun.utility.gui;

import lombok.Generated;
import naryn.sun.framework.base.CustomComponent;
import naryn.sun.framework.base.UIContext;
import naryn.sun.systems.setting.Setting;
import naryn.sun.systems.setting.settings.BezierSetting;
import naryn.sun.systems.setting.settings.BindSetting;
import naryn.sun.systems.setting.settings.BooleanSetting;
import naryn.sun.systems.setting.settings.ButtonSetting;
import naryn.sun.systems.setting.settings.ColorSetting;
import naryn.sun.systems.setting.settings.ModeSetting;
import naryn.sun.systems.setting.settings.RangeSetting;
import naryn.sun.systems.setting.settings.SelectSetting;
import naryn.sun.systems.setting.settings.SliderSetting;
import naryn.sun.systems.setting.settings.StringSetting;
import naryn.sun.systems.setting.settings.CommandBindEntrySetting;
import naryn.sun.systems.setting.settings.FriendListSetting;
import naryn.sun.systems.setting.settings.TargetListSetting;
import naryn.sun.systems.setting.settings.GroupSetting;
import naryn.sun.ui.menu.dropdown.components.settings.MenuSettingComponent;
import naryn.sun.ui.menu.dropdown.components.settings.impl.GroupSettingComponent;
import naryn.sun.ui.menu.dropdown.components.settings.impl.BezierSettingComponent;
import naryn.sun.ui.menu.dropdown.components.settings.impl.BindSettingComponent;
import naryn.sun.ui.menu.dropdown.components.settings.impl.BooleanSettingComponent;
import naryn.sun.ui.menu.dropdown.components.settings.impl.ButtonSettingComponent;
import naryn.sun.ui.menu.dropdown.components.settings.impl.ColorSettingComponent;
import naryn.sun.ui.menu.dropdown.components.settings.impl.CommandBindEntryComponent;
import naryn.sun.ui.menu.dropdown.components.settings.impl.FriendListComponent;
import naryn.sun.ui.menu.dropdown.components.settings.impl.TargetListComponent;
import naryn.sun.ui.menu.dropdown.components.settings.impl.ModeSettingComponent;
import naryn.sun.ui.menu.dropdown.components.settings.impl.RangeSettingComponent;
import naryn.sun.ui.menu.dropdown.components.settings.impl.SelectSettingComponent;
import naryn.sun.ui.menu.dropdown.components.settings.impl.SliderSettingComponent;
import naryn.sun.ui.menu.dropdown.components.settings.impl.StringSettingComponent;
import naryn.sun.utility.interfaces.IMinecraft;
import naryn.sun.utility.interfaces.IScaledResolution;
import naryn.sun.utility.render.obj.Rect;
import net.minecraft.client.util.math.Vector2f;

public final class GuiUtility {
   public static float getMiddleOfBox(float objectHeight, float boxHeight) {
      return (float)Math.ceil(boxHeight / 2.0F - objectHeight / 2.0F);
   }

   public static double getMiddleOfBox(double objectHeight, double boxHeight) {
      return Math.ceil(boxHeight / 2.0 - objectHeight / 2.0);
   }

   public static boolean isHovered(double x, double y, double width, double height, int mouseX, int mouseY) {
      return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
   }

   public static boolean isHovered(double x, double y, double width, double height, UIContext context) {
      return isHovered(x, y, width, height, context.getMouseX(), context.getMouseY());
   }

   public static boolean isHovered(Rect rect, double mouseX, double mouseY) {
      return isHovered((double)rect.getX(), (double)rect.getY(), (double)rect.getWidth(), (double)rect.getHeight(), mouseX, mouseY);
   }

   public static boolean isHovered(CustomComponent rect, double mouseX, double mouseY) {
      return isHovered((double)rect.getX(), (double)rect.getY(), (double)rect.getWidth(), (double)rect.getHeight(), mouseX, mouseY);
   }

   public static boolean isHovered(double x, double y, double width, double height, double mouseX, double mouseY) {
      return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
   }

   public static float getSliderValue(float min, float max, float start, float size, double mouse) {
      return (float)(Math.min(1.0, Math.max(0.0, (mouse - start) / size)) * (max - min)) + min;
   }

   public static float getSliderValueWithoutClamp(float min, float max, float start, float size, double mouse) {
      return (float)((mouse - start) / size * (max - min)) + min;
   }

   public static float getPercent(float value, float min, float max) {
      return (value - min) / (max - min);
   }

   public static Vector2f getMouse() {
      return new Vector2f(
         (float)(IMinecraft.mc.mouse.getX() / IScaledResolution.sr.getScaleFactor()),
         (float)(IMinecraft.mc.mouse.getY() / IScaledResolution.sr.getScaleFactor())
      );
   }

   public static MenuSettingComponent settinge(Setting setting, CustomComponent parent) {
      MenuSettingComponent settingComponent = null;
      if (setting instanceof BooleanSetting s) {
         settingComponent = new BooleanSettingComponent(s, parent);
      } else if (setting instanceof BindSetting s) {
         settingComponent = new BindSettingComponent(s, parent);
      } else if (setting instanceof ColorSetting s) {
         settingComponent = new ColorSettingComponent(s, parent);
      } else if (setting instanceof ModeSetting s) {
         settingComponent = new ModeSettingComponent(s, parent);
      } else if (setting instanceof RangeSetting s) {
         settingComponent = new RangeSettingComponent(s, parent);
      } else if (setting instanceof BezierSetting s) {
         settingComponent = new BezierSettingComponent(s, parent);
      } else if (setting instanceof ButtonSetting s) {
         settingComponent = new ButtonSettingComponent(s, parent);
      } else if (setting instanceof SelectSetting s) {
         settingComponent = new SelectSettingComponent(s, parent);
      } else if (setting instanceof SliderSetting s) {
         settingComponent = new SliderSettingComponent(s, parent);
      } else if (setting instanceof StringSetting s) {
         settingComponent = new StringSettingComponent(s, parent);
      } else if (setting instanceof CommandBindEntrySetting s) {
         settingComponent = new CommandBindEntryComponent(s, parent);
      } else if (setting instanceof FriendListSetting s) {
         settingComponent = new FriendListComponent(s, parent);
      } else if (setting instanceof TargetListSetting s) {
         settingComponent = new TargetListComponent(s, parent);
      } else if (setting instanceof GroupSetting s) {
         settingComponent = new GroupSettingComponent(s, parent);
      }

      if (settingComponent != null) {
         settingComponent.onInit();
      }

      return settingComponent;
   }

   @Generated
   private GuiUtility() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
