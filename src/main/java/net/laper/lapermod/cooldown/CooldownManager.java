package net.laper.lapermod.cooldown;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.laper.lapermod.LaperMod;
import net.laper.lapermod.config.ConfigManager;
import net.laper.lapermod.config.ModConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * Cooldown per-pemain (bukan per-slot/per-item) untuk Trident Riptide dan Ender Pearl.
 * Cooldown melekat pada pemain, jadi mengganti slot / memindahkan item tidak membantu.
 * Selama cooldown Trident Riptide: tidak bisa dipakai, tidak bisa dipegang (slot dipindah otomatis),
 * dan tidak bisa dipindahkan di inventory (lihat AbstractContainerMenuMixin).
 */
public final class CooldownManager {
    private static final Map<UUID, Long> TRIDENT = new HashMap<>();
    private static final Map<UUID, Long> PEARL = new HashMap<>();
    private static final Set<UUID> SPINNING = new HashSet<>();
    private static final Set<UUID> HUD_ACTIVE = new HashSet<>();
    private static MinecraftServer current;

    private CooldownManager() {}

    public static void register() {
        UseItemCallback.EVENT.register((player, level, hand) -> {
            ModConfig.Cooldowns cfg = ConfigManager.get().cooldowns;
            if (!cfg.enabled || level.isClientSide() || !(player instanceof ServerPlayer sp) || current == null) {
                return InteractionResult.PASS;
            }
            ItemStack stack = sp.getItemInHand(hand);
            long now = current.getTickCount();
            UUID id = sp.getUUID();
            if (isRiptideTrident(stack)) {
                // Cooldown berlaku untuk SEMUA Trident Riptide milik pemain, bukan hanya satu stack.
                if (remaining(TRIDENT, id, now) > 0) {
                    if (cfg.debug) LaperMod.LOGGER.info("[cooldown] {} dicegah memakai Trident (sisa {} tick)", sp.getName().getString(), remaining(TRIDENT, id, now));
                    return InteractionResult.FAIL;
                }
            } else if (stack.is(Items.ENDER_PEARL) && cfg.enderPearlSeconds > 0) {
                if (remaining(PEARL, id, now) > 0) return InteractionResult.FAIL;
                PEARL.put(id, now + cfg.enderPearlSeconds * 20L);
            }
            return InteractionResult.PASS;
        });
        ServerTickEvents.END_SERVER_TICK.register(CooldownManager::tick);
    }

    public static boolean isRiptideTrident(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.is(Items.TRIDENT)) return false;
        ItemEnchantments enchants = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        for (Holder<Enchantment> h : enchants.keySet()) {
            if (h.is(Enchantments.RIPTIDE)) return true;
        }
        return false;
    }

    private static long remaining(Map<UUID, Long> map, UUID id, long now) {
        Long until = map.get(id);
        return until == null ? 0 : Math.max(0, until - now);
    }

    /** True bila pemain sedang kena cooldown Trident Riptide. */
    public static boolean tridentLocked(ServerPlayer p) {
        ModConfig.Cooldowns cfg = ConfigManager.get().cooldowns;
        return cfg.enabled && current != null && remaining(TRIDENT, p.getUUID(), current.getTickCount()) > 0;
    }

    /**
     * Dipanggil oleh mixin sebelum klik inventory diproses. True = klik dibatalkan karena
     * menyentuh Trident Riptide yang sedang cooldown (ambil, shift-click, drop, swap hotbar/offhand).
     */
    public static boolean isClickBlocked(ServerPlayer p, AbstractContainerMenu menu, int slotId, int button, ContainerInput type) {
        if (!ConfigManager.get().cooldowns.lockTridentInInventory || !tridentLocked(p)) return false;
        if (type == ContainerInput.QUICK_CRAFT) return isRiptideTrident(menu.getCarried());
        if (type == ContainerInput.PICKUP_ALL && isRiptideTrident(menu.getCarried())) return true;
        if (slotId < 0 || slotId >= menu.slots.size()) return false;
        Slot slot = menu.slots.get(slotId);
        if (isRiptideTrident(slot.getItem())) return true;
        if (type == ContainerInput.SWAP) {
            Inventory inv = p.getInventory();
            if (button >= 0 && button < inv.getContainerSize() && (button < 9 || button == 40)) {
                return isRiptideTrident(inv.getItem(button));
            }
        }
        return false;
    }

    private static void tick(MinecraftServer server) {
        current = server;
        ModConfig.Cooldowns cfg = ConfigManager.get().cooldowns;
        if (!cfg.enabled) return;
        long now = server.getTickCount();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            UUID id = p.getUUID();
            // Riptide dimulai -> cooldown langsung aktif (deteksi tepi naik dari spin attack)
            if (p.isAutoSpinAttack()) {
                if (SPINNING.add(id) && cfg.tridentSeconds > 0) {
                    TRIDENT.put(id, now + cfg.tridentSeconds * 20L);
                    if (cfg.debug) LaperMod.LOGGER.info("[cooldown] {} memakai Riptide -> cooldown {} detik", p.getName().getString(), cfg.tridentSeconds);
                }
            } else {
                SPINNING.remove(id);
            }
            if (cfg.lockTridentInHand && remaining(TRIDENT, id, now) > 0) enforceHold(p);
            if (cfg.showHud && now % 2 == 0) hud(p, now);
        }
        if (now % 1200 == 0) {
            TRIDENT.values().removeIf(v -> v <= now);
            PEARL.values().removeIf(v -> v <= now);
        }
    }

    /** Bila Trident Riptide ada di tangan utama saat cooldown, pindahkan pilihan slot ke slot hotbar lain. */
    private static void enforceHold(ServerPlayer p) {
        Inventory inv = p.getInventory();
        int sel = inv.getSelectedSlot();
        if (!isRiptideTrident(inv.getItem(sel))) return;
        for (int i = 1; i <= 8; i++) {
            int candidate = (sel + i) % 9;
            if (!isRiptideTrident(inv.getItem(candidate))) {
                inv.setSelectedSlot(candidate);
                p.connection.send(new ClientboundSetHeldSlotPacket(candidate));
                return;
            }
        }
    }

    private static void hud(ServerPlayer p, long now) {
        UUID id = p.getUUID();
        long t = remaining(TRIDENT, id, now);
        long e = remaining(PEARL, id, now);
        if (t <= 0 && e <= 0) {
            if (HUD_ACTIVE.remove(id)) p.connection.send(new ClientboundSetActionBarTextPacket(Component.empty()));
            return;
        }
        MutableComponent c = Component.empty();
        if (t > 0) c.append(label("item.minecraft.trident", t));
        if (t > 0 && e > 0) c.append(Component.literal("  |  ").withStyle(ChatFormatting.DARK_GRAY));
        if (e > 0) c.append(label("item.minecraft.ender_pearl", e));
        HUD_ACTIVE.add(id);
        p.connection.send(new ClientboundSetActionBarTextPacket(c));
    }

    private static MutableComponent label(String nameKey, long ticks) {
        String time = String.format(Locale.ROOT, "%.1fs", ticks / 20.0);
        return Component.translatable(nameKey).withStyle(ChatFormatting.GOLD)
                .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(time).withStyle(ChatFormatting.RED));
    }
}
