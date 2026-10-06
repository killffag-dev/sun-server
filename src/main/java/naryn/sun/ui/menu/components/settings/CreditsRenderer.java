package naryn.sun.ui.menu.components.settings;

import naryn.sun.framework.base.UIContext;
import naryn.sun.framework.msdf.Font;
import naryn.sun.framework.msdf.Fonts;
import naryn.sun.framework.objects.BorderRadius;
import naryn.sun.systems.localization.Localizator;
import naryn.sun.ui.menu.skin.MenuSkin;
import naryn.sun.utility.colors.ColorRGBA;
import naryn.sun.utility.colors.Colors;

/**
 * Рендерер раздела Credits & Open Source Licenses в GUI Settings.
 * Отображает информацию об используемых открытых библиотеках,
 * их авторах и лицензиях без интерактивных тумблеров.
 */
public class CreditsRenderer extends SettingRenderer {

    private record CreditEntry(String nameKey, String author, String license, String descKey) {}

    private static final CreditEntry[] ENTRIES = {
            new CreditEntry("Sodium", "CaffeineMC", "LGPL-3.0", "menu.gui_settings.credits.sodium.desc"),
            new CreditEntry("Lithium", "CaffeineMC", "LGPL-3.0", "menu.gui_settings.credits.lithium.desc"),
            new CreditEntry("ImmediatelyFast", "Raphiiko", "LGPL-3.0", "menu.gui_settings.credits.immediatelyfast.desc"),
            new CreditEntry("FerriteCore", "malte0811", "MIT", "menu.gui_settings.credits.ferritecore.desc"),
            new CreditEntry("EntityCulling", "Tr7zw", "MIT", "menu.gui_settings.credits.entityculling.desc"),
            new CreditEntry("Krypton", "astei", "LGPL-3.0", "menu.gui_settings.credits.krypton.desc"),
            new CreditEntry("ModernFix", "embeddedt", "LGPL-3.0", "menu.gui_settings.credits.modernfix.desc"),
            new CreditEntry("Iris", "IrisShaders", "LGPL-3.0", "menu.gui_settings.credits.iris.desc")
    };

    private static final float ROW_HEIGHT = 28.0F;
    private static final float ROW_GAP = 4.0F;
    private static final float INNER_PAD = 8.0F;

    @Override
    public void render(UIContext context, float x, float y, float width, float settY, float expand) {
        Font nameFont = Fonts.MEDIUM.getFont(7.0F);
        Font authorFont = Fonts.REGULAR.getFont(6.0F);
        Font descFont = Fonts.REGULAR.getFont(6.0F);
        Font badgeFont = Fonts.MEDIUM.getFont(5.5F);

        float curY = settY;
        float cardW = width - INNER_PAD * 2.0F;
        float cardX = x + INNER_PAD;

        MenuSkin skin = MenuSkin.current();

        for (CreditEntry entry : ENTRIES) {
            // Фон плашки библиотеки
            skin.renderCard(context, cardX, curY, cardW, ROW_HEIGHT, BorderRadius.all(4.0F), false, 0.0F, 0.0F, expand);

            // Название библиотеки + автор
            context.drawText(nameFont, entry.nameKey(), cardX + 6.0F, curY + 4.0F,
                    Colors.getTextColor().withAlpha((int) (240 * expand)));

            String authorText = "• " + entry.author();
            float nameW = nameFont.width(entry.nameKey());
            context.drawText(authorFont, authorText, cardX + 8.0F + nameW, curY + 4.5F,
                    Colors.getTextColor().withAlpha((int) (130 * expand)));

            // Лицензионный бэдж (справа)
            float badgeW = badgeFont.width(entry.license()) + 8.0F;
            float badgeH = 11.0F;
            float badgeX = cardX + cardW - badgeW - 6.0F;
            float badgeY = curY + 4.0F;
            ColorRGBA badgeBg = new ColorRGBA(151, 71, 255, (int) (35 * expand));
            context.drawRoundedRect(badgeX, badgeY, badgeW, badgeH, BorderRadius.all(2.5F), badgeBg);
            context.drawCenteredText(badgeFont, entry.license(), badgeX + badgeW / 2.0F, badgeY + (badgeH - badgeFont.height()) / 2.0F,
                    Colors.ACCENT.withAlpha((int) (230 * expand)));

            // Описание назначения на текущем языке
            String desc = Localizator.translate(entry.descKey());
            context.drawText(descFont, desc, cardX + 6.0F, curY + 16.0F,
                    Colors.getTextColor().withAlpha((int) (110 * expand)));

            curY += ROW_HEIGHT + ROW_GAP;
        }
    }

    @Override
    public void handleClick(float x, float y, float width, float settY, double mouseX, double mouseY) {
        // Информационный блок, клики не требуются
    }

    @Override
    public float getContentHeight() {
        return ENTRIES.length * (ROW_HEIGHT + ROW_GAP) + CARD_BOTTOM_PAD;
    }
}
