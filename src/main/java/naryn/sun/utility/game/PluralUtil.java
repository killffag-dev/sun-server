package naryn.sun.utility.game;

import naryn.sun.systems.localization.Language;
import naryn.sun.systems.localization.Localizator;

/**
 * Утилиты для склонения существительных по числу
 * в разных языках (RU, EN, UA, PL).
 * Вынесены из TextUtility для чистоты разделения обязанностей.
 */
public final class PluralUtil {

    public static String makeGender(String parent) {
        if (parent.endsWith("а")) {
            return "а";
        } else if (parent.endsWith("a")) {
            return "а";
        } else if (parent.endsWith("y")) {
            return "о";
        } else if (parent.endsWith("ю")) {
            return "o";
        } else if (parent.endsWith("u")) {
            return "o";
        } else if (parent.endsWith("я")) {
            return "а";
        } else if (parent.endsWith("ы")) {
            return "ы";
        } else {
            return parent.endsWith("и") ? "ы" : "";
        }
    }

    public static String makeCount(float count) {
        double abs = Math.abs(count);
        long integerPart = (long) Math.floor(abs);
        double frac = abs - integerPart;
        if (frac > 1.0E-9) {
            return "а";
        } else {
            int n = (int) (integerPart % 100L);
            if (n >= 11 && n <= 14) {
                return "ов";
            } else {
                return switch (n % 10) {
                    case 1 -> "";
                    case 2, 3, 4 -> "а";
                    default -> "ов";
                };
            }
        }
    }

    public static String makeCountTranslated(float count) {
        Language currentLanguage = Localizator.getCurrentLanguage();

        return switch (currentLanguage) {
            case RU_RU -> makeCountRu(count);
            case EN_US -> makeCountEn(count);
        };
    }

    private static String makeCountRu(float count) {
        double abs = Math.abs(count);
        long integerPart = (long) Math.floor(abs);
        double frac = abs - integerPart;
        if (frac > 1.0E-9) {
            return "а";
        } else {
            int n = (int) (integerPart % 100L);
            if (n >= 11 && n <= 14) {
                return "ов";
            } else {
                return switch (n % 10) {
                    case 1 -> "";
                    case 2, 3, 4 -> "а";
                    default -> "ов";
                };
            }
        }
    }

    private static String makeCountUa(float count) {
        double abs = Math.abs(count);
        long integerPart = (long) Math.floor(abs);
        double frac = abs - integerPart;
        if (frac > 1.0E-9) {
            return "и";
        } else {
            int n = (int) (integerPart % 100L);
            if (n >= 11 && n <= 14) {
                return "ів";
            } else {
                return switch (n % 10) {
                    case 1 -> "";
                    case 2, 3, 4 -> "и";
                    default -> "ів";
                };
            }
        }
    }

    private static String makeCountPl(float count) {
        double abs = Math.abs(count);
        long integerPart = (long) Math.floor(abs);
        double frac = abs - integerPart;
        if (frac > 1.0E-9) {
            return "y";
        } else if (integerPart == 1L) {
            return "";
        } else {
            return integerPart >= 2L && integerPart <= 4L ? "y" : "ów";
        }
    }

    private static String makeCountEn(float count) {
        double abs = Math.abs(count);
        return abs == 1.0 ? "" : "s";
    }

    private PluralUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
