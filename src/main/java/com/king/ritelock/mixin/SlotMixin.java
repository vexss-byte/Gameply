package com.king.ritelock.mixin;

import com.king.ritelock.Rules;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Blok netherite (non tools/armor) masuk slot chest, ender chest, barrel, hopper, dll. */
@Mixin(Slot.class)
public abstract class SlotMixin {
	@Shadow @Final public Container container;

	@Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
	private void ritelock$blockNetherite(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		if (Rules.isBlockedFromStorage(this.container, stack)) {
			cir.setReturnValue(false);
		}
	}
}
