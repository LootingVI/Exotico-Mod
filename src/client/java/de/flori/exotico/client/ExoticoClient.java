package de.flori.exotico.client;

import de.flori.exotico.api.UserAPI;
import de.flori.exotico.scanner.PlayerScanner;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import de.flori.exotico.client.social.SocialManager;

public class ExoticoClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        PlayerScanner.init();
        ExoticoCommands.register();
        TooltipHandler.init();
        CollectionManager.init();
        HudRenderCallback.EVENT.register(new ExoticoHudOverlay());
        UserAPI.init();
        java.io.File confDir = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().toFile();
        de.flori.exotico.client.social.SocialCrypto.init(confDir);
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            SocialManager.init();
        });
    }
}