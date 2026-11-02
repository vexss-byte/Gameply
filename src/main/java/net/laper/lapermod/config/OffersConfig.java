package net.laper.lapermod.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Daftar penawaran LAPER SHOP: config/lapermod/lapershop_offers.json */
public final class OffersConfig {
    public List<OfferConfig> offers = new ArrayList<>();

    /** Hasil validasi (tidak disimpan ke JSON). */
    public transient List<OfferConfig> active = List.of();

    public static final class ItemSpec {
        public String item = "minecraft:stone";
        public int count = 1;

        public ItemSpec() {}

        public ItemSpec(String item, int count) {
            this.item = item;
            this.count = count;
        }
    }

    public static final class RewardSpec {
        public String item = "minecraft:stone";
        public int count = 1;
        public String name = "";
        public List<String> lore = new ArrayList<>();
        public Map<String, Integer> enchantments = new LinkedHashMap<>();
    }

    public static final class OfferConfig {
        public String id = "";
        public boolean enabled = true;
        /** 0 = tanpa batas. */
        public int limitPerPlayer = 0;
        /** "EVENT" = batas direset tiap event, "ALWAYS" = batas permanen. */
        public String limitScope = "EVENT";
        /** 1-2 item pembayaran. */
        public List<ItemSpec> payment = new ArrayList<>();
        /** Satu atau lebih hadiah (maks 5 ditampilkan di GUI). */
        public List<RewardSpec> rewards = new ArrayList<>();
    }

    public static OffersConfig defaults() {
        OffersConfig c = new OffersConfig();

        OfferConfig set = new OfferConfig();
        set.id = "netherite_set";
        set.limitPerPlayer = 1;
        set.payment.add(new ItemSpec("minecraft:netherite_ingot", 16));
        String[][] pieces = {
            {"minecraft:netherite_helmet", "Helm Laper"},
            {"minecraft:netherite_chestplate", "Baju Laper"},
            {"minecraft:netherite_leggings", "Celana Laper"},
            {"minecraft:netherite_boots", "Sepatu Laper"}
        };
        for (String[] p : pieces) {
            RewardSpec r = new RewardSpec();
            r.item = p[0];
            r.name = "&6" + p[1];
            r.lore.add("&7Hanya dari LAPER SHOP");
            r.enchantments.put("minecraft:protection", 4);
            r.enchantments.put("minecraft:unbreaking", 3);
            set.rewards.add(r);
        }
        c.offers.add(set);

        OfferConfig apples = new OfferConfig();
        apples.id = "golden_apples";
        apples.limitPerPlayer = 3;
        apples.payment.add(new ItemSpec("minecraft:diamond", 8));
        RewardSpec a = new RewardSpec();
        a.item = "minecraft:golden_apple";
        a.count = 4;
        apples.rewards.add(a);
        c.offers.add(apples);

        OfferConfig modded = new OfferConfig();
        modded.id = "contoh_item_mod_lain";
        modded.enabled = false;
        modded.payment.add(new ItemSpec("minecraft:emerald_block", 2));
        RewardSpec m = new RewardSpec();
        m.item = "modidlain:nama_item";
        modded.rewards.add(m);
        c.offers.add(modded);
        return c;
    }
}
