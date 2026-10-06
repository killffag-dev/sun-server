package naryn.sun.utility.game;

import com.ibm.icu.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.util.Date;
import java.util.Locale;
import lombok.Generated;
import naryn.sun.systems.localization.Localizator;
import naryn.sun.utility.interfaces.IMinecraft;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Текстовые утилиты: форматирование чисел, дат, клавиш, talisman-текст.
 * Генерация ников — {@link NickGenerator}.
 * Склонения по числу — {@link PluralUtil}.
 */
public final class TextUtility implements IMinecraft {

    private static final String STAR_TOKEN = "[★]";
    private static final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());

    // ==================== Делегирующие методы для обратной совместимости ====================

    @Deprecated
    public static String getRandomNick() {
        return NickGenerator.generate();
    }

    @Deprecated
    public static String makeGender(String parent) {
        return PluralUtil.makeGender(parent);
    }

    @Deprecated
    public static String makeCount(float count) {
        return PluralUtil.makeCount(count);
    }

    @Deprecated
    public static String makeCountTranslated(float count) {
        return PluralUtil.makeCountTranslated(count);
    }

    // ==================== Форматирование чисел ====================

    public static String formatNumberClean(double number) {
        if (number == (int) number) {
            return String.valueOf((int) number);
        } else {
            String formatted = String.format("%.1f", number).replace(",", ".").replaceAll("\\.?0+$", "");
            return formatted.endsWith(".") ? formatted.replace(".", "") : formatted;
        }
    }

    public static String formatNumber(double number) {
        return String.format("%.1f", number).replace(",", ".");
    }

    // ==================== Клавиши ====================

    public static String getKeyName(int key) {
        if (key >= 0 && key <= 7) {
            return switch (key) {
                case 0 -> Localizator.translate("key.mouse.lmb");
                case 1 -> Localizator.translate("key.mouse.rmb");
                case 2 -> Localizator.translate("key.mouse.wheel");
                case 3 -> "MOUSE4";
                case 4 -> "MOUSE5";
                case 5 -> "MOUSE6";
                case 6 -> "MOUSE7";
                case 7 -> "MOUSE8";
                default -> "MOUSE" + key;
            };
        } else if (key <= -1) {
            return Localizator.translate("key.none");
        } else {
            String name = InputUtil.fromKeyCode(key, -1).getTranslationKey();
            name = name.replace("key.keyboard.", "")
                .replace("key.", "")
                .replace(".", "")
                .replace("left", "l")
                .replace("right", "r")
                .replace("printscreen", "prtsc")
                .replace("graveaccent", "grave")
                .replace("control", "ctrl");
            return name.toUpperCase();
        }
    }

    // ==================== Дата / Время ====================

    public static String getCurrentTime() {
        return sdf.format(new Date());
    }

    public static String getFormattedDate() {
        LocalDate currentDate = LocalDate.now();
        String[] daysOfWeek = new String[]{
            "time.days.monday", "time.days.tuesday", "time.days.wednesday", "time.days.thursday",
            "time.days.friday", "time.days.saturday", "time.days.sunday"
        };
        String[] months = new String[]{
            "time.months.january", "time.months.february", "time.months.march",
            "time.months.april", "time.months.may", "time.months.june",
            "time.months.july", "time.months.august", "time.months.september",
            "time.months.october", "time.months.november", "time.months.december"
        };
        DayOfWeek dayOfWeek = currentDate.getDayOfWeek();
        String russianDay = Localizator.translate(daysOfWeek[dayOfWeek.getValue() - 1]);
        int dayOfMonth = currentDate.getDayOfMonth();
        Month month = currentDate.getMonth();
        String russianMonth = Localizator.translate(months[month.getValue() - 1]);
        return String.format("%s, %d %s", russianDay, dayOfMonth, russianMonth);
    }

    // ==================== Прочее ====================

    public static void copyText(String text) {
        mc.keyboard.setClipboard(text);
    }

    public static MutableText formatTalisman(String input) {
        if (input.startsWith("[★]")) {
            String rest = input.substring("[★]".length());
            MutableText redStar = Text.literal("[★]").formatted(Formatting.RED);
            MutableText orangeText = Text.literal(rest).formatted(Formatting.GOLD);
            return Text.literal("").append(redStar).append(orangeText);
        } else {
            return Text.literal(input);
        }
    }

    @Generated
    private TextUtility() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
