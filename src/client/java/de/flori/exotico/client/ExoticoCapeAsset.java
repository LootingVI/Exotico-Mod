package de.flori.exotico.client;

import net.minecraft.util.AssetInfo;
import net.minecraft.util.Identifier;

public record ExoticoCapeAsset(Identifier texturePath) implements AssetInfo.TextureAsset {
    @Override
    public Identifier id() {
        return texturePath;
    }
}