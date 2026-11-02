package net.laper.lapermod.config;

import java.util.ArrayList;
import java.util.List;

/** Konfigurasi utama: config/lapermod/lapermod.json */
public final class ModConfig {
    public Cooldowns cooldowns = new Cooldowns();
    public Rarity rarity = new Rarity();
    public Restrictions restrictions = new Restrictions();
    public Shop shop = new Shop();

    public static final class Cooldowns {
        public boolean enabled = true;
        public int tridentSeconds = 15;
        public int enderPearlSeconds = 5;
        public boolean showHud = true;
        /** Selama cooldown, Trident Riptide tidak bisa diambil/dipindah/di-drop/di-swap di inventory. */
        public boolean lockTridentInInventory = true;
        /** Selama cooldown, slot hotbar otomatis dipindah bila pemain memegang Trident Riptide. */
        public boolean lockTridentInHand = true;
        /** Catat kejadian cooldown di log server (untuk diagnosa). */
        public boolean debug = false;
    }

    public static final class Rarity {
        public boolean enabled = true;
        /** Peluang Evoker menjatuhkan Totem (vanilla: 1.0 = selalu). */
        public double evokerTotemDropChance = 0.05;
        /** Peluang buku Mending hasil memancing TETAP Mending (sisanya diganti Unbreaking III). */
        public double fishingMendingKeepChance = 0.10;
        /** Peluang penawaran Mending milik Villager TETAP Mending (sisanya diganti Unbreaking III). */
        public double villagerMendingKeepChance = 0.15;
    }

    public static final class Restrictions {
        public boolean disableCoordinates = true;
        public boolean disableLocatorBar = true;
    }

    public static final class Shop {
        public boolean enabled = true;
        /** Hanya untuk testing: toko selalu terbuka. */
        public boolean alwaysOpenForTesting = false;
        public String timezone = "Asia/Jakarta";
        public List<String> startTimes = new ArrayList<>(List.of("12:00", "20:00"));
        public int durationMinutes = 120;
        /** true = item pembayaran harus polos (tanpa enchant/nama/damage). */
        public boolean plainPaymentOnly = true;
        public String gradientStart = "#FFFF55";
        public String gradientEnd = "#FF8800";
        public String barLabel = "LAPERSHOP";
        public String guiTitle = "&6&lLAPER SHOP";
        public String openMessage = "LAPER SHOP SEDANG BUKA!";
        public String openHint = "&eKetik &6/lapershop &euntuk berbelanja!";
        public String closeMessage = "LAPER SHOP SUDAH TUTUP!";
        public String closedCommandMessage = "&cLAPER SHOP sedang tutup.";
        public String nextOpenFormat = "&7Buka lagi pukul &e{time} &7({zone}).";
    }

    public void sanitize() {
        if (cooldowns == null) cooldowns = new Cooldowns();
        if (rarity == null) rarity = new Rarity();
        if (restrictions == null) restrictions = new Restrictions();
        if (shop == null) shop = new Shop();
        cooldowns.tridentSeconds = Math.max(0, cooldowns.tridentSeconds);
        cooldowns.enderPearlSeconds = Math.max(0, cooldowns.enderPearlSeconds);
        rarity.evokerTotemDropChance = clamp01(rarity.evokerTotemDropChance);
        rarity.fishingMendingKeepChance = clamp01(rarity.fishingMendingKeepChance);
        rarity.villagerMendingKeepChance = clamp01(rarity.villagerMendingKeepChance);
        shop.durationMinutes = Math.max(1, Math.min(120, shop.durationMinutes));
        if (shop.startTimes == null || shop.startTimes.isEmpty()) shop.startTimes = new ArrayList<>(List.of("12:00", "20:00"));
        if (shop.timezone == null) shop.timezone = "Asia/Jakarta";
        Shop d = new Shop();
        if (shop.gradientStart == null) shop.gradientStart = d.gradientStart;
        if (shop.gradientEnd == null) shop.gradientEnd = d.gradientEnd;
        if (shop.barLabel == null) shop.barLabel = d.barLabel;
        if (shop.guiTitle == null) shop.guiTitle = d.guiTitle;
        if (shop.openMessage == null) shop.openMessage = d.openMessage;
        if (shop.openHint == null) shop.openHint = d.openHint;
        if (shop.closeMessage == null) shop.closeMessage = d.closeMessage;
        if (shop.closedCommandMessage == null) shop.closedCommandMessage = d.closedCommandMessage;
        if (shop.nextOpenFormat == null) shop.nextOpenFormat = d.nextOpenFormat;
    }

    private static double clamp01(double v) {
        if (Double.isNaN(v)) return 0;
        return Math.max(0.0, Math.min(1.0, v));
    }
}
