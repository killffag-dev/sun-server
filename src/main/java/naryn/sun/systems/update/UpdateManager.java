package naryn.sun.systems.update;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import naryn.sun.Sun;
import naryn.sun.systems.notifications.NotificationType;
import net.minecraft.util.Util;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Менеджер проверки обновлений SUN Client.
 * Проверяет наличие новой версии на сервере и перенаправляет в официального Telegram-бота: @SUN_Visuals_bot
 */
public final class UpdateManager {
    public static final String DEFAULT_BOT_URL = "https://t.me/SUN_Visuals_bot?start=update";
    private static final String UPDATE_API_URL = naryn.sun.systems.network.ServerConfig.API_URL + "/version";

    private static boolean updateAvailable = false;
    private static String latestVersion = Sun.VERSION;
    private static String changelog = "";
    private static String botUrl = DEFAULT_BOT_URL;
    private static boolean checked = false;

    private UpdateManager() {}

    /**
     * Асинхронная проверка обновлений (не блокирует поток игры).
     */
    public static void checkUpdatesAsync() {
        CompletableFuture.runAsync(() -> {
            try {
                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(3))
                        .build();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(naryn.sun.systems.network.ServerConfig.API_URL + "/version"))
                        .timeout(Duration.ofSeconds(4))
                        .header("User-Agent", "SUN-Client/" + Sun.VERSION)
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                    if (json.has("version")) {
                        String remoteVersion = json.get("version").getAsString();
                        latestVersion = remoteVersion;

                        if (json.has("bot_url")) {
                            botUrl = json.get("bot_url").getAsString();
                        }
                        if (json.has("changelog")) {
                            changelog = json.get("changelog").getAsString();
                        }

                        // Сравниваем версии (показываем обновление только если на сервере версия СТРОГО новее)
                        if (naryn.sun.systems.announcement.AnnouncementManager.isRemoteVersionNewer(remoteVersion, Sun.VERSION)) {
                            updateAvailable = true;
                            Sun.LOGGER.info("[SUN-UPDATE] 🔔 Обнаружено обновление клиента: v{} (текущая: v{})", remoteVersion, Sun.VERSION);

                            // Показываем уведомление игроку через NotificationManager
                            if (Sun.getInstance().getNotificationManager() != null) {
                                Sun.getInstance().getNotificationManager().addNotificationOther(
                                        NotificationType.INFO,
                                        "SUN Update",
                                        "Доступна версия v" + remoteVersion + "! Нажмите TG для загрузки"
                                );
                            }
                        } else {
                            Sun.LOGGER.info("[SUN-UPDATE] ✅ У вас установлена актуальная версия SUN Client (v{})", Sun.VERSION);
                        }
                    }
                }
            } catch (Exception e) {
                // Если сервер оффлайн — не крашим игру, просто логируем дебаг
                Sun.LOGGER.debug("[SUN-UPDATE] Проверка обновлений недоступна: {}", e.getMessage());
            } finally {
                checked = true;
            }
        });
    }

    /**
     * Открывает официального Telegram-бота для мгновенного скачивания новой версии.
     */
    public static void openUpdateBot() {
        try {
            String target = naryn.sun.systems.network.ServerConfig.BASE_URL + "/#download";
            Util.getOperatingSystem().open(URI.create(target));
        } catch (Exception e) {
            Sun.LOGGER.error("[SUN-UPDATE] Ошибка открытия браузера: {}", e.getMessage());
        }
    }

    public static boolean isUpdateAvailable() {
        return updateAvailable;
    }

    public static String getLatestVersion() {
        return latestVersion;
    }

    public static String getChangelog() {
        return changelog;
    }

    public static String getBotUrl() {
        return botUrl;
    }

    public static boolean isChecked() {
        return checked;
    }
}
