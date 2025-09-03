package pl.drytcha.venquests.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import pl.drytcha.venquests.VenQuests;
import pl.drytcha.venquests.player.PlayerData;

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
                plugin.getLogger().info("Załadowano dane dla gracza " + player.getName());
            });
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        // Asynchroniczne zapisywanie danych i usuwanie z cache
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            PlayerData data = plugin.getPlayerManager().getPlayerData(player.getUniqueId());
            if (data != null) {
                plugin.getDatabaseManager().savePlayerData(data);

                // Usunięcie gracza z mapy w głównym wątku, aby uniknąć problemów
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    plugin.getPlayerManager().removePlayer(player.getUniqueId());
                    plugin.getLogger().info("Zapisano i usunięto z cache dane dla gracza " + player.getName());
                });
            }
        });
    }
}

