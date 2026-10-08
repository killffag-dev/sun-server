package naryn.sun.systems.announcement;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import naryn.sun.Sun;
import naryn.sun.systems.file.FileManager;
import naryn.sun.systems.network.ServerConfig;
import net.minecraft.util.Util;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

/**
 * Менеджер объявлений SUN Client:
 * 1. Обычные объявления (одноразовые): закрываются крестиком и больше не беспокоят игрока.
 * 2. Уведомления о новой версии: отображаются при каждом запуске / перезаходе в меню,
 *    пока игрок не обновится до актуальной версии мода.
 */
public final class AnnouncementManager {
    public static class AnnouncementData {
        public String id = "";
        public boolean active = false;
        public boolean isUpdate = false;
        public String version = "";
        public String type = "info";
        public String title = "";
        public String message = "";
        public String url = "";
    }

    private static AnnouncementData currentAnnouncement = null;
    private static final Set<String> dismissedIds = new HashSet<>();
    private static boolean sessionDismissed = false;
    private static boolean loadedFile = false;
    private static volatile boolean fetching = false;

    private AnnouncementManager() {}

    private static File getDismissedFile() {
        return new File(FileManager.DIRECTORY, "dismissed_announcements.json");
    }

    public static synchronized void loadDismissed() {
        if (loadedFile) return;
        loadedFile = true;
        try {
            File file = getDismissedFile();
            if (file.exists()) {
                try (FileReader reader = new FileReader(file)) {
                    JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                    if (json.has("dismissed")) {
                        JsonArray arr = json.getAsJsonArray("dismissed");
                        for (JsonElement el : arr) {
                            dismissedIds.add(el.getAsString());
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    public static synchronized void saveDismissed() {
        try {
            File file = getDismissedFile();
            JsonObject json = new JsonObject();
            JsonArray arr = new JsonArray();
            for (String id : dismissedIds) {
                arr.add(id);
            }
            json.add("dismissed", arr);
            try (FileWriter writer = new FileWriter(file)) {
                FileManager.GSON.toJson(json, writer);
            }
        } catch (Exception ignored) {}
    }

    public static void fetchAnnouncementAsync() {
        if (fetching) return;
        fetching = true;
        loadDismissed();

        Thread thread = new Thread(() -> {
            try {
                HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build();
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(ServerConfig.API_URL + "/announcement"))
                        .timeout(Duration.ofSeconds(5))
                        .GET()
                        .build();

                HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
                if (res.statusCode() == 200) {
                    JsonObject obj = JsonParser.parseString(res.body()).getAsJsonObject();
                    if (obj.has("active") && obj.get("active").getAsBoolean()) {
                        AnnouncementData data = new AnnouncementData();
                        data.active = true;
                        if (obj.has("id")) data.id = obj.get("id").getAsString();
                        if (obj.has("isUpdate")) data.isUpdate = obj.get("isUpdate").getAsBoolean();
                        if (obj.has("version")) data.version = obj.get("version").getAsString();
                        if (obj.has("type")) data.type = obj.get("type").getAsString();
                        if (obj.has("title")) data.title = obj.get("title").getAsString();
                        if (obj.has("message")) data.message = obj.get("message").getAsString();
                        if (obj.has("url")) data.url = obj.get("url").getAsString();

                        if (data.id == null || data.id.isBlank()) {
                            data.id = "ann_" + data.title + "_" + data.message;
                        }
                        currentAnnouncement = data;
                    } else {
                        currentAnnouncement = null;
                    }
                }
            } catch (Exception ignored) {
            } finally {
                fetching = false;
            }
        }, "Sun-Announcement-Fetcher");
        thread.setDaemon(true);
        thread.start();
    }

    public static AnnouncementData getCurrentAnnouncement() {
        return currentAnnouncement;
    }

    /**
     * Сравнивает две версии semver (например "2.1" и "2.0", или "2.0.1" и "2.0.0").
     * Возвращает true только если remoteVersion строго новее localVersion.
     * Некорректные/мусорные строки вроде "2/2/" отсеиваются и не вызывают ложных срабатываний.
     */
    public static boolean isRemoteVersionNewer(String remoteVersion, String localVersion) {
        if (remoteVersion == null || remoteVersion.isBlank() || localVersion == null || localVersion.isBlank()) {
            return false;
        }

        // Очищаем от возможных префиксов 'v' или 'v.'
        String cleanRemote = remoteVersion.trim().replaceAll("^[vV]", "");
        String cleanLocal = localVersion.trim().replaceAll("^[vV]", "");

        // Отделяем пре-релизы (-beta, -alpha и т.д.)
        String[] remoteMain = cleanRemote.split("-")[0].split("[^0-9]+");
        String[] localMain = cleanLocal.split("-")[0].split("[^0-9]+");

        if (remoteMain.length == 0 || remoteMain[0].isEmpty()) return false;
        if (localMain.length == 0 || localMain[0].isEmpty()) return false;

        int maxLen = Math.max(remoteMain.length, localMain.length);
        for (int i = 0; i < maxLen; i++) {
            int remoteNum = 0;
            int localNum = 0;
            if (i < remoteMain.length && !remoteMain[i].isEmpty()) {
                try {
                    remoteNum = Integer.parseInt(remoteMain[i]);
                } catch (NumberFormatException ignored) {}
            }
            if (i < localMain.length && !localMain[i].isEmpty()) {
                try {
                    localNum = Integer.parseInt(localMain[i]);
                } catch (NumberFormatException ignored) {}
            }
            if (remoteNum > localNum) return true;
            if (remoteNum < localNum) return false;
        }

        return false;
    }

    public static boolean hasVisibleAnnouncement() {
        if (currentAnnouncement == null || !currentAnnouncement.active) {
            return false;
        }

        // 1. Уведомление о новой версии
        if (currentAnnouncement.isUpdate) {
            // Показываем ТОЛЬКО если версия с сервера валидна и СТРОГО новее установленной версии клиента
            if (!isRemoteVersionNewer(currentAnnouncement.version, Sun.VERSION)) {
                return false;
            }
            // Показываем при каждом входе в меню, пока человек не обновится (или не закроет в рамках текущего запуска)
            return !sessionDismissed;
        }

        // 2. Обычное одноразовое объявление
        if (dismissedIds.contains(currentAnnouncement.id)) {
            return false;
        }

        return !sessionDismissed;
    }

    public static void dismissCurrent() {
        if (currentAnnouncement == null) return;
        sessionDismissed = true;

        // Если это обычное объявление (не обновление версии), сохраняем навсегда
        if (!currentAnnouncement.isUpdate) {
            dismissedIds.add(currentAnnouncement.id);
            saveDismissed();
        }
    }

    public static void openDownloadPage() {
        String url = ServerConfig.BASE_URL + "/#download";
        if (currentAnnouncement != null && currentAnnouncement.url != null && !currentAnnouncement.url.isBlank()) {
            if (currentAnnouncement.url.startsWith("http")) {
                url = currentAnnouncement.url;
            } else if (currentAnnouncement.url.startsWith("/")) {
                url = ServerConfig.BASE_URL + currentAnnouncement.url;
            }
        }
        try {
            Util.getOperatingSystem().open(URI.create(url));
        } catch (Exception ignored) {}
    }
}
