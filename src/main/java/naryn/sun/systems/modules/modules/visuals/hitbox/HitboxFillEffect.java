package naryn.sun.systems.modules.modules.visuals.hitbox;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;

/**
 * Общий интерфейс для анимированного стиля заливки хитбокса (Космос, Аква и т.д.).
 * Каждая реализация сама решает, как красить грани бокса в зависимости от времени.
 * Новый режим = новый класс в hitbox/effects/, без правок остальных эффектов.
 */
public interface HitboxFillEffect {

    /**
     * Строит геометрию залитого бокса в буфер.
     *
     * @param box     бокс уже в camera-relative координатах, готов к передаче в vertex()
     * @param opacity множитель альфы 0..1 (из настройки Fill Opacity)
     * @param now     System.currentTimeMillis() на момент рендера, для анимации
     */
    void renderFill(BufferBuilder builder, MatrixStack matrices, Box box, float opacity, long now);
}