package naryn.sun.systems.modules.modules.utility.discord;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import naryn.sun.Sun;

public class DiscordIpc {

    private static final int OP_HANDSHAKE = 0;
    private static final int OP_FRAME = 1;
    private static final int OP_CLOSE = 2;

    private final String clientId;
    private RandomAccessFile winPipe;
    private SocketChannel unixChannel;
    private InputStream input;
    private OutputStream output;
    private boolean connected;

    public DiscordIpc(String clientId) {
        this.clientId = clientId;
    }

    public synchronized boolean connect() {
        if (this.connected) {
            return true;
        }

        try {
            boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
            if (isWindows) {
                for (int i = 0; i < 10; i++) {
                    try {
                        this.winPipe = new RandomAccessFile("\\\\.\\pipe\\discord-ipc-" + i, "rw");
                        break;
                    } catch (Throwable ignored) {
                    }
                }
                if (this.winPipe == null) {
                    return false;
                }
            } else {
                String runtimeDir = System.getenv("XDG_RUNTIME_DIR");
                String tmpDir = System.getenv("TMPDIR");
                String[] paths = {
                    runtimeDir != null ? runtimeDir + "/discord-ipc-0" : null,
                    tmpDir != null ? tmpDir + "/discord-ipc-0" : null,
                    "/tmp/discord-ipc-0"
                };
                for (String p : paths) {
                    if (p != null && new File(p).exists()) {
                        try {
                            this.unixChannel = SocketChannel.open(StandardProtocolFamily.UNIX);
                            this.unixChannel.connect(UnixDomainSocketAddress.of(p));
                            this.input = Channels.newInputStream(this.unixChannel);
                            this.output = Channels.newOutputStream(this.unixChannel);
                            break;
                        } catch (Throwable ignored) {
                        }
                    }
                }
                if (this.unixChannel == null) {
                    return false;
                }
            }

            // Handshake
            JsonObject handshake = new JsonObject();
            handshake.addProperty("v", 1);
            handshake.addProperty("client_id", this.clientId);
            writePacket(OP_HANDSHAKE, handshake.toString());

            // Read response
            readPacket();
            this.connected = true;
            return true;
        } catch (Throwable t) {
            close();
            return false;
        }
    }

    public synchronized void setActivity(String details, String state, long startTimestamp) {
        if (!this.connected && !connect()) {
            return;
        }

        try {
            JsonObject activity = new JsonObject();
            if (details != null && !details.isEmpty()) {
                activity.addProperty("details", details);
            }
            if (state != null && !state.isEmpty()) {
                activity.addProperty("state", state);
            }

            if (startTimestamp > 0) {
                JsonObject timestamps = new JsonObject();
                timestamps.addProperty("start", startTimestamp);
                activity.add("timestamps", timestamps);
            }

            JsonObject assets = new JsonObject();
            assets.addProperty("large_image", "sun");
            assets.addProperty("large_text", "SUN Client 2.0");
            activity.add("assets", assets);

            JsonObject args = new JsonObject();
            args.addProperty("pid", (int) ProcessHandle.current().pid());
            args.add("activity", activity);

            JsonObject root = new JsonObject();
            root.addProperty("cmd", "SET_ACTIVITY");
            root.add("args", args);
            root.addProperty("nonce", UUID.randomUUID().toString());

            writePacket(OP_FRAME, root.toString());
        } catch (Throwable t) {
            close();
        }
    }

    private void writePacket(int op, String json) throws Exception {
        byte[] payload = json.getBytes(StandardCharsets.UTF_8);
        ByteBuffer header = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
        header.putInt(op);
        header.putInt(payload.length);

        if (this.winPipe != null) {
            this.winPipe.write(header.array());
            this.winPipe.write(payload);
        } else if (this.output != null) {
            this.output.write(header.array());
            this.output.write(payload);
            this.output.flush();
        }
    }

    private void readPacket() throws Exception {
        byte[] headerBytes = new byte[8];
        if (this.winPipe != null) {
            this.winPipe.readFully(headerBytes);
        } else if (this.input != null) {
            int read = this.input.readNBytes(headerBytes, 0, 8);
            if (read < 8) return;
        } else {
            return;
        }

        ByteBuffer header = ByteBuffer.wrap(headerBytes).order(ByteOrder.LITTLE_ENDIAN);
        header.getInt(); // op
        int len = header.getInt();

        byte[] payload = new byte[len];
        if (this.winPipe != null) {
            this.winPipe.readFully(payload);
        } else if (this.input != null) {
            this.input.readNBytes(payload, 0, len);
        }
    }

    public synchronized void close() {
        this.connected = false;
        try {
            if (this.winPipe != null) {
                this.winPipe.close();
            }
        } catch (Throwable ignored) {
        }
        try {
            if (this.unixChannel != null) {
                this.unixChannel.close();
            }
        } catch (Throwable ignored) {
        }
        this.winPipe = null;
        this.unixChannel = null;
        this.input = null;
        this.output = null;
    }

    public boolean isConnected() {
        return this.connected;
    }
}
