package naryn.sun.systems.sparks;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import naryn.sun.systems.file.FileManager;
import naryn.sun.systems.network.ServerConfig;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class SparksManager {
    private static int sparksBalance = 0;
    private static boolean linked = false;
    private static boolean loading = false;
    private static String accountUsername = "";
    private static String accountKey = "";
    private static int accountUid = 10;
    private static String clientToken = "";
    private static boolean initialized = false;

    private static File getAccountFile() {
        return new File(FileManager.DIRECTORY, "account.json");
    }

    public static synchronized void init() {
        if (initialized) return;
        initialized = true;
        try {
            File file = getAccountFile();
            if (file.exists()) {
                try (FileReader reader = new FileReader(file)) {
                    JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();
                    if (obj.has("linked") && obj.get("linked").getAsBoolean()) {
                        linked = true;
                        if (obj.has("username")) accountUsername = obj.get("username").getAsString();
                        if (obj.has("key")) accountKey = obj.get("key").getAsString();
                        if (obj.has("uid")) accountUid = obj.get("uid").getAsInt();
                        if (obj.has("token")) clientToken = obj.get("token").getAsString();
                        if (obj.has("sparks")) sparksBalance = obj.get("sparks").getAsInt();
                    }
                }
            }
        } catch (Exception ignored) {
        }
        if (linked) {
            fetchSparks();
        }
    }

    public static boolean isLinked() {
        if (!initialized) init();
        return linked;
    }

    public static void setLinked(boolean state) {
        linked = state;
    }

    public static int getSparks() {
        if (!initialized) init();
        return sparksBalance;
    }

    public static String getAccountUsername() {
        if (!initialized) init();
        return accountUsername;
    }

    public static int getAccountUid() {
        if (!initialized) init();
        return accountUid;
    }

    public static String getAccountKey() {
        if (!initialized) init();
        return accountKey;
    }

    public static String getClientToken() {
        if (!initialized) init();
        return clientToken;
    }

    public static synchronized void saveAccount(String key, String username, int uid, int sparks, String token) {
        linked = true;
        accountKey = key != null ? key : "";
        accountUsername = username != null ? username : "";
        accountUid = uid;
        sparksBalance = sparks;
        clientToken = token != null ? token : "";
        initialized = true;

        try {
            File dir = FileManager.DIRECTORY;
            if (!dir.exists()) dir.mkdirs();
            File file = getAccountFile();
            JsonObject obj = new JsonObject();
            obj.addProperty("linked", true);
            obj.addProperty("key", accountKey);
            obj.addProperty("username", accountUsername);
            obj.addProperty("uid", accountUid);
            obj.addProperty("sparks", sparksBalance);
            obj.addProperty("token", clientToken);
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(obj.toString());
            }
        } catch (Exception ignored) {
        }
    }

    public static synchronized void unlink() {
        linked = false;
        accountKey = "";
        accountUsername = "";
        accountUid = 10;
        sparksBalance = 0;
        clientToken = "";
        try {
            File file = getAccountFile();
            if (file.exists()) {
                file.delete();
            }
        } catch (Exception ignored) {
        }
    }

    public static void fetchSparks() {
        if (loading) return;
        loading = true;
        Thread t = new Thread(() -> {
            try {
                HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build();
                StringBuilder urlBuilder = new StringBuilder(ServerConfig.API_URL).append("/sparks-balance");
                boolean hasParam = false;
                if (accountKey != null && !accountKey.isEmpty()) {
                    urlBuilder.append("?key=").append(accountKey);
                    hasParam = true;
                }
                if (clientToken != null && !clientToken.isEmpty()) {
                    urlBuilder.append(hasParam ? "&token=" : "?token=").append(clientToken);
                }

                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(urlBuilder.toString()))
                        .timeout(Duration.ofSeconds(5))
                        .GET()
                        .build();
                HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
                if (res.statusCode() == 200) {
                    JsonObject obj = JsonParser.parseString(res.body()).getAsJsonObject();
                    if (obj.has("sparks")) {
                        sparksBalance = obj.get("sparks").getAsInt();
                    }
                    if (obj.has("username")) {
                        accountUsername = obj.get("username").getAsString();
                    }
                    if (obj.has("uid")) {
                        accountUid = obj.get("uid").getAsInt();
                    }
                    if (obj.has("linked")) {
                        linked = obj.get("linked").getAsBoolean();
                    }
                }
            } catch (Exception ignored) {
            } finally {
                loading = false;
            }
        });
        t.setDaemon(true);
        t.start();
    }
}
