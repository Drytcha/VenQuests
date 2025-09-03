package pl.drytcha.venquests.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pl.drytcha.venquests.VenQuests;
import pl.drytcha.venquests.config.Quest;
import pl.drytcha.venquests.config.QuestType;
import pl.drytcha.venquests.database.PlayerProgress;
import pl.drytcha.venquests.player.PlayerData;
import pl.drytcha.venquests.utils.Utils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GUI implements Listener {

    private final VenQuests plugin;

    public GUI(VenQuests plugin) {
        this.plugin = plugin;
    }

    // GŁÓWNE MENU
    public void openMainMenu(Player player) {
        String title = Utils.colorize(plugin.getConfig().getString("gui.main_menu.title"));
        int size = plugin.getConfig().getInt("gui.main_menu.size");
        Inventory inv = Bukkit.createInventory(player, size, title);
        PlayerData playerData = plugin.getPlayerManager().getPlayerData(player.getUniqueId());

        // Dodatkowe zabezpieczenie, chociaż sprawdzane jest już w komendzie
        if (playerData == null) {
            player.sendMessage(Utils.colorize("&cWystąpił błąd podczas ładowania Twoich danych. Spróbuj wejść ponownie na serwer."));
            return;
        }

        // Misje Dzienne
        List<String> dailyLore = new ArrayList<>();
        if (playerData.getDailyQuest() != null) {
            dailyLore.add(Utils.colorize("&6Masz już aktywną misję."));
        } else if (playerData.isOnCooldown(QuestType.Category.DAILY)) {
            dailyLore.add(Utils.colorize("&cNowa misja będzie dostępna za:"));
            dailyLore.add(Utils.colorize("&c" + Utils.formatTime(playerData.getRemainingCooldown(QuestType.Category.DAILY))));
        } else {
            dailyLore.add(Utils.colorize("&aMożesz odebrać nową misję!"));
        }
        inv.setItem(11, createGuiItem(Material.CLOCK, "&e&lMisje Dzienne", dailyLore));

        // Misje Tygodniowe
        List<String> weeklyLore = new ArrayList<>();
        if (playerData.getWeeklyQuest() != null) {
            weeklyLore.add(Utils.colorize("&6Masz już aktywną misję."));
        } else if (playerData.isOnCooldown(QuestType.Category.WEEKLY)) {
            weeklyLore.add(Utils.colorize("&cNowa misja będzie dostępna za:"));
            weeklyLore.add(Utils.colorize("&c" + Utils.formatTime(playerData.getRemainingCooldown(QuestType.Category.WEEKLY))));
        } else {
            weeklyLore.add(Utils.colorize("&aMożesz odebrać nową misję!"));
        }
        inv.setItem(13, createGuiItem(Material.COMPASS, "&b&lMisje Tygodniowe", weeklyLore));

        // Misje Miesięczne
        List<String> monthlyLore = new ArrayList<>();
        if (playerData.getMonthlyQuest() != null) {
            monthlyLore.add(Utils.colorize("&6Masz już aktywną misję."));
        } else if (playerData.isOnCooldown(QuestType.Category.MONTHLY)) {
            monthlyLore.add(Utils.colorize("&cNowa misja będzie dostępna za:"));
            monthlyLore.add(Utils.colorize("&c" + Utils.formatTime(playerData.getRemainingCooldown(QuestType.Category.MONTHLY))));
        } else {
            monthlyLore.add(Utils.colorize("&aMożesz odebrać nową misję!"));
        }
        inv.setItem(15, createGuiItem(Material.BEACON, "&c&lMisje Miesięczne", monthlyLore));


        player.openInventory(inv);
    }

    // DEDYKOWANE MENU
    public void openCategoryMenu(Player player, QuestType.Category category) {
        String configPath = category.name().toLowerCase();
        String title = Utils.colorize(plugin.getConfig().getString("gui." + configPath + "_quests_menu.title"));
        int size = plugin.getConfig().getInt("gui." + configPath + "_quests_menu.size");
        Inventory inv = Bukkit.createInventory(player, size, title);
        PlayerData playerData = plugin.getPlayerManager().getPlayerData(player.getUniqueId());
        if (playerData == null) return;

        List<PlayerProgress> activeQuests = playerData.getActiveQuestsForCategory(category);

        for (int i = 0; i < activeQuests.size(); i++) {
            PlayerProgress progress = activeQuests.get(i);
            Quest quest = plugin.getQuestManager().getQuestById(progress.getQuestId());
            if(quest != null) {
                List<String> lore = new ArrayList<>(Utils.colorize(quest.getLore()));
                lore.add("");
                lore.add(Utils.getMessage("quest_progress")
                        .replace("%progress%", String.valueOf(progress.getProgress()))
                        .replace("%goal%", String.valueOf(quest.getAmount())));
                lore.add("");
                lore.add(Utils.colorize("&7Nagrody:"));
                // Tutaj można by dodać wyświetlanie nagród, jeśli jest taka potrzeba
                inv.setItem(i, createGuiItem(Material.BOOK, quest.getName(), lore));
            }
        }


        // Opcja zakupu
        if (plugin.getConfig().getBoolean("buy_quest." + configPath + ".enabled")) {
            double cost = plugin.getConfig().getDouble("buy_quest." + configPath + ".cost");
            inv.setItem(size - 5, createGuiItem(Material.EMERALD, "&a&lKup dodatkową misję", "&7Koszt: &e" + cost + "$"));
        }

        player.openInventory(inv);
    }


    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        String mainMenuTitle = Utils.colorize(plugin.getConfig().getString("gui.main_menu.title"));
        String dailyMenuTitle = Utils.colorize(plugin.getConfig().getString("gui.daily_quests_menu.title"));
        String weeklyMenuTitle = Utils.colorize(plugin.getConfig().getString("gui.weekly_quests_menu.title"));
        String monthlyMenuTitle = Utils.colorize(plugin.getConfig().getString("gui.monthly_quests_menu.title"));

        String clickedInventoryTitle = event.getView().getTitle();


        if(clickedInventoryTitle.equals(mainMenuTitle)) {
            handleMainMenuClick(event, player);
        } else if (clickedInventoryTitle.equals(dailyMenuTitle) ||
                clickedInventoryTitle.equals(weeklyMenuTitle) ||
                clickedInventoryTitle.equals(monthlyMenuTitle)) {
            handleCategoryMenuClick(event, player);
        }
    }

    private void handleCategoryMenuClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType().isAir()) return;

        if (clickedItem.getType() == Material.EMERALD) {
            String title = event.getView().getTitle();
            QuestType.Category category = null;
            if (title.contains("Dzienne")) category = QuestType.Category.DAILY;
            else if (title.contains("Tygodniowe")) category = QuestType.Category.WEEKLY;
            else if (title.contains("Miesięczne")) category = QuestType.Category.MONTHLY;

            if (category != null) {
                handleBuyQuest(player, category);
            }
        }
    }

    private void handleBuyQuest(Player player, QuestType.Category category) {
        player.closeInventory();
        String configPath = "buy_quest." + category.name().toLowerCase();
        double cost = plugin.getConfig().getDouble(configPath + ".cost");

        if (!plugin.getEconomyManager().hasEnough(player, cost)) {
            player.sendMessage(Utils.getMessage("not_enough_money").replace("%cost%", String.valueOf(cost)));
            return;
        }

        if (plugin.getEconomyManager().withdraw(player, cost)) {
            Quest newQuest = plugin.getQuestManager().getRandomQuest(category);
            if(newQuest == null) {
                player.sendMessage(Utils.colorize("&cNie ma więcej dostępnych misji do kupienia w tej kategorii."));
                // Zwróć pieniądze?
                return;
            }
            plugin.getPlayerManager().getPlayerData(player.getUniqueId()).addAdditionalQuest(category, newQuest.getId());
            player.sendMessage(Utils.getMessage("buy_quest_success"));
            openCategoryMenu(player, category);
        } else {
            player.sendMessage(Utils.colorize("&cWystąpił błąd podczas pobierania opłaty."));
        }
    }

    private void handleMainMenuClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType().isAir()) return;

        PlayerData playerData = plugin.getPlayerManager().getPlayerData(player.getUniqueId());
        if (playerData == null) return;

        QuestType.Category category = null;
        if(clickedItem.getType() == Material.CLOCK) category = QuestType.Category.DAILY;
        else if(clickedItem.getType() == Material.COMPASS) category = QuestType.Category.WEEKLY;
        else if(clickedItem.getType() == Material.BEACON) category = QuestType.Category.MONTHLY;

        if(category != null) {
            player.closeInventory();
            if(playerData.getMainQuest(category) != null || !playerData.getActiveQuestsForCategory(category).isEmpty()) {
                openCategoryMenu(player, category);
            } else if (playerData.isOnCooldown(category)) {
                String time = Utils.formatTime(playerData.getRemainingCooldown(category));
                player.sendMessage(Utils.getMessage("on_cooldown").replace("%time%", time));
            } else {
                Quest newQuest = plugin.getQuestManager().getRandomQuest(category);
                if (newQuest == null) {
                    player.sendMessage(Utils.colorize("&cNie znaleziono żadnych dostępnych misji w tej kategorii."));
                    return;
                }
                playerData.setMainQuest(category, newQuest.getId());
                player.sendMessage(Utils.getMessage("new_quest_assigned").replace("%quest_name%", Utils.colorize(newQuest.getName())));
                openCategoryMenu(player, category);
            }
        }
    }

    private ItemStack createGuiItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Utils.colorize(name));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createGuiItem(Material material, String name, String... lore) {
        return createGuiItem(material, name, Arrays.asList(lore));
    }
}

