package naryn.sun.utility.game.server;

import java.util.Arrays;
import naryn.sun.Sun;
import naryn.sun.systems.event.EventListener;
import naryn.sun.systems.event.impl.network.ReceivePacketEvent;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import net.minecraft.util.math.MathHelper;

public class TPSHandler {
   private final float[] tickRates = new float[20];
   private int nextIndex = 0;
   private long timeLastTimeUpdate;
   private final EventListener<ReceivePacketEvent> onReceivePacket = event -> {
      if (event.getPacket() instanceof WorldTimeUpdateS2CPacket) {
         if (this.timeLastTimeUpdate != -1L) {
            float timeElapsed = (float)(System.nanoTime() - this.timeLastTimeUpdate) / 1.0E9F;
            this.tickRates[this.nextIndex] = MathHelper.clamp(20.0F / timeElapsed, 0.0F, 20.0F);
            this.nextIndex = (this.nextIndex + 1) % this.tickRates.length;
         }

         this.timeLastTimeUpdate = System.nanoTime();
      }
   };

   public TPSHandler() {
      this.nextIndex = 0;
      this.timeLastTimeUpdate = -1L;
      Arrays.fill(this.tickRates, 0.0F);
      Sun.getInstance().getEventManager().subscribe(this);
   }

   public float getTPS() {
      float numTicks = 0.0F;
      float sumTickRates = 0.0F;

      for (float tickRate : this.tickRates) {
         if (tickRate > 0.0F) {
            sumTickRates += tickRate;
            numTicks++;
         }
      }

      return MathHelper.clamp(sumTickRates / numTicks, 0.0F, 20.0F);
   }
}
