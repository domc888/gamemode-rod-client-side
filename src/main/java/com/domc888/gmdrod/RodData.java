package com.domc888.gmdrod;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;

import java.util.UUID;

public final class RodData {

    public record Binding(UUID targetId, String targetName, String modeName) {
    }

    private static final String UUID_KEY = "target_uuid";
    private static final String NAME_KEY = "target_name";
    private static final String MODE_KEY = "mode";

    private RodData() {
    }

    public static GameType modeOf(String name) {
        return switch (name) {
            case "survival" -> GameType.SURVIVAL;
            case "creative" -> GameType.CREATIVE;
            case "spectator" -> GameType.SPECTATOR;
            case "adventure" -> GameType.ADVENTURE;
            default -> null;
        };
    }

    public static ItemStack create(ServerPlayer target, String modeName) {
        CompoundTag tag = new CompoundTag();
        tag.putString(UUID_KEY, target.getUUID().toString());
        tag.putString(NAME_KEY, target.getName().getString());
        tag.putString(MODE_KEY, modeName);

        ItemStack stack = new ItemStack(Items.FISHING_ROD);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        stack.set(DataComponents.ITEM_NAME, Component.literal("GM Rod"));
        stack.set(DataComponents.UNBREAKABLE, Unit.INSTANCE);
        return stack;
    }

    public static ItemStack createFake() {
        ItemStack stack = new ItemStack(Items.FISHING_ROD);
        stack.set(DataComponents.ITEM_NAME, Component.literal("GM Rod"));
        stack.set(DataComponents.UNBREAKABLE, Unit.INSTANCE);
        return stack;
    }

    public static Binding read(ItemStack stack) {
        if (!stack.is(Items.FISHING_ROD)) {
            return null;
        }
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return null;
        }

        CompoundTag tag = data.copyTag();
        String uuid = tag.getString(UUID_KEY).orElse(null);
        String name = tag.getString(NAME_KEY).orElse(null);
        String mode = tag.getString(MODE_KEY).orElse(null);
        if (uuid == null || name == null || mode == null || modeOf(mode) == null) {
            return null;
        }

        try {
            return new Binding(UUID.fromString(uuid), name, mode);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
