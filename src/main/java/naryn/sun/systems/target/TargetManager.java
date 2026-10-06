package naryn.sun.systems.target;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;
import lombok.Generated;
import naryn.sun.Sun;
import naryn.sun.systems.localization.Localizator;
import naryn.sun.utility.game.EntityUtility;
import naryn.sun.utility.game.MessageUtility;
import naryn.sun.utility.interfaces.IMinecraft;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.jetbrains.annotations.Nullable;

public class TargetManager implements IMinecraft {
   @Nullable
   private Entity currentTarget = null;
   private final List<String> target = new ArrayList<>();

   public void update(TargetSettings targetSettings) {
      this.currentTarget = this.getBestTarget(targetSettings);
   }

   public void addTarget(String name) {
      if (Sun.getInstance().getFriendManager().listFriends().contains(name)) {
         MessageUtility.error(Text.of(Localizator.translate("commands.target.friend_error")));
      } else if (this.target.contains(name)) {
         MessageUtility.error(Text.of(Localizator.translate("commands.target.already_exists", name)));
      } else if (name.equalsIgnoreCase(mc.getSession().getUsername())) {
         MessageUtility.error(Text.of(Localizator.translate("commands.target.self_error")));
      } else {
         this.target.add(name);
         MessageUtility.info(Text.of(Localizator.translate("commands.target.added", name)));
         if (EntityUtility.isInGame()) {
            Sun.getInstance().getFileManager().writeFile("client");
         }
      }
   }

   public void removeTarget(String name) {
      if (!this.target.contains(name)) {
         MessageUtility.error(Text.of(Localizator.translate("commands.target.not_found", name)));
      } else {
         this.target.remove(name);
         MessageUtility.info(Text.of(Localizator.translate("commands.target.removed", name)));
         if (EntityUtility.isInGame()) {
            Sun.getInstance().getFileManager().writeFile("client");
         }
      }
   }

   public void clearTarget() {
      if (this.target.isEmpty()) {
         MessageUtility.info(Text.of(Localizator.translate("commands.target.empty")));
      } else {
         this.target.clear();
         MessageUtility.info(Text.of(Localizator.translate("commands.target.cleared")));
         if (EntityUtility.isInGame()) {
            Sun.getInstance().getFileManager().writeFile("client");
         }
      }
   }

   public List<String> listTargets() {
      return Collections.unmodifiableList(this.target);
   }

   public void listTarget() {
      if (this.target.isEmpty()) {
         MessageUtility.info(Text.of(Localizator.translate("commands.target.empty")));
      } else {
         for (int i = 0; i < this.target.size(); i++) {
            String name = this.target.get(i);
            MessageUtility.info(Text.of(String.format(Localizator.translate("commands.target.list_entry"), i + 1, name)));
         }
      }
   }

   @Nullable
   public Entity getBestTarget(TargetSettings settings) {
      if (mc.world == null) {
         return null;
      } else {
         Comparator<Entity> comparator = Comparator.<Entity, Boolean>comparing(e -> !this.target.contains(e.getName().getString()))
            .thenComparing(settings.getTargetComparator());
         return StreamSupport.<Entity>stream(mc.world.getEntities().spliterator(), false).filter(settings::isEntityValid).min(comparator).orElse(null);
      }
   }

   @Nullable
   public Entity getCrosshairTarget(TargetSettings settings, double range) {
      if (mc.player == null || mc.world == null) {
         return null;
      }
      Camera camera = mc.gameRenderer.getCamera();
      Vec3d start = camera.getPos();
      Vec3d look = mc.player.getRotationVec(1.0F);
      Vec3d end = start.add(look.multiply(range));

      RaycastContext context = new RaycastContext(
         start, end,
         RaycastContext.ShapeType.COLLIDER,
         RaycastContext.FluidHandling.NONE,
         mc.player
      );
      BlockHitResult blockHit = mc.world.raycast(context);
      double maxDistance = range;
      if (blockHit != null && blockHit.getType() != HitResult.Type.MISS) {
         maxDistance = start.distanceTo(blockHit.getPos());
      }

      Entity result = null;
      double closestDistance = maxDistance;

      for (Entity entity : mc.world.getEntities()) {
         if (!settings.isEntityValid(entity)) {
            continue;
         }
         Box box = entity.getBoundingBox().expand(0.15);
         Optional<Vec3d> hit = box.raycast(start, end);
         if (hit.isPresent()) {
            double distance = start.distanceTo(hit.get());
            if (distance < closestDistance) {
               closestDistance = distance;
               result = entity;
            }
         }
      }

      return result;
   }

   public void updateCrosshair(TargetSettings settings, double range) {
      this.currentTarget = this.getCrosshairTarget(settings, range);
   }

   public void reset() {
      this.currentTarget = null;
   }

   public boolean isTarget(String name) {
      if (name == null || name.isEmpty()) {
         return false;
      }
      for (String t : this.target) {
         if (t.equalsIgnoreCase(name)) {
            return true;
         }
      }
      return false;
   }

   public LivingEntity getLivingTarget() {
      return Sun.getInstance().getTargetManager().getCurrentTarget() instanceof LivingEntity target2 ? target2 : null;
   }

   @Nullable
   @Generated
   public Entity getCurrentTarget() {
      return this.currentTarget;
   }

   @Generated
   public List<String> getTarget() {
      return this.target;
   }
}