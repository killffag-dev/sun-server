package naryn.sun.utility.culling;

import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Высокопроизводительный движок Occlusion Culling (отсечения невидимых объектов).
 * Использует алгоритм 3D DDA (Amanatides-Woo Fast Voxel Traversal) с нулевыми аллокациями в куче
 * и кэшированием результатов видимости по кадрам/тикам.
 */
public final class OcclusionCuller {

    private static final double MIN_CULL_DISTANCE_SQ = 16.0; // 4 блока - защитный радиус (не куллим в упоре)
    private static final long ENTITY_CACHE_TTL_MS = 60L;     // кэш для сущностей (~3-4 кадра при 60 FPS)
    private static final long BLOCK_CACHE_TTL_MS = 250L;     // кэш для блоков (сундуки не двигаются)

    // Кэш сущностей: entityId -> (timestamp | (isOccluded ? 1 : 0))
    private static final Int2LongOpenHashMap ENTITY_CACHE = new Int2LongOpenHashMap(256);
    // Кэш блочных сущностей: BlockPos.asLong() -> (timestamp | (isOccluded ? 1 : 0))
    private static final Long2LongOpenHashMap BLOCK_ENTITY_CACHE = new Long2LongOpenHashMap(512);

    static {
        ENTITY_CACHE.defaultReturnValue(0L);
        BLOCK_ENTITY_CACHE.defaultReturnValue(0L);
    }

    private static final ThreadLocal<BlockPos.Mutable> MUTABLE_POS = ThreadLocal.withInitial(BlockPos.Mutable::new);

    private OcclusionCuller() {
    }

    /**
     * Сброс кэша при смене мира или измерения.
     */
    public static void clearCache() {
        ENTITY_CACHE.clear();
        BLOCK_ENTITY_CACHE.clear();
    }

    /**
     * Проверяет, перекрыта ли сущность сплошными блоками от камеры.
     *
     * @param entity Сущность для проверки
     * @param camera Камера рендера
     * @return true, если сущность ПОЛНОСТЬЮ скрыта за непрозрачными блоками
     */
    public static boolean isEntityOccluded(Entity entity, Camera camera) {
        if (entity == null || camera == null) {
            return false;
        }

        World world = entity.getWorld();
        if (world == null) {
            return false;
        }

        Vec3d camPos = camera.getPos();
        double camX = camPos.x;
        double camY = camPos.y;
        double camZ = camPos.z;

        double entX = entity.getX();
        double entY = entity.getY();
        double entZ = entity.getZ();

        double dx = entX - camX;
        double dy = (entY + entity.getHeight() * 0.5) - camY;
        double dz = entZ - camZ;
        double distSq = dx * dx + dy * dy + dz * dz;

        // Не отсекаем в упор для избежания артефактов при резких движениях
        if (distSq < MIN_CULL_DISTANCE_SQ) {
            return false;
        }

        FrustumCuller.update(camera);
        double radius = Math.max(entity.getWidth(), entity.getHeight()) * 0.85 + 0.75;
        if (!FrustumCuller.isRelativeInside(dx, dy, dz, radius)) {
            return true;
        }

        int entityId = entity.getId();
        long now = System.currentTimeMillis();

        long packed = ENTITY_CACHE.get(entityId);
        if (packed != 0L) {
            long time = packed >>> 1;
            if (now - time < ENTITY_CACHE_TTL_MS) {
                return (packed & 1L) == 1L;
            }
        }

        double height = entity.getHeight();
        // Проверяем 3 контрольные точки (голова/глаза, центр тела, ноги)
        boolean visible = isPointVisible(camX, camY, camZ, entX, entY + height * 0.85, entZ, world)
                || isPointVisible(camX, camY, camZ, entX, entY + height * 0.5, entZ, world)
                || isPointVisible(camX, camY, camZ, entX, entY + 0.15, entZ, world);

        boolean isOccluded = !visible;
        long packedValue = (now << 1) | (isOccluded ? 1L : 0L);
        if (ENTITY_CACHE.size() > 2048) {
            ENTITY_CACHE.clear();
        }
        ENTITY_CACHE.put(entityId, packedValue);

        return isOccluded;
    }

    /**
     * Проверяет, перекрыта ли блочная сущность (сундук, спавнер, табличка) от камеры.
     *
     * @param pos    Позиция блока
     * @param camera Камера рендера
     * @param world  Мир
     * @return true, если блок ПОЛНОСТЬЮ скрыт за непрозрачными блоками
     */
    public static boolean isBlockEntityOccluded(BlockPos pos, Camera camera, World world) {
        if (pos == null || camera == null || world == null) {
            return false;
        }

        Vec3d camPos = camera.getPos();
        double camX = camPos.x;
        double camY = camPos.y;
        double camZ = camPos.z;

        double blockX = pos.getX() + 0.5;
        double blockY = pos.getY() + 0.5;
        double blockZ = pos.getZ() + 0.5;

        double dx = blockX - camX;
        double dy = blockY - camY;
        double dz = blockZ - camZ;
        double distSq = dx * dx + dy * dy + dz * dz;

        if (distSq < MIN_CULL_DISTANCE_SQ) {
            return false;
        }

        FrustumCuller.update(camera);
        if (!FrustumCuller.isRelativeInside(dx, dy, dz, 1.25)) {
            return true;
        }

        long posKey = pos.asLong();
        long now = System.currentTimeMillis();

        long packed = BLOCK_ENTITY_CACHE.get(posKey);
        if (packed != 0L) {
            long time = packed >>> 1;
            if (now - time < BLOCK_CACHE_TTL_MS) {
                return (packed & 1L) == 1L;
            }
        }

        boolean visible = isPointVisible(camX, camY, camZ, blockX, blockY, blockZ, world);
        boolean isOccluded = !visible;

        long packedValue = (now << 1) | (isOccluded ? 1L : 0L);
        if (BLOCK_ENTITY_CACHE.size() > 4096) {
            BLOCK_ENTITY_CACHE.clear();
        }
        BLOCK_ENTITY_CACHE.put(posKey, packedValue);

        return isOccluded;
    }

    /**
     * Быстрый воксельный обход (Amanatides-Woo 3D DDA) луча между точками без создания объектов.
     *
     * @return true, если луч дошел до цели без блокировки сплошными непрозрачными блоками
     */
    public static boolean isPointVisible(
            double x0, double y0, double z0,
            double x1, double y1, double z1,
            World world
    ) {
        int x = (int) Math.floor(x0);
        int y = (int) Math.floor(y0);
        int z = (int) Math.floor(z0);

        int targetX = (int) Math.floor(x1);
        int targetY = (int) Math.floor(y1);
        int targetZ = (int) Math.floor(z1);

        if (x == targetX && y == targetY && z == targetZ) {
            return true;
        }

        double dx = x1 - x0;
        double dy = y1 - y0;
        double dz = z1 - z0;

        int stepX = dx > 0 ? 1 : (dx < 0 ? -1 : 0);
        int stepY = dy > 0 ? 1 : (dy < 0 ? -1 : 0);
        int stepZ = dz > 0 ? 1 : (dz < 0 ? -1 : 0);

        double tDeltaX = stepX != 0 ? Math.abs(1.0 / dx) : Double.MAX_VALUE;
        double tDeltaY = stepY != 0 ? Math.abs(1.0 / dy) : Double.MAX_VALUE;
        double tDeltaZ = stepZ != 0 ? Math.abs(1.0 / dz) : Double.MAX_VALUE;

        double nextVBoundaryX = x + (stepX > 0 ? 1.0 : 0.0);
        double nextVBoundaryY = y + (stepY > 0 ? 1.0 : 0.0);
        double nextVBoundaryZ = z + (stepZ > 0 ? 1.0 : 0.0);

        double tMaxX = stepX != 0 ? (nextVBoundaryX - x0) / dx : Double.MAX_VALUE;
        double tMaxY = stepY != 0 ? (nextVBoundaryY - y0) / dy : Double.MAX_VALUE;
        double tMaxZ = stepZ != 0 ? (nextVBoundaryZ - z0) / dz : Double.MAX_VALUE;

        int maxSteps = 96;
        int steps = 0;
        BlockPos.Mutable mutablePos = MUTABLE_POS.get();

        while (steps++ < maxSteps) {
            if (tMaxX < tMaxY) {
                if (tMaxX < tMaxZ) {
                    x += stepX;
                    tMaxX += tDeltaX;
                } else {
                    z += stepZ;
                    tMaxZ += tDeltaZ;
                }
            } else {
                if (tMaxY < tMaxZ) {
                    y += stepY;
                    tMaxY += tDeltaY;
                } else {
                    z += stepZ;
                    tMaxZ += tDeltaZ;
                }
            }

            if (x == targetX && y == targetY && z == targetZ) {
                return true;
            }

            mutablePos.set(x, y, z);
            if (!world.isChunkLoaded(x >> 4, z >> 4)) {
                return false;
            }

            BlockState state = world.getBlockState(mutablePos);
            if (state.isOpaqueFullCube()) {
                return false;
            }
        }

        return true;
    }
}
