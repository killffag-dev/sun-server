package naryn.sun.ui.menu.layout;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import naryn.sun.Sun;
import naryn.sun.systems.file.FileManager;

public class MenuLayoutManager {

   private static MenuLayoutManager instance;

   private final File file;
   private MenuLayoutData data;

   private MenuLayoutManager() {
      File target = new File(FileManager.DIRECTORY, "menu_layout_v2.sun");
      File legacy = new File(FileManager.DIRECTORY, "menu_layout_v2.rock");
      if (!target.exists() && legacy.exists()) {
         legacy.renameTo(target);
      }
      this.file = target;
      this.load();
   }

   public static MenuLayoutManager getInstance() {
      if (instance == null) {
         instance = new MenuLayoutManager();
      }

      return instance;
   }

   public MenuLayoutData getData() {
      return this.data;
   }

   public void load() {
      if (!this.file.exists()) {
         this.data = MenuLayoutData.createDefault();
         return;
      }

      try (FileReader reader = new FileReader(this.file)) {
         MenuLayoutData loaded = FileManager.GSON.fromJson(reader, MenuLayoutData.class);
         this.data = loaded != null ? loaded : MenuLayoutData.createDefault();
      } catch (Exception e) {
         Sun.LOGGER.error("Не удалось загрузить menu_layout_v2.sun: {}", e.getMessage());
         this.data = MenuLayoutData.createDefault();
      }
   }

   public void save() {
      try {
         if (!this.file.exists() && !this.file.createNewFile()) {
            throw new IOException("Не удалось создать файл: " + this.file.getAbsolutePath());
         }

         try (FileWriter writer = new FileWriter(this.file)) {
            writer.write(FileManager.GSON.toJson(this.data));
         }
      } catch (IOException e) {
         Sun.LOGGER.error("Не удалось сохранить menu_layout_v2.sun: {}", e.getMessage());
      }
   }

   public void resetToDefault() {
      if (this.data != null) {
         this.data.reset(naryn.sun.utility.interfaces.IScaledResolution.sr.getScaledWidth(), naryn.sun.utility.interfaces.IScaledResolution.sr.getScaledHeight());
      } else {
         this.data = MenuLayoutData.createDefault();
      }
      this.save();
   }
}