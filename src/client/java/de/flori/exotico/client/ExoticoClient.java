package de.flori.exotico.client;

import de.flori.exotico.api.UserAPI;
import de.flori.exotico.scanner.PlayerScanner;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

public class ExoticoClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        PlayerScanner.init();
        ExoticoCommands.register();
        TooltipHandler.init();
        CollectionManager.init();
        HudRenderCallback.EVENT.register(new ExoticoHudOverlay());
        UserAPI.init();
    }
}