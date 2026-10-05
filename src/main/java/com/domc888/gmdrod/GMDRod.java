package com.domc888.gmdrod;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Locale;

public final class GMDRod implements ModInitializer {

    private static final List<String> MODES = List.of("survival", "creative", "spectator", "adventure");

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(Commands.literal("gmdrod")
                        .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("gamemode", StringArgumentType.word())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(MODES, builder))
                                        .executes(context -> giveRod(context, false))
                                        .then(Commands.literal("fake")
                                                .executes(context -> giveRod(context, true)))))));

        UseItemCallback.EVENT.register((player, level, hand) -> {
            if (!(player instanceof ServerPlayer holder)) {
                return InteractionResult.PASS;
            }
            RodData.Binding binding = RodData.read(holder.getItemInHand(hand));
            if (binding == null || holder.fishing == null) {
                return InteractionResult.PASS;
            }
            reelIn(holder, binding);
            return InteractionResult.SUCCESS;
        });
    }

    private static int giveRod(CommandContext<CommandSourceStack> context, boolean fake)
            throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer holder = source.getPlayerOrException();
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        String modeName = StringArgumentType.getString(context, "gamemode").toLowerCase(Locale.ROOT);

        if (RodData.modeOf(modeName) == null) {
            source.sendFailure(Component.literal("Invalid gamemode. Use survival, creative, spectator or adventure."));
            return 0;
        }

        ItemStack rod = fake ? RodData.createFake() : RodData.create(target, modeName);
        if (!holder.getInventory().add(rod)) {
            holder.drop(rod, false, Prediction.SERVER_ONLY);
        }

        if (fake) {
            source.sendSuccess(() -> Component.literal("Gave you a fake GM Rod."), false);
        } else {
            String targetName = target.getName().getString();
            source.sendSuccess(
                    () -> Component.literal("Gave you a GM Rod for " + targetName + " (" + modeName + ")."),
                    false
            );
        }
        return Command.SINGLE_SUCCESS;
    }

    private static void reelIn(ServerPlayer holder, RodData.Binding binding) {
        // Remove the bobber ourselves so no loot drops and nothing gets pulled.
        holder.fishing.discard();
        holder.fishing = null;

        if (!(holder.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        ServerPlayer target = serverLevel.getServer().getPlayerList().getPlayer(binding.targetId());
        if (target == null) {
            holder.sendSystemMessage(Component.literal(binding.targetName() + " is not online."));
            return;
        }

        target.setGameMode(RodData.modeOf(binding.modeName()));
        holder.sendSystemMessage(Component.literal(
                "Set " + binding.targetName() + " to " + binding.modeName() + "."
        ));
    }
}
