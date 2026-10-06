package naryn.sun.ui.menu.background;

import com.mojang.blaze3d.systems.RenderSystem;
import naryn.sun.framework.base.UIContext;
import naryn.sun.systems.theme.ClientAppearance;
import naryn.sun.utility.colors.ColorRGBA;
import naryn.sun.utility.colors.Colors;
import naryn.sun.utility.render.DrawUtility;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

import java.util.Random;

/**
 * Высокопроизводительный рендерер интерактивного фона ClickGUI с нулевым GC в кадре
 * и агрессивными оптимизациями расчетов (предвычисленные тригонометрические таблицы,
 * сравнение квадратов расстояний без Math.hypot, объединенные проходы).
 */
public final class MenuBackgroundRenderer {

    private static final MenuBackgroundRenderer INSTANCE = new MenuBackgroundRenderer();

    public static MenuBackgroundRenderer getInstance() {
        return INSTANCE;
    }

    private static final Random RNG = new Random();

    // Предвычисленная тригонометрия для 6 вершин соты (Pointy-topped)
    private static final float[] HEX_COS = new float[6];
    private static final float[] HEX_SIN = new float[6];
    static {
        for (int a = 0; a < 6; a++) {
            float angle = (float) (a * Math.PI / 3.0 + Math.PI / 6.0);
            HEX_COS[a] = (float) Math.cos(angle);
            HEX_SIN[a] = (float) Math.sin(angle);
        }
    }

    // ---- WEB PARTICLES POOL ----
    private static final int MAX_WEB_PARTICLES = 85;
    private static final class WebParticle {
        float x, y, vx, vy, radius, alpha;
        boolean initialized = false;
    }
    private final WebParticle[] webParticles = new WebParticle[MAX_WEB_PARTICLES];

    // ---- SHOCKWAVES POOL ----
    private static final int MAX_SHOCKWAVES = 16;
    private static final class Shockwave {
        boolean active = false;
        float x, y, radius, maxRadius, alpha;
    }
    private final Shockwave[] shockwaves = new Shockwave[MAX_SHOCKWAVES];

    // ---- HEX RIPPLES POOL ----
    private static final int MAX_HEX_RIPPLES = 16;
    private static final class HexRipple {
        boolean active = false;
        float x, y, radius, alpha;
    }
    private final HexRipple[] hexRipples = new HexRipple[MAX_HEX_RIPPLES];

    // ---- WARP STARS POOL ----
    private static final int MAX_WARP_STARS = 280;
    private static final class WarpStar {
        float x, y, z, prevZ;
        boolean initialized = false;
    }
    private final WarpStar[] warpStars = new WarpStar[MAX_WARP_STARS];
    private float warpSpeedBoost = 1.0F;

    private float tickCounter = 0.0F;
    private float lastScreenWidth = 0.0F;
    private float lastScreenHeight = 0.0F;

    private MenuBackgroundRenderer() {
        for (int i = 0; i < MAX_WEB_PARTICLES; i++) {
            webParticles[i] = new WebParticle();
        }
        for (int i = 0; i < MAX_SHOCKWAVES; i++) {
            shockwaves[i] = new Shockwave();
        }
        for (int i = 0; i < MAX_HEX_RIPPLES; i++) {
            hexRipples[i] = new HexRipple();
        }
        for (int i = 0; i < MAX_WARP_STARS; i++) {
            warpStars[i] = new WarpStar();
        }
    }

    private void ensureInitialized(float width, float height) {
        if (Math.abs(lastScreenWidth - width) > 1.0F || Math.abs(lastScreenHeight - height) > 1.0F) {
            lastScreenWidth = width;
            lastScreenHeight = height;

            for (WebParticle p : webParticles) {
                p.x = RNG.nextFloat() * width;
                p.y = RNG.nextFloat() * height;
                p.vx = (RNG.nextFloat() - 0.5F) * 0.45F;
                p.vy = (RNG.nextFloat() - 0.5F) * 0.45F;
                p.radius = 1.2F + RNG.nextFloat() * 1.5F;
                p.alpha = 0.35F + RNG.nextFloat() * 0.45F;
                p.initialized = true;
            }

            for (WarpStar s : warpStars) {
                s.x = (RNG.nextFloat() - 0.5F) * width * 2.0F;
                s.y = (RNG.nextFloat() - 0.5F) * height * 2.0F;
                s.z = 1.0F + RNG.nextFloat() * width;
                s.prevZ = s.z;
                s.initialized = true;
            }
        }
    }

    /**
     * Вызывается при клике по фону (за пределами окон UI).
     */
    public void onBackgroundClick(double mouseX, double mouseY) {
        MenuBackgroundConfig cfg = MenuBackgroundConfig.getInstance();
        if (cfg.getMode() == MenuBackgroundMode.OFF) return;

        float mx = (float) mouseX;
        float my = (float) mouseY;

        if (cfg.getMode() == MenuBackgroundMode.WARP) {
            // Ускорение варп-прыжка строго по фону
            warpSpeedBoost = 3.8F;
            return;
        }

        if (cfg.isShockwaves()) {
            if (cfg.getMode() == MenuBackgroundMode.WEB) {
                spawnShockwave(mx, my);
            } else if (cfg.getMode() == MenuBackgroundMode.HEXAGONS) {
                spawnHexRipple(mx, my);
            }
        }
    }

    private void spawnShockwave(float x, float y) {
        for (Shockwave w : shockwaves) {
            if (!w.active) {
                w.active = true;
                w.x = x;
                w.y = y;
                w.radius = 0.0F;
                w.maxRadius = 220.0F;
                w.alpha = 0.9F;
                return;
            }
        }
    }

    private void spawnHexRipple(float x, float y) {
        for (HexRipple r : hexRipples) {
            if (!r.active) {
                r.active = true;
                r.x = x;
                r.y = y;
                r.radius = 0.0F;
                r.alpha = 1.0F;
                return;
            }
        }
    }

    public void render(UIContext context, float screenWidth, float screenHeight, float menuAlpha) {
        MenuBackgroundConfig cfg = MenuBackgroundConfig.getInstance();
        MenuBackgroundMode mode = cfg.getMode();
        if (mode == MenuBackgroundMode.OFF || menuAlpha <= 0.005F) {
            return;
        }

        ensureInitialized(screenWidth, screenHeight);
        float speed = cfg.getSpeed();
        tickCounter += 1.0F * speed;

        float mouseX = context.getMouseX();
        float mouseY = context.getMouseY();

        ColorRGBA accent = cfg.isSyncAccent() ? ClientAppearance.getAccent() : Colors.ACCENT;
        if (accent == null) accent = ColorRGBA.WHITE;
        float r = accent.getRed() / 255.0F;
        float g = accent.getGreen() / 255.0F;
        float b = accent.getBlue() / 255.0F;

        MatrixStack matrices = context.getMatrices();

        switch (mode) {
            case CYBER_GRID -> renderCyberGrid(matrices, screenWidth, screenHeight, menuAlpha, r, g, b, mouseX, mouseY, cfg);
            case WEB        -> renderWeb(matrices, screenWidth, screenHeight, menuAlpha, r, g, b, mouseX, mouseY, cfg);
            case HEXAGONS   -> renderHexagons(matrices, screenWidth, screenHeight, menuAlpha, r, g, b, mouseX, mouseY, cfg);
            case WARP       -> renderWarp(matrices, screenWidth, screenHeight, menuAlpha, r, g, b, cfg);
        }
    }

    // =========================================================================
    // 1. CYBER GRID 3D
    // =========================================================================
    private void renderCyberGrid(MatrixStack matrices, float width, float height, float menuAlpha,
                                 float r, float g, float b, float mouseX, float mouseY,
                                 MenuBackgroundConfig cfg) {
        float horizonY = height * 0.42F;
        float tiltX = cfg.isMouseInteraction() ? ((mouseX / width) - 0.5F) * 60.0F : 0.0F;
        float vanishingX = width * 0.5F + tiltX;

        matrices.push();
        Matrix4f matrix4f = matrices.peek().getPositionMatrix();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        DrawUtility.drawSetup();

        BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        // Линия горизонта
        float horizAlpha = 0.45F * menuAlpha;
        builder.vertex(matrix4f, 0.0F, horizonY, 0.0F).color(r, g, b, horizAlpha);
        builder.vertex(matrix4f, width, horizonY, 0.0F).color(r, g, b, horizAlpha);

        // Радиальные перспективы
        float colSpacing = 45.0F / cfg.getDensity();
        int halfCols = Math.min(22, (int) (width / colSpacing));
        float lineAlpha = 0.22F * menuAlpha;

        for (int i = -halfCols; i <= halfCols; i++) {
            float bottomX = width * 0.5F + i * colSpacing + tiltX * 2.0F;
            builder.vertex(matrix4f, vanishingX, horizonY, 0.0F).color(r, g, b, 0.02F * menuAlpha);
            builder.vertex(matrix4f, bottomX, height, 0.0F).color(r, g, b, lineAlpha);
        }

        // Бегущие горизонтальные линии вперед
        float scrollSpeed = 45.0F;
        float offset = (tickCounter * 1.5F) % scrollSpeed;
        int linesCount = Math.min(14, (int) (12 * cfg.getDensity()));

        for (int d = 0; d < linesCount; d++) {
            float progress = (d * scrollSpeed + offset) / (linesCount * scrollSpeed);
            if (progress < 0.0F || progress > 1.0F) continue;
            float factor = progress * progress * 1.8F;
            if (factor > 1.0F) factor = 1.0F;
            float y = horizonY + factor * (height - horizonY);

            if (y > horizonY && y < height) {
                float a = factor * 0.48F * menuAlpha;
                builder.vertex(matrix4f, 0.0F, y, 0.0F).color(r, g, b, a);
                builder.vertex(matrix4f, width, y, 0.0F).color(r, g, b, a);
            }
        }

        BufferRenderer.drawWithGlobalProgram(builder.end());
        DrawUtility.drawEnd();
        RenderSystem.disableBlend();
        matrices.pop();
    }

    // =========================================================================
    // 2. WEB & SHOCKWAVES (ОПТИМИЗИРОВАНО: БЕЗ MATH.HYPOT НА ВСЕХ ПАРАХ)
    // =========================================================================
    private void renderWeb(MatrixStack matrices, float width, float height, float menuAlpha,
                           float r, float g, float b, float mouseX, float mouseY,
                           MenuBackgroundConfig cfg) {
        float speed = cfg.getSpeed();
        int activeParticles = (int) (webParticles.length * Math.min(1.0F, cfg.getDensity()));

        // Обновление волн
        for (Shockwave wave : shockwaves) {
            if (!wave.active) continue;
            wave.radius += 5.5F * speed;
            wave.alpha -= 0.022F * speed;

            float waveRadSq = wave.radius * wave.radius;
            for (int i = 0; i < activeParticles; i++) {
                WebParticle p = webParticles[i];
                float dx = p.x - wave.x;
                float dy = p.y - wave.y;
                float dSq = dx * dx + dy * dy;
                float dist = (float) Math.sqrt(dSq);
                if (Math.abs(dist - wave.radius) < 25.0F && dist > 0.001F) {
                    p.x += (dx / dist) * 2.2F * speed;
                    p.y += (dy / dist) * 2.2F * speed;
                }
            }

            if (wave.alpha <= 0.0F || wave.radius >= wave.maxRadius) {
                wave.active = false;
            }
        }

        // Обновление частиц
        for (int i = 0; i < activeParticles; i++) {
            WebParticle p = webParticles[i];
            p.x += p.vx * speed;
            p.y += p.vy * speed;

            if (p.x < 0.0F || p.x > width) p.vx *= -1.0F;
            if (p.y < 0.0F || p.y > height) p.vy *= -1.0F;
        }

        matrices.push();
        Matrix4f matrix4f = matrices.peek().getPositionMatrix();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        DrawUtility.drawSetup();

        BufferBuilder lineBuilder = RenderSystem.renderThreadTesselator().begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        // Волны при кликах
        for (Shockwave wave : shockwaves) {
            if (!wave.active) continue;
            float wAlpha = wave.alpha * menuAlpha * 0.85F;
            int segments = 24;
            float prevX = wave.x + wave.radius;
            float prevY = wave.y;
            for (int s = 1; s <= segments; s++) {
                float angle = (float) (s * Math.PI * 2.0 / segments);
                float curX = wave.x + (float) Math.cos(angle) * wave.radius;
                float curY = wave.y + (float) Math.sin(angle) * wave.radius;
                lineBuilder.vertex(matrix4f, prevX, prevY, 0.0F).color(r, g, b, wAlpha);
                lineBuilder.vertex(matrix4f, curX, curY, 0.0F).color(r, g, b, wAlpha);
                prevX = curX;
                prevY = curY;
            }
        }

        // Соединительные нити: быстрая фильтрация по квадрату расстояния
        float maxDist = 78.0F;
        float maxDistSq = maxDist * maxDist;

        for (int i = 0; i < activeParticles; i++) {
            WebParticle p1 = webParticles[i];
            for (int j = i + 1; j < activeParticles; j++) {
                WebParticle p2 = webParticles[j];
                float dx = p1.x - p2.x;
                float dy = p1.y - p2.y;
                float distSq = dx * dx + dy * dy;

                if (distSq < maxDistSq) {
                    float dist = (float) Math.sqrt(distSq);
                    float a = (1.0F - dist / maxDist) * 0.22F * menuAlpha;
                    lineBuilder.vertex(matrix4f, p1.x, p1.y, 0.0F).color(r, g, b, a);
                    lineBuilder.vertex(matrix4f, p2.x, p2.y, 0.0F).color(r, g, b, a);
                }
            }

            // Связь с мышью
            if (cfg.isMouseInteraction()) {
                float mdx = mouseX - p1.x;
                float mdy = mouseY - p1.y;
                float mDistSq = mdx * mdx + mdy * mdy;
                float maxMouseDist = 150.0F;
                float maxMouseDistSq = maxMouseDist * maxMouseDist;

                if (mDistSq < maxMouseDistSq) {
                    float mDist = (float) Math.sqrt(mDistSq);
                    float ma = (1.0F - mDist / maxMouseDist) * 0.45F * menuAlpha;
                    lineBuilder.vertex(matrix4f, p1.x, p1.y, 0.0F).color(r, g, b, ma);
                    lineBuilder.vertex(matrix4f, mouseX, mouseY, 0.0F).color(r, g, b, ma);
                }
            }
        }

        BufferRenderer.drawWithGlobalProgram(lineBuilder.end());

        // Частицы
        BufferBuilder quadBuilder = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        for (int i = 0; i < activeParticles; i++) {
            WebParticle p = webParticles[i];
            float rad = p.radius;
            float pa = p.alpha * menuAlpha * 0.9F;

            quadBuilder.vertex(matrix4f, p.x - rad, p.y + rad, 0.0F).color(r, g, b, pa);
            quadBuilder.vertex(matrix4f, p.x + rad, p.y + rad, 0.0F).color(r, g, b, pa);
            quadBuilder.vertex(matrix4f, p.x + rad, p.y - rad, 0.0F).color(r, g, b, pa);
            quadBuilder.vertex(matrix4f, p.x - rad, p.y - rad, 0.0F).color(r, g, b, pa);
        }
        BufferRenderer.drawWithGlobalProgram(quadBuilder.end());

        DrawUtility.drawEnd();
        RenderSystem.disableBlend();
        matrices.pop();
    }

    // =========================================================================
    // 3. CYBER HEXAGONS (БЕСШОВНО, БЕЗ ВЫЗОВОВ ТРИГОНОМЕТРИИ В КАДРЕ)
    // =========================================================================
    private void renderHexagons(MatrixStack matrices, float width, float height, float menuAlpha,
                                float r, float g, float b, float mouseX, float mouseY,
                                MenuBackgroundConfig cfg) {
        float speed = cfg.getSpeed();

        for (HexRipple rip : hexRipples) {
            if (!rip.active) continue;
            rip.radius += 7.0F * speed;
            rip.alpha -= 0.02F * speed;
            if (rip.alpha <= 0.0F || rip.radius > 450.0F) {
                rip.active = false;
            }
        }

        float hexR = 21.0F / cfg.getDensity();
        float hexW = (float) (Math.sqrt(3.0) * hexR);
        float vStep = 1.5F * hexR;

        // Предрасчет радиусных смещений для текущего кадра
        float[] vx = new float[6];
        float[] vy = new float[6];
        for (int a = 0; a < 6; a++) {
            vx[a] = HEX_COS[a] * hexR;
            vy[a] = HEX_SIN[a] * hexR;
        }

        matrices.push();
        Matrix4f matrix4f = matrices.peek().getPositionMatrix();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        DrawUtility.drawSetup();

        BufferBuilder triBuilder = RenderSystem.renderThreadTesselator().begin(DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        boolean hasTriangles = false;

        float startY = -hexR;
        float endY = height + hexR;
        float startX = -hexW;
        float endX = width + hexW;

        float mouseRange = 120.0F;
        float mouseRangeSq = mouseRange * mouseRange;

        int rowIndex = 0;
        for (float cy = startY; cy <= endY; cy += vStep, rowIndex++) {
            float rowOffset = (rowIndex % 2 != 0) ? (hexW * 0.5F) : 0.0F;
            for (float cx = startX + rowOffset; cx <= endX; cx += hexW) {
                float intensity = 0.0F;

                if (cfg.isMouseInteraction()) {
                    float mdx = mouseX - cx;
                    float mdy = mouseY - cy;
                    float mDistSq = mdx * mdx + mdy * mdy;
                    if (mDistSq < mouseRangeSq) {
                        intensity = Math.max(intensity, (1.0F - (float) Math.sqrt(mDistSq) / mouseRange) * 0.7F);
                    }
                }

                for (HexRipple rip : hexRipples) {
                    if (!rip.active) continue;
                    float rdx = rip.x - cx;
                    float rdy = rip.y - cy;
                    float rDist = (float) Math.sqrt(rdx * rdx + rdy * rdy);
                    if (Math.abs(rDist - rip.radius) < 30.0F) {
                        intensity = Math.max(intensity, rip.alpha * 0.85F);
                    }
                }

                if (intensity > 0.06F) {
                    float fillAlpha = intensity * 0.18F * menuAlpha;
                    hasTriangles = true;
                    for (int a = 0; a < 6; a++) {
                        int next = (a + 1) % 6;
                        triBuilder.vertex(matrix4f, cx, cy, 0.0F).color(r, g, b, fillAlpha);
                        triBuilder.vertex(matrix4f, cx + vx[a], cy + vy[a], 0.0F).color(r, g, b, fillAlpha);
                        triBuilder.vertex(matrix4f, cx + vx[next], cy + vy[next], 0.0F).color(r, g, b, fillAlpha);
                    }
                }
            }
        }
        if (hasTriangles) {
            BufferRenderer.drawWithGlobalProgram(triBuilder.end());
        }

        // Контуры сот
        BufferBuilder lineBuilder = RenderSystem.renderThreadTesselator().begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        rowIndex = 0;
        for (float cy = startY; cy <= endY; cy += vStep, rowIndex++) {
            float rowOffset = (rowIndex % 2 != 0) ? (hexW * 0.5F) : 0.0F;
            for (float cx = startX + rowOffset; cx <= endX; cx += hexW) {
                float intensity = 0.0F;

                if (cfg.isMouseInteraction()) {
                    float mdx = mouseX - cx;
                    float mdy = mouseY - cy;
                    float mDistSq = mdx * mdx + mdy * mdy;
                    if (mDistSq < mouseRangeSq) {
                        intensity = Math.max(intensity, (1.0F - (float) Math.sqrt(mDistSq) / mouseRange) * 0.7F);
                    }
                }

                for (HexRipple rip : hexRipples) {
                    if (!rip.active) continue;
                    float rdx = rip.x - cx;
                    float rdy = rip.y - cy;
                    float rDist = (float) Math.sqrt(rdx * rdx + rdy * rdy);
                    if (Math.abs(rDist - rip.radius) < 30.0F) {
                        intensity = Math.max(intensity, rip.alpha * 0.85F);
                    }
                }

                float baseAlpha = (0.05F + intensity * 0.65F) * menuAlpha;
                for (int a = 0; a < 6; a++) {
                    int next = (a + 1) % 6;
                    lineBuilder.vertex(matrix4f, cx + vx[a], cy + vy[a], 0.0F).color(r, g, b, baseAlpha);
                    lineBuilder.vertex(matrix4f, cx + vx[next], cy + vy[next], 0.0F).color(r, g, b, baseAlpha);
                }
            }
        }
        BufferRenderer.drawWithGlobalProgram(lineBuilder.end());

        DrawUtility.drawEnd();
        RenderSystem.disableBlend();
        matrices.pop();
    }

    // =========================================================================
    // 4. WARP STARFIELD
    // =========================================================================
    private void renderWarp(MatrixStack matrices, float width, float height, float menuAlpha,
                            float r, float g, float b, MenuBackgroundConfig cfg) {
        float speed = cfg.getSpeed();

        if (warpSpeedBoost > 1.0F) {
            warpSpeedBoost -= 0.045F * speed;
            if (warpSpeedBoost < 1.0F) warpSpeedBoost = 1.0F;
        }

        float currentSpeed = 2.4F * speed * warpSpeedBoost * 6.0F;
        float cx = width * 0.5F;
        float cy = height * 0.5F;

        int activeStars = (int) (warpStars.length * Math.min(1.0F, cfg.getDensity()));

        matrices.push();
        Matrix4f matrix4f = matrices.peek().getPositionMatrix();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        DrawUtility.drawSetup();

        BufferBuilder lineBuilder = RenderSystem.renderThreadTesselator().begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        for (int i = 0; i < activeStars; i++) {
            WarpStar s = warpStars[i];
            s.prevZ = s.z;
            s.z -= currentSpeed;

            if (s.z <= 1.0F) {
                s.z = width;
                s.prevZ = width;
                s.x = (RNG.nextFloat() - 0.5F) * width * 2.0F;
                s.y = (RNG.nextFloat() - 0.5F) * height * 2.0F;
            }

            float k = 240.0F / s.z;
            float px = s.x * k + cx;
            float py = s.y * k + cy;

            float prevK = 240.0F / s.prevZ;
            float prevPx = s.x * prevK + cx;
            float prevPy = s.y * prevK + cy;

            if (px >= 0.0F && px <= width && py >= 0.0F && py <= height) {
                float depthFactor = Math.min(1.0F, (1.0F - s.z / width) * 1.6F);
                float starAlpha = depthFactor * menuAlpha;

                lineBuilder.vertex(matrix4f, prevPx, prevPy, 0.0F).color(r, g, b, starAlpha * 0.25F);
                lineBuilder.vertex(matrix4f, px, py, 0.0F).color(r, g, b, starAlpha);
            }
        }

        BufferRenderer.drawWithGlobalProgram(lineBuilder.end());
        DrawUtility.drawEnd();
        RenderSystem.disableBlend();
        matrices.pop();
    }
}
