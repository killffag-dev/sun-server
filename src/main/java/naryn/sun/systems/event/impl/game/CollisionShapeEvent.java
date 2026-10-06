package naryn.sun.systems.event.impl.game;

import lombok.Generated;
import naryn.sun.systems.event.EventCancellable;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;

public class CollisionShapeEvent extends EventCancellable {
   public static final CollisionShapeEvent INSTANCE = new CollisionShapeEvent();

   private BlockState state;
   private BlockPos pos;
   private VoxelShape shape;

   public CollisionShapeEvent() {
   }

   @Generated
   public CollisionShapeEvent(BlockState state, BlockPos pos, VoxelShape shape) {
      this.set(state, pos, shape);
   }

   public CollisionShapeEvent set(BlockState state, BlockPos pos, VoxelShape shape) {
      this.state = state;
      this.pos = pos;
      this.shape = shape;
      this.setCancelled(false);
      return this;
   }

   @Generated
   public BlockState getState() {
      return this.state;
   }

   @Generated
   public BlockPos getPos() {
      return this.pos;
   }

   @Generated
   public VoxelShape getShape() {
      return this.shape;
   }

   @Generated
   public void setShape(VoxelShape shape) {
      this.shape = shape;
   }
}
