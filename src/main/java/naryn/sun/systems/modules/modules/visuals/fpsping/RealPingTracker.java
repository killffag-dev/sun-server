package naryn.sun.systems.modules.modules.visuals.fpsping;

import naryn.sun.Sun;
import naryn.sun.systems.event.EventListener;
import naryn.sun.systems.event.impl.network.ReceivePacketEvent;
import naryn.sun.utility.interfaces.IMinecraft;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.network.packet.c2s.query.QueryPingC2SPacket;
import net.minecraft.network.packet.s2c.query.PingResultS2CPacket;
import net.minecraft.util.Util;

public class RealPingTracker implements IMinecraft {

    private long lastPingSent = 0L;
    private int lastMeasuredPing = -1;
    private long lastResultReceivedTime = 0L;
    private boolean subscribed = false;

    private final EventListener<ReceivePacketEvent> onReceivePacket = event -> {
        if (event.getPacket() instanceof PingResultS2CPacket pingResult) {
            long now = Util.getMeasuringTimeMs();
            long rtt = now - pingResult.startTime();
            if (rtt >= 0 && rtt < 10000L) {
                this.lastMeasuredPing = (int) rtt;
                this.lastResultReceivedTime = System.currentTimeMillis();
            }
        }
    };

    public void start() {
        if (!this.subscribed) {
            Sun.getInstance().getEventManager().subscribe(this);
            this.subscribed = true;
        }
    }

    public void stop() {
        if (this.subscribed) {
            Sun.getInstance().getEventManager().unsubscribe(this);
            this.subscribed = false;
        }
        this.lastMeasuredPing = -1;
    }

    public void update() {
        if (mc.getNetworkHandler() != null && !mc.isInSingleplayer()) {
            long now = System.currentTimeMillis();
            if (now - this.lastPingSent >= 500L) {
                this.lastPingSent = now;
                try {
                    mc.getNetworkHandler().sendPacket(new QueryPingC2SPacket(Util.getMeasuringTimeMs()));
                } catch (Exception ignored) {
                }
            }
        }
    }

    public int getPing() {
        if (mc.isInSingleplayer()) {
            return 0;
        }

        long now = System.currentTimeMillis();
        if (this.lastMeasuredPing >= 0 && (now - this.lastResultReceivedTime <= 3000L)) {
            return this.lastMeasuredPing;
        }

        if (mc.player != null && mc.getNetworkHandler() != null) {
            PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            if (entry != null && entry.getLatency() > 0) {
                return entry.getLatency();
            }
        }

        return Math.max(this.lastMeasuredPing, 0);
    }
}
