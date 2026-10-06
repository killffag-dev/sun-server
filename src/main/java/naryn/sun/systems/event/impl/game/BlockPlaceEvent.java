package naryn.sun.systems.event.impl.game;

import lombok.Generated;
import naryn.sun.systems.event.Event;
import net.minecraft.util.hit.BlockHitResult;

public class BlockPlaceEvent extends Event {
   private final BlockHitResult hitResult;

   public BlockPlaceEvent(BlockHitResult hitResult) {
      this.hitResult = hitResult;
   }

   @Generated
   public BlockHitResult getHitResult() {
      return this.hitResult;
   }
}
