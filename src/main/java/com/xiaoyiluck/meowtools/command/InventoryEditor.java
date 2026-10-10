package com.xiaoyiluck.meowtools.command;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.*;
import org.bukkit.Material;

/** All transfers use live inventories on the server thread; GUI copies never commit. */
final class InventoryEditor {
    private InventoryEditor() { }

    static int targetSlot(int viewSlot) {
        if (viewSlot >= 9 && viewSlot <= 35) return viewSlot;
        if (viewSlot >= 45 && viewSlot <= 53) return viewSlot - 45;
        return switch (viewSlot) { case 0 -> 39; case 1 -> 38; case 2 -> 37; case 3 -> 36; case 4 -> 40; default -> -1; };
    }

    static void edit(Player viewer, Player target, InventoryView view, int rawSlot, ClickType click, int hotbar) {
        int slot = targetSlot(rawSlot);
        if (slot < 0) return;
        PlayerInventory inventory = target.getInventory();
        ItemStack current = copy(inventory.getItem(slot));
        if (click == ClickType.NUMBER_KEY && hotbar >= 0 && hotbar < 9) {
            if (viewer.equals(target) && slot == hotbar) return;
            ItemStack incoming = copy(viewer.getInventory().getItem(hotbar));
            if (!fits(incoming, slot)) return;
            inventory.setItem(slot, incoming);
            viewer.getInventory().setItem(hotbar, current);
            return;
        }
        if (click.isShiftClick()) {
            if (viewer.equals(target) || current == null) return;
            var remaining = viewer.getInventory().addItem(current);
            inventory.setItem(slot, copy(remaining.get(0)));
            return;
        }
        if (click != ClickType.LEFT && click != ClickType.RIGHT) return;
        ItemStack cursor = copy(view.getCursor());
        if (cursor == null) {
            if (current == null) return;
            int take = click == ClickType.RIGHT ? (current.getAmount() + 1) / 2 : current.getAmount();
            cursor = current.clone();
            cursor.setAmount(take);
            current.setAmount(current.getAmount() - take);
        } else {
            if (!fitsType(cursor, slot)) return;
            int limit = slot >= 36 && slot <= 39 ? 1 : cursor.getMaxStackSize();
            if (current == null || current.isSimilar(cursor)) {
                int present = current == null ? 0 : current.getAmount();
                int put = Math.min(click == ClickType.RIGHT ? 1 : cursor.getAmount(), Math.max(0, limit - present));
                if (put == 0) return;
                if (current == null) current = cursor.clone();
                current.setAmount(present + put);
                cursor.setAmount(cursor.getAmount() - put);
            } else {
                if (!fits(cursor, slot)) return;
                ItemStack previous = current;
                current = cursor;
                cursor = previous;
            }
        }
        inventory.setItem(slot, copy(current));
        view.setCursor(copy(cursor));
    }

    private static boolean fits(ItemStack item, int slot) {
        return item == null || (fitsType(item, slot) && (slot < 36 || slot > 39 || item.getAmount() == 1));
    }

    private static boolean fitsType(ItemStack item, int slot) {
        if (item == null || slot < 36 || slot > 39) return true;
        EquipmentSlot expected = switch (slot) {
            case 39 -> EquipmentSlot.HEAD; case 38 -> EquipmentSlot.CHEST;
            case 37 -> EquipmentSlot.LEGS; default -> EquipmentSlot.FEET;
        };
        return item.getType().getEquipmentSlot() == expected;
    }

    static ItemStack copy(ItemStack item) {
        return item == null || (item.getType() == Material.AIR || item.getType() == Material.CAVE_AIR || item.getType() == Material.VOID_AIR) || item.getAmount() <= 0 ? null : item.clone();
    }
}
