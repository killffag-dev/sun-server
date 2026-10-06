package naryn.sun.systems.modules.modules.visuals.killeffects;

import naryn.sun.utility.colors.ColorRGBA;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;

/**
 * Общий интерфейс для любого визуального эффекта смерти (лист, пепел, ...).
 * Каждая реализация сама хранит свои частицы и управляет их жизненным циклом.
 */
public interface KillEffectModel {

    /** Вызывается при смерти сущности — эффект сам решает, как заспавнить частицы внутри хитбокса. */
    void onDeath(Box box, ColorRGBA baseColor, int particleCount);

    /** Обновление состояния эффекта каждый тик (очистка истёкших частиц, посадка на землю и т.д.). */
    void tick(long now);

    /** Построение геометрии всех активных частиц в буфер. Вызывается только если !isEmpty(). */
    void render(BufferBuilder builder, MatrixStack matrices, long now);

    boolean isEmpty();

    /** Макс. время жизни одной частицы — модуль использует это, чтобы держать сущность "растворяющейся". */
    long getLifetimeMs();
}