package naryn.sun.systems.modules.modules.visuals.nameutility;

import com.mojang.blaze3d.systems.RenderSystem;
import naryn.sun.Sun;
import naryn.sun.framework.msdf.Font;
import naryn.sun.framework.msdf.Fonts;
import naryn.sun.framework.msdf.MsdfRenderer;
import naryn.sun.systems.modules.modules.visuals.NameUtility;
import naryn.sun.utility.mixins.EntityRenderStateAddition;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public final class NameTagRenderer {

    private NameTagRenderer() {
    }

    public static boolean render(
        NameUtility module,
        PlayerEntityRenderState state,
        Text text,
        MatrixStack matrices,
        VertexConsumerProvider vertexConsumers,
        int light,
        EntityRenderDispatcher dispatcher
    ) {
        Vec3d labelPos = state.nameLabelPos;
        if (labelPos == null) {
            return false;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc.textRenderer;

        Entity entity = ((EntityRenderStateAddition) state).sun$getEntity();
        PlayerEntity player = entity instanceof PlayerEntity p ? p : null;

        // 1. Имя игрока (чистый ник без серверных приписок)
        String rawName;
        if (player != null && player.getGameProfile() != null && !player.getGameProfile().getName().isEmpty()) {
            rawName = player.getGameProfile().getName();
        } else if (player != null && player.getName() != null) {
            rawName = player.getName().getString();
        } else if (state.name != null && !state.name.isEmpty()) {
            rawName = state.name;
        } else {
            rawName = text != null ? text.getString() : "Player";
        }

        rawName = module.patchName(rawName);

        // 2. Парсинг кастомных данных сервера (титулы/привилегии/кланы в верхнюю строку, ХП в нижнюю)
        ServerNameTagParser.ParsedTagData tagData = ServerNameTagParser.parse(player, rawName, text);
        String upperLine = tagData.upperLine();
        Integer health = tagData.health();

        // 3. Форматирование основной строки
        int nameColor = NameTagStyle.getNameColor(rawName, player);
        String displayNameStr = rawName;
        if (nameColor != NameTagStyle.DEFAULT_NAME_COLOR) {
            String stripped = Formatting.strip(rawName);
            if (stripped != null && !stripped.isEmpty()) {
                displayNameStr = stripped;
            }
        }

        // ХП игрока
        String hpStr = health != null ? health + " HP" : null;
        int hpColor = health != null ? (health > 15 ? 0xFF50DC78 : (health > 8 ? 0xFFFFD700 : 0xFFFF4646)) : 0xFF50DC78;

        // Пинг игрока
        boolean showPing = module.getShowPing().isEnabled();
        String pingStr = null;
        int pingColor = 0xFF50DC78;
        if (showPing && player != null) {
            int ping = getPlayerPing(player);
            if (ping >= 0) {
                pingStr = ping + "ms";
                pingColor = NameTagStyle.getPingColor(ping);
            }
        }

        // 4. Расчёт размеров в зависимости от шрифта и количества строк
        boolean isUi = module.isUiFont();
        Font msdfFont = isUi ? Fonts.MEDIUM.getFont(8.5F) : null;
        Font msdfUpperFont = isUi ? Fonts.MEDIUM.getFont(7.5F) : null;

        String cleanDisplayName = isUi ? Formatting.strip(displayNameStr) : displayNameStr;
        if (cleanDisplayName == null || cleanDisplayName.isEmpty()) {
            cleanDisplayName = displayNameStr;
        }

        float nameWidth = isUi ? msdfFont.width(cleanDisplayName) : tr.getWidth(displayNameStr);
        float sepWidth = isUi ? msdfFont.width(" | ") : tr.getWidth(" | ");
        float hpWidth = hpStr != null ? (isUi ? msdfFont.width(hpStr) : tr.getWidth(hpStr)) : 0.0F;
        float pingWidth = pingStr != null ? (isUi ? msdfFont.width(pingStr) : tr.getWidth(pingStr)) : 0.0F;

        float mainWidth = nameWidth
            + (hpStr != null ? sepWidth + hpWidth : 0.0F)
            + (pingStr != null ? sepWidth + pingWidth : 0.0F);

        float upperWidth = 0.0F;
        if (upperLine != null) {
            upperWidth = isUi ? msdfUpperFont.width(Formatting.strip(upperLine)) : tr.getWidth(upperLine);
        }

        float contentWidth = Math.max(upperWidth, mainWidth);
        float padX = 5.0F;
        float padY = isUi ? 3.0F : 3.5F;
        float lineSpacing = upperLine != null ? 2.5F : 0.0F;

        float mainHeight = isUi ? msdfFont.height() : 9.0F;
        float upperHeight = upperLine != null ? (isUi ? msdfUpperFont.height() : 8.0F) : 0.0F;

        float boxWidth = contentWidth + padX * 2.0F;
        float boxHeight = (upperLine != null ? upperHeight + lineSpacing : 0.0F) + mainHeight + padY * 2.0F;

        float boxX = -boxWidth / 2.0F;
        float boxY = -boxHeight;

        // 5. Позиционирование 3D матрицы
        matrices.push();
        matrices.translate(labelPos.x, labelPos.y + 0.5, labelPos.z);
        matrices.multiply(dispatcher.getRotation());

        float userScale = module.getScale().getCurrentValue();
        float baseScale = 0.025F * userScale;
        matrices.scale(baseScale, -baseScale, baseScale);
        Matrix4f matrix4f = matrices.peek().getPositionMatrix();

        boolean seeThrough = !state.sneaking;
        RenderLayer guiLayer = seeThrough ? RenderLayer.getGuiOverlay() : RenderLayer.getGui();
        boolean isGlass = module.isGlassStyle();

        // 6. Фон бокса — карточка в стиле материалов HUD (Стекло / Тёмный)
        VertexConsumer boxConsumer = vertexConsumers.getBuffer(guiLayer);
        int bg = NameTagStyle.getBgColor(isGlass);
        int borderTop = NameTagStyle.getBorderTopColor(isGlass);
        int border = NameTagStyle.getBorderColor(isGlass);

        drawQuad(boxConsumer, matrix4f, boxX, boxY, boxX + boxWidth, boxY + boxHeight, -0.01F, bg);
        // Верхняя линия акцента
        drawQuad(boxConsumer, matrix4f, boxX, boxY, boxX + boxWidth, boxY + 0.5F, 0.0F, borderTop);
        // Нижняя рамка
        drawQuad(boxConsumer, matrix4f, boxX, boxY + boxHeight - 0.5F, boxX + boxWidth, boxY + boxHeight, 0.0F, border);
        // Боковые рамки
        drawQuad(boxConsumer, matrix4f, boxX, boxY, boxX + 0.5F, boxY + boxHeight, 0.0F, border);
        drawQuad(boxConsumer, matrix4f, boxX + boxWidth - 0.5F, boxY, boxX + boxWidth, boxY + boxHeight, 0.0F, border);

        float upperY = boxY + padY;
        float upperX = -upperWidth / 2.0F;
        float mainY = upperLine != null ? upperY + upperHeight + lineSpacing : boxY + padY;
        float mainX = -mainWidth / 2.0F;

        // 7. Отрисовка текста
        int sepColor = 0x60FFFFFF;
        int upperColor = 0xFFFFAA00;

        if (isUi) {
            if (vertexConsumers instanceof VertexConsumerProvider.Immediate immediate) {
                immediate.draw();
            }

            if (seeThrough) {
                RenderSystem.disableDepthTest();
            } else {
                RenderSystem.enableDepthTest();
            }

            // Верхняя строка: кастомные привилегии/титулы
            if (upperLine != null) {
                MsdfRenderer.renderText(msdfUpperFont.getFont(), Formatting.strip(upperLine), msdfUpperFont.getSize(), upperColor, matrix4f, upperX, upperY, 0.0F);
            }

            // Основная строка: Ник
            float cursorX = mainX;
            MsdfRenderer.renderText(msdfFont.getFont(), cleanDisplayName, msdfFont.getSize(), nameColor, matrix4f, cursorX, mainY, 0.0F);
            cursorX += nameWidth;

            // ХП
            if (hpStr != null) {
                MsdfRenderer.renderText(msdfFont.getFont(), " | ", msdfFont.getSize(), sepColor, matrix4f, cursorX, mainY, 0.0F);
                cursorX += sepWidth;
                MsdfRenderer.renderText(msdfFont.getFont(), hpStr, msdfFont.getSize(), hpColor, matrix4f, cursorX, mainY, 0.0F);
                cursorX += hpWidth;
            }

            // Пинг
            if (pingStr != null) {
                MsdfRenderer.renderText(msdfFont.getFont(), " | ", msdfFont.getSize(), sepColor, matrix4f, cursorX, mainY, 0.0F);
                cursorX += sepWidth;
                MsdfRenderer.renderText(msdfFont.getFont(), pingStr, msdfFont.getSize(), pingColor, matrix4f, cursorX, mainY, 0.0F);
            }

            if (seeThrough) {
                RenderSystem.enableDepthTest();
            }
        } else {
            // Пиксельный ванильный шрифт
            TextRenderer.TextLayerType layerType = seeThrough ? TextRenderer.TextLayerType.SEE_THROUGH : TextRenderer.TextLayerType.NORMAL;

            // Верхняя строка
            if (upperLine != null) {
                Text upperText = Text.literal(upperLine);
                tr.draw(upperText, upperX, upperY, upperColor, false, matrix4f, vertexConsumers, layerType, 0, light);
                if (seeThrough) {
                    tr.draw(upperText, upperX, upperY, upperColor, false, matrix4f, vertexConsumers, TextRenderer.TextLayerType.NORMAL, 0, LightmapTextureManager.applyEmission(light, 2));
                }
            }

            // Основная строка: Ник
            float cursorX = mainX;
            Text cleanName = Text.literal(displayNameStr);
            tr.draw(cleanName, cursorX, mainY, nameColor, false, matrix4f, vertexConsumers, layerType, 0, light);
            if (seeThrough) {
                tr.draw(cleanName, cursorX, mainY, nameColor, false, matrix4f, vertexConsumers, TextRenderer.TextLayerType.NORMAL, 0, LightmapTextureManager.applyEmission(light, 2));
            }
            cursorX += nameWidth;

            // ХП
            if (hpStr != null) {
                Text sepText = Text.literal(" | ");
                tr.draw(sepText, cursorX, mainY, sepColor, false, matrix4f, vertexConsumers, layerType, 0, light);
                if (seeThrough) {
                    tr.draw(sepText, cursorX, mainY, sepColor, false, matrix4f, vertexConsumers, TextRenderer.TextLayerType.NORMAL, 0, LightmapTextureManager.applyEmission(light, 2));
                }
                cursorX += sepWidth;

                Text hpText = Text.literal(hpStr);
                tr.draw(hpText, cursorX, mainY, hpColor, false, matrix4f, vertexConsumers, layerType, 0, light);
                if (seeThrough) {
                    tr.draw(hpText, cursorX, mainY, hpColor, false, matrix4f, vertexConsumers, TextRenderer.TextLayerType.NORMAL, 0, LightmapTextureManager.applyEmission(light, 2));
                }
                cursorX += hpWidth;
            }

            // Пинг
            if (pingStr != null) {
                Text sepText = Text.literal(" | ");
                tr.draw(sepText, cursorX, mainY, sepColor, false, matrix4f, vertexConsumers, layerType, 0, light);
                if (seeThrough) {
                    tr.draw(sepText, cursorX, mainY, sepColor, false, matrix4f, vertexConsumers, TextRenderer.TextLayerType.NORMAL, 0, LightmapTextureManager.applyEmission(light, 2));
                }
                cursorX += sepWidth;

                Text pingText = Text.literal(pingStr);
                tr.draw(pingText, cursorX, mainY, pingColor, false, matrix4f, vertexConsumers, layerType, 0, light);
                if (seeThrough) {
                    tr.draw(pingText, cursorX, mainY, pingColor, false, matrix4f, vertexConsumers, TextRenderer.TextLayerType.NORMAL, 0, LightmapTextureManager.applyEmission(light, 2));
                }
            }
        }

        matrices.pop();
        return true;
    }

    private static int getPlayerPing(PlayerEntity player) {
        if (player == null) return -1;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getNetworkHandler() == null) return -1;
        PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(player.getUuid());
        if (entry == null && player.getGameProfile() != null) {
            entry = client.getNetworkHandler().getPlayerListEntry(player.getGameProfile().getName());
        }
        return entry != null ? entry.getLatency() : -1;
    }

    private static void drawQuad(VertexConsumer consumer, Matrix4f m, float x1, float y1, float x2, float y2, float z, int color) {
        consumer.vertex(m, x1, y1, z).color(color);
        consumer.vertex(m, x1, y2, z).color(color);
        consumer.vertex(m, x2, y2, z).color(color);
        consumer.vertex(m, x2, y1, z).color(color);
    }
}
