package naryn.sun.ui.menu.components.settings;

import naryn.sun.Sun;
import naryn.sun.framework.base.UIContext;
import naryn.sun.framework.msdf.Font;
import naryn.sun.framework.msdf.Fonts;
import naryn.sun.framework.objects.BorderRadius;
import naryn.sun.framework.objects.MouseButton;
import naryn.sun.systems.localization.Localizator;
import naryn.sun.systems.theme.ClientAppearance;
import naryn.sun.ui.menu.skin.MenuSkin;
import naryn.sun.utility.animation.base.Animation;
import naryn.sun.utility.animation.base.Easing;
import naryn.sun.utility.colors.ColorRGBA;
import naryn.sun.utility.colors.Colors;
import naryn.sun.utility.game.cursor.CursorType;
import naryn.sun.utility.game.cursor.CursorUtility;
import naryn.sun.utility.gui.GuiUtility;
import naryn.sun.utility.render.DrawUtility;
import naryn.sun.utility.render.penis.PenisPlayer;
import naryn.sun.utility.sounds.ClientSoundManager;

import java.util.HashMap;
import java.util.Map;

/**
 * Рендерер настроек Appearance (Внешний вид интерфейса + HUD материал).
 * Использует mode-box стиль с чекмарк-анимациями.
 */
public class AppearanceRenderer extends SettingRenderer {

    private static final String[] APPEARANCE_KEYS = {"FACET_DARK", "FACET_FROST"};
    private static final String[] APPEARANCE_NAMES = {
            "menu.gui_settings.interface_material.dark",
            "menu.gui_settings.interface_material.glass"
    };

    private static final String[] HUD_KEYS = {"FACET_DARK", "FACET_FROST"};
    private static final String[] HUD_ANIM_KEYS = {"HUD_DARK", "HUD_FROST"};
    private static final String[] HUD_NAMES = {
            "menu.gui_settings.hud_material.dark",
            "menu.gui_settings.hud_material.glass"
    };

    private boolean initializedPenis = false;
    private final Map<String, PenisPlayer> enablePenisMap = new HashMap<>();
    private final Map<String, PenisPlayer> disablePenisMap = new HashMap<>();
    private final Map<String, Boolean> lastStateMap = new HashMap<>();
    private final Map<String, Animation> modeHoverMap = new HashMap<>();
    private final Map<String, Animation> modeActiveMap = new HashMap<>();

    private void ensurePenis() {
        if (initializedPenis) return;
        String[] keys = {"FACET_DARK", "FACET_FROST", "HUD_DARK", "HUD_FROST"};
        for (String k : keys) {
            enablePenisMap.put(k, new PenisPlayer(Sun.id("penises/check_enable.penis")));
            disablePenisMap.put(k, new PenisPlayer(Sun.id("penises/check_disable.penis")));
            lastStateMap.put(k, false);
        }
        initializedPenis = true;
    }

    private Animation getModeHoverAnim(String key) {
        return modeHoverMap.computeIfAbsent(key, k -> new Animation(300L, 0.0F, Easing.FIGMA_EASE_IN_OUT));
    }

    private Animation getModeActiveAnim(String key, boolean initial) {
        return modeActiveMap.computeIfAbsent(key, k -> new Animation(300L, initial ? 1.0F : 0.0F, Easing.FIGMA_EASE_IN_OUT));
    }

    @Override
    public void render(UIContext context, float x, float y, float width, float settY, float expand) {
        ensurePenis();
        renderModeBox(context, x, width, settY,
                "menu.gui_settings.interface_material.title",
                APPEARANCE_KEYS, APPEARANCE_KEYS, APPEARANCE_NAMES,
                ClientAppearance.getMode().name());

        float box1H = getModeBoxHeight(APPEARANCE_KEYS.length);
        float box2Y = settY + box1H + SETTINGS_SEP_Y;

        renderModeBox(context, x, width, box2Y,
                "menu.gui_settings.hud_material.title",
                HUD_KEYS, HUD_ANIM_KEYS, HUD_NAMES,
                ClientAppearance.getHudMode().name());

        float box2H = getModeBoxHeight(HUD_KEYS.length);
    }

    @Override
    public void handleClick(float x, float y, float width, float settY, double mouseX, double mouseY) {
        float innerX = x + 9.0F;
        float innerW = width - 18.0F;
        float boxTop = settY + 17.0F;

        for (int i = 0; i < APPEARANCE_KEYS.length; i++) {
            float rowTop = boxTop + 3.0F + i * 19.0F;
            if (GuiUtility.isHovered((double)(innerX - 1.0F), (double)rowTop, (double)(innerW + 2.0F), 19.0, mouseX, mouseY)) {
                ClientAppearance.setMode(ClientAppearance.Mode.valueOf(APPEARANCE_KEYS[i]));
                Sun.getInstance().getFileManager().writeFile("client");
                ClientSoundManager.getInstance().playButtonClick();
                return;
            }
        }

        float box1H = 17.0F + 8.0F + APPEARANCE_KEYS.length * 19.0F;
        float box2Top = settY + box1H + SETTINGS_SEP_Y + 17.0F;
        for (int i = 0; i < HUD_KEYS.length; i++) {
            float rowTop = box2Top + 3.0F + i * 19.0F;
            if (GuiUtility.isHovered((double)(innerX - 1.0F), (double)rowTop, (double)(innerW + 2.0F), 19.0, mouseX, mouseY)) {
                ClientAppearance.setHudMode(ClientAppearance.Mode.valueOf(HUD_KEYS[i]));
                Sun.getInstance().getFileManager().writeFile("client");
                ClientSoundManager.getInstance().playButtonClick();
                return;
            }
        }
    }

    @Override
    public void onMouseDragged(float x, float width, double mouseX, double mouseY) {
    }

    @Override
    public void onMouseReleased(double mouseX, double mouseY, MouseButton button) {
    }

    @Override
    public boolean isDragging() {
        return false;
    }

    @Override
    public float getContentHeight() {
        float box1H = getModeBoxHeight(APPEARANCE_KEYS.length);
        float box2H = getModeBoxHeight(HUD_KEYS.length);
        return SETTINGS_SEP_Y + box1H + SETTINGS_SEP_Y + box2H + CARD_BOTTOM_PAD;
    }

    private void renderModeBox(UIContext context, float x, float width, float settY,
                               String titleKey, String[] keys, String[] animKeys,
                               String[] names, String selectedKey) {
        Font nameFont = Fonts.REGULAR.getFont(8.0F);
        Font valueFont = Fonts.REGULAR.getFont(7.0F);
        float innerX = x + 9.0F;
        float innerW = width - 18.0F;
        float leftPadding = 10.0F;

        String title = Localizator.translate(titleKey);
        context.drawText(nameFont, title, x + leftPadding, settY + 4.0F,
                Colors.getTextColor().withAlpha(220));

        float boxTop = settY + 17.0F;
        float totalValuesH = keys.length * 19.0F;

        MenuSkin.current().renderSettingBox(
                context, innerX - 1.0F, boxTop, innerW + 2.0F, 8.0F + totalValuesH,
                BorderRadius.all(6.0F), 1.0F
        );

        float offset = 0.0F;
        for (int i = 0; i < keys.length; i++) {
            String k = keys[i];
            String ak = animKeys != null && i < animKeys.length ? animKeys[i] : k;
            boolean selected = k.equalsIgnoreCase(selectedKey);
            float rowHeight = 19.0F;
            float rowTop = boxTop + 3.0F + offset;

            boolean hover = GuiUtility.isHovered(
                    (double)(innerX - 1.0F), (double)rowTop, (double)(innerW + 2.0F), (double)rowHeight,
                    context.getMouseX(), context.getMouseY()
            );
            if (hover) CursorUtility.set(CursorType.HAND);

            Animation hAnim = getModeHoverAnim(ak);
            hAnim.update(hover);
            Animation aAnim = getModeActiveAnim(ak, selected);
            aAnim.update(selected);

            PenisPlayer enableP = enablePenisMap.get(ak);
            PenisPlayer disableP = disablePenisMap.get(ak);
            boolean lastState = lastStateMap.getOrDefault(ak, false);
            if (selected != lastState) {
                if (selected && enableP != null) enableP.playOnce();
                else if (disableP != null) disableP.playOnce();
                lastStateMap.put(ak, selected);
            }

            PenisPlayer currentP = selected ? enableP : disableP;
            if (currentP != null) currentP.update();

            ColorRGBA valColor = Colors.getTextColor()
                    .withAlpha(255.0F * (0.75F + 0.25F * hAnim.getValue() + 0.25F * aAnim.getValue()));
            float lineY = rowTop + valueFont.height() + 0.5F;
            context.drawText(valueFont, Localizator.translate(names[i]), innerX + 7.0F, lineY, valColor);

            if (currentP != null && (aAnim.getValue() > 0.0F || currentP.isPlaying())) {
                DrawUtility.drawAnimationSprite(
                        context.getMatrices(),
                        currentP.getCurrentSprite(),
                        innerX + innerW - 11.0F - aAnim.getValue() * 2.0F,
                        rowTop + valueFont.height() - 1.0F,
                        6.0F, 6.0F,
                        Colors.getTextColor().mulAlpha(0.1F + 0.9F * aAnim.getValue())
                );
            }
            offset += rowHeight;
        }
    }
}
