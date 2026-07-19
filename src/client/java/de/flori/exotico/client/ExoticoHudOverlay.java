package de.flori.exotico.client;

import de.flori.exotico.config.ExoticoConfig;
//? if !mojmap {
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
//?} else {
/*import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
 *///?}
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

import java.util.ArrayList;
import java.util.List;

//? if !mojmap {
public class ExoticoHudOverlay implements HudRenderCallback {
//?} else {
    /*public class ExoticoHudOverlay implements HudElement {
     *///?}

    public static final List<RecentExotic> recentExotics = new ArrayList<>();
    private static final long SHOW_DURATION = 120000;

    public static void addExotic(String playerName, String items) {
        recentExotics.removeIf(e -> e.playerName.equals(playerName));
        recentExotics.add(0, new RecentExotic(playerName, items, System.currentTimeMillis()));
        if (recentExotics.size() > 5) {
            recentExotics.remove(recentExotics.size() - 1);
        }
    }

    //? if !mojmap {
    @Override
    public void onHudRender(DrawContext drawContext, RenderTickCounter tickCounter) {
        //?} else {
    /*@Override
    public void extractRenderState(DrawContext drawContext, RenderTickCounter tickCounter) {
    *///?}
        if (!ExoticoConfig.getInstance().enableHudOverlay)
            return;

        long now = System.currentTimeMillis();
        recentExotics.removeIf(e -> now - e.timestamp > SHOW_DURATION);

        if (recentExotics.isEmpty())
            return;

        MinecraftClient client = MinecraftClient.getInstance();
        int screenWidth = client.getWindow().getScaledWidth();
        int y = 5;


        String title = "✦ Recent Exotics ✦";
        int titleWidth = client.textRenderer.getWidth(title);
        drawContext.fill(screenWidth - titleWidth - 15, y, screenWidth - 5, y + 14, 0xAA060610);
        //? if !mojmap {
        drawContext.drawTextWithShadow(
                //?} else {
                /*drawContext.text(*/
                //?}
                client.textRenderer, title, screenWidth - titleWidth - 10, y + 3, 0xFFFFAA00);
        y += 16;

        for (RecentExotic entry : recentExotics) {
            String text = entry.playerName + " - " + entry.items;
            if (text.length() > 30) {
                text = text.substring(0, 27) + "...";
            }
            int width = client.textRenderer.getWidth(text);

            drawContext.fill(screenWidth - width - 15, y, screenWidth - 5, y + 12, 0x881A1A2A);
            drawContext.fill(screenWidth - width - 16, y, screenWidth - width - 15, y + 12, 0xFFFFAA00);

            //? if !mojmap {
            drawContext.drawTextWithShadow(
                    //?} else {
                    /*drawContext.text(*/
                    //?}
                    client.textRenderer, text, screenWidth - width - 10, y + 2, 0xFFE0E0E0);

            y += 14;
        }
    }

    private static class RecentExotic {
        String playerName;
        String items;
        long timestamp;

        public RecentExotic(String playerName, String items, long timestamp) {
            this.playerName = playerName;
            this.items = items;
            this.timestamp = timestamp;
        }
    }
}