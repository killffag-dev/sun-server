package naryn.sun.systems.modules.modules.visuals;

import naryn.sun.framework.base.CustomDrawContext;
import naryn.sun.framework.msdf.Font;
import naryn.sun.framework.msdf.Fonts;
import naryn.sun.framework.objects.BorderRadius;
import naryn.sun.systems.event.EventListener;
import naryn.sun.systems.event.impl.render.HudRenderEvent;
import naryn.sun.systems.modules.api.ModuleCategory;
import naryn.sun.systems.modules.api.ModuleInfo;
import naryn.sun.systems.modules.impl.PositionableHudModule;
import naryn.sun.systems.modules.modules.visuals.fpsping.FpsPingLogo;
import naryn.sun.systems.modules.modules.visuals.fpsping.RealPingTracker;
import naryn.sun.systems.setting.settings.BooleanSetting;
import naryn.sun.systems.setting.settings.ModeSetting;
import naryn.sun.utility.colors.ColorRGBA;
import naryn.sun.utility.colors.Colors;
import naryn.sun.utility.interfaces.IMinecraft;

@ModuleInfo(name = "FPS Ping", category = ModuleCategory.VISUALS, desc = "modules.descriptions.fps_ping")
public class FpsPing extends PositionableHudModule implements IMinecraft {

    private static final ColorRGBA COLOR_GOOD = new ColorRGBA(80, 220, 120, 255);
    private static final ColorRGBA COLOR_MID  = new ColorRGBA(230, 200, 70, 255);
    private static final ColorRGBA COLOR_BAD  = new ColorRGBA(220, 70, 70, 255);
    private static final ColorRGBA SEPARATOR_COLOR = new ColorRGBA(255, 255, 255, 75);
    private static final ColorRGBA COORD_COLOR = new ColorRGBA(235, 235, 240, 225);

    private final ModeSetting orientation = new ModeSetting(this, "modules.settings.fps_ping.orientation");
    private final ModeSetting.Value horizontal = new ModeSetting.Value(this.orientation, "modules.settings.fps_ping.orientation.horizontal");
    private final ModeSetting.Value vertical = new ModeSetting.Value(this.orientation, "modules.settings.fps_ping.orientation.vertical");

    private final BooleanSetting background = new BooleanSetting(this, "modules.settings.fps_ping.hud.background").enable();
    private final BooleanSetting watermark = new BooleanSetting(this, "modules.settings.fps_ping.watermark").enable();
    private final BooleanSetting ping = new BooleanSetting(this, "modules.settings.fps_ping.ping").enable();
    private final BooleanSetting fps = new BooleanSetting(this, "modules.settings.fps_ping.fps").enable();
    private final BooleanSetting coordinates = new BooleanSetting(this, "modules.settings.fps_ping.coordinates").enable();

    private final RealPingTracker pingTracker = new RealPingTracker();

    private long lastUpdate = 0L;
    private int cachedFps = 0;
    private int cachedPing = 0;

    private final EventListener<HudRenderEvent> onHudRender = event -> this.draw(event.getContext());

    @Override
    public void onEnable() {
        this.pingTracker.start();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        this.pingTracker.stop();
        super.onDisable();
    }

    @Override
    public void tick() {
        this.pingTracker.update();
    }

    @Override
    public void renderPreview(CustomDrawContext context) {
        this.draw(context);
    }

    private void updateMetrics() {
        long now = System.currentTimeMillis();
        if (now - this.lastUpdate >= 500L) {
            this.cachedFps = mc.getCurrentFps();
            this.cachedPing = this.pingTracker.getPing();
            this.lastUpdate = now;
        }
    }

    private void draw(CustomDrawContext context) {
        this.updateMetrics();
        boolean editing = PositionableHudModule.isEditingActive();

        boolean showWm = this.watermark.isEnabled();
        boolean showPing = this.ping.isEnabled();
        boolean showFps = this.fps.isEnabled();
        boolean showCoords = this.coordinates.isEnabled();

        int activeElements = (showWm ? 1 : 0) + (showPing ? 1 : 0) + (showFps ? 1 : 0) + (showCoords ? 1 : 0);
        if (activeElements == 0 && !editing) {
            return;
        }

        Font font = Fonts.REGULAR.getFont(7.0F);
        Font wmFont = Fonts.SEMIBOLD.getFont(7.0F);

        int currentFps = this.cachedFps > 0 ? this.cachedFps : (editing ? 144 : mc.getCurrentFps());
        int currentPing = this.cachedPing >= 0 ? this.cachedPing : (editing ? 24 : this.pingTracker.getPing());

        String pingText = currentPing + " ms";
        String fpsText = currentFps + " FPS";
        String coordsText = "";

        if (showCoords || editing) {
            if (mc.player != null) {
                int px = (int) Math.floor(mc.player.getX());
                int py = (int) Math.floor(mc.player.getY());
                int pz = (int) Math.floor(mc.player.getZ());
                coordsText = px + "    " + py + "    " + pz;
            } else {
                coordsText = "20    -60    12";
            }
        }

        ColorRGBA fpsColor = currentFps >= 60 ? COLOR_GOOD : currentFps >= 30 ? COLOR_MID : COLOR_BAD;
        ColorRGBA pingColor = currentPing < 50 ? COLOR_GOOD : currentPing < 100 ? COLOR_MID : COLOR_BAD;

        if (this.orientation.is(this.vertical)) {
            this.drawVertical(context, font, wmFont, editing, showWm, showPing, showFps, showCoords, pingText, fpsText, coordsText, fpsColor, pingColor);
        } else {
            this.drawHorizontal(context, font, wmFont, editing, showWm, showPing, showFps, showCoords, pingText, fpsText, coordsText, fpsColor, pingColor);
        }
    }

    private void drawHorizontal(
        CustomDrawContext context, Font font, Font wmFont, boolean editing,
        boolean showWm, boolean showPing, boolean showFps, boolean showCoords,
        String pingText, String fpsText, String coordsText,
        ColorRGBA fpsColor, ColorRGBA pingColor
    ) {
        float iconSize = 10.0F;
        float sepWidth = font.width("|");
        float sepGap = 6.0F;
        float totalWidth = 14.0F;
        int renderedCount = 0;

        if (showWm) {
            if (renderedCount > 0) totalWidth += sepGap * 2.0F + sepWidth;
            totalWidth += iconSize + 5.0F + wmFont.width("SUN Visuals");
            renderedCount++;
        }
        if (showPing) {
            if (renderedCount > 0) totalWidth += sepGap * 2.0F + sepWidth;
            totalWidth += font.width(pingText);
            renderedCount++;
        }
        if (showFps) {
            if (renderedCount > 0) totalWidth += sepGap * 2.0F + sepWidth;
            totalWidth += font.width(fpsText);
            renderedCount++;
        }
        if (showCoords) {
            if (renderedCount > 0) totalWidth += sepGap * 2.0F + sepWidth;
            totalWidth += font.width(coordsText);
            renderedCount++;
        }

        if (renderedCount == 0 && editing) {
            totalWidth += font.width("Info Bar");
        }

        float height = 18.0F;
        float x = this.resolveX(10.0F);
        float y = this.resolveY(10.0F);

        this.beginScaledRender(context, totalWidth, height);

        if (editing) {
            context.drawRoundedRect(x, y, totalWidth, height, BorderRadius.all(6.0F), Colors.getBackgroundColor().withAlpha(200.0F));
        } else if (this.background.isEnabled()) {
            context.drawClientRect(x, y, totalWidth, height, 1.0F, 0.0F, 1.0F);
        }

        float textY = y + (height - font.height()) / 2.0F + 1.0F;
        float currX = x + 7.0F;
        boolean needSep = false;

        if (showWm) {
            float iconY = y + (height - iconSize) / 2.0F;
            float wmTextY = y + (height - wmFont.height()) / 2.0F + 1.0F;
            FpsPingLogo.draw(context, currX, iconY, iconSize);
            currX += iconSize + 5.0F;

            context.drawText(wmFont, "SUN Visuals", currX, wmTextY, Colors.getTextColor());
            currX += wmFont.width("SUN Visuals");
            needSep = true;
        }

        if (showPing) {
            if (needSep) {
                currX += sepGap;
                context.drawText(font, "|", currX, textY, SEPARATOR_COLOR);
                currX += sepWidth + sepGap;
            }
            context.drawText(font, pingText, currX, textY, pingColor);
            currX += font.width(pingText);
            needSep = true;
        }

        if (showFps) {
            if (needSep) {
                currX += sepGap;
                context.drawText(font, "|", currX, textY, SEPARATOR_COLOR);
                currX += sepWidth + sepGap;
            }
            context.drawText(font, fpsText, currX, textY, fpsColor);
            currX += font.width(fpsText);
            needSep = true;
        }

        if (showCoords) {
            if (needSep) {
                currX += sepGap;
                context.drawText(font, "|", currX, textY, SEPARATOR_COLOR);
                currX += sepWidth + sepGap;
            }
            context.drawText(font, coordsText, currX, textY, COORD_COLOR);
            currX += font.width(coordsText);
        }

        if (!needSep && editing) {
            context.drawText(font, "Info Bar", currX, textY, Colors.getTextColor());
        }

        this.endScaledRender(context);
    }

    private void drawVertical(
        CustomDrawContext context, Font font, Font wmFont, boolean editing,
        boolean showWm, boolean showPing, boolean showFps, boolean showCoords,
        String pingText, String fpsText, String coordsText,
        ColorRGBA fpsColor, ColorRGBA pingColor
    ) {
        float iconSize = 10.0F;
        float maxContentWidth = 0.0F;
        int linesCount = 0;

        float wmWidth = 0.0F;
        if (showWm) {
            wmWidth = iconSize + 5.0F + wmFont.width("SUN Visuals");
            maxContentWidth = Math.max(maxContentWidth, wmWidth);
            linesCount++;
        }
        if (showPing) {
            maxContentWidth = Math.max(maxContentWidth, font.width(pingText));
            linesCount++;
        }
        if (showFps) {
            maxContentWidth = Math.max(maxContentWidth, font.width(fpsText));
            linesCount++;
        }
        if (showCoords) {
            maxContentWidth = Math.max(maxContentWidth, font.width(coordsText));
            linesCount++;
        }

        if (linesCount == 0 && editing) {
            maxContentWidth = font.width("Info Bar");
            linesCount = 1;
        }

        float totalWidth = maxContentWidth + 20.0F;
        float lineH = 13.0F;
        float height = lineH * Math.max(linesCount, 1) + 8.0F;

        float x = this.resolveX(10.0F);
        float y = this.resolveY(10.0F);

        this.beginScaledRender(context, totalWidth, height);

        if (editing) {
            context.drawRoundedRect(x, y, totalWidth, height, BorderRadius.all(6.0F), Colors.getBackgroundColor().withAlpha(200.0F));
        } else if (this.background.isEnabled()) {
            context.drawClientRect(x, y, totalWidth, height, 1.0F, 0.0F, 1.0F);
        }

        float currY = y + 4.0F;

        if (showWm) {
            float rowX = x + (totalWidth - wmWidth) / 2.0F;
            float iconY = currY + (lineH - iconSize) / 2.0F;
            float textY = currY + (lineH - wmFont.height()) / 2.0F + 1.0F;

            FpsPingLogo.draw(context, rowX, iconY, iconSize);
            context.drawText(wmFont, "SUN Visuals", rowX + iconSize + 5.0F, textY, Colors.getTextColor());
            currY += lineH;
        }

        if (showPing) {
            float rowX = x + (totalWidth - font.width(pingText)) / 2.0F;
            float textY = currY + (lineH - font.height()) / 2.0F + 1.0F;
            context.drawText(font, pingText, rowX, textY, pingColor);
            currY += lineH;
        }

        if (showFps) {
            float rowX = x + (totalWidth - font.width(fpsText)) / 2.0F;
            float textY = currY + (lineH - font.height()) / 2.0F + 1.0F;
            context.drawText(font, fpsText, rowX, textY, fpsColor);
            currY += lineH;
        }

        if (showCoords) {
            float rowX = x + (totalWidth - font.width(coordsText)) / 2.0F;
            float textY = currY + (lineH - font.height()) / 2.0F + 1.0F;
            context.drawText(font, coordsText, rowX, textY, COORD_COLOR);
        }

        if (linesCount == 0 && editing) {
            float rowX = x + (totalWidth - font.width("Info Bar")) / 2.0F;
            float textY = currY + (lineH - font.height()) / 2.0F + 1.0F;
            context.drawText(font, "Info Bar", rowX, textY, Colors.getTextColor());
        }

        this.endScaledRender(context);
    }
}