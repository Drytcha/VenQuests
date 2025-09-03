package pl.drytcha.venquests.commands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import pl.drytcha.venquests.VenQuests;
import pl.drytcha.venquests.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CommandManager implements CommandExecutor, TabCompleter {

    private final VenQuests plugin;

    public CommandManager(VenQuests plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("misje")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(Utils.getMessage("player_only_command"));
                return true;
            }
            Player player = (Player) sender;
            if (!player.hasPermission("venquests.player.use")) {
                player.sendMessage(Utils.getMessage("no_permission"));
                return true;
            }
            // Sprawdzamy, czy dane gracza są już załadowane
            if (plugin.getPlayerManager().getPlayerData(player.getUniqueId()) == null) {
                player.sendMessage(Utils.colorize("&cTwoje dane są wciąż wczytywane. Spróbuj ponownie za chwilę."));
                return true;
            }
            plugin.getGui().openMainMenu(player);
            return true;
        }

        if (command.getName().equalsIgnoreCase("venquests")) {
            if (!sender.hasPermission("venquests.admin")) {
                sender.sendMessage(Utils.getMessage("no_permission"));
                return true;
            }
            if (args.length == 0) {
                sender.sendMessage(Utils.colorize("&6VenQuests &7- Dostępne komendy:"));
                sender.sendMessage(Utils.colorize("&e/vq reload &8- &7Przeładowuje konfigurację."));
                sender.sendMessage(Utils.colorize("&e/vq reset <gracz> <typ> &8- &7Resetuje misje gracza."));
                return true;
            }

            if (args[0].equalsIgnoreCase("reload")) {
                plugin.reloadConfig();
                Utils.loadMessages(plugin);
                plugin.getQuestManager().loadQuests();
                sender.sendMessage(Utils.getMessage("reload_success"));
                return true;
            }

            if (args[0].equalsIgnoreCase("reset")) {
                if (args.length < 3) {
                    sender.sendMessage(Utils.getMessage("reset_usage"));
                    return true;
                }

                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                if (!target.hasPlayedBefore() && !target.isOnline()) {
                    sender.sendMessage(Utils.getMessage("player_not_found").replace("%player%", args[1]));
                    return true;
                }

                String type = args[2].toLowerCase();
                plugin.getDatabaseManager().resetPlayerData(target.getUniqueId(), type);
                sender.sendMessage(Utils.getMessage("reset_success")
                        .replace("%player%", target.getName())
                        .replace("%type%", type));
                return true;
            }
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (command.getName().equalsIgnoreCase("venquests") && sender.hasPermission("venquests.admin")) {
            if (args.length == 1) {
                completions.add("reload");
                completions.add("reset");
            } else if (args.length == 2 && args[0].equalsIgnoreCase("reset")) {
                completions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
            } else if (args.length == 3 && args[0].equalsIgnoreCase("reset")) {
                completions.addAll(Arrays.asList("daily", "weekly", "monthly", "all"));
            }
        }
        return completions.stream().filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase())).collect(Collectors.toList());
    }
}

