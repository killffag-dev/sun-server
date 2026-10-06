package naryn.sun.ui.menu.layout;

import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import naryn.sun.Sun;
import naryn.sun.systems.file.FileManager;
import naryn.sun.systems.modules.impl.PositionableHudModule;

public class HudModuleLayoutManager {

   public static class Entry {
      public float x;
      public float y;
      public float scale;
   }

   private static final Type MAP_TYPE = new TypeToken<Map<String, Entry>>() {}.getType();

   private static HudModuleLayoutManager instance;

   private final File file;
   private Map<String, Entry> data = new HashMap<>();

   private HudModuleLayoutManager() {
      File target = new File(FileManager.DIRECTORY, "hud_modules_layout.sun");
      File legacy = new File(FileManager.DIRECTORY, "hud_modules_layout.rock");
      if (!target.exists() && legacy.exists()) {
         legacy.renameTo(target);
      }
      this.file = target;
      this.load();
   }

   public static HudModuleLayoutManager getInstance() {
      if (instance == null) {
         instance = new HudModuleLayoutManager();
      }

      return instance;
   }

   public void load() {
      if (!this.file.exists()) {
         this.data = new HashMap<>();
         return;
      }

      try (FileReader reader = new FileReader(this.file)) {
         Map<String, Entry> loaded = FileManager.GSON.fromJson(reader, MAP_TYPE);
         this.data = loaded != null ? loaded : new HashMap<>();
      } catch (Exception e) {
         Sun.LOGGER.error("Не удалось загрузить hud_modules_layout.sun: {}", e.getMessage());
         this.data = new HashMap<>();
      }
   }

   public void save() {
      try {
         if (!this.file.exists() && !this.file.createNewFile()) {
            throw new IOException("Не удалось создать файл: " + this.file.getAbsolutePath());
         }

         try (FileWriter writer = new FileWriter(this.file)) {
            writer.write(FileManager.GSON.toJson(this.data, MAP_TYPE));
         }
      } catch (IOException e) {
         Sun.LOGGER.error("Не удалось сохранить hud_modules_layout.sun: {}", e.getMessage());
      }
   }

   /**
    * Вызывается из конструктора PositionableHudModule — применяет сохранённую
    * позицию/масштаб к модулю, если для него есть запись.
    */
   public void applyTo(PositionableHudModule module) {
      Entry e = this.data.get(module.getName());
      if (e != null) {
         module.setEditorPos(e.x, e.y);
         module.setEditorScale(e.scale <= 0.0F ? 1.0F : e.scale);
      }
   }

   /**
    * Сохраняет позиции всех переданных модулей, у которых есть кастомная позиция.
    * Модули без изменений (все еще на дефолтной позиции) в файл не пишутся.
    */
   public void saveAll(List<PositionableHudModule> modules) {
      Map<String, Entry> map = new HashMap<>();
      for (PositionableHudModule module : modules) {
         if (module.hasCustomPosition()) {
            Entry e = new Entry();
            e.x = module.getLayoutX();
            e.y = module.getLayoutY();
            e.scale = module.getLayoutScale();
            map.put(module.getName(), e);
         }
      }

      this.data = map;
      this.save();
   }

   public void resetAll(List<PositionableHudModule> modules) {
      for (PositionableHudModule module : modules) {
         module.resetLayout();
      }

      this.data = new HashMap<>();
      this.save();
   }
}