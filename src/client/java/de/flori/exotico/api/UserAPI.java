package de.flori.exotico.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class UserAPI {

    public static final String API_BASE = "https://mod.flori.tv/api";
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();


    public static final Map<String, String> userRanks = new HashMap<>();


    private static final Map<String, Identifier> capeTextures = new ConcurrentHashMap<>();

    private static final Set<String> capeFetchAttempted = ConcurrentHashMap.newKeySet();

    private static final Map<String, Long> capeUpdateTimestamps = new ConcurrentHashMap<>();

    private static long lastHeartbeatTime = 0;
    private static final long HEARTBEAT_INTERVAL = 60000;

    private static long lastFetchTime = 0;
    private static final long FETCH_INTERVAL = 30000;

    public static String myRank = "USER";

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.player == null)
                return;

            long now = System.currentTimeMillis();

            if (now - lastHeartbeatTime > HEARTBEAT_INTERVAL) {
                lastHeartbeatTime = now;
                sendHeartbeat(mc);
            }

            if (now - lastFetchTime > FETCH_INTERVAL) {
                lastFetchTime = now;
                fetchUsers();
            }
        });
    }

    private static void sendHeartbeat(MinecraftClient mc) {
        String uuid = mc.player.getUuidAsString();
        String name = mc.player.getGameProfile().name();

        JsonObject body = new JsonObject();
        body.addProperty("uuid", uuid);
        body.addProperty("name", name);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE + "/heartbeat"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        JsonObject obj = JsonParser.parseString(response.body()).getAsJsonObject();
                        if (obj.has("rank")) {
                            myRank = obj.get("rank").getAsString();
                        }
                    }
                }).exceptionally(e -> null);
    }

    private static void fetchUsers() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE + "/players"))
                .GET()
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        JsonObject obj = JsonParser.parseString(response.body()).getAsJsonObject();
                        if (obj.has("players")) {
                            JsonArray players = obj.getAsJsonArray("players");

                            Map<String, String> newRanks = new HashMap<>();
                            for (var el : players) {
                                JsonObject p = el.getAsJsonObject();
                                String pName = p.get("name").getAsString();
                                String rank = p.get("rank").getAsString();
                                newRanks.put(pName, rank);

                                if (p.has("cape_updated")) {
                                    long updated = p.get("cape_updated").getAsLong();
                                    String key = pName.toLowerCase();
                                    Long currentUpdated = capeUpdateTimestamps.get(key);
                                    if (currentUpdated != null && updated > currentUpdated) {
                                        capeUpdateTimestamps.put(key, updated);
                                        capeFetchAttempted.remove(key);
                                        capeTextures.remove(key);
                                    } else if (currentUpdated == null) {
                                        capeUpdateTimestamps.put(key, updated);
                                    }
                                }
                            }

                            synchronized (userRanks) {
                                userRanks.clear();
                                userRanks.putAll(newRanks);
                            }
                        }
                    }
                }).exceptionally(e -> null);
    }




    public static Identifier getCapeTexture(String name) {
        String key = name.toLowerCase();
        if (capeTextures.containsKey(key)) {
            return capeTextures.get(key);
        }
        if (!capeFetchAttempted.contains(key)) {
            capeFetchAttempted.add(key);
            fetchCapeAsync(name);
        }
        return null;
    }

    private static void fetchCapeAsync(String name) {
        String key = name.toLowerCase();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE + "/cape/" + key))
                .GET()
                .timeout(Duration.ofSeconds(5))
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        try {
                            JsonObject obj = JsonParser.parseString(response.body()).getAsJsonObject();
                            if (obj.has("cape")) {
                                byte[] imageBytes = Base64.getDecoder().decode(obj.get("cape").getAsString());

                                MinecraftClient.getInstance().execute(() -> {
                                    try {
                                        NativeImage image = NativeImage.read(new ByteArrayInputStream(imageBytes));

                                        Identifier oldId = capeTextures.get(key);
                                        if (oldId != null) {
                                            MinecraftClient.getInstance().getTextureManager().destroyTexture(oldId);
                                        }

                                        long unique = System.currentTimeMillis();
                                        NativeImageBackedTexture texture = new NativeImageBackedTexture(
                                                () -> "exotico:cape/" + key + "_" + unique, image);
                                        Identifier id = Identifier.of("exotico", "cape/" + key + "_" + unique);
                                        MinecraftClient.getInstance().getTextureManager().registerTexture(id, texture);
                                        capeTextures.put(key, id);
                                    } catch (Exception e) {
                                        System.err.println("Error registering cape texture for " + key + ":");
                                        e.printStackTrace();
                                    }
                                });
                            }
                        } catch (Exception e) {
                            System.err.println("JSON parse error for cape " + key + ":");
                            e.printStackTrace();
                        }
                    }

                }).exceptionally(e -> null);
    }


    public static void uploadCape(Path pngPath) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null)
            return;

        String uuid = mc.player.getUuidAsString();
        String name = mc.player.getGameProfile().name();

        CompletableFuture.runAsync(() -> {
            try {
                byte[] bytes = Files.readAllBytes(pngPath);
                String base64 = Base64.getEncoder().encodeToString(bytes);

                JsonObject body = new JsonObject();
                body.addProperty("uuid", uuid);
                body.addProperty("name", name);
                body.addProperty("cape", base64);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(API_BASE + "/cape/upload"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                        .timeout(Duration.ofSeconds(10))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                mc.execute(() -> {
                    if (mc.player == null)
                        return;
                    if (response.statusCode() == 200) {
                        mc.player.sendMessage(
                                net.minecraft.text.Text.literal("§a✔ Cape uploaded! (Rejoin to apply globally)"),
                                false);
                    } else {
                        JsonObject err = JsonParser.parseString(response.body()).getAsJsonObject();
                        String msg = err.has("error") ? err.get("error").getAsString() : "Unknown error";
                        mc.player.sendMessage(
                                net.minecraft.text.Text.literal("§cCape upload failed: " + msg), false);
                    }
                });
            } catch (Exception e) {
                mc.execute(() -> {
                    if (mc.player != null)
                        mc.player.sendMessage(
                                net.minecraft.text.Text.literal("§cError uploading cape: " + e.getMessage()), false);
                });
            }
        });
    }



    public static String getRankFormat(String name) {
        String rank;
        synchronized (userRanks) {
            rank = userRanks.getOrDefault(name, "USER");
        }
        return switch (rank.toUpperCase()) {
            case "DEV", "DEVELOPER" -> "§8[§cDEV§8] §c";
            case "ADMIN" -> "§8[§4ADMIN§8] §4";
            case "MOD" -> "§8[§2MOD§8] §2";
            case "VIP" -> "§8[§6VIP§8] §6";
            case "USER" -> "§8[§aExotico§8] §a";
            default -> "";
        };
    }

    public static boolean hasMod(String name) {
        synchronized (userRanks) {
            return userRanks.containsKey(name);
        }
    }

    public static String getRawRank(String name) {
        synchronized (userRanks) {
            return userRanks.getOrDefault(name, "USER").toUpperCase();
        }
    }

    public static void setRank(String name, String rank) {
        String secret = de.flori.exotico.config.ExoticoConfig.getInstance().adminSecret;
        if (secret == null || secret.isEmpty()) {
            MinecraftClient.getInstance().execute(() -> {
                if (MinecraftClient.getInstance().player != null) {
                    MinecraftClient.getInstance().player.sendMessage(
                            net.minecraft.text.Text.literal("§cError: No admin secret configured in settings!"), false);
                }
            });
            return;
        }

        JsonObject body = new JsonObject();
        body.addProperty("secret", secret);
        body.addProperty("name", name);
        body.addProperty("rank", rank.toUpperCase());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE + "/admin/setrank"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    MinecraftClient.getInstance().execute(() -> {
                        if (MinecraftClient.getInstance().player == null)
                            return;
                        if (response.statusCode() == 200) {
                            fetchUsers();
                            MinecraftClient.getInstance().player.sendMessage(
                                    net.minecraft.text.Text.literal("§aRank updated successfully!"), false);
                        } else {
                            MinecraftClient.getInstance().player.sendMessage(
                                    net.minecraft.text.Text.literal("§cFailed: " + response.body()), false);
                        }
                    });
                }).exceptionally(e -> {
                    MinecraftClient.getInstance().execute(() -> {
                        if (MinecraftClient.getInstance().player != null)
                            MinecraftClient.getInstance().player.sendMessage(
                                    net.minecraft.text.Text.literal("§cError connecting to API."), false);
                    });
                    return null;
                });
    }

    public static CompletableFuture<String> shareScreenshot(Path path) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null)
            return CompletableFuture.completedFuture(null);
        String uuid = mc.player.getUuidAsString();
        String name = mc.player.getGameProfile().name();

        return CompletableFuture.supplyAsync(() -> {
            try {
                byte[] bytes = Files.readAllBytes(path);
                String base64 = Base64.getEncoder().encodeToString(bytes);

                JsonObject body = new JsonObject();
                body.addProperty("uuid", uuid);
                body.addProperty("name", name);
                body.addProperty("image", base64);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(API_BASE + "/screenshot/upload"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                        .timeout(Duration.ofSeconds(15))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    JsonObject obj = JsonParser.parseString(response.body()).getAsJsonObject();
                    if (obj.has("id")) {
                        return API_BASE.replace("/api", "") + "/api/share/" + uuid + "/" + obj.get("id").getAsString();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        });
    }
}