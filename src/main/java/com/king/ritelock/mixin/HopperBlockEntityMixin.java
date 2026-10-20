package com.king.ritelock.mixin;

import com.king.ritelock.Rules;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Semua perpindahan item lewat hopper (hopper -> container, container -> hopper,
 * hopper ambil item di tanah, dropper -> container) lewat canPlaceItemInContainer.
 */
@Mixin(HopperBlockEntity.class)
public abstract class HopperBlockEntityMixin {
	@Inject(method = "canPlaceItemInContainer", at = @At("HEAD"), cancellable = true)
	private static void ritelock$blockNetherite(Container container, ItemStack stack, int slot, Direction direction, CallbackInfoReturnable<Boolean> cir) {
		if (Rules.isRestricted(stack)) {
			cir.setReturnValue(false);
		}
	}
}
