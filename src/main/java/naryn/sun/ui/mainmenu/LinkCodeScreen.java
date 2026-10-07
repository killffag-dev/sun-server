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
import naryn.sun.utility.colors.ColorRGBA;
import naryn.sun.utility.colors.Colors;
import naryn.sun.utility.game.cursor.CursorType;
import naryn.sun.utility.game.cursor.CursorUtility;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class LinkCodeScreen extends Screen {
    private final Screen parent;
    private String code = "Загрузка...";
    private String clientToken = "";
    private boolean loaded = false;
    private boolean linked = false;
    private String statusMessage = "Получение кода от сервера...";
    private boolean copied = false;
    private volatile boolean running = true;
    private final naryn.sun.utility.animation.base.Animation slideAnim =
            new naryn.sun.utility.animation.base.Animation(300L, naryn.sun.utility.animation.base.Easing.CUBIC_OUT);

    public LinkCodeScreen(Screen parent) {
        super(Text.literal("Привязка аккаунта SUN"));
        this.parent = parent;
        fetchCodeAndStartPolling();
    }

    private void fetchCodeAndStartPolling() {
        Thread worker = new Thread(() -> {
            try {
                HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build();
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(ServerConfig.API_URL + "/generate-link-code"))
                        .timeout(Duration.ofSeconds(5))
                        .GET()
                        .build();
                HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
                if (res.statusCode() == 200) {
                    JsonObject obj = JsonParser.parseString(res.body()).getAsJsonObject();
                    if (obj.has("code")) {
                        code = obj.get("code").getAsString();
                        if (obj.has("token")) {
                            clientToken = obj.get("token").getAsString();
                        }
                        loaded = true;
                        statusMessage = "Введите код в профиле на сайте";
                        startPollingLoop();
                        return;
                    }
                }
                code = "Ошибка";
                statusMessage = "Не удалось сгенерировать код";
            } catch (Exception e) {
                code = "Ошибка сети";
                statusMessage = "Проверьте подключение к серверу";
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    private void startPollingLoop() {
        Thread pollThread = new Thread(() -> {
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
            while (running && !linked) {
                try {
                    Thread.sleep(1500L);
                    if (!running) break;

                    String pollUrl = ServerConfig.API_URL + "/link-status?code=" + code;
                    if (clientToken != null && !clientToken.isEmpty()) {
                        pollUrl += "&token=" + clientToken;
                    }
                    HttpRequest req = HttpRequest.newBuilder()
                            .uri(URI.create(pollUrl))
                            .timeout(Duration.ofSeconds(4))
                            .GET()
                            .build();
                    HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
                    if (res.statusCode() == 200) {
                        JsonObject obj = JsonParser.parseString(res.body()).getAsJsonObject();
                        if (obj.has("linked") && obj.get("linked").getAsBoolean()) {
                            linked = true;
                            String username = obj.has("username") ? obj.get("username").getAsString() : "Игрок";
                            String key = obj.has("key") ? obj.get("key").getAsString() : "";
                            int uid = obj.has("uid") ? obj.get("uid").getAsInt() : 10;
                            int sparks = obj.has("sparks") ? obj.get("sparks").getAsInt() : 0;

                            SparksManager.saveAccount(key, username, uid, sparks, clientToken);
                            statusMessage = "✓ Успешно привязано к " + username + "!";

                            Thread.sleep(1200L);
                            if (this.client != null) {
                                this.client.execute(() -> this.client.setScreen(parent));
                            }
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
            this.client.setScreen(parent);
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

        float w = 240;
        float h = 145;
        float x = (this.width - w) / 2.0F;
        float targetY = this.height - h - 25.0F;
        float y = targetY + (1.0F - prog) * (h + 35.0F);

        BorderRadius radius = BorderRadius.all(6.0F);
        skin.renderCard(context, x, y, w, h, radius, false, 0, 1, 1);

        Font titleFont = Fonts.SEMIBOLD.getFont(10.0F);
        context.drawCenteredText(titleFont, "Привязка к сайту SUN", this.width / 2.0F, y + 18, ColorRGBA.WHITE);

        String displayCode = code;
        if (loaded && code.length() == 6) {
            displayCode = code.substring(0, 3) + " " + code.substring(3, 6);
        }

        Font codeFont = Fonts.BOLD.getFont(20.0F);
        ColorRGBA codeColor = linked ? new ColorRGBA(115, 240, 135, 255) : Colors.ACCENT;
        context.drawCenteredText(codeFont, displayCode, this.width / 2.0F, y + 46, codeColor);

        Font subFont = Fonts.REGULAR.getFont(6.2F);
        ColorRGBA subColor = linked ? new ColorRGBA(115, 240, 135, 255) : new ColorRGBA(170, 175, 185, 255);
        context.drawCenteredText(subFont, statusMessage, this.width / 2.0F, y + 76, subColor);

        float btnW = 96;
        float btnH = 22;
        float copyX = this.width / 2.0F - btnW - 6;
        float siteX = this.width / 2.0F + 6;
        float btnY = y + 104;

        String copyLabel = copied ? "Скопировано!" : "Копировать код";
        drawButton(context, copyX, btnY, btnW, btnH, copyLabel, mouseX, mouseY);
        drawButton(context, siteX, btnY, btnW, btnH, "Переход на сайт", mouseX, mouseY);
    }

    private void drawButton(UIContext context, float x, float y, float w, float h, String text, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
        if (hovered) {
            CursorUtility.set(CursorType.HAND);
            context.drawRoundedRect(x, y, w, h, BorderRadius.all(4.0F), new ColorRGBA(45, 50, 62, 255));
        } else {
            context.drawRoundedRect(x, y, w, h, BorderRadius.all(4.0F), new ColorRGBA(28, 31, 40, 255));
        }
        context.drawRoundedBorder(x, y, w, h, 1.0F, BorderRadius.all(4.0F), Colors.ACCENT);
        Font font = Fonts.REGULAR.getFont(6.5F);
        context.drawCenteredText(font, text, x + w / 2.0F, y + (h - font.height()) / 2.0F, ColorRGBA.WHITE);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float w = 240, h = 145;
        float sx = (this.width - w) / 2.0F;
        float sy = this.height - h - 25.0F;
        float btnW = 96, btnH = 22;
        float copyX = this.width / 2.0F - btnW - 6;
        float siteX = this.width / 2.0F + 6;
        float btnY = sy + 104;

        if (button == 0) {
            if (mouseX >= copyX && mouseX <= copyX + btnW && mouseY >= btnY && mouseY <= btnY + btnH) {
                if (loaded && this.client != null) {
                    this.client.keyboard.setClipboard(code);
                    copied = true;
                }
                return true;
            }
            if (mouseX >= siteX && mouseX <= siteX + btnW && mouseY >= btnY && mouseY <= btnY + btnH) {
                Util.getOperatingSystem().open(ServerConfig.BASE_URL + "/profile.html");
                return true;
            }
            if (mouseX < sx || mouseX > sx + w || mouseY < sy || mouseY > sy + h) {
                close();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
