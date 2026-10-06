package naryn.sun.mixin.accessors;

import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Mouse.class)
public interface MouseAccessor {
   @Accessor("cursorDeltaX")
   double getCursorDeltaX();

   @Accessor("cursorDeltaX")
   void setCursorDeltaX(double cursorDeltaX);

   @Accessor("cursorDeltaY")
   double getCursorDeltaY();

   @Accessor("cursorDeltaY")
   void setCursorDeltaY(double cursorDeltaY);

   @Accessor("x")
   double getX();

   @Accessor("x")
   void setX(double x);

   @Accessor("y")
   double getY();

   @Accessor("y")
   void setY(double y);
}
