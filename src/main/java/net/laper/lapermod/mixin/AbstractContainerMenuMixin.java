package net.laper.lapermod.mixin;

import net.laper.lapermod.cooldown.CooldownManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Mencegah Trident Riptide yang sedang cooldown diambil/dipindah/di-drop/di-swap dari inventory. */
@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin {
    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
    private void lapermod$lockCooldownTrident(int slotId, int button, ContainerInput type, Player player, CallbackInfo ci) {
        if (player instanceof ServerPlayer sp) {
            AbstractContainerMenu self = (AbstractContainerMenu) (Object) this;
            if (CooldownManager.isClickBlocked(sp, self, slotId, button, type)) {
                ci.cancel();
                self.sendAllDataToRemote(); // batalkan prediksi klien
            }
        }
    }
}
