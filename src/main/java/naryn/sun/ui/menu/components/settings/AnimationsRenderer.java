package naryn.sun.ui.menu.components.settings;

import naryn.sun.Sun;
import naryn.sun.framework.base.UIContext;
import naryn.sun.framework.objects.MouseButton;
import naryn.sun.systems.animation.ClientAnimationConfig;
import naryn.sun.systems.modules.modules.visuals.MenuModule;
import naryn.sun.systems.setting.settings.SliderSetting;
import naryn.sun.utility.gui.GuiUtility;
import naryn.sun.utility.sounds.ClientSoundManager;

/**
 * Рендерер настроек Animations.
 * Содержит мастер-toggle, 5 суб-toggle и слайдер скорости анимаций.
 */
public class AnimationsRenderer extends SettingRenderer {

    private boolean animSpeedDragging = false;
    private boolean scrollSpeedDragging = false;

    @Override
    public void render(UIContext context, float x, float y, float width, float settY, float expand) {
        ClientAnimationConfig cfg = ClientAnimationConfig.getInstance();
        float rowY = settY;

        // Master toggle
        renderModulesToggle(context, x, y, width, rowY, "menu.gui_settings.animations.master",
                cfg.isAnimationsEnabled(), "ANIM_MASTER");
        rowY += TOGGLE_ROW_H;

        // Sub-toggles
        renderModulesToggle(context, x, y, width, rowY, "menu.gui_settings.animations.button_hover",
                cfg.isButtonHoverLift(), "ANIM_BTN_HOVER");
        rowY += TOGGLE_ROW_H;

        renderModulesToggle(context, x, y, width, rowY, "menu.gui_settings.animations.smooth_tabs",
                cfg.isSmoothTabs(), "ANIM_SMOOTH_TABS");
        rowY += TOGGLE_ROW_H;

        renderModulesToggle(context, x, y, width, rowY, "menu.gui_settings.animations.smooth_tooltip",
                cfg.isSmoothTooltip(), "ANIM_SMOOTH_TOOLTIP");
        rowY += TOGGLE_ROW_H;

        renderModulesToggle(context, x, y, width, rowY, "menu.gui_settings.animations.star_effect",
                cfg.isStarEffect(), "ANIM_STAR");
        rowY += TOGGLE_ROW_H;

        renderModulesToggle(context, x, y, width, rowY, "menu.gui_settings.animations.toggle_physics",
                cfg.isTogglePhysics(), "ANIM_TOGGLE_PHYSICS");
        rowY += TOGGLE_ROW_H;

        // Speed slider
        renderModulesSlider(context, x, y, width, rowY, "menu.gui_settings.animations.speed",
                cfg.getAnimationSpeed(), 0.5F, 2.0F, "ANIM_SPEED", animSpeedDragging);
        rowY += SLIDER_ROW_H + 2.0F;

        // Scroll speed slider
        renderModulesSlider(context, x, y, width, rowY, "menu.gui_settings.animations.scroll_speed",
                cfg.getMenuScrollSpeed(), 0.2F, 3.0F, "SCROLL_SPEED", scrollSpeedDragging);
    }

    @Override
    public void handleClick(float x, float y, float width, float settY, double mouseX, double mouseY) {
        ClientAnimationConfig cfg = ClientAnimationConfig.getInstance();
        float rowY = settY;

        // Master toggle
        if (GuiUtility.isHovered(x, rowY, width, TOGGLE_ROW_H, mouseX, mouseY)) {
            cfg.setAnimationsEnabled(!cfg.isAnimationsEnabled());
            Sun.getInstance().getFileManager().writeFile("client");
            ClientSoundManager.getInstance().playButtonClick();
            return;
        }
        rowY += TOGGLE_ROW_H;

        // Sub toggles
        if (GuiUtility.isHovered(x, rowY, width, TOGGLE_ROW_H, mouseX, mouseY)) {
            cfg.setButtonHoverLift(!cfg.isButtonHoverLift());
            Sun.getInstance().getFileManager().writeFile("client");
            ClientSoundManager.getInstance().playButtonClick();
            return;
        }
        rowY += TOGGLE_ROW_H;
        if (GuiUtility.isHovered(x, rowY, width, TOGGLE_ROW_H, mouseX, mouseY)) {
            cfg.setSmoothTabs(!cfg.isSmoothTabs());
            Sun.getInstance().getFileManager().writeFile("client");
            ClientSoundManager.getInstance().playButtonClick();
            return;
        }
        rowY += TOGGLE_ROW_H;
        if (GuiUtility.isHovered(x, rowY, width, TOGGLE_ROW_H, mouseX, mouseY)) {
            cfg.setSmoothTooltip(!cfg.isSmoothTooltip());
            Sun.getInstance().getFileManager().writeFile("client");
            ClientSoundManager.getInstance().playButtonClick();
            return;
        }
        rowY += TOGGLE_ROW_H;
        if (GuiUtility.isHovered(x, rowY, width, TOGGLE_ROW_H, mouseX, mouseY)) {
            cfg.setStarEffect(!cfg.isStarEffect());
            Sun.getInstance().getFileManager().writeFile("client");
            ClientSoundManager.getInstance().playButtonClick();
            return;
        }
        rowY += TOGGLE_ROW_H;
        if (GuiUtility.isHovered(x, rowY, width, TOGGLE_ROW_H, mouseX, mouseY)) {
            cfg.setTogglePhysics(!cfg.isTogglePhysics());
            Sun.getInstance().getFileManager().writeFile("client");
            ClientSoundManager.getInstance().playButtonClick();
            return;
        }
        rowY += TOGGLE_ROW_H;

        // Speed slider
        if (GuiUtility.isHovered(x, rowY, width, SLIDER_ROW_H, mouseX, mouseY)) {
            animSpeedDragging = true;
            float sliderX = x + 9.0F;
            float sliderW = width - 18.0F;
            float val = GuiUtility.getSliderValue(0.5F, 2.0F, sliderX, sliderW, mouseX);
            cfg.setAnimationSpeed(val);
            Sun.getInstance().getFileManager().writeFile("client");
            ClientSoundManager.getInstance().playButtonClick();
            return;
        }
        rowY += SLIDER_ROW_H + 2.0F;

        // Scroll speed slider
        if (GuiUtility.isHovered(x, rowY, width, SLIDER_ROW_H, mouseX, mouseY)) {
            scrollSpeedDragging = true;
            float sliderX = x + 9.0F;
            float sliderW = width - 18.0F;
            float val = GuiUtility.getSliderValue(0.2F, 3.0F, sliderX, sliderW, mouseX);
            cfg.setMenuScrollSpeed(val);
            syncScrollSpeedToModule();
            Sun.getInstance().getFileManager().writeFile("client");
            ClientSoundManager.getInstance().playButtonClick();
        }
    }

    /** Синхронизирует значение скорости скролла из ClientAnimationConfig в MenuModule.scrollSpeed setting,
     *  чтобы autosave.sun хранил актуальное значение и не перезаписывал client.data при загрузке. */
    private static void syncScrollSpeedToModule() {
        try {
            MenuModule menuModule = Sun.getInstance().getModuleManager().getModule(MenuModule.class);
            SliderSetting setting = menuModule.getScrollSpeedSetting();
            if (setting != null) {
                float val = ClientAnimationConfig.getInstance().getMenuScrollSpeed();
                setting.setCurrentValueSilent(val);
            }
        } catch (Exception ignored) {}
    }

    @Override
    public void onMouseDragged(float x, float width, double mouseX, double mouseY) {
        if (animSpeedDragging) {
            float sliderX = x + 9.0F;
            float sliderW = width - 18.0F;
            float val = GuiUtility.getSliderValue(0.5F, 2.0F, sliderX, sliderW, mouseX);
            val = Math.round(val * 20.0F) / 20.0F; // step 0.05
            ClientAnimationConfig.getInstance().setAnimationSpeed(val);
        }
        if (scrollSpeedDragging) {
            float sliderX = x + 9.0F;
            float sliderW = width - 18.0F;
            float val = GuiUtility.getSliderValue(0.2F, 3.0F, sliderX, sliderW, mouseX);
            val = Math.round(val * 20.0F) / 20.0F; // step 0.05
            ClientAnimationConfig.getInstance().setMenuScrollSpeed(val);
        }
    }

    @Override
    public void onMouseReleased(double mouseX, double mouseY, MouseButton button) {
        if (animSpeedDragging || scrollSpeedDragging) {
            animSpeedDragging = false;
            scrollSpeedDragging = false;
            syncScrollSpeedToModule();
            Sun.getInstance().getFileManager().writeFile("client");
        }
    }

    @Override
    public boolean isDragging() {
        return animSpeedDragging || scrollSpeedDragging;
    }

    @Override
    public float getContentHeight() {
        float h = SETTINGS_SEP_Y;
        h += TOGGLE_ROW_H;           // master
        h += 5 * TOGGLE_ROW_H;       // 5 sub-toggles
        h += SLIDER_ROW_H + 2.0F;    // speed slider
        h += SLIDER_ROW_H + 2.0F;    // scroll speed slider
        h += CARD_BOTTOM_PAD;
        return h;
    }
}
