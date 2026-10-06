package naryn.sun.systems.modules.modules.visuals.world;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

/**
 * Утилита рендеринга проволочных фигур без заливки для модуля World.
 */
public final class WorldGeometry {

    private WorldGeometry() {}

    /**
     * Отрисовывает рёбра модели в буфер без заливки (DrawMode.DEBUG_LINES).
     *
     * @param ms      стек матриц с применёнными трансформациями позиции, поворота и масштаба
     * @param builder буфер линий VertexFormats.POSITION_COLOR
     * @param model   проволочная модель
     * @param r       красный цвет [0..1]
     * @param g       зелёный цвет [0..1]
     * @param b       синий цвет [0..1]
     * @param a       прозрачность [0..1]
     */
    public static void renderModel(MatrixStack ms, BufferBuilder builder, WorldModel model, float r, float g, float b, float a) {
        if (model == null) return;
        float[] edges = model.getEdges();
        if (edges == null || edges.length == 0) return;

        Matrix4f matrix = ms.peek().getPositionMatrix();
        for (int i = 0; i < edges.length; i += 6) {
            builder.vertex(matrix, edges[i], edges[i + 1], edges[i + 2]).color(r, g, b, a);
            builder.vertex(matrix, edges[i + 3], edges[i + 4], edges[i + 5]).color(r, g, b, a);
        }
    }
}
