package de.flori.exotico.util;

import com.google.gson.JsonObject;
import de.flori.exotico.config.ExoticoConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class DiscordWebhook {
    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    public static void sendExoticAlert(String player, String items) {
        String url = ExoticoConfig.getInstance().discordWebhook;
        if (url == null || url.isEmpty()) return;

        JsonObject json = new JsonObject();
        json.addProperty("username", "Exotico Alerts");
        json.addProperty("content", "🚨 **Exotic Found!** 🚨\n**Player:** " + player + "\n**Items:** " + items);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                .build();

        CLIENT.sendAsync(request, HttpResponse.BodyHandlers.discarding());
    }
}