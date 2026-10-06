package naryn.sun.utility.gui;

import naryn.sun.framework.msdf.Font;

import java.util.ArrayList;
import java.util.List;

public final class TextWrapUtility {

    private TextWrapUtility() {
    }

    public static List<String> wrap(Font font, String text, float maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            lines.add("");
            return lines;
        }
        if (maxWidth <= 0.0F) {
            lines.add(text);
            return lines;
        }
        String[] words = text.split(" ");
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            String candidate = current.length() == 0 ? word : current + " " + word;
            if (font.width(candidate) > maxWidth && current.length() > 0) {
                lines.add(current.toString());
                current = new StringBuilder(word);
            } else {
                current = new StringBuilder(candidate);
            }
        }
        lines.add(current.toString());
        return lines;
    }
}