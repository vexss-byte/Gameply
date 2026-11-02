package net.laper.lapermod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.laper.lapermod.config.ConfigManager;
import net.laper.lapermod.cooldown.CooldownManager;
import net.laper.lapermod.deaths.DeathCounter;
import net.laper.lapermod.loot.RareDrops;
import net.laper.lapermod.shop.ShopManager;
import net.laper.lapermod.world.GameRuleEnforcer;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LaperMod implements ModInitializer {
    public static final String MOD_ID = "lapermod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static MinecraftServer server;

    public static MinecraftServer server() {
        return server;
    }

    @Override
    public void onInitialize() {
        ConfigManager.loadMain();

        CooldownManager.register();
        RareDrops.register();
        DeathCounter.register();
        GameRuleEnforcer.register();
        ShopManager.register();

        ServerLifecycleEvents.SERVER_STARTED.register(s -> {
            server = s;
            ConfigManager.loadOffers();
            DeathCounter.onServerStarted(s);
            GameRuleEnforcer.apply(s);
            ShopManager.onServerStarted(s);
            LOGGER.info("LaperMod aktif. Penawaran LAPER SHOP aktif: {}", ConfigManager.offers().active.size());
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(s -> {
            DeathCounter.save();
            ShopManager.onServerStopping();
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(s -> server = null);
        ServerTickEvents.END_SERVER_TICK.register(ConfigManager::tickReload);
    }
}
