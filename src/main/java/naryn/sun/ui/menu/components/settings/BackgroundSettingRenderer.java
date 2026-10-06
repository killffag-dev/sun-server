package naryn.sun.ui.menu.components.settings;

import naryn.sun.Sun;
import naryn.sun.framework.base.UIContext;
import naryn.sun.framework.msdf.Font;
import naryn.sun.framework.msdf.Fonts;
import naryn.sun.framework.objects.BorderRadius;
import naryn.sun.framework.objects.MouseButton;
import naryn.sun.systems.localization.Localizator;
import naryn.sun.systems.theme.ClientAppearance;
import naryn.sun.ui.menu.background.MenuBackgroundConfig;
import naryn.sun.ui.menu.background.MenuBackgroundMode;
import naryn.sun.ui.menu.skin.MenuSkin;
import naryn.sun.utility.animation.base.Animation;
import naryn.sun.utility.animation.base.Easing;
import naryn.sun.utility.colors.ColorRGBA;
import naryn.sun.utility.colors.Colors;
import naryn.sun.utility.game.cursor.CursorType;
import naryn.sun.utility.game.cursor.CursorUtility;
import naryn.sun.utility.gui.GuiUtility;
import naryn.sun.utility.render.DrawUtility;
import naryn.sun.utility.render.penis.PenisPlayer;
import naryn.sun.utility.sounds.ClientSoundManager;

import java.util.HashMap;
import java.util.Map;

/**
 * Карточка "Настройки фона" в GUI Settings.
 * Содержит:
 *  - Затемнение и размытие фона (перенесены из Appearance)
 *  - Выбор режима (Off, Cyber Grid 3D, Web, Hexagons, Warp)
 *  - Контекстные параметры под каждый режим (скрывает нерелевантные настройки)
 */
public class BackgroundSettingRenderer extends SettingRenderer {

    private static final String[] MODE_KEYS = {
            "OFF", "CYBER_GRID", "WEB", "HEXAGONS", "WARP"
    };
    private static final String[] MODE_NAMES = {
            "menu.gui_settings.background.mode.off",
            "menu.gui_settings.background.mode.cyber_grid",
            "menu.gui_settings.background.mode.web",
            "menu.gui_settings.background.mode.hexagons",
            "menu.gui_settings.background.mode.warp"
    };

    private boolean initializedPenis = false;
    private final Map<String, PenisPlayer> enablePenisMap = new HashMap<>();
    private final Map<String, PenisPlayer> disablePenisMap = new HashMap<>();
    private final Map<String, Boolean> lastStateMap = new HashMap<>();
    private final Map<String, Animation> modeHoverMap = new HashMap<>();
    private final Map<String, Animation> modeActiveMap = new HashMap<>();

    private boolean draggingDarkness = false;
    private boolean draggingBlur = false;
    private boolean draggingSpeed = false;
    private boolean draggingDensity = false;

    private void ensurePenis() {
        if (initializedPenis) return;
        for (String k : MODE_KEYS) {
            enablePenisMap.put(k, new PenisPlayer(Sun.id("penises/check_enable.penis")));
            disablePenisMap.put(k, new PenisPlayer(Sun.id("penises/check_disable.penis")));
            lastStateMap.put(k, false);
        }
        initializedPenis = true;
    }

    private Animation getModeHoverAnim(String key) {
        return modeHoverMap.computeIfAbsent(key, k -> new Animation(300L, 0.0F, Easing.FIGMA_EASE_IN_OUT));
    }

    private Animation getModeActiveAnim(String key, boolean initial) {
        return modeActiveMap.computeIfAbsent(key, k -> new Animation(300L, initial ? 1.0F : 0.0F, Easing.FIGMA_EASE_IN_OUT));
    }

    @Override
    public void render(UIContext context, float x, float y, float width, float settY, float expand) {
        ensurePenis();
        MenuBackgroundConfig cfg = MenuBackgroundConfig.getInstance();
        MenuBackgroundMode mode = cfg.getMode();

        // 1. Выбор режима фона
        renderModeBox(context, x, width, settY,
                "menu.gui_settings.background.title",
                MODE_KEYS, MODE_KEYS, MODE_NAMES,
                mode.name());

        float boxH = getModeBoxHeight(MODE_KEYS.length);
        float curY = settY + boxH + SETTINGS_SEP_Y;

        // 2. Затемнение фона (всегда доступно)
        renderModulesSlider(context, x, y, width, curY,
                "menu.gui_settings.appearance.bg_darkness",
                ClientAppearance.getMenuBackgroundDarkness(), 0.0F, 1.0F, "BG_DARKNESS",
                draggingDarkness);
        curY += SLIDER_ROW_H + 2.0F;

        // 3. Размытие фона (всегда доступно)
        renderModulesSlider(context, x, y, width, curY,
                "menu.gui_settings.appearance.bg_blur",
                ClientAppearance.getMenuBackgroundBlur(), 0.0F, 1.0F, "BG_BLUR",
                draggingBlur);
        curY += SLIDER_ROW_H + 2.0F;

        // 4. Параметры активного эффекта (скрыты, если режим OFF)
        if (mode != MenuBackgroundMode.OFF) {
            // Скорость анимации
            float speedNorm = (cfg.getSpeed() - 0.2F) / (2.5F - 0.2F);
            renderModulesSlider(context, x, y, width, curY,
                    "menu.gui_settings.background.speed",
                    speedNorm, 0.0F, 1.0F, "BG_SPEED",
                    draggingSpeed);
            curY += SLIDER_ROW_H + 2.0F;

            // Плотность / Количество
            float densityNorm = (cfg.getDensity() - 0.2F) / (2.0F - 0.2F);
            renderModulesSlider(context, x, y, width, curY,
                    "menu.gui_settings.background.density",
                    densityNorm, 0.0F, 1.0F, "BG_DENSITY",
                    draggingDensity);
            curY += SLIDER_ROW_H + 4.0F;

            // Волны при клике (ТОЛЬКО для WEB и HEXAGONS)
            if (mode == MenuBackgroundMode.WEB || mode == MenuBackgroundMode.HEXAGONS) {
                renderModulesToggle(context, x, y, width, curY,
                        "menu.gui_settings.background.shockwaves",
                        cfg.isShockwaves(), "BG_SHOCKWAVES");
                curY += TOGGLE_ROW_H + 2.0F;
            }

            // Реакция на мышь (для CYBER_GRID, WEB, HEXAGONS)
            if (mode == MenuBackgroundMode.CYBER_GRID || mode == MenuBackgroundMode.WEB || mode == MenuBackgroundMode.HEXAGONS) {
                renderModulesToggle(context, x, y, width, curY,
                        "menu.gui_settings.background.mouse_react",
                        cfg.isMouseInteraction(), "BG_MOUSE_REACT");
                curY += TOGGLE_ROW_H + 2.0F;
            }

            // Цвет темы (для всех активных эффектов)
            renderModulesToggle(context, x, y, width, curY,
                    "menu.gui_settings.background.sync_accent",
                    cfg.isSyncAccent(), "BG_SYNC_ACCENT");
        }
    }

    @Override
    public void handleClick(float x, float y, float width, float settY, double mouseX, double mouseY) {
        MenuBackgroundConfig cfg = MenuBackgroundConfig.getInstance();
        MenuBackgroundMode mode = cfg.getMode();
        float innerX = x + 9.0F;
        float innerW = width - 18.0F;
        float boxTop = settY + 17.0F;

        // 1. Клик по режимам
        for (int i = 0; i < MODE_KEYS.length; i++) {
            float rowTop = boxTop + 3.0F + i * 19.0F;
            if (GuiUtility.isHovered((double) (innerX - 1.0F), (double) rowTop, (double) (innerW + 2.0F), 19.0, mouseX, mouseY)) {
                cfg.setMode(MenuBackgroundMode.valueOf(MODE_KEYS[i]));
                Sun.getInstance().getFileManager().writeFile("client");
                ClientSoundManager.getInstance().playButtonClick();
                return;
            }
        }

        float boxH = getModeBoxHeight(MODE_KEYS.length);
        float curY = settY + boxH + SETTINGS_SEP_Y;

        // 2. Слайдер затемнения
        if (GuiUtility.isHovered(x, curY, width, SLIDER_ROW_H, mouseX, mouseY)) {
            draggingDarkness = true;
            updateDarkness(innerX, innerW, mouseX);
            return;
        }
        curY += SLIDER_ROW_H + 2.0F;

        // 3. Слайдер размытия
        if (GuiUtility.isHovered(x, curY, width, SLIDER_ROW_H, mouseX, mouseY)) {
            draggingBlur = true;
            updateBlur(innerX, innerW, mouseX);
            return;
        }
        curY += SLIDER_ROW_H + 2.0F;

        // 4. Параметры активных эффектов
        if (mode != MenuBackgroundMode.OFF) {
            // Скорость
            if (GuiUtility.isHovered(x, curY, width, SLIDER_ROW_H, mouseX, mouseY)) {
                draggingSpeed = true;
                updateSpeed(innerX, innerW, mouseX);
                return;
            }
            curY += SLIDER_ROW_H + 2.0F;

            // Плотность
            if (GuiUtility.isHovered(x, curY, width, SLIDER_ROW_H, mouseX, mouseY)) {
                draggingDensity = true;
                updateDensity(innerX, innerW, mouseX);
                return;
            }
            curY += SLIDER_ROW_H + 4.0F;

            // Волны (WEB / HEXAGONS)
            if (mode == MenuBackgroundMode.WEB || mode == MenuBackgroundMode.HEXAGONS) {
                if (GuiUtility.isHovered(x, curY, width, TOGGLE_ROW_H, mouseX, mouseY)) {
                    cfg.setShockwaves(!cfg.isShockwaves());
                    Sun.getInstance().getFileManager().writeFile("client");
                    ClientSoundManager.getInstance().playButtonClick();
                    return;
                }
                curY += TOGGLE_ROW_H + 2.0F;
            }

            // Реакция на мышь (CYBER_GRID / WEB / HEXAGONS)
            if (mode == MenuBackgroundMode.CYBER_GRID || mode == MenuBackgroundMode.WEB || mode == MenuBackgroundMode.HEXAGONS) {
                if (GuiUtility.isHovered(x, curY, width, TOGGLE_ROW_H, mouseX, mouseY)) {
                    cfg.setMouseInteraction(!cfg.isMouseInteraction());
                    Sun.getInstance().getFileManager().writeFile("client");
                    ClientSoundManager.getInstance().playButtonClick();
                    return;
                }
                curY += TOGGLE_ROW_H + 2.0F;
            }

            // Цвет темы
            if (GuiUtility.isHovered(x, curY, width, TOGGLE_ROW_H, mouseX, mouseY)) {
                cfg.setSyncAccent(!cfg.isSyncAccent());
                Sun.getInstance().getFileManager().writeFile("client");
                ClientSoundManager.getInstance().playButtonClick();
                return;
            }
        }
    }

    @Override
    public void onMouseDragged(float x, float width, double mouseX, double mouseY) {
        float innerX = x + 9.0F;
        float innerW = width - 18.0F;
        if (draggingDarkness) {
            updateDarkness(innerX, innerW, mouseX);
        } else if (draggingBlur) {
            updateBlur(innerX, innerW, mouseX);
        } else if (draggingSpeed) {
            updateSpeed(innerX, innerW, mouseX);
        } else if (draggingDensity) {
            updateDensity(innerX, innerW, mouseX);
        }
    }

    @Override
    public void onMouseReleased(double mouseX, double mouseY, MouseButton button) {
        if (draggingDarkness || draggingBlur || draggingSpeed || draggingDensity) {
            Sun.getInstance().getFileManager().writeFile("client");
            draggingDarkness = false;
            draggingBlur = false;
            draggingSpeed = false;
            draggingDensity = false;
        }
    }

    @Override
    public boolean isDragging() {
        return draggingDarkness || draggingBlur || draggingSpeed || draggingDensity;
    }

    private void updateDarkness(float sliderX, float sliderW, double mouseX) {
        float val = GuiUtility.getSliderValue(0.0F, 1.0F, sliderX, sliderW, mouseX);
        val = Math.round(val * 100.0F) / 100.0F;
        ClientAppearance.setMenuBackgroundDarkness(val);
    }

    private void updateBlur(float sliderX, float sliderW, double mouseX) {
        float val = GuiUtility.getSliderValue(0.0F, 1.0F, sliderX, sliderW, mouseX);
        val = Math.round(val * 100.0F) / 100.0F;
        ClientAppearance.setMenuBackgroundBlur(val);
    }

    private void updateSpeed(float sliderX, float sliderW, double mouseX) {
        float norm = GuiUtility.getSliderValue(0.0F, 1.0F, sliderX, sliderW, mouseX);
        float val = 0.2F + norm * (2.5F - 0.2F);
        val = Math.round(val * 100.0F) / 100.0F;
        MenuBackgroundConfig.getInstance().setSpeed(val);
    }

    private void updateDensity(float sliderX, float sliderW, double mouseX) {
        float norm = GuiUtility.getSliderValue(0.0F, 1.0F, sliderX, sliderW, mouseX);
        float val = 0.2F + norm * (2.0F - 0.2F);
        val = Math.round(val * 100.0F) / 100.0F;
        MenuBackgroundConfig.getInstance().setDensity(val);
    }

    @Override
    public float getContentHeight() {
        MenuBackgroundConfig cfg = MenuBackgroundConfig.getInstance();
        MenuBackgroundMode mode = cfg.getMode();

        float boxH = getModeBoxHeight(MODE_KEYS.length);
        float h = SETTINGS_SEP_Y + boxH + SETTINGS_SEP_Y;

        // Darkness + blur
        h += (SLIDER_ROW_H + 2.0F) * 2.0F;

        if (mode != MenuBackgroundMode.OFF) {
            // Speed + Density
            h += (SLIDER_ROW_H + 2.0F) * 2.0F + 2.0F;

            int toggles = 1; // sync_accent
            if (mode == MenuBackgroundMode.WEB || mode == MenuBackgroundMode.HEXAGONS) {
                toggles++; // shockwaves
            }
            if (mode == MenuBackgroundMode.CYBER_GRID || mode == MenuBackgroundMode.WEB || mode == MenuBackgroundMode.HEXAGONS) {
                toggles++; // mouse_react
            }
            h += toggles * (TOGGLE_ROW_H + 2.0F);
        }

        return h + CARD_BOTTOM_PAD;
    }

    private void renderModeBox(UIContext context, float x, float width, float settY,
                               String titleKey, String[] keys, String[] animKeys,
                               String[] names, String selectedKey) {
        Font nameFont = Fonts.REGULAR.getFont(8.0F);
        Font valueFont = Fonts.REGULAR.getFont(7.0F);
        float innerX = x + 9.0F;
        float innerW = width - 18.0F;
        float leftPadding = 10.0F;

        String title = Localizator.translate(titleKey);
        context.drawText(nameFont, title, x + leftPadding, settY + 4.0F,
                Colors.getTextColor().withAlpha(220));

        float boxTop = settY + 17.0F;
        float totalValuesH = keys.length * 19.0F;

        MenuSkin.current().renderSettingBox(
                context, innerX - 1.0F, boxTop, innerW + 2.0F, 8.0F + totalValuesH,
                BorderRadius.all(6.0F), 1.0F
        );

        float offset = 0.0F;
        for (int i = 0; i < keys.length; i++) {
            String k = keys[i];
            String ak = animKeys != null && i < animKeys.length ? animKeys[i] : k;
            boolean selected = k.equalsIgnoreCase(selectedKey);
            float rowHeight = 19.0F;
            float rowTop = boxTop + 3.0F + offset;

            boolean hover = GuiUtility.isHovered(
                    (double) (innerX - 1.0F), (double) rowTop, (double) (innerW + 2.0F), (double) rowHeight,
                    context.getMouseX(), context.getMouseY()
            );
            if (hover) CursorUtility.set(CursorType.HAND);

            Animation hAnim = getModeHoverAnim(ak);
            hAnim.update(hover);
            Animation aAnim = getModeActiveAnim(ak, selected);
            aAnim.update(selected);

            PenisPlayer enableP = enablePenisMap.get(ak);
            PenisPlayer disableP = disablePenisMap.get(ak);
            boolean lastState = lastStateMap.getOrDefault(ak, false);
            if (selected != lastState) {
                if (selected && enableP != null) enableP.playOnce();
                else if (disableP != null) disableP.playOnce();
                lastStateMap.put(ak, selected);
            }

            PenisPlayer currentP = selected ? enableP : disableP;
            if (currentP != null) currentP.update();

            ColorRGBA valColor = Colors.getTextColor()
                    .withAlpha(255.0F * (0.75F + 0.25F * hAnim.getValue() + 0.25F * aAnim.getValue()));
            float lineY = rowTop + valueFont.height() + 0.5F;
            context.drawText(valueFont, Localizator.translate(names[i]), innerX + 8.0F, lineY, valColor);

            if (currentP != null && (aAnim.getValue() > 0.0F || currentP.isPlaying())) {
                DrawUtility.drawAnimationSprite(
                        context.getMatrices(),
                        currentP.getCurrentSprite(),
                        innerX + innerW - 11.0F - aAnim.getValue() * 2.0F,
                        rowTop + valueFont.height() - 1.0F,
                        6.0F, 6.0F,
                        Colors.getTextColor().mulAlpha(0.1F + 0.9F * aAnim.getValue())
                );
            }

            offset += rowHeight;
        }
    }
}
