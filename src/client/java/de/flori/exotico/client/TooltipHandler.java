package de.flori.exotico.client;

import de.flori.exotico.api.ExoticAPI;
import de.flori.exotico.config.ExoticoConfig;
import de.flori.exotico.data.ColorCheckResult;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class TooltipHandler {

    public static final Map<String, ColorCheckResult> colorCache = new ConcurrentHashMap<>();
    public static final Map<String, Boolean> pendingRequests = new ConcurrentHashMap<>();

    public static void init() {
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
            if (!ExoticoConfig.getInstance().enableTooltips)
                return;

            DyedColorComponent colorComponent = stack.get(DataComponentTypes.DYED_COLOR);
            if (colorComponent == null)
                return;

            int color = colorComponent.rgb();
            String hex = String.format("%06X", color);
            String itemId = extractItemId(stack);
            String cacheKey = hex + "|" + itemId;

            lines.add(Text.empty());
            lines.add(Text.literal("⬛ Color: ").formatted(Formatting.GOLD)
                    .append(Text.literal("#" + hex).formatted(Formatting.WHITE)));

            String dye = extractDyeItem(stack);
            if (!dye.isEmpty()) {
                String cleanDye = dye.replace("DYE_", "").replace("_", " ");
                cleanDye = cleanDye.substring(0, 1).toUpperCase() + cleanDye.substring(1).toLowerCase();
                lines.add(Text.literal("✿ " + cleanDye + " Dyed").formatted(Formatting.GOLD));
                return;
            }

            ColorCheckResult cached = colorCache.get(cacheKey);
            if (cached != null) {

                appendColorResult(lines, cached);
            } else if (!pendingRequests.getOrDefault(cacheKey, false)) {

                pendingRequests.put(cacheKey, true);
                ExoticAPI.checkColor(hex, itemId)
                        .thenAccept(result -> {
                            if (result != null) {
                                colorCache.put(cacheKey, result);
                                if (result.is_exotic) {
                                    CollectionManager.updateCollection(result);
                                }
                            } else {

                                ColorCheckResult none = new ColorCheckResult();
                                none.is_exotic = false;
                                none.variant = "None";
                                none.category = "none";
                                none.description = "Not a known exotic color.";
                                colorCache.put(cacheKey, none);
                            }
                            pendingRequests.remove(cacheKey);
                        })
                        .exceptionally(e -> {
                            pendingRequests.remove(cacheKey);
                            return null;
                        });

                lines.add(Text.literal("⏳ Checking color...").formatted(Formatting.GRAY));
            } else {
                lines.add(Text.literal("⏳ Checking color...").formatted(Formatting.GRAY));
            }
        });
    }

    private static void appendColorResult(java.util.List<Text> lines, ColorCheckResult result) {
        if (result.is_exotic) {

            Formatting variantColor;
            String icon;
            switch (result.category != null ? result.category : "exotic") {
                case "glitched" -> {
                    variantColor = Formatting.DARK_PURPLE;
                    icon = "✦";
                }
                case "og_fairy" -> {
                    variantColor = Formatting.LIGHT_PURPLE;
                    icon = "✧";
                }
                case "fairy_dyed" -> {
                    variantColor = Formatting.LIGHT_PURPLE;
                    icon = "✦";
                }
                case "crystal_dyed" -> {
                    variantColor = Formatting.AQUA;
                    icon = "◆";
                }
                case "bleached" -> {
                    variantColor = Formatting.WHITE;
                    icon = "❆";
                }
                case "exotic" -> {
                    variantColor = Formatting.GOLD;
                    icon = "⭐";
                }
                default -> {
                    variantColor = Formatting.YELLOW;
                    icon = "✴";
                }
            }

            lines.add(Text.literal(icon + " ").formatted(variantColor)
                    .append(Text.literal("EXOTIC").formatted(Formatting.BOLD).formatted(variantColor))
                    .append(Text.literal(" — " + (result.variant != null ? result.variant : "Unknown Variant"))
                            .formatted(variantColor)));

            if (result.description != null && !result.description.isEmpty()) {
                lines.add(Text.literal("  " + result.description).formatted(Formatting.GRAY)
                        .formatted(Formatting.ITALIC));
            }
        } else if ("normal".equalsIgnoreCase(result.category)) {

            lines.add(Text.literal("✓ Standard Color").formatted(Formatting.GREEN));
            lines.add(Text.literal("  This color is the default for this item.").formatted(Formatting.DARK_GRAY)
                    .formatted(Formatting.ITALIC));
        } else {

            lines.add(Text.literal("✗ Not an exotic color").formatted(Formatting.RED));
            lines.add(Text.literal("  This color matches no known exotic variant.").formatted(Formatting.DARK_GRAY)
                    .formatted(Formatting.ITALIC));
        }
    }

    public static String extractItemId(ItemStack stack) {
        return extractTagString(stack, "id");
    }

    public static String extractDyeItem(ItemStack stack) {
        return extractTagString(stack, "dye_item");
    }

    private static String extractTagString(ItemStack stack, String key) {
        try {
            var customData = stack.get(DataComponentTypes.CUSTOM_DATA);
            if (customData == null)
                return "";
            NbtCompound nbt = customData.copyNbt();

            var val = nbt.getString(key);
            if (val.isPresent() && !val.get().isBlank()) {
                return val.get();
            }

            var extraOpt = nbt.getCompound("ExtraAttributes");
            if (extraOpt.isPresent()) {
                var legacyVal = extraOpt.get().getString(key);
                if (legacyVal.isPresent() && !legacyVal.get().isBlank()) {
                    return legacyVal.get();
                }
            }
        } catch (Exception ignored) {
        }
        return "";
    }
}