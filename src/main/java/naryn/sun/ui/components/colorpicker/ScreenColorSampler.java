package naryn.sun.ui.components.colorpicker;

import naryn.sun.framework.base.UIContext;
import naryn.sun.framework.msdf.Font;
import naryn.sun.framework.msdf.Fonts;
import naryn.sun.framework.objects.BorderRadius;
import naryn.sun.framework.objects.MouseButton;
import naryn.sun.systems.localization.Localizator;
import naryn.sun.utility.colors.ColorRGBA;
import naryn.sun.utility.colors.Colors;
import naryn.sun.utility.interfaces.IMinecraft;
import naryn.sun.utility.interfaces.IScaledResolution;
import naryn.sun.utility.interfaces.IWindow;
import naryn.sun.utility.sounds.ClientSoundManager;

import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.Robot;

/**
 * Полноэкранная пипетка: захватывает реальный цвет пикселя отовсюду с экрана монитора.
 * Использует системный {@link java.awt.Robot} для снятия цвета в любых окнах и приложениях,
 * с автоматическим fallback на OpenGL фреймбуфер Minecraft.
 */
public class ScreenColorSampler implements IMinecraft, IScaledResolution, IWindow {

    private static Robot robotInstance;
    private static boolean robotInitFailed = false;

    private boolean active = false;
    private ColorRGBA lastSampledColor = ColorRGBA.WHITE;
    private ColorConsumer onColorSelected;

    public interface ColorConsumer {
        void accept(ColorRGBA color);
    }

    private static Robot getRobot() {
        if (robotInstance == null && !robotInitFailed) {
            try {
                robotInstance = new Robot();
            } catch (Throwable t) {
                robotInitFailed = true;
            }
        }
        return robotInstance;
    }

    public boolean isActive() {
        return active;
    }

    public void start(ColorConsumer consumer) {
        this.active = true;
        this.onColorSelected = consumer;
        ClientSoundManager.getInstance().playButtonClick();
    }

    public void cancel() {
        this.active = false;
        this.onColorSelected = null;
        ClientSoundManager.getInstance().playButtonClick();
    }

    public void update(int mouseX, int mouseY) {
        if (!active) return;
        lastSampledColor = sampleScreenColor(mouseX, mouseY);
    }

    /**
     * Захватывает цвет пикселя с абсолютных экранных координат курсора (Windows Desktop).
     */
    public ColorRGBA sampleScreenColor(int mouseX, int mouseY) {
        Robot robot = getRobot();
        if (robot != null) {
            try {
                PointerInfo pointer = MouseInfo.getPointerInfo();
                if (pointer != null) {
                    Point pt = pointer.getLocation();
                    java.awt.Color awt = robot.getPixelColor(pt.x, pt.y);
                    return new ColorRGBA(awt.getRed(), awt.getGreen(), awt.getBlue(), 255.0F);
                }
            } catch (Throwable ignored) {
            }
        }

        // Fallback: OpenGL framebuffer внутри Minecraft
        try {
            float glX = (float) (mouseX * sr.getScaleFactor());
            float glY = (float) (mw.getHeight() - mouseY * sr.getScaleFactor());
            return ColorRGBA.fromPixel(glX, glY);
        } catch (Throwable t) {
            return ColorRGBA.WHITE;
        }
    }

    /**
     * Отрисовка парящей лупы у курсора мыши.
     */
    public void renderLoupe(UIContext context, int mouseX, int mouseY) {
        if (!active) return;

        update(mouseX, mouseY);

        float lx = mouseX + 14.0F;
        float ly = mouseY - 42.0F;

        // Корректировка выхода за пределы экрана
        int screenW = mc.getWindow().getScaledWidth();
        int screenH = mc.getWindow().getScaledHeight();
        if (lx + 130.0F > screenW) {
            lx = mouseX - 144.0F;
        }
        if (ly < 10.0F) {
            ly = mouseY + 18.0F;
        }

        float cardW = 126.0F;
        float cardH = 36.0F;
        BorderRadius r = BorderRadius.all(6.0F);

        // Тень и фон лупы
        context.drawPerimeterShadow(lx, ly + 1.0F, cardW, cardH, 8.0F, r, ColorRGBA.BLACK.withAlpha(150.0F));
        context.drawRoundedRect(lx, ly, cardW, cardH, r, new ColorRGBA(18, 20, 26, 240));
        context.drawRoundedBorder(lx, ly, cardW, cardH, 0.8F, r, ColorRGBA.WHITE.withAlpha(50.0F));

        // Свотч цвета слева с перекрестием
        float swatchSize = 24.0F;
        float swatchX = lx + 6.0F;
        float swatchY = ly + (cardH - swatchSize) / 2.0F;
        BorderRadius sr = BorderRadius.all(4.0F);

        context.drawRoundedRect(swatchX, swatchY, swatchSize, swatchSize, sr, lastSampledColor);
        context.drawRoundedBorder(swatchX, swatchY, swatchSize, swatchSize, 0.8F, sr, ColorRGBA.WHITE.withAlpha(160.0F));

        // Микро-прицел в центре свотча
        float cx = swatchX + swatchSize / 2.0F;
        float cy = swatchY + swatchSize / 2.0F;
        ColorRGBA crossColor = (lastSampledColor.getBrightness() > 0.5F) ? ColorRGBA.BLACK : ColorRGBA.WHITE;
        context.drawRect(cx - 3.0F, cy - 0.5F, 6.0F, 1.0F, crossColor.withAlpha(200.0F));
        context.drawRect(cx - 0.5F, cy - 3.0F, 1.0F, 6.0F, crossColor.withAlpha(200.0F));

        // Текстовая информация (RGB и HEX)
        Font font = Fonts.MEDIUM.getFont(6.5F);
        Font hintFont = Fonts.REGULAR.getFont(5.5F);
        float infoX = swatchX + swatchSize + 7.0F;

        String rgbText = String.format("RGB %d %d %d", (int) lastSampledColor.getRed(), (int) lastSampledColor.getGreen(), (int) lastSampledColor.getBlue());
        String hexText = lastSampledColor.toHex().substring(0, 7).toUpperCase();

        context.drawText(font, hexText, infoX, ly + 7.0F, Colors.getTextColor());
        context.drawText(hintFont, rgbText, infoX, ly + 17.0F, Colors.getTextColor().withAlpha(170.0F));

        // Подсказка управления внизу
        String hintText = Localizator.translate("colorpicker.pipette_hint");
        Font subHint = Fonts.REGULAR.getFont(5.0F);
        context.drawCenteredText(subHint, hintText, lx + cardW / 2.0F, ly + cardH + 4.0F, Colors.getTextColor().withAlpha(200.0F));
    }

    public boolean handleMouseClicked(double mouseX, double mouseY, MouseButton button) {
        if (!active) return false;

        if (button == MouseButton.LEFT) {
            ColorRGBA picked = sampleScreenColor((int) mouseX, (int) mouseY);
            if (onColorSelected != null) {
                onColorSelected.accept(picked);
            }
            active = false;
            onColorSelected = null;
            ClientSoundManager.getInstance().playButtonClick();
            return true;
        } else {
            cancel();
            return true;
        }
    }

    public boolean handleKeyPressed(int keyCode) {
        if (!active) return false;
        if (keyCode == 256) { // ESC
            cancel();
            return true;
        }
        return false;
    }
}
