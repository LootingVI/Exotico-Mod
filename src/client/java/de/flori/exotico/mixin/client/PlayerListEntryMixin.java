package de.flori.exotico.mixin.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import de.flori.exotico.api.UserAPI;
import de.flori.exotico.client.ExoticoCapeAsset;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collection;

@Mixin(PlayerListEntry.class)
public class PlayerListEntryMixin {


    @ModifyArg(method = "texturesSupplier", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/texture/PlayerSkinProvider;supplySkinTextures(Lcom/mojang/authlib/GameProfile;Z)Ljava/util/function/Supplier;"))
    private static boolean forceDisableSecureSkins(boolean requireSecure) {
        return false;
    }

    @Shadow
    @Final
    private GameProfile profile;

    @Inject(method = "getSkinTextures", at = @At("RETURN"), cancellable = true)
    public void onGetSkinTextures(CallbackInfoReturnable<SkinTextures> cir) {
        if (this.profile == null || this.profile.name() == null)
            return;

        String name = this.profile.name().trim();
        if (name.isEmpty())
            return;


        String rank = UserAPI.getRawRank(name);
        if ("VIP".equals(rank) || "MOD".equals(rank) || "ADMIN".equals(rank) || "DEV".equals(rank)) {
            Identifier capeTexture = UserAPI.getCapeTexture(name);
            if (capeTexture != null) {
                SkinTextures original = cir.getReturnValue();


                ExoticoCapeAsset customCapeAsset = new ExoticoCapeAsset(capeTexture);

                SkinTextures newTextures = new SkinTextures(
                        original.body(),
                        customCapeAsset,
                        original.elytra(),
                        original.model(),
                        original.secure());
                cir.setReturnValue(newTextures);
            }
        }
    }
}