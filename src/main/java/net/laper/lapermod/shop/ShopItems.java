package net.laper.lapermod.shop;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.laper.lapermod.LaperMod;
import net.laper.lapermod.config.OffersConfig.ItemSpec;
import net.laper.lapermod.config.OffersConfig.RewardSpec;
import net.laper.lapermod.util.Text;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/** Pembuatan item hadiah dari config, serta hitung/ambil item pembayaran pemain. */
public final class ShopItems {
    private static final int MAIN_SLOTS = 36;

    private ShopItems() {}

    public static Optional<Item> item(String id) {
        Identifier key = id == null ? null : Identifier.tryParse(id.trim());
        if (key == null) return Optional.empty();
        return BuiltInRegistries.ITEM.getOptional(key).filter(i -> i != Items.AIR);
    }

    /** Item hadiah lengkap (jumlah penuh, nama, lore, enchant). EMPTY bila item tidak valid. */
    public static ItemStack reward(MinecraftServer server, RewardSpec spec) {
        Optional<Item> item = item(spec.item);
        if (item.isEmpty()) return ItemStack.EMPTY;
        ItemStack stack = new ItemStack(item.get(), Math.max(1, spec.count));

        if (spec.name != null && !spec.name.isBlank()) {
            stack.set(DataComponents.CUSTOM_NAME, Text.legacy(spec.name, true));
        }
        if (spec.lore != null && !spec.lore.isEmpty()) {
            List<Component> lines = new ArrayList<>();
            for (String l : spec.lore) lines.add(Text.legacy(l, true));
            stack.set(DataComponents.LORE, new ItemLore(lines));
        }
        if (spec.enchantments != null && !spec.enchantments.isEmpty() && server != null) {
            HolderLookup.RegistryLookup<Enchantment> registry = server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            ItemEnchantments.Mutable m = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
            for (var e : spec.enchantments.entrySet()) {
                Identifier eid = Identifier.tryParse(e.getKey());
                if (eid == null) {
                    LaperMod.LOGGER.warn("Enchantment tidak valid: {}", e.getKey());
                    continue;
                }
                Optional<Holder.Reference<Enchantment>> holder = registry.get(ResourceKey.create(Registries.ENCHANTMENT, eid));
                if (holder.isEmpty()) {
                    LaperMod.LOGGER.warn("Enchantment tidak dikenal: {}", e.getKey());
                    continue;
                }
                m.set(holder.get(), Math.max(1, Math.min(255, e.getValue())));
            }
            stack.set(item.get() == Items.ENCHANTED_BOOK ? DataComponents.STORED_ENCHANTMENTS : DataComponents.ENCHANTMENTS, m.toImmutable());
        }
        return stack;
    }

    /** Tampilan di GUI (jumlah dibatasi ukuran stack). */
    public static ItemStack rewardDisplay(MinecraftServer server, RewardSpec spec) {
        ItemStack s = reward(server, spec);
        if (s.isEmpty()) return ItemStack.EMPTY;
        s.setCount(Math.max(1, Math.min(s.getMaxStackSize(), spec.count)));
        return s;
    }

    public static ItemStack paymentDisplay(ItemSpec spec) {
        Optional<Item> item = item(spec.item);
        if (item.isEmpty()) return new ItemStack(Items.BARRIER);
        ItemStack s = new ItemStack(item.get());
        s.setCount(Math.max(1, Math.min(s.getMaxStackSize(), spec.count)));
        return s;
    }

    public static int count(ServerPlayer p, Item item, boolean plainOnly) {
        Inventory inv = p.getInventory();
        int total = 0;
        for (int i = 0; i < MAIN_SLOTS; i++) {
            ItemStack s = inv.getItem(i);
            if (matches(s, item, plainOnly)) total += s.getCount();
        }
        return total;
    }

    public static boolean take(ServerPlayer p, Item item, int amount, boolean plainOnly) {
        Inventory inv = p.getInventory();
        int left = amount;
        for (int i = 0; i < MAIN_SLOTS && left > 0; i++) {
            ItemStack s = inv.getItem(i);
            if (matches(s, item, plainOnly)) {
                int n = Math.min(left, s.getCount());
                s.shrink(n);
                left -= n;
            }
        }
        return left <= 0;
    }

    private static boolean matches(ItemStack s, Item item, boolean plainOnly) {
        return !s.isEmpty() && s.is(item) && (!plainOnly || s.getComponentsPatch().isEmpty());
    }
}
