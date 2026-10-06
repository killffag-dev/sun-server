package naryn.sun.ui.menu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Хелпер для мультиязычного поиска модулей:
 * 1. Конвертация раскладки клавиатуры (RU <-> EN QWERTY, например: ылн -> sky, фгкф -> aura).
 * 2. Фонетическая транслитерация и синонимы (например: скай -> sky, аим -> aim, небо -> sky).
 */
public final class SearchTextHelper {

    private static final Map<Character, Character> RU_TO_EN_LAYOUT = new HashMap<>();
    private static final Map<Character, Character> EN_TO_RU_LAYOUT = new HashMap<>();

    private static final String RU_KEYS = "йцукенгшщзхъфывапролджэячсмитьбюё\"№;:?./\\ЙЦУКЕНГШЩЗХЪФЫВАПРОЛДЖЭЯЧСМИТЬБЮЁ";
    private static final String EN_KEYS = "qwertyuiop[]asdfghjkl;'zxcvbnm,.~@#&|/QWERTYUIOP{}ASDFGHJKL:\"ZXCVBNM<>~";

    private static final Map<String, String[]> SYNONYMS = new HashMap<>();

    static {
        int len = Math.min(RU_KEYS.length(), EN_KEYS.length());
        for (int i = 0; i < len; i++) {
            char ru = RU_KEYS.charAt(i);
            char en = EN_KEYS.charAt(i);
            RU_TO_EN_LAYOUT.put(ru, en);
            EN_TO_RU_LAYOUT.put(en, ru);
        }

        // Синонимы и фонетические соответствия (RU <-> EN)
        // Синонимы и фонетические соответствия (RU <-> EN)
        addSynonym("sky", "небо", "скай", "облака", "небеса");
        addSynonym("aim", "аим", "прицел", "наводка", "aimbot");
        addSynonym("kill", "килл", "убийство", "смерть", "killeffects");
        addSynonym("speed", "спид", "скорость", "бег", "быстро");
        addSynonym("fly", "флай", "полет", "полёт", "летать");
        addSynonym("zoom", "зум", "приближение", "бинокль");
        addSynonym("sprint", "спринт", "автоспринт", "бег");
        addSynonym("hud", "худ", "интерфейс", "панель");
        addSynonym("hitbox", "хитбокс", "боксы", "коробки", "размер");
        addSynonym("hitsound", "хитсаунд", "звук удара", "звуки");
        addSynonym("hitcolor", "хитколор", "цвет удара", "урон");
        addSynonym("inventory", "инвентарь", "вещи", "предметы", "инв");
        addSynonym("inventoryview", "вид инвентаря", "инвентарь");
        addSynonym("armor", "броня", "армор", "прочность");
        addSynonym("potion", "зелья", "поушены", "эффекты", "статус");
        addSynonym("esp", "еsp", "подсветка", "вх", "вижн");
        addSynonym("target", "таргет", "цель", "фокус");
        addSynonym("friend", "друг", "друзья", "тимейт");
        addSynonym("fakeplayer", "фейк", "фантом", "клон", "бот");
        addSynonym("autoeat", "автоеда", "еда", "кушать", "пища");
        addSynonym("autoleave", "автолив", "выход", "лив");
        addSynonym("freecam", "свободная камера", "фрикэмера", "камера");
        addSynonym("freelook", "свободный обзор", "обзор");
        addSynonym("keystrokes", "кейстроксы", "клавиши", "кнопки");
        addSynonym("motionblur", "моушенблюр", "размытие", "блюр");
        addSynonym("music", "музыка", "треки", "плеер");
        addSynonym("shulker", "шалкер", "шалкера", "просмотр");
        addSynonym("tnt", "тнт", "динамит", "бомба");
        addSynonym("trails", "трейлы", "следы", "хвосты", "полосы");
        addSynonym("click", "клик", "нажатие");
        addSynonym("mine", "шахта", "копание", "бурение");
        addSynonym("protect", "защита", "скрытие", "аноним");
        addSynonym("cleaner", "очистка", "клинер");
    }

    private static void addSynonym(String key, String... values) {
        SYNONYMS.put(key.toLowerCase(), values);
    }

    private SearchTextHelper() {}

    /**
     * Конвертирует строку из русской раскладки в английскую (например ылн -> sky).
     */
    public static String convertRuToEnLayout(String input) {
        if (input == null || input.isEmpty()) return "";
 StringBuilder sb = new StringBuilder(input.length());
 for (int i = 0; i < input.length(); i++) {
 char c = input.charAt(i);
 sb.append(RU_TO_EN_LAYOUT.getOrDefault(c, c));
 }
 return sb.toString();
 }

 /**
 * Конвертирует строку из английской раскладки в русскую (например sky -> ылн).
 */
 public static String convertEnToRuLayout(String input) {
 if (input == null || input.isEmpty()) return "";
 StringBuilder sb = new StringBuilder(input.length());
 for (int i = 0; i < input.length(); i++) {
 char c = input.charAt(i);
 sb.append(EN_TO_RU_LAYOUT.getOrDefault(c, c));
 }
 return sb.toString();
 }

 /**
 * Транслитерация RU -> EN (например: скай -> skai / sky).
 */
 public static String transliterateRuToEn(String input) {
 if (input == null || input.isEmpty()) return "";
 StringBuilder sb = new StringBuilder();
 String s = input.toLowerCase();
 for (int i = 0; i < s.length(); i++) {
 char c = s.charAt(i);
 switch (c) {
 case 'а': sb.append('a'); break;
 case 'б': sb.append('b'); break;
 case 'в': sb.append('v'); break;
 case 'г': sb.append('g'); break;
 case 'д': sb.append('d'); break;
 case 'е': case 'ё': case 'э': sb.append('e'); break;
 case 'ж': sb.append("zh"); break;
 case 'з': sb.append('z'); break;
 case 'и': sb.append('i'); break;
 case 'й': sb.append('y'); break;
 case 'к': sb.append('k'); break;
 case 'л': sb.append('l'); break;
 case 'м': sb.append('m'); break;
 case 'н': sb.append('n'); break;
 case 'о': sb.append('o'); break;
 case 'п': sb.append('p'); break;
 case 'р': sb.append('r'); break;
 case 'с': sb.append('s'); break;
 case 'т': sb.append('t'); break;
 case 'у': sb.append('u'); break;
 case 'ф': sb.append('f'); break;
 case 'х': sb.append("kh"); break;
 case 'ц': sb.append("ts"); break;
 case 'ч': sb.append("ch"); break;
 case 'ш': sb.append("sh"); break;
 case 'щ': sb.append("shch"); break;
 case 'ы': sb.append('y'); break;
 case 'ю': sb.append("yu"); break;
 case 'я': sb.append("ya"); break;
 case 'ь': case 'ъ': break;
 default: sb.append(c); break;
 }
 }
 return sb.toString();
 }

 /**
 * Возвращает список всех поисковых вариантов запроса (оригинал, другая раскладка, транслит, синонимы).
 */
 public static List<String> getSearchVariants(String query) {
 List<String> variants = new ArrayList<>();
 if (query == null || query.isBlank()) return variants;

 String q = query.toLowerCase().trim();
 variants.add(q);

 String enLayout = convertRuToEnLayout(q).toLowerCase();
 if (!enLayout.equals(q)) variants.add(enLayout);

 String ruLayout = convertEnToRuLayout(q).toLowerCase();
 if (!ruLayout.equals(q) && !ruLayout.equals(enLayout)) variants.add(ruLayout);

 String translit = transliterateRuToEn(q);
 if (!translit.equals(q) && !translit.equals(enLayout)) variants.add(translit);

 // Фонетические вариации для окончания ай -> y (скай -> sky)
 if (q.endsWith("\u0430\u0439")) {
 variants.add(transliterateRuToEn(q.substring(0, q.length() - 2)) + "y");
 }

 // Проверяем синонимы
 for (Map.Entry<String, String[]> entry : SYNONYMS.entrySet()) {
 String key = entry.getKey();
 if (key.contains(q) || q.contains(key) || enLayout.contains(key)) {
 variants.add(key);
 for (String val : entry.getValue()) variants.add(val);
 }
 for (String val : entry.getValue()) {
 if (val.contains(q) || q.contains(val)) {
 variants.add(key);
 variants.add(val);
 }
 }
 }

 return variants;
 }
}