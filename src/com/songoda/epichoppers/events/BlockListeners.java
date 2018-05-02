package com.songoda.epichoppers.events;

import com.songoda.arconix.Arconix;
import com.songoda.epichoppers.EpicHoppers;
import com.songoda.epichoppers.Lang;
import com.songoda.epichoppers.utils.Debugger;
import com.songoda.epichoppers.utils.Methods;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * Created by songoda on 3/14/2017.
 */
public class BlockListeners implements Listener {

    private EpicHoppers plugin = EpicHoppers.pl();

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent e) {
        try {
            if (e.getBlock().getType().equals(Material.ENDER_CHEST)) {
                plugin.dataFile.getConfig().set("data.enderTracker." + Arconix.pl().serialize().serializeLocation(e.getBlock()), e.getPlayer().getUniqueId().toString());
                return;
            }

            if (e.getBlock().getType() != Material.HOPPER) return;

            int amt = count(e.getBlock().getChunk());
            if (amt >= plugin.getConfig().getInt("Main.Max Hoppers Per Chunk") && plugin.getConfig().getInt("Main.Max Hoppers Per Chunk") != -1) {
                e.getPlayer().sendMessage(Lang.TOO_MANY.getConfigValue(plugin.getConfig().getInt("Main.Max Hoppers Per Chunk")));
                e.setCancelled(true);
                return;
            }

            if (!e.getItemInHand().getItemMeta().hasDisplayName()) return;

            ItemStack item = e.getItemInHand().clone();

            byte b = e.getBlock().getData();
            e.getBlock().setType(Material.AIR);
            e.getBlock().getLocation().getBlock().setType(Material.HOPPER);
            e.getBlock().getLocation().getBlock().setData(b);

            if (plugin.getApi().getILevel(item) != 1) {
                plugin.dataFile.getConfig().set("data.sync." + Arconix.pl().serialize().serializeLocation(e.getBlock()) + ".level", plugin.getApi().getILevel(item));
            }


        } catch (Exception ee) {
            Debugger.runReport(ee);
        }
    }

    public int count(Chunk c) {
        try {
            int count = 0;
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = 0; y < 256; y++) {
                        if (c.getBlock(x, y, z).getType() == Material.HOPPER) count++;
                    }
                }
            }
            return count;
        } catch (Exception e) {
            Debugger.runReport(e);
        }
        return 9999;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        try {
            if (!e.isCancelled())
                if (e.getBlock().getType().equals(Material.ENDER_CHEST)) {
                    plugin.dataFile.getConfig().set("data.enderTracker." + Arconix.pl().serialize().serializeLocation(e.getBlock()), null);
                }

            if (e.getPlayer().getItemInHand() == null) return;

            handlePick(e);

            if (e.getBlock().getType() != Material.HOPPER) return;
            int level = plugin.dataFile.getConfig().getInt("data.sync." + Arconix.pl().serialize().serializeLocation(e.getBlock()) + ".level");
            if (level != 0) {
                e.setCancelled(true);
                ItemStack item = new ItemStack(Material.HOPPER, 1);
                ItemMeta itemmeta = item.getItemMeta();
                itemmeta.setDisplayName(Arconix.pl().format().formatText(Methods.formatName(level, true)));
                item.setItemMeta(itemmeta);

                e.getBlock().setType(Material.AIR);
                e.getBlock().getLocation().getWorld().dropItemNaturally(e.getBlock().getLocation(), item);
            }

            if (plugin.dataFile.getConfig().contains("data.sync." + Arconix.pl().serialize().serializeLocation(e.getBlock()) + ".whitelist")) {
                List<ItemStack> owhite = (List<ItemStack>) plugin.dataFile.getConfig().getList("data.sync." + Arconix.pl().serialize().serializeLocation(e.getBlock()) + ".whitelist");
                for (ItemStack i : owhite) {
                    if (i != null)
                        e.getBlock().getLocation().getWorld().dropItemNaturally(e.getBlock().getLocation(), i);
                }
            }
            if (plugin.dataFile.getConfig().contains("data.sync." + Arconix.pl().serialize().serializeLocation(e.getBlock()) + ".blacklist")) {
                List<ItemStack> oblack = (List<ItemStack>) plugin.dataFile.getConfig().getList("data.sync." + Arconix.pl().serialize().serializeLocation(e.getBlock()) + ".blacklist");
                for (ItemStack i : oblack) {
                    if (i != null)
                        e.getBlock().getLocation().getWorld().dropItemNaturally(e.getBlock().getLocation(), i);
                }
            }
            if (plugin.dataFile.getConfig().contains("data.sync." + Arconix.pl().serialize().serializeLocation(e.getBlock()) + ".void")) {
                List<ItemStack> ovoid = (List<ItemStack>) plugin.dataFile.getConfig().getList("data.sync." + Arconix.pl().serialize().serializeLocation(e.getBlock()) + ".void");
                for (ItemStack i : ovoid) {
                    if (i != null)
                        e.getBlock().getLocation().getWorld().dropItemNaturally(e.getBlock().getLocation(), i);
                }
            }
            plugin.dataFile.getConfig().set("data.sync." + Arconix.pl().serialize().serializeLocation(e.getBlock()), null);
            plugin.sync.remove(e.getPlayer());


        } catch (Exception ee) {
            Debugger.runReport(ee);
        }
    }

    private void handlePick(BlockBreakEvent e) {
        if (!Methods.isSync(e.getPlayer())) return;
        ItemStack item = e.getPlayer().getItemInHand();
        ItemMeta meta = item.getItemMeta();
        if (item.getItemMeta().getLore().size() != 2) return;
        Location location = Arconix.pl().serialize().unserializeLocation(meta.getLore().get(1).replaceAll("§", ""));
        if (location.getBlock().getType() != Material.CHEST) return;
        InventoryHolder ih = (InventoryHolder) location.getBlock().getState();
        if (e.getPlayer().getItemInHand().getItemMeta().hasEnchant(Enchantment.SILK_TOUCH)) {
            ih.getInventory().addItem(new ItemStack(e.getBlock().getType()));
        } else {
            for (ItemStack is : e.getBlock().getDrops())
                ih.getInventory().addItem(is);
        }
        if (plugin.v1_12) {
            e.setDropItems(false);
            return;
        }

        e.isCancelled();
        short dur = e.getPlayer().getItemInHand().getDurability();
        e.getPlayer().getItemInHand().setDurability((short) (dur + 1));
        if (e.getExpToDrop() > 0)
            e.getPlayer().getWorld().spawn(e.getBlock().getLocation(), ExperienceOrb.class).setExperience(e.getExpToDrop());
        e.getBlock().setType(Material.AIR);
    }

}
