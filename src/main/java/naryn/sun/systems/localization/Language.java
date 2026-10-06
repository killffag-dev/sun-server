package naryn.sun.systems.localization;

import lombok.Generated;

public enum Language {
   EN_US("en_us"),
   RU_RU("ru_ru");

   private final String code;

   private Language(String code) {
      this.code = code;
   }

   @Generated
   public String getCode() {
      return this.code;
   }
}
