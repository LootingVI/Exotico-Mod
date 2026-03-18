package de.flori.exotico.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.flori.exotico.config.ExoticoConfig;
import de.flori.exotico.data.ColorCheckResult;
import de.flori.exotico.data.ExoticResponse;
import de.flori.exotico.data.PriceObject;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public class ExoticAPI {

    private static final String BASE_URL = "https://api.flori.tv/api";

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();

    private static final Gson GSON = new Gson();


    private static volatile long rateLimitedUntil = 0L;

    public static CompletableFuture<ExoticResponse> getExoticItems(String playerName) {
        String encoded = URLEncoder.encode(playerName, StandardCharsets.UTF_8);
        String url = BASE_URL + "/exotic-items?playername=" + encoded;
        return sendRequest(url, ExoticResponse.class);
    }

    public static CompletableFuture<PriceObject> getPrice(String itemId, String hex) {

        String cleanHex = hex.replace("#", "");
        String url = BASE_URL + "/price-check?item_id="
                + URLEncoder.encode(itemId.toUpperCase(), StandardCharsets.UTF_8)
                + "&hex=" + URLEncoder.encode(cleanHex, StandardCharsets.UTF_8);
        return sendRequest(url, PriceObject.class);
    }


    public static CompletableFuture<ColorCheckResult> checkColor(String hex, String itemId) {
        String cleanHex = hex.replace("#", "").toUpperCase();
        StringBuilder url = new StringBuilder(BASE_URL + "/check-color?hex=" + cleanHex);
        if (itemId != null && !itemId.isEmpty()) {
            url.append("&item_id=").append(URLEncoder.encode(itemId.toUpperCase(), StandardCharsets.UTF_8));
        }
        return sendRequest(url.toString(), ColorCheckResult.class);
    }


    public static boolean isRateLimited() {
        return System.currentTimeMillis() < rateLimitedUntil;
    }


    public static long rateLimitRemainingSeconds() {
        long remaining = rateLimitedUntil - System.currentTimeMillis();
        return remaining > 0 ? remaining / 1000 : 0;
    }

    private static <T> CompletableFuture<T> sendRequest(String url, Class<T> responseClass) {
        String apiKey = ExoticoConfig.getInstance().apiKey;
        if (apiKey == null || apiKey.isEmpty()) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("API Key not set! Use /exotico key <key> or /exotico settings"));
        }


        if (isRateLimited()) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("Rate limited! " + rateLimitRemainingSeconds() + "s remaining."));
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))

                .header("Authorization", "Bearer " + apiKey)
                .header("Accept", "application/json")
                .GET()
                .build();

        return CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    int status = response.statusCode();

                    if (status == 200) {
                        return GSON.fromJson(response.body(), responseClass);
                    }


                    String errorMsg = "API Error: " + status;
                    String errorCode = "";
                    try {
                        JsonObject err = JsonParser.parseString(response.body()).getAsJsonObject();
                        if (err.has("error"))
                            errorMsg = err.get("error").getAsString();
                        if (err.has("code"))
                            errorCode = " [" + err.get("code").getAsString() + "]";
                    } catch (Exception ignored) {
                    }

                    if (status == 429) {

                        long retryAfterSec = 15 * 60;
                        String retryHeader = response.headers().firstValue("Retry-After").orElse(null);
                        if (retryHeader != null) {
                            try {
                                retryAfterSec = Long.parseLong(retryHeader.trim());
                            } catch (Exception ignored) {
                            }
                        }
                        rateLimitedUntil = System.currentTimeMillis() + (retryAfterSec * 1000);
                        throw new RuntimeException("Rate limited! Retry in " + retryAfterSec + "s." + errorCode);
                    }

                    if (status == 401 || status == 403) {
                        throw new RuntimeException("Invalid API Key! (401/403)" + errorCode);
                    }

                    if (status == 404) {

                        return null;
                    }

                    throw new RuntimeException(errorMsg + errorCode);
                })
                .exceptionally(e -> {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    if (cause instanceof java.net.ConnectException) {
                        throw new RuntimeException("Could not connect to API server (" + BASE_URL + ")");
                    }
                    throw new RuntimeException(cause.getMessage());
                });
    }
}