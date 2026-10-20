package com.king.ritelock.mixin;

import com.king.ritelock.Rules;
import net.minecraft.world.inventory.ShulkerBoxSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** ShulkerBoxSlot override mayPlace sendiri, jadi perlu mixin terpisah. */
@Mixin(ShulkerBoxSlot.class)
public abstract class ShulkerBoxSlotMixin {
	@Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
	private void ritelock$blockNetherite(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		if (Rules.isRestricted(stack)) {
			cir.setReturnValue(false);
		}
	}
}
