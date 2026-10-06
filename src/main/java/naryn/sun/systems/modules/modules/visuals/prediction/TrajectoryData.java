package naryn.sun.systems.modules.modules.visuals.prediction;

import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Vec3d;

public record TrajectoryData(
    Entity entity,
    List<Vec3d> positions,
    int ticks,
    Entity collidedEntity,
    BlockHitResult hitResult,
    ItemStack itemStack,
    String displayName,
    boolean inHand,
    List<StatusEffectInstance> potionEffects,
    List<String> effectLabels,
    float splashRadius
) {
    public Vec3d getLandingPos() {
        if (positions.isEmpty()) {
            return Vec3d.ZERO;
        }
        return positions.getLast();
    }
}
