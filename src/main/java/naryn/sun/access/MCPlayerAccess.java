package naryn.sun.access;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public final class MCPlayerAccess {

   public static boolean isPresent() {
      return MinecraftClient.getInstance().player != null;
   }

   public static ClientPlayerEntity get() {
      return MinecraftClient.getInstance().player;
   }

   public static boolean isOnGround() {
      ClientPlayerEntity player = get();
      return player != null && player.isOnGround();
   }

   public static BlockPos getBlockPos() {
      ClientPlayerEntity player = get();
      return player != null ? player.getBlockPos() : BlockPos.ORIGIN;
   }

   public static Vec3d getPos() {
      ClientPlayerEntity player = get();
      return player != null ? player.getPos() : Vec3d.ZERO;
   }

   public static double getX() {
      ClientPlayerEntity player = get();
      return player != null ? player.getX() : 0.0;
   }

   public static double getY() {
      ClientPlayerEntity player = get();
      return player != null ? player.getY() : 0.0;
   }

   public static double getZ() {
      ClientPlayerEntity player = get();
      return player != null ? player.getZ() : 0.0;
   }

   public static Vec3d getVelocity() {
      ClientPlayerEntity player = get();
      return player != null ? player.getVelocity() : Vec3d.ZERO;
   }

   private MCPlayerAccess() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
