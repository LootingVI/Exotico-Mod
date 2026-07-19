package de.flori.exotico.client;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

import com.mojang.brigadier.arguments.StringArgumentType;
import de.flori.exotico.api.ExoticAPI;
import de.flori.exotico.api.UserAPI;
import de.flori.exotico.config.ExoticoConfig;
import de.flori.exotico.scanner.PlayerScanner;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
//? if !mojmap {
import net.minecraft.client.network.PlayerListEntry;
//?} else {
/*import net.minecraft.client.multiplayer.PlayerInfo;
 *///?}
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class ExoticoCommands {

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {

            dispatcher.register(literal("exotico")

                    .then(literal("key")
                            .then(argument("apiKey", StringArgumentType.string())
                                    .executes(context -> {
                                        String key = StringArgumentType
                                                .getString(context,
                                                        "apiKey");
                                        ExoticoConfig.getInstance().apiKey = key;
                                        ExoticoConfig.save();

                                        context.getSource().sendFeedback(
                                                Text.literal("API Key updated!")
                                                        .formatted(Formatting.GREEN));
                                        return 1;
                                    })))

                    .then(literal("autoscan")
                            .executes(context -> {
                                boolean newVal = !ExoticoConfig.getInstance().autoScan;
                                ExoticoConfig.getInstance().autoScan = newVal;
                                ExoticoConfig.save();

                                context.getSource().sendFeedback(
                                        Text.literal("Auto-scan " + (newVal
                                                        ? "enabled"
                                                        : "disabled"))
                                                .formatted(newVal
                                                        ? Formatting.GREEN
                                                        : Formatting.RED));
                                return 1;
                            }))

                    .then(literal("scan")
                            .then(argument("player", StringArgumentType.word())
                                    .executes(context -> {
                                        String target = StringArgumentType
                                                .getString(context,
                                                        "player");

                                        context.getSource().sendFeedback(
                                                Text.literal("Scanning "
                                                                + target
                                                                + "...")
                                                        .formatted(Formatting.YELLOW));

                                        ExoticAPI.getExoticItems(target)
                                                .thenAccept(response -> {
                                                    if (response == null
                                                            || response.exoticItemsCombined == null)
                                                        return;

                                                    List<de.flori.exotico.data.ItemObject> filtered = response.exoticItemsCombined
                                                            .stream()
                                                            .filter(i -> i.name == null
                                                                    || !i.name.toUpperCase()
                                                                    .startsWith("FAIRY_"))
                                                            .collect(Collectors
                                                                    .toList());

                                                    if (!filtered.isEmpty()) {
                                                        context.getSource()
                                                                .sendFeedback(
                                                                        Text.literal("Found "
                                                                                        + filtered.size()
                                                                                        + " exotic item(s) on "
                                                                                        + target
                                                                                        + "!")
                                                                                .formatted(Formatting.AQUA));

                                                        filtered.forEach(
                                                                item -> {
                                                                    context.getSource()
                                                                            .sendFeedback(
                                                                                    Text.literal("- "
                                                                                                    + item.name
                                                                                                    + " ("
                                                                                                    + item.color
                                                                                                    + ")")
                                                                                            .formatted(Formatting.LIGHT_PURPLE));
                                                                });
                                                    } else {
                                                        context.getSource()
                                                                .sendFeedback(
                                                                        Text.literal("No exotic items found on "
                                                                                        + target
                                                                                        + ".")
                                                                                .formatted(Formatting.GRAY));
                                                    }
                                                }).exceptionally(e -> {
                                                    context.getSource()
                                                            .sendError(
                                                                    Text.literal("Error: "
                                                                            + e.getMessage()));
                                                    return null;
                                                });

                                        return 1;
                                    })))

                    .then(literal("webhook")
                            .then(argument("url", StringArgumentType.string())
                                    .executes(context -> {
                                        String url = StringArgumentType
                                                .getString(context,
                                                        "url");
                                        ExoticoConfig.getInstance().discordWebhook = url;
                                        ExoticoConfig.save();

                                        context.getSource().sendFeedback(
                                                Text.literal("Discord Webhook updated!")
                                                        .formatted(Formatting.GREEN));
                                        return 1;
                                    })))

                    .then(literal("toggle")

                            .then(literal("sounds")
                                    .executes(context -> {
                                        boolean newVal = !ExoticoConfig
                                                .getInstance().enableSounds;
                                        ExoticoConfig.getInstance().enableSounds = newVal;
                                        ExoticoConfig.save();

                                        context.getSource().sendFeedback(
                                                Text.literal("Sounds "
                                                                + (newVal ? "enabled"
                                                                : "disabled"))
                                                        .formatted(newVal
                                                                ? Formatting.GREEN
                                                                : Formatting.RED));
                                        return 1;
                                    }))

                            .then(literal("tooltips")
                                    .executes(context -> {
                                        boolean newVal = !ExoticoConfig
                                                .getInstance().enableTooltips;
                                        ExoticoConfig.getInstance().enableTooltips = newVal;
                                        ExoticoConfig.save();

                                        context.getSource().sendFeedback(
                                                Text.literal("Tooltips "
                                                                + (newVal ? "enabled"
                                                                : "disabled"))
                                                        .formatted(newVal
                                                                ? Formatting.GREEN
                                                                : Formatting.RED));
                                        return 1;
                                    }))

                            .then(literal("highlight")
                                    .executes(context -> {
                                        boolean newVal = !ExoticoConfig
                                                .getInstance().enablePlayerHighlight;
                                        ExoticoConfig.getInstance().enablePlayerHighlight = newVal;
                                        ExoticoConfig.save();

                                        context.getSource().sendFeedback(
                                                Text.literal("Highlight "
                                                                + (newVal ? "enabled"
                                                                : "disabled"))
                                                        .formatted(newVal
                                                                ? Formatting.GREEN
                                                                : Formatting.RED));
                                        return 1;
                                    })))

                    .then(literal("settings")
                            .executes(context -> {
                                MinecraftClient.getInstance().execute(() -> {
                                    //? if !guiScreenHolder {
                                    MinecraftClient.getInstance().setScreen(
                                            new ExoticoSettingsScreen(
                                                    MinecraftClient.getInstance().currentScreen));
                                    //?} else {
                                                                        /*Minecraft.getInstance().gui.setScreen(
                                                                                        new ExoticoSettingsScreen(
                                                                                                        Minecraft.getInstance().gui.screen()));*/
                                    //?}
                                });
                                return 1;
                            }))

                    .then(literal("level")
                            .then(argument("player", StringArgumentType.word())
                                    .executes(context -> {
                                        String target = StringArgumentType
                                                .getString(context,
                                                        "player");
                                        MinecraftClient client = MinecraftClient
                                                .getInstance();

                                        if (client.getNetworkHandler() != null) {
                                            //? if !mojmap {
                                            for (PlayerListEntry entry : client
                                                    //?} else {
                                                    /*for (PlayerInfo entry : client
                                                     *///?}
                                                    .getNetworkHandler()
                                                    .getPlayerList()) {
                                                if (target.equalsIgnoreCase(
                                                        entry.getProfile()
                                                                .name())) {
                                                    int level = PlayerScanner
                                                            .extractLevel(entry);

                                                    if (level >= 0) {
                                                        context.getSource()
                                                                .sendFeedback(
                                                                        Text.literal(target
                                                                                        + "'s Level: ")
                                                                                .formatted(Formatting.AQUA)
                                                                                .append(Text.literal(
                                                                                                String.valueOf(level))
                                                                                        .formatted(Formatting.GOLD)));
                                                    } else {
                                                        context.getSource()
                                                                .sendFeedback(
                                                                        Text.literal("Could not determine level for "
                                                                                        + target
                                                                                        + ". Are you on Hypixel?")
                                                                                .formatted(Formatting.RED));
                                                    }
                                                    return 1;
                                                }
                                            }
                                        }

                                        context.getSource().sendFeedback(
                                                Text.literal("Player "
                                                                + target
                                                                + " not found in the current lobby.")
                                                        .formatted(Formatting.RED));
                                        return 1;
                                    })))

                    .then(literal("clearcache")
                            .executes(context -> {
                                PlayerScanner.clearCache();
                                context.getSource().sendFeedback(
                                        Text.literal("Scanner cache cleared!")
                                                .formatted(Formatting.GREEN));
                                return 1;
                            }))

                    .then(literal("price")
                            .then(argument("itemId", StringArgumentType.string())
                                    .then(argument("hex",
                                            StringArgumentType.string())
                                            .executes(context -> {
                                                String itemId = StringArgumentType
                                                        .getString(context,
                                                                "itemId");
                                                String hex = StringArgumentType
                                                        .getString(context,
                                                                "hex");

                                                context.getSource()
                                                        .sendFeedback(
                                                                Text.literal(
                                                                                "Checking price for "
                                                                                        + itemId
                                                                                        + " (#"
                                                                                        + hex
                                                                                        + ")...")
                                                                        .formatted(Formatting.YELLOW));

                                                ExoticAPI.getPrice(
                                                                itemId,
                                                                hex)
                                                        .thenAccept(price -> {
                                                            context.getSource()
                                                                    .sendFeedback(
                                                                            Text.literal("Price for "
                                                                                            + itemId
                                                                                            + " (#"
                                                                                            + hex
                                                                                            + "): ")
                                                                                    .formatted(Formatting.AQUA)
                                                                                    .append(Text.literal(
                                                                                                    price.priceFormatted)
                                                                                            .formatted(Formatting.GOLD)));

                                                            if (price.metadata != null) {
                                                                context.getSource()
                                                                        .sendFeedback(
                                                                                Text.literal("Confidence: "
                                                                                                + price.metadata.confidence
                                                                                                + "% | Volatility: "
                                                                                                + price.metadata.volatility)
                                                                                        .formatted(Formatting.GRAY));
                                                            }
                                                        })
                                                        .exceptionally(e -> {
                                                            context.getSource()
                                                                    .sendError(
                                                                            Text.literal("Error: "
                                                                                    + e.getMessage()));
                                                            return null;
                                                        });

                                                return 1;
                                            }))))

                    .then(literal("setadminsecret")
                            .then(argument("secret", StringArgumentType.string())
                                    .executes(context -> {
                                        String secret = StringArgumentType
                                                .getString(context,
                                                        "secret");
                                        ExoticoConfig.getInstance().adminSecret = secret;
                                        ExoticoConfig.save();
                                        context.getSource().sendFeedback(
                                                Text.literal("Admin secret updated successfully! (Kept local only)")
                                                        .formatted(Formatting.GREEN));
                                        return 1;
                                    })))

                    .then(literal("social")
                            .executes(context -> {
                                MinecraftClient.getInstance().execute(() -> {
                                    //? if !guiScreenHolder {
                                    MinecraftClient.getInstance().setScreen(
                                            new de.flori.exotico.client.social.SocialScreen(
                                                    MinecraftClient.getInstance().currentScreen));
                                    //?} else {
                                                                        /*Minecraft.getInstance().gui.setScreen(
                                                                                        new de.flori.exotico.client.social.SocialScreen(
                                                                                                        Minecraft.getInstance().gui.screen()));*/
                                    //?}
                                });
                                return 1;
                            }))

                    .then(literal("admin")
                            .then(literal("setrank")
                                    .then(argument("player",
                                            StringArgumentType.word())
                                            .then(argument("rank",
                                                    StringArgumentType
                                                            .word())
                                                    .executes(context -> {
                                                        String target = StringArgumentType
                                                                .getString(context,
                                                                        "player");
                                                        String rank = StringArgumentType
                                                                .getString(context,
                                                                        "rank");

                                                        context.getSource()
                                                                .sendFeedback(
                                                                        Text.literal("Sending API request to update rank for "
                                                                                        + target
                                                                                        + " to "
                                                                                        + rank
                                                                                        + "...")
                                                                                .formatted(Formatting.YELLOW));

                                                        de.flori.exotico.api.UserAPI
                                                                .setRank(target, rank);
                                                        return 1;
                                                    }))))));
        });
    }
}