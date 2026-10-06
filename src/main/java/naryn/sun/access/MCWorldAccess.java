package naryn.sun.access;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;

import java.util.Collections;
import java.util.List;

/**
 * Тонкий слой доступа к текущему миру (ClientWorld) и состояниям блоков.
 * Покрывает паттерны из ASMRBlocks и подобных модулей: проверка воздуха,
 * коллизии, свободного пространства над точкой.
 */
public final class MCWorldAccess {

   public static boolean isPresent() {
      return MinecraftClient.getInstance().world != null;
   }

   public static ClientWorld get() {
      return MinecraftClient.getInstance().world;
   }

   public static BlockState getBlockState(BlockPos pos) {
      ClientWorld world = get();
      return world != null ? world.getBlockState(pos) : null;
   }

   public static boolean isAir(BlockPos pos) {
      BlockState state = getBlockState(pos);
      return state == null || state.isAir();
   }

   public static boolean hasCollision(BlockPos pos) {
      ClientWorld world = get();
      if (world == null) return false;
      BlockState state = world.getBlockState(pos);
      return !state.getCollisionShape(world, pos).isEmpty();
   }

   public static List<? extends PlayerEntity> getPlayers() {
      ClientWorld world = get();
      return world != null ? world.getPlayers() : Collections.emptyList();
   }

   /** Проверяет, что levels блоков над pos свободны (воздух). */
   public static boolean isAirAbove(BlockPos pos, int levels) {
      for (int i = 1; i <= levels; i++) {
         if (!isAir(pos.up(i))) {
            return false;
         }
      }
      return true;
   }

   private MCWorldAccess() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
