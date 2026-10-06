package naryn.sun.systems.modules.modules.visuals.nameutility;

import naryn.sun.Sun;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Formatting;

public final class NameTagStyle {

    // --- Nick colors: friend / target / default ---
    public static final int FRIEND_NAME_COLOR = 0xFF55FF55;
    public static final int TARGET_NAME_COLOR = 0xFFFF5555;
    public static final int DEFAULT_NAME_COLOR = 0xFFFFFFFF;

    // --- Glass Mode Colors (соответствуют материалу "Стекло" в HUD) ---
    public static final int GLASS_BG = 0x900A0C10;
    public static final int GLASS_BORDER_TOP = 0x40FFFFFF;
    public static final int GLASS_BORDER = 0x20FFFFFF;

    // --- Dark Mode Colors (соответствуют материалу "Тёмный" в HUD) ---
    public static final int DARK_BG = 0xE6171820;
    public static final int DARK_BORDER_TOP = 0x30FFFFFF;
    public static final int DARK_BORDER = 0x18FFFFFF;

    private NameTagStyle() {
    }

    public static int getBgColor(boolean isGlass) {
        return isGlass ? GLASS_BG : DARK_BG;
    }

    public static int getBorderColor(boolean isGlass) {
        return isGlass ? GLASS_BORDER : DARK_BORDER;
    }

    public static int getBorderTopColor(boolean isGlass) {
        return isGlass ? GLASS_BORDER_TOP : DARK_BORDER_TOP;
    }

    public static int getPingColor(int ping) {
        if (ping < 0) return 0xFF888888;
        if (ping < 65) return 0xFF50DC78;  // Green
        if (ping < 130) return 0xFFFFD700; // Yellow
        return 0xFFFF4646;                 // Red
    }

    public static int getNameColor(String playerName) {
        return getNameColor(playerName, null);
    }

    public static int getNameColor(String playerName, PlayerEntity player) {
        // 1. Проверка по переданному имени
        if (playerName != null && !playerName.isEmpty()) {
            if (isFriendName(playerName)) {
                return FRIEND_NAME_COLOR;
            }
            if (isTargetName(playerName)) {
                return TARGET_NAME_COLOR;
            }
        }

        // 3. Дополнительная проверка по PlayerEntity
        if (player != null) {
            if (player.getGameProfile() != null) {
                String profileName = player.getGameProfile().getName();
                if (isFriendName(profileName)) {
                    return FRIEND_NAME_COLOR;
                }
                if (isTargetName(profileName)) {
                    return TARGET_NAME_COLOR;
                }
            }
            if (player.getName() != null) {
                String entityName = player.getName().getString();
                if (isFriendName(entityName)) {
                    return FRIEND_NAME_COLOR;
                }
                if (isTargetName(entityName)) {
                    return TARGET_NAME_COLOR;
                }
            }
        }

        return DEFAULT_NAME_COLOR;
    }

    private static boolean isFriendName(String name) {
        if (name == null || name.isEmpty()) return false;
        if (Sun.getInstance().getFriendManager().isFriend(name)) return true;
        String stripped = Formatting.strip(name);
        return stripped != null && !stripped.isEmpty() && Sun.getInstance().getFriendManager().isFriend(stripped);
    }

    private static boolean isTargetName(String name) {
        if (name == null || name.isEmpty()) return false;
        if (Sun.getInstance().getTargetManager().isTarget(name)) return true;
        String stripped = Formatting.strip(name);
        return stripped != null && !stripped.isEmpty() && Sun.getInstance().getTargetManager().isTarget(stripped);
    }
}
