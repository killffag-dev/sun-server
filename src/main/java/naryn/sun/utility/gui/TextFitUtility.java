package naryn.sun.utility.gui;

import naryn.sun.framework.msdf.Font;

import java.util.List;
import java.util.function.Function;

public final class TextFitUtility {

    private TextFitUtility() {
    }

    public record Result(List<String> lines, Font font) {
    }

    private static final float STEP = 0.25F;

    public static Result fit(Function<Float, Font> fontProvider, String text,
                              float maxWidth, float maxHeight,
                              float maxFontSize, float minFontSize, float lineGap) {
        float size = maxFontSize;
        Result last = null;
        while (size >= minFontSize) {
            Font font = fontProvider.apply(size);
            List<String> lines = TextWrapUtility.wrap(font, text, maxWidth);
            float totalHeight = lines.size() * font.height() + Math.max(0, lines.size() - 1) * lineGap;
            last = new Result(lines, font);
            if (totalHeight <= maxHeight) {
                return last;
            }
            size -= STEP;
        }
        return last;
    }
}