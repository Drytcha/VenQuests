package pl.drytcha.venquests.player;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;
import pl.drytcha.venquests.VenQuests;

import java.util.Map;
import java.util.UUID;

public class PlayerEvents implements Listener {
    private final VenQuests plugin;

    public PlayerEvents(VenQuests plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Asynchroniczne wczytywanie danych gracza
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            PlayerData data = plugin.getDatabaseManager().loadPlayerData(player.getUniqueId());

            // Powrót do głównego wątku serwera, aby bezpiecznie dodać dane do mapy
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                plugin.getPlayerManager().addPlayer(player.getUniqueId(), data);
//                plugin.getLogger().info("Załadowano dane dla gracza " + player.getName());
            });
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        // Anuluj zadanie odświeżania GUI, jeśli istnieje
        Map<UUID, BukkitTask> tasks = plugin.getGuiUpdateTasks();
        if (tasks.containsKey(playerUUID)) {
            tasks.get(playerUUID).cancel();
            tasks.remove(playerUUID);
        }

        // Asynchroniczne zapisywanie danych i usuwanie z cache
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            PlayerData data = plugin.getPlayerManager().getPlayerData(playerUUID);
            if (data != null) {
                plugin.getDatabaseManager().savePlayerData(data);

                // Usunięcie gracza z mapy w głównym wątku, aby uniknąć problemów
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    plugin.getPlayerManager().removePlayer(playerUUID);
//                    plugin.getLogger().info("Zapisano i usunięto z cache dane dla gracza " + player.getName());
                });
            }
        });
    }
}

