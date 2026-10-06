package naryn.sun.systems.file.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Map.Entry;
import naryn.sun.Sun;
import naryn.sun.systems.config.ConfigFile;
import naryn.sun.systems.file.ClientFile;
import naryn.sun.systems.file.FileManager;
import naryn.sun.systems.file.api.FileInfo;
import naryn.sun.systems.modules.constructions.swinganim.SwingManager;
import naryn.sun.systems.modules.constructions.swinganim.SwingPhase;
import naryn.sun.systems.modules.constructions.swinganim.presets.SwingPreset;
import naryn.sun.systems.modules.constructions.swinganim.presets.SwingPresetManager;
import naryn.sun.systems.modules.modules.utility.AutoAuth;
import naryn.sun.systems.theme.Theme;
import naryn.sun.systems.theme.ClientAppearance;
import naryn.sun.systems.localization.Language;
import naryn.sun.ui.components.ColorPicker;
import naryn.sun.utility.colors.ColorRGBA;
import naryn.sun.utility.interfaces.IMinecraft;
import net.minecraft.client.session.Session;
import net.minecraft.client.session.Session.AccountType;

@FileInfo(name = "client")
public class ClientDataFile extends ClientFile implements IMinecraft {
   @Override
   public void write() {
      JsonObject json = new JsonObject();
      json.addProperty("username", mc.getSession().getUsername());
      json.addProperty("theme", Sun.getInstance().getThemeManager().getCurrentTheme().name());
      json.addProperty("appearanceMode", ClientAppearance.getMode().name());
      json.addProperty("hudMaterial", ClientAppearance.getHudMode().name());
      json.addProperty("accent", ClientAppearance.getRawAccent().toHex());
      json.addProperty("bgDarkness", ClientAppearance.getMenuBackgroundDarkness());
      json.addProperty("bgBlur", ClientAppearance.getMenuBackgroundBlur());
      json.addProperty("language", ClientAppearance.getLanguage().name());
      json.addProperty("swing", Sun.getInstance().getSwingManager().getCurrent());
      json.add("friends", this.getFriendsJsonArray());
      json.add("targets", this.getTargetsJsonArray());
      json.add("colorPickerPresets", this.getColorPickerPresetsJsonArray());
      json.add("password", this.getPassword());
      json.add("sounds", naryn.sun.utility.sounds.ClientSoundManager.getInstance().getConfig().toJson());
      json.add("animations", naryn.sun.systems.animation.ClientAnimationConfig.getInstance().toJson());
      json.add("palette", naryn.sun.systems.theme.PaletteConfig.getInstance().toJson());
      json.add("background", naryn.sun.ui.menu.background.MenuBackgroundConfig.getInstance().toJson());
      ConfigFile currentConfig = Sun.getInstance().getConfigManager().getCurrent();
      if (currentConfig != null && !currentConfig.getFileName().equalsIgnoreCase("autosave")) {
         json.addProperty("lastConfig", currentConfig.getFileName());
      } else {
         String activeName = Sun.getInstance().getConfigManager().getActiveConfigName();
         if (activeName != null && !activeName.isBlank() && !activeName.equalsIgnoreCase("autosave")) {
            json.addProperty("lastConfig", activeName);
         }
      }

      try (FileWriter writer = new FileWriter(this.file)) {
         writer.write(FileManager.GSON.toJson(json));
      } catch (Exception var8) {
         var8.printStackTrace();
      }
   }

   @Override
   public void read() {
      try (FileReader reader = new FileReader(this.getFile())) {
         JsonObject object = (JsonObject)FileManager.GSON.fromJson(reader, JsonObject.class);
         if (object.has("username")) {
            String username = object.get("username").getAsString();
            new Session(username, UUID.randomUUID(), "", Optional.empty(), Optional.empty(), AccountType.MOJANG);
         }

         if (object.has("password")) {
            this.loadPass(object.getAsJsonArray("password"));
         }

         if (object.has("swing")) {
            String swing = object.get("swing").getAsString();
            SwingManager swingManager = Sun.getInstance().getSwingManager();
            SwingPresetManager manager = Sun.getInstance().getSwingPresetManager();

            for (SwingPreset value : Sun.getInstance().getSwingManager().getPresets()) {
               if (value.getName().equals(swing)) {
                  swingManager.getBezier().start(value.getBezierStart()).end(value.getBezierEnd());
                  swingManager.getBack().enabled(value.isSwingBack());
                  swingManager.getSpeed().setCurrentValue(value.getSpeed());
                  SwingPhase start = swingManager.getStartPhase();
                  start.getAnchorX().setCurrentValue(value.getFrom().anchorX());
                  start.getAnchorY().setCurrentValue(value.getFrom().anchorY());
                  start.getAnchorZ().setCurrentValue(value.getFrom().anchorZ());
                  start.getMoveX().setCurrentValue(value.getFrom().moveX());
                  start.getMoveY().setCurrentValue(value.getFrom().moveY());
                  start.getMoveZ().setCurrentValue(value.getFrom().moveZ());
                  start.getRotateX().setCurrentValue(value.getFrom().rotateX());
                  start.getRotateY().setCurrentValue(value.getFrom().rotateY());
                  start.getRotateZ().setCurrentValue(value.getFrom().rotateZ());
                  SwingPhase end = swingManager.getEndPhase();
                  end.getAnchorX().setCurrentValue(value.getTo().anchorX());
                  end.getAnchorY().setCurrentValue(value.getTo().anchorY());
                  end.getAnchorZ().setCurrentValue(value.getTo().anchorZ());
                  end.getMoveX().setCurrentValue(value.getTo().moveX());
                  end.getMoveY().setCurrentValue(value.getTo().moveY());
                  end.getMoveZ().setCurrentValue(value.getTo().moveZ());
                  end.getRotateX().setCurrentValue(value.getTo().rotateX());
                  end.getRotateY().setCurrentValue(value.getTo().rotateY());
                  end.getRotateZ().setCurrentValue(value.getTo().rotateZ());
                  swingManager.setCurrent(swing);
               }
            }
         }

         if (object.has("theme")) {
            String themeName = object.get("theme").getAsString();

            try {
               Theme theme = Theme.valueOf(themeName);
               Sun.getInstance().getThemeManager().setCurrentTheme(theme);
            } catch (IllegalArgumentException var16) {
               Sun.getInstance().getThemeManager().setCurrentTheme(Theme.DARK);
            }
         }

         if (object.has("appearanceMode")) {
            try {
               ClientAppearance.setMode(ClientAppearance.Mode.valueOf(object.get("appearanceMode").getAsString()));
            } catch (IllegalArgumentException ignored) {
               ClientAppearance.setMode(ClientAppearance.Mode.FACET_FROST);
            }
         }

         if (object.has("hudMaterial")) {
            try {
               ClientAppearance.setHudMode(ClientAppearance.Mode.valueOf(object.get("hudMaterial").getAsString()));
            } catch (IllegalArgumentException ignored) {
               ClientAppearance.setHudMode(ClientAppearance.Mode.FACET_FROST);
            }
         }

         if (object.has("bgDarkness")) {
            try {
               ClientAppearance.setMenuBackgroundDarkness(object.get("bgDarkness").getAsFloat());
            } catch (Exception ignored) {
            }
         }

         if (object.has("bgBlur")) {
            try {
               ClientAppearance.setMenuBackgroundBlur(object.get("bgBlur").getAsFloat());
            } catch (Exception ignored) {
            }
         }

         if (object.has("accent")) {
            try {
               ClientAppearance.setAccent(ColorRGBA.fromHex(object.get("accent").getAsString()));
            } catch (IllegalArgumentException ignored) {
               // Keep the default accent when an older or malformed client file is loaded.
            }
         }

         if (object.has("language")) {
            try {
               ClientAppearance.setLanguage(Language.valueOf(object.get("language").getAsString()));
            } catch (IllegalArgumentException ignored) {
               ClientAppearance.setLanguage(Language.RU_RU);
            }
         }

         if (object.has("friends")) {
            JsonArray friendsArray = object.getAsJsonArray("friends");
            Sun.getInstance().getFriendManager().clear();

            for (JsonElement friendElement : friendsArray) {
               Sun.getInstance().getFriendManager().add(friendElement.getAsString());
            }
         }

         if (object.has("targets")) {
            JsonArray targetsArray = object.getAsJsonArray("targets");
            Sun.getInstance().getTargetManager().getTarget().clear();

            for (JsonElement targetElement : targetsArray) {
               Sun.getInstance().getTargetManager().getTarget().add(targetElement.getAsString());
            }
         }

         if (object.has("colorPickerPresets")) {
            this.loadColorPickerPresets(object.getAsJsonArray("colorPickerPresets"));
         }

         if (object.has("sounds")) {
            naryn.sun.utility.sounds.ClientSoundManager.getInstance().getConfig().fromJson(object.getAsJsonObject("sounds"));
         }

         if (object.has("animations")) {
            naryn.sun.systems.animation.ClientAnimationConfig.getInstance().fromJson(object.getAsJsonObject("animations"));
         }

         if (object.has("palette")) {
            naryn.sun.systems.theme.PaletteConfig.getInstance().fromJson(object.getAsJsonObject("palette"));
         }

         if (object.has("background")) {
            naryn.sun.ui.menu.background.MenuBackgroundConfig.getInstance().fromJson(object.getAsJsonObject("background"));
         }

         if (object.has("lastConfig")) {
            String configName = object.get("lastConfig").getAsString();
            ConfigFile current = Sun.getInstance().getConfigManager().getCurrent();
            if (current == null || !current.getFileName().equalsIgnoreCase(configName)) {
               ConfigFile config = Sun.getInstance().getConfigManager().getConfig(configName);
               if (config != null) {
                  config.load();
               }
            }
         }
      } catch (Exception var18) {
         var18.printStackTrace();
      }
   }

   private JsonArray getFriendsJsonArray() {
      JsonArray friendsJsonArray = new JsonArray();

      for (String friendsName : Sun.getInstance().getFriendManager().listFriends()) {
         friendsJsonArray.add(friendsName);
      }

      return friendsJsonArray;
   }

   private JsonArray getTargetsJsonArray() {
      JsonArray targetsJsonArray = new JsonArray();

      for (String targetName : Sun.getInstance().getTargetManager().getTarget()) {
         targetsJsonArray.add(targetName);
      }

      return targetsJsonArray;
   }

   private JsonArray getPassword() {
      JsonArray passwordJsonArray = new JsonArray();

      for (Entry<String, String> pass : Sun.getInstance().getModuleManager().getModule(AutoAuth.class).listPassword().entrySet()) {
         JsonObject passObject = new JsonObject();
         passObject.addProperty("nick", pass.getValue());
         passObject.addProperty("pass", pass.getKey());
         passwordJsonArray.add(passObject);
      }

      return passwordJsonArray;
   }

   private JsonArray getColorPickerPresetsJsonArray() {
      JsonArray presetsArray = new JsonArray();

      for (ColorPicker.Preset preset : ColorPicker.COLOR_PRESETS) {
         if (preset.isShowing()) {
            JsonObject presetObject = new JsonObject();
            ColorRGBA color = preset.getColor();
            presetObject.addProperty("red", color.getRed());
            presetObject.addProperty("green", color.getGreen());
            presetObject.addProperty("blue", color.getBlue());
            presetObject.addProperty("alpha", color.getAlpha());
            presetsArray.add(presetObject);
         }
      }

      return presetsArray;
   }

   private void loadColorPickerPresets(JsonArray presetsArray) {
      List<ColorPicker.Preset> loadedPresets = new ArrayList<>();

      for (JsonElement presetElement : presetsArray) {
         JsonObject presetObject = presetElement.getAsJsonObject();
         float red = presetObject.get("red").getAsFloat();
         float green = presetObject.get("green").getAsFloat();
         float blue = presetObject.get("blue").getAsFloat();
         float alpha = presetObject.get("alpha").getAsFloat();
         ColorRGBA color = new ColorRGBA(red, green, blue, alpha);
         loadedPresets.add(new ColorPicker.Preset(color));
      }

      ColorPicker.setColorPresets(loadedPresets);
   }

   private void loadPass(JsonArray password) {
      for (JsonElement passElement : password) {
         JsonObject passObject = passElement.getAsJsonObject();
         String nick = passObject.get("nick").getAsString();
         String pass = passObject.get("pass").getAsString();
         Sun.getInstance().getModuleManager().getModule(AutoAuth.class).put(nick, pass);
      }
   }
}
