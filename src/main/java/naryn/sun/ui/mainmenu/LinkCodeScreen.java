package naryn.sun.ui.mainmenu;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import naryn.sun.framework.base.UIContext;
import naryn.sun.framework.msdf.Font;
import naryn.sun.framework.msdf.Fonts;
import naryn.sun.framework.objects.BorderRadius;
import naryn.sun.systems.network.ServerConfig;
import naryn.sun.systems.sparks.SparksManager;
import naryn.sun.ui.menu.skin.MenuSkin;
import naryn.sun.utility.animation.base.Animation;
import naryn.sun.utility.animation.base.Easing;
import naryn.sun.utility.colors.ColorRGBA;
import naryn.sun.utility.colors.Colors;
import naryn.sun.utility.game.cursor.CursorType;
import naryn.sun.utility.game.cursor.CursorUtility;
import naryn.sun.utility.sounds.ClientSoundManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Экран мгновенной авторизации и сопряжения с сайтом SUN (One-Click Connect).
 * Генерирует токен, открывает браузер и ожидает подтверждения от пользователя.
 */
public class LinkCodeScreen extends Screen {
    private final Screen parent;
    private String code = "......";
    private String clientToken = "";
    private String linkUrl = "";
    private boolean loaded = false;
    private boolean linked = false;
    private String statusMessage = "Подготовка ссылки для входа...";
    private boolean copied = false;
    private volatile boolean running = true;
    private final Animation slideAnim = new Animation(280L, Easing.CUBIC_OUT);

    public LinkCodeScreen(Screen parent) {
        super(Text.literal("Привязка аккаунта SUN"));
        this.parent = parent;
        fetchCodeAndStartPolling();
    }

    public synchronized void fetchCodeAndStartPolling() {
        if (!running) return;
        this.loaded = false;
        this.copied = false;
        this.statusMessage = "Подготовка ссылки для входа...";

        Thread worker = new Thread(() -> {
            try {
                HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build();
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(ServerConfig.API_URL + "/auth/pair/start"))
                        .timeout(Duration.ofSeconds(5))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build();

                HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
                if (!running) return;

                if (res.statusCode() == 200) {
                    JsonObject obj = JsonParser.parseString(res.body()).getAsJsonObject();
                    if (obj.has("token")) {
                        clientToken = obj.get("token").getAsString();
                        if (obj.has("code")) code = obj.get("code").getAsString();
                        linkUrl = ServerConfig.BASE_URL + "/link?token=" + clientToken;
                        loaded = true;
                        statusMessage = "Ожидание подтверждения...";
                        openBrowser(linkUrl);
                        startPollingLoop();
                        return;
                    }
                }

                // Запасной fallback на старый эндпоинт
                HttpRequest fallbackReq = HttpRequest.newBuilder()
                        .uri(URI.create(ServerConfig.API_URL + "/generate-link-code"))
                        .timeout(Duration.ofSeconds(5))
                        .GET()
                        .build();
                HttpResponse<String> fallbackRes = client.send(fallbackReq, HttpResponse.BodyHandlers.ofString());
                if (!running) return;

                if (fallbackRes.statusCode() == 200) {
                    JsonObject obj = JsonParser.parseString(fallbackRes.body()).getAsJsonObject();
                    if (obj.has("code")) {
                        code = obj.get("code").getAsString();
                        if (obj.has("token")) clientToken = obj.get("token").getAsString();
                        linkUrl = ServerConfig.BASE_URL + "/link?token=" + clientToken;
                        loaded = true;
                        statusMessage = "Ожидание подтверждения...";
                        openBrowser(linkUrl);
                        startPollingLoop();
                        return;
                    }
                }

                if (running) {
                    statusMessage = "Не удалось сгенерировать ссылку";
                }
            } catch (Exception e) {
                if (running) {
                    statusMessage = "Ошибка подключения к серверу";
                }
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    private void openBrowser(String url) {
        if (url == null || url.isEmpty() || !running) return;
        try {
            Util.getOperatingSystem().open(URI.create(url));
        } catch (Exception e) {
            if (this.client != null) {
                this.client.execute(() -> {
                    if (this.client.keyboard != null) {
                        this.client.keyboard.setClipboard(url);
                    }
                });
            }
        }
    }

    private void startPollingLoop() {
        Thread pollThread = new Thread(() -> {
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
            while (running && !linked) {
                try {
                    Thread.sleep(1400L);
                    if (!running) break;

                    String pollUrl = ServerConfig.API_URL + "/link-status?token=" + clientToken;
                    if (code != null && !code.isEmpty()) {
                        pollUrl += "&code=" + code;
                    }
                    HttpRequest req = HttpRequest.newBuilder()
                            .uri(URI.create(pollUrl))
                            .timeout(Duration.ofSeconds(4))
                            .GET()
                            .build();
                    HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
                    if (!running) break;

                    if (res.statusCode() == 200) {
                        JsonObject obj = JsonParser.parseString(res.body()).getAsJsonObject();
                        if (obj.has("linked") && obj.get("linked").getAsBoolean()) {
                            linked = true;
                            String username = obj.has("username") ? obj.get("username").getAsString() : "Игрок";
                            String key = obj.has("key") ? obj.get("key").getAsString() : "";
                            int uid = obj.has("uid") ? obj.get("uid").getAsInt() : 10;
                            int sparks = obj.has("sparks") ? obj.get("sparks").getAsInt() : 0;

                            SparksManager.saveAccount(key, username, uid, sparks, clientToken);
                            statusMessage = "✓ Привязано к " + username + "!";
                            if (this.client != null) {
                                this.client.execute(() -> ClientSoundManager.getInstance().playButtonClick());
                            }

                            Thread.sleep(1100L);
                            if (running && this.client != null) {
                                this.client.execute(() -> this.client.setScreen(parent != null ? parent : new SunMainMenuScreen()));
                            }
                            break;
                        } else if (obj.has("error") && "not_found".equals(obj.get("error").getAsString())) {
                            statusMessage = "Время действия ссылки истекло";
                            loaded = false;
                            break;
                        }
                    }
                } catch (InterruptedException ie) {
                    break;
                } catch (Exception ignored) {
                }
            }
        });
        pollThread.setDaemon(true);
        pollThread.start();
    }

    @Override
    public void close() {
        this.running = false;
        if (this.client != null) {
            this.client.setScreen(parent != null ? parent : new SunMainMenuScreen());
        }
    }

    @Override
    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
        if (parent != null) {
            parent.render(drawContext, -1, -1, delta);
        }

        UIContext context = UIContext.of(drawContext, mouseX, mouseY, delta);
        MenuSkin skin = MenuSkin.current();

        float prog = slideAnim.update(1.0F);
        context.drawRect(0, 0, (float) this.width, (float) this.height, new ColorRGBA(0, 0, 0, (int) (160 * prog)));

        float w = 250.0F;
        float h = 152.0F;
        float x = (this.width - w) / 2.0F;
        float targetY = (this.height - h) / 2.0F;
        float y = targetY + (1.0F - prog) * 16.0F;

        BorderRadius radius = BorderRadius.all(6.0F);
        skin.renderCard(context, x, y, w, h, radius, false, 0, 1, 1);

        Font titleFont = Fonts.SEMIBOLD.getFont(10.0F);
        context.drawCenteredText(titleFont, "Подключение к сайту SUN", this.width / 2.0F, y + 16.0F, ColorRGBA.WHITE);

        if (linked) {
            Font boldFont = Fonts.BOLD.getFont(8.0F);
            context.drawCenteredText(boldFont, statusMessage, this.width / 2.0F, y + 44.0F, new ColorRGBA(115, 240, 135, 255));
        } else if (loaded) {
            Font subFont = Fonts.REGULAR.getFont(6.2F);
            context.drawCenteredText(subFont, "В браузере открыта страница входа", this.width / 2.0F, y + 36.0F, new ColorRGBA(220, 225, 235, 255));

            Font statusFont = Fonts.SEMIBOLD.getFont(7.0F);
            context.drawCenteredText(statusFont, statusMessage, this.width / 2.0F, y + 52.0F, Colors.ACCENT);
        } else {
            Font subFont = Fonts.REGULAR.getFont(6.5F);
            context.drawCenteredText(subFont, statusMessage, this.width / 2.0F, y + 44.0F, new ColorRGBA(170, 175, 185, 255));
        }

        // Кнопки
        float btnW = 110.0F, btnH = 21.0F;
        float btnY1 = y + 84.0F;
        float b1X = this.width / 2.0F - btnW - 5.0F;
        float b2X = this.width / 2.0F + 5.0F;

        if (loaded) {
            drawButton(context, b1X, btnY1, btnW, btnH, "Открыть снова", mouseX, mouseY);
            drawButton(context, b2X, btnY1, btnW, btnH, copied ? "Скопировано!" : "Копировать ссылку", mouseX, mouseY);
        } else {
            drawButton(context, b1X, btnY1, btnW, btnH, "Повторить", mouseX, mouseY);
            drawButton(context, b2X, btnY1, btnW, btnH, "Закрыть", mouseX, mouseY);
        }

        float cancelW = 84.0F, cancelH = 19.0F;
        float cancelX = this.width / 2.0F - cancelW / 2.0F;
        float cancelY = y + 115.0F;
        drawButton(context, cancelX, cancelY, cancelW, cancelH, "Отмена", mouseX, mouseY);
    }

    private void drawButton(UIContext context, float x, float y, float w, float h, String text, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
        if (hovered) {
            CursorUtility.set(CursorType.HAND);
            context.drawRoundedRect(x, y, w, h, BorderRadius.all(4.0F), new ColorRGBA(45, 50, 62, 255));
        } else {
            context.drawRoundedRect(x, y, w, h, BorderRadius.all(4.0F), new ColorRGBA(28, 31, 40, 255));
        }
        ColorRGBA borderCol = hovered ? Colors.ACCENT : new ColorRGBA(255, 255, 255, 30);
        context.drawRoundedBorder(x, y, w, h, 1.0F, BorderRadius.all(4.0F), borderCol);
        Font font = Fonts.REGULAR.getFont(6.3F);
        context.drawCenteredText(font, text, x + w / 2.0F, y + (h - font.height()) / 2.0F, ColorRGBA.WHITE);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float w = 250.0F;
        float h = 152.0F;
        float x = (this.width - w) / 2.0F;
        float y = (this.height - h) / 2.0F;

        float btnW = 110.0F, btnH = 21.0F;
        float btnY1 = y + 84.0F;
        float b1X = this.width / 2.0F - btnW - 5.0F;
        float b2X = this.width / 2.0F + 5.0F;

        float cancelW = 84.0F, cancelH = 19.0F;
        float cancelX = this.width / 2.0F - cancelW / 2.0F;
        float cancelY = y + 115.0F;

        if (button == 0) {
            if (mouseX >= b1X && mouseX <= b1X + btnW && mouseY >= btnY1 && mouseY <= btnY1 + btnH) {
                ClientSoundManager.getInstance().playButtonClick();
                if (loaded && !linkUrl.isEmpty()) {
                    openBrowser(linkUrl);
                } else if (!loaded) {
                    fetchCodeAndStartPolling();
                }
                return true;
            }
            if (mouseX >= b2X && mouseX <= b2X + btnW && mouseY >= btnY1 && mouseY <= btnY1 + btnH) {
                ClientSoundManager.getInstance().playButtonClick();
                if (loaded && !linkUrl.isEmpty() && this.client != null) {
                    this.client.keyboard.setClipboard(linkUrl);
                    copied = true;
                } else if (!loaded) {
                    close();
                }
                return true;
            }
            if (mouseX >= cancelX && mouseX <= cancelX + cancelW && mouseY >= cancelY && mouseY <= cancelY + cancelH) {
                ClientSoundManager.getInstance().playButtonClick();
                close();
                return true;
            }
            if (mouseX < x || mouseX > x + w || mouseY < y || mouseY > y + h) {
                close();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
