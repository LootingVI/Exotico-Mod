package de.flori.exotico.client;

import de.flori.exotico.api.UserAPI;
import de.flori.exotico.config.ExoticoConfig;
import de.flori.exotico.data.ColorCheckResult;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
//? if !mojmap {
import net.minecraft.client.gui.Click;
//?} else {
/*import net.minecraft.client.input.MouseButtonEvent;
 *///?}
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
//? if !mojmap {
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
//?} else {
/*import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
*///?}
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class ExoticoSettingsScreen extends Screen {

    private final Screen parent;

    // Clamped to the window size in init() - a fixed 500x340 panel overflows the screen at
    // small window sizes / high GUI Scale, where the scaled width/height can drop well below that.
    private static final int PANEL_W_MAX = 500;
    private static final int PANEL_H_MAX = 340;
    private int PANEL_W = PANEL_W_MAX;
    private int PANEL_H = PANEL_H_MAX;
    private static final int HEADER_H = 32;
    private static final int TAB_H = 26;

    private static final int COL_BG = 0xFF0E0E18;
    private static final int COL_PANEL = 0xFF12121F;
    private static final int COL_SURFACE = 0xFF1A1A2E;
    private static final int COL_HOVER = 0xFF242438;
    private static final int COL_BORDER = 0xFF353550;
    private static final int COL_BORDER_ACT = 0xFF6B5BFF;
    private static final int COL_GOLD = 0xFFFFAA00;
    private static final int COL_TEXT = 0xFFE8E8FF;
    private static final int COL_TEXT_DIM = 0xFF808099;
    private static final int COL_GREEN = 0xFF44DD88;
    private static final int COL_RED = 0xFFFF5555;
    private static final int COL_ACCENT = 0xFF7C5EFF;
    private static final int COL_PURPLE = 0xFFAA55FF;
    private static final int COL_INPUT_BG = 0xFF0C0C1A;
    private static final int COL_INPUT_ACT = 0xFF181830;

    private int px, py;
    private int currentTab = 0;
    private int activeInput = -1;
    private int tickCounter = 0;
    private int collectionPage = 0;
    private int screenshotPage = 0;
    private double scrollOffset = 0;
    private double targetScroll = 0;

    private final Map<Path, Identifier> screenshotTextures = new HashMap<>();
    private final Set<Path> loadingThumbnails = new HashSet<>();
    private List<Path> cachedShots = null;
    private long lastShotCache = 0;
    private static final long SHOT_CACHE_MS = 3000;
    private static final int SHOTS_PER_PAGE = 6;
    private static final int ITEMS_PER_PAGE = 9;

    // FIX: Track which screenshot is currently being shared (index), not just a boolean.
    // This allows per-card state and prevents the global lock bug.
    private final Set<Integer> sharingIndices = new HashSet<>();

    private final String[] inputLabels = {
            "API Key", "Discord Webhook", "Cache Interval (ms)",
            "Auto-Message Template ({name})", "Min Level", "Max Level", "Msg Cooldown (ms)",
            "Hopper Delay (ms)"
    };
    private final String[] inputValues = { "", "", "", "", "", "", "", "" };
    private final int[] inputMaxLen = { 128, 256, 10, 200, 4, 4, 9, 10 };

    private final boolean[] inputMasked = { true, false, false, false, false, false, false, false };

    private final String[] inputPlaceholder = {
            "Paste your API key here…",
            "https://discord.com/api/webhooks/…",
            "e.g. 5000",
            "e.g. Hey {name}, nice exotics!",
            "e.g. 0",
            "e.g. 999",
            "e.g. 60000",
            "e.g. 4000"
    };

    private final String[] inputHints = {
            "§7DM §blinaaaaa_aaaaa §7on Discord to get your API key",
            "§7Leave empty to disable Discord alerts",
            "§7Minimum ms between two scan requests (default: 5000)",
            "§7Use {name} as placeholder for the player's name",
            "§7Only message players with level ≥ this (0 = everyone)",
            "§7Only message players with level ≤ this (999 = everyone)",
            "§7Minimum ms before the same player gets messaged again",
            "§7Time to wait before /hub"
    };

    private final String[] toggleLabels = {
            "Auto Scan", "Sounds", "Tooltips", "Player Highlight",
            "HUD Overlay", "Auto-Message", "Inv. Highlight", "Wavy Capes",
            "Auto Hopper", "AH Sniper"
    };
    private final boolean[] toggles = new boolean[10];

    private volatile String capePath = "";
    private String statusMsg = "";
    private int statusMsgColor = COL_TEXT_DIM;
    private final AtomicBoolean capeUploading = new AtomicBoolean(false);

    public ExoticoSettingsScreen(Screen parent) {
        super(Text.literal("Exotico Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        PANEL_W = Math.min(PANEL_W_MAX, Math.max(280, width - 20));
        PANEL_H = Math.min(PANEL_H_MAX, Math.max(200, height - 20));
        px = (width - PANEL_W) / 2;
        py = (height - PANEL_H) / 2;
        loadConfig();
    }

    private void loadConfig() {
        ExoticoConfig c = ExoticoConfig.getInstance();
        inputValues[0] = nvl(c.apiKey);
        inputValues[1] = nvl(c.discordWebhook);
        inputValues[2] = String.valueOf(c.scanCooldown);
        inputValues[3] = nvl(c.autoMsgTemplate);
        inputValues[4] = String.valueOf(c.autoMsgMinLevel);
        inputValues[5] = String.valueOf(c.autoMsgMaxLevel);
        inputValues[6] = String.valueOf(c.autoMsgCooldown);
        inputValues[7] = String.valueOf(c.autoHopperDelay);
        toggles[0] = c.autoScan;
        toggles[1] = c.enableSounds;
        toggles[2] = c.enableTooltips;
        toggles[3] = c.enablePlayerHighlight;
        toggles[4] = c.enableHudOverlay;
        toggles[5] = c.enableAutoMsg;
        toggles[6] = c.enableInventoryHighlight;
        toggles[7] = c.enableWavyCapes;
        toggles[8] = c.enableAutoHopper;
        toggles[9] = c.enableAhSniper;
    }

    private void saveConfig() {
        ExoticoConfig c = ExoticoConfig.getInstance();
        c.apiKey = inputValues[0].trim();
        c.discordWebhook = inputValues[1].trim();
        c.autoMsgTemplate = inputValues[3].trim();
        c.autoScan = toggles[0];
        c.enableSounds = toggles[1];
        c.enableTooltips = toggles[2];
        c.enablePlayerHighlight = toggles[3];
        c.enableHudOverlay = toggles[4];
        c.enableAutoMsg = toggles[5];
        c.enableInventoryHighlight = toggles[6];
        c.enableWavyCapes = toggles[7];
        c.enableAutoHopper = toggles[8];
        c.enableAhSniper = toggles[9];
        try {
            c.scanCooldown = Long.parseLong(inputValues[2].trim());
        } catch (Exception ignored) {
        }
        try {
            c.autoMsgMinLevel = Integer.parseInt(inputValues[4].trim());
        } catch (Exception ignored) {
        }
        try {
            c.autoMsgMaxLevel = Integer.parseInt(inputValues[5].trim());
        } catch (Exception ignored) {
        }
        try {
            c.autoMsgCooldown = Long.parseLong(inputValues[6].trim());
        } catch (Exception ignored) {
        }
        try {
            c.autoHopperDelay = Long.parseLong(inputValues[7].trim());
        } catch (Exception ignored) {
        }
        ExoticoConfig.save();
    }

    private String nvl(String s) {
        return s != null ? s : "";
    }

    private float maxScroll() {
        return switch (currentTab) {
            case 0 -> 280f;
            case 1 -> toggles[5] ? 300f : 80f;
            default -> 0f;
        };
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double hAmount, double vAmount) {
        float max = maxScroll();
        if (max <= 0)
            return false;
        targetScroll = Math.clamp(targetScroll - vAmount * 20, 0, max);
        return true;
    }

    //? if !mojmap {
    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        //?} else {
    /*@Override
    public void extractRenderState(DrawContext ctx, int mouseX, int mouseY, float delta) {
    *///?}
        tickCounter++;

        float max = maxScroll();
        if (targetScroll > max)
            targetScroll = max;
        if (targetScroll < 0)
            targetScroll = 0;
        scrollOffset += (targetScroll - scrollOffset) * 0.25f;

        ctx.fill(0, 0, width, height, 0xBB080814);

        fillRect(ctx, px, py, PANEL_W, PANEL_H, COL_PANEL);
        drawBorder(ctx, px, py, PANEL_W, PANEL_H, COL_BORDER);

        fillRect(ctx, px, py, PANEL_W, HEADER_H, COL_BG);

        fillRect(ctx, px, py, 3, HEADER_H, COL_GOLD);
        //? if !mojmap {
        ctx.drawTextWithShadow(
                //?} else {
                /*ctx.text(*/
                //?}
                textRenderer, "✦ EXOTICO  Settings", px + 10, py + 12, COL_GOLD);

        int clX = px + PANEL_W - 26, clY = py + 7;
        boolean clHov = hover(mouseX, mouseY, clX, clY, 18, 18);
        fillRect(ctx, clX, clY, 18, 18, clHov ? 0x88FF3344 : 0x44FF3344);
        drawBorder(ctx, clX, clY, 18, 18, clHov ? COL_RED : 0xFF993333);
        //? if !mojmap {
        ctx.drawCenteredTextWithShadow(
                //?} else {
                /*ctx.centeredText(*/
                //?}
                textRenderer, "×", clX + 9, clY + 5, COL_TEXT);

        String[] tabs = { "General", "Notify", "Cape", "Collection", "Screenshots", "About" };
        int tabW = PANEL_W / tabs.length;
        int tabY = py + HEADER_H;
        for (int i = 0; i < tabs.length; i++) {
            int tx = px + i * tabW;
            boolean act = currentTab == i;
            boolean hov = !act && hover(mouseX, mouseY, tx, tabY, tabW, TAB_H);
            fillRect(ctx, tx, tabY, tabW, TAB_H, act ? COL_SURFACE : (hov ? COL_HOVER : COL_BG));

            if (act)
                fillRect(ctx, tx + 2, tabY + TAB_H - 2, tabW - 4, 2, COL_GOLD);
            //? if !mojmap {
            ctx.drawCenteredTextWithShadow(
                    //?} else {
                    /*ctx.centeredText(*/
                    //?}
                    textRenderer, tabs[i], tx + tabW / 2, tabY + 9,
                    act ? COL_GOLD : (hov ? COL_TEXT : COL_TEXT_DIM));
        }

        fillRect(ctx, px, tabY + TAB_H, PANEL_W, 1, COL_BORDER);

        int contentTop = tabY + TAB_H + 1;
        int bottomBarH = 36;
        int contentBot = py + PANEL_H - bottomBarH;
        ctx.enableScissor(px + 1, contentTop, px + PANEL_W - 1, contentBot);

        int scrolled = contentTop - (int) scrollOffset;
        switch (currentTab) {
            case 0 -> renderGeneral(ctx, mouseX, mouseY, scrolled);
            case 1 -> renderNotifications(ctx, mouseX, mouseY, scrolled);
            case 2 -> renderCape(ctx, mouseX, mouseY, scrolled);
            case 3 -> renderCollection(ctx, mouseX, mouseY, scrolled);
            case 4 -> renderScreenshots(ctx, mouseX, mouseY, scrolled);
            case 5 -> renderAbout(ctx, scrolled);
        }

        ctx.disableScissor();

        fillRect(ctx, px, contentBot, PANEL_W, bottomBarH, COL_BG);
        fillRect(ctx, px, contentBot, PANEL_W, 1, COL_BORDER);
        drawButton(ctx, mouseX, mouseY, px + 12, contentBot + 8, 120, 20, "Save & Close", COL_GREEN);
        drawButton(ctx, mouseX, mouseY, px + PANEL_W - 132, contentBot + 8, 120, 20, "Cancel", COL_RED);
    }

    private void renderGeneral(DrawContext ctx, int mx, int my, int baseY) {
        int pad = 12;
        int fw = PANEL_W - pad * 2;
        int hw = (fw - 8) / 2;

        sectionLabel(ctx, "Account", px + pad, baseY + pad);
        drawInput(ctx, mx, my, px + pad, baseY + pad + 16, fw, 0);

        sectionLabel(ctx, "Performance & Automation", px + pad, baseY + pad + 62);
        drawInput(ctx, mx, my, px + pad, baseY + pad + 78, hw, 2);
        if (toggles[8]) {
            drawInput(ctx, mx, my, px + pad + hw + 8, baseY + pad + 78, hw, 7);
        }

        sectionLabel(ctx, "Display & Detection", px + pad, baseY + pad + 124);
        drawToggle(ctx, mx, my, px + pad, baseY + pad + 140, 0); // Auto Scan
        drawToggle(ctx, mx, my, px + pad + hw + 8, baseY + pad + 140, 6); // Inv Highlight

        drawToggle(ctx, mx, my, px + pad, baseY + pad + 180, 8); // Auto Hopper
        drawToggle(ctx, mx, my, px + pad + hw + 8, baseY + pad + 180, 9); // AH Sniper

        drawToggle(ctx, mx, my, px + pad, baseY + pad + 220, 3); // Player Highlight
        drawToggle(ctx, mx, my, px + pad + hw + 8, baseY + pad + 220, 4); // HUD Overlay

        drawToggle(ctx, mx, my, px + pad, baseY + pad + 260, 2); // Tooltips
        drawToggle(ctx, mx, my, px + pad + hw + 8, baseY + pad + 260, 7); // Capes
    }

    private void renderNotifications(DrawContext ctx, int mx, int my, int baseY) {
        int pad = 12;
        int fw = PANEL_W - pad * 2;
        int hw = (fw - 8) / 2;

        sectionLabel(ctx, "Discord Webhook", px + pad, baseY + pad);
        drawInput(ctx, mx, my, px + pad, baseY + pad + 16, fw, 1);

        sectionLabel(ctx, "Alerts", px + pad, baseY + pad + 62);
        drawToggle(ctx, mx, my, px + pad, baseY + pad + 78, 1);
        drawToggle(ctx, mx, my, px + pad + hw + 8, baseY + pad + 78, 5);

        if (toggles[5]) {
            sectionLabel(ctx, "Auto-Message Config", px + pad, baseY + pad + 124);
            drawInput(ctx, mx, my, px + pad, baseY + pad + 140, fw, 3);
            drawInput(ctx, mx, my, px + pad, baseY + pad + 186, hw, 4);
            drawInput(ctx, mx, my, px + pad + hw + 8, baseY + pad + 186, hw, 5);
            drawInput(ctx, mx, my, px + pad, baseY + pad + 232, fw, 6);
        }
    }

    private void renderCape(DrawContext ctx, int mx, int my, int baseY) {
        int pad = 12;
        String rank = UserAPI.myRank != null ? UserAPI.myRank : "USER";
        boolean hasAccess = !"USER".equals(rank);

        sectionLabel(ctx, "Custom Cape  (Rank: §e" + rank + "§r)", px + pad, baseY + pad);

        if (!hasAccess) {
            int bY = baseY + pad + 20;
            fillRect(ctx, px + pad, bY, PANEL_W - pad * 2, 48, 0xFF1A0D00);
            drawBorder(ctx, px + pad, bY, PANEL_W - pad * 2, 48, COL_GOLD);
            //? if !mojmap {
            ctx.drawCenteredTextWithShadow(
                    //?} else {
                    /*ctx.centeredText(*/
                    //?}
                    textRenderer, "⭐ VIP / Premium required", px + PANEL_W / 2, bY + 18,
                    COL_GOLD);
            return;
        }

        int inputY = baseY + pad + 20;
        fillRect(ctx, px + pad, inputY, PANEL_W - pad * 2, 22, COL_INPUT_BG);
        drawBorder(ctx, px + pad, inputY, PANEL_W - pad * 2, 22, COL_BORDER);
        String disp = capePath.isEmpty() ? "§7Click Browse… to select a PNG file" : capePath;
        //? if !mojmap {
        ctx.drawTextWithShadow(
                //?} else {
                /*ctx.text(*/
                //?}
                textRenderer, disp, px + pad + 6, inputY + 7, COL_TEXT);

        int btnY = inputY + 32;
        drawButton(ctx, mx, my, px + pad, btnY, 110, 22, "Browse…", COL_ACCENT);
        drawButton(ctx, mx, my, px + pad + 118, btnY, 140, 22,
                capeUploading.get() ? "Uploading…" : "Upload Cape", COL_PURPLE);

        if (!statusMsg.isEmpty())
            //? if !mojmap {
            ctx.drawTextWithShadow(
                    //?} else {
                    /*ctx.text(*/
                    //?}
                    textRenderer, statusMsg, px + pad, btnY + 32, statusMsgColor);
    }

    private void renderCollection(DrawContext ctx, int mx, int my, int baseY) {
        List<ColorCheckResult> coll = CollectionManager.getCollection();
        int pad = 12;

        sectionLabel(ctx, "Collection Log  (" + coll.size() + " items)", px + pad, baseY + pad);

        if (coll.isEmpty()) {
            //? if !mojmap {
            ctx.drawCenteredTextWithShadow(
                    //?} else {
                    /*ctx.centeredText(*/
                    //?}
                    textRenderer, "No exotic items discovered yet.", px + PANEL_W / 2,
                    baseY + 80, COL_TEXT_DIM);
            return;
        }

        int cw = (PANEL_W - pad * 2 - 16) / 3;
        int ch = 58;
        int start = collectionPage * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, coll.size());

        for (int i = start; i < end; i++) {
            ColorCheckResult item = coll.get(i);
            int idx = i - start;
            int cx = px + pad + (idx % 3) * (cw + 8);
            int cy = baseY + pad + 20 + (idx / 3) * (ch + 6);

            boolean hv = hover(mx, my, cx, cy, cw, ch);
            fillRect(ctx, cx, cy, cw, ch, hv ? COL_HOVER : COL_SURFACE);
            drawBorder(ctx, cx, cy, cw, ch, hv ? COL_GOLD : COL_BORDER);

            Item base = Items.LEATHER_CHESTPLATE;
            String id = item.item_id != null ? item.item_id.toUpperCase() : "";
            if (id.contains("BOOTS"))
                base = Items.LEATHER_BOOTS;
            else if (id.contains("LEGGINGS"))
                base = Items.LEATHER_LEGGINGS;
            else if (id.contains("HELMET"))
                base = Items.LEATHER_HELMET;
            ItemStack stack = new ItemStack(base);
            try {
                String hex = (item.hex != null ? item.hex : "FFFFFF").replace("#", "");
                stack.set(DataComponentTypes.DYED_COLOR, new DyedColorComponent(Integer.parseInt(hex, 16)));
            } catch (Exception ignored) {
            }
            //? if !mojmap {
            ctx.drawItem(stack, cx + 4, cy + (ch - 16) / 2);
            //?} else {
            /*ctx.item(stack, cx + 4, cy + (ch - 16) / 2);*/
            //?}

            String variant = item.variant != null ? item.variant : "Unknown";
            String cat = (item.category != null ? item.category : "EXOTIC").toUpperCase();
            //? if !mojmap {
            ctx.drawTextWithShadow(
                    //?} else {
                    /*ctx.text(*/
                    //?}
                    textRenderer, variant, cx + 24, cy + 10, COL_TEXT);
            //? if !mojmap {
            ctx.drawTextWithShadow(
                    //?} else {
                    /*ctx.text(*/
                    //?}
                    textRenderer, "§7" + cat, cx + 24, cy + 22, COL_TEXT_DIM);
        }

        int totalPages = Math.max(1, (int) Math.ceil((double) coll.size() / ITEMS_PER_PAGE));
        collectionPage = Math.clamp(collectionPage, 0, totalPages - 1);
        if (totalPages > 1)
            drawPagination(ctx, mx, my, collectionPage, totalPages, py + PANEL_H - 50);
    }

    private void renderScreenshots(DrawContext ctx, int mx, int my, int baseY) {
        List<Path> shots = getScreenshots();
        int pad = 12;

        sectionLabel(ctx, "Screenshots  (" + shots.size() + ")", px + pad, baseY + pad);

        if (shots.isEmpty()) {
            //? if !mojmap {
            ctx.drawCenteredTextWithShadow(
                    //?} else {
                    /*ctx.centeredText(*/
                    //?}
                    textRenderer, "No screenshots found.", px + PANEL_W / 2, baseY + 80,
                    COL_TEXT_DIM);
            return;
        }

        int cardW = (PANEL_W - pad * 2 - 10) / 2;
        int cardH = 60;
        int start = screenshotPage * SHOTS_PER_PAGE;
        int end = Math.min(start + SHOTS_PER_PAGE, shots.size());

        for (int i = start; i < end; i++) {
            Path path = shots.get(i);
            int local = i - start;
            int cx = px + pad + (local % 2) * (cardW + 10);
            int cy = baseY + pad + 20 + (local / 2) * (cardH + 6);

            boolean hov = hover(mx, my, cx, cy, cardW, cardH);
            fillRect(ctx, cx, cy, cardW, cardH, hov ? COL_HOVER : COL_SURFACE);
            drawBorder(ctx, cx, cy, cardW, cardH, hov ? COL_GOLD : COL_BORDER);

            Identifier texId = screenshotTextures.get(path);
            if (texId != null) {
                ctx.drawTexture(RenderPipelines.GUI_TEXTURED, texId, cx + 4, cy + 4, 0f, 0f, 52, 32, 52, 32);
            } else {
                fillRect(ctx, cx + 4, cy + 4, 52, 32, 0xFF222235);
                //? if !mojmap {
                ctx.drawCenteredTextWithShadow(
                        //?} else {
                        /*ctx.centeredText(*/
                        //?}
                        textRenderer, "§8...", cx + 30, cy + 14, COL_TEXT_DIM);
                loadThumbnail(path);
            }

            String name = path.getFileName().toString();
            if (name.length() > 20)
                name = name.substring(0, 17) + "...";
            //? if !mojmap {
            ctx.drawTextWithShadow(
                    //?} else {
                    /*ctx.text(*/
                    //?}
                    textRenderer, name, cx + 62, cy + 6, COL_TEXT);

            String date = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                    .format(new Date(path.toFile().lastModified()).toInstant().atZone(ZoneId.systemDefault()));
            //? if !mojmap {
            ctx.drawTextWithShadow(
                    //?} else {
                    /*ctx.text(*/
                    //?}
                    textRenderer, "§7" + date, cx + 62, cy + 18, COL_TEXT_DIM);

            // FIX: Share button — use absolute index i so sharing state is per-screenshot
            int sx = cx + cardW - 56, sy = cy + cardH - 20;
            boolean sharing = sharingIndices.contains(i);
            boolean shov = !sharing && hover(mx, my, sx, sy, 52, 16);
            fillRect(ctx, sx, sy, 52, 16, sharing ? COL_SURFACE : (shov ? COL_ACCENT : COL_SURFACE));
            drawBorder(ctx, sx, sy, 52, 16, sharing ? COL_TEXT_DIM : (shov ? COL_GOLD : COL_BORDER));
            //? if !mojmap {
            ctx.drawCenteredTextWithShadow(
                    //?} else {
                    /*ctx.centeredText(*/
                    //?}
                    textRenderer,
                    sharing ? "§7..." : "Share",
                    sx + 26, sy + 4,
                    sharing ? COL_TEXT_DIM : (shov ? COL_TEXT : COL_TEXT_DIM));
        }

        int totalPages = Math.max(1, (int) Math.ceil((double) shots.size() / SHOTS_PER_PAGE));
        screenshotPage = Math.clamp(screenshotPage, 0, totalPages - 1);
        if (totalPages > 1)
            drawPagination(ctx, mx, my, screenshotPage, totalPages, py + PANEL_H - 50);

        if (!statusMsg.isEmpty()) {
            //? if !mojmap {
            ctx.drawCenteredTextWithShadow(
                    //?} else {
                    /*ctx.centeredText(*/
                    //?}
                    textRenderer, statusMsg, px + PANEL_W / 2, py + PANEL_H - 25,
                    statusMsgColor);
        }
    }

    private void renderAbout(DrawContext ctx, int baseY) {
        //? if !mojmap {
        ctx.drawCenteredTextWithShadow(
                //?} else {
                /*ctx.centeredText(*/
                //?}
                textRenderer, "✦ Exotico Mod ✦", px + PANEL_W / 2, baseY + 20, COL_GOLD);
        //? if !mojmap {
        ctx.drawCenteredTextWithShadow(
                //?} else {
                /*ctx.centeredText(*/
                //?}
                textRenderer, "Premium Exotic Armor Scanner & Cape Tool", px + PANEL_W / 2,
                baseY + 40, COL_TEXT);
        //? if !mojmap {
        ctx.drawCenteredTextWithShadow(
                //?} else {
                /*ctx.centeredText(*/
                //?}
                textRenderer, "§7API: api.flori.tv", px + PANEL_W / 2, baseY + 60, COL_TEXT_DIM);
    }

    private void sectionLabel(DrawContext ctx, String label, int x, int y) {
        //? if !mojmap {
        ctx.drawTextWithShadow(
                //?} else {
                /*ctx.text(*/
                //?}
                textRenderer, "§e" + label, x, y, COL_GOLD);
        fillRect(ctx, x, y + 10, textRenderer.getWidth(label), 1, 0x44FFAA00);
    }

    private void drawInput(DrawContext ctx, int mx, int my, int x, int y, int w, int idx) {
        boolean active = activeInput == idx;
        boolean hov = hover(mx, my, x, y, w, 22) && !active;
        fillRect(ctx, x, y, w, 22, active ? COL_INPUT_ACT : (hov ? COL_HOVER : COL_INPUT_BG));
        drawBorder(ctx, x, y, w, 22, active ? COL_BORDER_ACT : COL_BORDER);

        String raw = inputValues[idx];
        boolean empty = raw.isEmpty();
        String disp;
        if (empty && !active) {
            disp = inputPlaceholder[idx];
            //? if !mojmap {
            ctx.drawTextWithShadow(
                    //?} else {
                    /*ctx.text(*/
                    //?}
                    textRenderer, "§8" + disp, x + 6, y + 7, 0xFF505068);
        } else {
            disp = (inputMasked[idx] && !active)
                    ? "•".repeat(Math.min(raw.length(), 28))
                    : raw;
            if (active && (tickCounter / 10 % 2 == 0))
                disp += "|";
            //? if !mojmap {
            ctx.drawTextWithShadow(
                    //?} else {
                    /*ctx.text(*/
                    //?}
                    textRenderer, disp, x + 6, y + 7, active ? COL_TEXT : COL_TEXT_DIM);
        }

        if (active) {
            String hint = "§8Ctrl+C copy · Ctrl+V paste · Ctrl+A clear";
            //? if !mojmap {
            ctx.drawTextWithShadow(
                    //?} else {
                    /*ctx.text(*/
                    //?}
                    textRenderer, hint, x + 6, y + 24, 0xFF444460);
        } else if (!empty) {
            //? if !mojmap {
            ctx.drawTextWithShadow(
                    //?} else {
                    /*ctx.text(*/
                    //?}
                    textRenderer, inputHints[idx], x + 2, y + 24, 0xFF444460);
        }
    }

    private void drawToggle(DrawContext ctx, int mx, int my, int x, int y, int idx) {
        boolean val = toggles[idx];
        int w = (PANEL_W - 24 - 8) / 2;
        int h = 30;
        boolean hov = hover(mx, my, x, y, w, h);

        fillRect(ctx, x, y, w, h, hov ? COL_HOVER : COL_SURFACE);
        fillRect(ctx, x, y, 2, h, val ? COL_GREEN : COL_BORDER);
        fillRect(ctx, x, y + h - 1, w, 1, val ? COL_GREEN : (hov ? COL_GOLD : COL_BORDER));
        drawBorder(ctx, x, y, w, h, val ? 0xFF2FAE6E : (hov ? COL_GOLD : COL_BORDER));
        //? if !mojmap {
        ctx.drawTextWithShadow(
                //?} else {
                /*ctx.text(*/
                //?}
                textRenderer, toggleLabels[idx], x + 10, y + 11, COL_TEXT);

        int trackW = 34, trackH = 14;
        int tx = x + w - trackW - 8;
        int ty = y + (h - trackH) / 2;

        int trackColor = val ? 0xFF1E7A4E : 0xFF1A1A30;
        fillRect(ctx, tx, ty, trackW, trackH, trackColor);
        drawBorder(ctx, tx, ty, trackW, trackH, val ? COL_GREEN : COL_BORDER);

        int knobW = 12, knobH = 10;
        int kx = val ? tx + trackW - knobW - 2 : tx + 2;
        int ky = ty + 2;
        int knobColor = val ? COL_GREEN : 0xFF666680;
        fillRect(ctx, kx, ky, knobW, knobH, knobColor);
        fillRect(ctx, kx + 1, ky, knobW - 2, 1, val ? 0xFF88FFB8 : 0xFF8888AA);
    }

    private void drawButton(DrawContext ctx, int mx, int my, int x, int y, int w, int h, String label, int accent) {
        boolean hov = hover(mx, my, x, y, w, h);
        fillRect(ctx, x, y, w, h, hov ? COL_HOVER : COL_SURFACE);
        drawBorder(ctx, x, y, w, h, hov ? accent : COL_BORDER);
        //? if !mojmap {
        ctx.drawCenteredTextWithShadow(
                //?} else {
                /*ctx.centeredText(*/
                //?}
                textRenderer, label, x + w / 2, y + (h - 8) / 2 + 1, hov ? accent : COL_TEXT);
    }

    private void drawPagination(DrawContext ctx, int mx, int my, int page, int total, int y) {
        int bx = px + PANEL_W / 2;
        drawButton(ctx, mx, my, bx - 66, y, 22, 18, "<", COL_GOLD);
        //? if !mojmap {
        ctx.drawCenteredTextWithShadow(
                //?} else {
                /*ctx.centeredText(*/
                //?}
                textRenderer, (page + 1) + " / " + total, bx, y + 5, COL_TEXT_DIM);
        drawButton(ctx, mx, my, bx + 44, y, 22, 18, ">", COL_GOLD);
    }

    private void fillRect(DrawContext ctx, int x, int y, int w, int h, int col) {
        ctx.fill(x, y, x + w, y + h, col);
    }

    private void drawBorder(DrawContext ctx, int x, int y, int w, int h, int col) {
        ctx.fill(x, y, x + w, y + 1, col);
        ctx.fill(x, y + h - 1, x + w, y + h, col);
        ctx.fill(x, y + 1, x + 1, y + h - 1, col);
        ctx.fill(x + w - 1, y + 1, x + w, y + h - 1, col);
    }

    private boolean hover(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    //? if !mojmap {
    @Override
    public boolean mouseClicked(Click click, boolean bl) {
        //?} else {
    /*@Override
    public boolean mouseClicked(MouseButtonEvent click, boolean bl) {
    *///?}
        if (click.button() != 0)
            return false;
        int mx = (int) click.x();
        int my = (int) click.y();

        int tabY = py + HEADER_H;
        int contentTop = tabY + TAB_H + 1;
        int bottomY = py + PANEL_H - 36;

        if (hover(mx, my, px + PANEL_W - 26, py + 7, 18, 18)) {
            close();
            return true;
        }

        if (my >= tabY && my < tabY + TAB_H) {
            int tabW = PANEL_W / 6;
            int t = (mx - px) / tabW;
            if (t >= 0 && t < 6) {
                if (t != currentTab) {
                    scrollOffset = 0;
                    targetScroll = 0;
                }
                currentTab = t;
                activeInput = -1;
                return true;
            }
        }

        if (my >= bottomY && my < bottomY + 36) {
            if (hover(mx, my, px + 12, bottomY + 8, 120, 20)) {
                saveConfig();
                close();
                return true;
            }
            if (hover(mx, my, px + PANEL_W - 132, bottomY + 8, 120, 20)) {
                close();
                return true;
            }
        }

        // FIX: Only handle content clicks when the click is actually inside the content area.
        // Previously, clicks below/outside the scissor region still triggered share buttons.
        int contentBot = py + PANEL_H - 36;
        if (my < contentTop || my >= contentBot) {
            return false;
        }

        int scrolledY = contentTop - (int) scrollOffset;
        boolean handled = switch (currentTab) {
            case 0, 1 -> clickInputs(mx, my, scrolledY) || clickToggles(mx, my, scrolledY);
            case 2 -> clickCape(mx, my, scrolledY);
            case 3 -> clickCollection(mx, my);
            case 4 -> clickScreenshots(mx, my, scrolledY);
            default -> false;
        };

        if (!handled)
            activeInput = -1;
        return handled || super.mouseClicked(click, bl);
    }

    private boolean clickInputs(int mx, int my, int baseY) {
        int pad = 12;
        int fw = PANEL_W - pad * 2;
        int hw = (fw - 8) / 2;

        if (currentTab == 0) {
            if (hover(mx, my, px + pad, baseY + pad + 16, fw, 22)) {
                activeInput = 0;
                return true;
            }
            if (hover(mx, my, px + pad, baseY + pad + 78, hw, 22)) {
                activeInput = 2;
                return true;
            }
            if (toggles[8] && hover(mx, my, px + pad + hw + 8, baseY + pad + 78, hw, 22)) {
                activeInput = 7;
                return true;
            }
        } else {
            if (hover(mx, my, px + pad, baseY + pad + 16, fw, 22)) {
                activeInput = 1;
                return true;
            }
            if (toggles[5]) {
                if (hover(mx, my, px + pad, baseY + pad + 140, fw, 22)) {
                    activeInput = 3;
                    return true;
                }
                if (hover(mx, my, px + pad, baseY + pad + 186, hw, 22)) {
                    activeInput = 4;
                    return true;
                }
                if (hover(mx, my, px + pad + hw + 8, baseY + pad + 186, hw, 22)) {
                    activeInput = 5;
                    return true;
                }
                if (hover(mx, my, px + pad, baseY + pad + 232, fw, 22)) {
                    activeInput = 6;
                    return true;
                }
            }
        }
        return false;
    }

    private boolean clickToggles(int mx, int my, int baseY) {
        int pad = 12;
        int fw = PANEL_W - pad * 2;
        int hw = (fw - 8) / 2;
        int toggleW = (PANEL_W - 24 - 8) / 2;

        int[][] pos;
        if (currentTab == 0) {
            pos = new int[][] {
                    { 0, px + pad, baseY + pad + 140 }, // Auto Scan
                    { 6, px + pad + hw + 8, baseY + pad + 140 }, // Inv Highlight
                    { 8, px + pad, baseY + pad + 180 }, // Auto Hopper
                    { 9, px + pad + hw + 8, baseY + pad + 180 }, // AH Sniper
                    { 3, px + pad, baseY + pad + 220 }, // Player Highlight
                    { 4, px + pad + hw + 8, baseY + pad + 220 }, // HUD Overlay
                    { 2, px + pad, baseY + pad + 260 }, // Tooltips
                    { 7, px + pad + hw + 8, baseY + pad + 260 }, // Capes
            };
        } else {
            pos = new int[][] {
                    { 1, px + pad, baseY + pad + 78 },
                    { 5, px + pad + hw + 8, baseY + pad + 78 },
            };
        }

        for (int[] p : pos) {
            if (hover(mx, my, p[1], p[2], toggleW, 30)) {
                toggles[p[0]] = !toggles[p[0]];
                return true;
            }
        }
        return false;
    }

    private boolean clickCape(int mx, int my, int baseY) {
        int pad = 12;
        int btnY = baseY + pad + 20 + 32;
        if (hover(mx, my, px + pad, btnY, 110, 22)) {
            openFileChooser();
            return true;
        }
        if (hover(mx, my, px + pad + 118, btnY, 140, 22)) {
            startCapeUpload();
            return true;
        }
        return false;
    }

    private boolean clickCollection(int mx, int my) {
        int total = Math.max(1, (int) Math.ceil((double) CollectionManager.getCollection().size() / ITEMS_PER_PAGE));
        int bx = px + PANEL_W / 2, by = py + PANEL_H - 50;
        if (hover(mx, my, bx - 66, by, 22, 18)) {
            if (collectionPage > 0)
                collectionPage--;
            return true;
        }
        if (hover(mx, my, bx + 44, by, 22, 18)) {
            if (collectionPage < total - 1)
                collectionPage++;
            return true;
        }
        return false;
    }

    private boolean clickScreenshots(int mx, int my, int baseY) {
        List<Path> shots = getScreenshots();
        int total = Math.max(1, (int) Math.ceil((double) shots.size() / SHOTS_PER_PAGE));
        int bx = px + PANEL_W / 2, by = py + PANEL_H - 50;

        // Pagination buttons
        if (hover(mx, my, bx - 66, by, 22, 18)) {
            if (screenshotPage > 0)
                screenshotPage--;
            return true;
        }
        if (hover(mx, my, bx + 44, by, 22, 18)) {
            if (screenshotPage < total - 1)
                screenshotPage++;
            return true;
        }

        int pad = 12, cardW = (PANEL_W - pad * 2 - 10) / 2, cardH = 60;
        int start = screenshotPage * SHOTS_PER_PAGE;
        int end = Math.min(start + SHOTS_PER_PAGE, shots.size());

        for (int i = start; i < end; i++) {
            int local = i - start;
            int cx = px + pad + (local % 2) * (cardW + 10);
            int cy = baseY + pad + 20 + (local / 2) * (cardH + 6);
            int sx = cx + cardW - 56, sy = cy + cardH - 20;

            if (hover(mx, my, sx, sy, 52, 16)) {
                // FIX: Use per-screenshot index instead of a global boolean lock.
                // This allows each card to independently show its sharing state,
                // and prevents a stuck global flag from blocking all future clicks.
                if (sharingIndices.contains(i))
                    return true; // already uploading this one

                Path p = shots.get(i);
                final int shareIdx = i;

                statusMsg = "§eSharing...";
                statusMsgColor = COL_GOLD;
                sharingIndices.add(shareIdx);

                // FIX: Wrap in exceptionally() so the index is ALWAYS removed,
                // even if UserAPI.shareScreenshot() throws or returns a failed future.
                UserAPI.shareScreenshot(p)
                        .thenAccept(url -> MinecraftClient.getInstance().execute(() -> {
                            sharingIndices.remove(shareIdx);
                            if (url != null && !url.isBlank()) {
                                MinecraftClient.getInstance().keyboard.setClipboard(url);
                                statusMsg = "§a✔ Shared! Link copied.";
                                statusMsgColor = COL_GREEN;
                            } else {
                                statusMsg = "§cFailed to share screenshot.";
                                statusMsgColor = COL_RED;
                            }
                        }))
                        .exceptionally(ex -> {
                            MinecraftClient.getInstance().execute(() -> {
                                sharingIndices.remove(shareIdx);
                                statusMsg = "§cShare failed.";
                                statusMsgColor = COL_RED;
                            });
                            return null;
                        });

                return true;
            }
        }
        return false;
    }

    //? if !mojmap {
    @Override
    public boolean keyPressed(KeyInput input) {
        //?} else {
    /*@Override
    public boolean keyPressed(KeyEvent input) {
    *///?}
        int key = input.key();
        int mods = input.modifiers();
        boolean ctrl = (mods & GLFW.GLFW_MOD_CONTROL) != 0;

        if (key == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }

        if (activeInput >= 0) {
            if (ctrl) {
                if (key == GLFW.GLFW_KEY_C) {
                    String val = inputValues[activeInput];
                    if (!val.isEmpty())
                        MinecraftClient.getInstance().keyboard.setClipboard(val);
                    return true;
                }
                if (key == GLFW.GLFW_KEY_V) {
                    String clip = MinecraftClient.getInstance().keyboard.getClipboard();
                    if (clip != null && !clip.isEmpty()) {
                        String cur = inputValues[activeInput];
                        int limit = inputMaxLen[activeInput];
                        String merged = (cur + clip).length() <= limit
                                ? cur + clip
                                : (cur + clip).substring(0, limit);
                        inputValues[activeInput] = merged;
                    }
                    return true;
                }
                if (key == GLFW.GLFW_KEY_A) {
                    inputValues[activeInput] = "";
                    return true;
                }
            }

            if (key == GLFW.GLFW_KEY_BACKSPACE) {
                String cur = inputValues[activeInput];
                if (!cur.isEmpty())
                    inputValues[activeInput] = cur.substring(0, cur.length() - 1);
                return true;
            }
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                activeInput = -1;
                return true;
            }
            if (key == GLFW.GLFW_KEY_TAB) {
                activeInput = (activeInput + 1) % inputValues.length;
                return true;
            }
        }
        return super.keyPressed(input);
    }

    //? if !mojmap {
    @Override
    public boolean charTyped(CharInput input) {
        if (!input.isValidChar() || activeInput < 0 || activeInput >= inputValues.length)
            return false;
        //?} else {
    /*@Override
    public boolean charTyped(CharacterEvent input) {
        if (!input.isAllowedChatCharacter() || activeInput < 0 || activeInput >= inputValues.length)
            return false;
    *///?}
        String cur = inputValues[activeInput];
        if (cur.length() < inputMaxLen[activeInput])
            inputValues[activeInput] = cur + (char) input.codepoint();
        return true;
    }

    private List<Path> getScreenshots() {
        long now = System.currentTimeMillis();
        if (cachedShots == null || now - lastShotCache > SHOT_CACHE_MS) {
            Path dir = MinecraftClient.getInstance().runDirectory.toPath().resolve("screenshots");
            if (!Files.exists(dir)) {
                cachedShots = Collections.emptyList();
            } else {
                try (var s = Files.list(dir)) {
                    cachedShots = s.filter(p -> p.toString().toLowerCase().endsWith(".png"))
                            .sorted(Comparator.comparingLong(p -> -p.toFile().lastModified()))
                            .toList();
                } catch (Exception e) {
                    cachedShots = Collections.emptyList();
                }
            }
            lastShotCache = now;
        }
        return cachedShots;
    }

    private void loadThumbnail(Path path) {
        if (screenshotTextures.containsKey(path) || loadingThumbnails.contains(path))
            return;
        loadingThumbnails.add(path);
        CompletableFuture.runAsync(() -> {
            try (InputStream in = Files.newInputStream(path)) {
                NativeImage img = NativeImage.read(in);
                String safe = "ss_" + path.getFileName().toString().replaceAll("[^a-zA-Z0-9]", "_");
                MinecraftClient.getInstance().execute(() -> {
                    Identifier id = Identifier.of("exotico", safe);
                    NativeImageBackedTexture tex = new NativeImageBackedTexture(() -> safe, img);
                    MinecraftClient.getInstance().getTextureManager().registerTexture(id, tex);
                    screenshotTextures.put(path, id);
                    loadingThumbnails.remove(path);
                });
            } catch (Exception ignored) {
                loadingThumbnails.remove(path);
            }
        });
    }

    private void openFileChooser() {
        new Thread(() -> {
            String chosen = TinyFileDialogs.tinyfd_openFileDialog("Select Cape PNG", null, null, "PNG Files (*.png)",
                    false);
            if (chosen != null && !chosen.isBlank())
                capePath = chosen;
        }).start();
    }

    private void startCapeUpload() {
        if (capePath.isEmpty() || capeUploading.get())
            return;
        statusMsg = "§eUploading…";
        statusMsgColor = COL_GOLD;
        capeUploading.set(true);

        CompletableFuture.runAsync(() -> {
            try {
                UserAPI.uploadCape(Paths.get(capePath));
                MinecraftClient.getInstance().execute(() -> {
                    capeUploading.set(false);
                    statusMsg = "§a✔ Cape uploaded successfully!";
                    statusMsgColor = COL_GREEN;
                });
            } catch (Exception ex) {
                MinecraftClient.getInstance().execute(() -> {
                    capeUploading.set(false);
                    statusMsg = "§cUpload error: " + ex.getMessage();
                    statusMsgColor = COL_RED;
                });
            }
        });
    }

    @Override
    public void close() {
        screenshotTextures.values().forEach(id -> MinecraftClient.getInstance().getTextureManager().destroyTexture(id));
        screenshotTextures.clear();
        //? if !guiScreenHolder {
        MinecraftClient.getInstance().setScreen(parent);
        //?} else {
        /*Minecraft.getInstance().gui.setScreen(parent);*/
        //?}
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}