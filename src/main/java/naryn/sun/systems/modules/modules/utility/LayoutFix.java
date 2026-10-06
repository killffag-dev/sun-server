package naryn.sun.systems.modules.modules.utility;

import naryn.sun.systems.modules.api.ModuleCategory;
import naryn.sun.systems.modules.api.ModuleInfo;
import naryn.sun.systems.modules.impl.BaseModule;

import java.util.HashMap;
import java.util.Map;

@ModuleInfo(name = "Layout Fix", category = ModuleCategory.UTILITY)
public class LayoutFix extends BaseModule {

    // ЙЦУКЕН -> QWERTY, совпадающие физические клавиши (нижний регистр).
    private static final Map<Character, Character> RU_TO_EN = new HashMap<>();

    static {
        String ru = "йцукенгшщзхъфывапролджэячсмитьбю.";
        String en = "qwertyuiop[]asdfghjkl;'zxcvbnm,./";
        for (int i = 0; i < ru.length(); i++) {
            RU_TO_EN.put(ru.charAt(i), en.charAt(i));
        }
    }

    // Конвертирует строку через таблицу раскладки. Первый символ должен быть
    // '.' (реальная опечатка раскладки) или уже '/' (частично верно набрано) —
    // в обоих случаях результат начинается с '/'. Пробелы и цифры дальше не
    // трогает (аргументы команд вроде координат). Возвращает null, если
    // строка не подходит (не тот первый символ либо что-то не мапится).
    public static String convert(String input) {
        if (input.isEmpty()) {
            return null;
        }

        char first = input.charAt(0);
        if (first != '.' && first != '/') {
            return null;
        }

        StringBuilder result = new StringBuilder(input.length());
        result.append('/');

        for (int i = 1; i < input.length(); i++) {
            char c = input.charAt(i);

            if (c == ' ' || Character.isDigit(c)) {
                result.append(c);
                continue;
            }

            Character mapped = RU_TO_EN.get(Character.toLowerCase(c));
            if (mapped == null) {
                return null;
            }

            result.append(Character.isUpperCase(c) ? Character.toUpperCase(mapped) : mapped);
        }

        return result.toString();
    }
}