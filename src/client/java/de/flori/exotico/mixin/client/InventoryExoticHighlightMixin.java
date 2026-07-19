package de.flori.exotico.mixin.client;

import de.flori.exotico.api.ExoticAPI;
import de.flori.exotico.client.CollectionManager;
import de.flori.exotico.client.TooltipHandler;
import de.flori.exotico.config.ExoticoConfig;
import de.flori.exotico.data.ColorCheckResult;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public abstract class InventoryExoticHighlightMixin {

    private static net.minecraft.client.gui.screen.Screen lastScreen = null;
    private static final java.util.Set<String> alertedSniperItems = new java.util.HashSet<>();

    //? if !mojmap {
    @Inject(method = "drawSlot(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/screen/slot/Slot;II)V", at = @At("TAIL"))
    private void onDrawSlot(DrawContext context, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        //?} else {
    /*@Inject(method = "extractSlot(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/screen/slot/Slot;II)V", at = @At("TAIL"))
    private void onDrawSlot(DrawContext context, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
    *///?}
        if (!ExoticoConfig.getInstance().enableInventoryHighlight) {
            return;
        }

        ItemStack stack = slot.getStack();
        if (stack.isEmpty()) {
            return;
        }

        DyedColorComponent colorComp = stack.get(DataComponentTypes.DYED_COLOR);
        if (colorComp == null) {
            return;
        }

        int rgb = colorComp.rgb();
        String hex = String.format("%06X", rgb & 0xFFFFFF);

        String itemId = TooltipHandler.extractItemId(stack);
        if (itemId == null) {
            itemId = "";
        }

        if (!TooltipHandler.extractDyeItem(stack).isEmpty()) {
            return;
        }

        String cacheKey = hex + "|" + itemId;
        ColorCheckResult cached = TooltipHandler.colorCache.get(cacheKey);

        if (cached == null) {
            if (!Boolean.TRUE.equals(TooltipHandler.pendingRequests.get(cacheKey))) {
                TooltipHandler.pendingRequests.put(cacheKey, true);

                ExoticAPI.checkColor(hex, itemId)
                        .thenAccept(result -> {
                            ColorCheckResult toCache;
                            if (result != null) {
                                toCache = result;
                                if (result.is_exotic) {
                                    CollectionManager.updateCollection(result);
                                }
                            } else {
                                toCache = new ColorCheckResult();
                                toCache.is_exotic = false;
                                toCache.category = "none";
                            }
                            TooltipHandler.colorCache.put(cacheKey, toCache);
                            TooltipHandler.pendingRequests.remove(cacheKey);
                        })
                        .exceptionally(ex -> {
                            TooltipHandler.pendingRequests.remove(cacheKey);
                            return null;
                        });
            }
            return;
        }

        if (!cached.is_exotic) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        //? if !guiScreenHolder {
        if (client.currentScreen != lastScreen) {
            alertedSniperItems.clear();
            lastScreen = client.currentScreen;
        }
        //?} else {
        /*if (client.gui.screen() != lastScreen) {
            alertedSniperItems.clear();
            lastScreen = client.gui.screen();
        }*/
        //?}

        if (ExoticoConfig.getInstance().enableAhSniper) {
            String alertKey = slot.id + "_" + hex;
            if (!alertedSniperItems.contains(alertKey)) {
                alertedSniperItems.add(alertKey);
                if (client.player != null) {
                    // Play a distinct alert sound and send a chat message
                    client.player.playSound(net.minecraft.sound.SoundEvents.ENTITY_WITHER_SPAWN, 0.5f, 2.0f);
                    //? if !mojmap {
                    client.player.sendMessage(net.minecraft.text.Text.literal("§c§l[EXOTICO SNIPER] §eExotic item detected in GUI!").formatted(net.minecraft.util.Formatting.BOLD), false);
                    //?} else {
                    /*client.player.sendSystemMessage(net.minecraft.text.Text.literal("§c§l[EXOTICO SNIPER] §eExotic item detected in GUI!").formatted(net.minecraft.util.Formatting.BOLD));*/
                    //?}
                }
            }
        }

        int slotX = slot.x;
        int slotY = slot.y;

        String category = (cached.category != null ? cached.category.toLowerCase() : "exotic");

        int highlightColor = switch (category) {
            case "glitched" -> 0xFFAA00FF;
            case "og_fairy" -> 0xFFFF66FF;
            case "fairy_dyed" -> 0xFFFF88FF;
            case "crystal_dyed" -> 0xFF00DDFF;
            case "bleached" -> 0xFFFFFFFF;
            default -> 0xFFFFAA00;
        };

        int bgColor = (highlightColor & 0x00FFFFFF) | 0x60000000;

        context.fill(slotX, slotY, slotX + 16, slotY + 16, bgColor);

        context.fill(slotX, slotY - 1, slotX + 16, slotY, highlightColor);
        context.fill(slotX, slotY + 16, slotX + 16, slotY + 17, highlightColor);
        context.fill(slotX - 1, slotY - 1, slotX, slotY + 17, highlightColor);
        context.fill(slotX + 16, slotY - 1, slotX + 17, slotY + 17, highlightColor);
    }
}