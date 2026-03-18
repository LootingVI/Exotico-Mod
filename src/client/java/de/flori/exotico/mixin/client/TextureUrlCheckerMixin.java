package de.flori.exotico.mixin.client;

import com.mojang.authlib.yggdrasil.TextureUrlChecker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = TextureUrlChecker.class, remap = false)
public class TextureUrlCheckerMixin {
    @Inject(method = "isAllowedTextureDomain", at = @At("HEAD"), cancellable = true)
    private static void allowAnyDomain(String url, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(true);
    }
}