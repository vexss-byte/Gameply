package net.laper.lapermod.loot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.laper.lapermod.config.ConfigManager;
import net.laper.lapermod.config.ModConfig;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.phys.Vec3;

/**
 * Totem dari Evoker dan buku Mending dibuat langka. Seluruhnya diatur lewat config (rarity.*).
 * Dibuat dengan event Fabric (tanpa mixin) agar stabil lintas update Minecraft.
 */
public final class RareDrops {
    private record EvokerDeath(ServerLevel level, long tick, Vec3 pos) {}

    private static final List<EvokerDeath> RECENT = new ArrayList<>();

    private RareDrops() {}

    public static void register() {
        // 1) Tandai kematian Evoker (semua Evoker, termasuk dari Raid). Drop terjadi di tick yang sama.
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
            if (entity.getType() == EntityTypes.EVOKER && entity.level() instanceof ServerLevel sl) {
                long now = sl.getGameTime();
                RECENT.removeIf(d -> now - d.tick() > 5);
                RECENT.add(new EvokerDeath(sl, now, entity.position()));
            }
            return true;
        });

        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            ModConfig.Rarity cfg = ConfigManager.get().rarity;
            if (!cfg.enabled || !(entity instanceof ItemEntity item)) return;
            ItemStack stack = item.getItem();
            if (stack.is(Items.TOTEM_OF_UNDYING)) {
                handleTotem(item, world, cfg);
            } else if (stack.is(Items.ENCHANTED_BOOK) && hasStoredMending(stack)) {
                handleFishedBook(item, world, cfg);
            }
        });

        // 2) Villager: koreksi penawaran Mending sebelum jendela trading dibuka.
        UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            ModConfig.Rarity cfg = ConfigManager.get().rarity;
            if (cfg.enabled && !world.isClientSide() && entity instanceof Merchant merchant) {
                fixOffers(entity, merchant, cfg, world);
            }
            return InteractionResult.PASS;
        });
    }

    private static void handleTotem(ItemEntity item, ServerLevel world, ModConfig.Rarity cfg) {
        long now = world.getGameTime();
        for (EvokerDeath d : RECENT) {
            if (d.level() == world && Math.abs(now - d.tick()) <= 1 && d.pos().distanceToSqr(item.position()) <= 25.0) {
                if (ThreadLocalRandom.current().nextDouble() >= cfg.evokerTotemDropChance) {
                    item.setItem(ItemStack.EMPTY); // item kosong otomatis dihapus oleh ItemEntity.tick()
                }
                return;
            }
        }
    }

    private static void handleFishedBook(ItemEntity item, ServerLevel world, ModConfig.Rarity cfg) {
        // Hasil memancing muncul tepat di posisi pelampung (bobber)
        List<Entity> hooks = world.getEntitiesOfClass(Entity.class, item.getBoundingBox().inflate(2.5),
                e -> e.getType() == EntityTypes.FISHING_BOBBER);
        if (hooks.isEmpty()) return;
        if (ThreadLocalRandom.current().nextDouble() < cfg.fishingMendingKeepChance) return;
        item.setItem(withoutMending(item.getItem(), world));
    }

    private static void fixOffers(Entity entity, Merchant merchant, ModConfig.Rarity cfg, net.minecraft.world.level.Level world) {
        MerchantOffers offers = merchant.getOffers();
        // Tag unik per jumlah penawaran: pengecekan diulang hanya saat villager naik level (penawaran baru).
        String tag = "lapermod_mending_" + offers.size();
        if (entity.entityTags().contains(tag)) return;
        entity.addTag(tag);
        for (MerchantOffer offer : offers) {
            ItemStack result = offer.getResult();
            if (result.is(Items.ENCHANTED_BOOK) && hasStoredMending(result)
                    && ThreadLocalRandom.current().nextDouble() >= cfg.villagerMendingKeepChance) {
                ItemStack replacement = withoutMending(result, world);
                result.set(DataComponents.STORED_ENCHANTMENTS, replacement.get(DataComponents.STORED_ENCHANTMENTS));
            }
        }
    }

    private static boolean hasStoredMending(ItemStack stack) {
        ItemEnchantments stored = stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
        for (Holder<Enchantment> h : stored.keySet()) {
            if (h.is(Enchantments.MENDING)) return true;
        }
        return false;
    }

    /** Buku baru tanpa Mending; bila kosong, diganti Unbreaking III. */
    private static ItemStack withoutMending(ItemStack book, net.minecraft.world.level.Level world) {
        ItemEnchantments stored = book.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
        ItemEnchantments.Mutable m = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        for (Holder<Enchantment> h : stored.keySet()) {
            if (!h.is(Enchantments.MENDING)) m.set(h, stored.getLevel(h));
        }
        if (m.keySet().isEmpty()) {
            Holder<Enchantment> unbreaking = world.registryAccess()
                    .lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.UNBREAKING);
            m.set(unbreaking, 3);
        }
        ItemStack out = new ItemStack(Items.ENCHANTED_BOOK);
        out.set(DataComponents.STORED_ENCHANTMENTS, m.toImmutable());
        return out;
    }
}
