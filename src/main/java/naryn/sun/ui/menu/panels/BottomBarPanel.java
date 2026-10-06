package naryn.sun.ui.menu.panels;

import naryn.sun.framework.base.UIContext;
import naryn.sun.framework.msdf.Font;
import naryn.sun.framework.objects.BorderRadius;
import naryn.sun.ui.menu.skin.MenuSkin;
import naryn.sun.ui.menu.widgets.ActionButton;
import naryn.sun.utility.colors.ColorRGBA;
import naryn.sun.utility.colors.Colors;
import naryn.sun.systems.animation.ClientAnimationConfig;
import naryn.sun.systems.localization.Localizator;
import naryn.sun.utility.animation.base.Animation;
import naryn.sun.utility.animation.base.Easing;
import naryn.sun.utility.game.cursor.CursorType;
import naryn.sun.utility.game.cursor.CursorUtility;
import naryn.sun.utility.gui.GuiUtility;

/**
 * Нижний островок:
 * 1-я кнопка: Modules (открывает вкладку модулей).
 * 2-я кнопка: HUD Editor (при наведении раскрывается: Modules/Interface).
 * 3-я кнопка: GUI Settings.
 *
 * Полностью реагирует на тему (Dark / Frost Glass) через MenuSkin.current().
 */
public final class BottomBarPanel {

    // Keys for localization of bottom bar labels
    private static final String[] LABEL_KEYS = {"menu.bottom_bar.modules", "menu.bottom_bar.hud_editor", "menu.bottom_bar.gui_settings"};
    private static final String[] POPUP_KEYS = {"menu.bottom_bar.modules", "menu.bottom_bar.interface"};

    private static final Animation barIndicatorX = new Animation(240L, 0.0F, Easing.QUARTIC_OUT);
    private static final Animation barIndicatorAlpha = new Animation(240L, 0.0F, Easing.QUARTIC_OUT);
    private static boolean barIndicatorInit = false;

    private BottomBarPanel() {
    }

    public static void render(UIContext context, float alpha, float mouseX, float mouseY,
                              Font botFont, boolean layoutEditMode, boolean moduleEditMode,
                              boolean guiSettingsVisible,
                              float botW, float botH, float botPad, float botR) {
        MenuSkin skin = MenuSkin.current();
        skin.renderPanelShell(context, 0.0F, 0.0F, botW, botH, BorderRadius.all(botR), alpha);

        float btnW = (botW - botPad * 2 - 6.0F) / 3.0F;
        float btnX = botPad;

        // Плавный скользящий бокс между отделами нижнего островка
        int activeIndex = -1;
        if (guiSettingsVisible) {
            activeIndex = 2;
        } else if (!layoutEditMode && !moduleEditMode) {
            activeIndex = 0;
        }

        boolean smooth = ClientAnimationConfig.getInstance().isSmoothTabs();
        float targetX = activeIndex >= 0 ? botPad + activeIndex * (btnW + 3.0F) : barIndicatorX.getValue();
        float targetAlpha = activeIndex >= 0 ? 1.0F : 0.0F;

        if (!barIndicatorInit || !smooth) {
            barIndicatorX.setValue(targetX);
            barIndicatorAlpha.setValue(targetAlpha);
            barIndicatorInit = true;
        } else {
            barIndicatorX.update(targetX);
            barIndicatorAlpha.update(targetAlpha);
        }

        float indAlpha = barIndicatorAlpha.getValue() * alpha;
        if (indAlpha > 0.01F) {
            float indX = barIndicatorX.getValue();
            if (skin.neumorphic()) {
                skin.renderRecessedChip(context, indX, botPad, btnW, botH - botPad * 2,
                        BorderRadius.all(6.0F), indAlpha);
            } else {
                context.drawRoundedRect(indX, botPad, btnW, botH - botPad * 2,
                        BorderRadius.all(6.0F), Colors.ACCENT.withAlpha((int) (35 * indAlpha)));
            }
        }

        for (int i = 0; i < LABEL_KEYS.length; i++) {
            boolean bh = !layoutEditMode && !moduleEditMode
                    && GuiUtility.isHovered(btnX, botPad, btnW, botH - botPad * 2, mouseX, mouseY);
            boolean active = (i == activeIndex);

            if (bh && !active) {
                if (skin.neumorphic()) {
                    skin.renderRecessedChip(context, btnX, botPad, btnW, botH - botPad * 2,
                            BorderRadius.all(6.0F), alpha * 0.7F);
                } else {
                    context.drawRoundedRect(btnX, botPad, btnW, botH - botPad * 2,
                            BorderRadius.all(6.0F), skin.hoverBackground(alpha));
                }
            }

            if (bh) {
                CursorUtility.set(CursorType.HAND);
            }

            if (i == 1 && bh) {
                // HUD Editor: Modules / Interface
                float pbW = btnW / 2.0F;
                float pbX = btnX;
                for (String key : POPUP_KEYS) {
                    String label = Localizator.translate(key);
                    boolean ph = GuiUtility.isHovered(pbX, botPad, pbW, botH - botPad * 2, mouseX, mouseY);
                    if (ph) {
                        context.drawRoundedRect(pbX + 1.0F, botPad + 1.0F, pbW - 2.0F, botH - botPad * 2 - 2.0F,
                                BorderRadius.all(4.0F), ColorRGBA.WHITE.withAlpha((int) (15 * alpha)));
                    }
                    ActionButton.renderTextOnly(context, botFont, label, pbX, 0.0F, pbW, botH,
                            ph, ColorRGBA.WHITE.withAlpha((int) (200 * alpha)), ColorRGBA.WHITE.withAlpha((int) (255 * alpha)), alpha);
                    pbX += pbW;
                }
            } else {
                ColorRGBA normalColor = active
                        ? ColorRGBA.WHITE.withAlpha((int) (255 * alpha))
                        : ColorRGBA.WHITE.withAlpha((int) (200 * alpha));
                ColorRGBA hoverColor = ColorRGBA.WHITE.withAlpha((int) (255 * alpha));
                String label = Localizator.translate(LABEL_KEYS[i]);
                ActionButton.renderTextOnly(context, botFont, label, btnX, 0.0F, btnW, botH,
                        bh || active, normalColor, hoverColor, alpha);
            }

            btnX += btnW + 3.0F;
        }
    }
}