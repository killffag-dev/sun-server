package naryn.sun.ui.components;

import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Generated;
import naryn.sun.Sun;
import naryn.sun.framework.base.CustomComponent;
import naryn.sun.framework.base.UIContext;
import naryn.sun.framework.msdf.Font;
import naryn.sun.framework.msdf.Fonts;
import naryn.sun.framework.objects.BorderRadius;
import naryn.sun.framework.objects.MouseButton;
import naryn.sun.systems.localization.Language;
import naryn.sun.systems.localization.Localizator;
import naryn.sun.ui.components.colorpicker.ColorPickerSkin;
import naryn.sun.ui.components.colorpicker.ScreenColorSampler;
import naryn.sun.utility.animation.base.Animation;
import naryn.sun.utility.animation.base.Easing;
import org.lwjgl.glfw.GLFW;
import naryn.sun.utility.colors.ColorRGBA;
import naryn.sun.utility.colors.Colors;
import naryn.sun.utility.game.cursor.CursorType;
import naryn.sun.utility.game.cursor.CursorUtility;
import naryn.sun.utility.gui.GuiUtility;
import naryn.sun.utility.interfaces.IScaledResolution;
import naryn.sun.utility.interfaces.IWindow;
import naryn.sun.utility.render.RenderUtility;
import naryn.sun.utility.render.ShapeDrawUtility;
import naryn.sun.utility.sounds.ClientSoundManager;
import naryn.sun.utility.time.Timer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;

import java.util.ArrayList;
import java.util.List;

/**
 * Неоморфный ColorPicker:
 * - Заголовок с локализацией ("Выбор цвета" / "Color Picker");
 * - Выпадающая шторка выбора формата (Hex / RGB / HSL) с полутреугольником-индикатором;
 * - 2D HSV палитра (Saturation / Brightness) со скруглёнными углами и полым кольцевым курсором ◯;
 * - Горизонтальный радужный слайдер тона (Hue);
 * - Горизонтальный слайдер прозрачности (Alpha) с шахматной подложкой;
 * - Вдавленное поле ввода цвета с превью-свотчем, автовыделением всего текста при фокусе,
 *   лимитом символов и мгновенной заменой при наборе;
 * - Выпуклая кнопка пипетки с экранной лупой;
 * - Локализованное название оттенка ("Светящийся синий", "Яркий зелёный" и т.д.)
 *   и полоса оттенков с всплывающим бейджем активного цвета.
 */
public class ColorPicker extends CustomComponent implements IScaledResolution, IWindow {

    public static final float PICKER_W = 176.0F;
    public static final float PICKER_H = 286.0F;
    private static final float PAD_X = 12.0F;
    private static final float CONTENT_W = PICKER_W - PAD_X * 2.0F; // 152.0F

    public enum ColorMode {
        HEX("Hex"),
        RGBA("RGB"),
        HSLA("HSL");

        private final String label;
        ColorMode(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    public static final List<Preset> COLOR_PRESETS = new ArrayList<>(
            List.of(
                    new Preset(new ColorRGBA(0.0F, 122.0F, 255.0F)),
                    new Preset(new ColorRGBA(52.0F, 199.0F, 89.0F)),
                    new Preset(new ColorRGBA(255.0F, 204.0F, 0.0F)),
                    new Preset(new ColorRGBA(255.0F, 59.0F, 48.0F)),
                    new Preset(new ColorRGBA(151.0F, 71.0F, 255.0F))
            )
    );

    private final Animation animation = new Animation(260L, 0.0F, Easing.QUARTIC_OUT);
    private final ScreenColorSampler sampler = new ScreenColorSampler();
    private final Timer copyFeedbackTimer = new Timer();
    private final Timer cursorBlinkTimer = new Timer();

    private final String title;
    private final boolean enableAlpha;
    private boolean showing = true;
    private ColorMode mode = ColorMode.HEX;

    private float hue = 0.74F;
    private float saturation = 0.76F;
    private float brightness = 0.86F;
    private float alpha = 1.0F;

    private ColorRGBA originalColor;

    // Перетаскивание
    private boolean dragWindow;
    private float dragWinX, dragWinY;
    private boolean dragSV;
    private boolean dragHue;
    private boolean dragAlpha;

    // Выпадающая шторка выбора формата
    private boolean dropdownOpen = false;
    private final Animation dropdownAnim = new Animation(200L, 0.0F, Easing.FIGMA_EASE_IN_OUT);

    // Редактирование текста инпута
    private boolean inputFocused = false;
    private boolean selectAllOnFocus = false;
    private String inputBuffer = "";
    private int inputCursor = 0;

    // Анимации кнопок
    private final Animation pipetteHoverAnim = new Animation(180L, 0.0F, Easing.FIGMA_EASE_IN_OUT);
    private final Animation dropdownHoverAnim = new Animation(180L, 0.0F, Easing.FIGMA_EASE_IN_OUT);

    public ColorPicker(float x, float y, float offsetFactor, boolean enableAlpha, ColorRGBA color, String title) {
        super(x, y, PICKER_W, PICKER_H);
        this.enableAlpha = enableAlpha;
        this.title = title;
        this.originalColor = color != null ? color : ColorRGBA.WHITE;
        this.update(this.originalColor);
    }

    public static void setColorPresets(List<Preset> newPresets) {
        COLOR_PRESETS.clear();
        COLOR_PRESETS.addAll(newPresets);
    }

    public ColorRGBA built() {
        ColorRGBA base = ColorRGBA.fromHSB(this.hue, this.saturation, this.brightness);
        return base.withAlpha(this.enableAlpha ? 255.0F * this.alpha : 255.0F);
    }

    public void update(ColorRGBA color) {
        if (color == null) color = ColorRGBA.WHITE;
        this.hue = color.getHue();
        this.saturation = color.getSaturation();
        this.brightness = color.getBrightness();
        this.alpha = color.getAlpha() / 255.0F;
        this.dragWindow = false;
        this.dragSV = false;
        this.dragHue = false;
        this.dragAlpha = false;
        if (!inputFocused) {
            this.inputBuffer = formatColorValue(built());
            this.inputCursor = this.inputBuffer.length();
        }
    }

    @Override
    public boolean isHovered(double mouseX, double mouseY) {
        float margin = 8.0F;
        return mouseX >= this.x - margin && mouseX <= this.x + this.width + margin
                && mouseY >= this.y - margin && mouseY <= this.y + this.height + margin;
    }

    public boolean isDragging() {
        return this.dragWindow || this.dragSV || this.dragHue || this.dragAlpha;
    }

    public boolean isInputFocused() {
        return this.inputFocused;
    }

    @Override
    protected void renderComponent(UIContext context) {
        animation.setEasing(showing ? Easing.QUARTIC_OUT : Easing.FIGMA_EASE_IN_OUT);
        animation.update(showing ? 1.0F : 0.0F);
        float animVal = animation.getValue();
        if (animVal <= 0.001F && !showing) return;

        dropdownAnim.update(dropdownOpen ? 1.0F : 0.0F);

        int mx = context.getMouseX();
        int my = context.getMouseY();

        // Проверка физического состояния мыши: если левая кнопка отпущена, немедленно сбрасываем перетаскивание
        long windowHandle = mc.getWindow().getHandle();
        boolean leftPressed = GLFW.glfwGetMouseButton(windowHandle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        if (!leftPressed) {
            this.dragWindow = false;
            this.dragSV = false;
            this.dragHue = false;
            this.dragAlpha = false;
        }

        // 1. Перетаскивание окна
        if (dragWindow) {
            this.x = mx - dragWinX;
            this.y = my - dragWinY;
        }

        // Геометрия элементов
        float startX = this.x + PAD_X;
        float currY = this.y + 12.0F;

        float headerH = 17.0F;
        float canvasY = currY + headerH + 9.0F;
        float canvasH = 126.0F;

        float hueY = canvasY + canvasH + 8.0F;
        float hueH = 9.0F;

        float alphaY = hueY + hueH + 6.0F;
        float alphaH = 9.0F;

        // 2. Интерактивная обработка перетаскивания слайдеров
        if (dragSV) {
            this.saturation = GuiUtility.getSliderValue(0.0F, 1.0F, startX, CONTENT_W, mx);
            this.brightness = 1.0F - GuiUtility.getSliderValue(0.0F, 1.0F, canvasY, canvasH, my);
            if (!inputFocused) this.inputBuffer = formatColorValue(built());
        }

        if (dragHue) {
            this.hue = GuiUtility.getSliderValue(0.0F, 1.0F, startX, CONTENT_W, mx);
            if (!inputFocused) this.inputBuffer = formatColorValue(built());
        }

        if (dragAlpha && enableAlpha) {
            this.alpha = GuiUtility.getSliderValue(0.0F, 1.0F, startX, CONTENT_W, mx);
            if (!inputFocused) this.inputBuffer = formatColorValue(built());
        }

        // Анимация масштабирования при открытии/закрытии
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, animVal);
        RenderUtility.scale(context.getMatrices(), this.x + this.width / 2.0F, this.y + this.height / 2.0F, 0.88F + 0.12F * animVal);

        // 1. Оболочка окна (Стекло с Kawase Blur или тёмный неоморфизм)
        ColorPickerSkin.renderWindowShell(context, this.x, this.y, this.width, this.height, animVal);

        // 2. Верхняя строка: Заголовок + Селектор формата со шторкой
        renderHeader(context, startX, currY, headerH, animVal, mx, my);

        // 3. 2D HSV Палитра (Saturation / Brightness)
        renderSvCanvas(context, startX, canvasY, CONTENT_W, canvasH, animVal, mx, my);

        // 4. Горизонтальный радужный Hue слайдер
        renderHueSlider(context, startX, hueY, CONTENT_W, hueH, animVal, mx, my);

        // 5. Горизонтальный Alpha слайдер
        renderAlphaSlider(context, startX, alphaY, CONTENT_W, alphaH, animVal, mx, my);

        // 6. Строка действий: Вдавленное поле ввода цвета + Кнопка пипетки
        float controlsY = alphaY + alphaH + 11.0F;
        float controlsH = 24.0F;
        renderControlsRow(context, startX, controlsY, CONTENT_W, controlsH, animVal, mx, my);

        // 7. Секция оттенков с всплывающим активным бейджем
        float paletteSectionY = controlsY + controlsH + 11.0F;
        renderPaletteSection(context, startX, paletteSectionY, CONTENT_W, animVal, mx, my);

        // 8. Выпадающая шторка формата (рендерится поверх холста)
        if (dropdownAnim.getValue() > 0.01F) {
            renderDropdownCurtain(context, startX, currY + headerH, animVal, mx, my);
        }

        RenderUtility.end(context.getMatrices());
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        // Полноэкранная пипетка поверх всего
        if (sampler.isActive()) {
            sampler.renderLoupe(context, mx, my);
        }
    }

    /**
     * Верхняя строка: Заголовок ("Выбор цвета" / "Color Picker") и селектор формата.
     */
    private void renderHeader(UIContext context, float startX, float headerY, float headerH, float alpha, int mx, int my) {
        Font titleFont = Fonts.MEDIUM.getFont(7.8F);
        Font dropdownFont = Fonts.MEDIUM.getFont(6.8F);

        // Локализованный заголовок
        String titleText = (this.title != null && !this.title.isEmpty()) ? this.title : Localizator.translate("colorpicker.title");
        float titleY = headerY + (headerH - titleFont.height()) / 2.0F - 0.5F;
        context.drawText(titleFont, titleText, startX, titleY, ColorPickerSkin.getTitleColor(alpha));

        // Кнопка селектора формата (Hex / RGB / HSL)
        float dropW = 40.0F;
        float dropX = startX + CONTENT_W - dropW;
        BorderRadius dropRadius = BorderRadius.all(4.5F);

        boolean dropHover = GuiUtility.isHovered(dropX, headerY, dropW, headerH, mx, my);
        if (dropHover) CursorUtility.set(CursorType.HAND);
        dropdownHoverAnim.update(dropHover);

        ColorPickerSkin.renderRecessedWell(context, dropX, headerY, dropW, headerH, dropRadius, alpha);

        String modeText = this.mode.getLabel();
        float textStartX = dropX + 6.0F;
        float modeTextY = headerY + (headerH - dropdownFont.height()) / 2.0F - 0.5F;
        context.drawText(dropdownFont, modeText, textStartX, modeTextY, ColorPickerSkin.getDropdownTextColor(alpha));

        // Полутреугольник-индикатор (▾ смотрит вниз при закрытой, ▴ вверх при открытой)
        float arrowCenterX = dropX + dropW - 8.5F;
        float arrowCenterY = headerY + headerH / 2.0F;
        ColorRGBA arrowColor = ColorPickerSkin.getIconColor(dropdownOpen, dropHover, alpha);

        if (dropdownOpen) {
            // Стрелка вверх (▴)
            ShapeDrawUtility.drawLine(context.getMatrices(),
                    new Vec2f(arrowCenterX - 2.5F, arrowCenterY + 1.2F),
                    new Vec2f(arrowCenterX, arrowCenterY - 1.5F), arrowColor);
            ShapeDrawUtility.drawLine(context.getMatrices(),
                    new Vec2f(arrowCenterX, arrowCenterY - 1.5F),
                    new Vec2f(arrowCenterX + 2.5F, arrowCenterY + 1.2F), arrowColor);
        } else {
            // Стрелка вниз (▾)
            ShapeDrawUtility.drawLine(context.getMatrices(),
                    new Vec2f(arrowCenterX - 2.5F, arrowCenterY - 1.2F),
                    new Vec2f(arrowCenterX, arrowCenterY + 1.5F), arrowColor);
            ShapeDrawUtility.drawLine(context.getMatrices(),
                    new Vec2f(arrowCenterX, arrowCenterY + 1.5F),
                    new Vec2f(arrowCenterX + 2.5F, arrowCenterY - 1.2F), arrowColor);
        }

        // Drag области заголовка
        if (GuiUtility.isHovered(startX, headerY, CONTENT_W - dropW - 6.0F, headerH, mx, my)) {
            CursorUtility.set(CursorType.RESIZE_ALL);
        }
    }

    /**
     * Выпадающая шторка выбора формата.
     */
    private void renderDropdownCurtain(UIContext context, float startX, float topY, float alpha, int mx, int my) {
        float dropW = 42.0F;
        float dropX = startX + CONTENT_W - dropW;
        float curtainY = topY + 2.0F;
        float rowH = 15.0F;
        ColorMode[] modes = ColorMode.values();
        float fullH = modes.length * rowH + 4.0F;

        float anim = dropdownAnim.getValue();
        float currentH = fullH * anim;
        BorderRadius curtainRadius = BorderRadius.all(5.0F);

        ColorPickerSkin.renderDropdownCurtain(context, dropX, curtainY, dropW, currentH, curtainRadius, alpha);

        if (anim > 0.4F) {
            Font optFont = Fonts.MEDIUM.getFont(6.8F);
            for (int i = 0; i < modes.length; i++) {
                ColorMode m = modes[i];
                float rowY = curtainY + 2.0F + i * rowH;
                boolean isHover = GuiUtility.isHovered(dropX, rowY, dropW, rowH, mx, my);
                if (isHover) CursorUtility.set(CursorType.HAND);

                boolean isCurrent = (m == this.mode);
                if (isHover || isCurrent) {
                    context.drawRoundedRect(dropX + 2.0F, rowY, dropW - 4.0F, rowH,
                            BorderRadius.all(3.0F),
                            isCurrent ? Colors.ACCENT.withAlpha((int) (45 * alpha))
                                      : Colors.WHITE.withAlpha((int) (18 * alpha)));
                }

                ColorRGBA col = isCurrent ? Colors.ACCENT.withAlpha((int) (245 * alpha))
                                         : ColorPickerSkin.getDropdownTextColor(alpha);
                float ty = rowY + (rowH - optFont.height()) / 2.0F - 0.5F;
                context.drawText(optFont, m.getLabel(), dropX + 7.0F, ty, col);
            }
        }
    }

    /**
     * 2D HSV палитра (Saturation / Brightness) со скруглёнными углами.
     */
    private void renderSvCanvas(UIContext context, float svX, float svY, float svW, float svH, float alpha, int mx, int my) {
        BorderRadius svRadius = BorderRadius.all(8.0F);

        ColorRGBA pureHue = ColorRGBA.fromHSB(this.hue, 1.0F, 1.0F);
        ShapeDrawUtility.drawRoundedRect(context.getMatrices(), svX, svY, svW, svH, svRadius,
                Colors.WHITE, Colors.BLACK, Colors.BLACK, pureHue);

        context.drawRoundedBorder(svX, svY, svW, svH, 0.5F, svRadius,
                ColorPickerSkin.isFrost() ? new ColorRGBA(255, 255, 255, (int) (20 * alpha))
                                         : new ColorRGBA(255, 255, 255, (int) (12 * alpha)));

        float thumbX = svX + svW * this.saturation;
        float thumbY = svY + svH * (1.0F - this.brightness);
        float circleSize = 11.0F;
        float half = circleSize / 2.0F;

        context.drawRoundedBorder(thumbX - half - 0.5F, thumbY - half - 0.5F,
                circleSize + 1.0F, circleSize + 1.0F, 1.0F, BorderRadius.all((circleSize + 1.0F) / 2.0F),
                new ColorRGBA(0, 0, 0, (int) (70 * alpha)));
        context.drawRoundedBorder(thumbX - half, thumbY - half,
                circleSize, circleSize, 1.4F, BorderRadius.all(half),
                Colors.WHITE.withAlpha((int) (245 * alpha)));

        if (GuiUtility.isHovered(svX, svY, svW, svH, mx, my) || dragSV) {
            CursorUtility.set(CursorType.CROSSHAIR);
        }
    }

    /**
     * Горизонтальный радужный спектр (Hue).
     */
    private void renderHueSlider(UIContext context, float hX, float hY, float hW, float hH, float alpha, int mx, int my) {
        BorderRadius barRadius = BorderRadius.all(hH / 2.0F);

        context.drawRoundedTexture(Sun.id("textures/hue_horizontal.png"), hX, hY, hW, hH, barRadius);
        context.drawRoundedBorder(hX, hY, hW, hH, 0.5F, barRadius,
                new ColorRGBA(0, 0, 0, (int) (25 * alpha)));

        float thumbX = hX + hW * this.hue;
        float thumbY = hY + hH / 2.0F;
        float circleSize = 10.0F;
        float half = circleSize / 2.0F;

        context.drawRoundedBorder(thumbX - half - 0.5F, thumbY - half - 0.5F,
                circleSize + 1.0F, circleSize + 1.0F, 1.0F, BorderRadius.all((circleSize + 1.0F) / 2.0F),
                new ColorRGBA(0, 0, 0, (int) (65 * alpha)));
        context.drawRoundedBorder(thumbX - half, thumbY - half,
                circleSize, circleSize, 1.4F, BorderRadius.all(half),
                Colors.WHITE.withAlpha((int) (245 * alpha)));

        if (GuiUtility.isHovered(hX, hY - 1.0F, hW, hH + 2.0F, mx, my) || dragHue) {
            CursorUtility.set(CursorType.HAND);
        }
    }

    /**
     * Горизонтальный Alpha слайдер с шахматной сеткой.
     */
    private void renderAlphaSlider(UIContext context, float aX, float aY, float aW, float aH, float alpha, int mx, int my) {
        BorderRadius barRadius = BorderRadius.all(aH / 2.0F);

        context.drawRoundedTexture(Sun.id("textures/checkerboard.png"), aX, aY, aW, aH, barRadius);

        ColorRGBA baseColor = ColorRGBA.fromHSB(this.hue, this.saturation, this.brightness);
        ColorRGBA c0 = baseColor.withAlpha(0.0F);
        ColorRGBA c1 = baseColor.withAlpha(255.0F);
        ShapeDrawUtility.drawRoundedRect(context.getMatrices(), aX, aY, aW, aH, barRadius, c0, c0, c1, c1);

        context.drawRoundedBorder(aX, aY, aW, aH, 0.5F, barRadius,
                new ColorRGBA(0, 0, 0, (int) (25 * alpha)));

        float thumbX = aX + aW * this.alpha;
        float thumbY = aY + aH / 2.0F;
        float circleSize = 10.0F;
        float half = circleSize / 2.0F;

        context.drawRoundedBorder(thumbX - half - 0.5F, thumbY - half - 0.5F,
                circleSize + 1.0F, circleSize + 1.0F, 1.0F, BorderRadius.all((circleSize + 1.0F) / 2.0F),
                new ColorRGBA(0, 0, 0, (int) (65 * alpha)));
        context.drawRoundedBorder(thumbX - half, thumbY - half,
                circleSize, circleSize, 1.4F, BorderRadius.all(half),
                Colors.WHITE.withAlpha((int) (245 * alpha)));

        if (GuiUtility.isHovered(aX, aY - 1.0F, aW, aH + 2.0F, mx, my) || dragAlpha) {
            CursorUtility.set(CursorType.HAND);
        }
    }

    /**
     * Строка действий: Расширенное вдавленное поле ввода цвета + Кнопка пипетки.
     */
    private void renderControlsRow(UIContext context, float startX, float rowY, float rowW, float rowH, float alpha, int mx, int my) {
        float btnSize = 24.0F;
        float gap = 8.0F;
        float inputW = rowW - btnSize - gap; // 120.0F

        // 1. Поле ввода цвета
        renderInputWell(context, startX, rowY, inputW, rowH, alpha, mx, my);

        // 2. Кнопка пипетки
        float pipetteX = startX + inputW + gap;
        boolean pipHover = GuiUtility.isHovered(pipetteX, rowY, btnSize, rowH, mx, my);
        if (pipHover) CursorUtility.set(CursorType.HAND);
        pipetteHoverAnim.update(pipHover);

        ColorPickerSkin.renderRaisedButton(context, pipetteX, rowY, btnSize, rowH,
                BorderRadius.all(5.5F), sampler.isActive(), pipetteHoverAnim.getValue(), alpha);

        ColorRGBA pipIconCol = ColorPickerSkin.getIconColor(sampler.isActive(), pipHover, alpha);
        float iconSize = 11.0F;
        context.drawTexture(Sun.id("icons/colorpicker/pipette.png"),
                pipetteX + (btnSize - iconSize) / 2.0F,
                rowY + (rowH - iconSize) / 2.0F,
                iconSize, iconSize, pipIconCol);
    }

    /**
     * Вдавленное поле ввода со свотчем, подсветкой выделения и редактированием.
     */
    private void renderInputWell(UIContext context, float inX, float inY, float inW, float inH, float alpha, int mx, int my) {
        BorderRadius wellRadius = BorderRadius.all(6.0F);
        boolean hovered = GuiUtility.isHovered(inX, inY, inW, inH, mx, my);
        if (hovered) CursorUtility.set(CursorType.TEXT);

        ColorPickerSkin.renderRecessedWell(context, inX, inY, inW, inH, wellRadius, alpha);

        // Свотч текущего цвета
        float swatchSize = 11.0F;
        float swatchX = inX + 7.0F;
        float swatchY = inY + (inH - swatchSize) / 2.0F;
        BorderRadius swatchR = BorderRadius.all(2.5F);

        ColorRGBA currentColor = built();
        context.drawRoundedRect(swatchX, swatchY, swatchSize, swatchSize, swatchR, currentColor);
        context.drawRoundedBorder(swatchX, swatchY, swatchSize, swatchSize, 0.5F, swatchR,
                new ColorRGBA(0, 0, 0, (int) (35 * alpha)));

        // Текст значения
        Font valFont = Fonts.MEDIUM.getFont(7.5F);
        String displayText;
        boolean copied = !copyFeedbackTimer.finished(1000L);

        if (copied) {
            displayText = Localizator.translate("colorpicker.copied");
        } else if (inputFocused) {
            displayText = inputBuffer;
        } else {
            displayText = formatColorValue(currentColor);
        }

        float textX = swatchX + swatchSize + 7.0F;
        float textY = inY + (inH - valFont.height()) / 2.0F - 0.5F;
        float textW = valFont.getFont().getWidth(displayText, valFont.getSize());

        // Подсветка полного выделения при фокусе
        if (inputFocused && selectAllOnFocus && !copied && !displayText.isEmpty()) {
            context.drawRoundedRect(textX - 2.0F, inY + 4.0F, textW + 4.0F, inH - 8.0F,
                    BorderRadius.all(2.0F), Colors.ACCENT.withAlpha((int) (130 * alpha)));
        }

        ColorRGBA textCol = copied ? new ColorRGBA(46, 204, 113, (int) (245 * alpha))
                : (inputFocused && selectAllOnFocus ? Colors.WHITE : ColorPickerSkin.getValueTextColor(alpha));

        context.drawText(valFont, displayText, textX, textY, textCol);

        // Мигающий курсор при редактировании без выделения всего текста
        if (inputFocused && !selectAllOnFocus && !copied) {
            boolean showCursor = (System.currentTimeMillis() / 450) % 2 == 0;
            if (showCursor) {
                String sub = inputBuffer.substring(0, Math.min(inputCursor, inputBuffer.length()));
                float curOffsetX = valFont.getFont().getWidth(sub, valFont.getSize());
                float curX = textX + curOffsetX + 0.5F;
                context.drawRect(curX, inY + 5.0F, 0.8F, inH - 10.0F, textCol);
            }
        }
    }

    /**
     * Секция палитры внизу: Локализованное название оттенка и полоса оттенков с бейджем.
     */
    private void renderPaletteSection(UIContext context, float startX, float secY, float secW, float alpha, int mx, int my) {
        Font labelFont = Fonts.MEDIUM.getFont(7.0F);

        // Название оттенка
        String shadeName = getAestheticColorName();
        context.drawText(labelFont, shadeName, startX, secY, ColorPickerSkin.getShadeTitleColor(alpha));

        // Полоса из 5 оттенков
        float barY = secY + 13.0F;
        float barH = 10.0F;
        float slotW = secW / 5.0F;
        BorderRadius barRadius = BorderRadius.all(3.0F);

        ColorRGBA[] shades = generateShades();

        context.drawRoundedBorder(startX, barY, secW, barH, 0.5F, barRadius,
                ColorPickerSkin.getPaletteBorderColor(alpha));

        int activeIndex = 2;

        for (int i = 0; i < shades.length; i++) {
            float slotX = startX + i * slotW;
            ColorRGBA sCol = shades[i];

            BorderRadius sRadius = BorderRadius.ZERO;
            if (i == 0) sRadius = BorderRadius.left(3.0F, 3.0F);
            else if (i == shades.length - 1) sRadius = BorderRadius.right(3.0F, 3.0F);

            context.drawRoundedRect(slotX, barY, slotW, barH, sRadius, sCol);

            boolean slotHover = GuiUtility.isHovered(slotX, barY, slotW, barH, mx, my);
            if (slotHover) CursorUtility.set(CursorType.HAND);
        }

        // Всплывающий бейдж активного цвета
        float pillW = 44.0F;
        float pillH = 15.0F;
        float activeCenterX = startX + activeIndex * slotW + slotW / 2.0F;
        float pillX = activeCenterX - pillW / 2.0F;
        float pillY = barY - 2.5F;
        BorderRadius pillRadius = BorderRadius.all(3.0F);

        ColorRGBA activeCol = built().withAlpha(255.0F);

        context.drawShadow(pillX, pillY + 1.5F, pillW, pillH, 5.0F, pillRadius,
                new ColorRGBA(0, 0, 0, (int) (90 * alpha)));
        context.drawRoundedRect(pillX, pillY, pillW, pillH, pillRadius, activeCol);
        context.drawRoundedBorder(pillX, pillY, pillW, pillH, 0.5F, pillRadius,
                Colors.WHITE.withAlpha((int) (80 * alpha)));

        Font pillFont = Fonts.MEDIUM.getFont(6.2F);
        String pillText = activeCol.toHex().substring(0, 7).toUpperCase();
        float pillTextW = pillFont.getFont().getWidth(pillText, pillFont.getSize());
        float pillTextX = pillX + (pillW - pillTextW) / 2.0F;
        float pillTextY = pillY + (pillH - pillFont.height()) / 2.0F - 0.5F;

        context.drawText(pillFont, pillText, pillTextX, pillTextY, Colors.WHITE.withAlpha((int) (255 * alpha)));
    }

    private ColorRGBA[] generateShades() {
        ColorRGBA[] shades = new ColorRGBA[5];
        shades[0] = ColorRGBA.fromHSB(this.hue, Math.min(1.0F, this.saturation * 1.1F), Math.max(0.18F, this.brightness * 0.32F));
        shades[1] = ColorRGBA.fromHSB(this.hue, this.saturation, Math.max(0.35F, this.brightness * 0.65F));
        shades[2] = ColorRGBA.fromHSB(this.hue, this.saturation, this.brightness);
        shades[3] = ColorRGBA.fromHSB(this.hue, Math.max(0.15F, this.saturation * 0.68F), Math.min(1.0F, this.brightness * 1.05F));
        shades[4] = ColorRGBA.fromHSB(this.hue, Math.max(0.10F, this.saturation * 0.40F), Math.min(1.0F, this.brightness * 1.12F));
        return shades;
    }

    /**
     * Локализованное название оттенка.
     */
    private String getAestheticColorName() {
        boolean isRussian = Localizator.getCurrentLanguage() == Language.RU_RU;

        if (this.brightness < 0.12F) return isRussian ? "Обсидиановый чёрный" : "Obsidian Black";
        if (this.saturation < 0.08F) {
            if (this.brightness > 0.85F) return isRussian ? "Чистый белый" : "Pure White";
            return isRussian ? "Графитовый серый" : "Graphite Gray";
        }

        int hDeg = Math.round(this.hue * 360.0F);
        String baseName;
        if (hDeg >= 345 || hDeg < 15) baseName = isRussian ? "красный" : "Red";
        else if (hDeg < 45) baseName = isRussian ? "оранжевый" : "Orange";
        else if (hDeg < 70) baseName = isRussian ? "янтарный" : "Amber";
        else if (hDeg < 165) baseName = isRussian ? "зелёный" : "Green";
        else if (hDeg < 195) baseName = isRussian ? "бирюзовый" : "Cyan";
        else if (hDeg < 255) baseName = isRussian ? "синий" : "Blue";
        else if (hDeg < 290) baseName = isRussian ? "фиолетовый" : "Violet";
        else baseName = isRussian ? "пурпурный" : "Magenta";

        String prefix;
        if (this.brightness > 0.75F && this.saturation > 0.60F) {
            if (hDeg >= 195 && hDeg < 255) prefix = isRussian ? "Светящийся" : "Glowing";
            else prefix = isRussian ? "Неоновый" : "Electric";
        } else if (this.brightness > 0.80F && this.saturation < 0.50F) {
            prefix = isRussian ? "Пастельный" : "Pastel";
        } else if (this.brightness < 0.45F) {
            prefix = isRussian ? "Тёмный" : "Deep";
        } else {
            prefix = isRussian ? "Яркий" : "Vibrant";
        }

        return prefix + " " + baseName;
    }

    private String formatColorValue(ColorRGBA c) {
        return switch (this.mode) {
            case HEX -> {
                String hex = c.toHex();
                yield enableAlpha ? hex.toUpperCase() : hex.substring(0, 7).toUpperCase();
            }
            case RGBA -> enableAlpha
                    ? String.format("%d, %d, %d, %d", (int) c.getRed(), (int) c.getGreen(), (int) c.getBlue(), (int) c.getAlpha())
                    : String.format("%d, %d, %d", (int) c.getRed(), (int) c.getGreen(), (int) c.getBlue());
            case HSLA -> {
                int hDeg = Math.round(this.hue * 360.0F);
                int sPct = Math.round(this.saturation * 100.0F);
                int bPct = Math.round(this.brightness * 100.0F);
                yield enableAlpha
                        ? String.format("%d°, %d%%, %d%%, %d%%", hDeg, sPct, bPct, Math.round(this.alpha * 100.0F))
                        : String.format("%d°, %d%%, %d%%", hDeg, sPct, bPct);
            }
        };
    }

    private int getMaxInputLength() {
        return switch (this.mode) {
            case HEX -> enableAlpha ? 9 : 7;
            case RGBA -> 16;
            case HSLA -> 18;
        };
    }

    private void tryParseAndUpdateColor(String text) {
        if (text == null || text.trim().isEmpty()) return;
        String t = text.trim();
        try {
            if (t.startsWith("#") || t.length() == 6 || t.length() == 8) {
                ColorRGBA parsed = ColorRGBA.fromHex(t);
                if (parsed != null) {
                    this.update(parsed);
                    return;
                }
            }

            if (t.contains(",") || t.contains(" ")) {
                String[] parts = t.replace("rgb", "").replace("rgba", "").replace("(", "").replace(")", "").split("[,\\s]+");
                if (parts.length >= 3) {
                    float r = Float.parseFloat(parts[0].trim());
                    float g = Float.parseFloat(parts[1].trim());
                    float b = Float.parseFloat(parts[2].trim());
                    float a = parts.length >= 4 ? Float.parseFloat(parts[3].trim()) : 255.0F;
                    if (a <= 1.0F && parts.length >= 4 && parts[3].contains(".")) a *= 255.0F;
                    this.update(new ColorRGBA(r, g, b, a));
                }
            }
        } catch (Exception ignored) {}
    }

    @Override
    public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
        if (sampler.isActive()) {
            sampler.handleMouseClicked(mouseX, mouseY, button);
            return;
        }

        // Клик вне окна закрывает палитру
        if (!isHovered(mouseX, mouseY)) {
            this.showing = false;
            this.inputFocused = false;
            this.selectAllOnFocus = false;
            this.dropdownOpen = false;
            return;
        }

        // Правый клик внутри окна — быстрое закрытие
        if (button == MouseButton.RIGHT) {
            this.showing = false;
            this.inputFocused = false;
            this.selectAllOnFocus = false;
            this.dropdownOpen = false;
            ClientSoundManager.getInstance().playButtonClick();
            return;
        }

        if (button != MouseButton.LEFT) {
            return;
        }

        float startX = this.x + PAD_X;
        float currY = this.y + 12.0F;
        float headerH = 17.0F;

        // 1. Клик по шторке выбора формата, если она открыта
        if (dropdownOpen) {
            float dropW = 42.0F;
            float dropX = startX + CONTENT_W - dropW;
            float curtainY = currY + headerH + 2.0F;
            float rowH = 15.0F;
            ColorMode[] modes = ColorMode.values();
            float fullH = modes.length * rowH + 4.0F;

            if (GuiUtility.isHovered(dropX, curtainY, dropW, fullH, mouseX, mouseY)) {
                int clickedIndex = (int) ((mouseY - curtainY - 2.0F) / rowH);
                if (clickedIndex >= 0 && clickedIndex < modes.length) {
                    this.mode = modes[clickedIndex];
                    this.dropdownOpen = false;
                    this.inputBuffer = formatColorValue(built());
                    this.inputCursor = this.inputBuffer.length();
                    ClientSoundManager.getInstance().playButtonClick();
                    return;
                }
            } else {
                this.dropdownOpen = false;
            }
        }

        // 2. Клик по кнопке селектора формата (открывает/закрывает шторку)
        float dropW = 40.0F;
        float dropX = startX + CONTENT_W - dropW;
        if (GuiUtility.isHovered(dropX, currY, dropW, headerH, mouseX, mouseY)) {
            this.dropdownOpen = !this.dropdownOpen;
            ClientSoundManager.getInstance().playButtonClick();
            return;
        }

        // Перетаскивание окна за свободную область заголовка
        if (GuiUtility.isHovered(startX, currY, CONTENT_W - dropW - 6.0F, headerH, mouseX, mouseY)) {
            this.dragWindow = true;
            this.dragWinX = (float) (mouseX - this.x);
            this.dragWinY = (float) (mouseY - this.y);
            return;
        }

        // 3. Клик по 2D HSV палитре
        float canvasY = currY + headerH + 9.0F;
        float canvasH = 126.0F;
        if (GuiUtility.isHovered(startX, canvasY, CONTENT_W, canvasH, mouseX, mouseY)) {
            this.dragSV = true;
            this.saturation = GuiUtility.getSliderValue(0.0F, 1.0F, startX, CONTENT_W, mouseX);
            this.brightness = 1.0F - GuiUtility.getSliderValue(0.0F, 1.0F, canvasY, canvasH, mouseY);
            this.inputFocused = false;
            this.selectAllOnFocus = false;
            this.dropdownOpen = false;
            return;
        }

        // 4. Клик по Hue слайдеру
        float hueY = canvasY + canvasH + 8.0F;
        float hueH = 9.0F;
        if (GuiUtility.isHovered(startX, hueY - 2.0F, CONTENT_W, hueH + 4.0F, mouseX, mouseY)) {
            this.dragHue = true;
            this.hue = GuiUtility.getSliderValue(0.0F, 1.0F, startX, CONTENT_W, mouseX);
            this.inputFocused = false;
            this.selectAllOnFocus = false;
            this.dropdownOpen = false;
            return;
        }

        // 5. Клик по Alpha слайдеру
        float alphaY = hueY + hueH + 6.0F;
        float alphaH = 9.0F;
        if (enableAlpha && GuiUtility.isHovered(startX, alphaY - 2.0F, CONTENT_W, alphaH + 4.0F, mouseX, mouseY)) {
            this.dragAlpha = true;
            this.alpha = GuiUtility.getSliderValue(0.0F, 1.0F, startX, CONTENT_W, mouseX);
            this.inputFocused = false;
            this.selectAllOnFocus = false;
            this.dropdownOpen = false;
            return;
        }

        // 6. Строка действий
        float controlsY = alphaY + alphaH + 11.0F;
        float controlsH = 24.0F;
        float btnSize = 24.0F;
        float gap = 8.0F;
        float inputW = CONTENT_W - btnSize - gap; // 120.0F

        // 6.1 Клик по полю ввода -> фокус с полным выделением
        if (GuiUtility.isHovered(startX, controlsY, inputW, controlsH, mouseX, mouseY)) {
            this.inputFocused = true;
            this.selectAllOnFocus = true;
            this.inputBuffer = formatColorValue(built());
            this.inputCursor = this.inputBuffer.length();
            this.dropdownOpen = false;
            cursorBlinkTimer.reset();
            return;
        } else {
            this.inputFocused = false;
            this.selectAllOnFocus = false;
        }

        // 6.2 Клик по пипетке
        float pipetteX = startX + inputW + gap;
        if (GuiUtility.isHovered(pipetteX, controlsY, btnSize, controlsH, mouseX, mouseY)) {
            sampler.start(this::update);
            this.dropdownOpen = false;
            ClientSoundManager.getInstance().playButtonClick();
            return;
        }

        // 7. Клик по полосе оттенков
        float paletteSectionY = controlsY + controlsH + 11.0F;
        float barY = paletteSectionY + 13.0F;
        float barH = 10.0F;
        if (GuiUtility.isHovered(startX, barY - 2.0F, CONTENT_W, barH + 4.0F, mouseX, mouseY)) {
            float slotW = CONTENT_W / 5.0F;
            int clickedSlot = (int) ((mouseX - startX) / slotW);
            clickedSlot = MathHelper.clamp(clickedSlot, 0, 4);
            ColorRGBA[] shades = generateShades();
            this.update(shades[clickedSlot]);
            this.dropdownOpen = false;
            ClientSoundManager.getInstance().playButtonClick();
            return;
        }
    }

    @Override
    public void onMouseReleased(double mouseX, double mouseY, MouseButton button) {
        this.dragWindow = false;
        this.dragSV = false;
        this.dragHue = false;
        this.dragAlpha = false;
    }

    @Override
    public void onScroll(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!enableAlpha) return;
        if (isHovered(mouseX, mouseY)) {
            this.alpha = Math.max(0.0F, Math.min(1.0F, this.alpha + (float) verticalAmount * 0.05F));
            if (!inputFocused) this.inputBuffer = formatColorValue(built());
        }
    }

    @Override
    public void onKeyPressed(int keyCode, int scanCode, int modifiers) {
        if (sampler.handleKeyPressed(keyCode)) return;

        // Закрытие шторки или инпута или палитры по ESC
        if (keyCode == 256) {
            if (dropdownOpen) {
                dropdownOpen = false;
                return;
            }
            if (inputFocused) {
                inputFocused = false;
                selectAllOnFocus = false;
                return;
            }
            this.showing = false;
            return;
        }

        // Обработка клавиш в активном инпуте
        if (inputFocused) {
            cursorBlinkTimer.reset();

            // Enter
            if (keyCode == 257 || keyCode == 335) {
                tryParseAndUpdateColor(inputBuffer);
                inputFocused = false;
                selectAllOnFocus = false;
                this.inputBuffer = formatColorValue(built());
                return;
            }

            // Выделить всё (Ctrl+A)
            if (Screen.isSelectAll(keyCode)) {
                selectAllOnFocus = true;
                return;
            }

            // Backspace / Delete
            if (keyCode == 259 || keyCode == 261) {
                if (selectAllOnFocus) {
                    inputBuffer = "";
                    inputCursor = 0;
                    selectAllOnFocus = false;
                    return;
                }
                if (keyCode == 259 && !inputBuffer.isEmpty() && inputCursor > 0) {
                    inputBuffer = inputBuffer.substring(0, inputCursor - 1) + inputBuffer.substring(inputCursor);
                    inputCursor--;
                    tryParseAndUpdateColor(inputBuffer);
                    return;
                }
                if (keyCode == 261 && inputCursor < inputBuffer.length()) {
                    inputBuffer = inputBuffer.substring(0, inputCursor) + inputBuffer.substring(inputCursor + 1);
                    tryParseAndUpdateColor(inputBuffer);
                    return;
                }
            }

            // Стрелка влево
            if (keyCode == 263) {
                if (selectAllOnFocus) {
                    selectAllOnFocus = false;
                    inputCursor = 0;
                } else if (inputCursor > 0) {
                    inputCursor--;
                }
                return;
            }

            // Стрелка вправо
            if (keyCode == 262) {
                if (selectAllOnFocus) {
                    selectAllOnFocus = false;
                    inputCursor = inputBuffer.length();
                } else if (inputCursor < inputBuffer.length()) {
                    inputCursor++;
                }
                return;
            }

            // Вставка Ctrl+V
            if (Screen.isPaste(keyCode)) {
                String clip = mc.keyboard.getClipboard();
                if (clip != null && !clip.isEmpty()) {
                    clip = clip.trim();
                    if (selectAllOnFocus) {
                        inputBuffer = "";
                        inputCursor = 0;
                        selectAllOnFocus = false;
                    }
                    int maxLen = getMaxInputLength();
                    int spaceLeft = Math.max(0, maxLen - inputBuffer.length());
                    if (clip.length() > spaceLeft) {
                        clip = clip.substring(0, spaceLeft);
                    }
                    inputBuffer = inputBuffer.substring(0, inputCursor) + clip + inputBuffer.substring(inputCursor);
                    inputCursor += clip.length();
                    tryParseAndUpdateColor(inputBuffer);
                }
                return;
            }

            // Копирование Ctrl+C
            if (Screen.isCopy(keyCode)) {
                mc.keyboard.setClipboard(inputBuffer);
                copyFeedbackTimer.reset();
                return;
            }
            return;
        }

        // Копирование вне инпута
        if (Screen.isCopy(keyCode)) {
            mc.keyboard.setClipboard(built().toHex().substring(0, enableAlpha ? 9 : 7));
            copyFeedbackTimer.reset();
        } else if (Screen.isPaste(keyCode)) {
            String clip = mc.keyboard.getClipboard();
            tryParseAndUpdateColor(clip);
        }
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (!inputFocused) return false;

        // Если выделен весь текст — заменяем его первым введённым символом
        if (selectAllOnFocus) {
            inputBuffer = "";
            inputCursor = 0;
            selectAllOnFocus = false;
        }

        // Проверка лимита символов
        int maxLen = getMaxInputLength();
        if (inputBuffer.length() >= maxLen) {
            return false;
        }

        // Разрешаем только символы для цвета
        if (Character.isLetterOrDigit(chr) || chr == '#' || chr == ',' || chr == '.' || chr == ' ' || chr == '%') {
            inputBuffer = inputBuffer.substring(0, inputCursor) + chr + inputBuffer.substring(inputCursor);
            inputCursor++;
            cursorBlinkTimer.reset();
            tryParseAndUpdateColor(inputBuffer);
            return true;
        }
        return false;
    }

    public boolean isPick() {
        return sampler.isActive();
    }

    @Generated
    public Animation getAnimation() {
        return this.animation;
    }

    @Generated
    public boolean isShowing() {
        return this.showing;
    }

    @Generated
    public void setShowing(boolean showing) {
        this.showing = showing;
    }

    public static class Preset {
        public final ColorRGBA color;
        private boolean showing = true;

        public Preset(ColorRGBA color) {
            this.color = color;
        }

        public ColorRGBA getColor() {
            return this.color;
        }

        public boolean isShowing() {
            return this.showing;
        }

        public void setShowing(boolean showing) {
            this.showing = showing;
        }
    }
}
