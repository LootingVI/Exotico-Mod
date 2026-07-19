package de.flori.exotico.client.social;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.flori.exotico.config.ExoticoConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletionStage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SocialManager {
    private static final String WS_URL = "wss://mod.flori.tv/api/social-ws";
    private static WebSocket webSocket;
    private static final Gson GSON = new Gson();

    public static class ChatMessage {
        public String id;
        public String sender;
        public String rank;
        public String content;
        public long timestamp;
        public boolean isAdminDeleted;
        public boolean isPrivate;
    }

    public static class FriendRequest {
        public String uuid;
        public String name;
    }

    public static class FriendInfo {
        public String uuid;
        public String name;
        public String rank;
        public boolean online;
        public String publicKey;
    }

    public static List<ChatMessage> globalMessages = new ArrayList<>();
    public static Map<String, List<ChatMessage>> privateMessages = new HashMap<>();
    public static List<String> onlineFriends = new ArrayList<>(); // Still maintain for quick lookups
    public static List<FriendRequest> pendingRequests = new ArrayList<>();
    public static List<FriendInfo> friendsList = new ArrayList<>();
    public static String selfRank = "USER";

    // sendMessage(Component, boolean) was split into sendSystemMessage/sendOverlayMessage on
    // Mojmap; every call site in this file passes false (chat, not overlay), so this covers all
    // of them in one place instead of repeating the branch at each call.
    private static void sendChat(net.minecraft.text.Text msg) {
        //? if !mojmap {
        MinecraftClient.getInstance().player.sendMessage(msg, false);
        //?} else {
        /*MinecraftClient.getInstance().player.sendSystemMessage(msg);*/
        //?}
    }

    public static void init() {
        connect();
    }

    public static void connect() {
        if (webSocket != null && !webSocket.isInputClosed()) {
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Reconnecting");
        }
        new Thread(() -> {
            try {
                java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
                webSocket = client.newWebSocketBuilder()
                        .buildAsync(java.net.URI.create(WS_URL), new java.net.http.WebSocket.Listener() {
                            @Override
                            public void onOpen(java.net.http.WebSocket ws) {
                                System.out.println("[Exotico Social] Connected to WebSocket.");
                                auth(ws);
                                ws.request(1);
                            }

                            private final StringBuilder messageBuffer = new StringBuilder();

                            @Override
                            public java.util.concurrent.CompletionStage<?> onText(java.net.http.WebSocket ws,
                                                                                  CharSequence data, boolean last) {
                                messageBuffer.append(data);
                                if (last) {
                                    handleMessage(messageBuffer.toString());
                                    messageBuffer.setLength(0);
                                }
                                ws.request(1);
                                return null;
                            }

                            @Override
                            public java.util.concurrent.CompletionStage<?> onClose(java.net.http.WebSocket ws,
                                                                                   int statusCode, String reason) {
                                System.out.println("[Exotico Social] Disconnected: " + reason);
                                return null;
                            }

                            @Override
                            public void onError(java.net.http.WebSocket ws, Throwable error) {
                                System.err.println("[Exotico Social] Deep WS Error: " + error.getClass().getName()
                                        + " - " + error.getMessage());
                                error.printStackTrace();
                            }
                        }).join();
            } catch (Exception e) {
                if (e instanceof java.util.concurrent.CompletionException
                        && e.getCause() instanceof java.net.http.WebSocketHandshakeException) {
                    java.net.http.WebSocketHandshakeException whe = (java.net.http.WebSocketHandshakeException) e
                            .getCause();
                    System.err.println(
                            "[Exotico Social] Handshake Failed with HTTP Status: " + whe.getResponse().statusCode());
                    System.err.println("[Exotico Social] Response Details: " + whe.getResponse().headers().map());
                } else if (e instanceof java.net.http.WebSocketHandshakeException) {
                    java.net.http.WebSocketHandshakeException whe = (java.net.http.WebSocketHandshakeException) e;
                    System.err.println(
                            "[Exotico Social] Handshake Failed with HTTP Status: " + whe.getResponse().statusCode());
                    System.err.println("[Exotico Social] Response Details: " + whe.getResponse().headers().map());
                } else {
                    System.err.println("[Exotico Social] Failed to connect: " + e.getMessage());
                }
            }
        }).start();
    }

    private static void auth(WebSocket ws) {
        MinecraftClient mc = MinecraftClient.getInstance();
        //? if !mojmap {
        if (mc.getSession() != null && mc.player != null) {
            //?} else {
            /*if (mc.getUser() != null && mc.player != null) {*/
            //?}
            String uuid = mc.player.getUuidAsString();
            String serverIp = "Unknown";
            if (mc.getNetworkHandler() != null && mc.getNetworkHandler().getServerInfo() != null) {
                serverIp = mc.getNetworkHandler().getServerInfo().address;
            }

            JsonObject authPayload = new JsonObject();
            authPayload.addProperty("type", "AUTH");
            authPayload.addProperty("uuid", uuid);
            authPayload.addProperty("name", mc.player.getName().getString());
            authPayload.addProperty("server", serverIp);
            authPayload.addProperty("publicKey", SocialCrypto.getMyPublicKeyBase64());

            ws.sendText(GSON.toJson(authPayload), true);
        }
    }

    private static void handleMessage(String json) {
        try {
            JsonObject obj = GSON.fromJson(json, JsonObject.class);
            String type = obj.get("type").getAsString();
            JsonObject payload = obj.has("payload") ? obj.getAsJsonObject("payload") : new JsonObject();

            MinecraftClient mc = MinecraftClient.getInstance();

            mc.execute(() -> {
                switch (type) {
                    case "FRIEND_ONLINE": {
                        if (mc.player != null) {
                            String fUuid = payload.get("uuid").getAsString();
                            if (!onlineFriends.contains(fUuid))
                                onlineFriends.add(fUuid);

                            // Update in friends list
                            for (FriendInfo fi : friendsList) {
                                if (fi.uuid.equals(fUuid)) {
                                    fi.online = true;
                                    if (payload.has("publicKey") && !payload.get("publicKey").isJsonNull()) {
                                        fi.publicKey = payload.get("publicKey").getAsString();
                                    }
                                }
                            }

                            String server = payload.has("server") ? payload.get("server").getAsString() : "Lobby";
                            String name = payload.has("name") ? payload.get("name").getAsString() : fUuid;
                            sendChat(
                                    Text.literal(
                                            "§a[Exotico Social] §7Your friend §b" + name + " §7just joined §e" + server));
                        }
                        break;
                    }

                    case "FRIEND_OFFLINE":
                        String offUuid = payload.get("uuid").getAsString();
                        onlineFriends.remove(offUuid);
                        for (FriendInfo fi : friendsList) {
                            if (fi.uuid.equals(offUuid))
                                fi.online = false;
                        }
                        break;

                    case "FRIEND_LIST":
                        JsonArray flist = payload.getAsJsonArray();
                        friendsList.clear();
                        onlineFriends.clear();
                        for (var el : flist) {
                            JsonObject fObj = el.getAsJsonObject();
                            FriendInfo fi = new FriendInfo();
                            fi.uuid = fObj.get("uuid").getAsString();
                            fi.name = fObj.get("name").getAsString();
                            fi.rank = fObj.get("rank").getAsString();
                            fi.online = fObj.get("online").getAsBoolean();
                            if (fObj.has("publicKey") && !fObj.get("publicKey").isJsonNull()) {
                                fi.publicKey = fObj.get("publicKey").getAsString();
                            }
                            friendsList.add(fi);
                            if (fi.online)
                                onlineFriends.add(fi.uuid);
                        }
                        break;

                    case "GLOBAL_CHAT_MSG":
                        ChatMessage msg = new ChatMessage();
                        msg.id = payload.get("id").getAsString();
                        msg.sender = payload.get("sender_name").getAsString();
                        msg.rank = payload.get("sender_rank").getAsString();
                        msg.content = payload.get("content").getAsString();
                        msg.isAdminDeleted = payload.get("deleted").getAsInt() == 1;
                        msg.isPrivate = false;
                        globalMessages.add(msg);
                        if (globalMessages.size() > 100)
                            globalMessages.remove(0);
                        break;

                    case "GLOBAL_CHAT_HISTORY": {
                        JsonArray hist = payload.getAsJsonArray();
                        globalMessages.clear();
                        for (var el : hist) {
                            JsonObject o = el.getAsJsonObject();
                            ChatMessage m = new ChatMessage();
                            m.id = o.get("id").getAsString();
                            m.sender = o.get("sender_name").getAsString();
                            m.rank = o.get("sender_rank").getAsString();
                            m.content = o.get("content").getAsString();
                            m.isAdminDeleted = o.has("deleted") && o.get("deleted").getAsInt() == 1;
                            m.isPrivate = false;
                            globalMessages.add(m);
                        }
                        break;
                    }

                    case "AUTH_SUCCESS": {
                        selfRank = payload.getAsJsonObject().get("rank").getAsString();
                        System.out.println("[Exotico Social] Authenticated with rank: " + selfRank);
                        break;
                    }

                    case "GLOBAL_CHAT_DELETE": {
                        String delId = payload.get("id").getAsString();
                        for (ChatMessage m : globalMessages) {
                            if (m.id != null && m.id.equals(delId)) {
                                m.isAdminDeleted = true;
                                m.content = "[Deleted by Admin]";
                            }
                        }
                        break;
                    }

                    case "FRIEND_REQUEST": {
                        if (mc.player != null) {
                            String fromUuid = payload.get("from").getAsString();
                            String fromName = payload.get("fromName").getAsString();

                            FriendRequest req = new FriendRequest();
                            req.uuid = fromUuid;
                            req.name = fromName;
                            pendingRequests.add(req);

                            sendChat(
                                    Text.literal("§a[Exotico Social] §7You received a friend request from §b" + fromName));
                        }
                        break;
                    }

                    case "FRIEND_ACCEPTED": {
                        if (mc.player != null) {
                            String byUuid = payload.get("by").getAsString();
                            String byName = payload.has("name") ? payload.get("name").getAsString() : byUuid;

                            FriendInfo fi = new FriendInfo();
                            fi.uuid = byUuid;
                            fi.name = byName;
                            fi.rank = payload.has("rank") ? payload.get("rank").getAsString() : "USER";
                            fi.online = payload.has("online") && payload.get("online").getAsBoolean();
                            if (payload.has("publicKey") && !payload.get("publicKey").isJsonNull()) {
                                fi.publicKey = payload.get("publicKey").getAsString();
                            }

                            // Add or Update
                            friendsList.removeIf(f -> f.uuid.equals(byUuid));
                            friendsList.add(fi);
                            if (fi.online && !onlineFriends.contains(byUuid))
                                onlineFriends.add(byUuid);

                            sendChat(Text.literal("§a[Exotico Social] §7Friend request §eaccepted§7!"));
                        }
                        break;
                    }

                    case "PRIVATE_MSG_RECEIVE": {
                        String sUuid = payload.get("sender").getAsString();
                        String encRecv = payload.get("content").getAsString();
                        String decRecv = SocialCrypto.decryptString(encRecv);

                        ChatMessage pmR = new ChatMessage();
                        pmR.id = payload.get("id").getAsString();
                        pmR.sender = sUuid;
                        for (FriendInfo fi : friendsList)
                            if (fi.uuid.equals(sUuid))
                                pmR.sender = fi.name;
                        pmR.content = decRecv;
                        pmR.timestamp = payload.get("timestamp").getAsLong();
                        pmR.isPrivate = true;

                        privateMessages.computeIfAbsent(sUuid, k -> new ArrayList<>()).add(pmR);
                        break;
                    }

                    case "PRIVATE_MSG_CONFIRM": {
                        String receiver = payload.get("receiver").getAsString();
                        String encSend = payload.get("content").getAsString();
                        String decSend = SocialCrypto.decryptString(encSend);

                        ChatMessage pmC = new ChatMessage();
                        pmC.id = payload.get("id").getAsString();
                        pmC.sender = mc.player != null ? mc.player.getName().getString() : "Me";
                        pmC.content = decSend;
                        pmC.timestamp = payload.get("timestamp").getAsLong();
                        pmC.isPrivate = true;

                        privateMessages.computeIfAbsent(receiver, k -> new ArrayList<>()).add(pmC);
                        break;
                    }

                    case "PRIVATE_MSG_HISTORY": {
                        String hOther = payload.get("otherUuid").getAsString();
                        JsonArray mH = payload.getAsJsonArray("messages");
                        List<ChatMessage> h = new ArrayList<>();
                        for (var el : mH) {
                            JsonObject o = el.getAsJsonObject();
                            ChatMessage cm = new ChatMessage();
                            cm.id = o.get("id").getAsString();
                            cm.sender = o.get("sender").getAsString();
                            String decH = SocialCrypto.decryptString(o.get("content").getAsString());
                            cm.content = decH;
                            cm.timestamp = o.get("timestamp").getAsLong();
                            h.add(cm);
                        }
                        privateMessages.put(hOther, h);
                        break;
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void sendFriendRequest(String name) {
        if (webSocket != null && !name.isEmpty()) {
            MinecraftClient mc = MinecraftClient.getInstance();
            JsonObject msg = new JsonObject();
            msg.addProperty("type", "FRIEND_ADD");
            msg.addProperty("targetName", name);
            msg.addProperty("myName", mc.player != null ? mc.player.getName().getString() : "Unknown");
            webSocket.sendText(GSON.toJson(msg), true);
        }
    }

    public static void acceptFriendRequest(String uuid) {
        if (webSocket != null && !uuid.isEmpty()) {
            JsonObject msg = new JsonObject();
            msg.addProperty("type", "FRIEND_ACCEPT");
            msg.addProperty("targetUuid", uuid);
            webSocket.sendText(GSON.toJson(msg), true);

            // Remove from pending
            pendingRequests.removeIf(r -> r.uuid.equals(uuid));
        }
    }

    public static void fetchPrivateHistory(String otherUuid) {
        if (webSocket != null) {
            JsonObject msg = new JsonObject();
            msg.addProperty("type", "PRIVATE_MSG_FETCH");
            msg.addProperty("otherUuid", otherUuid);
            webSocket.sendText(GSON.toJson(msg), true);
        }
    }

    public static void sendPrivateMessage(String targetUuid, String content) {
        if (webSocket != null && !content.isEmpty()) {
            JsonObject msg = new JsonObject();
            msg.addProperty("type", "PRIVATE_MSG_SEND");
            msg.addProperty("receiver", targetUuid);

            // Find friend's public key
            String friendPubBase64 = null;
            for (FriendInfo fi : friendsList) {
                if (fi.uuid.equals(targetUuid)) {
                    friendPubBase64 = fi.publicKey;
                    break;
                }
            }

            if (friendPubBase64 == null) {
                System.err.println("[Exotico Social] Cannot send E2E message: No public key for " + targetUuid);
                return;
            }

            // Encrypt for Receiver
            String encReceiver = SocialCrypto.encryptString(content, SocialCrypto.decodePublicKey(friendPubBase64));
            // Encrypt for OURSELF (so we can read it later in history)
            String encSender = SocialCrypto.encryptString(content,
                    SocialCrypto.decodePublicKey(SocialCrypto.getMyPublicKeyBase64()));

            msg.addProperty("content_receiver", encReceiver);
            msg.addProperty("content_sender", encSender);
            webSocket.sendText(GSON.toJson(msg), true);
        }
    }

    public static void deleteMessage(String msgId) {
        if (webSocket != null) {
            JsonObject msg = new JsonObject();
            msg.addProperty("type", "ADMIN_DELETE_MSG");
            msg.addProperty("msgId", msgId);
            webSocket.sendText(GSON.toJson(msg), true);
        }
    }

    public static void banUser(String username) {
        if (webSocket != null) {
            JsonObject msg = new JsonObject();
            msg.addProperty("type", "ADMIN_BAN_USER");
            msg.addProperty("username", username);
            webSocket.sendText(GSON.toJson(msg), true);
        }
    }

    public static void sendGlobalMessage(String content) {
        if (webSocket != null && !content.isEmpty()) {
            JsonObject msg = new JsonObject();
            msg.addProperty("type", "GLOBAL_CHAT_SEND");
            msg.addProperty("content", content);
            webSocket.sendText(GSON.toJson(msg), true);
        }
    }
}
