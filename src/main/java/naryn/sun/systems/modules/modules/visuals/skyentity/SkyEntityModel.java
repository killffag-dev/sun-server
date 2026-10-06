package naryn.sun.systems.modules.modules.visuals.skyentity;

import net.minecraft.client.render.BufferBuilder;
import org.joml.Matrix4f;

/**
 * Общий интерфейс для любой "небесной сущности" (дракон, НЛО, дирижабль).
 * Каждая реализация строит свою геометрию прямо в буфер вершин.
 *
 * @param animTime  время анимации в секундах (для взмахов, покачивания и т.д.)
 * @param wingPhase фаза взмаха крыльев (уже посчитанная синусоида, чтобы не дублировать расчёт в каждой модели)
 */
public interface SkyEntityModel {
    void build(BufferBuilder buffer, Matrix4f matrix, float animTime, float wingPhase);
}