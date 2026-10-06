package naryn.sun.systems.event.impl.network;

import lombok.Generated;
import naryn.sun.systems.event.EventCancellable;
import net.minecraft.network.packet.Packet;

public class ReceivePacketEvent extends EventCancellable {
   public static final ReceivePacketEvent INSTANCE = new ReceivePacketEvent();

   private Packet<?> packet;

   public ReceivePacketEvent() {
   }

   @Generated
   public ReceivePacketEvent(Packet<?> packet) {
      this.set(packet);
   }

   public ReceivePacketEvent set(Packet<?> packet) {
      this.packet = packet;
      this.setCancelled(false);
      return this;
   }

   @Generated
   public Packet<?> getPacket() {
      return this.packet;
   }
}
