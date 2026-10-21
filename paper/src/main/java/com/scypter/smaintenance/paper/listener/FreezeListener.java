package com.scypter.smaintenance.paper.listener;

import com.scypter.smaintenance.paper.PaperRuntime;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;

public final class FreezeListener implements Listener {
    private final PaperRuntime rt;

    public FreezeListener(PaperRuntime rt) {
        this.rt = rt;
    }

    private boolean frozen(Player p) {
        return rt.manager.isEnabled() && rt.viewOnly() && !rt.hasBypass(p);
    }

    private void block(Player p, Cancellable e) {
        if (frozen(p)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBreak(BlockBreakEvent e) {
        block(e.getPlayer(), e);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlace(BlockPlaceEvent e) {
        block(e.getPlayer(), e);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent e) {
        block(e.getPlayer(), e);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDrop(PlayerDropItemEvent e) {
        block(e.getPlayer(), e);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    @SuppressWarnings("deprecation")
    public void onPickup(PlayerPickupItemEvent e) {
        block(e.getPlayer(), e);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent e) {
        block(e.getPlayer(), e);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onMove(PlayerMoveEvent e) {
        if (!frozen(e.getPlayer())) {
            return;
        }
        if (e.getFrom().getX() != e.getTo().getX() || e.getFrom().getZ() != e.getTo().getZ() || e.getFrom().getY() != e.getTo().getY()) {
            e.setTo(e.getFrom());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player && frozen((Player) e.getEntity())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventory(InventoryClickEvent e) {
        if (e.getWhoClicked() instanceof Player && !(e.getInventory().getHolder() instanceof com.scypter.smaintenance.paper.gui.GuiHolder)) {
            block((Player) e.getWhoClicked(), e);
        }
    }
}
