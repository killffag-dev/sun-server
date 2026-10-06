package naryn.sun.systems.event.impl.network;

import lombok.Generated;
import naryn.sun.systems.event.EventCancellable;
import net.minecraft.network.packet.Packet;

public class SendPacketEvent extends EventCancellable {
   public static final SendPacketEvent INSTANCE = new SendPacketEvent();

   private Packet<?> packet;

   public SendPacketEvent() {
   }

   @Generated
   public SendPacketEvent(Packet<?> packet) {
      this.set(packet);
   }

   public SendPacketEvent set(Packet<?> packet) {
      this.packet = packet;
      this.setCancelled(false);
      return this;
   }

   @Generated
   public Packet<?> getPacket() {
      return this.packet;
   }

   @Generated
   public void setPacket(Packet<?> packet) {
      this.packet = packet;
   }
}
