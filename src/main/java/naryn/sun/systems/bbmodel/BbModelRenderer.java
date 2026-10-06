package naryn.sun.systems.bbmodel;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Collections;
import java.util.Map;

/**
 * Рисует {@link BbModel} напрямую через Tesselator — тот же подход, что уже
 * используется в проекте (см. ChinaHatFeatureRenderer, SkyEntityGeometry),
 * а не через батчинг vertexConsumers/RenderLayer. Это значит:
 *  - нет ванильного освещения мира (light-параметр не используется) — модель
 *    рисуется с фиксированным тинтом (r,g,b,a), который ты передаёшь сам;
 *  - вызывающий код должен сам вызвать RenderSystem.setShaderTexture/setShader
 *    ДО первого рендера модели за кадр — этот класс это делает сам внутри
 *    render(...), поэтому отдельно вызывать не нужно;
 *  - культинг граней отключается на время рисования (как и в остальных
 *    визуалах проекта), чтобы не думать о точном порядке вершин.
 *
 * UV-маппинг граней куба — приблизительный "стандартный" вариант (как у
 * ванильных боксов). Если на конкретной грани текстура выглядит повёрнутой
 * или отражённой — поправь порядок точек в {@link #FACE_UV_ORDER} эквиваленте
 * ниже, в methodе {@link #buildCube}.
 */
public final class BbModelRenderer {

    private BbModelRenderer() {
    }

    /** Рендер без анимации — все кости в базовой (статической, из самого Blockbench) позе. */
    public static void render(BbModel model, MatrixStack matrices, float r, float g, float b, float a) {
        render(model, matrices, Collections.emptyMap(), r, g, b, a);
    }

    /** Рендер с анимацией: pose — результат {@link BbAnimation#sampleAll(float)}. */
    public static void render(BbModel model, MatrixStack matrices, Map<String, BbBoneAnimator.Pose> pose,
                               float r, float g, float b, float a) {
        if (model.textureId == null || model.rootBones.isEmpty()) return;

        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.setShaderTexture(0, model.textureId);
        // Если у тебя в проекте другое имя ключа для POSITION_TEX_COLOR шейдера —
        // поменяй эту строку на актуальную (см. ShaderProgramKeys в твоей версии Minecraft).
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);

        BufferBuilder buf = RenderSystem.renderThreadTesselator()
            .begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_TEXTURE_COLOR);

        for (BbBone root : model.rootBones) {
            renderBone(root, matrices, pose, buf, model.textureWidth, model.textureHeight, r, g, b, a);
        }

        BuiltBuffer built = buf.endNullable();
        if (built != null) {
            BufferRenderer.drawWithGlobalProgram(built);
        }

        RenderSystem.enableCull();
    }

    private static void renderBone(BbBone bone, MatrixStack matrices, Map<String, BbBoneAnimator.Pose> pose,
                                    BufferBuilder buf, int texW, int texH, float r, float g, float b, float a) {
        BbBoneAnimator.Pose p = pose.getOrDefault(bone.name, BbBoneAnimator.Pose.IDENTITY);

        matrices.push();
        matrices.translate(bone.pivot.x / 16.0, bone.pivot.y / 16.0, bone.pivot.z / 16.0);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(bone.baseRotation.z + p.rotationOffset.z));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(bone.baseRotation.y + p.rotationOffset.y));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(bone.baseRotation.x + p.rotationOffset.x));
        if (p.scale.x != 1F || p.scale.y != 1F || p.scale.z != 1F) {
            matrices.scale(p.scale.x, p.scale.y, p.scale.z);
        }
        matrices.translate(-bone.pivot.x / 16.0, -bone.pivot.y / 16.0, -bone.pivot.z / 16.0);
        matrices.translate(p.positionOffset.x / 16.0, p.positionOffset.y / 16.0, p.positionOffset.z / 16.0);

        for (BbCube cube : bone.cubes) {
            buildCube(cube, matrices, buf, texW, texH, r, g, b, a);
        }
        for (BbBone child : bone.children) {
            renderBone(child, matrices, pose, buf, texW, texH, r, g, b, a);
        }

        matrices.pop();
    }

    private static void buildCube(BbCube cube, MatrixStack matrices, BufferBuilder buf, int texW, int texH,
                                   float r, float g, float b, float a) {
        boolean localRotation = cube.rotation != null && cube.origin != null
            && (cube.rotation.x != 0F || cube.rotation.y != 0F || cube.rotation.z != 0F);

        matrices.push();
        if (localRotation) {
            matrices.translate(cube.origin.x / 16.0, cube.origin.y / 16.0, cube.origin.z / 16.0);
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(cube.rotation.z));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(cube.rotation.y));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(cube.rotation.x));
            matrices.translate(-cube.origin.x / 16.0, -cube.origin.y / 16.0, -cube.origin.z / 16.0);
        }

        Matrix4f m = matrices.peek().getPositionMatrix();

        float x1 = cube.from.x / 16F, y1 = cube.from.y / 16F, z1 = cube.from.z / 16F;
        float x2 = cube.to.x / 16F, y2 = cube.to.y / 16F, z2 = cube.to.z / 16F;

        float[] north = cube.faceUv.get(BbCube.Face.NORTH);
        if (north != null) {
            quad(buf, m,
                v(x2, y2, z1), v(x1, y2, z1), v(x1, y1, z1), v(x2, y1, z1),
                north, texW, texH, r, g, b, a);
        }
        float[] south = cube.faceUv.get(BbCube.Face.SOUTH);
        if (south != null) {
            quad(buf, m,
                v(x1, y2, z2), v(x2, y2, z2), v(x2, y1, z2), v(x1, y1, z2),
                south, texW, texH, r, g, b, a);
        }
        float[] east = cube.faceUv.get(BbCube.Face.EAST);
        if (east != null) {
            quad(buf, m,
                v(x2, y2, z1), v(x2, y2, z2), v(x2, y1, z2), v(x2, y1, z1),
                east, texW, texH, r, g, b, a);
        }
        float[] west = cube.faceUv.get(BbCube.Face.WEST);
        if (west != null) {
            quad(buf, m,
                v(x1, y2, z2), v(x1, y2, z1), v(x1, y1, z1), v(x1, y1, z2),
                west, texW, texH, r, g, b, a);
        }
        float[] up = cube.faceUv.get(BbCube.Face.UP);
        if (up != null) {
            quad(buf, m,
                v(x1, y2, z1), v(x2, y2, z1), v(x2, y2, z2), v(x1, y2, z2),
                up, texW, texH, r, g, b, a);
        }
        float[] down = cube.faceUv.get(BbCube.Face.DOWN);
        if (down != null) {
            quad(buf, m,
                v(x1, y1, z2), v(x2, y1, z2), v(x2, y1, z1), v(x1, y1, z1),
                down, texW, texH, r, g, b, a);
        }

        matrices.pop();
    }

    private static Vector3f v(float x, float y, float z) {
        return new Vector3f(x, y, z);
    }

    /** uv = {u1, v1, u2, v2} в пикселях текстуры, соответствуют p1..p4 по кругу (как обычный quad). */
    private static void quad(BufferBuilder buf, Matrix4f m, Vector3f p1, Vector3f p2, Vector3f p3, Vector3f p4,
                              float[] uv, int texW, int texH, float r, float g, float b, float a) {
        float u1 = uv[0] / texW, v1 = uv[1] / texH, u2 = uv[2] / texW, v2 = uv[3] / texH;
        vertex(buf, m, p1, u1, v1, r, g, b, a);
        vertex(buf, m, p2, u2, v1, r, g, b, a);
        vertex(buf, m, p3, u2, v2, r, g, b, a);

        vertex(buf, m, p1, u1, v1, r, g, b, a);
        vertex(buf, m, p3, u2, v2, r, g, b, a);
        vertex(buf, m, p4, u1, v2, r, g, b, a);
    }

    private static void vertex(BufferBuilder buf, Matrix4f m, Vector3f p, float u, float v,
                                float r, float g, float b, float a) {
        buf.vertex(m, p.x, p.y, p.z).texture(u, v).color(r, g, b, a);
    }
}
