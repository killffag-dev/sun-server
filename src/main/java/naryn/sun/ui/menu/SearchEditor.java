package naryn.sun.ui.menu;

import naryn.sun.framework.base.UIContext;
import naryn.sun.framework.msdf.Font;
import naryn.sun.framework.objects.BorderRadius;
import naryn.sun.framework.objects.MouseButton;
import naryn.sun.systems.localization.Localizator;
import naryn.sun.systems.theme.ClientAppearance;
import naryn.sun.ui.menu.skin.MenuSkin;
import naryn.sun.utility.colors.ColorRGBA;
import naryn.sun.utility.colors.Colors;
import naryn.sun.utility.game.cursor.CursorType;
import naryn.sun.utility.game.cursor.CursorUtility;
import naryn.sun.utility.gui.GuiUtility;
import naryn.sun.utility.render.ScissorUtility;
import naryn.sun.utility.sounds.ClientSoundManager;
import naryn.sun.utility.time.Timer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.math.MathHelper;

/**
 * Полноценный текстовый редактор для поисковой строки в ClickGUI:
 * - Кнопка сброса (✕) в правой части;
 * - Выделение текста мышью и Ctrl+A;
 * - Копирование, вставка и вырезание (Ctrl+C, Ctrl+V, Ctrl+X);
 * - Удаление по словам (Ctrl+Backspace, Ctrl+Delete);
 * - Навигация стрелками (с Ctrl — по словам, с Shift — выделение);
 * - Клик мышью в любую точку строки для позиционирования курсора.
 */
public class SearchEditor {

    private String text = "";
 private int cursor = 0;
 private int selectionStart = -1;
 private int selectionEnd = -1;
 private boolean focused = false;

 private int dragAnchor = -1;
 private long lastClickTime = 0L;
 private int clickCount = 0;
 private final Timer typingTimer = new Timer();

 private static final float CLEAR_BTN_SIZE = 14.0F;
 private static final float CLEAR_BTN_PAD_RIGHT = 5.0F;

 public String getText() {
 return text;
 }

 public void setText(String text) {
 this.text = text != null ? text : "";
        this.cursor = MathHelper.clamp(this.cursor, 0, this.text.length());
        this.clearSelection();
    }

    public boolean isFocused() {
        return focused;
    }

    public void setFocused(boolean focused) {
        this.focused = focused;
        if (!focused) {
            this.clearSelection();
            this.dragAnchor = -1;
        }
    }

    public void clear() {
        this.text = "";
 this.cursor = 0;
 this.clearSelection();
 }

 public boolean hasSelection() {
 return selectionStart >= 0 && selectionEnd >= 0 && selectionStart != selectionEnd;
 }

 public int getSelectionMin() {
 return Math.min(selectionStart, selectionEnd);
 }

 public int getSelectionMax() {
 return Math.max(selectionStart, selectionEnd);
 }

 public void clearSelection() {
 this.selectionStart = -1;
 this.selectionEnd = -1;
 }

 public void selectAll() {
 if (!text.isEmpty()) {
 this.selectionStart = 0;
 this.selectionEnd = text.length();
 this.cursor = text.length();
 }
 }

 public String getSelectedText() {
 if (!hasSelection()) return "";
        int min = getSelectionMin();
        int max = getSelectionMax();
        return text.substring(Math.min(min, text.length()), Math.min(max, text.length()));
    }

    public void deleteSelection() {
        if (!hasSelection()) return;
        int min = getSelectionMin();
        int max = getSelectionMax();
        this.text = text.substring(0, min) + text.substring(Math.min(max, text.length()));
        this.cursor = min;
        this.clearSelection();
    }

    public void insertText(String str) {
        if (str == null || str.isEmpty()) return;
        if (hasSelection()) {
            deleteSelection();
        }
        this.cursor = MathHelper.clamp(this.cursor, 0, this.text.length());
        this.text = text.substring(0, cursor) + str + text.substring(cursor);
        this.cursor += str.length();
        this.typingTimer.reset();
        ClientSoundManager.getInstance().playTyping();
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!focused) return false;

        MinecraftClient mc = MinecraftClient.getInstance();

        // Ctrl+A — Выделить всё
        if (Screen.isSelectAll(keyCode)) {
            selectAll();
            return true;
        }

        // Ctrl+C — Копировать
        if (Screen.isCopy(keyCode)) {
            String toCopy = hasSelection() ? getSelectedText() : text;
            if (!toCopy.isEmpty() && mc != null && mc.keyboard != null) {
                mc.keyboard.setClipboard(toCopy);
            }
            return true;
        }

        // Ctrl+X — Вырезать
        if (Screen.isCut(keyCode)) {
            if (hasSelection()) {
                String toCopy = getSelectedText();
                if (mc != null && mc.keyboard != null) {
                    mc.keyboard.setClipboard(toCopy);
                }
                deleteSelection();
                ClientSoundManager.getInstance().playTyping();
            } else if (!text.isEmpty()) {
                if (mc != null && mc.keyboard != null) {
                    mc.keyboard.setClipboard(text);
                }
                clear();
                ClientSoundManager.getInstance().playTyping();
            }
            return true;
        }

        // Ctrl+V — Вставить
        if (Screen.isPaste(keyCode)) {
            if (mc != null && mc.keyboard != null) {
                String clip = mc.keyboard.getClipboard();
                if (clip != null && !clip.isEmpty()) {
                    // Удаляем переводы строк
                    clip = clip.replace("\r", "").replace("\n", "");
                    insertText(clip);
                }
            }
            return true;
        }

        // Backspace (259)
        if (keyCode == 259) {
            if (hasSelection()) {
                deleteSelection();
                ClientSoundManager.getInstance().playTyping();
            } else if (cursor > 0) {
                if (Screen.hasControlDown()) {
                    int wordStart = findWordBoundary(cursor, false);
                    this.text = text.substring(0, wordStart) + text.substring(cursor);
                    this.cursor = wordStart;
                } else {
                    this.text = text.substring(0, cursor - 1) + text.substring(cursor);
                    this.cursor--;
                }
                ClientSoundManager.getInstance().playTyping();
            }
            this.typingTimer.reset();
            return true;
        }

        // Delete (261)
        if (keyCode == 261) {
            if (hasSelection()) {
                deleteSelection();
                ClientSoundManager.getInstance().playTyping();
            } else if (cursor < text.length()) {
                if (Screen.hasControlDown()) {
                    int wordEnd = findWordBoundary(cursor, true);
                    this.text = text.substring(0, cursor) + text.substring(wordEnd);
                } else {
                    this.text = text.substring(0, cursor) + text.substring(cursor + 1);
                }
                ClientSoundManager.getInstance().playTyping();
            }
            this.typingTimer.reset();
            return true;
        }

        // Left Arrow (263)
        if (keyCode == 263) {
            int newCursor = Screen.hasControlDown() ? findWordBoundary(cursor, false) : Math.max(0, cursor - 1);
            if (Screen.hasShiftDown()) {
                if (!hasSelection()) {
                    this.selectionStart = cursor;
                }
                this.selectionEnd = newCursor;
            } else {
                clearSelection();
            }
            this.cursor = newCursor;
            this.typingTimer.reset();
            return true;
        }

        // Right Arrow (262)
        if (keyCode == 262) {
            int newCursor = Screen.hasControlDown() ? findWordBoundary(cursor, true) : Math.min(text.length(), cursor + 1);
            if (Screen.hasShiftDown()) {
                if (!hasSelection()) {
                    this.selectionStart = cursor;
                }
                this.selectionEnd = newCursor;
            } else {
                clearSelection();
            }
            this.cursor = newCursor;
            this.typingTimer.reset();
            return true;
        }

        // Home (268)
        if (keyCode == 268) {
            if (Screen.hasShiftDown()) {
                if (!hasSelection()) this.selectionStart = cursor;
                this.selectionEnd = 0;
            } else {
                clearSelection();
            }
            this.cursor = 0;
            this.typingTimer.reset();
            return true;
        }

        // End (269)
        if (keyCode == 269) {
            if (Screen.hasShiftDown()) {
                if (!hasSelection()) this.selectionStart = cursor;
                this.selectionEnd = text.length();
            } else {
                clearSelection();
            }
            this.cursor = text.length();
            this.typingTimer.reset();
            return true;
        }

        // Escape (256) / Enter (257)
        if (keyCode == 257) {
            this.focused = false;
            return true;
        }

        return false;
    }

    public boolean charTyped(char chr, int modifiers) {
        if (!focused) return false;
        if (chr >= 32 && chr != 127) {
            insertText(String.valueOf(chr));
            return true;
        }
        return false;
    }

    public boolean mouseClicked(double mouseX, double mouseY, MouseButton button,
                                float searchX, float searchY, float searchW, float searchH, Font font) {
        boolean hovered = GuiUtility.isHovered(searchX, searchY, searchW, searchH, mouseX, mouseY);
        if (!hovered) {
            this.focused = false;
            this.clearSelection();
            return false;
        }

        if (button != MouseButton.LEFT) {
            return false;
        }

        this.focused = true;

        // Проверяем клик по крестику
        if (!text.isEmpty()) {
            float clearX = searchX + searchW - CLEAR_BTN_SIZE - CLEAR_BTN_PAD_RIGHT;
            float clearY = searchY + (searchH - CLEAR_BTN_SIZE) / 2.0F;
            if (GuiUtility.isHovered(clearX - 2.0F, clearY - 2.0F, CLEAR_BTN_SIZE + 4.0F, CLEAR_BTN_SIZE + 4.0F, mouseX, mouseY)) {
                clear();
                ClientSoundManager.getInstance().playButtonClick();
                return true;
            }
        }

        long now = System.currentTimeMillis();
        if (now - lastClickTime < 450L) {
            clickCount++;
        } else {
            clickCount = 1;
        }
        lastClickTime = now;

        float textLeft = searchX + 7.0F;
        float clickRelX = (float) mouseX - textLeft;
        int clickedIndex = getCharIndexAt(clickRelX, font);

        this.cursor = clickedIndex;

        if (clickCount == 2) {
            // Двойной клик — выделить слово
            selectWordAt(cursor);
            this.dragAnchor = -1;
        } else {
            this.clearSelection();
            this.dragAnchor = clickedIndex;
        }

        this.typingTimer.reset();
        return true;
    }

    public void mouseReleased(double mouseX, double mouseY, MouseButton button) {
        this.dragAnchor = -1;
    }

    public void mouseDragged(double mouseX, double mouseY, float searchX, float searchY, float searchW, float searchH, Font font) {
        if (!focused || dragAnchor == -1) return;

        float textLeft = searchX + 7.0F;
        float clickRelX = (float) mouseX - textLeft;
        int curIndex = getCharIndexAt(clickRelX, font);

        if (curIndex != dragAnchor) {
            this.selectionStart = dragAnchor;
            this.selectionEnd = curIndex;
            this.cursor = curIndex;
        } else {
            this.clearSelection();
            this.cursor = curIndex;
        }
    }

    private int getCharIndexAt(float relX, Font font) {
        if (text.isEmpty() || relX <= 0.0F) return 0;

        float curW = 0.0F;
        for (int i = 0; i < text.length(); i++) {
            float charW = font.width(text.substring(i, i + 1));
            if (relX < curW + charW / 2.0F) {
                return i;
            }
            curW += charW;
        }
        return text.length();
    }

    private int findWordBoundary(int fromIndex, boolean forward) {
        if (forward) {
            int i = fromIndex;
            while (i < text.length() && Character.isWhitespace(text.charAt(i))) i++;
            while (i < text.length() && !Character.isWhitespace(text.charAt(i))) i++;
            return i;
        } else {
            int i = fromIndex;
            while (i > 0 && Character.isWhitespace(text.charAt(i - 1))) i--;
            while (i > 0 && !Character.isWhitespace(text.charAt(i - 1))) i--;
            return i;
        }
    }

    private void selectWordAt(int index) {
        if (text.isEmpty()) return;
        int start = Math.max(0, Math.min(index, text.length() - 1));
        int end = start;

        while (start > 0 && Character.isLetterOrDigit(text.charAt(start - 1))) {
            start--;
        }
        while (end < text.length() && Character.isLetterOrDigit(text.charAt(end))) {
            end++;
        }

        if (start != end) {
            this.selectionStart = start;
            this.selectionEnd = end;
            this.cursor = end;
        }
    }

    public void render(UIContext context, float searchX, float searchY, float searchW, float searchH,
                       Font searchFont, float alpha, double mouseX, double mouseY) {
        MenuSkin skin = MenuSkin.current();

        // 1. Фон и фокус
        skin.renderRecessedChip(context, searchX, searchY, searchW, searchH, BorderRadius.all(6.0F), alpha);
        if (focused) {
            skin.renderSearchFocus(context, searchX, searchY, searchW, searchH, BorderRadius.all(6.0F), alpha);
        }

        float textLeft = searchX + 7.0F;
        float rightReserve = text.isEmpty() ? 7.0F : (CLEAR_BTN_SIZE + CLEAR_BTN_PAD_RIGHT + 4.0F);
        float visibleTextW = searchW - 7.0F - rightReserve;
        float textY = searchY + (searchH - searchFont.height()) / 2.0F + 0.5F;

        ScissorUtility.push(context.getMatrices(), textLeft, searchY, visibleTextW, searchH);

        if (text.isEmpty()) {
            // Плейсхолдер
            String placeholder = Localizator.translate("menu.search.placeholder");
            ColorRGBA phColor = ColorRGBA.WHITE.withAlpha((int) (130 * alpha));
            context.drawText(searchFont, placeholder, textLeft, textY, phColor);

            // Мигающий курсор на пустой строке
            if (focused) {
                boolean showCursor = !typingTimer.finished(350L) || (System.currentTimeMillis() % 1000L < 500L);
                if (showCursor) {
                    context.drawRect(textLeft, textY - 1.0F, 1.0F, searchFont.height() + 2.0F,
                            ClientAppearance.getAccent().withAlpha((int) (230 * alpha)));
                }
            }
        } else {
            // Подсветка выделения
            if (hasSelection()) {
                int min = getSelectionMin();
                int max = getSelectionMax();
                float selStartX = textLeft + searchFont.width(text.substring(0, min));
                float selWidth = searchFont.width(text.substring(min, max));
                context.drawRoundedRect(selStartX, textY - 1.0F, selWidth, searchFont.height() + 2.0F,
                        BorderRadius.all(2.0F), ClientAppearance.getAccent().withAlpha((int) (90 * alpha)));
            }

            // Текст
            ColorRGBA textCol = ColorRGBA.WHITE.withAlpha((int) (255 * alpha));
            context.drawText(searchFont, text, textLeft, textY, textCol);

            // Мигающий курсор в точной позиции
            if (focused) {
                boolean showCursor = !typingTimer.finished(350L) || (System.currentTimeMillis() % 1000L < 500L);
                if (showCursor) {
                    float cursorX = textLeft + searchFont.width(text.substring(0, Math.min(cursor, text.length())));
                    context.drawRect(cursorX, textY - 1.0F, 1.0F, searchFont.height() + 2.0F,
                            ClientAppearance.getAccent().withAlpha((int) (240 * alpha)));
                }
            }
        }

        ScissorUtility.pop();

        // 2. Кнопка крестика для очистки
        if (!text.isEmpty()) {
            float clearX = searchX + searchW - CLEAR_BTN_SIZE - CLEAR_BTN_PAD_RIGHT;
            float clearY = searchY + (searchH - CLEAR_BTN_SIZE) / 2.0F;

            boolean clearHovered = GuiUtility.isHovered(clearX - 1.0F, clearY - 1.0F, CLEAR_BTN_SIZE + 2.0F, CLEAR_BTN_SIZE + 2.0F, mouseX, mouseY);
            if (clearHovered) {
                CursorUtility.set(CursorType.HAND);
            }

            // Фоновая подсветка при наведении
            if (clearHovered) {
                context.drawRoundedRect(clearX, clearY, CLEAR_BTN_SIZE, CLEAR_BTN_SIZE,
                        BorderRadius.all(CLEAR_BTN_SIZE / 2.0F),
                        new ColorRGBA(255, 255, 255, (int) (35 * alpha)));
            }

            // Явный крестик из двух линий через матричное вращение
            ColorRGBA crossColor = clearHovered
                    ? ColorRGBA.WHITE.withAlpha((int) (220 * alpha))
                    : ColorRGBA.WHITE.withAlpha((int) (130 * alpha));

            float cx = clearX + CLEAR_BTN_SIZE / 2.0F;
            float cy = clearY + CLEAR_BTN_SIZE / 2.0F;
            float arm = 3.2F; // полудлина плеча крестика
            float thick = 1.3F; // толщина линии

            context.getMatrices().push();
            context.getMatrices().translate(cx, cy, 0.0F);
            context.getMatrices().multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(45.0F));
            context.getMatrices().translate(-cx, -cy, 0.0F);
            context.drawRect(cx - arm, cy - thick / 2.0F, arm * 2.0F, thick, crossColor);
            context.drawRect(cx - thick / 2.0F, cy - arm, thick, arm * 2.0F, crossColor);
            context.getMatrices().pop();
        }

        if (GuiUtility.isHovered(searchX, searchY, searchW, searchH, mouseX, mouseY)) {
            CursorUtility.set(CursorType.TEXT);
        }
    }
}