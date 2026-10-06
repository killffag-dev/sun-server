package naryn.sun.systems.event.impl.game;

import lombok.Generated;
import naryn.sun.systems.event.EventCancellable;
import net.minecraft.util.math.BlockPos;

public class BlockBreakEvent extends EventCancellable {
   private final BlockPos blockPos;

   public BlockBreakEvent(BlockPos blockPos) {
      this.blockPos = blockPos;
   }

   @Generated
   public BlockPos getBlockPos() {
      return this.blockPos;
   }
}
