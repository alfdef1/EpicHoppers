package com.songoda.epichoppers.events;

import com.songoda.arconix.Arconix;
import com.songoda.epichoppers.EpicHoppers;
import com.songoda.epichoppers.Lang;
import com.songoda.epichoppers.hopper.Hopper;
import com.songoda.epichoppers.utils.Debugger;
import com.songoda.epichoppers.utils.Methods;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Created by songoda on 3/14/2017.
 */
public class InventoryListeners implements Listener {

    private EpicHoppers plugin = EpicHoppers.pl();

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        try {

            Inventory inv = e.getInventory();
            Player p = (Player) e.getWhoClicked();
            if (inv == null || e.getCurrentItem() == null) return;

            if ((inv != null) && (e.getCursor() != null) && (e.getCurrentItem() != null)) {
                ItemStack c = e.getCursor();
                ItemStack item = e.getCurrentItem();
                if (c.hasItemMeta()
                        && c.getItemMeta().hasLore()
                        && c.getType() == Material.ENCHANTED_BOOK
                        && (item.getType().name().toUpperCase().contains("AXE") || item.getType().name().toUpperCase().contains("SPADE") || item.getType().name().toUpperCase().contains("SWORD"))
                        && c.getItemMeta().getLore().equals(plugin.enchant.getbook().getItemMeta().getLore())) {
                    plugin.enchant.createSyncTouch(item, null);
                    e.setCancelled(true);
                    p.setItemOnCursor(new ItemStack(Material.AIR));
                    p.updateInventory();


                }
            }
            if (!e.getCurrentItem().hasItemMeta()) return;

            if (doWhiteList(e)) return;

            if (e.getSlot() != 64537
                    && e.getInventory().getType() == InventoryType.ANVIL
                    && e.getAction() != InventoryAction.NOTHING
                    && e.getCurrentItem().getType() != Material.AIR) {
                ItemStack item = e.getCurrentItem();
                if (item.getType() == Material.HOPPER) {
                    e.setCancelled(true);
                }
            }

            if (!plugin.inShow.containsKey(p) || plugin.inFilter.containsKey(p)) {
                return;
            }
            e.setCancelled(true);
            Hopper hopper = new Hopper(p);
            if (e.getCurrentItem().getItemMeta().hasDisplayName()
                    && e.getCurrentItem().getItemMeta().getDisplayName().equals(Arconix.pl().format().formatText(Lang.PEARL_TITLE.getConfigValue(null)))
                    && (plugin.getConfig().getBoolean("Main.Allow Players To Teleport Through Hoppers") || p.hasPermission("EpicHoppers.Teleport"))) {
                Block block = plugin.lastBlock.get(p);
                if (e.isLeftClick()) {
                    if (plugin.dataFile.getConfig().contains("data.sync." + Arconix.pl().serialize().serializeLocation(block) + ".block")) {
                        Methods.tpPlayer(p, block);
                    }
                } else {
                    if (plugin.dataFile.getConfig().contains("data.sync." + Arconix.pl().serialize().serializeLocation(block) + ".telewalk")) {
                        p.sendMessage(plugin.references.getPrefix() + Lang.WALKTELE_DISABLE.getConfigValue(null));
                        plugin.dataFile.getConfig().set("data.sync." + Arconix.pl().serialize().serializeLocation(block) + ".telewalk", null);
                    } else {
                        p.sendMessage(plugin.references.getPrefix() + Lang.WALKTELE_ENABLE.getConfigValue(null));
                        plugin.dataFile.getConfig().set("data.sync." + Arconix.pl().serialize().serializeLocation(block) + ".telewalk", true);
                    }
                }
                p.closeInventory();


            } else if (e.getCurrentItem().getItemMeta().hasDisplayName() && e.getCurrentItem().getItemMeta().getDisplayName().equals(Arconix.pl().format().formatText(Lang.FILTER_TITLE.getConfigValue(null))) && p.hasPermission("EpicHoppers.Filter")) {
                if (e.getCurrentItem().getItemMeta().getDisplayName() != "§l") {
                    hopper.filter();
                }
            } else if (e.getSlot() == 11 && p.hasPermission("EpicHoppers.Upgrade.XP")) {
                if (e.getCurrentItem().getItemMeta().getDisplayName() != "§l") {
                    hopper.upgrade("XP");
                    p.closeInventory();
                }
            } else if (e.getSlot() == 15 && p.hasPermission("EpicHoppers.Upgrade.ECO")) {
                if (e.getCurrentItem().getItemMeta().getDisplayName() != "§l") {
                    hopper.upgrade("ECO");
                    p.closeInventory();
                }
            } else if (e.getSlot() == 22) {
                if (e.isRightClick()) {
                    p.sendMessage(plugin.references.getPrefix() + Lang.UNSYNC.getConfigValue(null));
                    plugin.dataFile.getConfig().set("data.sync." + Arconix.pl().serialize().serializeLocation(plugin.lastBlock.get(p)) + ".block", null);
                } else {
                    boolean can = true;
                    if (plugin.dataFile.getConfig().contains("data.sync." + Arconix.pl().serialize().serializeLocation(plugin.lastBlock.get(p)) + ".player")) {
                        if (!plugin.dataFile.getConfig().getString("data.sync." + Arconix.pl().serialize().serializeLocation(plugin.lastBlock.get(p)) + ".player").equals(p.getUniqueId().toString())) {
                            p.sendMessage(plugin.references.getPrefix() + Lang.SYNC_DID_NOT_PLACE.getConfigValue(null));
                            can = false;
                        }
                    }
                    if (can) {
                        plugin.sync.put(p, plugin.lastBlock.get(p));
                        p.sendMessage(plugin.references.getPrefix() + Lang.SYNC_NEXT.getConfigValue(null));
                        hopper.timeout();
                    }
                }
                p.closeInventory();
            }
        } catch (Exception ee) {
            Debugger.runReport(ee);
        }
    }

    private boolean doWhiteList(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        if (!plugin.inFilter.containsKey(p)
                || e.getClickedInventory() == null
                || !e.getClickedInventory().equals(p.getOpenInventory().getTopInventory())) {
            return false;
        }
        if (e.getClick().equals(ClickType.SHIFT_LEFT)) {
            e.setCancelled(true);
            return true;
        }

        Hopper hopper = new Hopper(p);
        hopper.compile(p);
        if (e.getSlot() == 40) {
            plugin.bsync.put(p, plugin.lastBlock.get(p));
            p.sendMessage(plugin.references.getPrefix() + Lang.SYNC_NEXT.getConfigValue(null));
            hopper.timeout();
            p.closeInventory();
            return true;
        }

        int[] a = {0, 1, 9, 10, 18, 19, 27, 28, 36, 37, 45, 46, 7, 8, 16, 17, 25, 26, 34, 35, 43, 44, 52, 53};
        e.setCancelled(true);
        for (int aa : a) {
            if (aa != e.getSlot()) continue;
            String name = "";
            if (e.getCurrentItem().hasItemMeta() && e.getCurrentItem().getItemMeta().hasDisplayName())
                name = e.getCurrentItem().getItemMeta().getDisplayName();
            if (!name.equals(Arconix.pl().format().formatText(Lang.WHITELIST.getConfigValue())) &&
                    !name.equals(Arconix.pl().format().formatText(Lang.BLACKLIST.getConfigValue())) &&
                    !name.equals(Arconix.pl().format().formatText(Lang.VOID.getConfigValue()))) {
                e.setCancelled(false);
                return true;
            }

            if (e.getCursor().getType().equals(Material.AIR)) return true;
            if (e.getCursor().getAmount() != 1) {
                e.setCancelled(true);
                p.sendMessage(plugin.references.getPrefix() + Lang.ONLY_ONE.getConfigValue());
                return true;
            }

            e.setCancelled(false);
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> p.setItemOnCursor(new ItemStack(Material.AIR)), 1L);
            return true;
        }
        return true;
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {

        if (!e.getItemDrop().getItemStack().hasItemMeta() && !e.getItemDrop().getItemStack().getItemMeta().hasDisplayName()) {
            return;
        }
        String name = e.getItemDrop().getItemStack().getItemMeta().getDisplayName();

        if (!name.equals(Arconix.pl().format().formatText(Lang.WHITELIST.getConfigValue())) &&
                !name.equals(Arconix.pl().format().formatText(Lang.BLACKLIST.getConfigValue())) &&
                !name.equals(Arconix.pl().format().formatText(Lang.VOID.getConfigValue()))) {
            return;
        }
        e.getItemDrop().remove();
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        try {
            final Player player = (Player) event.getPlayer();
            plugin.inShow.remove(player);
            if (plugin.inFilter.containsKey(player)) {

                Hopper hopper = new Hopper(player);
                hopper.compile(player);
                plugin.inFilter.remove(player);
            }
        } catch (Exception e) {
            Debugger.runReport(e);
        }
    }
}
