package net.laper.lapermod.shop;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.laper.lapermod.LaperMod;
import net.laper.lapermod.config.ConfigManager;
import net.laper.lapermod.config.ModConfig;
import net.laper.lapermod.config.OffersConfig.ItemSpec;
import net.laper.lapermod.config.OffersConfig.OfferConfig;
import net.laper.lapermod.config.OffersConfig.RewardSpec;
import net.laper.lapermod.util.Text;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Prediction;
import net.minecraft.world.BossEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Event LAPER SHOP: jadwal waktu nyata, boss bar, pengumuman, validasi & transaksi di sisi server. */
public final class ShopManager {
    public enum Status { OK, CLOSED, LIMIT, FUNDS }

    private static final PurchaseStore STORE = new PurchaseStore();
    private static ShopSchedule schedule = new ShopSchedule("UTC", List.of("12:00"), 120);
    private static ServerBossEvent bar;
    private static ShopSchedule.Window active;
    private static boolean firstTick = true;

    private ShopManager() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(ShopManager::tick);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (active != null && bar != null) {
                bar.addPlayer(handler.getPlayer());
                handler.getPlayer().sendSystemMessage(announcement(ConfigManager.get().shop.openMessage));
                handler.getPlayer().sendSystemMessage(Text.legacy(ConfigManager.get().shop.openHint, false));
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            if (bar != null) bar.removePlayer(handler.getPlayer());
        });
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> ShopCommand.register(dispatcher));
        ConfigManager.onReload(ShopManager::rebuildSchedule);
    }

    public static void onServerStarted(MinecraftServer server) {
        STORE.load(server);
        rebuildSchedule();
    }

    public static void onServerStopping() {
        STORE.save();
    }

    private static void rebuildSchedule() {
        ModConfig.Shop c = ConfigManager.get().shop;
        schedule = new ShopSchedule(c.timezone, c.startTimes, c.durationMinutes);
    }

    // ---------------------------------------------------------------- status

    public static boolean isOpen() {
        return active != null;
    }

    private static String scopeKey(OfferConfig o) {
        if ("ALWAYS".equalsIgnoreCase(o.limitScope) || active == null) return "ALWAYS";
        return "EVENT-" + active.start().getEpochSecond();
    }

    // ---------------------------------------------------------------- tick

    private static void tick(MinecraftServer server) {
        if (server.getTickCount() % 20 != 0) return;
        ModConfig.Shop cfg = ConfigManager.get().shop;
        Instant now = Instant.now();

        ShopSchedule.Window w;
        if (!cfg.enabled) {
            w = null;
        } else if (cfg.alwaysOpenForTesting) {
            w = new ShopSchedule.Window(Instant.EPOCH, now.plusSeconds(cfg.durationMinutes * 60L));
        } else {
            w = schedule.activeAt(now).orElse(null);
        }

        if (w != null && (active == null || !active.start().equals(w.start()))) {
            if (active != null) end(server, false);
            active = w;
            start(server, !firstTick);
        } else if (w == null && active != null) {
            end(server, true);
            active = null;
        } else if (w != null) {
            active = w;
        }
        firstTick = false;

        if (active != null) updateBar(server, cfg);
    }

    private static void start(MinecraftServer server, boolean announce) {
        STORE.retainOnly(Set.of("ALWAYS", "EVENT-" + active.start().getEpochSecond()));
        if (bar == null) {
            bar = new ServerBossEvent(UUID.randomUUID(), Component.literal("LAPERSHOP"), BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.PROGRESS);
        }
        bar.setVisible(true);
        for (ServerPlayer p : server.getPlayerList().getPlayers()) bar.addPlayer(p);
        if (announce) {
            ModConfig.Shop cfg = ConfigManager.get().shop;
            server.getPlayerList().broadcastSystemMessage(announcement(cfg.openMessage), false);
            server.getPlayerList().broadcastSystemMessage(Text.legacy(cfg.openHint, false), false);
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                p.playSound(SoundEvents.PLAYER_LEVELUP, 0.8f, 1.0f);
            }
        }
        LaperMod.LOGGER.info("LAPER SHOP dibuka sampai {}", active.end());
    }

    private static void end(MinecraftServer server, boolean announce) {
        if (bar != null) {
            bar.removeAllPlayers();
            bar.setVisible(false);
        }
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (p.containerMenu instanceof ShopMenu) p.closeContainer();
        }
        if (announce) {
            server.getPlayerList().broadcastSystemMessage(announcement(ConfigManager.get().shop.closeMessage), false);
        }
        LaperMod.LOGGER.info("LAPER SHOP ditutup.");
    }

    private static void updateBar(MinecraftServer server, ModConfig.Shop cfg) {
        long totalMs = Math.max(1, Duration.ofMinutes(cfg.durationMinutes).toMillis());
        long remainingMs = Math.max(0, Duration.between(Instant.now(), active.end()).toMillis());
        int from = Text.parseColor(cfg.gradientStart, 0xFFFF55);
        int to = Text.parseColor(cfg.gradientEnd, 0xFF8800);
        Component title = Text.gradient(cfg.barLabel, from, to, true)
                .append(Component.literal(" | ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(Text.formatTime(remainingMs / 1000)).withStyle(ChatFormatting.WHITE));
        bar.setName(title);
        bar.setProgress((float) Math.max(0.0, Math.min(1.0, remainingMs / (double) totalMs)));
        for (ServerPlayer p : server.getPlayerList().getPlayers()) bar.addPlayer(p); // idempotent
    }

    private static Component announcement(String text) {
        ModConfig.Shop cfg = ConfigManager.get().shop;
        return Text.gradient(text, Text.parseColor(cfg.gradientStart, 0xFFFF55), Text.parseColor(cfg.gradientEnd, 0xFF8800), true);
    }

    // ---------------------------------------------------------------- buka GUI & transaksi

    public static long remainingSeconds() {
        if (active == null) return 0;
        return Math.max(0, Duration.between(Instant.now(), active.end()).toSeconds());
    }

    public static void openFor(ServerPlayer p) {
        if (!isOpen()) {
            ModConfig.Shop cfg = ConfigManager.get().shop;
            p.sendSystemMessage(Text.legacy(cfg.closedCommandMessage, false));
            schedule.nextStart(Instant.now()).ifPresent(next -> p.sendSystemMessage(Text.legacy(
                    cfg.nextOpenFormat.replace("{time}", next.format(DateTimeFormatter.ofPattern("HH:mm")))
                            .replace("{zone}", schedule.zone().getId()), false)));
            return;
        }
        if (ConfigManager.offers().active.isEmpty()) {
            p.sendSystemMessage(Component.literal("Belum ada barang yang dijual.").withStyle(ChatFormatting.RED));
            return;
        }
        ShopMenu.open(p, ConfigManager.offers().active);
    }

    private static boolean plain() {
        return ConfigManager.get().shop.plainPaymentOnly;
    }

    /** Total kebutuhan per item (jaga-jaga bila dua slot pembayaran memakai item yang sama). */
    private static Map<Item, Integer> needs(OfferConfig o) {
        Map<Item, Integer> m = new HashMap<>();
        for (ItemSpec s : o.payment) ShopItems.item(s.item).ifPresent(i -> m.merge(i, s.count, Integer::sum));
        return m;
    }

    public static int used(ServerPlayer p, OfferConfig o) {
        return STORE.get(scopeKey(o), p.getUUID(), o.id);
    }

    public static Status status(ServerPlayer p, OfferConfig o) {
        if (!isOpen()) return Status.CLOSED;
        if (o.limitPerPlayer > 0 && used(p, o) >= o.limitPerPlayer) return Status.LIMIT;
        for (Map.Entry<Item, Integer> e : needs(o).entrySet()) {
            if (ShopItems.count(p, e.getKey(), plain()) < e.getValue()) return Status.FUNDS;
        }
        return Status.OK;
    }

    /** Semua validasi dilakukan di server; klien hanya mengirim klik slot. */
    public static void purchase(ServerPlayer p, OfferConfig o) {
        Status s = status(p, o);
        switch (s) {
            case CLOSED -> {
                p.sendSystemMessage(Text.legacy(ConfigManager.get().shop.closedCommandMessage, false));
                p.closeContainer();
                return;
            }
            case LIMIT -> {
                p.sendSystemMessage(Component.literal("Batas pembelian untuk penawaran ini sudah tercapai.").withStyle(ChatFormatting.RED));
                p.playSound(SoundEvents.VILLAGER_NO, 1f, 1f);
                return;
            }
            case FUNDS -> {
                p.sendSystemMessage(Component.literal("Item pembayaran kamu tidak cukup.").withStyle(ChatFormatting.RED));
                p.playSound(SoundEvents.VILLAGER_NO, 1f, 1f);
                return;
            }
            case OK -> { }
        }

        // Siapkan semua hadiah dulu agar kesalahan config tidak memakan pembayaran pemain.
        List<ItemStack> rewards = new ArrayList<>();
        for (RewardSpec r : o.rewards) {
            ItemStack st = ShopItems.reward(p.level().getServer(), r);
            if (st.isEmpty()) {
                LaperMod.LOGGER.error("Penawaran '{}' gagal: hadiah '{}' tidak valid.", o.id, r.item);
                p.sendSystemMessage(Component.literal("Penawaran ini bermasalah, hubungi admin.").withStyle(ChatFormatting.RED));
                return;
            }
            rewards.add(st);
        }

        for (Map.Entry<Item, Integer> e : needs(o).entrySet()) {
            ShopItems.take(p, e.getKey(), e.getValue(), plain());
        }
        for (ItemStack reward : rewards) give(p, reward);

        STORE.increment(scopeKey(o), p.getUUID(), o.id);
        p.sendSystemMessage(Component.literal("Pembelian berhasil!").withStyle(ChatFormatting.GREEN));
        p.playSound(SoundEvents.VILLAGER_YES, 1f, 1f);
    }

    private static void give(ServerPlayer p, ItemStack stack) {
        int max = Math.max(1, stack.getMaxStackSize());
        while (!stack.isEmpty()) {
            ItemStack part = stack.split(Math.min(max, stack.getCount()));
            p.getInventory().add(part);
            if (!part.isEmpty()) p.drop(part, false, Prediction.SERVER_ONLY);
        }
    }
}
