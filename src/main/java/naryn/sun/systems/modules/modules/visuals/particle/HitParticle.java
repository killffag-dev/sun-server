package naryn.sun.systems.modules.modules.visuals.particle;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

public class HitParticle {
   private final HitParticleRenderer.Shape shape;
   private double prevX;
   private double prevY;
   private double prevZ;
   private double x;
   private double y;
   private double z;
   private double motionX;
   private double motionY;
   private double motionZ;
   private float rotation;
   private final float rotationSpeed;
   private final long spawnTime;
   private final long lifetime;
   private final float size;
   private final int id;
   private boolean onGround;

   public HitParticle(int id, HitParticleRenderer.Shape shape, double x, double y, double z,
                      double motionX, double motionY, double motionZ,
                      float rotation, float rotationSpeed, long lifetime, float size) {
      this.id = id;
      this.shape = shape;
      this.x = this.prevX = x;
      this.y = this.prevY = y;
      this.z = this.prevZ = z;
      this.motionX = motionX;
      this.motionY = motionY;
      this.motionZ = motionZ;
      this.rotation = rotation;
      this.rotationSpeed = rotationSpeed;
      this.spawnTime = System.currentTimeMillis();
      this.lifetime = lifetime;
      this.size = size;
      this.onGround = false;
   }

   public void tick(boolean gravity, boolean collision, ClientWorld world) {
      this.prevX = this.x;
      this.prevY = this.y;
      this.prevZ = this.z;

      if (this.onGround) {
         this.motionX *= 0.82;
         this.motionZ *= 0.82;
         this.motionY = 0.0;
      } else {
         if (gravity) {
            this.motionY -= 0.014;
         }
         this.motionX *= 0.95;
         this.motionY *= 0.96;
         this.motionZ *= 0.95;
      }

      double nextX = this.x + this.motionX;
      double nextY = this.y + this.motionY;
      double nextZ = this.z + this.motionZ;

      if (collision && world != null) {
         BlockPos checkFloor = BlockPos.ofFloored(nextX, nextY, nextZ);
         if (!world.getBlockState(checkFloor).isAir() && world.getBlockState(checkFloor).isSolidBlock(world, checkFloor)) {
            if (this.motionY < 0.0 && this.y >= checkFloor.getY() + 0.85) {
               nextY = checkFloor.getY() + 1.0;
               if (Math.abs(this.motionY) > 0.04) {
                  this.motionY = -this.motionY * 0.38;
               } else {
                  this.motionY = 0.0;
                  this.onGround = true;
               }
            } else {
               this.motionX = -this.motionX * 0.3;
               this.motionZ = -this.motionZ * 0.3;
            }
         }
      }

      this.x = nextX;
      this.y = nextY;
      this.z = nextZ;
      this.rotation += this.rotationSpeed;
   }

   public boolean isDead(long now) {
      return (now - this.spawnTime) >= this.lifetime;
   }

   public float getProgress(long now) {
      return MathHelper.clamp((float) (now - this.spawnTime) / (float) this.lifetime, 0.0F, 1.0F);
   }

   public double getInterpolatedX(float tickDelta) {
      return MathHelper.lerp(tickDelta, this.prevX, this.x);
   }

   public double getInterpolatedY(float tickDelta) {
      return MathHelper.lerp(tickDelta, this.prevY, this.y);
   }

   public double getInterpolatedZ(float tickDelta) {
      return MathHelper.lerp(tickDelta, this.prevZ, this.z);
   }

   public HitParticleRenderer.Shape getShape() {
      return this.shape;
   }

   public float getRotation() {
      return this.rotation;
   }

   public float getSize() {
      return this.size;
   }

   public int getId() {
      return this.id;
   }
}
