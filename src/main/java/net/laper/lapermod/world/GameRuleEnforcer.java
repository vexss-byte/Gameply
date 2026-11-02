package net.laper.lapermod.world;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.laper.lapermod.LaperMod;
import net.laper.lapermod.config.ConfigManager;
import net.laper.lapermod.config.ModConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;

/**
 * Menonaktifkan koordinat (reduced_debug_info -> F3 tanpa XYZ) dan Locator Bar (locator_bar=false).
 * Dijalankan via command gamerule agar tidak bergantung pada perubahan API GameRules antar versi,
 * dan diterapkan ulang berkala sehingga tidak bisa dinyalakan lagi lewat /gamerule.
 */
public final class GameRuleEnforcer {
    private static boolean warnedCoords;
    private static boolean warnedLocator;

    private GameRuleEnforcer() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 200 == 0) apply(server);
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> apply(server));
    }

    public static void apply(MinecraftServer server) {
        ModConfig.Restrictions r = ConfigManager.get().restrictions;
        if (r.disableCoordinates) {
            if (!set(server, "true", "reduced_debug_info", "reducedDebugInfo") && !warnedCoords) {
                warnedCoords = true;
                LaperMod.LOGGER.warn("Gamerule reduced_debug_info tidak ditemukan - koordinat tidak dapat dinonaktifkan.");
            }
        }
        if (r.disableLocatorBar) {
            if (!set(server, "false", "locator_bar", "locatorBar") && !warnedLocator) {
                warnedLocator = true;
                LaperMod.LOGGER.warn("Gamerule locator_bar tidak ditemukan - Locator Bar tidak dapat dinonaktifkan.");
            }
        }
    }

    private static boolean set(MinecraftServer server, String value, String... names) {
        CommandSourceStack source = server.createCommandSourceStack().withSuppressedOutput();
        for (String name : names) {
            try {
                server.getCommands().getDispatcher().execute("gamerule " + name + " " + value, source);
                return true;
            } catch (CommandSyntaxException ignored) {
                // coba nama alternatif
            }
        }
        return false;
    }
}
