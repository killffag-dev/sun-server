package naryn.sun.systems.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import lombok.Generated;
import naryn.sun.Sun;
import naryn.sun.systems.file.FileManager;
import naryn.sun.systems.localization.Localizator;
import naryn.sun.systems.modules.Module;
import naryn.sun.systems.modules.exception.UnknownModuleException;
import naryn.sun.systems.modules.modules.visuals.MenuModule;
import naryn.sun.systems.notifications.NotificationType;
import naryn.sun.systems.setting.Setting;
import naryn.sun.utility.game.MessageUtility;
import naryn.sun.utility.interfaces.IMinecraft;
import naryn.sun.utility.sounds.ClientSoundManager;
import net.minecraft.text.Text;

public class ConfigFile implements IMinecraft {

   /**
    * Текущая версия формата конфига (п.2 плана развития).
    * Увеличивать при любом изменении структуры JSON (переименование/перенос полей),
    * добавляя соответствующий шаг в {@link #migrate(JsonObject, int)}.
    */
   public static final int CONFIG_VERSION = 1;

   private List<Module> modules = Sun.getInstance().getModuleManager().getModules();
   private File file;
   private String fileName;

   public ConfigFile(String fileName) {
      this.fileName = fileName;
      File configsFolder = new File(FileManager.DIRECTORY, "configs");
      if (!configsFolder.exists()) {
         configsFolder.mkdir();
      }

      File sunFile = new File(configsFolder, fileName + ".sun");
      File legacyRockFile = new File(configsFolder, fileName + ".rock");
      if (!sunFile.exists() && legacyRockFile.exists()) {
         legacyRockFile.renameTo(sunFile);
      }
      this.file = sunFile;
   }

   public void load() {
      if (!this.file.exists()) {
         Sun.LOGGER.warn("Config file not found: {}", this.file.getAbsolutePath());
      } else {
         boolean needsResave = false;
         boolean hadApplyErrors = false;

         try {
            try (BufferedReader reader = new BufferedReader(new FileReader(this.file))) {
               JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();

               int loadedVersion = jsonObject.has("configVersion") ? jsonObject.get("configVersion").getAsInt() : 0;
               if (loadedVersion < CONFIG_VERSION) {
                  jsonObject = this.migrate(jsonObject, loadedVersion);
                  needsResave = true;
               }

               if (jsonObject.has("modules")) {
                  JsonArray modulesArray = jsonObject.getAsJsonArray("modules");
                  int loadedModules = 0;

                  for (JsonElement moduleElement : modulesArray) {
                     JsonObject moduleObject = moduleElement.getAsJsonObject();
                     if (moduleObject.has("name")) {
                        String moduleName = moduleObject.get("name").getAsString();
                        boolean enabled = moduleObject.has("enabled") && moduleObject.get("enabled").getAsBoolean();
                        int key = moduleObject.has("key") ? moduleObject.get("key").getAsInt() : 0;
                        boolean favorite = moduleObject.has("favorite") && moduleObject.get("favorite").getAsBoolean();

                        try {
                           Module module = Sun.getInstance().getModuleManager().getModule(moduleName);
                           if (!(module instanceof MenuModule)) {
                              boolean effectiveEnabled = module.getInfo().holdToActivate() ? false : enabled;
                              module.setEnabled(effectiveEnabled, true);
                           }

                           module.setKey(key);
                           module.setFavorite(favorite);

                           if (moduleObject.has("settings")) {
                              JsonObject settingsObject = moduleObject.getAsJsonObject("settings");

                              for (Setting setting : module.getSettings()) {
                                 if (settingsObject.has(setting.getName())) {
                                    try {
                                       setting.load(settingsObject.get(setting.getName()));
                                    } catch (Exception settingError) {
                                       // Битое/невалидное значение одной настройки (например, слайдер вне min/max) —
                                       // не роняем загрузку всего конфига, оставляем настройке значение по умолчанию
                                       // (заданное конструктором) и продолжаем со следующей настройкой/модулем.
                                       hadApplyErrors = true;
                                       Sun.LOGGER.warn(
                                          "Failed to apply setting '{}' of module '{}' in config {}: {}",
                                          setting.getName(), moduleName, this.fileName, settingError.getMessage()
                                       );
                                    }
                                 } else if (setting instanceof naryn.sun.systems.setting.settings.GroupSetting group) {
                                    try {
                                       group.load(settingsObject);
                                    } catch (Exception settingError) {
                                       hadApplyErrors = true;
                                       Sun.LOGGER.warn(
                                          "Failed to apply group setting '{}' of module '{}' in config {}: {}",
                                          setting.getName(), moduleName, this.fileName, settingError.getMessage()
                                       );
                                    }
                                 }
                              }
                           }

                           loadedModules++;
                        } catch (UnknownModuleException var17) {
                           Sun.LOGGER.warn("Module not found during config load: {}", moduleName);
                        }
                     }
                  }

                  ClientSoundManager.getInstance().playModuleToggle(true);
                  Sun.getInstance().getNotificationManager().addNotification(NotificationType.SUCCESS, Localizator.translate("configs.loaded"));
                  Sun.LOGGER.info("Loaded {} modules from config {}", loadedModules, this.fileName);
                  if (!this.fileName.equals("autosave")) {
                     Sun.getInstance().getConfigManager().setCurrent(this);
                  }
               } else {
                  Sun.LOGGER.warn("Invalid config format: missing 'modules' array in {}", this.fileName);
               }
            }
         } catch (Exception var19) {
            Sun.LOGGER.error("Failed to load config file {}: {}", this.fileName, var19.getMessage());
            return;
         }

         // Разовая перезапись файла с проставленным CONFIG_VERSION после миграции.
         // Выполняется ТОЛЬКО если весь цикл применения настроек отработал без ошибок
         // (hadApplyErrors == false) — иначе на диск закрепится файл с частично
         // дефолтными значениями, которые пользователь на самом деле не выбирал.
         // В этом случае конфиг остаётся немигрированным на диске (но применённым
         // в рантайме там, где смогли) — он будет повторно мигрирован при следующей
         // успешной загрузке, пока пользователь не поправит/пересохранит его вручную.
         if (needsResave && !hadApplyErrors) {
            try {
               this.save();
            } catch (Exception resaveError) {
               Sun.LOGGER.error("Failed to resave migrated config {}: {}", this.fileName, resaveError.getMessage());
            }
         } else if (needsResave) {
            Sun.LOGGER.warn(
               "Config {} was migrated but not resaved due to setting apply errors — will retry migration on next load",
               this.fileName
            );
         }
      }
   }

   /**
    * Дотягивает старый конфиг до текущей структуры перед обычным парсингом.
    * Мутирует и возвращает тот же {@link JsonObject}, не создавая новый файл на диске —
    * запись новой версии на диск происходит отдельно, в {@link #load()}, после этого метода,
    * и только при полностью успешном применении настроек.
    *
    * Каждый шаг миграции должен быть привязан к конкретной версии-источнику,
    * чтобы конфиги, "застрявшие" на старых версиях, проходили все промежуточные шаги по цепочке.
    */
   private JsonObject migrate(JsonObject json, int fromVersion) {
      if (fromVersion < 1) {
         // Версии 0 (конфиги без поля configVersion, т.е. созданные до этого изменения)
         // структурно совпадают с версией 1 — реальных преобразований не требуется,
         // здесь только фиксируем факт миграции.
         //
         // Пример будущего шага (версия 1 -> 2), для справки:
         // if (json.has("oldFieldName")) {
         //     json.add("newFieldName", json.get("oldFieldName"));
         //     json.remove("oldFieldName");
         // }
      }

      Sun.LOGGER.info("Config '{}' migrated: version {} -> {}", this.fileName, fromVersion, CONFIG_VERSION);
      return json;
   }

   public void save() {
      try {
         if (!this.file.exists() && !this.file.createNewFile()) {
            throw new IOException("Failed to create config file: " + this.file.getAbsolutePath());
         }

         JsonObject json = new JsonObject();
         json.addProperty("configVersion", CONFIG_VERSION);
         JsonArray modulesJsonArray = this.getModulesJsonArray();
         json.add("modules", modulesJsonArray);
         FileWriter fileWriter = new FileWriter(this.file);

         try {
            fileWriter.write(FileManager.GSON.toJson(json));
         } catch (Throwable var7) {
            try {
               fileWriter.close();
            } catch (Throwable var6) {
               var7.addSuppressed(var6);
            }

            throw var7;
         }

         fileWriter.close();
         if (!this.fileName.equals("autosave")) {
            Sun.getInstance().getConfigManager().setCurrent(this);
         }

         Sun.LOGGER.info("Successfully saved config " + this.fileName);
      } catch (IOException var8) {
         Sun.LOGGER.error("Failed to save config file", var8);
      }
   }

   public void delete() {
      if (this.file.exists() && this.file.delete()) {
         Sun.getInstance().getConfigManager().getConfigFiles().remove(this);
         MessageUtility.info(Text.of("Конфиг " + this.fileName + " успешно удален"));
         Sun.LOGGER.info("Config file deleted: {}", this.file.getAbsolutePath());
      } else {
         MessageUtility.error(Text.of("Произошла ошибка при удалении"));
         Sun.LOGGER.warn("Failed to delete config file: {}", this.file.getAbsolutePath());
      }
   }

   /**
    * Переименовывает файл конфига на диске (без пересохранения содержимого).
    * Возвращает false, если newName пустое, совпадает с текущим, конфиг с таким
    * именем уже существует, или сама файловая операция не удалась.
    */
   public boolean rename(String newName) {
      if (newName == null || newName.isBlank() || newName.equalsIgnoreCase(this.fileName)) {
         return false;
      }

      File configsFolder = this.file.getParentFile();
      File newFile = new File(configsFolder, newName + ".sun");
      if (newFile.exists()) {
         return false;
      }

      if (this.file.exists() && !this.file.renameTo(newFile)) {
         Sun.LOGGER.error("Не удалось переименовать конфиг {} -> {}", this.fileName, newName);
         return false;
      }

      this.file = newFile;
      this.fileName = newName;
      return true;
   }

   private JsonArray getModulesJsonArray() {
      JsonArray modulesJsonArray = new JsonArray();

      for (Module module : this.modules) {
         JsonObject moduleObject = new JsonObject();
         moduleObject.addProperty("name", module.getName());
         moduleObject.addProperty("enabled", module.isEnabled());
         moduleObject.addProperty("key", module.getKey());
         moduleObject.addProperty("favorite", module.isFavorite());
         moduleObject.add("settings", this.getSettingsJsonObject(module));
         modulesJsonArray.add(moduleObject);
      }

      return modulesJsonArray;
   }

   private JsonObject getSettingsJsonObject(Module module) {
      JsonObject settingsObject = new JsonObject();

      for (Setting setting : module.getSettings()) {
         settingsObject.add(setting.getName(), setting.save());
      }

      return settingsObject;
   }

   @Generated
   public String getFileName() {
      return this.fileName;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      ConfigFile that = (ConfigFile) o;
      return fileName != null && fileName.equalsIgnoreCase(that.fileName);
   }

   @Override
   public int hashCode() {
      return fileName != null ? fileName.toLowerCase().hashCode() : 0;
   }
}