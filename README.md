# LaperMod (Fabric, Minecraft 26.3)

Satu mod **server-side** untuk survival multiplayer kompetitif. Klien vanilla cukup (tidak perlu memasang mod di sisi pemain).

## Versi yang dipakai (dicek 31 Okt 2026)
| Komponen | Versi |
|---|---|
| Minecraft | 26.3 (unobfuscated, tanpa mappings) |
| Fabric Loader | 0.19.5 |
| Fabric Loom | 1.17 (`net.fabricmc.fabric-loom`) |
| Fabric API | 0.161.0+26.3 |
| Gradle | 9.6.0 |
| Java | 25 |

## Instalasi
1. Pasang Fabric Loader 0.19.5+ di server Minecraft 26.3 dan Java 25.
2. Taruh `fabric-api-0.161.0+26.3.jar` dan `lapermod-<versi>.jar` di folder `mods/`.
3. Jalankan server sekali. Config dibuat otomatis di `config/lapermod/`.

## Build lewat GitHub
1. Buat repo GitHub, push seluruh isi folder ini ke branch `main`.
2. Tab **Actions** → workflow **Build** berjalan otomatis (atau klik *Run workflow*).
3. Unduh `lapermod-jar` dari bagian **Artifacts**.
4. Rilis: buat tag, mis. `git tag v1.0.0 && git push origin v1.0.0` → JAR muncul di **Releases**.

Build lokal: `gradle build` (Gradle 9.6.0, JDK 25) → `build/libs/lapermod-<versi>.jar`.

## Fitur
| Fitur | Cara kerja |
|---|---|
| Cooldown Trident Riptide | 15 dtk, mulai saat Riptide aktif. Per pemain: ganti slot/pindah inventory tidak membantu. Trident tanpa Riptide tidak terkena. |
| Cooldown Ender Pearl | 5 dtk untuk semua pearl milik pemain. |
| HUD | Action bar tepat di atas hotbar, update tiap 0,1 dtk, dua cooldown tampil bersamaan. |
| Totem Evoker | Peluang drop `rarity.evokerTotemDropChance` (default 5%), berlaku untuk semua Evoker termasuk Raid. |
| Buku Mending | Memancing & Villager: peluang tetap Mending `fishingMendingKeepChance` / `villagerMendingKeepChance`; sisanya jadi Unbreaking III. |
| Death counter | Disimpan di `<world>/lapermod/deaths.json`; tab list menampilkan `☠ N` (☠ merah). |
| Koordinat | `reduced_debug_info` dipaksa aktif (F3 tanpa XYZ). |
| Locator Bar | `locator_bar` dipaksa nonaktif. |
| LAPER SHOP | Jadwal waktu nyata, boss bar, pengumuman gradasi, `/lapershop`, GUI toko. |

## Konfigurasi
Semua file di `config/lapermod/` dan **dimuat ulang otomatis** (cek tiap 5 detik) saat disimpan.

### `lapermod.json`
- `cooldowns`: `enabled`, `tridentSeconds`, `enderPearlSeconds`, `showHud`, `lockTridentInInventory`, `lockTridentInHand`, `debug` (`true` = catat kejadian cooldown di log server)
- `rarity`: `enabled`, `evokerTotemDropChance`, `fishingMendingKeepChance`, `villagerMendingKeepChance` (0.0–1.0)
- `restrictions`: `disableCoordinates`, `disableLocatorBar`
- `shop`: `enabled`, `timezone` (mis. `Asia/Jakarta`), `startTimes` (mis. `["12:00","20:00"]`), `durationMinutes` (maks 120), `gradientStart`/`gradientEnd` (hex), `barLabel`, `guiTitle`, `openMessage`, `closeMessage`, dll.
  `alwaysOpenForTesting: true` membuka toko terus-menerus untuk uji coba.

### `lapershop_offers.json`
```json
{
  "offers": [
    {
      "id": "netherite_set",
      "enabled": true,
      "limitPerPlayer": 1,
      "limitScope": "EVENT",
      "payment": [ { "item": "minecraft:netherite_ingot", "count": 16 } ],
      "rewards": [
        {
          "item": "minecraft:netherite_helmet",
          "count": 1,
          "name": "&6Helm Laper",
          "lore": ["&7Hanya dari LAPER SHOP"],
          "enchantments": { "minecraft:protection": 4, "minecraft:unbreaking": 3 }
        }
      ]
    }
  ]
}
```
- `payment`: 1–2 item. `rewards`: satu atau banyak item (set armor lengkap = 4 reward); 5 pertama ditampilkan di GUI, semuanya tetap diberikan.
- `limitPerPlayer`: 0 = tanpa batas. `limitScope`: `EVENT` (reset tiap event) atau `ALWAYS`.
- `enabled: false` mematikan satu penawaran. Item mod lain: pakai id `modid:item` (mod harus terpasang).
- Kode warna `&`, juga hex `&#RRGGBB`.
- Data pembelian: `<world>/lapermod/shop_purchases.json`.

## Penyimpangan dari spesifikasi
- **HUD cooldown** memakai action bar, bukan HUD kustom (mod ini tidak butuh instalasi klien).
- **Trident (Riptide)**: selama cooldown tidak bisa dipakai, tidak bisa dipegang (slot hotbar otomatis dipindah), dan tidak bisa diambil/dipindah/di-drop/di-swap di inventory (memakai 1 mixin). Tersisa: Trident di offhand tidak dipindah paksa, tapi tetap tidak bisa dipakai.
- **Koordinat**: F3 tidak menampilkan XYZ, tetapi mod klien cheat tetap tidak bisa dicegah dari server.
- **GUI toko** berbentuk menu chest bergaya trading (baris bayar → hadiah), bukan layar Villager asli, agar mendukung hadiah banyak item dan klien vanilla.
- **Tab list** memakai objective scoreboard slot LIST (milik mod): jangan pakai slot LIST untuk objective lain.
