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
import naryn.sun.systems.setting.settings.BooleanSetting;
import naryn.sun.utility.colors.ColorRGBA;
import naryn.sun.utility.colors.Colors;
import naryn.sun.utility.interfaces.IMinecraft;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

@ModuleInfo(name = "Keystrokes", category = ModuleCategory.VISUALS, desc = "Отображает нажатые клавиши движения и кнопки мыши")
public class Keystrokes extends PositionableHudModule implements IMinecraft {

    private final BooleanSetting background = new BooleanSetting(this, "hud.background").enable();
    private final BooleanSetting jumpSetting = new BooleanSetting(this, "modules.settings.keystrokes.jump").enable();
    private final BooleanSetting sneakSetting = new BooleanSetting(this, "modules.settings.keystrokes.sneak").enable();
    private final BooleanSetting showCps = new BooleanSetting(this, "modules.settings.keystrokes.cps").enable();

    private final java.util.List<Long> lmbClicks = new java.util.ArrayList<>();
    private final java.util.List<Long> rmbClicks = new java.util.ArrayList<>();

    private static final float KEY = 20.0F;
    private static final float GAP = 2.0F;
    private static final float BAR_H = 13.0F;
    private static final float TOTAL_W = KEY * 3 + GAP * 2;
    private static final float PANEL_W = TOTAL_W + 16.0F;

    private final EventListener<HudRenderEvent> onHudRender = event -> this.draw(event.getContext());

    private final EventListener<naryn.sun.systems.event.impl.window.MouseEvent> onMouse = event -> {
        if (event.getAction() == GLFW.GLFW_PRESS) {
            long now = System.currentTimeMillis();
            if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                this.lmbClicks.add(now);
            } else if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                this.rmbClicks.add(now);
            }
        }
    };

    @Override
    public void renderPreview(CustomDrawContext context) {
        this.draw(context);
    }

    private int getCps(java.util.List<Long> clicks) {
        long now = System.currentTimeMillis();
        clicks.removeIf(time -> now - time > 1000L);
        return clicks.size();
    }

    private float getPanelHeight() {
        float h = KEY * 3 + GAP * 2;
        if (this.jumpSetting.isEnabled()) {
            h += GAP + BAR_H;
        }
        if (this.sneakSetting.isEnabled()) {
            h += GAP + BAR_H;
        }
        return h + 16.0F;
    }

    private void draw(CustomDrawContext context) {
        Font font = Fonts.REGULAR.getFont(7.0F);
        Font smallFont = Fonts.REGULAR.getFont(5.5F);
        float sw = mc.getWindow().getScaledWidth();
        float sh = mc.getWindow().getScaledHeight();
        float panelH = this.getPanelHeight();

        float x = this.resolveX(sw / 2.0F - PANEL_W / 2.0F);
        float y = this.resolveY(sh - panelH - 20.0F);

        this.beginScaledRender(context, PANEL_W, panelH);

        this.drawBackground(context, this.background, x, y, PANEL_W, panelH);

        long window = mc.getWindow().getHandle();
        boolean w     = (mc.options != null && mc.options.forwardKey.isPressed()) || InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_W);
        boolean a     = (mc.options != null && mc.options.leftKey.isPressed())    || InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_A);
        boolean s     = (mc.options != null && mc.options.backKey.isPressed())    || InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_S);
        boolean d     = (mc.options != null && mc.options.rightKey.isPressed())   || InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_D);
        boolean lmb   = (mc.options != null && mc.options.attackKey.isPressed())  || GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean rmb   = (mc.options != null && mc.options.useKey.isPressed())     || GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        boolean jump  = (mc.options != null && mc.options.jumpKey.isPressed())    || InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_SPACE);
        boolean sneak = (mc.options != null && mc.options.sneakKey.isPressed())   || InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_LEFT_SHIFT) || InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_RIGHT_SHIFT);

        float ox = x + 8.0F;
        float oy = y + 8.0F;
        float mouseBtnW = (TOTAL_W - GAP) / 2.0F;

        // Row 0: W
        this.drawKey(context, font, ox + KEY + GAP, oy, KEY, KEY, "W", w);

        // Row 1: A S D
        this.drawKey(context, font, ox, oy + KEY + GAP, KEY, KEY, "A", a);
        this.drawKey(context, font, ox + KEY + GAP, oy + KEY + GAP, KEY, KEY, "S", s);
        this.drawKey(context, font, ox + (KEY + GAP) * 2, oy + KEY + GAP, KEY, KEY, "D", d);

        // Row 2: LMB RMB
        int lmbCps = this.getCps(this.lmbClicks);
        int rmbCps = this.getCps(this.rmbClicks);
        this.drawMouseKey(context, font, smallFont, ox, oy + (KEY + GAP) * 2, mouseBtnW, KEY, "LMB", lmbCps, lmb);
        this.drawMouseKey(context, font, smallFont, ox + mouseBtnW + GAP, oy + (KEY + GAP) * 2, mouseBtnW, KEY, "RMB", rmbCps, rmb);

        float curY = oy + (KEY + GAP) * 2 + KEY + GAP;

        // Row 3: Jump (Space)
        if (this.jumpSetting.isEnabled()) {
            this.drawKey(context, font, ox, curY, TOTAL_W, BAR_H, "——", jump);
            curY += BAR_H + GAP;
        }

        // Row 4: Sneak (Shift)
        if (this.sneakSetting.isEnabled()) {
            this.drawKey(context, font, ox, curY, TOTAL_W, BAR_H, "SNEAK", sneak);
        }

        this.endScaledRender(context);
    }

    private void drawKey(CustomDrawContext context, Font font, float x, float y, float w, float h, String text, boolean pressed) {
        ColorRGBA bg = pressed ? Colors.getTextColor().withAlpha(220.0F) : ColorRGBA.WHITE.withAlpha(25.0F);
        ColorRGBA fg = pressed ? Colors.getBackgroundColor().withAlpha(255.0F) : Colors.getTextColor().withAlpha(255.0F);
        context.drawRoundedRect(x, y, w, h, BorderRadius.all(4.0F), bg);
        float tw = font.width(text);
        context.drawText(font, text, x + (w - tw) / 2.0F, y + (h - font.height()) / 2.0F, fg);
    }

    private void drawMouseKey(CustomDrawContext context, Font font, Font smallFont, float x, float y, float w, float h, String text, int cps, boolean pressed) {
        ColorRGBA bg = pressed ? Colors.getTextColor().withAlpha(220.0F) : ColorRGBA.WHITE.withAlpha(25.0F);
        ColorRGBA fg = pressed ? Colors.getBackgroundColor().withAlpha(255.0F) : Colors.getTextColor().withAlpha(255.0F);
        context.drawRoundedRect(x, y, w, h, BorderRadius.all(4.0F), bg);

        if (this.showCps.isEnabled()) {
            float tw = font.width(text);
            context.drawText(font, text, x + (w - tw) / 2.0F, y + 2.5F, fg);
            String cpsText = cps + " CPS";
            float cw = smallFont.width(cpsText);
            ColorRGBA subColor = pressed ? Colors.getBackgroundColor().withAlpha(180.0F) : Colors.getTextColor().withAlpha(180.0F);
            context.drawText(smallFont, cpsText, x + (w - cw) / 2.0F, y + 11.5F, subColor);
        } else {
            float tw = font.width(text);
            context.drawText(font, text, x + (w - tw) / 2.0F, y + (h - font.height()) / 2.0F, fg);
        }
    }
}