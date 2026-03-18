package de.flori.exotico.mixin.client;

import de.flori.exotico.config.ExoticoConfig;
import de.flori.exotico.scanner.PlayerScanner;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(method = "isGlowing", at = @At("HEAD"), cancellable = true)
    private void onIsGlowing(CallbackInfoReturnable<Boolean> cir) {
        if (!ExoticoConfig.getInstance().enablePlayerHighlight)
            return;

        if ((Object) this instanceof PlayerEntity player) {
            String name = player.getGameProfile().name();
            if (name != null && PlayerScanner.playersWithExotics.contains(name)) {
                cir.setReturnValue(true);
            }
        }
    }
}