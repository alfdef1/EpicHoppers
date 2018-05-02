package com.songoda.epichoppers.events;

import com.songoda.epichoppers.EpicHoppers;
import com.songoda.epichoppers.Lang;
import com.songoda.epichoppers.hopper.Hopper;
import com.songoda.epichoppers.utils.Debugger;
import com.songoda.epichoppers.utils.Methods;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

/**
 * Created by songoda on 3/14/2017.
 */
public class InteractListeners implements Listener {

    private EpicHoppers plugin = EpicHoppers.pl();

    @EventHandler
    public void onBlockInteract(PlayerInteractEvent e) {
        try {
            Player p = e.getPlayer();
            if (e.getAction() != Action.LEFT_CLICK_BLOCK
                    || e.getClickedBlock() == null
                    || p.isSneaking()
                    || !p.hasPermission("EpicHoppers.Use")
                    || !plugin.hooks.canBuild(p, e.getClickedBlock().getLocation())
                    || !(e.getClickedBlock().getState() instanceof InventoryHolder || e.getClickedBlock().getType().equals(Material.ENDER_CHEST))) {
                return;
            }

            if (e.getClickedBlock().getType() == Material.CHEST && Methods.isSync(p)) {
                ItemStack item = e.getPlayer().getItemInHand();
                String name = (e.getPlayer().getItemInHand().getType().name().substring(0, 1).toUpperCase() + e.getPlayer().getItemInHand().getType().name().toLowerCase().substring(1)).replace("_", " ");
                if (item.getItemMeta().getLore().size() == 2) {
                    p.sendMessage(Lang.UNSYNC_CHEST.getConfigValue(name));
                    plugin.enchant.createSyncTouch(item, null);
                } else {
                    p.sendMessage(Lang.SYNC_CHEST.getConfigValue(name));
                    plugin.enchant.createSyncTouch(item, e.getClickedBlock());
                }
                e.setCancelled(true);
                return;
            }

            if (!plugin.sync.containsKey(p) && !plugin.bsync.containsKey(p)) {
                if (e.getClickedBlock().getType() == Material.HOPPER) {
                    plugin.lastBlock.put(p, e.getClickedBlock());
                    Hopper hopper = new Hopper(p);
                    if (plugin.getConfig().getBoolean("Main.Allow Hopper Upgrading")) {
                        if (p.getItemInHand().getType() != Material.WOOD_PICKAXE && p.getItemInHand().getType() != Material.STONE_PICKAXE &&
                                p.getItemInHand().getType() != Material.IRON_PICKAXE && p.getItemInHand().getType() != Material.DIAMOND_PICKAXE) {
                            hopper.view();
                            e.setCancelled(true);
                        }
                    } else {
                        if (p.hasPermission("EpicHoppers.Admin")) {
                            plugin.sync.put(p, plugin.lastBlock.get(p));
                            p.sendMessage(Lang.SYNC_NEXT.getConfigValue(null));
                            hopper.timeout();
                            p.closeInventory();
                        }
                        e.setCancelled(true);
                    }
                }
                return;
            }

            if (e.getClickedBlock().getType() == Material.BREWING_STAND) return;

            if (e.getClickedBlock().getState() instanceof InventoryHolder || e.getClickedBlock().getType().equals(Material.ENDER_CHEST) && plugin.getConfig().getBoolean("Main.Support Enderchests")) {
                if (plugin.sync.containsKey(p) && plugin.sync.get(p).equals(e.getClickedBlock()) || plugin.bsync.containsKey(p) && plugin.bsync.get(p).equals(e.getClickedBlock())) {
                    p.sendMessage(Lang.SYNC_SELF.getConfigValue(null));
                } else {
                    Hopper hopper = new Hopper(p);
                    if (plugin.sync.containsKey(p))
                        hopper.sync(e.getClickedBlock(), false);
                    else if (plugin.bsync.containsKey(p))
                        hopper.sync(e.getClickedBlock(), true);
                }
                e.setCancelled(true);
                plugin.sync.remove(p);
                plugin.bsync.remove(p);
            }


        } catch (Exception ee) {
            Debugger.runReport(ee);
        }
    }
}
