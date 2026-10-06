package naryn.sun.systems.config;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import lombok.Generated;
import naryn.sun.Sun;
import naryn.sun.systems.file.FileManager;
import naryn.sun.utility.game.MessageUtility;
import net.minecraft.text.Text;

public class ConfigManager {
   private static final String ACTIVE_CONFIG_FILENAME = "active_config.txt";
   private final List<ConfigFile> configFiles = new ArrayList<>();
   private ConfigFile current;
   private boolean initialized = false;

   public void handle() {
      if (!this.initialized) {
         this.scanConfigDirectory();
         this.initialized = true;
      }

      String savedActive = this.getActiveConfigName();
      ConfigFile target = null;
      if (savedActive != null && !savedActive.isBlank() && !savedActive.equalsIgnoreCase("autosave")) {
         target = this.getConfig(savedActive, false);
      }

      if (target != null) {
         this.current = target;
         target.load();
         this.current = target;
         Sun.LOGGER.info("Restored active config: {}", target.getFileName());
      } else {
         ConfigFile autosave = this.getConfig("autosave", false);
         if (autosave == null) {
            autosave = this.createConfig("autosave");
         } else {
            autosave.load();
         }
         this.current = null;
      }
   }

   public String getActiveConfigName() {
      try {
         File configFile = new File(FileManager.DIRECTORY, ACTIVE_CONFIG_FILENAME);
         if (configFile.exists()) {
            String name = Files.readString(configFile.toPath()).trim();
            if (!name.isEmpty()) {
               return name;
            }
         }
      } catch (Exception e) {
         Sun.LOGGER.warn("Failed to read active config file: {}", e.getMessage());
      }
      return null;
   }

   public void saveActiveConfigName(String name) {
      try {
         if (!FileManager.DIRECTORY.exists()) {
            FileManager.DIRECTORY.mkdirs();
         }
         File configFile = new File(FileManager.DIRECTORY, ACTIVE_CONFIG_FILENAME);
         Files.writeString(configFile.toPath(), name != null ? name : "autosave",
            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
      } catch (Exception e) {
         Sun.LOGGER.error("Failed to write active config file: {}", e.getMessage());
      }
   }

   public void directionConfig() {
      try {
         File configDir = new File(FileManager.DIRECTORY, "configs");
         if (!configDir.exists()) {
            configDir.mkdirs();
         }
         net.minecraft.util.Util.getOperatingSystem().open(configDir);
      } catch (Exception var3) {
         Sun.LOGGER.error("Не удалось открыть папку с конфигами: {}", var3.getMessage());
      }
   }

   public ConfigFile createConfig(String name) {
      if (name != null) {
         this.refresh();
         ConfigFile config = new ConfigFile(name);
         config.save();
         this.configFiles.add(config);
         if (!name.equalsIgnoreCase("autosave")) {
            this.setCurrent(config);
         }
         return config;
      }
      return null;
   }

   public void listConfigs() {
      this.refresh();
      MessageUtility.info(Text.of("Список конфигов:"));

      for (ConfigFile configFile : this.configFiles) {
         int idx = this.configFiles.indexOf(configFile) + 1;
         MessageUtility.info(Text.of("[" + idx + "] " + configFile.getFileName()));
      }
   }

   private void scanConfigDirectory() {
      this.configFiles.clear();
      Path configPath = Paths.get(FileManager.DIRECTORY.getPath(), "configs");
      if (!Files.exists(configPath)) {
         try {
            Files.createDirectories(configPath);
         } catch (IOException var5) {
            Sun.LOGGER.error("Не удалось создать директорию конфигов: {}", var5.getMessage());
         }
      } else {
         try (Stream<Path> stream = Files.list(configPath)) {
            stream.filter(Files::isRegularFile).filter(path -> {
               String s = path.toString();
               return s.endsWith(".sun") || s.endsWith(".rock");
            }).forEach(path -> {
               String fileName = path.getFileName().toString();
               String name = fileName.substring(0, fileName.lastIndexOf('.'));
               if (this.configFiles.stream().noneMatch(c -> c.getFileName().equalsIgnoreCase(name))) {
                  ConfigFile configFile = new ConfigFile(name);
                  this.configFiles.add(configFile);
               }
            });
         } catch (IOException var8) {
            Sun.LOGGER.error("Ошибка при сканировании директории конфигов: {}", var8.getMessage());
         }
      }

      if (this.current != null) {
         this.current = this.configFiles.stream()
               .filter(c -> c.getFileName().equalsIgnoreCase(this.current.getFileName()))
               .findFirst()
               .orElse(this.current);
      } else {
         String saved = this.getActiveConfigName();
         if (saved != null && !saved.isBlank() && !saved.equalsIgnoreCase("autosave")) {
            this.configFiles.stream()
                  .filter(c -> c.getFileName().equalsIgnoreCase(saved))
                  .findFirst()
                  .ifPresent(c -> this.current = c);
         }
      }
   }

   public boolean isCurrent(ConfigFile cfg) {
      if (cfg == null || this.current == null) {
         return false;
      }
      return this.current.getFileName().equalsIgnoreCase(cfg.getFileName());
   }

   public ConfigFile getConfig(String name, boolean rescan) {
      if (rescan) {
         this.scanConfigDirectory();
      }

      return this.configFiles.stream().filter(configFile -> configFile.getFileName().equalsIgnoreCase(name)).findFirst().orElse(null);
   }

   public ConfigFile getConfig(String name) {
      return this.getConfig(name, false);
   }

   public ConfigFile getAutoSaveConfig() {
      return this.current != null ? this.current : this.getConfig("autosave", false);
   }

   public void refresh() {
      this.scanConfigDirectory();
   }

   @Generated
   public List<ConfigFile> getConfigFiles() {
      return this.configFiles;
   }

   @Generated
   public ConfigFile getCurrent() {
      return this.current;
   }

   @Generated
   public boolean isInitialized() {
      return this.initialized;
   }

   @Generated
   public void setCurrent(ConfigFile current) {
      this.current = current;
      String name = (current != null && !current.getFileName().equalsIgnoreCase("autosave"))
         ? current.getFileName()
         : "autosave";
      this.saveActiveConfigName(name);
   }
}
