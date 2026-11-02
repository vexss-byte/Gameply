package net.laper.lapermod.shop;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.laper.lapermod.LaperMod;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

/** Jumlah pembelian per pemain: scopeKey -> uuid -> offerId -> jumlah. Disimpan di <world>/lapermod/shop_purchases.json */
final class PurchaseStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private Map<String, Map<String, Map<String, Integer>>> data = new HashMap<>();
    private Path file;

    void load(MinecraftServer server) {
        file = server.getWorldPath(LevelResource.ROOT).resolve("lapermod").resolve("shop_purchases.json");
        data = new HashMap<>();
        try {
            if (Files.exists(file)) {
                Map<String, Map<String, Map<String, Integer>>> raw = GSON.fromJson(
                        Files.readString(file, StandardCharsets.UTF_8),
                        new TypeToken<Map<String, Map<String, Map<String, Integer>>>>() {}.getType());
                if (raw != null) data = raw;
            }
        } catch (Exception e) {
            LaperMod.LOGGER.error("Gagal membaca shop_purchases.json: {}", e.toString());
        }
    }

    int get(String scope, UUID player, String offerId) {
        return data.getOrDefault(scope, Map.of()).getOrDefault(player.toString(), Map.of()).getOrDefault(offerId, 0);
    }

    void increment(String scope, UUID player, String offerId) {
        data.computeIfAbsent(scope, k -> new HashMap<>())
                .computeIfAbsent(player.toString(), k -> new HashMap<>())
                .merge(offerId, 1, Integer::sum);
        save();
    }

    /** Buang data event lama (kecuali scope yang disebut). */
    void retainOnly(Set<String> scopes) {
        if (data.keySet().retainAll(scopes)) save();
    }

    void save() {
        if (file == null) return;
        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling("shop_purchases.json.tmp");
            Files.writeString(tmp, GSON.toJson(data), StandardCharsets.UTF_8);
            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicFail) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            LaperMod.LOGGER.error("Gagal menyimpan shop_purchases.json: {}", e.toString());
        }
    }
}
