package naryn.sun.systems.target;

import java.util.Comparator;
import java.util.function.Function;
import lombok.Generated;
import naryn.sun.Sun;
import naryn.sun.utility.game.server.ServerUtility;
import naryn.sun.utility.game.FakePlayerEntity;
import naryn.sun.utility.interfaces.IMinecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.DisplayEntity.ItemDisplayEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

public class TargetSettings implements IMinecraft {
   private boolean targetPlayers = false;
   private boolean targetAnimals = false;
   private boolean targetMobs = false;
   private boolean targetInvisibles = false;
   private boolean targetNakedPlayers = false;
   private boolean targetFriends = false;
   private boolean targetArmorStands = false;
   private float requiredRange = -1.0F;
   private Comparator<Entity> targetComparator = TargetComparators.DISTANCE;

   public boolean isEntityValid(Entity entity) {
      if (mc.player != null && mc.world != null && entity != null) {
         if (entity instanceof LivingEntity && entity != mc.player) {
            if (entity instanceof LivingEntity living && living.isDead()) {
               return false;
            } else if (!this.isWithinRange(entity)) {
               return false;
            } else if (entity instanceof ArmorStandEntity) {
               return this.targetArmorStands;
            } else if (!this.targetInvisibles && entity.isInvisible()) {
               return false;
            } else if (entity instanceof FakePlayerEntity) {
               // Манекен "Fake Player" - не настоящий игрок, обычные фильтры для игроков
               // (голый/друг) к нему неприменимы. Единственное, что должно решать,
               // виден ли он ESP/HUD - явный флаг targetPlayers.
               return this.targetPlayers;
            } else if (entity instanceof PlayerEntity player) {
               boolean isFriend = Sun.getInstance().getFriendManager().isFriend(player.getName().getString());
               if (!this.targetFriends && isFriend) {
                  return false;
               } else {
                  boolean isNaked = this.isPlayerNaked(player);
                  if (!this.targetPlayers && !this.targetNakedPlayers) {
                     return false;
                  } else if (this.targetPlayers && this.targetNakedPlayers) {
                     return true;
                  } else {
                     return this.targetNakedPlayers ? isNaked : !isNaked;
                  }
               }
            } else if (entity instanceof MobEntity) {
               return this.targetMobs || (entity instanceof AnimalEntity && this.targetAnimals);
            } else {
               return false;
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public boolean isWithinRange(Entity entity) {
      if (this.getRequiredRange() <= 0.0F) {
         return true;
      }
      if (entity.distanceTo(mc.player) <= this.getRequiredRange()) {
         return true;
      }
      return mc.player != null && mc.player.getEyePos().distanceTo(entity.getBoundingBox().getCenter()) <= this.getRequiredRange();
   }

   private boolean isPlayerNaked(PlayerEntity player) {
      for (ItemStack armorStack : player.getAllArmorItems()) {
         if (armorStack != null && !armorStack.isEmpty()) {
            return false;
         }
      }

      return true;
   }

   @Generated
   public boolean isTargetPlayers() {
      return this.targetPlayers;
   }

   @Generated
   public boolean isTargetAnimals() {
      return this.targetAnimals;
   }

   @Generated
   public boolean isTargetMobs() {
      return this.targetMobs;
   }

   @Generated
   public boolean isTargetInvisibles() {
      return this.targetInvisibles;
   }

   @Generated
   public boolean isTargetNakedPlayers() {
      return this.targetNakedPlayers;
   }

   @Generated
   public boolean isTargetFriends() {
      return this.targetFriends;
   }

   @Generated
   public boolean isTargetArmorStands() {
      return this.targetArmorStands;
   }

   @Generated
   public float getRequiredRange() {
      return this.requiredRange;
   }

   @Generated
   public Comparator<Entity> getTargetComparator() {
      return this.targetComparator;
   }

   public static class Builder {
      private final TargetSettings settings = new TargetSettings();

      public TargetSettings.Builder targetPlayers(boolean targetPlayers) {
         this.settings.targetPlayers = targetPlayers;
         return this;
      }

      public TargetSettings.Builder targetAnimals(boolean targetAnimals) {
         this.settings.targetAnimals = targetAnimals;
         return this;
      }

      public TargetSettings.Builder targetMobs(boolean targetMobs) {
         this.settings.targetMobs = targetMobs;
         return this;
      }

      public TargetSettings.Builder targetInvisibles(boolean targetInvisibles) {
         this.settings.targetInvisibles = targetInvisibles;
         return this;
      }

      public TargetSettings.Builder targetNakedPlayers(boolean targetNakedPlayers) {
         this.settings.targetNakedPlayers = targetNakedPlayers;
         return this;
      }

      public TargetSettings.Builder targetFriends(boolean targetFriends) {
         this.settings.targetFriends = targetFriends;
         return this;
      }

      public TargetSettings.Builder targetArmorStands(boolean targetArmorStands) {
         this.settings.targetArmorStands = targetArmorStands;
         return this;
      }

      public TargetSettings.Builder requiredRange(float range) {
         this.settings.requiredRange = range;
         return this;
      }

      public TargetSettings.Builder sortBy(Comparator<Entity> comparator) {
         this.settings.targetComparator = comparator;
         return this;
      }

      public TargetSettings.Builder sortByValue(Function<Entity, Double> valueExtractor) {
         this.settings.targetComparator = TargetComparators.byValue(valueExtractor);
         return this;
      }

      public TargetSettings.Builder sortByValueReversed(Function<Entity, Double> valueExtractor) {
         this.settings.targetComparator = TargetComparators.byValueReversed(valueExtractor);
         return this;
      }

      public TargetSettings build() {
         return this.settings;
      }
   }
}