package com.songoda.epichoppers.hopper.levels.modules;

import com.songoda.core.compatibility.CompatibleMaterial;
import com.songoda.core.compatibility.CompatibleParticleHandler;
import com.songoda.core.locale.Locale;
import com.songoda.core.utils.TextUtils;
import com.songoda.epichoppers.EpicHoppers;
import com.songoda.epichoppers.hopper.Hopper;
import com.songoda.epichoppers.settings.Settings;
import com.songoda.epichoppers.utils.Methods;
import com.songoda.epichoppers.utils.StorageContainerCache;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class ModuleSuction extends Module {

    private final int maxSearchRadius;

    private static List<UUID> blacklist = new ArrayList<>();

    private static final boolean wildStacker = Bukkit.getPluginManager().isPluginEnabled("WildStacker");
    private static final boolean ultimateStacker = Bukkit.getPluginManager().isPluginEnabled("UltimateStacker");

    public ModuleSuction(EpicHoppers plugin, int amount) {
        super(plugin);
        this.maxSearchRadius = amount;
    }

    @Override
    public String getName() {
        return "Suction";
    }

    @Override
    public void run(Hopper hopper, StorageContainerCache.Cache hopperCache) {
        double radius = getRadius(hopper) + .5;

        if (!isEnabled(hopper)) return;

        Set<Item> itemsToSuck = hopper.getLocation().getWorld().getNearbyEntities(hopper.getLocation().add(0.5, 0.5, 0.5), radius, radius, radius)
                .stream()
                .filter(entity -> entity.getType() == EntityType.DROPPED_ITEM
                        && !entity.isDead()
                        && entity.getTicksLived() >= ((Item) entity).getPickupDelay()
                        && entity.getLocation().getBlock().getType() != Material.HOPPER)
                .map(Item.class::cast)
                .collect(Collectors.toSet());

        if (itemsToSuck.isEmpty())
            return;

        boolean filterEndpoint = hopper.getFilter().getEndPoint() != null;

        Inventory hopperInventory = null;
        if (Settings.EMIT_INVENTORYPICKUPITEMEVENT.getBoolean()) {
            InventoryHolder inventoryHolder = (InventoryHolder) hopper.getBlock().getState();
            hopperInventory = Bukkit.createInventory(inventoryHolder, InventoryType.HOPPER);
        }

        for (Item item : itemsToSuck) {

            ItemStack itemStack = item.getItemStack();

            if (item.getPickupDelay() == 0) {
                item.setPickupDelay(25);
                continue;
            }

            if (itemStack.getType().name().contains("SHULKER_BOX"))
                return;

            if (itemStack.hasItemMeta() && itemStack.getItemMeta().hasDisplayName() &&
                    itemStack.getItemMeta().getDisplayName().startsWith("***")) {
                return; //Compatibility with Shop instance: https://www.spigotmc.org/resources/shop-a-simple-intuitive-shop-instance.9628/
            }

            if (blacklist.contains(item.getUniqueId()))
                return;

            // respect filter if no endpoint
            if (!filterEndpoint
                    && !(hopper.getFilter().getWhiteList().isEmpty() && hopper.getFilter().getBlackList().isEmpty())) {
                // this hopper has a filter with no rejection endpoint, so don't absorb disalowed items
                // whitelist has priority
                if (!hopper.getFilter().getWhiteList().isEmpty()) {
                    // is this item on the whitelist?
                    if (hopper.getFilter().getWhiteList().stream().noneMatch(filterItem -> Methods.isSimilarMaterial(itemStack, filterItem))) {
                        // nope!
                        continue;
                    }
                } else {
                    // check the blacklist
                    if (hopper.getFilter().getBlackList().stream().anyMatch(filterItem -> Methods.isSimilarMaterial(itemStack, filterItem))) {
                        // don't grab this, then
                        continue;
                    }
                }
            }

            if (Settings.EMIT_INVENTORYPICKUPITEMEVENT.getBoolean()) {
                hopperInventory.setContents(hopperCache.cachedInventory);
                InventoryPickupItemEvent pickupevent = new InventoryPickupItemEvent(hopperInventory, item);
                Bukkit.getPluginManager().callEvent(pickupevent);
                if (pickupevent.isCancelled())
                    continue;
            }

            // try to add the items to the hopper
            int toAdd, added = hopperCache.addAny(itemStack, toAdd = getActualItemAmount(item));
            if (added == 0)
                return;

            // items added ok!
            if (added == toAdd)
                item.remove();
            else {
                // update the item's total
                updateAmount(item, toAdd - added);

                // wait before trying to add again
                blacklist.add(item.getUniqueId());
                Bukkit.getScheduler().runTaskLater(EpicHoppers.getInstance(),
                        () -> blacklist.remove(item.getUniqueId()), 10L);
            }

            float xx = (float) (0 + (Math.random() * .1));
            float yy = (float) (0 + (Math.random() * .1));
            float zz = (float) (0 + (Math.random() * .1));
            CompatibleParticleHandler.spawnParticles(CompatibleParticleHandler.ParticleType.FLAME,
                    item.getLocation(), 5, xx, yy, zz);
        }
    }

    private int getActualItemAmount(Item item) {
        return item.getItemStack().getAmount();
    }

    private void updateAmount(Item item, int amount) {
        item.getItemStack().setAmount(Math.min(amount, item.getItemStack().getMaxStackSize()));
    }

    public static boolean isBlacklisted(UUID uuid) {
        return blacklist.contains(uuid);
    }

    @Override
    public ItemStack getGUIButton(Hopper hopper) {
        Locale locale = EpicHoppers.getInstance().getLocale();
        ItemStack item = CompatibleMaterial.CAULDRON.getItem();
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(locale.getMessage("interface.hopper.suctiontitle").getMessage());
        List<String> lore = new ArrayList<>();
        String[] parts = locale.getMessage("interface.hopper.suctionlore")
                .processPlaceholder("status", isEnabled(hopper) ? locale.getMessage("general.word.enabled").getMessage() : locale.getMessage("general.word.disabled").getMessage())
                .processPlaceholder("radius", getRadius(hopper)).getMessage().split("\\|");
        for (String line : parts) {
            lore.add(TextUtils.formatText(line));
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public void runButtonPress(Player player, Hopper hopper, ClickType type) {
        if (type == ClickType.LEFT) {
            toggleEnabled(hopper);
        } else if (type == ClickType.RIGHT) {
            int setRadius = getRadius(hopper);
            if (setRadius >= maxSearchRadius) {
                setRadius(hopper, 1);
            } else {
                setRadius(hopper, ++setRadius);
            }
        }
    }


    private boolean isEnabled(Hopper hopper) {
        Object obj = getData(hopper, "enabled");
        return obj == null || (boolean) obj;
    }

    private void toggleEnabled(Hopper hopper) {
        saveData(hopper, "enabled", !isEnabled(hopper));
    }

    private int getRadius(Hopper hopper) {
        Object foundRadius = getData(hopper, "radius");
        return foundRadius == null ? maxSearchRadius : (int) foundRadius;
    }

    private void setRadius(Hopper hopper, int radius) {
        saveData(hopper, "radius", radius);
    }

    @Override
    public List<Material> getBlockedItems(Hopper hopper) {
        return null;
    }

    @Override
    public String getDescription() {
        return EpicHoppers.getInstance().getLocale().getMessage("interface.hopper.suction")
                .processPlaceholder("suction", maxSearchRadius).getMessage();
    }
}
