package naryn.sun.systems.modules.modules.utility;

import naryn.sun.systems.event.EventListener;
import naryn.sun.systems.event.impl.player.ClientPlayerTickEvent;
import naryn.sun.systems.event.impl.window.MouseScrollEvent;
import naryn.sun.systems.modules.api.ModuleCategory;
import naryn.sun.systems.modules.api.ModuleInfo;
import naryn.sun.systems.modules.impl.BaseModule;
import naryn.sun.systems.setting.settings.SliderSetting;
import org.lwjgl.glfw.GLFW;

@ModuleInfo(name = "Zoom", category = ModuleCategory.UTILITY, key = GLFW.GLFW_KEY_C, enabledByDefault = false, holdToActivate = true)
public class Zoom extends BaseModule {

    // Диапазон реального увеличения: 0% -> x1 (без зума), 100% -> MAX_ZOOM
    private static final double MIN_ZOOM = 1.0;
    private static final double MAX_ZOOM = 20.0;

    // Сколько процентов зума меняется за одно деление колеса
    private static final float SCROLL_SENSITIVITY = 2.0f;

    // Процент зума 0-100, сохраняется в конфиг автоматически как обычная настройка
    private final SliderSetting zoomPercent = new SliderSetting(this, "zoom_percent")
            .min(0f)
            .max(100f)
            .step(1f)
            .currentValue(20f)
            .suffix("%");

    private double currentFovMult = 1.0;

    private final EventListener<ClientPlayerTickEvent> onTick = event -> {
        // Без плавного лерпа - сразу нужное значение, без анимации
        this.currentFovMult = 1.0 / getZoomLevel();
    };

    private final EventListener<MouseScrollEvent> onScroll = event -> {
        if (!isEnabled()) return;

        float delta = (float) (event.getVerticalAmount() * SCROLL_SENSITIVITY);
        zoomPercent.setCurrentValue(zoomPercent.getCurrentValue() + delta);
    };

    @Override
    public void onEnable() {
        // Сразу выставляем целевой фов при активации, без анимации нарастания
        this.currentFovMult = 1.0 / getZoomLevel();
    }

    @Override
    public void onDisable() {
        this.currentFovMult = 1.0;
    }

    public double getCurrentFovMult() {
        return currentFovMult;
    }

    private double getZoomLevel() {
        double percent = zoomPercent.getCurrentValue() / 100.0;
        return MIN_ZOOM + percent * (MAX_ZOOM - MIN_ZOOM);
    }
}