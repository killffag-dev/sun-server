package naryn.sun.ui.menu.components.settings;

import naryn.sun.Sun;
import naryn.sun.framework.base.UIContext;
import naryn.sun.framework.msdf.Font;
import naryn.sun.framework.msdf.Fonts;
import naryn.sun.framework.objects.BorderRadius;
import naryn.sun.framework.objects.MouseButton;
import naryn.sun.framework.objects.gradient.Gradient;
import naryn.sun.systems.localization.Localizator;
import naryn.sun.systems.theme.PaletteConfig;
import naryn.sun.ui.components.ColorPicker;
import naryn.sun.ui.menu.NewScreen;
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
 * Рендерер настроек Цветовой гаммы (Palette) в GUI Settings.
 * Содержит мастер-тумблер, бокс пресетов (включая Радугу и Кастомный с ColorPicker) и тумблеры синхронизации.
 */
public class PaletteRenderer extends SettingRenderer {

    private boolean initializedPenis = false;
    private final Map<String, PenisPlayer> enablePenisMap = new HashMap<>();
    private final Map<String, PenisPlayer> disablePenisMap = new HashMap<>();
    private final Map<String, Boolean> lastStateMap = new HashMap<>();
    private final Map<String, Animation> modeHoverMap = new HashMap<>();
    private final Map<String, Animation> modeActiveMap = new HashMap<>();

    private ColorPicker customColorPicker = null;

    private void ensurePenis() {
        if (initializedPenis) return;
        for (PaletteConfig.PaletteMode m : PaletteConfig.PaletteMode.values()) {
            String k = m.name();
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

    private void openCustomColorPicker() {
        PaletteConfig cfg = PaletteConfig.getInstance();
        if (customColorPicker != null && customColorPicker.isShowing()) {
            return;
        }
        if (Sun.getInstance().getMenuScreen() instanceof NewScreen newScreen) {
            if (customColorPicker != null) {
                newScreen.getColorPickers().remove(customColorPicker);
            }
            String title = Localizator.translate("menu.gui_settings.palette.mode.custom");
            customColorPicker = newScreen.createCenteredColorPicker(false, cfg.getCustomColor(), title);
        }
    }

    @Override
    public void render(UIContext context, float x, float y, float width, float settY, float expand) {
        ensurePenis();
        PaletteConfig cfg = PaletteConfig.getInstance();

        // Живая синхронизация выбранного цвета из колор-пикера
        if (customColorPicker != null) {
            if (customColorPicker.isShowing()) {
                ColorRGBA picked = customColorPicker.built();
                if (!picked.equals(cfg.getCustomColor())) {
                    cfg.setCustomColor(picked);
                }
            } else if (customColorPicker.getAnimation().getValue() == 0.0F) {
                customColorPicker = null;
                Sun.getInstance().getFileManager().writeFile("client");
            }
        }

        // 1. Мастер-переключатель
        renderModulesToggle(context, x, y, width, settY, "menu.gui_settings.palette.enable",
                cfg.isEnabled(), "PALETTE_MASTER");
        settY += TOGGLE_ROW_H + SETTINGS_SEP_Y;

        // 2. Бокс пресетов
        renderPresetsBox(context, x, width, settY, cfg);
        settY += getModeBoxHeight(PaletteConfig.PaletteMode.values().length) + SETTINGS_SEP_Y;

        // 3. Тумблеры синхронизации
        renderModulesToggle(context, x, y, width, settY, "menu.gui_settings.palette.sync_gui",
                cfg.isSyncGui(), "PALETTE_SYNC_GUI");
        settY += TOGGLE_ROW_H;

        renderModulesToggle(context, x, y, width, settY, "menu.gui_settings.palette.sync_modules",
                cfg.isSyncModules(), "PALETTE_SYNC_MODS");
    }

    @Override
    public void handleClick(float x, float y, float width, float settY, double mouseX, double mouseY) {
        PaletteConfig cfg = PaletteConfig.getInstance();

        // 1. Клик по мастер-переключателю
        if (GuiUtility.isHovered(x, settY, width, TOGGLE_ROW_H, mouseX, mouseY)) {
            cfg.setEnabled(!cfg.isEnabled());
            Sun.getInstance().getFileManager().writeFile("client");
            ClientSoundManager.getInstance().playButtonClick();
            return;
        }

        // 2. Клик по пресетам
        float boxY = settY + TOGGLE_ROW_H + SETTINGS_SEP_Y;
        float innerX = x + 9.0F;
        float innerW = width - 18.0F;
        float boxTop = boxY + 17.0F;

        PaletteConfig.PaletteMode[] modes = PaletteConfig.PaletteMode.values();
        for (int i = 0; i < modes.length; i++) {
            float rowTop = boxTop + 3.0F + i * 19.0F;
            if (GuiUtility.isHovered((double)(innerX - 1.0F), (double)rowTop, (double)(innerW + 2.0F), 19.0, mouseX, mouseY)) {
                PaletteConfig.PaletteMode selectedMode = modes[i];
                cfg.setMode(selectedMode);
                if (!cfg.isEnabled()) {
                    cfg.setEnabled(true);
                }
                if (selectedMode == PaletteConfig.PaletteMode.CUSTOM) {
                    openCustomColorPicker();
                } else if (customColorPicker != null && customColorPicker.isShowing()) {
                    customColorPicker.setShowing(false);
                }
                Sun.getInstance().getFileManager().writeFile("client");
                ClientSoundManager.getInstance().playButtonClick();
                return;
            }
        }

        // 3. Клик по тумблерам синхронизации
        float subY = boxY + getModeBoxHeight(modes.length) + SETTINGS_SEP_Y;
        if (GuiUtility.isHovered(x, subY, width, TOGGLE_ROW_H, mouseX, mouseY)) {
            cfg.setSyncGui(!cfg.isSyncGui());
            Sun.getInstance().getFileManager().writeFile("client");
            ClientSoundManager.getInstance().playButtonClick();
            return;
        }

        float sub2Y = subY + TOGGLE_ROW_H;
        if (GuiUtility.isHovered(x, sub2Y, width, TOGGLE_ROW_H, mouseX, mouseY)) {
            cfg.setSyncModules(!cfg.isSyncModules());
            Sun.getInstance().getFileManager().writeFile("client");
            ClientSoundManager.getInstance().playButtonClick();
        }
    }

    @Override
    public void onMouseReleased(double mouseX, double mouseY, MouseButton button) {
        if (customColorPicker != null && customColorPicker.isShowing()) {
            Sun.getInstance().getFileManager().writeFile("client");
        }
    }

    @Override
    public float getContentHeight() {
        int modesCount = PaletteConfig.PaletteMode.values().length;
        return TOGGLE_ROW_H + SETTINGS_SEP_Y + getModeBoxHeight(modesCount) + SETTINGS_SEP_Y + TOGGLE_ROW_H + TOGGLE_ROW_H + CARD_BOTTOM_PAD;
    }

    private void renderPresetsBox(UIContext context, float x, float width, float settY, PaletteConfig cfg) {
        Font nameFont = Fonts.REGULAR.getFont(8.0F);
        Font valueFont = Fonts.REGULAR.getFont(7.0F);
        float innerX = x + 9.0F;
        float innerW = width - 18.0F;
        float leftPadding = 10.0F;

        String title = Localizator.translate("menu.gui_settings.palette.presets");
        context.drawText(nameFont, title, x + leftPadding, settY + 4.0F,
                Colors.getTextColor().withAlpha(220));

        float boxTop = settY + 17.0F;
        PaletteConfig.PaletteMode[] modes = PaletteConfig.PaletteMode.values();
        float totalValuesH = modes.length * 19.0F;

        MenuSkin.current().renderSettingBox(
                context, innerX - 1.0F, boxTop, innerW + 2.0F, 8.0F + totalValuesH,
                BorderRadius.all(6.0F), 1.0F
        );

        float offset = 0.0F;
        for (PaletteConfig.PaletteMode m : modes) {
            String k = m.name();
            boolean selected = (cfg.getMode() == m);
            float rowHeight = 19.0F;
            float rowTop = boxTop + 3.0F + offset;

            boolean hover = GuiUtility.isHovered(
                    (double)(innerX - 1.0F), (double)rowTop, (double)(innerW + 2.0F), (double)rowHeight,
                    context.getMouseX(), context.getMouseY()
            );
            if (hover) CursorUtility.set(CursorType.HAND);

            Animation hAnim = getModeHoverAnim(k);
            hAnim.update(hover);
            Animation aAnim = getModeActiveAnim(k, selected);
            aAnim.update(selected);

            PenisPlayer enableP = enablePenisMap.get(k);
            PenisPlayer disableP = disablePenisMap.get(k);
            boolean lastState = lastStateMap.getOrDefault(k, false);
            if (selected != lastState) {
                if (selected && enableP != null) enableP.playOnce();
                else if (disableP != null) disableP.playOnce();
                lastStateMap.put(k, selected);
            }

            PenisPlayer currentP = selected ? enableP : disableP;
            if (currentP != null) currentP.update();

            // Круглый индикатор цвета режима слева от названия
            float dotRadius = 3.0F;
            float dotX = innerX + 7.0F + dotRadius;
            float dotY = rowTop + rowHeight / 2.0F;

            if (m == PaletteConfig.PaletteMode.RAINBOW) {
                // Живой радужный спектр: 4 вращающиеся фазы с шагом 90 градусов
                ColorRGBA c1 = PaletteConfig.getRainbowColor(0.0F, 1.0F, 1.0F);
                ColorRGBA c2 = PaletteConfig.getRainbowColor(0.25F, 1.0F, 1.0F);
                ColorRGBA c3 = PaletteConfig.getRainbowColor(0.50F, 1.0F, 1.0F);
                ColorRGBA c4 = PaletteConfig.getRainbowColor(0.75F, 1.0F, 1.0F);
                Gradient rainbowGrad = Gradient.of(c1, c3, c2, c4);
                context.drawRoundedRect(dotX - dotRadius, dotY - dotRadius, dotRadius * 2.0F, dotRadius * 2.0F, BorderRadius.all(dotRadius), rainbowGrad);
                context.drawRoundedBorder(dotX - dotRadius, dotY - dotRadius, dotRadius * 2.0F, dotRadius * 2.0F, 0.5F, BorderRadius.all(dotRadius), Colors.WHITE.withAlpha(60));
            } else {
                ColorRGBA dotColor = PaletteConfig.getStaticColorForMode(m);
                context.drawRoundedRect(dotX - dotRadius, dotY - dotRadius, dotRadius * 2.0F, dotRadius * 2.0F, BorderRadius.all(dotRadius), dotColor.withAlpha(240));
                context.drawRoundedBorder(dotX - dotRadius, dotY - dotRadius, dotRadius * 2.0F, dotRadius * 2.0F, 0.5F, BorderRadius.all(dotRadius), Colors.getTextColor().withAlpha(35));
            }

            ColorRGBA valColor = Colors.getTextColor()
                    .withAlpha(255.0F * (0.75F + 0.25F * hAnim.getValue() + 0.25F * aAnim.getValue()));
            float lineY = rowTop + valueFont.height() + 0.5F;
            context.drawText(valueFont, Localizator.translate(m.getNameKey()), innerX + 17.0F, lineY, valColor);

            // Иконка пипетки/настройки для кастомного цвета
            if (m == PaletteConfig.PaletteMode.CUSTOM) {
                float pipSize = 7.0F;
                float pipX = innerX + innerW - 22.0F;
                float pipY = rowTop + (rowHeight - pipSize) / 2.0F;
                ColorRGBA pipColor = selected
                        ? Colors.ACCENT.withAlpha(220)
                        : Colors.getTextColor().withAlpha((int)(255 * (0.35F + 0.35F * hAnim.getValue())));
                context.drawTexture(Sun.id("icons/colorpicker/pipette.png"), pipX, pipY, pipSize, pipSize, pipColor);
            }

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
