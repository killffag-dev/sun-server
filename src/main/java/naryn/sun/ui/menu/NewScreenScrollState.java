package naryn.sun.ui.menu;

/**
 * Состояние скролла для NewScreen.
 * Инкапсулирует offset/target/dragging как для основного контента,
 * так и для панели GUI Settings.
 */
public class NewScreenScrollState {

    // Основной скролл
    float scrollOffset = 0.0F;
    float scrollTarget = 0.0F;
    boolean scrollbarDragging = false;
    float scrollbarDragOffset = 0.0F;

    // Скролл GUI Settings
    float guiSettingsScrollOffset = 0.0F;
    float guiSettingsScrollTarget = 0.0F;
    boolean guiSettingsScrollDragging = false;
    float guiSettingsScrollDragOffset = 0.0F;

    /** Обновляет плавную анимацию скролла (вызывается каждый кадр). */
    public void smoothUpdate() {
        scrollOffset += (scrollTarget - scrollOffset) * 0.15F;
    }

    /** Сбрасывает основной скролл в начало. */
    public void resetScroll() {
        scrollOffset = 0.0F;
        scrollTarget = 0.0F;
    }

    /** Сбрасывает все состояния перетаскивания. */
    public void resetDragging() {
        scrollbarDragging = false;
        guiSettingsScrollDragging = false;
    }

    /** Прокрутка основного контента. */
    public void scroll(float delta, float maxScroll) {
        float speed = naryn.sun.systems.animation.ClientAnimationConfig.getInstance().getMenuScrollSpeed();
        scrollTarget -= delta * 12.0F * speed;
        clampScroll(maxScroll);
    }

    /** Прокрутка GUI Settings. */
    public void scrollGuiSettings(float delta, float maxScroll) {
        float speed = naryn.sun.systems.animation.ClientAnimationConfig.getInstance().getMenuScrollSpeed();
        guiSettingsScrollTarget -= delta * 16.0F * speed;
        clampGuiSettingsScroll(maxScroll);
    }

    /** Обновляет плавную анимацию скролла GUI Settings. */
    public void smoothUpdateGuiSettings() {
        guiSettingsScrollOffset += (guiSettingsScrollTarget - guiSettingsScrollOffset) * 0.3F;
    }

    public void clampScroll(float maxScroll) {
        if (scrollTarget > maxScroll) scrollTarget = maxScroll;
        if (scrollTarget < 0) scrollTarget = 0;
    }

    public void clampGuiSettingsScroll(float maxScroll) {
        if (guiSettingsScrollTarget > maxScroll) guiSettingsScrollTarget = maxScroll;
        if (guiSettingsScrollTarget < 0.0F) guiSettingsScrollTarget = 0.0F;
    }
}
