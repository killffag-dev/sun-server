package naryn.sun.systems.localization;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import lombok.Generated;
import naryn.sun.Sun;
import ru.kotopushka.compiler.sdk.annotations.VMProtect;
import ru.kotopushka.compiler.sdk.enums.VMProtectType;

public final class Localizator {
   private static final Language DEFAULT_LANG = Language.RU_RU;
   private static Language currentLanguage = DEFAULT_LANG;
   private static final Map<Language, Map<String, String>> allLanguageTranslations = new HashMap<>();
   private static final Map<String, String> translations = new HashMap<>();

   private static final String[] SUB_LANG_FILES = {
      "ui/ui.lang",
      "modules/modules.lang",
      "settings/settings.lang",
      "descriptions/descriptions.lang",
      "descriptions/setting_descriptions.lang"
   };

   public static void loadTranslations() {
      translations.clear();
      for (Language lang : Language.values()) {
         loadLanguage(lang);
      }
      Map<String, String> current = allLanguageTranslations.get(currentLanguage);
      if (current != null) {
         translations.putAll(current);
      }
   }

   private static void loadLanguage(Language lang) {
      Map<String, String> map = allLanguageTranslations.computeIfAbsent(lang, k -> new HashMap<>());
      map.clear();
      for (String subFile : SUB_LANG_FILES) {
         String resourcePath = "/assets/" + Sun.MOD_ID + "/lang/" + lang.getCode() + "/" + subFile;
         InputStream is = Localizator.class.getResourceAsStream(resourcePath);
         if (is != null) {
            loadFromStream(is, resourcePath, map);
         }
      }
   }

   private static void loadFromStream(InputStream inputStream, String sourceName, Map<String, String> targetMap) {
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
         int lineNumber = 0;
         String line;
         while ((line = reader.readLine()) != null) {
            lineNumber++;
            line = removeComments(line).trim();
            if (!line.isEmpty()) {
               parseLine(line, lineNumber, sourceName, targetMap);
            }
         }
      } catch (IOException e) {
         Sun.LOGGER.error("Failed to load translation stream from {}", sourceName, e);
      }
   }

   public static void setLanguage(@Nonnull Language lang) {
      currentLanguage = lang;
      loadTranslations();
   }

   public static String translate(String key) {
      return translations.getOrDefault(key, key);
   }

   public static String translate(Language lang, String key) {
      Map<String, String> map = allLanguageTranslations.get(lang);
      if (map != null && map.containsKey(key)) {
         return map.get(key);
      }
      return translations.getOrDefault(key, key);
   }

   public static String translateOrDefault(String key, String defaultValue) {
      return translations.getOrDefault(key, defaultValue);
   }

   public static String translate(String key, Object... args) {
      String format = translations.getOrDefault(key, key);
      return String.format(format, args);
   }

   public static String translateOrEmpty(String key) {
      return translations.getOrDefault(key, " ");
   }

   @VMProtect(type = VMProtectType.MUTATION)
   private static void parseLine(String line, int lineNumber, String fileName, Map<String, String> targetMap) {
      int equalIndex = line.indexOf(61);
      if (equalIndex == -1) {
         Sun.LOGGER.warn("Warning: Invalid line format at line {} in {}: {}", new Object[]{lineNumber, fileName, line});
      } else {
         String key = line.substring(0, equalIndex).trim();
         String value = line.substring(equalIndex + 1).trim();
         if (key.isEmpty()) {
            Sun.LOGGER.warn("Warning: Empty key at line {} in {}", lineNumber, fileName);
         } else {
            targetMap.put(key, value);
         }
      }
   }

   private static String removeComments(String line) {
      int commentIndex = line.indexOf("#");
      return commentIndex != -1 ? line.substring(0, commentIndex) : line;
   }

   @Generated
   private Localizator() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }

   @Generated
   public static Language getCurrentLanguage() {
      return currentLanguage;
   }
}
