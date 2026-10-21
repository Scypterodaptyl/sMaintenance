package com.scypter.smaintenance.paper.gui;

import com.scypter.smaintenance.core.message.Messages;
import com.scypter.smaintenance.paper.PaperRuntime;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collections;

public final class GuiMenu implements Listener {
    private static final int SLOT_TOGGLE = 11;
    private static final int SLOT_EXTEND_SHORT = 13;
    private static final int SLOT_EXTEND_LONG = 14;
    private static final int SLOT_STATUS = 15;
    private static final int SLOT_WHITELIST = 22;

    private final PaperRuntime rt;

    public GuiMenu(PaperRuntime rt) {
        this.rt = rt;
    }

    private static Material material(String... names) {
        for (String n : names) {
            try {
                return Material.valueOf(n);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return Material.STONE;
    }

    private ItemStack item(Material m, String key, java.util.Map<String, String> ph) {
        ItemStack it = new ItemStack(m, 1);
        ItemMeta meta = it.getItemMeta();
        meta.setDisplayName(rt.text.legacyKick(rt.messages.raw(key, ph)));
        it.setItemMeta(meta);
        return it;
    }

    public void open(Player p) {
        GuiHolder holder = new GuiHolder();
        String title = rt.text.legacyKick(rt.messages.raw("gui-title", Messages.ph()));
        if (title.length() > 32) {
            title = title.substring(0, 32);
        }
        Inventory inv = Bukkit.createInventory(holder, 27, title);
        holder.setInventory(inv);
        render(inv);
        p.openInventory(inv);
    }

    private void render(Inventory inv) {
        boolean on = rt.manager.isEnabled();
        inv.clear();
        inv.setItem(SLOT_TOGGLE, on
                ? item(material("REDSTONE_BLOCK"), "gui-disable", Messages.ph())
                : item(material("EMERALD_BLOCK"), "gui-enable", Messages.ph()));
        inv.setItem(SLOT_EXTEND_SHORT, item(material("CLOCK", "WATCH"), "gui-extend", Messages.ph("time", "10m")));
        inv.setItem(SLOT_EXTEND_LONG, item(material("CLOCK", "WATCH"), "gui-extend", Messages.ph("time", "1h")));
        inv.setItem(SLOT_STATUS, item(material("PAPER"), "gui-status", Messages.ph("state", on ? "ON" : "OFF")));
        inv.setItem(SLOT_WHITELIST, item(material("BOOK"), "gui-whitelist", Messages.ph("count", String.valueOf(rt.whitelist.size()))));
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof GuiHolder)) {
            return;
        }
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player)) {
            return;
        }
        Player p = (Player) e.getWhoClicked();
        int slot = e.getRawSlot();
        if (slot == SLOT_TOGGLE) {
            if (!p.hasPermission("smaintenance.command.toggle")) {
                return;
            }
            if (rt.manager.isEnabled()) {
                rt.tryDisable(p.getName());
            } else {
                rt.tryEnable(p.getName(), 0, "");
            }
        } else if (slot == SLOT_EXTEND_SHORT || slot == SLOT_EXTEND_LONG) {
            if (p.hasPermission("smaintenance.command.toggle")) {
                rt.manager.extend(slot == SLOT_EXTEND_SHORT ? 600_000L : 3_600_000L);
            }
        } else if (slot == SLOT_WHITELIST) {
            java.util.List<String> names = new java.util.ArrayList<String>(rt.whitelist.entries().values());
            StringBuilder sb = new StringBuilder();
            for (String n : names) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(n);
            }
            rt.text.message(p, rt.messages.get("whitelist-list", Messages.ph("count", String.valueOf(names.size()), "list", names.isEmpty() ? "-" : sb.toString())));
        }
        render(e.getInventory());
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof GuiHolder) {
            e.setCancelled(true);
        }
    }
}
