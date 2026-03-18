package de.flori.exotico.mixin.client;

import de.flori.exotico.api.UserAPI;
import de.flori.exotico.config.ExoticoConfig;
import de.flori.exotico.scanner.PlayerScanner;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(EntityRenderer.class)
public class EntityRendererMixin<T extends Entity, S extends EntityRenderState> {

    @Inject(method = "updateRenderState", at = @At("TAIL"))
    private void onUpdateRenderState(T entity, S state, float tickDelta, CallbackInfo ci) {
        if (!ExoticoConfig.getInstance().enablePlayerHighlight)
            return;

        if (entity instanceof PlayerEntity player) {
            String name = player.getGameProfile().name();


            if (state.displayName != null && UserAPI.hasMod(name)) {
                String rankStr = UserAPI.getRankFormat(name);
                if (!rankStr.isEmpty()) {
                    state.displayName = Text.literal(rankStr).append(state.displayName);
                }
            }


            Set<String> exotics = PlayerScanner.playersWithExotics;
            if (exotics.contains(name) && state.displayName != null) {
                state.displayName = Text.literal("⭐ ").formatted(Formatting.GOLD)
                        .append(state.displayName);
            }
        }
    }
}