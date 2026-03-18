package de.flori.exotico.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import de.flori.exotico.data.ColorCheckResult;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CollectionManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File COLLECTION_FILE = FabricLoader.getInstance().getConfigDir()
            .resolve("exotico_collection.json").toFile();

    private static final Set<String> discoveredKeys = new HashSet<>();
    private static final List<ColorCheckResult> collection = new ArrayList<>();

    private static boolean initialized = false;

    public static void init() {
        if (initialized)
            return;
        load();
        initialized = true;
    }

    public static void load() {
        if (!COLLECTION_FILE.exists())
            return;

        try (FileReader reader = new FileReader(COLLECTION_FILE)) {
            Type listType = new TypeToken<ArrayList<ColorCheckResult>>() {
            }.getType();
            List<ColorCheckResult> loaded = GSON.fromJson(reader, listType);
            if (loaded != null) {
                collection.clear();
                discoveredKeys.clear();
                for (ColorCheckResult res : loaded) {
                    String key = res.hex + "|" + (res.item_id != null ? res.item_id : "");
                    if (!discoveredKeys.contains(key)) {
                        collection.add(res);
                        discoveredKeys.add(key);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(COLLECTION_FILE)) {
            GSON.toJson(collection, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void updateCollection(ColorCheckResult result) {
        if (result == null || !result.is_exotic)
            return;

        String key = result.hex + "|" + (result.item_id != null ? result.item_id : "");
        if (!discoveredKeys.contains(key)) {

            discoveredKeys.add(key);

            if (result.timestamp == 0) {
                result.timestamp = System.currentTimeMillis();
            }
            collection.add(result);
            save();
        }
    }

    public static List<ColorCheckResult> getCollection() {
        return new ArrayList<>(collection);
    }
}