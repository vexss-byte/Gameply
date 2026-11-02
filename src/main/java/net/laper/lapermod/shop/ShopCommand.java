package net.laper.lapermod.shop;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

public final class ShopCommand {
    private ShopCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("lapershop").executes(ctx -> {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            ShopManager.openFor(player);
            return Command.SINGLE_SUCCESS;
        }));
    }
}
