package com.king.ritelock;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class RiteLock implements ModInitializer {
	public static final String MOD_ID = "ritelock";

	/** Player yang tadi sedang riptide (supaya cooldown cuma dipasang sekali per lontaran). */
	private final Set<UUID> spinning = new HashSet<>();
	private int tickCounter = 0;

	@Override
	public void onInitialize() {
		ModConfig.load();

		ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);

		// ===== Chat dimatikan =====
		ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) -> {
			if (!ModConfig.get().chatDisabled) return true;
			warn(sender);
			return false; // pesan dibatalkan
		});

		// /say /me /msg dll dari player juga diblok (console tetap boleh)
		ServerMessageEvents.ALLOW_COMMAND_MESSAGE.register((message, source, params) -> {
			if (!ModConfig.get().chatDisabled) return true;
			ServerPlayer player = source.getPlayer();
			if (player == null) return true;
			warn(player);
			return false;
		});
	}

	private static void warn(ServerPlayer player) {
		Component text = GalaxyText.gradient(ModConfig.get().chatWarningText);
		player.sendSystemMessage(text);        // muncul di chat
		player.sendSystemMessage(text, true);  // muncul di action bar
	}

	private void onServerTick(MinecraftServer server) {
		if (++tickCounter >= 100) { // tiap 5 detik
			tickCounter = 0;
			ModConfig.reloadIfChanged();
			spinning.removeIf(id -> server.getPlayerList().getPlayer(id) == null);
		}

		ModConfig cfg = ModConfig.get();
		if (!cfg.riptideCooldownEnabled) return;

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (player.isAutoSpinAttack()) {
				// baru mulai riptide -> pasang cooldown
				if (spinning.add(player.getUUID())) {
					applyRiptideCooldown(player, cfg.riptideCooldownTicks());
				}
			} else {
				spinning.remove(player.getUUID());
			}
		}
	}

	private static void applyRiptideCooldown(ServerPlayer player, int ticks) {
		if (ticks <= 0) return;
		for (InteractionHand hand : InteractionHand.values()) {
			ItemStack stack = player.getItemInHand(hand);
			if (stack.is(Items.TRIDENT) && Rules.hasRiptide(stack)) {
				player.getCooldowns().addCooldown(stack, ticks);
				return;
			}
		}
	}
}
