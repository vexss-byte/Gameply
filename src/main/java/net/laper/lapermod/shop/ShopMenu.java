package net.laper.lapermod.shop;

import java.util.ArrayList;
import java.util.List;
import net.laper.lapermod.LaperMod;
import net.laper.lapermod.config.ConfigManager;
import net.laper.lapermod.config.OffersConfig.ItemSpec;
import net.laper.lapermod.config.OffersConfig.OfferConfig;
import net.laper.lapermod.util.Text;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

/**
 * GUI toko bergaya "trading": tiap baris = [bayar A][bayar B] ➜ [hadiah 1..5] [tombol beli].
 * Semua slot terkunci (tidak bisa diambil/ditaruh); klik hanya memicu transaksi yang divalidasi server.
 * Memakai tipe menu chest 9x6 sehingga klien vanilla tanpa mod tetap bisa melihatnya.
 */
public final class ShopMenu extends AbstractContainerMenu {
    private static final int SIZE = 54;
    private static final int PER_PAGE = 5;
    private static final int PREV = 45;
    private static final int INFO = 49;
    private static final int NEXT = 53;

    private final ServerPlayer owner;
    private final SimpleContainer container = new SimpleContainer(SIZE);
    private final List<OfferConfig> offers;
    private int page = 0;

    private ShopMenu(int id, Inventory inventory, ServerPlayer owner, List<OfferConfig> offers) {
        super(MenuType.GENERIC_9x6, id);
        this.owner = owner;
        this.offers = offers;
        for (int r = 0; r < 6; r++) {
            for (int c = 0; c < 9; c++) addSlot(new LockedSlot(container, c + r * 9, 8 + c * 18, 18 + r * 18));
        }
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) addSlot(new Slot(inventory, c + r * 9 + 9, 8 + c * 18, 103 + r * 18 + 36));
        }
        for (int c = 0; c < 9; c++) addSlot(new Slot(inventory, c, 8 + c * 18, 161 + 36));
        render();
    }

    public static void open(ServerPlayer player, List<OfferConfig> offers) {
        List<OfferConfig> snapshot = List.copyOf(offers);
        player.openMenu(new SimpleMenuProvider(
                (id, inv, p) -> new ShopMenu(id, inv, player, snapshot),
                Text.legacy(ConfigManager.get().shop.guiTitle, false)));
    }

    // ------------------------------------------------------------------ klik

    @Override
    public void clicked(int slotId, int button, ClickType type, Player player) {
        if (slotId >= 0 && slotId < SIZE) {
            if ((type == ClickType.PICKUP || type == ClickType.QUICK_MOVE) && player instanceof ServerPlayer sp) {
                onTopClick(slotId, sp);
            }
            sendAllDataToRemote(); // batalkan prediksi klien
            return;
        }
        if (type == ClickType.PICKUP_ALL) { // klik ganda tidak boleh "mengumpulkan" item tampilan
            sendAllDataToRemote();
            return;
        }
        super.clicked(slotId, button, type, player);
    }

    private void onTopClick(int slot, ServerPlayer p) {
        if (slot == PREV && page > 0) {
            page--;
        } else if (slot == NEXT && page < pages() - 1) {
            page++;
        } else if (slot < PER_PAGE * 9) {
            int idx = page * PER_PAGE + slot / 9;
            if (idx < offers.size()) ShopManager.purchase(p, offers.get(idx));
        }
        if (!ShopManager.isOpen()) return;
        render();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return ShopManager.isOpen(); // otomatis menutup GUI saat event berakhir
    }

    // ------------------------------------------------------------------ tampilan

    private int pages() {
        return Math.max(1, (offers.size() + PER_PAGE - 1) / PER_PAGE);
    }

    private void render() {
        ItemStack filler = named(new ItemStack(Items.GRAY_STAINED_GLASS_PANE), Component.literal(" "));
        for (int i = 0; i < SIZE; i++) container.setItem(i, filler.copy());
        if (page >= pages()) page = pages() - 1;

        for (int row = 0; row < PER_PAGE; row++) {
            int idx = page * PER_PAGE + row;
            if (idx >= offers.size()) break;
            OfferConfig o = offers.get(idx);
            int base = row * 9;
            container.setItem(base, payment(o, 0));
            container.setItem(base + 1, o.payment.size() > 1 ? payment(o, 1) : filler.copy());
            container.setItem(base + 2, named(new ItemStack(Items.ARROW), Text.legacy("&7➜", true)));
            for (int k = 0; k < 5 && k < o.rewards.size(); k++) {
                ItemStack display = ShopItems.rewardDisplay(LaperMod.server(), o.rewards.get(k));
                container.setItem(base + 3 + k, display.isEmpty() ? filler.copy() : display);
            }
            container.setItem(base + 8, button(o));
        }

        if (page > 0) container.setItem(PREV, named(new ItemStack(Items.ARROW), Text.legacy("&e◀ Halaman sebelumnya", true)));
        if (page < pages() - 1) container.setItem(NEXT, named(new ItemStack(Items.ARROW), Text.legacy("&eHalaman berikutnya ▶", true)));
        ItemStack info = named(new ItemStack(Items.CLOCK), Text.legacy("&6Halaman " + (page + 1) + "/" + pages(), true));
        lore(info, List.of(Text.legacy("&7Sisa waktu: &f" + Text.formatTime(ShopManager.remainingSeconds()), true),
                Text.legacy("&7Klik baris mana pun untuk membeli.", true)));
        container.setItem(INFO, info);
    }

    private ItemStack payment(OfferConfig o, int index) {
        ItemSpec spec = o.payment.get(index);
        ItemStack s = ShopItems.paymentDisplay(spec);
        lore(s, List.of(Text.legacy("&7Dibutuhkan: &f" + spec.count + "×", true)));
        return s;
    }

    private ItemStack button(OfferConfig o) {
        ShopManager.Status st = ShopManager.status(owner, o);
        List<Component> lines = new ArrayList<>();
        ItemStack b;
        switch (st) {
            case OK -> {
                b = named(new ItemStack(Items.LIME_STAINED_GLASS_PANE), Text.legacy("&a&lBELI", true));
                lines.add(Text.legacy("&7Klik untuk membeli", true));
            }
            case FUNDS -> {
                b = named(new ItemStack(Items.RED_STAINED_GLASS_PANE), Text.legacy("&c&lITEM KURANG", true));
                lines.add(Text.legacy("&7Kamu belum punya bahan pembayaran.", true));
            }
            default -> {
                b = named(new ItemStack(Items.BARRIER), Text.legacy("&c&lBATAS TERCAPAI", true));
                lines.add(Text.legacy("&7Kamu sudah mencapai batas pembelian.", true));
            }
        }
        if (o.limitPerPlayer > 0) {
            String scope = "ALWAYS".equalsIgnoreCase(o.limitScope) ? "permanen" : "per event";
            lines.add(Text.legacy("&7Terbeli: &f" + ShopManager.used(owner, o) + "/" + o.limitPerPlayer + " &8(" + scope + ")", true));
        }
        if (o.rewards.size() > 5) lines.add(Text.legacy("&7+" + (o.rewards.size() - 5) + " hadiah lain (ikut diberikan)", true));
        for (int i = 0; i < o.rewards.size(); i++) {
            var r = o.rewards.get(i);
            if (r.count > 1) {
                MutableComponent line = Component.literal(r.count + "× ").withStyle(ChatFormatting.GRAY);
                line.append(new ItemStack(ShopItems.item(r.item).orElse(Items.BARRIER)).getHoverName());
                lines.add(line);
            }
        }
        lore(b, lines);
        return b;
    }

    private static ItemStack named(ItemStack s, Component name) {
        s.set(DataComponents.CUSTOM_NAME, name);
        return s;
    }

    private static void lore(ItemStack s, List<Component> lines) {
        s.set(DataComponents.LORE, new ItemLore(new ArrayList<>(lines)));
    }

    /** Slot yang tidak bisa diisi maupun diambil pemain. */
    private static final class LockedSlot extends Slot {
        LockedSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }
    }
}
