package net.laper.lapermod.deaths;

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
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.laper.lapermod.LaperMod;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.FixedFormat;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ScoreAccess;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

/**
 * Jumlah kematian disimpan permanen di <world>/lapermod/deaths.json dan ditampilkan di tab list
 * lewat objective scoreboard (slot LIST) dengan format tetap "☠ N" -> klien vanilla cukup.
 */
public final class DeathCounter {
    private static final String OBJECTIVE = "lapermod_deaths";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<UUID, Integer> DEATHS = new HashMap<>();
    private static Path file;

    private DeathCounter() {}

    public static void register() {
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer p) {
                DEATHS.merge(p.getUUID(), 1, Integer::sum);
                save();
                MinecraftServer server = LaperMod.server();
                if (server != null) update(server, p);
            }
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> update(server, handler.getPlayer()));
    }

    public static void onServerStarted(MinecraftServer server) {
        file = server.getWorldPath(LevelResource.ROOT).resolve("lapermod").resolve("deaths.json");
        DEATHS.clear();
        try {
            if (Files.exists(file)) {
                Map<String, Integer> raw = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8),
                        new TypeToken<Map<String, Integer>>() {}.getType());
                if (raw != null) raw.forEach((k, v) -> DEATHS.put(UUID.fromString(k), v));
            }
        } catch (Exception e) {
            LaperMod.LOGGER.error("Gagal membaca deaths.json: {}", e.toString());
        }
        objective(server);
        for (ServerPlayer p : server.getPlayerList().getPlayers()) update(server, p);
    }

    public static int get(UUID id) {
        return DEATHS.getOrDefault(id, 0);
    }

    public static void save() {
        if (file == null) return;
        try {
            Files.createDirectories(file.getParent());
            Map<String, Integer> raw = new HashMap<>();
            DEATHS.forEach((k, v) -> raw.put(k.toString(), v));
            Path tmp = file.resolveSibling("deaths.json.tmp");
            Files.writeString(tmp, GSON.toJson(raw), StandardCharsets.UTF_8);
            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicFail) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            LaperMod.LOGGER.error("Gagal menyimpan deaths.json: {}", e.toString());
        }
    }

    private static Objective objective(MinecraftServer server) {
        Scoreboard sb = server.getScoreboard();
        Objective obj = sb.getObjective(OBJECTIVE);
        if (obj == null) {
            obj = sb.addObjective(OBJECTIVE, ObjectiveCriteria.DUMMY, Component.literal("Kematian"),
                    ObjectiveCriteria.RenderType.INTEGER, true, null);
        }
        sb.setDisplayObjective(DisplaySlot.LIST, obj);
        return obj;
    }

    private static void update(MinecraftServer server, ServerPlayer p) {
        Scoreboard sb = server.getScoreboard();
        Objective obj = objective(server);
        int n = get(p.getUUID());
        ScoreAccess score = sb.getOrCreatePlayerScore(p, obj);
        score.set(n);
        score.numberFormatOverride(new FixedFormat(
                Component.literal("☠ ").withStyle(ChatFormatting.RED)
                        .append(Component.literal(String.valueOf(n)).withStyle(ChatFormatting.WHITE))));
    }
}
