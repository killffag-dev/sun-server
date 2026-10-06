package naryn.sun.ui.menu.components.settings;

import naryn.sun.Sun;
import naryn.sun.framework.base.UIContext;
import naryn.sun.framework.msdf.Font;
import naryn.sun.framework.msdf.Fonts;
import naryn.sun.framework.objects.BorderRadius;
import naryn.sun.systems.localization.Language;
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
 * Рендерер настроек Language (Язык интерфейса).
 */
public class LanguageRenderer extends SettingRenderer {

    private static final String[] LANG_KEYS = {"RU_RU", "EN_US"};
    private static final String[] LANG_NAMES = {
            "menu.gui_settings.language.russian",
            "menu.gui_settings.language.english"
    };

    private boolean initializedPenis = false;
    private final Map<String, PenisPlayer> enablePenisMap = new HashMap<>();
    private final Map<String, PenisPlayer> disablePenisMap = new HashMap<>();
    private final Map<String, Boolean> lastStateMap = new HashMap<>();
    private final Map<String, Animation> modeHoverMap = new HashMap<>();
    private final Map<String, Animation> modeActiveMap = new HashMap<>();

    private void ensurePenis() {
        if (initializedPenis) return;
        for (String k : LANG_KEYS) {
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

        Font nameFont = Fonts.REGULAR.getFont(8.0F);
        Font valueFont = Fonts.REGULAR.getFont(7.0F);
        float innerX = x + 9.0F;
        float innerW = width - 18.0F;
        float leftPadding = 10.0F;

        String title = Localizator.translate("menu.gui_settings.language.title");
        context.drawText(nameFont, title, x + leftPadding, settY + 4.0F,
                Colors.getTextColor().withAlpha(220));

        float boxTop = settY + 17.0F;
        float totalValuesH = LANG_KEYS.length * 19.0F;
        String selectedKey = ClientAppearance.getLanguage().name();

        MenuSkin.current().renderSettingBox(
                context, innerX - 1.0F, boxTop, innerW + 2.0F, 8.0F + totalValuesH,
                BorderRadius.all(6.0F), 1.0F
        );

        float offset = 0.0F;
        for (int i = 0; i < LANG_KEYS.length; i++) {
            String k = LANG_KEYS[i];
            boolean selected = k.equalsIgnoreCase(selectedKey);
            float rowHeight = 19.0F;
            float rowTop = boxTop + 3.0F + offset;

            boolean hover = GuiUtility.isHovered(
                    (double)(innerX - 1.0F), (double)rowTop, (double)(innerW + 2.0F), (double)rowHeight,
                    context.getMouseX(), context.getMouseY()
            );
            if (hover) CursorUtility.set(CursorType.HAND);

            Animation hAnim = getModeHoverAnim(k);
            hAnim.update(hover);
            Animation aAnim = getModeActiveAnim(k, selected);
            aAnim.update(selected);

            PenisPlayer enableP = enablePenisMap.get(k);
            PenisPlayer disableP = disablePenisMap.get(k);
            boolean lastState = lastStateMap.getOrDefault(k, false);
            if (selected != lastState) {
                if (selected && enableP != null) enableP.playOnce();
                else if (disableP != null) disableP.playOnce();
                lastStateMap.put(k, selected);
            }

            PenisPlayer currentP = selected ? enableP : disableP;
            if (currentP != null) currentP.update();

            ColorRGBA valColor = Colors.getTextColor()
                    .withAlpha(255.0F * (0.75F + 0.25F * hAnim.getValue() + 0.25F * aAnim.getValue()));
            float lineY = rowTop + valueFont.height() + 0.5F;
            context.drawText(valueFont, Localizator.translate(LANG_NAMES[i]), innerX + 7.0F, lineY, valColor);

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

    @Override
    public void handleClick(float x, float y, float width, float settY, double mouseX, double mouseY) {
        float innerX = x + 9.0F;
        float innerW = width - 18.0F;
        float boxTop = settY + 17.0F;

        for (int i = 0; i < LANG_KEYS.length; i++) {
            float rowTop = boxTop + 3.0F + i * 19.0F;
            if (GuiUtility.isHovered((double)(innerX - 1.0F), (double)rowTop, (double)(innerW + 2.0F), 19.0, mouseX, mouseY)) {
                ClientAppearance.setLanguage(Language.valueOf(LANG_KEYS[i]));
                Sun.getInstance().getFileManager().writeFile("client");
                ClientSoundManager.getInstance().playButtonClick();
                return;
            }
        }
    }

    @Override
    public float getContentHeight() {
        float boxH = getModeBoxHeight(LANG_KEYS.length);
        return SETTINGS_SEP_Y + boxH + CARD_BOTTOM_PAD;
    }
}
