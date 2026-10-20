package com.king.ritelock;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Config ada di: <folder game/server>/config/ritelock.json
 * File ini di-reload otomatis (cek tiap 5 detik), jadi bisa diedit saat server jalan.
 */
public class ModConfig {
	private static final Logger LOGGER = LoggerFactory.getLogger("ritelock");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("ritelock.json");

	private static volatile ModConfig instance = new ModConfig();
	private static long lastModified = -1L;

	// ====== Riptide trident ======
	public boolean riptideCooldownEnabled = true;
	/** Lama cooldown dalam DETIK. Boleh desimal, contoh 7.5 */
	public double riptideCooldownSeconds = 15.0;

	// ====== Netherite ======
	public boolean netheriteRestrictionEnabled = true;
	/** Item tambahan yang ikut dilarang masuk storage, contoh "minecraft:ancient_debris" */
	public List<String> extraRestrictedItems = new ArrayList<>();

	// ====== Chat ======
	public boolean chatDisabled = true;
	public String chatWarningText = "Chat Tidak Bisa Digunakan";

	public static ModConfig get() {
		return instance;
	}

	public int riptideCooldownTicks() {
		return (int) Math.round(riptideCooldownSeconds * 20.0);
	}

	public static synchronized void load() {
		try {
			if (Files.exists(PATH)) {
				try (Reader r = Files.newBufferedReader(PATH)) {
					ModConfig c = GSON.fromJson(r, ModConfig.class);
					if (c != null) {
						if (c.extraRestrictedItems == null) c.extraRestrictedItems = new ArrayList<>();
						if (c.chatWarningText == null || c.chatWarningText.isEmpty()) c.chatWarningText = "Chat Tidak Bisa Digunakan";
						instance = c;
					}
				}
			}
			// tulis ulang supaya semua opsi muncul di file
			try (Writer w = Files.newBufferedWriter(PATH)) {
				GSON.toJson(instance, w);
			}
			lastModified = Files.getLastModifiedTime(PATH).toMillis();
		} catch (Exception e) {
			LOGGER.error("[RiteLock] Gagal load config, pakai nilai terakhir/default", e);
		}
	}

	public static void reloadIfChanged() {
		try {
			if (Files.exists(PATH) && Files.getLastModifiedTime(PATH).toMillis() != lastModified) {
				load();
				LOGGER.info("[RiteLock] Config di-reload");
			}
		} catch (Exception ignored) {
		}
	}
}
