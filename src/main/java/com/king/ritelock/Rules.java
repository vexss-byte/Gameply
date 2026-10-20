package com.king.ritelock;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;

public final class Rules {
	private Rules() {}

	/** Netherite yang BOLEH masuk storage (tools, armor, senjata). */
	private static final String[] ALLOWED_SUFFIX = {
		"_sword", "_axe", "_pickaxe", "_shovel", "_hoe",
		"_helmet", "_chestplate", "_leggings", "_boots",
		"_spear", "_horse_armor", "_nautilus_armor"
	};

	/** true kalau item ini netherite NON tools/armor (ingot, scrap, block, template, dll). */
	public static boolean isRestricted(ItemStack stack) {
		ModConfig cfg = ModConfig.get();
		if (!cfg.netheriteRestrictionEnabled || stack.isEmpty()) return false;

		var key = BuiltInRegistries.ITEM.getKey(stack.getItem());
		if (cfg.extraRestrictedItems.contains(key.toString())) return true;

		String path = key.getPath();
		if (!path.contains("netherite")) return false;
		for (String suffix : ALLOWED_SUFFIX) {
			if (path.endsWith(suffix)) return false;
		}
		return true;
	}

	/** true kalau slot ini milik tempat penyimpanan (chest, ender chest, shulker, barrel, hopper, dll). */
	public static boolean isBlockedFromStorage(Container container, ItemStack stack) {
		if (!isRestricted(stack)) return false;
		return container instanceof BaseContainerBlockEntity
			|| container instanceof CompoundContainer      // double chest
			|| container instanceof PlayerEnderChestContainer
			|| container instanceof Entity;                // minecart chest, chest boat, dll
	}

	public static boolean hasRiptide(ItemStack stack) {
		for (Holder<Enchantment> holder : stack.getEnchantments().keySet()) {
			if (holder.is(Enchantments.RIPTIDE)) return true;
		}
		return false;
	}
}
