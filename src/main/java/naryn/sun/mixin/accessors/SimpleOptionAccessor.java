package naryn.sun.mixin.accessors;

import net.minecraft.client.option.SimpleOption;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SimpleOption.class)
public interface SimpleOptionAccessor<T> {
   @Accessor("text")
   Text sun$getText();

   @Accessor("value")
   T sun$getValue();

   @Accessor("value")
   void sun$setValue(T value);
}
