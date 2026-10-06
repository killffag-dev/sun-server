package naryn.sun.systems.modules.modules.visuals.nameutility;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.scoreboard.ReadableScoreboardScore;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Извлекает кастомные привилегии, титулы, кланы и показатели здоровья (HP),
 * отправляемые сервером через арморстенды, скорборд или DisplayName.
 */
public final class ServerNameTagParser {

    private static final Pattern HEALTH_PATTERN = Pattern.compile(
        "(?i)(\\d+(?:\\.\\d+)?)\\s*(?:hp|❤|♥)|(?:❤|♥)\\s*(\\d+(?:\\.\\d+)?)|(\\d+)\\s*/\\s*(\\d+)"
    );

    private ServerNameTagParser() {
    }

    public record ParsedTagData(String cleanName, String upperLine, Integer health) {
    }

    public static ParsedTagData parse(PlayerEntity player, String cleanName, Text labelText) {
        Integer health = null;
        Set<String> customSegments = new LinkedHashSet<>();

        // 1. Поиск здоровья и титулов в прикрепленных или парящих серверных голограммах
        if (player != null) {
            List<String> serverHologramTexts = collectServerHologramTexts(player);
            for (String rawTag : serverHologramTexts) {
                String stripped = Formatting.strip(rawTag);
                if (stripped == null || stripped.isBlank()) {
                    continue;
                }

                // Проверяем, не является ли эта строка индикатором здоровья
                Matcher matcher = HEALTH_PATTERN.matcher(stripped);
                if (matcher.find()) {
                    if (health == null) {
                        health = parseHealthValue(matcher);
                    }
                    continue;
                }

                // Если это не ХП, значит это титул, клан или привилегия
                String cleanedTag = stripPlayerName(stripped, cleanName).trim();
                if (!cleanedTag.isEmpty() && !cleanedTag.equalsIgnoreCase(cleanName)) {
                    customSegments.add(cleanedTag);
                }
            }
        }

        // 2. Извлечение префикса/суффикса из команды скорборда (Team)
        if (player != null) {
            AbstractTeam abstractTeam = player.getScoreboardTeam();
            if (abstractTeam instanceof net.minecraft.scoreboard.Team team) {
                String prefix = Formatting.strip(team.getPrefix().getString());
                if (prefix != null && !prefix.isBlank()) {
                    customSegments.add(prefix.trim());
                }
                String suffix = Formatting.strip(team.getSuffix().getString());
                if (suffix != null && !suffix.isBlank()) {
                    customSegments.add(suffix.trim());
                }
            }
        }

        // 3. Извлечение кастомного префикса/суффикса из переданного сервером labelText
        if (labelText != null) {
            String fullLabel = Formatting.strip(labelText.getString());
            if (fullLabel != null && !fullLabel.isBlank() && cleanName != null) {
                String remaining = stripPlayerName(fullLabel, cleanName).trim();
                if (!remaining.isEmpty()) {
                    customSegments.add(remaining);
                }
            }
        }

        // 4. Определение здоровья, если серверные голограммы его не дали
        if (health == null && player != null) {
            health = resolvePlayerHealth(player);
        }

        // 5. Формирование верхней строки (привилегии/титулы)
        String upperLine = null;
        if (!customSegments.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (String seg : customSegments) {
                if (seg.isEmpty() || seg.equals("[]") || seg.equals("()")) {
                    continue;
                }
                if (!sb.isEmpty()) {
                    sb.append(" ");
                }
                sb.append(seg);
            }
            if (!sb.isEmpty()) {
                upperLine = sb.toString();
            }
        }

        return new ParsedTagData(cleanName, upperLine, health);
    }

    private static Integer resolvePlayerHealth(PlayerEntity player) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world != null) {
            Scoreboard scoreboard = mc.world.getScoreboard();
            ScoreboardObjective obj = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.BELOW_NAME);
            if (obj != null) {
                ReadableScoreboardScore score = scoreboard.getScore(player, obj);
                if (score != null) {
                    return score.getScore();
                }
            }
        }

        float entityHealth = player.getHealth() + player.getAbsorptionAmount();
        if (entityHealth > 0.0F) {
            return (int) Math.ceil(entityHealth);
        }

        return null;
    }

    private static Integer parseHealthValue(Matcher matcher) {
        try {
            if (matcher.group(1) != null) {
                return (int) Math.ceil(Double.parseDouble(matcher.group(1)));
            }
            if (matcher.group(2) != null) {
                return (int) Math.ceil(Double.parseDouble(matcher.group(2)));
            }
            if (matcher.group(3) != null) {
                return Integer.parseInt(matcher.group(3));
            }
        } catch (NumberFormatException ignored) {
        }
        return null;
    }

    private static String stripPlayerName(String text, String playerName) {
        if (text == null || playerName == null || playerName.isEmpty()) {
            return text != null ? text : "";
        }
        return text.replace(playerName, "").replaceAll("\\s{2,}", " ");
    }

    private static List<String> collectServerHologramTexts(PlayerEntity player) {
        List<String> texts = new ArrayList<>();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) {
            return texts;
        }

        // 1. Пассажиры игрока
        for (Entity passenger : player.getPassengerList()) {
            collectFromEntity(passenger, texts);
        }

        // 2. Сущности в непосредственной близости над головой игрока
        List<Entity> nearby = mc.world.getOtherEntities(player, player.getBoundingBox().expand(0.8, 3.2, 0.8));
        for (Entity entity : nearby) {
            collectFromEntity(entity, texts);
        }

        return texts;
    }

    private static void collectFromEntity(Entity entity, List<String> texts) {
        if (entity instanceof ArmorStandEntity armorStand) {
            if (armorStand.hasCustomName() && armorStand.getCustomName() != null) {
                texts.add(armorStand.getCustomName().getString());
            }
        } else if (entity instanceof DisplayEntity.TextDisplayEntity textDisplay) {
            if (textDisplay.hasCustomName() && textDisplay.getCustomName() != null) {
                texts.add(textDisplay.getCustomName().getString());
            }
        }
        for (Entity p : entity.getPassengerList()) {
            collectFromEntity(p, texts);
        }
    }
}
