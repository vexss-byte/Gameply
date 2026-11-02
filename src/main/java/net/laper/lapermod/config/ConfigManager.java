package net.laper.lapermod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import net.laper.lapermod.LaperMod;
import net.laper.lapermod.config.OffersConfig.ItemSpec;
import net.laper.lapermod.config.OffersConfig.OfferConfig;
import net.laper.lapermod.config.OffersConfig.RewardSpec;
import net.laper.lapermod.shop.ShopItems;
import net.minecraft.server.MinecraftServer;

/** Memuat config JSON, menulis default bila belum ada, dan hot-reload saat file berubah. */
public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Path DIR = FabricLoader.getInstance().getConfigDir().resolve("lapermod");
    private static final Path MAIN = DIR.resolve("lapermod.json");
    private static final Path OFFERS = DIR.resolve("lapershop_offers.json");

    private static ModConfig config = new ModConfig();
    private static OffersConfig offers = OffersConfig.defaults();
    private static long mainStamp = -1;
    private static long offersStamp = -1;
    private static final List<Runnable> LISTENERS = new ArrayList<>();

    private ConfigManager() {}

    public static ModConfig get() {
        return config;
    }

    public static OffersConfig offers() {
        return offers;
    }

    public static void onReload(Runnable r) {
        LISTENERS.add(r);
    }

    public static void loadAll() {
        loadMain();
        loadOffers();
    }

    public static void loadMain() {
        ModConfig loaded = read(MAIN, ModConfig.class, ModConfig::new);
        if (loaded != null) {
            loaded.sanitize();
            config = loaded;
        }
        mainStamp = stamp(MAIN);
    }

    public static void loadOffers() {
        OffersConfig loaded = read(OFFERS, OffersConfig.class, OffersConfig::defaults);
        if (loaded != null) {
            validate(loaded);
            offers = loaded;
        }
        offersStamp = stamp(OFFERS);
    }

    /** Dipanggil tiap tick server; cek perubahan file tiap 5 detik. */
    public static void tickReload(MinecraftServer server) {
        if (server.getTickCount() % 100 != 0) return;
        boolean changed = false;
        if (stamp(MAIN) != mainStamp) {
            loadMain();
            changed = true;
            LaperMod.LOGGER.info("lapermod.json dimuat ulang.");
        }
        if (stamp(OFFERS) != offersStamp) {
            loadOffers();
            changed = true;
            LaperMod.LOGGER.info("lapershop_offers.json dimuat ulang ({} penawaran aktif).", offers.active.size());
        }
        if (changed) LISTENERS.forEach(Runnable::run);
    }

    private static long stamp(Path p) {
        try {
            return Files.exists(p) ? Files.getLastModifiedTime(p).toMillis() : -1;
        } catch (IOException e) {
            return -1;
        }
    }

    private static <T> T read(Path path, Class<T> type, java.util.function.Supplier<T> defaults) {
        try {
            Files.createDirectories(DIR);
            if (!Files.exists(path)) {
                T d = defaults.get();
                Files.writeString(path, GSON.toJson(d), StandardCharsets.UTF_8);
                return d;
            }
            T v = GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), type);
            return v != null ? v : defaults.get();
        } catch (Exception e) {
            LaperMod.LOGGER.error("Gagal membaca {} - memakai konfigurasi sebelumnya. Penyebab: {}", path.getFileName(), e.toString());
            return null;
        }
    }

    private static void validate(OffersConfig c) {
        List<OfferConfig> ok = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        if (c.offers == null) c.offers = new ArrayList<>();
        int n = 0;
        for (OfferConfig o : c.offers) {
            n++;
            if (o == null || !o.enabled) continue;
            if (o.id == null || o.id.isBlank()) o.id = "offer_" + n;
            if (!ids.add(o.id)) {
                LaperMod.LOGGER.warn("Penawaran '{}' dilewati: id ganda.", o.id);
                continue;
            }
            if (o.payment == null || o.payment.isEmpty() || o.payment.size() > 2) {
                LaperMod.LOGGER.warn("Penawaran '{}' dilewati: payment harus 1-2 item.", o.id);
                continue;
            }
            if (o.rewards == null || o.rewards.isEmpty()) {
                LaperMod.LOGGER.warn("Penawaran '{}' dilewati: rewards kosong.", o.id);
                continue;
            }
            boolean valid = true;
            for (ItemSpec p : o.payment) {
                if (p == null || ShopItems.item(p.item).isEmpty()) {
                    LaperMod.LOGGER.warn("Penawaran '{}' dilewati: item pembayaran tidak dikenal '{}'.", o.id, p == null ? null : p.item);
                    valid = false;
                    break;
                }
                p.count = Math.max(1, Math.min(2304, p.count));
            }
            if (!valid) continue;
            for (RewardSpec r : o.rewards) {
                if (r == null || ShopItems.item(r.item).isEmpty()) {
                    LaperMod.LOGGER.warn("Penawaran '{}' dilewati: item hadiah tidak dikenal '{}' (mod belum terpasang?).", o.id, r == null ? null : r.item);
                    valid = false;
                    break;
                }
                r.count = Math.max(1, Math.min(2304, r.count));
                if (r.lore == null) r.lore = new ArrayList<>();
                if (r.enchantments == null) r.enchantments = new java.util.LinkedHashMap<>();
                if (r.name == null) r.name = "";
            }
            if (!valid) continue;
            o.limitPerPlayer = Math.max(0, o.limitPerPlayer);
            if (o.limitScope == null) o.limitScope = "EVENT";
            ok.add(o);
        }
        c.active = List.copyOf(ok);
    }
}
