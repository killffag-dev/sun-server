package naryn.sun.systems.network;

public class ServerConfig {
    /**
     * Основной боевой адрес сервера и сайта на Railway:
     * https://sun-server-production.up.railway.app
     * Может быть переопределен параметром JVM: -Dsun.server.url=https://...
     */
    public static final String BASE_URL = normalize(System.getProperty("sun.server.url", "https://sun-server-production.up.railway.app"));
    
    /**
     * Базовый путь для API запросов
     */
    public static final String API_URL = BASE_URL + "/api";

    private static String normalize(String url) {
        if (url == null || url.isBlank()) return "https://sun-server-production.up.railway.app";
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
