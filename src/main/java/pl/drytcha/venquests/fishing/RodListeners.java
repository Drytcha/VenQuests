package pl.drytcha.venquests.fishing;

import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import pl.drytcha.venquests.VenQuests;

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
        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        if (!rodManager.isSpecialRod(itemInHand)) {
            return;
        }

        event.setCancelled(true); // Zawsze anulujemy domyślny event

        if (plugin.getActiveFishingTasks().containsKey(player.getUniqueId())) {
            plugin.getActiveFishingTasks().get(player.getUniqueId()).cancelTask(false);
        } else {
            if (event.getState() == PlayerFishEvent.State.FISHING) {
                castTripleHook(player);
            }
        }
    }

    private void castTripleHook(Player player) {
        List<FishHook> hooks = new ArrayList<>();
        Vector direction = player.getEyeLocation().getDirection();

        hooks.add(player.launchProjectile(FishHook.class, direction));

        Vector leftVector = direction.clone().rotateAroundY(Math.toRadians(-10));
        hooks.add(player.launchProjectile(FishHook.class, leftVector));

        Vector rightVector = direction.clone().rotateAroundY(Math.toRadians(10));
        hooks.add(player.launchProjectile(FishHook.class, rightVector));

        FishingRodTask task = new FishingRodTask(player, hooks, rodManager);
        task.runTaskTimer(plugin, 0L, 20L); // Sprawdzanie co sekundę
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
}

