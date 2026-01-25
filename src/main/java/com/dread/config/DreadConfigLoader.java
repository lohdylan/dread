package com.dread.config;

import com.dread.DreadMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class DreadConfigLoader {
    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .serializeNulls()
        .disableHtmlEscaping()
        .create();

    private static DreadConfig instance;

    public static DreadConfig load() {
        if (instance != null) return instance;

        Path configPath = FabricLoader.getInstance().getConfigDir().resolve("dread.json");

        if (Files.exists(configPath)) {
            try (Reader reader = Files.newBufferedReader(configPath)) {
                instance = GSON.fromJson(reader, DreadConfig.class);
                if (instance == null) {
                    instance = new DreadConfig();
                }
                validate();
                save(); // Write back clamped values
                DreadMod.LOGGER.info("Loaded config from {}", configPath);
            } catch (Exception e) {
                DreadMod.LOGGER.error("Failed to load config, using defaults", e);
                instance = new DreadConfig();
                save();
            }
        } else {
            instance = new DreadConfig();
            save(); // Create default config
            DreadMod.LOGGER.info("Created default config at {}", configPath);
        }

        return instance;
    }

    public static DreadConfig getConfig() {
        if (instance == null) {
            return load();
        }
        return instance;
    }

    private static void validate() {
        // Clamp spawn rates (0.0 to 1.0)
        instance.baseSpawnChancePerSecond = Math.max(0.0f, Math.min(1.0f, instance.baseSpawnChancePerSecond));
        instance.miningBonusPerBlock = Math.max(0.0f, Math.min(0.1f, instance.miningBonusPerBlock));
        instance.dayEscalationCap = Math.max(1, Math.min(100, instance.dayEscalationCap));

        // Clamp damage (0.0 to 100.0)
        instance.dreadAttackDamage = Math.max(0.0f, Math.min(100.0f, instance.dreadAttackDamage));
    }

    private static void save() {
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve("dread.json");
        try (Writer writer = Files.newBufferedWriter(configPath)) {
            GSON.toJson(instance, writer);
        } catch (Exception e) {
            DreadMod.LOGGER.error("Failed to save config", e);
        }
    }
}
