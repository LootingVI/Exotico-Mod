plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "1.21.11"

stonecutter parameters {
    // true on every version except 1.21.11 (the only one that still has Yarn mappings).
    val mojmap = current.version != "1.21.11"
    // 26.2 removed Minecraft.screen/setScreen(Screen) entirely, moving current-screen tracking to
    // the new Minecraft.gui (Gui) holder: Minecraft.getInstance().gui.screen() / .gui.setScreen(...)
    // (verified via javap on the real 26.2 jar - Minecraft has no Screen field/getter at all
    // anymore, and Gui.screen()/Gui.setScreen(Screen) do what Minecraft.screen/setScreen used to).
    // 26.1.2 still has the old direct accessors, so this is its own boundary.
    val guiScreenHolder = mojmap && current.parsed >= "26.2"
    constants["mojmap"] = mojmap
    constants["guiScreenHolder"] = guiScreenHolder

    // Verified against the real remapped jars (javap on minecraft-merged-deobf / the project's
    // own loom-cache jars), not guessed. Structural API changes (new HUD registry, input record
    // types, Screen.render -> extractRenderState, close -> onClose, shouldPause -> isPauseScreen,
    // sendMessage(Component,boolean) -> sendSystemMessage/sendOverlayMessage) are handled with
    // explicit //? if blocks in the affected files instead, since those aren't 1:1 token swaps.
    replacements {
        string(mojmap) {
            // import paths / fully qualified names (safe as exact substrings)
            replace("net.minecraft.text.Text.literal", "net.minecraft.network.chat.Component.literal")
            replace("net.minecraft.client.gui.screen.Screen", "net.minecraft.client.gui.screens.Screen")
            replace("net.minecraft.client.gl.RenderPipelines", "net.minecraft.client.renderer.RenderPipelines")
            replace("net.minecraft.client.texture.NativeImageBackedTexture", "net.minecraft.client.renderer.texture.DynamicTexture")
            replace("net.minecraft.client.texture.NativeImage", "com.mojang.blaze3d.platform.NativeImage")
            replace("net.minecraft.component.DataComponentTypes", "net.minecraft.core.component.DataComponents")
            replace("net.minecraft.component.type.DyedColorComponent", "net.minecraft.world.item.component.DyedItemColor")
            replace("net.minecraft.item.Item", "net.minecraft.world.item.Item")
            replace("net.minecraft.item.Items", "net.minecraft.world.item.Items")
            replace("net.minecraft.text.Text", "net.minecraft.network.chat.Component")
            // bare "Text" as a standalone type (not "*Text*" inside words like capeTextures,
            // getTextureManager, NativeImageBackedTexture - those have their own exact rules).
            replace("<Text>", "<Component>")
            replace("Text.empty(", "Component.empty(")
            replace("Text displayName", "Component displayName")
            replace("Text msg", "Component msg")
            replace("net.minecraft.client.render.RenderTickCounter", "net.minecraft.client.DeltaTracker")
            replace("net.minecraft.util.Formatting", "net.minecraft.ChatFormatting")
            replace("net.minecraft.util.AssetInfo", "net.minecraft.core.ClientAsset")
            replace("net.minecraft.nbt.NbtCompound", "net.minecraft.nbt.CompoundTag")
            replace("net.minecraft.client.render.entity.state.EntityRenderState", "net.minecraft.client.renderer.entity.state.EntityRenderState")
            replace("net.minecraft.client.render.entity.EntityRenderer", "net.minecraft.client.renderer.entity.EntityRenderer")
            replace("net.minecraft.entity.player.PlayerEntity", "net.minecraft.world.entity.player.Player")
            replace("net.minecraft.entity.Entity;", "net.minecraft.world.entity.Entity;")
            replace("net.minecraft.client.gui.screen.ingame.HandledScreen", "net.minecraft.client.gui.screens.inventory.AbstractContainerScreen")
            replace("net.minecraft.screen.slot.Slot", "net.minecraft.world.inventory.Slot")
            replace("net/minecraft/screen/slot/Slot", "net/minecraft/world/inventory/Slot")
            replace("net.minecraft.sound.SoundEvents", "net.minecraft.sounds.SoundEvents")
            replace("net.minecraft.entity.player.SkinTextures", "net.minecraft.world.entity.player.PlayerSkin")
            // Cape/wavy-cape mixin: whole rendering pipeline rewrite, verified via javap on
            // CapeLayer/PoseStack/VertexConsumer/AvatarRenderState in the real 26.1.2 jar.
            // RenderLayer/RenderLayers -> RenderType/RenderTypes handled manually per-file in
            // WavyCapeMixin.java with //? if blocks instead of a global rule (only one file uses it).
            replace("net.minecraft.client.render.VertexConsumer", "com.mojang.blaze3d.vertex.VertexConsumer")
            replace("net.minecraft.client.render.command.OrderedRenderCommandQueue", "net.minecraft.client.renderer.SubmitNodeCollector")
            replace("net.minecraft.client.render.entity.feature.CapeFeatureRenderer", "net.minecraft.client.renderer.entity.layers.CapeLayer")
            replace("net.minecraft.client.render.entity.state.PlayerEntityRenderState", "net.minecraft.client.renderer.entity.state.AvatarRenderState")
            replace("net.minecraft.client.util.math.MatrixStack.Entry", "com.mojang.blaze3d.vertex.PoseStack.Pose")
            replace("net.minecraft.client.util.math.MatrixStack", "com.mojang.blaze3d.vertex.PoseStack")
            replace("net.minecraft.util.math.RotationAxis", "com.mojang.math.Axis")
            replace("net.minecraft.scoreboard.Team", "net.minecraft.world.scores.PlayerTeam")
            replace("net.minecraft.text.ClickEvent", "net.minecraft.network.chat.ClickEvent")
            replace("net.minecraft.text.HoverEvent", "net.minecraft.network.chat.HoverEvent")
            replace("method = \"render\"", "method = \"submit\"")
            // Yarn's PlayerListEntry#texturesSupplier calls the external PlayerSkinProvider to
            // build the skin lookup; Mojmap's PlayerInfo#createSkinLookup does the equivalent
            // work inline, calling Minecraft.getSkinManager().createLookup(profile, requireSecure)
            // directly (verified via javap -c on the real jar, not guessed).
            replace(
                "Lnet/minecraft/client/texture/PlayerSkinProvider;supplySkinTextures(Lcom/mojang/authlib/GameProfile;Z)Ljava/util/function/Supplier;",
                "Lnet/minecraft/client/resources/SkinManager;createLookup(Lcom/mojang/authlib/GameProfile;Z)Ljava/util/function/Supplier;"
            )
            replace("texturesSupplier", "createSkinLookup")
            replace("getSkinTextures", "getSkin")
            // PlayerListEntry -> PlayerInfo bare-word usages are NOT replaced globally: it's a
            // substring of our own PlayerListEntryMixin class name, so that rename is done
            // per-file with //? if blocks instead.
            replace("net.minecraft.client.network.PlayerListEntry", "net.minecraft.client.multiplayer.PlayerInfo")

            // class / member renames
            replace("MinecraftClient", "Minecraft")
            replace("RenderTickCounter", "DeltaTracker")
            replace("textRenderer", "font")
            replace("drawTexture", "blit")
            replace("getWidth", "width")
            replace("close(", "onClose(")
            replace("shouldPause", "isPauseScreen")
            replace("Text.literal", "Component.literal")
            replace("DataComponentTypes", "DataComponents")
            replace("DyedColorComponent", "DyedItemColor")
            replace("Formatting.", "ChatFormatting.")
            replace(".formatted(", ".withStyle(")
            replace("AssetInfo.TextureAsset", "ClientAsset.Texture")
            replace("currentScreen", "screen")
            replace("getNetworkHandler", "getConnection")
            replace("getPlayerList()", "getOnlinePlayers()")
            replace("getServerInfo", "getServerData")
            replace("getUuidAsString", "getStringUUID")
            replace("destroyTexture", "release")
            replace("registerTexture", "register")
            replace("NativeImageBackedTexture", "DynamicTexture")
            replace("NbtCompound", "CompoundTag")
            replace("copyNbt", "copyTag")
            replace("PlayerEntity player", "Player player")
            replace("updateRenderState", "extractRenderState")
            replace("state.displayName", "state.nameTag")
            replace("HandledScreen", "AbstractContainerScreen")
            replace("getStack()", "getItem()")
            replace("slot.id", "slot.index")
            replace("SkinTextures", "PlayerSkin")
            replace("PlayerEntityRenderState", "AvatarRenderState")
            replace("CapeFeatureRenderer", "CapeLayer")
            replace("MatrixStack.Entry", "PoseStack.Pose")
            replace("MatrixStack", "PoseStack")
            replace("RotationAxis", "Axis")
            replace("getPositionMatrix", "pose")
            replace(".vertex(", ".addVertex(")
            replace(".color(", ".setColor(")
            replace(".texture(", ".setUv(")
            replace(".overlay(", ".setOverlay(")
            replace(".light(", ".setLight(")
            replace(".normal(", ".setNormal(")
            replace("matrices.multiply(", "matrices.mulPose(")
            replace(".push()", ".pushPose()")
            replace(".pop()", ".popPose()")
            replace("RotationAxis.POSITIVE_X", "Axis.XP")
            replace("RotationAxis.POSITIVE_Y", "Axis.YP")
            replace("RotationAxis.POSITIVE_Z", "Axis.ZP")
            replace("getScaledWidth", "getGuiScaledWidth")
            replace("getInstance().keyboard", "getInstance().keyboardHandler")
            replace("runDirectory", "gameDirectory")
            replace("Formatting variantColor", "ChatFormatting variantColor")
            replace("SoundEvents.ENTITY_WITHER_SPAWN", "SoundEvents.WITHER_SPAWN")
            replace("SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP", "SoundEvents.EXPERIENCE_ORB_PICKUP")
            // Fully-qualified inline usage (no import) overlaps with the package-rename rule above,
            // so the bare "SoundEvents.ENTITY_WITHER_SPAWN" rule never gets a chance to match here -
            // needs its own atomic rule covering the whole qualified name.
            replace("net.minecraft.sound.SoundEvents.ENTITY_WITHER_SPAWN", "net.minecraft.sounds.SoundEvents.WITHER_SPAWN")
            replace(".getServerInfo().address", ".getServerData().ip")
            replace("state.skinTextures", "state.skin")
            replace("state.capeVisible", "state.showCape")
            replace("state.invisible", "state.isInvisibleToPlayer")
            replace("limbSwingAmplitude", "walkAnimationSpeed")
            replace("limbSwingAnimationProgress", "walkAnimationPos")
            replace("submitCustom(", "submitCustomGeometry(")
            replace("OrderedRenderCommandQueue", "SubmitNodeCollector")
            replace("entry.getDisplayName", "entry.getTabListDisplayName")
            replace("client.world", "client.level")
            replace("getScoreHolderTeam", "getPlayersTeam")
            replace("Team.decorateName", "PlayerTeam.formatNameForTeam")
            replace("Team", "PlayerTeam")
            replace("getPrefix()", "getPlayerPrefix()")
            replace("getSuffix()", "getPlayerSuffix()")
            replace("networkHandler", "connection")
            replace("sendChatCommand", "sendCommand")
            replace("sendChatMessage", "sendChat")

            // Identifier/ResourceLocation rename: run the ".of(" call-site rule BEFORE the bare
            // class-name rule so the method rename doesn't get double-touched by the bare rule.
            replace("Identifier.of(", "Identifier.fromNamespaceAndPath(")
            replace("net.minecraft.util.Identifier", "net.minecraft.resources.Identifier")
            replace("DrawContext", "GuiGraphicsExtractor")
            replace("ClientCommandManager", "ClientCommands")
        }
    }
}
