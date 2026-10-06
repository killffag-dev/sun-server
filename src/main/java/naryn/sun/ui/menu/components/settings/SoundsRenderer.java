package naryn.sun.ui.menu.components.settings;

import naryn.sun.Sun;
import naryn.sun.framework.base.UIContext;
import naryn.sun.framework.msdf.Font;
import naryn.sun.framework.msdf.Fonts;
import naryn.sun.framework.objects.BorderRadius;
import naryn.sun.framework.objects.MouseButton;
import naryn.sun.systems.localization.Localizator;
import naryn.sun.ui.menu.skin.MenuSkin;
import naryn.sun.utility.animation.base.Animation;
import naryn.sun.utility.animation.base.Easing;
import naryn.sun.utility.colors.Colors;
import naryn.sun.utility.game.cursor.CursorType;
import naryn.sun.utility.game.cursor.CursorUtility;
import naryn.sun.utility.gui.GuiUtility;
import naryn.sun.utility.sounds.ClientSoundManager;
import naryn.sun.utility.sounds.SoundConfig;
import naryn.sun.utility.sounds.SoundType;

/**
 * Рендерер настроек Sounds (Звуки).
 * Содержит мастер-громкость, toggle/slider для каждого типа звука,
 * бокс «Мои звуки» с кнопкой папки.
 */
public class SoundsRenderer extends SettingRenderer {

    private enum DraggingSlider {
        NONE, MASTER, SCROLL, TYPING, MODULE_TOGGLE, BUTTON_CLICK, MENU_OPEN
    }

    private DraggingSlider activeDrag = DraggingSlider.NONE;
    private final Animation folderBtnHoverAnim = new Animation(300L, 0.0F, Easing.FIGMA_EASE_IN_OUT);

    @Override
    public void render(UIContext context, float x, float y, float width, float settY, float expand) {
        SoundConfig soundConfig = ClientSoundManager.getInstance().getConfig();

        // 1. Бокс «Мои звуки»
        renderMySoundsBox(context, x, width, settY, soundConfig);
        settY += 48.0F;

        // 2. Слайдер «Общая громкость»
        renderModulesSlider(context, x, y, width, settY, "menu.gui_settings.sounds.master_volume",
                soundConfig.getMasterVolume(), 0.0F, 1.0F, "MASTER",
                activeDrag == DraggingSlider.MASTER);
        settY += SLIDER_ROW_H + 2.0F;

        // 3. Индивидуальные звуки
        SoundType[] soundTypes = SoundType.values();
        for (SoundType st : soundTypes) {
            String key = st.name();
            boolean enabled = soundConfig.isEnabled(st);

            renderModulesToggle(context, x, y, width, settY, st.getNameKey(), enabled, key);
            settY += TOGGLE_ROW_H;

            renderModulesSlider(context, x, y, width, settY,
                    "   └ " + Localizator.translate("menu.gui_settings.sounds.volume"),
                    soundConfig.getVolume(st), 0.0F, 1.0F, key,
                    mapSoundTypeToDrag(st) == activeDrag);
            settY += SLIDER_ROW_H + 2.0F;
        }
    }

    private void renderMySoundsBox(UIContext context, float x, float width, float settY, SoundConfig soundConfig) {
        float innerX = x + 9.0F;
        float innerW = width - 18.0F;
        float boxH = 42.0F;

        MenuSkin.current().renderSettingBox(context, innerX - 1.0F, settY, innerW + 2.0F, boxH, BorderRadius.all(6.0F), 1.0F);

        float toggleRowY = settY + 3.0F;
        renderModulesToggle(context, x, 0, width, toggleRowY, "menu.gui_settings.sounds.custom",
                soundConfig.isCustomSoundsEnabled(), "CUSTOM_SOUNDS");

        float btnY = settY + 20.0F;
        float btnX = innerX + 6.0F;
        float btnW = innerW - 12.0F;
        float btnH = 17.0F;

        boolean btnHover = GuiUtility.isHovered(btnX, btnY, btnW, btnH, context.getMouseX(), context.getMouseY());
        folderBtnHoverAnim.update(btnHover);
        if (btnHover) CursorUtility.set(CursorType.HAND);

        Font btnFont = Fonts.REGULAR.getFont(7.0F);
        MenuSkin.current().renderButtonBox(context, btnX, btnY, btnW, btnH, BorderRadius.all(5.0F), folderBtnHoverAnim.getValue(), 1.0F);
        context.drawCenteredText(btnFont, Localizator.translate("menu.gui_settings.sounds.open_folder"),
                btnX + btnW / 2.0F, btnY + (btnH - btnFont.height()) / 2.0F,
                Colors.getTextColor().withAlpha(255.0F * (0.80F + 0.20F * folderBtnHoverAnim.getValue())));
    }

    @Override
    public void handleClick(float x, float y, float width, float settY, double mouseX, double mouseY) {
        SoundConfig soundConfig = ClientSoundManager.getInstance().getConfig();
        float innerX = x + 9.0F;
        float innerW = width - 18.0F;

        // 1. Тогл пользовательских звуков
        if (GuiUtility.isHovered(x, settY + 3.0F, width, TOGGLE_ROW_H, mouseX, mouseY)) {
            soundConfig.setCustomSoundsEnabled(!soundConfig.isCustomSoundsEnabled());
            Sun.getInstance().getFileManager().writeFile("client");
            ClientSoundManager.getInstance().playButtonClick();
            return;
        }

        // Кнопка «Папка звуков»
        float btnY = settY + 20.0F;
        float btnX = innerX + 6.0F;
        float btnW = innerW - 12.0F;
        if (GuiUtility.isHovered(btnX, btnY, btnW, 17.0, mouseX, mouseY)) {
            ClientSoundManager.getInstance().openSoundsFolder();
            ClientSoundManager.getInstance().playButtonClick();
            return;
        }

        settY += 48.0F;

        // 2. Слайдер Master volume
        if (GuiUtility.isHovered(x, settY, width, SLIDER_ROW_H, mouseX, mouseY)) {
            activeDrag = DraggingSlider.MASTER;
            updateMasterVolume(innerX, innerW, mouseX);
            return;
        }
        settY += SLIDER_ROW_H + 2.0F;

        // 3. Индивидуальные звуки
        SoundType[] soundTypes = SoundType.values();
        for (SoundType st : soundTypes) {
            if (GuiUtility.isHovered(x, settY, width, TOGGLE_ROW_H, mouseX, mouseY)) {
                soundConfig.setEnabled(st, !soundConfig.isEnabled(st));
                Sun.getInstance().getFileManager().writeFile("client");
                ClientSoundManager.getInstance().playButtonClick();
                return;
            }
            settY += TOGGLE_ROW_H;

            if (GuiUtility.isHovered(x, settY, width, SLIDER_ROW_H, mouseX, mouseY)) {
                activeDrag = mapSoundTypeToDrag(st);
                updateSoundVolume(st, innerX, innerW, mouseX);
                return;
            }
            settY += SLIDER_ROW_H + 2.0F;
        }
    }

    @Override
    public void onMouseDragged(float x, float width, double mouseX, double mouseY) {
        if (activeDrag == DraggingSlider.NONE) return;
        float innerX = x + 9.0F;
        float innerW = width - 18.0F;

        if (activeDrag == DraggingSlider.MASTER) {
            updateMasterVolume(innerX, innerW, mouseX);
            return;
        }

        SoundType[] soundTypes = SoundType.values();
        for (SoundType st : soundTypes) {
            if (mapSoundTypeToDrag(st) == activeDrag) {
                updateSoundVolume(st, innerX, innerW, mouseX);
                return;
            }
        }
    }

    @Override
    public void onMouseReleased(double mouseX, double mouseY, MouseButton button) {
        if (activeDrag != DraggingSlider.NONE) {
            SoundType preview = dragToSoundType(activeDrag);
            if (preview != null) {
                ClientSoundManager.getInstance().playSoundPreview(preview);
            } else if (activeDrag == DraggingSlider.MASTER) {
                ClientSoundManager.getInstance().playSoundPreview(SoundType.BUTTON_CLICK);
            }
            Sun.getInstance().getFileManager().writeFile("client");
            activeDrag = DraggingSlider.NONE;
        }
    }

    @Override
    public boolean isDragging() {
        return activeDrag != DraggingSlider.NONE;
    }

    @Override
    public float getContentHeight() {
        float h = SETTINGS_SEP_Y + 48.0F + SLIDER_ROW_H + 2.0F;
        h += SoundType.values().length * (TOGGLE_ROW_H + SLIDER_ROW_H + 2.0F);
        h += CARD_BOTTOM_PAD;
        return h;
    }

    // ==================== HELPERS ====================

    private void updateMasterVolume(float sliderX, float sliderW, double mouseX) {
        float val = GuiUtility.getSliderValue(0.0F, 1.0F, sliderX, sliderW, mouseX);
        val = Math.round(val * 100.0F) / 100.0F;
        ClientSoundManager.getInstance().getConfig().setMasterVolume(val);
    }

    private void updateSoundVolume(SoundType type, float sliderX, float sliderW, double mouseX) {
        float val = GuiUtility.getSliderValue(0.0F, 1.0F, sliderX, sliderW, mouseX);
        val = Math.round(val * 100.0F) / 100.0F;
        ClientSoundManager.getInstance().getConfig().setVolume(type, val);
    }

    private DraggingSlider mapSoundTypeToDrag(SoundType type) {
        return switch (type) {
            case SCROLL -> DraggingSlider.SCROLL;
            case TYPING -> DraggingSlider.TYPING;
            case MODULE_TOGGLE -> DraggingSlider.MODULE_TOGGLE;
            case BUTTON_CLICK -> DraggingSlider.BUTTON_CLICK;
            case MENU_OPEN -> DraggingSlider.MENU_OPEN;
        };
    }

    private SoundType dragToSoundType(DraggingSlider drag) {
        return switch (drag) {
            case SCROLL -> SoundType.SCROLL;
            case TYPING -> SoundType.TYPING;
            case MODULE_TOGGLE -> SoundType.MODULE_TOGGLE;
            case BUTTON_CLICK -> SoundType.BUTTON_CLICK;
            case MENU_OPEN -> SoundType.MENU_OPEN;
            default -> null;
        };
    }
}
