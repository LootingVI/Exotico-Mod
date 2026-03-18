package de.flori.exotico.scanner;

import de.flori.exotico.api.ExoticAPI;
import de.flori.exotico.client.ExoticoHudOverlay;
import de.flori.exotico.config.ExoticoConfig;
import de.flori.exotico.data.ItemObject;
import de.flori.exotico.util.DiscordWebhook;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.scoreboard.Team;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PlayerScanner {




    private static final Set<String> scannedPlayers = new HashSet<>();
    public static final Set<String> playersWithExotics = new HashSet<>();


    private static final Map<String, Long> lastMsgTime = new HashMap<>();


    private static final long API_CALL_DELAY = 2500;
    private static long lastApiCallTime = 0;




    private static final Pattern LEVEL_PATTERN = Pattern.compile(
            "\\[\\s*[★✫✪✥]*\\s*(\\d{1,5})\\s*]");

    private static final Pattern LEVEL_FALLBACK = Pattern.compile("\\[(\\d{1,5})]");


    public static void init() {


        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            scannedPlayers.clear();
            playersWithExotics.clear();
            lastMsgTime.clear();
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.world == null || client.player == null)
                return;
            if (!ExoticoConfig.getInstance().autoScan)
                return;
            if (client.getNetworkHandler() == null)
                return;


            if (client.getNetworkHandler().getServerInfo() != null) {
                String ip = client.getNetworkHandler().getServerInfo().address.toLowerCase();
                if (!ip.contains("hypixel.net") && !ip.contains("localhost") && !ip.contains("127.0.0.1"))
                    return;
            }

            long now = System.currentTimeMillis();
            if (now - lastApiCallTime < API_CALL_DELAY)
                return;

            for (PlayerListEntry entry : client.getNetworkHandler().getPlayerList()) {
                String name = entry.getProfile().name();
                if (name == null || name.isEmpty())
                    continue;
                if (scannedPlayers.contains(name))
                    continue;


                int level = extractLevel(entry);

                scanPlayer(name, level, client);
                scannedPlayers.add(name);
                lastApiCallTime = now;
                break;
            }
        });
    }



    public static int extractLevel(PlayerListEntry entry) {

        Text displayName = entry.getDisplayName();
        if (displayName != null) {
            int level = levelFromText(displayName.getString());
            if (level >= 0)
                return level;
        }


        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != null && client.world.getScoreboard() != null) {
            Team team = client.world.getScoreboard()
                    .getScoreHolderTeam(entry.getProfile().name());
            if (team != null) {
                String decorated = Team.decorateName(team,
                        Text.literal(entry.getProfile().name())).getString();
                int level = levelFromText(decorated);
                if (level >= 0)
                    return level;


                String prefixSuffix = team.getPrefix().getString() + team.getSuffix().getString();
                int lvl = levelFromText(prefixSuffix);
                if (lvl >= 0)
                    return lvl;
            }
        }


        return levelFromText(entry.getProfile().name());
    }


    private static int levelFromText(String text) {
        if (text == null || text.isEmpty())
            return -1;
        String stripped = text.replaceAll("§.", "").trim();
        Matcher m = LEVEL_PATTERN.matcher(stripped);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException ignored) {
            }
        }
        m = LEVEL_FALLBACK.matcher(stripped);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException ignored) {
            }
        }
        return -1;
    }


    private static boolean isFairyItem(ItemObject item) {
        return item.name != null && item.name.toUpperCase().startsWith("FAIRY_");
    }


    private static void scanPlayer(String name, int level, MinecraftClient client) {
        ExoticAPI.getExoticItems(name).thenAccept(response -> {
            if (response == null || response.exoticItemsCombined == null)
                return;

            List<ItemObject> filtered = response.exoticItemsCombined.stream()
                    .filter(i -> !isFairyItem(i))
                    .collect(Collectors.toList());

            if (filtered.isEmpty())
                return;

            client.execute(() -> {
                playersWithExotics.add(name);
                ExoticoConfig cfg = ExoticoConfig.getInstance();

                if (cfg.enableSounds && client.player != null) {
                    client.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                }

                String itemsText = filtered.stream()
                        .map(i -> i.name)
                        .collect(Collectors.joining(", "));

                ExoticoHudOverlay.addExotic(name, itemsText);


                String levelStr = level >= 0 ? " §7[§6" + level + "§7]" : "";

                Text msg = Text.literal("[Exotico] ").formatted(Formatting.GOLD)
                        .append(Text.literal("Found " + filtered.size() + " exotic(s) on ")
                                .formatted(Formatting.YELLOW))
                        .append(Text.literal(name + levelStr).formatted(Formatting.AQUA)
                                .styled(style -> style
                                        .withClickEvent(new ClickEvent.SuggestCommand("/exotico scan " + name))
                                        .withHoverEvent(
                                                new HoverEvent.ShowText(Text.literal("Click to view details")))))
                        .append(Text.literal(" "))
                        .append(Text.literal("[Party]").formatted(Formatting.LIGHT_PURPLE)
                                .styled(style -> style
                                        .withClickEvent(new ClickEvent.RunCommand("/p " + name))
                                        .withHoverEvent(new HoverEvent.ShowText(Text.literal("Invite to Party")))))
                        .append(Text.literal(" "))
                        .append(Text.literal("[Trade]").formatted(Formatting.GREEN)
                                .styled(style -> style
                                        .withClickEvent(new ClickEvent.SuggestCommand("/trade " + name))
                                        .withHoverEvent(new HoverEvent.ShowText(Text.literal("Trade with Player")))))
                        .append(Text.literal(" "))
                        .append(Text.literal("[SkyCrypt]").formatted(Formatting.BLUE)
                                .styled(style -> style
                                        .withClickEvent(new ClickEvent.OpenUrl(
                                                URI.create("https://sky.shiiyu.moe/stats/" + name)))
                                        .withHoverEvent(new HoverEvent.ShowText(Text.literal("Open in Browser")))));

                if (client.player != null)
                    client.player.sendMessage(msg, false);


                if (cfg.enableAutoMsg && client.player != null) {
                    long now = System.currentTimeMillis();
                    long lastMsg = lastMsgTime.getOrDefault(name, 0L);

                    boolean levelOk = level < 0
                            || (level >= cfg.autoMsgMinLevel && level <= cfg.autoMsgMaxLevel);

                    boolean cooldownOk = (now - lastMsg) >= cfg.autoMsgCooldown;

                    if (levelOk && cooldownOk) {
                        String msgText = cfg.autoMsgTemplate.replace("{name}", name);
                        client.player.networkHandler.sendChatMessage("/msg " + name + " " + msgText);
                        lastMsgTime.put(name, now);
                    }
                }


                if (cfg.discordWebhook != null && !cfg.discordWebhook.isEmpty()) {
                    String items = filtered.stream()
                            .map(i -> i.name + " (" + i.color + ")")
                            .collect(Collectors.joining(", "));
                    DiscordWebhook.sendExoticAlert(name, items);
                }
            });
        }).exceptionally(e -> null);
    }


    public static void clearCache() {
        scannedPlayers.clear();
        playersWithExotics.clear();
        lastMsgTime.clear();
    }
}