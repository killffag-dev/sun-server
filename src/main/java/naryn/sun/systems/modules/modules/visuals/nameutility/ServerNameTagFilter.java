package naryn.sun.systems.modules.modules.visuals.nameutility;

import naryn.sun.Sun;
import naryn.sun.systems.modules.modules.visuals.NameUtility;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Фильтрация и подавление серверных сущностей-голограмм (ArmorStand, TextDisplay),
 * используемых серверами для отрисовки приписок над головами игроков.
 */
public final class ServerNameTagFilter {

    private ServerNameTagFilter() {
    }

    public static boolean shouldSuppress(Entity entity) {
        if (entity == null) {
            return false;
        }

        NameUtility nameUtility = Sun.getInstance().getModuleManager().getModule(NameUtility.class);
        if (nameUtility == null || !nameUtility.isEnabled() || !nameUtility.getNameTag().isEnabled()) {
            return false;
        }

        return isServerPlayerNametag(entity);
    }

    public static boolean isServerPlayerNametag(Entity entity) {
        boolean isArmorStand = entity instanceof ArmorStandEntity;
        boolean isDisplay = entity instanceof DisplayEntity.TextDisplayEntity;
        if (!isArmorStand && !isDisplay) {
            return false;
        }

        // Сущность сидит прямо на игроке (пассажир)
        Entity rootVehicle = entity.getRootVehicle();
        if (rootVehicle instanceof PlayerEntity) {
            return true;
        }
        Entity directVehicle = entity.getVehicle();
        if (directVehicle instanceof PlayerEntity) {
            return true;
        }

        // Если это арморстенд, проверяем характерные маркеры серверных голограмм
        if (isArmorStand) {
            ArmorStandEntity armorStand = (ArmorStandEntity) entity;
            boolean isHologramMarker = armorStand.isMarker() || armorStand.isSmall() || armorStand.isInvisible();
            boolean hasName = armorStand.hasCustomName() || armorStand.isCustomNameVisible();
            if (!isHologramMarker || !hasName) {
                return false;
            }
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) {
            return false;
        }

        double entityX = entity.getX();
        double entityY = entity.getY();
        double entityZ = entity.getZ();

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == entity) {
                continue;
            }
            double dx = entityX - player.getX();
            double dz = entityZ - player.getZ();
            if (dx * dx + dz * dz <= 0.64) {
                double dy = entityY - player.getY();
                if (dy >= -0.5 && dy <= 3.2) {
                    return true;
                }
            }
        }

        return false;
    }
}
