package naryn.sun.systems.modules.modules.visuals.prediction;

import java.util.ArrayList;
import java.util.List;
import naryn.sun.access.MCPlayerAccess;
import naryn.sun.access.MCWorldAccess;
import naryn.sun.systems.setting.settings.SelectSetting;
import naryn.sun.systems.setting.settings.shared.PredicateValue;
import naryn.sun.utility.game.PotionUtility;
import naryn.sun.utility.game.TextUtility;
import naryn.sun.utility.inventory.EnchantmentUtility;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ChargedProjectilesComponent;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.projectile.AbstractWindChargeEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.EnderPearlItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.TridentItem;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

public final class ProjectilePhysics {

    private static final class MutableRaycastContext extends RaycastContext {
        private Vec3d mutableStart = Vec3d.ZERO;
        private Vec3d mutableEnd = Vec3d.ZERO;

        public MutableRaycastContext() {
            super(Vec3d.ZERO, Vec3d.ZERO, ShapeType.COLLIDER, FluidHandling.NONE, ShapeContext.absent());
        }

        public void set(Vec3d start, Vec3d end) {
            this.mutableStart = start;
            this.mutableEnd = end;
        }

        @Override
        public Vec3d getStart() {
            return this.mutableStart;
        }

        @Override
        public Vec3d getEnd() {
            return this.mutableEnd;
        }
    }

    private static final ThreadLocal<MutableRaycastContext> RAYCAST_CONTEXT =
        ThreadLocal.withInitial(MutableRaycastContext::new);

    private static final java.util.function.Predicate<Entity> ENTITY_COLLISION_FILTER = entity ->
        entity.isAlive()
        && !(entity instanceof ItemEntity)
        && !(entity instanceof ExperienceOrbEntity)
        && !(entity instanceof AreaEffectCloudEntity);

    private ProjectilePhysics() {
    }

    public static void collectInHandTrajectories(List<TrajectoryData> output) {
        AbstractClientPlayerEntity player = MCPlayerAccess.get();
        if (player == null) return;

        ItemStack stack = player.getMainHandStack();
        if (stack.isEmpty() || !isSupportedHandItem(stack, player)) {
            stack = player.getOffHandStack();
            if (stack.isEmpty() || !isSupportedHandItem(stack, player)) {
                return;
            }
        }

        // 1. Лук (при натяжении рассчитываем силу натяжения)
        if (stack.getItem() instanceof BowItem && player.isUsingItem()) {
            int useTicks = player.getItemUseTime();
            float pull = BowItem.getPullProgress(useTicks);
            float speed = Math.max(0.12F, pull) * 3.0F;
            simulateHandShot(player, stack, "Стрела", 0.0F, 0.0F, speed, 0.05, 0.99, 0.0F, output);
            return;
        }

        // 2. Арбалет (заряженный или натягиваемый, с гарантированной поддержкой Multishot на 3 стрелы)
        if (stack.getItem() instanceof CrossbowItem && (CrossbowItem.isCharged(stack) || player.isUsingItem())) {
            boolean multishot = isMultishotCrossbow(stack);
            if (multishot) {
                simulateHandShot(player, stack, "Стрела", 0.0F, -10.0F, 3.15F, 0.05, 0.99, 0.0F, output);
                simulateHandShot(player, stack, "Стрела", 0.0F, 0.0F, 3.15F, 0.05, 0.99, 0.0F, output);
                simulateHandShot(player, stack, "Стрела", 0.0F, 10.0F, 3.15F, 0.05, 0.99, 0.0F, output);
            } else {
                simulateHandShot(player, stack, "Стрела", 0.0F, 0.0F, 3.15F, 0.05, 0.99, 0.0F, output);
            }
            return;
        }

        // 3. Трезубец (при замахе)
        if (stack.getItem() instanceof TridentItem && player.isUsingItem()) {
            simulateHandShot(player, stack, "Трезубец", 0.0F, 0.0F, 2.5F, 0.05, 0.99, 0.0F, output);
            return;
        }

        // 4. Эндер-жемчуг
        if (stack.getItem() instanceof EnderPearlItem) {
            simulateHandShot(player, stack, "Эндер-жемчуг", 0.0F, 0.0F, 1.5F, 0.03, 0.99, 0.0F, output);
            return;
        }

        // 5. Взрывные / оседающие зелья (радиус AoE блоков: 2 для обычных, 3 для оседающих)
        if (stack.isOf(Items.SPLASH_POTION) || stack.isOf(Items.LINGERING_POTION)) {
            String potionName = stack.getName().getString().replace("] ", "").replace("[", "");
            float radius = stack.isOf(Items.LINGERING_POTION) ? 3.0F : 2.0F;
            simulateHandShot(player, stack, potionName, -20.0F, 0.0F, 0.5F, 0.05, 0.99, radius, output);
            return;
        }

        // 6. Ветровой заряд (радиус AoE блоков: 1)
        if (stack.isOf(Items.WIND_CHARGE)) {
            simulateHandShot(player, stack, "Ветровой заряд", 0.0F, 0.0F, 1.5F, 0.0, 0.99, 1.0F, output);
            return;
        }

        // 7. Снежки и яйца
        if (stack.isOf(Items.SNOWBALL)) {
            simulateHandShot(player, stack, "Снежок", 0.0F, 0.0F, 1.5F, 0.03, 0.99, 0.0F, output);
        } else if (stack.isOf(Items.EGG)) {
            simulateHandShot(player, stack, "Яйцо", 0.0F, 0.0F, 1.5F, 0.03, 0.99, 0.0F, output);
        }
    }

    private static boolean isMultishotCrossbow(ItemStack stack) {
        ChargedProjectilesComponent charged = stack.get(DataComponentTypes.CHARGED_PROJECTILES);
        if (charged != null && charged.getProjectiles().size() > 1) {
            return true;
        }
        if (EnchantmentUtility.getEnchantmentLevel(stack, Enchantments.MULTISHOT) > 0) {
            return true;
        }
        ItemEnchantmentsComponent ench = stack.get(DataComponentTypes.ENCHANTMENTS);
        if (ench != null) {
            for (var entry : ench.getEnchantmentEntries()) {
                if (entry.getKey().matchesKey(Enchantments.MULTISHOT)) {
                    return true;
                }
                String keyStr = entry.getKey().getIdAsString();
                if (keyStr != null && keyStr.contains("multishot")) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isSupportedHandItem(ItemStack stack, AbstractClientPlayerEntity player) {
        if (stack.getItem() instanceof BowItem) {
            return player.isUsingItem();
        }
        if (stack.getItem() instanceof CrossbowItem) {
            return CrossbowItem.isCharged(stack) || player.isUsingItem();
        }
        if (stack.getItem() instanceof TridentItem) {
            return player.isUsingItem();
        }
        return stack.getItem() instanceof EnderPearlItem
            || stack.isOf(Items.SPLASH_POTION)
            || stack.isOf(Items.LINGERING_POTION)
            || stack.isOf(Items.WIND_CHARGE)
            || stack.isOf(Items.SNOWBALL)
            || stack.isOf(Items.EGG);
    }

    private static void simulateHandShot(
        AbstractClientPlayerEntity shooter,
        ItemStack stack,
        String baseName,
        float pitchOffset,
        float yawOffset,
        float speed,
        double gravity,
        double drag,
        float splashRadius,
        List<TrajectoryData> output
    ) {
        float pitch = shooter.getPitch() + pitchOffset;
        float yaw = shooter.getYaw() + yawOffset;
        float f = -MathHelper.sin(yaw * 0.017453292F) * MathHelper.cos(pitch * 0.017453292F);
        float g = -MathHelper.sin(pitch * 0.017453292F);
        float h = MathHelper.cos(yaw * 0.017453292F) * MathHelper.cos(pitch * 0.017453292F);

        Vec3d motion = new Vec3d(f, g, h).normalize().multiply(speed);
        Vec3d move = shooter.getMovement();
        motion = motion.add(move.x, shooter.isOnGround() ? 0.0 : move.y, move.z);

        Vec3d startPos = shooter.getEyePos().subtract(0.0, 0.1, 0.0);
        simulateTrajectory(shooter, stack, baseName, startPos, motion, gravity, drag, true, splashRadius, output);
    }

    public static void collectWorldTrajectories(SelectSetting entitiesFilter, List<TrajectoryData> output) {
        if (MCWorldAccess.get() == null) return;

        for (Entity entity : MCWorldAccess.get().getEntities()) {
            // Активные оседающие облака зелий в мире — подсвечиваем область под ними всегда
            if (entity instanceof AreaEffectCloudEntity cloud && cloud.isAlive()) {
                if (!isEntityFilterEnabled(cloud, entitiesFilter)) continue;

                BlockPos cloudBasePos = BlockPos.ofFloored(cloud.getX(), cloud.getY(), cloud.getZ());
                BlockHitResult bHit = new BlockHitResult(cloud.getPos(), Direction.UP, cloudBasePos, false);
                List<Vec3d> positions = new ArrayList<>();
                positions.add(cloud.getPos());
                output.add(new TrajectoryData(
                    cloud,
                    positions,
                    0,
                    null,
                    bHit,
                    Items.LINGERING_POTION.getDefaultStack(),
                    "Оседающее зелье",
                    false,
                    null,
                    null,
                    Math.max(1.0F, cloud.getRadius())
                ));
                continue;
            }

            if (!isValidWorldEntity(entity, entitiesFilter)) continue;

            Vec3d motion = entity.getVelocity();
            if (motion.lengthSquared() < 0.001) continue;

            double gravity = entity.getFinalGravity();
            double drag = 0.99;
            ItemStack stack = resolveItemStack(entity);
            String name = resolveEntityName(entity, stack);
            float splashRadius = resolveSplashRadius(entity, stack);

            simulateTrajectory(entity, stack, name, entity.getPos(), motion, gravity, drag, false, splashRadius, output);
        }
    }

    private static void simulateTrajectory(
        Entity sourceEntity,
        ItemStack stack,
        String baseName,
        Vec3d startPos,
        Vec3d initialMotion,
        double gravity,
        double drag,
        boolean inHand,
        float splashRadius,
        List<TrajectoryData> output
    ) {
        List<Vec3d> positions = new ArrayList<>(150);
        positions.add(startPos);

        Vec3d currentPos = startPos;
        Vec3d currentMotion = initialMotion;
        Entity collidedEntity = null;
        BlockHitResult blockHitResult = null;
        int ticks = 0;
        MutableRaycastContext raycastContext = RAYCAST_CONTEXT.get();

        for (int i = 0; i < 150; i++) {
            currentMotion = currentMotion.multiply(drag).add(0.0, -gravity, 0.0);
            Vec3d nextPos = currentPos.add(currentMotion);
            ticks = i + 1;

            raycastContext.set(currentPos, nextPos);
            BlockHitResult bHit = MCWorldAccess.get().raycast(raycastContext);

            Entity eHit = checkEntityCollision(currentPos, nextPos, sourceEntity);
            if (eHit != null) {
                positions.add(nextPos);
                collidedEntity = eHit;
                break;
            }

            if (bHit.getType() != HitResult.Type.MISS) {
                positions.add(bHit.getPos());
                blockHitResult = bHit;
                break;
            }

            positions.add(nextPos);
            currentPos = nextPos;
        }

        if (!positions.isEmpty()) {
            String displayName = baseName + " (" + TextUtility.formatNumber(ticks / 20.0F) + " сек)";
            List<StatusEffectInstance> effects = PotionUtility.effects(stack);
            List<String> effectLabels = null;
            if (effects != null && !effects.isEmpty()) {
                effectLabels = new ArrayList<>(effects.size());
                for (StatusEffectInstance effect : effects) {
                    String effName = ((StatusEffect) effect.getEffectType().value()).getName().getString();
                    int amp = effect.getAmplifier();
                    String effLevel = amp > 0 ? " " + (amp + 1) : "";
                    int seconds = effect.getDuration() / 20;
                    int minutes = seconds / 60;
                    int rem = seconds % 60;
                    String effTime = minutes + (rem < 10 ? ":0" : ":") + rem;
                    effectLabels.add(effName + effLevel + " (" + effTime + ")");
                }
            }
            output.add(new TrajectoryData(
                sourceEntity,
                positions,
                ticks,
                collidedEntity,
                blockHitResult,
                stack,
                displayName,
                inHand,
                effects,
                effectLabels,
                splashRadius
            ));
        }
    }

    private static float resolveSplashRadius(Entity entity, ItemStack stack) {
        if (entity instanceof PotionEntity potion) {
            return potion.getStack().isOf(Items.LINGERING_POTION) ? 3.0F : 2.0F;
        }
        if (entity instanceof AbstractWindChargeEntity) {
            return 1.0F;
        }
        if (stack.isOf(Items.LINGERING_POTION)) {
            return 3.0F;
        }
        if (stack.isOf(Items.SPLASH_POTION)) {
            return 2.0F;
        }
        if (stack.isOf(Items.WIND_CHARGE)) {
            return 1.0F;
        }
        return 0.0F;
    }

    private static Entity checkEntityCollision(Vec3d currentPos, Vec3d nextPos, Entity ignoreEntity) {
        double dx = nextPos.x - currentPos.x;
        double dy = nextPos.y - currentPos.y;
        double dz = nextPos.z - currentPos.z;
        double distSq = dx * dx + dy * dy + dz * dz;
        if (distSq == 0.0) return null;

        double minX = Math.min(currentPos.x, nextPos.x) - 0.5;
        double minY = Math.min(currentPos.y, nextPos.y) - 0.5;
        double minZ = Math.min(currentPos.z, nextPos.z) - 0.5;
        double maxX = Math.max(currentPos.x, nextPos.x) + 0.5;
        double maxY = Math.max(currentPos.y, nextPos.y) + 0.5;
        double maxZ = Math.max(currentPos.z, nextPos.z) + 0.5;
        Box box = new Box(minX, minY, minZ, maxX, maxY, maxZ);

        Entity player = MCPlayerAccess.get();
        Entity shooter = ignoreEntity != null ? ignoreEntity : player;
        EntityHitResult hitResult = ProjectileUtil.raycast(
            shooter,
            currentPos,
            nextPos,
            box,
            entity -> ENTITY_COLLISION_FILTER.test(entity) && entity != ignoreEntity && entity != player,
            distSq
        );
        return hitResult != null ? hitResult.getEntity() : null;
    }

    private static boolean isEntityFilterEnabled(Entity entity, SelectSetting entitiesFilter) {
        for (SelectSetting.Value selectedValue : entitiesFilter.getSelectedValues()) {
            if (selectedValue instanceof PredicateValue<?> predicate) {
                if (((PredicateValue<Entity>) predicate).predicated(entity)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isValidWorldEntity(Entity entity, SelectSetting entitiesFilter) {
        if (entity instanceof TridentEntity trident && trident.returnTimer > 0) {
            return false;
        }

        for (SelectSetting.Value selectedValue : entitiesFilter.getSelectedValues()) {
            PredicateValue<Entity> predicate = (PredicateValue<Entity>) selectedValue;
            if (predicate.predicated(entity)) {
                return Math.abs(entity.getVelocity().x + entity.getVelocity().z) > 0.01F || Math.abs(entity.getVelocity().y) > 0.2F;
            }
        }
        return false;
    }

    private static ItemStack resolveItemStack(Entity entity) {
        return switch (entity) {
            case ThrownItemEntity item -> item.getStack();
            case PersistentProjectileEntity itemx -> itemx.getItemStack();
            case ItemEntity itemxx -> itemxx.getStack();
            case AbstractWindChargeEntity wind -> Items.WIND_CHARGE.getDefaultStack();
            default -> Items.ARROW.getDefaultStack();
        };
    }

    private static String resolveEntityName(Entity entity, ItemStack stack) {
        if (entity instanceof PotionEntity potion) {
            return potion.getStack().getFormattedName().getString().replace("] ", "").replace("[", "");
        }
        return entity.getName().getString()
            .replace("Брошенный эндер-жемчуг", "Эндер-жемчуг")
            .replace("] ", "")
            .replace("[", "");
    }
}
