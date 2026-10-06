package naryn.sun.systems.modules.modules.visuals;

import com.mojang.blaze3d.systems.RenderSystem;
import naryn.sun.systems.bbmodel.BbAnimation;
import naryn.sun.systems.bbmodel.BbBoneAnimator;
import naryn.sun.systems.bbmodel.BbModel;
import naryn.sun.systems.bbmodel.BbModelLoader;
import naryn.sun.systems.bbmodel.BbModelRenderer;
import naryn.sun.systems.event.EventListener;
import naryn.sun.systems.event.impl.render.Render3DEvent;
import naryn.sun.systems.event.impl.window.KeyPressEvent;
import naryn.sun.systems.modules.api.ModuleCategory;
import naryn.sun.systems.modules.api.ModuleInfo;
import naryn.sun.systems.modules.impl.BaseModule;
import naryn.sun.systems.modules.modules.visuals.skyentity.SkyEntityGeometry;
import naryn.sun.systems.modules.modules.visuals.skyentity.SkyEntityModel;
import naryn.sun.systems.modules.modules.visuals.skyentity.models.BlimpModel;
import naryn.sun.systems.modules.modules.visuals.skyentity.models.DragonModel;
import naryn.sun.systems.modules.modules.visuals.skyentity.models.FakeCowModel;
import naryn.sun.systems.modules.modules.visuals.skyentity.models.UfoModel;
import naryn.sun.systems.setting.settings.ModeSetting;
import naryn.sun.systems.setting.settings.SliderSetting;
import naryn.sun.utility.render.RenderUtility;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

@ModuleInfo(name = "Sky Entity", category = ModuleCategory.VISUALS, desc = "Летающие существа в небе: дракон, НЛО, дирижабль, кит")
public class SkyEntityModule extends BaseModule {

    // --- Выбор существа ---
    private final ModeSetting entityType = new ModeSetting(this, "modules.settings.sky_entity.type");
    private final ModeSetting.Value dragonMode = new ModeSetting.Value(entityType, "modules.settings.sky_entity.type.dragon").select();
    private final ModeSetting.Value ufoMode = new ModeSetting.Value(entityType, "modules.settings.sky_entity.type.ufo");
    private final ModeSetting.Value blimpMode = new ModeSetting.Value(entityType, "modules.settings.sky_entity.type.blimp");
    private final ModeSetting.Value kiteMode = new ModeSetting.Value(entityType, "modules.settings.sky_entity.type.kite");

    // --- Настройки поведения ---
    private final SliderSetting count = new SliderSetting(this, "modules.settings.sky_entity.count")
        .min(1F).max(3F).step(1F).currentValue(1F);
    private final SliderSetting speed = new SliderSetting(this, "modules.settings.sky_entity.speed")
        .min(0.2F).max(3.0F).step(0.1F).currentValue(1.0F);
    private final SliderSetting size = new SliderSetting(this, "modules.settings.sky_entity.size")
        .min(0.5F).max(3.0F).step(0.1F).currentValue(1.0F);
    private final SliderSetting height = new SliderSetting(this, "modules.settings.sky_entity.height")
        .min(60F).max(160F).step(1F).currentValue(110F);

    // --- Модели (процедурные, общий POSITION_COLOR-конвейер) ---
    private final DragonModel dragonModel = new DragonModel();
    private final UfoModel ufoModel = new UfoModel();
    private final BlimpModel blimpModel = new BlimpModel();
    private final FakeCowModel cowModel = new FakeCowModel();

    // --- Кит (.bbmodel, текстурированный, отдельный конвейер BbModelRenderer) ---
    private static final Identifier KITE_RESOURCE = Identifier.of("sun", "bbmodels/whale.bbmodel");
    // Впиши сюда имя анимации ровно как в Blockbench (вкладка Animations), если она есть.
    // Если анимации нет/не нужна — оставь null, кит будет рисоваться в статичной позе.
    private static final String KITE_ANIMATION_NAME = "animation.unknown.kivok";
	private static final float KITE_SIZE_MULTIPLIER = 10F; // подбери под свою модель
    private static final float KITE_YAW_OFFSET_DEGREES = -90F; // 0/90/-90/180 — пока не встанет носом по ходу
    private BbModel kiteModel;
    private boolean kiteLoadFailed = false;

    @Override
    public void onEnable() {
        kiteLoadFailed = false;
        moduleStartTime = -1L;
    }

    // --- Похищение коровы (только режим НЛО) ---
    // Похищает всегда орбитальный НЛО с индексом 0 (первый/основной).
    private static final int ABDUCT_TARGET_INDEX = 0;
    private static final long ABDUCTION_AUTO_INTERVAL_MS = 30 * 60 * 1000L; // раз в 30 минут
    private static final float COW_DROP_DISTANCE = 15F; // блоков ниже НЛО

    private static final long PHASE_BRAKE_MS = 1200L;      // НЛО тормозит и плавно останавливается
    private static final long PHASE_PRE_DELAY_MS = 300L;   // короткая пауза после остановки
    private static final long PHASE_FADE_IN_MS = 1500L;    // корова проявляется из ниоткуда
    private static final long PHASE_HOLD_MS = 1000L;       // висит внизу, полностью видна
    private static final long PHASE_LIFT_MS = 3000L;       // луч тянет её вверх к НЛО
    private static final long PHASE_FADE_OUT_MS = 500L;    // скрывается, "втянута" в корабль
    private static final long PHASE_DEPART_HOLD_MS = 300L; // короткая пауза перед разгоном
    private static final long PHASE_ACCEL_MS = 1200L;      // НЛО плавно разгоняется обратно на орбиту

    private static final long B0 = PHASE_BRAKE_MS;
    private static final long B1 = B0 + PHASE_PRE_DELAY_MS;
    private static final long B2 = B1 + PHASE_FADE_IN_MS;
    private static final long B3 = B2 + PHASE_HOLD_MS;
    private static final long B4 = B3 + PHASE_LIFT_MS;
    private static final long B5 = B4 + PHASE_FADE_OUT_MS;
    private static final long B6 = B5 + PHASE_DEPART_HOLD_MS;
    private static final long ABDUCTION_TOTAL_MS = B6 + PHASE_ACCEL_MS;

    private long moduleStartTime = -1L;
    private long nextAutoAbductionTime = -1L;

    private boolean abductionActive = false;
    private long abductionStartTime = 0L;
    // Целевая (конечная) поза похищающего НЛО, к которой он плавно тормозит в фазе BRAKE —
    // предвычислена на момент триггера как "где он будет через PHASE_BRAKE_MS, если продолжит лететь
    // как обычно". Камера-относительные координаты, та же система, что использует renderOne().
    private OrbitPose frozenPose = null;

    // --- Равномерная по длине дуги параметризация орбиты (лемниската Жероно) ---
    // Раньше ang рос линейно по времени, но у lx=sin(ang)*R, lz=sin(ang)*cos(ang)*R реальная
    // скорость вдоль кривой (не угловая) меняется почти в 2 раза за один цикл: максимальна
    // при прохождении "центра" восьмёрки и падает на боковых петлях. Из-за этого на поворотах
    // сущность визуально "довращивается" почти на месте — рывок сильнее всего заметен у кита,
    // потому что он крупный и анимация плавная, любой скачок скорости бросается в глаза.
    // Таблица ниже даёт компенсацию: по равномерному прогрессу времени возвращает такой ang,
    // что физическая скорость вдоль пути остаётся примерно постоянной.
    private static final int ORBIT_LUT_SIZE = 720;
    private static final double[] ORBIT_LUT_ANG = new double[ORBIT_LUT_SIZE + 1];

    static {
        int samples = 4096;
        double[] sampleAng = new double[samples + 1];
        double[] sampleLen = new double[samples + 1];
        double len = 0.0;
        double prevSpeed = orbitCurveSpeed(0.0);
        sampleAng[0] = 0.0;
        sampleLen[0] = 0.0;
        for (int i = 1; i <= samples; i++) {
            double ang = (2.0 * Math.PI) * i / samples;
            double curSpeed = orbitCurveSpeed(ang);
            len += 0.5 * (prevSpeed + curSpeed) * (2.0 * Math.PI / samples);
            sampleAng[i] = ang;
            sampleLen[i] = len;
            prevSpeed = curSpeed;
        }
        double totalLen = sampleLen[samples];

        int j = 0;
        for (int k = 0; k <= ORBIT_LUT_SIZE; k++) {
            double target = totalLen * k / ORBIT_LUT_SIZE;
            while (j < samples && sampleLen[j + 1] < target) j++;
            double segLen = sampleLen[j + 1] - sampleLen[j];
            double f = segLen > 1.0e-9 ? (target - sampleLen[j]) / segLen : 0.0;
            ORBIT_LUT_ANG[k] = sampleAng[j] + (sampleAng[j + 1] - sampleAng[j]) * f;
        }
    }

    /** "Скорость" лемнискаты Жероно по параметру ang при R=1 — форма одна и та же для любого R. */
    private static double orbitCurveSpeed(double ang) {
        double dx = Math.cos(ang);
        double dz = Math.cos(2.0 * ang);
        return Math.sqrt(dx * dx + dz * dz);
    }

    /** Равномерный по времени прогресс [0,1) внутри одного цикла -> ang с постоянной скоростью вдоль кривой. */
    private static double orbitAngForProgress(double progress01) {
        double f = progress01 * ORBIT_LUT_SIZE;
        int i0 = (int) Math.floor(f);
        if (i0 >= ORBIT_LUT_SIZE) i0 = ORBIT_LUT_SIZE - 1;
        int i1 = i0 + 1;
        double frac = f - i0;
        return ORBIT_LUT_ANG[i0] + (ORBIT_LUT_ANG[i1] - ORBIT_LUT_ANG[i0]) * frac;
    }

    private final EventListener<Render3DEvent> onRender3D = event -> {
        MatrixStack matrices = event.getMatrices();
        long now = System.currentTimeMillis();

        if (moduleStartTime < 0L) {
            moduleStartTime = now;
            nextAutoAbductionTime = moduleStartTime + ABDUCTION_AUTO_INTERVAL_MS;
        }

        int n = Math.round(count.getCurrentValue());
        n = Math.max(1, Math.min(3, n));

        if (entityType.is(kiteMode)) {
            // Кит — свой рендер-путь (текстурированный, шейдер сам выставляется внутри
            // BbModelRenderer), не завязан на общий POSITION_COLOR-буфер процедурных моделей
            // и не участвует в похищении коровы.
            renderKites(matrices, now, n);
            return;
        }

        SkyEntityModel model = getCurrentModel();
        if (model == null) return;

        if (!abductionActive && entityType.is(ufoMode) && now >= nextAutoAbductionTime) {
            triggerAbduction(now);
            nextAutoAbductionTime += ABDUCTION_AUTO_INTERVAL_MS;
        }

        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        boolean abducting = abductionActive && entityType.is(ufoMode);
        for (int i = 0; i < n; i++) {
            if (abducting && i == ABDUCT_TARGET_INDEX) continue; // рендерится отдельно ниже
            renderOne(matrices, model, now, i);
        }

        if (abducting) {
            renderAbduction(matrices, now);
        }

        RenderSystem.enableCull();
    };

    // Shift+L — ручной тестовый триггер похищения (не влияет на 30-минутный автотаймер)
    private final EventListener<KeyPressEvent> onKeyPress = event -> {
        if (event.getAction() != GLFW.GLFW_PRESS) return;
        if (event.getKey() != GLFW.GLFW_KEY_L) return;
        if (!Screen.hasShiftDown()) return;
        if (!entityType.is(ufoMode)) return;
        triggerAbduction(System.currentTimeMillis());
    };

    private SkyEntityModel getCurrentModel() {
        if (entityType.is(dragonMode)) return dragonModel;
        if (entityType.is(ufoMode)) return ufoModel;
        if (entityType.is(blimpMode)) return blimpModel;
        return null;
    }

    private void renderKites(MatrixStack matrices, long now, int n) {
        if (kiteLoadFailed) return;
        if (kiteModel == null) {
            try {
                kiteModel = BbModelLoader.loadFromResource(KITE_RESOURCE);
            } catch (Exception e) {
                kiteLoadFailed = true;
                return;
            }
        }

        float t = (now - moduleStartTime) / 1000F;

        for (int i = 0; i < n; i++) {
            OrbitPose pose = computeOrbitPose(now, i);
            float s = size.getCurrentValue() * KITE_SIZE_MULTIPLIER * (1F - i * 0.12F);

            matrices.push();
            matrices.translate(pose.lx, pose.ly, pose.lz);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotation(-pose.yaw));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(KITE_YAW_OFFSET_DEGREES));
            matrices.multiply(RotationAxis.POSITIVE_X.rotation(pose.roll));
            matrices.scale(s, s, s);

            Map<String, BbBoneAnimator.Pose> animPose = Collections.emptyMap();
            if (KITE_ANIMATION_NAME != null) {
                BbAnimation anim = kiteModel.animation(KITE_ANIMATION_NAME);
                if (anim != null) {
                    animPose = anim.sampleAll(t);
                }
            }

            BbModelRenderer.render(kiteModel, matrices, animPose, 1.0F, 1.0F, 1.0F, 1.0F);
            matrices.pop();
        }
    }

    private void triggerAbduction(long now) {
        if (abductionActive) return;
        // Целевая поза — где НЛО окажется через PHASE_BRAKE_MS, если бы продолжил лететь как обычно.
        // Так в фазе торможения он плавно "доезжает" именно до этой точки, а не дёргается на месте.
        frozenPose = computeOrbitPose(now + PHASE_BRAKE_MS, ABDUCT_TARGET_INDEX);
        abductionActive = true;
        abductionStartTime = now;
    }

    /** Камера-относительная орбитальная позиция НЛО с индексом idx в момент времени now (без bob). */
    private OrbitPose computeOrbitPose(long now, int idx) {
        double spd = speed.getCurrentValue();
        double t = now * 0.001 * spd;

        double raw = t * 0.11 + idx * 2.4;
        double progress = raw / (2.0 * Math.PI);
        progress -= Math.floor(progress); // привести к [0,1)
        double ang = orbitAngForProgress(progress);

        double R = 150.0 + idx * 28.0;
        double lx = Math.sin(ang) * R;
        double lz = Math.sin(ang) * Math.cos(ang) * R;
        double ly = height.getCurrentValue() + idx * 14.0 + Math.sin(t * 0.23 + idx) * 9.0;

        double yaw = Math.atan2(Math.cos(2.0 * ang), Math.cos(ang));

        // Шаг для конечно-разностной оценки roll берём в исходном "равномерном" времени (raw),
        // как и раньше (0.03), а не в скомпенсированном ang — так масштаб банкинга остаётся
        // прежним, привычным по ощущениям для дракона/НЛО/дирижабля.
        double raw2 = raw + 0.03;
        double progress2 = raw2 / (2.0 * Math.PI);
        progress2 -= Math.floor(progress2);
        double ang2 = orbitAngForProgress(progress2);
        double dYaw = Math.atan2(Math.cos(2.0 * ang2), Math.cos(ang2)) - yaw;
        while (dYaw > Math.PI) dYaw -= 2 * Math.PI;
        while (dYaw < -Math.PI) dYaw += 2 * Math.PI;
        float roll = (float) Math.max(-0.5, Math.min(0.5, dYaw * 4.0));

        return new OrbitPose(lx, ly, lz, (float) yaw, roll);
    }

    private void renderOne(MatrixStack matrices, SkyEntityModel model, long now, int idx) {
        OrbitPose pose = computeOrbitPose(now, idx);

        float animSpeed = 1.2F;
        long animPeriodMs = 1000000L;
        double animT = (now % animPeriodMs) * 0.001 * animSpeed;
        float at = (float) animT;
        float phase = (float) (at * Math.PI * 2.0 * 0.85 + idx * 1.7);
        float bob = (float) Math.sin(phase - 0.6) * 0.35F;

        matrices.push();
        matrices.translate(pose.lx, pose.ly + bob, pose.lz);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotation(-pose.yaw));
        matrices.multiply(RotationAxis.POSITIVE_X.rotation(pose.roll));
        float s = size.getCurrentValue() * (1F - idx * 0.12F);
        matrices.scale(s, s, s);
        Matrix4f m = matrices.peek().getPositionMatrix();

        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        model.build(buf, m, at, phase);
        RenderUtility.buildBuffer(buf);

        matrices.pop();
    }

    private void renderAbduction(MatrixStack matrices, long now) {
        long elapsed = now - abductionStartTime;
        if (elapsed >= ABDUCTION_TOTAL_MS || frozenPose == null) {
            abductionActive = false;
            frozenPose = null;
            return;
        }

        float s = size.getCurrentValue();

        // Время анимации по модулю периода — как в renderOne(), иначе на больших значениях now
        // float теряет точность и геометрия начинает мерцать/схлопываться между кадрами.
        long animPeriodMs = 1000000L;
        float frozenAt = (float) ((now % animPeriodMs) * 0.001);

        // --- Поза НЛО: торможение (B0) / стоит (B0..B6) / разгон (B6..конец) ---
        OrbitPose shipPose;
        if (elapsed < B0) {
            OrbitPose live = computeOrbitPose(abductionStartTime + elapsed, ABDUCT_TARGET_INDEX);
            float k = easeIn(elapsed / (float) B0);
            shipPose = blendPose(live, frozenPose, k);
        } else if (elapsed < B6) {
            shipPose = frozenPose;
        } else {
            OrbitPose live = computeOrbitPose(now, ABDUCT_TARGET_INDEX);
            float k = easeOut((elapsed - B6) / (float) PHASE_ACCEL_MS);
            shipPose = blendPose(frozenPose, live, k);
        }

        matrices.push();
        matrices.translate(shipPose.lx, shipPose.ly, shipPose.lz);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotation(-shipPose.yaw));
        matrices.multiply(RotationAxis.POSITIVE_X.rotation(shipPose.roll));
        matrices.scale(s, s, s);
        Matrix4f ufoMatrix = matrices.peek().getPositionMatrix();
        BufferBuilder ufoBuf = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        ufoModel.build(ufoBuf, ufoMatrix, frozenAt, 0F);
        RenderUtility.buildBuffer(ufoBuf);
        matrices.pop();

        // --- Фазы коровы (существует только пока корабль неподвижен, B0..B6) ---
        boolean cowVisible;
        boolean beamVisible = false;
        float cowY;
        float alpha;

        float belowY = (float) frozenPose.ly - COW_DROP_DISTANCE * s;
        float liftedY = (float) frozenPose.ly - 1.4F * s;

        if (elapsed < B1) {
            cowVisible = false;
            cowY = belowY;
            alpha = 0F;
        } else if (elapsed < B2) {
            float k = (elapsed - B1) / (float) PHASE_FADE_IN_MS;
            cowVisible = true;
            cowY = belowY;
            alpha = easeOut(k);
        } else if (elapsed < B3) {
            cowVisible = true;
            cowY = belowY;
            alpha = 1F;
        } else if (elapsed < B4) {
            float k = (elapsed - B3) / (float) PHASE_LIFT_MS;
            cowVisible = true;
            beamVisible = true;
            cowY = lerp(belowY, liftedY, easeInOut(k));
            alpha = 1F;
        } else if (elapsed < B5) {
            float k = (elapsed - B4) / (float) PHASE_FADE_OUT_MS;
            cowVisible = true;
            beamVisible = true;
            cowY = liftedY;
            alpha = 1F - k;
        } else {
            cowVisible = false;
            cowY = liftedY;
            alpha = 0F;
        }

        if (cowVisible && alpha > 0.01F) {
            boolean translucent = alpha < 0.999F;
            if (translucent) {
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.depthMask(false);
            }

            matrices.push();
            matrices.translate(frozenPose.lx, cowY, frozenPose.lz);
            matrices.scale(s, s, s);
            Matrix4f cowMatrix = matrices.peek().getPositionMatrix();

            SkyEntityGeometry.setAlpha(alpha);
            BufferBuilder cowBuf = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
            cowModel.build(cowBuf, cowMatrix, frozenAt, 0F);
            RenderUtility.buildBuffer(cowBuf);
            SkyEntityGeometry.resetAlpha();

            matrices.pop();

            if (translucent) {
                RenderSystem.depthMask(true);
                RenderSystem.disableBlend();
            }
        }

        if (beamVisible) {
            Vector3f top = new Vector3f((float) frozenPose.lx, (float) frozenPose.ly - 0.4F * s, (float) frozenPose.lz);
            Vector3f bottom = new Vector3f((float) frozenPose.lx, cowY, (float) frozenPose.lz);

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.depthMask(false);

            float pulse = 0.6F + 0.4F * (float) Math.sin(now * 0.006);
            Matrix4f beamMatrix = matrices.peek().getPositionMatrix();
            BufferBuilder beamBuf = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
            drawBeam(beamBuf, beamMatrix, top, bottom, 1.1F * s, 0.4F, 0.85F, 1.0F, 0.35F * pulse * alpha);
            RenderUtility.buildBuffer(beamBuf);

            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
        }
    }

    private static OrbitPose blendPose(OrbitPose a, OrbitPose b, float k) {
        k = Math.max(0F, Math.min(1F, k));
        double lx = a.lx + (b.lx - a.lx) * k;
        double ly = a.ly + (b.ly - a.ly) * k;
        double lz = a.lz + (b.lz - a.lz) * k;
        float yaw = a.yaw + shortestAngleDelta(a.yaw, b.yaw) * k;
        float roll = a.roll + (b.roll - a.roll) * k;
        return new OrbitPose(lx, ly, lz, yaw, roll);
    }

    private static float shortestAngleDelta(float from, float to) {
        float d = to - from;
        while (d > Math.PI) d -= 2F * (float) Math.PI;
        while (d < -Math.PI) d += 2F * (float) Math.PI;
        return d;
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * Math.max(0F, Math.min(1F, t));
    }

    private static float easeIn(float t) {
        t = Math.max(0F, Math.min(1F, t));
        return t * t;
    }

    private static float easeOut(float t) {
        t = Math.max(0F, Math.min(1F, t));
        return 1F - (1F - t) * (1F - t);
    }

    private static float easeInOut(float t) {
        t = Math.max(0F, Math.min(1F, t));
        return t < 0.5F ? 2F * t * t : 1F - (float) Math.pow(-2F * t + 2F, 2) / 2F;
    }

    /** Полупрозрачный луч крестом из двух повёрнутых плоскостей (как у маяка). */
    private static void drawBeam(BufferBuilder buf, Matrix4f m, Vector3f top, Vector3f bottom, float radius, float r, float g, float b, float a) {
        for (int q = 0; q < 2; q++) {
            float ang = q * (float) (Math.PI / 2.0);
            float ox = (float) Math.cos(ang) * radius;
            float oz = (float) Math.sin(ang) * radius;
            Vector3f t1 = new Vector3f(top.x + ox, top.y, top.z + oz);
            Vector3f t2 = new Vector3f(top.x - ox, top.y, top.z - oz);
            Vector3f b1 = new Vector3f(bottom.x + ox * 0.3F, bottom.y, bottom.z + oz * 0.3F);
            Vector3f b2 = new Vector3f(bottom.x - ox * 0.3F, bottom.y, bottom.z - oz * 0.3F);

            beamVertex(buf, m, t1, r, g, b, a);
            beamVertex(buf, m, t2, r, g, b, a);
            beamVertex(buf, m, b2, r, g, b, a * 0.2F);

            beamVertex(buf, m, t1, r, g, b, a);
            beamVertex(buf, m, b2, r, g, b, a * 0.2F);
            beamVertex(buf, m, b1, r, g, b, a * 0.2F);
        }
    }

    private static void beamVertex(BufferBuilder buf, Matrix4f m, Vector3f p, float r, float g, float b, float a) {
        buf.vertex(m, p.x, p.y, p.z).color(r, g, b, a);
    }

    /** Камера-относительная поза орбитального НЛО (та же система координат, что использует renderOne). */
    private static final class OrbitPose {
        final double lx, ly, lz;
        final float yaw, roll;

        OrbitPose(double lx, double ly, double lz, float yaw, float roll) {
            this.lx = lx;
            this.ly = ly;
            this.lz = lz;
            this.yaw = yaw;
            this.roll = roll;
        }
    }
}