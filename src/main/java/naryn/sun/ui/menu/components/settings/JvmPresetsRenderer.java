package naryn.sun.ui.menu.components.settings;

import naryn.sun.Sun;
import naryn.sun.framework.base.UIContext;
import naryn.sun.framework.msdf.Font;
import naryn.sun.framework.msdf.Fonts;
import naryn.sun.framework.objects.BorderRadius;
import naryn.sun.systems.localization.Localizator;
import naryn.sun.systems.notifications.NotificationType;
import naryn.sun.ui.menu.skin.MenuSkin;
import naryn.sun.utility.colors.ColorRGBA;
import naryn.sun.utility.colors.Colors;
import naryn.sun.utility.gui.GuiUtility;
import naryn.sun.utility.jvm.JvmLauncherPresets;
import naryn.sun.utility.profiler.MemoryTracker;

/**
 * Рендерер раздела JVM & GC Tuning в GUI Settings.
 * Показывает статус текущего сборщика мусора и пресеты аргументов для лаунчера с возможностью копирования.
 */
public class JvmPresetsRenderer extends SettingRenderer {

    private static final float ROW_HEIGHT = 44.0F;
    private static final float ROW_GAP = 5.0F;
    private static final float INNER_PAD = 8.0F;
    private static final float STATS_HEIGHT = 20.0F;

    @Override
    public void render(UIContext context, float x, float y, float width, float settY, float expand) {
        Font titleFont = Fonts.MEDIUM.getFont(7.0F);
        Font descFont = Fonts.REGULAR.getFont(5.5F);
        Font argsFont = Fonts.REGULAR.getFont(5.0F);
        Font badgeFont = Fonts.MEDIUM.getFont(5.5F);
        Font statsFont = Fonts.REGULAR.getFont(6.0F);

        float curY = settY;
        float cardW = width - INNER_PAD * 2.0F;
        float cardX = x + INNER_PAD;

        MenuSkin skin = MenuSkin.current();

        // 1. Статус текущего GC и памяти
        skin.renderCard(context, cardX, curY, cardW, STATS_HEIGHT, BorderRadius.all(4.0F), false, 0.0F, 0.0F, expand);
        String gcStatus = "GC: " + MemoryTracker.getGcNames() + " | Heap: " + MemoryTracker.getUsedMemoryMb() + "M / " + MemoryTracker.getMaxMemoryMb() + "M";
        context.drawText(statsFont, gcStatus, cardX + 6.0F, curY + 6.0F, Colors.ACCENT.withAlpha((int) (240 * expand)));
        curY += STATS_HEIGHT + ROW_GAP;

        // 2. Список пресетов
        for (JvmLauncherPresets.Preset preset : JvmLauncherPresets.ALL_PRESETS) {
            boolean hovered = GuiUtility.isHovered(cardX, curY, cardW, ROW_HEIGHT, context.getMouseX(), context.getMouseY());
            if (hovered) {
                naryn.sun.utility.game.cursor.CursorUtility.set(naryn.sun.utility.game.cursor.CursorType.HAND);
            }
            skin.renderCard(context, cardX, curY, cardW, ROW_HEIGHT, BorderRadius.all(4.0F), false, 0.0F, hovered ? 1.0F : 0.0F, expand);

            // Заголовок пресета
            String title = Localizator.translate(preset.nameKey());
            context.drawText(titleFont, title, cardX + 6.0F, curY + 4.0F, Colors.getTextColor().withAlpha((int) (240 * expand)));

            // Бэдж RAM
            float badgeW = badgeFont.width(preset.targetRam()) + 8.0F;
            float badgeH = 11.0F;
            float badgeX = cardX + cardW - badgeW - 6.0F;
            float badgeY = curY + 4.0F;
            ColorRGBA badgeBg = new ColorRGBA(151, 71, 255, (int) (40 * expand));
            context.drawRoundedRect(badgeX, badgeY, badgeW, badgeH, BorderRadius.all(2.5F), badgeBg);
            context.drawCenteredText(badgeFont, preset.targetRam(), badgeX + badgeW / 2.0F, badgeY + (badgeH - badgeFont.height()) / 2.0F,
                    Colors.ACCENT.withAlpha((int) (230 * expand)));

            // Описание
            String desc = Localizator.translate(preset.descKey());
            context.drawText(descFont, desc, cardX + 6.0F, curY + 16.0F, Colors.getTextColor().withAlpha((int) (130 * expand)));

            // Аргументы JVM
            float argsY = curY + 28.0F;
            float argsH = 12.0F;
            context.drawRoundedRect(cardX + 4.0F, argsY, cardW - 8.0F, argsH, BorderRadius.all(2.0F),
                    new ColorRGBA(0, 0, 0, (int) (60 * expand)));
            ColorRGBA argsColor = hovered ? Colors.ACCENT.withAlpha((int) (230 * expand)) : Colors.getTextColor().withAlpha((int) (130 * expand));
            context.drawText(argsFont, preset.args(), cardX + 7.0F, argsY + 2.5F, argsColor);

            curY += ROW_HEIGHT + ROW_GAP;
        }
    }

    @Override
    public void handleClick(float x, float y, float width, float settY, double mouseX, double mouseY) {
        float cardW = width - INNER_PAD * 2.0F;
        float cardX = x + INNER_PAD;
        float curY = settY + STATS_HEIGHT + ROW_GAP;

        for (JvmLauncherPresets.Preset preset : JvmLauncherPresets.ALL_PRESETS) {
            if (GuiUtility.isHovered(cardX, curY, cardW, ROW_HEIGHT, mouseX, mouseY)) {
                if (JvmLauncherPresets.copyToClipboard(preset)) {
                    Sun.getInstance().getNotificationManager().addNotificationOther(
                            NotificationType.INFO,
                            "JVM Tuning",
                            Localizator.translate("menu.gui_settings.jvm.copied")
                    );
                }
                return;
            }
            curY += ROW_HEIGHT + ROW_GAP;
        }
    }

    @Override
    public float getContentHeight() {
        return STATS_HEIGHT + ROW_GAP + JvmLauncherPresets.ALL_PRESETS.length * (ROW_HEIGHT + ROW_GAP) + CARD_BOTTOM_PAD;
    }
}
