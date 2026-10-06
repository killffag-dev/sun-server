package naryn.sun.utility.game;

import lombok.Generated;
import naryn.sun.access.MCPlayerAccess;
import naryn.sun.access.MCWorldAccess;
import naryn.sun.utility.game.server.ServerUtility;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MaceItem;
import net.minecraft.item.SwordItem;
import net.minecraft.item.TridentItem;
import net.minecraft.scoreboard.ReadableScoreboardScore;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.number.StyledNumberFormat;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

public final class EntityUtility {
   private static float timer = 1.0F;

   public static void resetTimer() {
      timer = 1.0F;
   }

   public static Block getBlock() {
      return getBlock(0.0, 0.0, 0.0);
   }

   public static Block getBlock(double x, double y, double z) {
      return !isInGame() ? Blocks.AIR : MCWorldAccess.getBlockState(BlockPos.ofFloored(MCPlayerAccess.get().getPos().add(x, y, z))).getBlock();
   }

   public static boolean collideWith(LivingEntity entity) {
      return collideWith(entity, 0.0F);
   }

   public static boolean collideWith(LivingEntity entity, float grow) {
      Box box = MCPlayerAccess.get().getBoundingBox();
      Box targetbox = entity.getBoundingBox().expand(grow, 0.0, grow);
      return box.maxX > targetbox.minX
         && box.maxY > targetbox.minY
         && box.maxZ > targetbox.minZ
         && box.minX < targetbox.maxX
         && box.minY < targetbox.maxY
         && box.minZ < targetbox.maxZ;
   }

   public static void setSpeed(double speed) {
      double forward = MCPlayerAccess.get().input.movementForward;
      double strafe = MCPlayerAccess.get().input.movementSideways;
      float yaw = MCPlayerAccess.get().getYaw();
      if (forward == 0.0 && strafe == 0.0) {
         MCPlayerAccess.get().setVelocity(0.0, MCPlayerAccess.getVelocity().y, 0.0);
      } else {
         if (forward != 0.0) {
            if (strafe > 0.0) {
               yaw += forward > 0.0 ? -45 : 45;
            } else if (strafe < 0.0) {
               yaw += forward > 0.0 ? 45 : -45;
            }

            strafe = 0.0;
            forward = forward > 0.0 ? 1.0 : -1.0;
         }

         double motionX = forward * speed * Math.cos(Math.toRadians(yaw + 90.0)) + strafe * speed * Math.sin(Math.toRadians(yaw + 90.0));
         double motionZ = forward * speed * Math.sin(Math.toRadians(yaw + 90.0)) - strafe * speed * Math.cos(Math.toRadians(yaw + 90.0));
         MCPlayerAccess.get().setVelocity(motionX, MCPlayerAccess.getVelocity().y, motionZ);
      }
   }

   public static boolean isPlayerMoving() {
      return MCPlayerAccess.isPresent() && MCWorldAccess.isPresent() && MCPlayerAccess.get().input != null
         ? MCPlayerAccess.get().forwardSpeed != 0.0 || MCPlayerAccess.get().input.movementSideways != 0.0
         : false;
   }

   public static Block getBlockBelow(Entity entity) {
      if (entity == null) {
         return null;
      } else {
         BlockPos pos = entity.getBlockPos().down();
         return getBlockAt(pos, entity.getWorld());
      }
   }

   public static Block getBlockAbove(Entity entity) {
      if (entity == null) {
         return null;
      } else {
         BlockPos pos = entity.getBlockPos().add(0, Math.round(entity.getHeight()), 0).up();
         return getBlockAt(pos, entity.getWorld());
      }
   }

   public static Block getBlockBelowPlayer() {
      if (MCPlayerAccess.isPresent() && MCWorldAccess.isPresent()) {
         BlockPos pos = MCPlayerAccess.getBlockPos().down().up();
         return getBlockAt(pos, MCWorldAccess.get());
      } else {
         return null;
      }
   }

   public static Block getBlockAbovePlayer() {
      if (MCPlayerAccess.isPresent() && MCWorldAccess.isPresent()) {
         BlockPos pos = MCPlayerAccess.getBlockPos().up();
         return getBlockAt(pos, MCWorldAccess.get());
      } else {
         return null;
      }
   }

   public static Block getBlockStandingOn(Entity entity) {
      if (entity == null) {
         return null;
      } else {
         BlockPos pos = entity.getBlockPos();
         return getBlockAt(pos, entity.getWorld());
      }
   }

   public static double getVelocity() {
      return Math.hypot(MCPlayerAccess.getVelocity().x, MCPlayerAccess.getVelocity().z);
   }

   public static Block getBlockStandingOnPlayer() {
      if (MCPlayerAccess.isPresent() && MCWorldAccess.isPresent()) {
         BlockPos pos = MCPlayerAccess.getBlockPos();
         return getBlockAt(pos, MCWorldAccess.get());
      } else {
         return null;
      }
   }

   public static Block getBlockAt(BlockPos pos, World world) {
      return world.getBlockState(pos).getBlock();
   }

   public static double direction(float rotationYaw, double moveForward, double moveStrafing) {
      if (moveForward < 0.0) {
         rotationYaw += 180.0F;
      }

      float forward = 1.0F;
      if (moveForward < 0.0) {
         forward = -0.5F;
      } else if (moveForward > 0.0) {
         forward = 0.5F;
      }

      if (moveStrafing > 0.0) {
         rotationYaw -= 90.0F * forward;
      }

      if (moveStrafing < 0.0) {
         rotationYaw += 90.0F * forward;
      }

      return Math.toRadians(rotationYaw);
   }

   public static boolean isInGame() {
      return MCPlayerAccess.isPresent() && MCWorldAccess.isPresent();
   }

   public static float getHealth(PlayerEntity ent) {
      if (ent == null) {
         return 0.0F;
      } else if (!ServerUtility.isServerForHPFix()) {
         return ent.getHealth() + ent.getAbsorptionAmount();
      } else {
         ScoreboardObjective scoreBoard = ent.getScoreboard().getObjectiveForSlot(ScoreboardDisplaySlot.BELOW_NAME);
         if (scoreBoard != null) {
            ReadableScoreboardScore score = ent.getScoreboard().getScore(ent, scoreBoard);
            String text = ReadableScoreboardScore.getFormattedScore(score, scoreBoard.getNumberFormatOr(StyledNumberFormat.EMPTY)).getString();
            String digits = text.replaceAll("[^0-9.]", "");

            try {
               return Float.parseFloat(digits);
            } catch (NumberFormatException var6) {
            }
         }

         return ent.getMaxHealth();
      }
   }

   public static boolean isHoldingWeapon() {
      if (!MCPlayerAccess.isPresent()) {
         return false;
      } else {
         ItemStack heldStack = MCPlayerAccess.get().getMainHandStack();
         Item heldItem = heldStack.getItem();
         return heldStack.isEmpty()
            ? false
            : heldItem instanceof SwordItem || heldItem instanceof AxeItem || heldItem instanceof TridentItem || heldItem instanceof MaceItem;
      }
   }

   @Generated
   private EntityUtility() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }

   @Generated
   public static void setTimer(float timer) {
      EntityUtility.timer = timer;
   }

   @Generated
   public static float getTimer() {
      return timer;
   }
}
