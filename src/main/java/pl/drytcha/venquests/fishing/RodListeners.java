package pl.drytcha.venquests.fishing;

import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import pl.drytcha.venquests.VenQuests;
import pl.drytcha.venquests.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class RodListeners implements Listener {

    private final VenQuests plugin;
    private final RodManager rodManager;

    public RodListeners(VenQuests plugin, RodManager rodManager) {
        this.plugin = plugin;
        this.rodManager = rodManager;
    }

    @EventHandler
    public void onPlayerFish(PlayerFishEvent event) {
        Player player = event.getPlayer();
        ItemStack usedItem = player.getInventory().getItem(event.getHand());

        if (!rodManager.isSpecialRod(usedItem)) {
            return;
        }

        if (event.getHand() == EquipmentSlot.OFF_HAND) {
            event.setCancelled(true);
            return;
        }

        PlayerFishEvent.State state = event.getState();
        FishingRodTask task = plugin.getActiveFishingTasks().get(player.getUniqueId());
        boolean taskExists = task != null;

        if (state == PlayerFishEvent.State.FISHING && !taskExists) {
            event.setCancelled(true);
            castTripleHook(player);
        }
        else if (state == PlayerFishEvent.State.REEL_IN && taskExists) {
            task.cancel();
            plugin.getActiveFishingTasks().remove(player.getUniqueId());
        }
        else if (taskExists) {
            event.setCancelled(true);
            task.cancelTask(false);
        }
    }

    private void castTripleHook(Player player) {
        if (plugin.getActiveFishingTasks().containsKey(player.getUniqueId())) {
            plugin.getActiveFishingTasks().get(player.getUniqueId()).cancelTask(false);
        }

        List<FishHook> hooks = new ArrayList<>();
        Vector direction = player.getEyeLocation().getDirection();

        hooks.add(player.launchProjectile(FishHook.class, direction));

        Vector leftVector = direction.clone().rotateAroundY(Math.toRadians(-10));
        hooks.add(player.launchProjectile(FishHook.class, leftVector));

        Vector rightVector = direction.clone().rotateAroundY(Math.toRadians(10));
        hooks.add(player.launchProjectile(FishHook.class, rightVector));

        FishingRodTask task = new FishingRodTask(player, hooks, rodManager);
        task.runTaskTimer(plugin, 0L, 20L);
        plugin.getActiveFishingTasks().put(player.getUniqueId(), task);
    }


    @EventHandler
    public void onHeldItemChange(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        if (plugin.getActiveFishingTasks().containsKey(player.getUniqueId())) {
            ItemStack previousItem = player.getInventory().getItem(event.getPreviousSlot());
            if (rodManager.isSpecialRod(previousItem)) {
                plugin.getActiveFishingTasks().get(player.getUniqueId()).cancelTask(false);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (plugin.getActiveFishingTasks().containsKey(player.getUniqueId())) {
            plugin.getActiveFishingTasks().get(player.getUniqueId()).cancelTask(false);
        }
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        for (ItemStack item : event.getInventory().getContents()) {
            if (rodManager.isSpecialRod(item)) {
                event.setResult(null);
                return;
            }
        }
    }

    @EventHandler
    public void onCraftItem(CraftItemEvent event) {
        for (ItemStack item : event.getInventory().getMatrix()) {
            if (rodManager.isSpecialRod(item)) {
                event.setCancelled(true);
                if (event.getWhoClicked() instanceof Player) {
                    ((Player) event.getWhoClicked()).sendMessage(Utils.getMessage("rod_crafting_blocked"));
                }
                return;
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        InventoryType topInventoryType = topInventory.getType();

        // Sprawdza, czy górny ekwipunek jest "zakazany"
        boolean isForbiddenInventory = false;
        if (topInventoryType == InventoryType.ANVIL || topInventoryType == InventoryType.WORKBENCH) {
            isForbiddenInventory = true;
        } else if (topInventoryType == InventoryType.CRAFTING && topInventory.getSize() == 5) { // Crafting 2x2 gracza
            isForbiddenInventory = true;
        }

        if (!isForbiddenInventory) {
            return;
        }

        if (event.isShiftClick()) {
            if (event.getClickedInventory() != null && event.getClickedInventory().getType() == InventoryType.PLAYER) {
                ItemStack clickedItem = event.getCurrentItem();
                if (rodManager.isSpecialRod(clickedItem)) {
                    event.setCancelled(true);
                }
            }
            return;
        }

        ItemStack cursorItem = event.getCursor();
        if (rodManager.isSpecialRod(cursorItem)) {
            if (event.getRawSlot() < topInventory.getSize()) {
                event.setCancelled(true);
            }
            return;
        }

        if (event.getClick() == ClickType.NUMBER_KEY) {
            if (event.getRawSlot() < topInventory.getSize()) {
                ItemStack hotbarItem = event.getWhoClicked().getInventory().getItem(event.getHotbarButton());
                if (rodManager.isSpecialRod(hotbarItem)) {
                    event.setCancelled(true);
                }
            }
        }
    }
}

