package de.flori.exotico.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ExoticoConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("exotico.json").toFile();

    private static ExoticoConfig instance;

    public String apiKey = "";
    public boolean autoScan = false;
    public long scanCooldown = 300000;
    public String discordWebhook = "";
    public boolean enableSounds = true;
    public boolean enableTooltips = true;
    public boolean enablePlayerHighlight = true;
    public boolean enableHudOverlay = true;
    public boolean enableInventoryHighlight = true;
    public boolean enableWavyCapes = true;


    public boolean enableAutoMsg = false;
    public String autoMsgTemplate = "Hey {name}, nice exotic armor! Wanna trade? :)";
    public int autoMsgMinLevel = 0;
    public int autoMsgMaxLevel = 999;
    public long autoMsgCooldown = 60000;


    public String adminSecret = "";

    public static ExoticoConfig getInstance() {
        if (instance == null) {
            load();
        }
        return instance;
    }

    public static void load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                instance = GSON.fromJson(reader, ExoticoConfig.class);
            } catch (IOException e) {
                e.printStackTrace();
                instance = new ExoticoConfig();
            }
        } else {
            instance = new ExoticoConfig();
            save();
        }
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(instance, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}