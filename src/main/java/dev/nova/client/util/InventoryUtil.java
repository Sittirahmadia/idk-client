package dev.nova.client.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public final class InventoryUtil {

    private static final MinecraftClient MC = MinecraftClient.getInstance();

    /** Returns hotbar slot index (0-8) containing the item, or -1. */
    public static int findInHotbar(Item item) {
        var inv = MC.player.getInventory();
        for (int i = 0; i < 9; i++) {
            if (inv.getStack(i).isOf(item)) return i;
        }
        return -1;
    }

    /** Returns first inventory slot (0-35) with item, or -1. */
    public static int findInInventory(Item item) {
        var inv = MC.player.getInventory();
        for (int i = 0; i < 36; i++) {
            if (inv.getStack(i).isOf(item)) return i;
        }
        return -1;
    }

    /** Switch to hotbar slot. */
    public static void switchTo(int slot) {
        if (slot < 0 || slot > 8) return;
        MC.player.getInventory().selectedSlot = slot;
    }

    /** Switch to item in hotbar, returns true if found. */
    public static boolean switchToItem(Item item) {
        int slot = findInHotbar(item);
        if (slot == -1) return false;
        switchTo(slot);
        return true;
    }

    /** Returns true if the player is holding the given item in main hand. */
    public static boolean isHolding(Item item) {
        return MC.player.getMainHandStack().isOf(item);
    }

    /** Counts total items in full inventory (hotbar + main). */
    public static int countInInventory(Item item) {
        var inv = MC.player.getInventory();
        int count = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack s = inv.getStack(i);
            if (s.isOf(item)) count += s.getCount();
        }
        return count;
    }
}
