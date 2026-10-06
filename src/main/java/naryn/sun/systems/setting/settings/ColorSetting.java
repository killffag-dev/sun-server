package naryn.sun.systems.setting.settings;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.function.BooleanSupplier;
import lombok.Generated;
import naryn.sun.systems.setting.SettingsContainer;
import naryn.sun.systems.setting.impl.AbstractSetting;
import naryn.sun.utility.colors.ColorRGBA;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.NotNull;

public class ColorSetting extends AbstractSetting {
   private ColorRGBA color;
   private boolean alpha = true;

   public ColorSetting(@NotNull SettingsContainer parent, String name, @NotNull BooleanSupplier hideCondition) {
      super(parent, name, hideCondition);
   }

   public ColorSetting(@NotNull SettingsContainer parent, String name) {
      super(parent, name);
   }

   public ColorSetting color(ColorRGBA color) {
      this.color = color;
      return this;
   }

   public ColorSetting alpha(boolean alpha) {
      this.alpha = alpha;
      return this;
   }

   @Override
   public JsonElement save() {
      ColorRGBA c = this.getRawColor();
      if (c == null) c = ColorRGBA.WHITE;
      JsonObject jsonObject = new JsonObject();
      jsonObject.addProperty("r", c.getRed());
      jsonObject.addProperty("g", c.getGreen());
      jsonObject.addProperty("b", c.getBlue());
      jsonObject.addProperty("a", c.getAlpha());
      return jsonObject;
   }

   @Override
   public void load(JsonElement element) {
      if (element.isJsonObject()) {
         JsonObject jsonObject = element.getAsJsonObject();
         int red = jsonObject.get("r").getAsInt();
         int green = jsonObject.get("g").getAsInt();
         int blue = jsonObject.get("b").getAsInt();
         int alpha = jsonObject.get("a").getAsInt();
         this.color = new ColorRGBA(this.validateColorRange(red), this.validateColorRange(green), this.validateColorRange(blue), this.validateColorRange(alpha));
      }
   }

   private int validateColorRange(int in) {
      return MathHelper.clamp(in, 0, 255);
   }

   public ColorRGBA getRawColor() {
      return this.color;
   }

   public ColorRGBA getColor() {
      naryn.sun.systems.theme.PaletteConfig palette = naryn.sun.systems.theme.PaletteConfig.getInstance();
      if (palette != null && palette.isEnabled() && palette.isSyncModules()) {
         float alphaVal = this.color != null ? this.color.getAlpha() : 255.0F;
         return palette.getActiveColor().withAlpha(alphaVal);
      }
      return this.color;
   }

   @Generated
   public boolean isAlpha() {
      return this.alpha;
   }

   @Generated
   public void setColor(ColorRGBA color) {
      this.color = color;
   }

   @Generated
   public void setAlpha(boolean alpha) {
      this.alpha = alpha;
   }
}
