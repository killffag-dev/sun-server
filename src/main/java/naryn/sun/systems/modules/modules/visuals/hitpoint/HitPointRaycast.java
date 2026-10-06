package naryn.sun.systems.modules.modules.visuals.hitpoint;

import java.util.Optional;
import naryn.sun.Sun;
import naryn.sun.utility.rotations.RotationHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public final class HitPointRaycast {

   private HitPointRaycast() {}

   public static Vec3d calculateHit(Entity target, PlayerEntity player) {
      MinecraftClient mc = MinecraftClient.getInstance();
      HitResult crosshair = mc.crosshairTarget;

      Vec3d eyePos = player.getCameraPosVec(1.0F);
      Vec3d hitPos = null;

      // 1. Прямое попадание прицелом в хитбокс
      if (crosshair instanceof EntityHitResult ehr && ehr.getEntity() == target) {
         hitPos = ehr.getPos();
      }

      // Расширяем хитбокс по бокам, чтобы захватывать руки и плечи модели (печень/бока)
      double armExpandX = (target instanceof PlayerEntity || target.getWidth() >= 0.5F) ? 0.12 : 0.04;
      double armExpandZ = (target instanceof PlayerEntity) ? 0.06 : 0.04;
      Box box = target.getBoundingBox().expand(armExpandX, 0.02, armExpandZ);

      // 2. Рейкаст по лучу взгляда игрока (с учетом RotationHandler)
      if (hitPos == null) {
         RotationHandler rh = Sun.getInstance().getRotationHandler();
         Vec3d lookVec = rh.isIdling() ? player.getRotationVec(1.0F) : rh.getCurrentRotation().getRotationVector();
         Vec3d reachEnd = eyePos.add(lookVec.multiply(6.0));
         Optional<Vec3d> hitOpt = box.raycast(eyePos, reachEnd);
         if (hitOpt.isPresent()) {
            hitPos = hitOpt.get();
         }
      }

      // 3. Рейкаст в центр хитбокса цели
      if (hitPos == null) {
         Vec3d center = box.getCenter();
         Vec3d dir = center.subtract(eyePos).normalize();
         Optional<Vec3d> hitOpt = box.raycast(eyePos, eyePos.add(dir.multiply(6.0)));
         hitPos = hitOpt.orElse(center);
      }

      // Смещение наружу в сторону атакующего на 0.04 блока, чтобы эффект ложился поверх рук и брони
      Vec3d toAttacker = eyePos.subtract(hitPos).normalize();
      if (toAttacker.lengthSquared() > 1e-4) {
         hitPos = hitPos.add(toAttacker.multiply(0.04));
      }

      return hitPos;
   }
}
