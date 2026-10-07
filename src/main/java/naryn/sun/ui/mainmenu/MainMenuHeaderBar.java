package naryn.sun.ui.mainmenu;

import naryn.sun.Sun;
import naryn.sun.framework.base.UIContext;
import naryn.sun.framework.msdf.Font;
import naryn.sun.framework.msdf.Fonts;
import naryn.sun.framework.objects.BorderRadius;
import naryn.sun.systems.license.LicenseService;
import naryn.sun.systems.license.EntitlementState;
import naryn.sun.systems.theme.ClientAppearance;
import naryn.sun.ui.menu.skin.MenuSkin;
import naryn.sun.utility.animation.base.Animation;
import naryn.sun.utility.animation.base.Easing;
import naryn.sun.utility.colors.ColorRGBA;
import naryn.sun.utility.colors.Colors;
import naryn.sun.utility.game.cursor.CursorType;
import naryn.sun.utility.game.cursor.CursorUtility;
import naryn.sun.utility.interfaces.IMinecraft;
import naryn.sun.utility.render.EntityHeadDrawUtility;
import naryn.sun.utility.sounds.ClientSoundManager;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Минималистичная компактная шапка главного меню SUN Client.
 * Аккуратный профиль игрока с чёткой отрисовкой скина, лаконичные кнопки действий
 * и плавные анимации наведения с парящими подсказками.
 */
public class MainMenuHeaderBar implements IMinecraft {
    private static final float HEADER_Y = 10.0F;
    private static final float HEADER_H = 20.0F;

    private final List<HeaderActionBtn> actionButtons = new ArrayList<>();
    private final Animation profileHoverAnim = new Animation(160L, Easing.CUBIC_OUT);
    private final Animation profileTooltipAnim = new Animation(180L, Easing.CUBIC_OUT);

    private float profileX, profileY, profileW, profileH;

    public void init(float screenWidth) {
        init(screenWidth, null, null);
    }

    public void init(float screenWidth, Runnable onVanillaClicked) {
        init(screenWidth, onVanillaClicked, null);
    }

    public void init(float screenWidth, Runnable onVanillaClicked, Runnable onFxClicked) {
        actionButtons.clear();

        profileX = 12.0F;
        profileY = HEADER_Y;
        profileH = HEADER_H;

        Font nameFont = Fonts.SEMIBOLD.getFont(6.2F);
        String username = mc != null && mc.getSession() != null ? mc.getSession().getUsername() : "Player";
        float nameW = nameFont.width(username);
        profileW = 2.0F + 16.0F + 5.0F + nameW + 5.0F + 3.5F + 5.0F;

        float spacing = 4.0F;

        // ПРАВАЯ СТОРОНА
        float rightX = screenWidth - 12.0F;
        
        float quitW = 20.0F;
        rightX -= quitW;
        actionButtons.add(new HeaderActionBtn(HeaderActionBtn.BtnType.EXIT, null, "Выход из игры",
                rightX, HEADER_Y, quitW, HEADER_H, new ColorRGBA(245, 75, 75, 255), () -> {
            if (mc != null) mc.scheduleStop();
        }));

        float vanillaW = 46.0F;
        rightX -= (vanillaW + spacing);
        actionButtons.add(new HeaderActionBtn(HeaderActionBtn.BtnType.TEXT, "Ванилла", "Оригинальное меню Minecraft",
                rightX, HEADER_Y, vanillaW, HEADER_H, null, () -> {
            if (onVanillaClicked != null) {
                onVanillaClicked.run();
            } else {
                SunMainMenuScreen.setVanillaRequested(true);
                if (mc != null) mc.setScreen(new TitleScreen());
            }
        }));

        // ЛЕВАЯ СТОРОНА
        float leftX = 12.0F;

        // Если не привязан, показываем кнопку Войти, иначе показывается профиль через renderProfile.
        // Для симуляции пока будем опираться на то, что если sparksBalance == 0, то типа не вошли? Нет.
        // Добавим кнопку "Войти", которая перекрывает/заменяет профиль, если Account не привязан.
        float loginW = 40.0F;
        boolean isLinked = naryn.sun.systems.sparks.SparksManager.isLinked();
        if (!isLinked) {
            actionButtons.add(new HeaderActionBtn(HeaderActionBtn.BtnType.TEXT, "Войти", "Привязать аккаунт",
                    leftX, HEADER_Y, loginW, HEADER_H, Colors.ACCENT, () -> {
                if (mc != null && mc.currentScreen != null) {
                    mc.setScreen(new LinkCodeScreen(mc.currentScreen));
                }
            }));
            leftX += (loginW + spacing);
        } else {
            leftX += (profileW + spacing);
        }

        // Спарксы
        int sparks = naryn.sun.systems.sparks.SparksManager.getSparks();
        String sparksText = String.valueOf(sparks);
        Font boldFont = Fonts.BOLD.getFont(5.8F);
        float sparksW = 7.0F + 4.0F + boldFont.width(sparksText) + 12.0F; // Иконка + отступ + текст + паддинг
        actionButtons.add(new HeaderActionBtn(HeaderActionBtn.BtnType.SPARKS, sparksText, "Баланс Спарксов (Нажмите, чтобы пополнить)",
                leftX, HEADER_Y, sparksW, HEADER_H, new ColorRGBA(253, 224, 71, 255),
                () -> openUrl(naryn.sun.systems.network.ServerConfig.BASE_URL + "/profile.html?tab=sparks")));
        naryn.sun.systems.sparks.SparksManager.fetchSparks();
        leftX += (sparksW + spacing);

        // Telegram (TG)
        float tgW = 20.0F;
        actionButtons.add(new HeaderActionBtn(HeaderActionBtn.BtnType.TEXT, "TG", "Telegram канал SUN",
                leftX, HEADER_Y, tgW, HEADER_H, new ColorRGBA(55, 175, 245, 255),
                () -> openUrl("https://t.me/SUN_Visual")));
        leftX += (tgW + spacing);

        // Фон
        naryn.sun.ui.mainmenu.background.MainMenuBackgroundRenderer bgRenderer =
                naryn.sun.ui.mainmenu.background.MainMenuBackgroundRenderer.getInstance();
        float bgBtnW = 32.0F;
        actionButtons.add(new HeaderActionBtn(HeaderActionBtn.BtnType.BACKGROUND, "Фон",
                "Настройка фона (" + bgRenderer.getMode().getNameRu() + ")",
                leftX, HEADER_Y, bgBtnW, HEADER_H, bgRenderer.getActiveColor1(), () -> {
            if (onFxClicked != null) {
                onFxClicked.run();
            } else {
                bgRenderer.cycleMode();
            }
            ClientSoundManager.getInstance().playButtonClick();
        }));
    }

    public void render(UIContext context, int mouseX, int mouseY, float delta) {
        MenuSkin skin = MenuSkin.current();
        boolean isFrost = ClientAppearance.isFrost();
        boolean isLinked = naryn.sun.systems.sparks.SparksManager.isLinked();

        // 1. Компактный профиль игрока
        if (isLinked) {
            renderProfile(context, skin, isFrost, mouseX, mouseY, delta);
        }

        // 2. Кнопки действий
        for (HeaderActionBtn btn : actionButtons) {
            btn.render(context, skin, mouseX, mouseY, delta);
        }

        // 3. Тултипы поверх всех элементов шапки
        if (isLinked) {
            renderProfileTooltip(context, skin, isFrost);
        }
        for (HeaderActionBtn btn : actionButtons) {
            btn.renderTooltip(context, skin);
        }
    }

    private void renderProfile(UIContext context, MenuSkin skin, boolean isFrost, int mouseX, int mouseY, float delta) {
        boolean hovered = mouseX >= profileX && mouseX <= profileX + profileW
                && mouseY >= profileY && mouseY <= profileY + profileH;
        if (hovered) {
            CursorUtility.set(CursorType.HAND);
        }

        float hover = profileHoverAnim.update(hovered ? 1.0F : 0.0F);
        profileTooltipAnim.update(hovered ? 1.0F : 0.0F);

        BorderRadius radius = BorderRadius.all(5.0F);

        float liftY = hover * -1.0F;
        float scale = 1.0F + hover * 0.02F;
        float cx = profileX + profileW / 2.0F;
        float cy = profileY + profileH / 2.0F;

        if (hover > 0.02F) {
            context.drawPerimeterShadow(profileX, profileY + liftY, profileW, profileH, 6.0F, radius,
                    new ColorRGBA(0, 0, 0, (int) (55 * hover)));
        }

        context.pushMatrix();
        context.getMatrices().translate(cx, cy + liftY, 0.0F);
        context.getMatrices().scale(scale, scale, 1.0F);
        context.getMatrices().translate(-cx, -cy, 0.0F);

        skin.renderCard(context, profileX, profileY, profileW, profileH, radius, false, 0.0F, hover, 1.0F);

        if (hover > 0.01F) {
            context.drawRoundedRect(profileX, profileY, profileW, profileH, radius, skin.hoverBackground(hover));
            context.drawRoundedBorder(profileX, profileY, profileW, profileH, 0.5F, radius,
                    new ColorRGBA(255, 255, 255, (int) (30 * hover)));
        }

        // Голова игрока (16x16 — чёткое пиксельное соотношение)
        float headX = profileX + 2.0F;
        float headY = profileY + 2.0F;
        float headSize = 16.0F;
        BorderRadius headRadius = BorderRadius.all(2.5F);

        // Тёмная подложка под голову
        context.drawRoundedRect(headX, headY, headSize, headSize, headRadius, new ColorRGBA(20, 24, 34, 180));

        try {
            SkinTextures skinTextures = mc.getSkinProvider().getSkinTextures(mc.getGameProfile());
            Identifier texture = skinTextures.texture();
            EntityHeadDrawUtility.drawPlayerHeadWithRoundedShader(context.getMatrices(), texture, headX, headY, headSize, headRadius, ColorRGBA.WHITE);
            EntityHeadDrawUtility.drawPlayerHatLayerWithRoundedShader(context.getMatrices(), texture, headX, headY, headSize, headRadius, ColorRGBA.WHITE);
        } catch (Exception e) {
            context.drawRoundedRect(headX, headY, headSize, headSize, headRadius, new ColorRGBA(40, 45, 60, 255));
        }

        // Аккуратная микро-обводка аватара
        context.drawRoundedBorder(headX, headY, headSize, headSize, 0.5F, headRadius, new ColorRGBA(255, 255, 255, 30));

        // Никнейм
        String username = naryn.sun.systems.sparks.SparksManager.isLinked() && !naryn.sun.systems.sparks.SparksManager.getAccountUsername().isEmpty()
                ? naryn.sun.systems.sparks.SparksManager.getAccountUsername()
                : mc.getSession().getUsername();
        Font nameFont = Fonts.SEMIBOLD.getFont(6.2F);
        float nameX = profileX + 23.0F;
        float nameY = profileY + (profileH - nameFont.height()) / 2.0F - 0.5F;
        context.drawText(nameFont, username, nameX, nameY, ColorRGBA.WHITE);

        // Индикатор активного статуса
        float dotX = nameX + nameFont.width(username) + 5.0F;
        float dotY = profileY + (profileH - 3.5F) / 2.0F;
        ColorRGBA dotColor = naryn.sun.systems.sparks.SparksManager.isLinked()
                ? new ColorRGBA(115, 240, 135, 255)
                : ("active".equals(LicenseService.getState().status())
                    ? new ColorRGBA(115, 240, 135, 255)
                    : (isFrost ? new ColorRGBA(140, 215, 255, 240) : new ColorRGBA(255, 195, 75, 240)));
        context.drawRoundedRect(dotX, dotY, 3.5F, 3.5F, BorderRadius.all(1.75F), dotColor);

        context.popMatrix();
    }

    private void renderProfileTooltip(UIContext context, MenuSkin skin, boolean isFrost) {
        float tipProg = profileTooltipAnim.update(isProfileHovered(context.getMouseX(), context.getMouseY()) ? 1.0F : 0.0F);
        if (tipProg > 0.02F) {
            Font subFont = Fonts.REGULAR.getFont(6.0F);
            String subText = naryn.sun.systems.sparks.SparksManager.isLinked()
                    ? "SUN: " + naryn.sun.systems.sparks.SparksManager.getAccountUsername() + " (#" + naryn.sun.systems.sparks.SparksManager.getAccountUid() + ")"
                    : ("active".equals(LicenseService.getState().status())
                        ? "Подписка: Активна (" + LicenseService.getState().tier() + ")"
                        : "Подписка: " + LicenseService.getState().status());

            float tipW = subFont.width(subText) + 18.0F;
            float tipH = 16.0F;
            float tipX = profileX;

            float slideOffset = (1.0F - tipProg) * 5.0F;
            float tipY = profileY + profileH + 5.0F + slideOffset;

            float tipScale = 0.94F + 0.06F * tipProg;
            float tipAlpha = MathHelper.clamp(tipProg, 0.0F, 1.0F);

            BorderRadius radius = BorderRadius.all(5.0F);

            context.pushMatrix();
            float tipCx = tipX + tipW / 2.0F;
            float tipCy = tipY + tipH / 2.0F;
            context.getMatrices().translate(tipCx, tipCy, 0.0F);
            context.getMatrices().scale(tipScale, tipScale, 1.0F);
            context.getMatrices().translate(-tipCx, -tipCy, 0.0F);

            context.drawPerimeterShadow(tipX, tipY, tipW, tipH, 6.0F, radius,
                    new ColorRGBA(0, 0, 0, (int) (80 * tipAlpha)));

            skin.renderPanelShell(context, tipX, tipY, tipW, tipH, radius, tipAlpha);
            context.drawRoundedBorder(tipX, tipY, tipW, tipH, 0.5F, radius,
                    new ColorRGBA(255, 255, 255, (int) (22 * tipAlpha)));

            ColorRGBA dotColor = naryn.sun.systems.sparks.SparksManager.isLinked()
                    ? new ColorRGBA(115, 240, 135, 255)
                    : ("active".equals(LicenseService.getState().status())
                        ? new ColorRGBA(115, 240, 135, 255)
                        : (isFrost ? new ColorRGBA(140, 215, 255, 240) : new ColorRGBA(255, 195, 75, 240)));

            context.drawRoundedRect(tipX + 6.0F, tipY + (tipH - 3.5F) / 2.0F, 3.5F, 3.5F, BorderRadius.all(1.75F),
                    dotColor.withAlpha((int) (255 * tipAlpha)));
            context.drawText(subFont, subText, tipX + 13.0F, tipY + (tipH - subFont.height()) / 2.0F - 0.5F,
                    ColorRGBA.WHITE.withAlpha((int) (255 * tipAlpha)));

            context.popMatrix();
        }
    }

    private boolean isProfileHovered(double mouseX, double mouseY) {
        return mouseX >= profileX && mouseX <= profileX + profileW
                && mouseY >= profileY && mouseY <= profileY + profileH;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;

        if (isProfileHovered(mouseX, mouseY)) {
            ClientSoundManager.getInstance().playButtonClick();
            openUrl(naryn.sun.systems.network.ServerConfig.BASE_URL + "/profile.html");
            return true;
        }

        for (HeaderActionBtn btn : actionButtons) {
            if (btn.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }

        return false;
    }

    private static void openUrl(String url) {
        try {
            Util.getOperatingSystem().open(URI.create(url));
        } catch (Exception e) {
            if (mc != null && mc.keyboard != null) {
                mc.keyboard.setClipboard(url);
            }
        }
    }
}
