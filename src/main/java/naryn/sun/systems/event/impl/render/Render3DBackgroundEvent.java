package naryn.sun.systems.event.impl.render;

import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

/**
 * Отдельное от Render3DEvent событие для фоновых full-screen эффектов (сейчас — только
 * Sky), которые должны рисоваться ДО любых сущностей за кадр, а не после всего мира
 * (как Render3DEvent на WorldRenderEvents.END).
 *
 * Триггерится на WorldRenderEvents.BEFORE_ENTITIES — то есть после того как
 * непрозрачный террейн уже выведен в framebuffer, но до отрисовки любых сущностей
 * (ников, HeadCosmetics, мобов и т.д.). Это делает "depth == 1.0" по-настоящему
 * надёжным признаком "здесь ничего нет, это небо": в этот момент кадра depth==1.0
 * не может означать ничего кроме реально пустого места — в отличие от END, где так
 * же выглядела бы translucent-геометрия без записи глубины (см.
 * references/entity-overlay-rendering.md, п.6 и п.7 в sun-client skill).
 *
 * EventManager.triggerEvent маршрутизирует по точному классу события
 * (event.getClass()), поэтому наследование от Render3DEvent здесь безопасно:
 * слушатели, подписанные на EventListener<Render3DEvent>, никогда не получат
 * Render3DBackgroundEvent и наоборот — это два разных ключа в listenerMap,
 * несмотря на общий родительский класс (нужен только чтобы переиспользовать поля
 * и геттеры, а не дублировать их).
 */
public class Render3DBackgroundEvent extends Render3DEvent {

   public Render3DBackgroundEvent(MatrixStack matrices, Matrix4f positionMatrix, Matrix4f projectionMatrix, Camera camera, float tickDelta) {
      super(matrices, positionMatrix, projectionMatrix, camera, tickDelta);
   }
}