package pl.drytcha.venquests.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import pl.drytcha.venquests.VenQuests;
import pl.drytcha.venquests.config.Quest;
import pl.drytcha.venquests.config.QuestType;
import pl.drytcha.venquests.database.PlayerProgress;
import pl.drytcha.venquests.player.PlayerData;
import pl.drytcha.venquests.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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

        if (playerData == null) {
            player.sendMessage(Utils.colorize("&cWystąpił błąd podczas ładowania Twoich danych. Spróbuj wejść ponownie na serwer."));
            return;
        }

        // Misje Dzienne
        List<String> dailyLore = new ArrayList<>();
        if (!playerData.getActiveQuestsForCategory(QuestType.Category.DAILY).isEmpty()) {
            dailyLore.add(Utils.colorize("&6Masz już aktywne misje."));
        } else if (playerData.isOnCooldown(QuestType.Category.DAILY)) {
            dailyLore.add(Utils.colorize("&cNowa misja będzie dostępna za:"));
            dailyLore.add(Utils.colorize("&c" + Utils.formatTime(playerData.getRemainingCooldown(QuestType.Category.DAILY))));
        } else {
            dailyLore.add(Utils.colorize("&aMożesz odebrać nową misję!"));
        }
        inv.setItem(11, createGuiItem(Material.CLOCK, "&e&lMisje Dzienne", dailyLore));

        // Misje Tygodniowe
        List<String> weeklyLore = new ArrayList<>();
        if (!playerData.getActiveQuestsForCategory(QuestType.Category.WEEKLY).isEmpty()) {
            weeklyLore.add(Utils.colorize("&6Masz już aktywne misje."));
        } else if (playerData.isOnCooldown(QuestType.Category.WEEKLY)) {
            weeklyLore.add(Utils.colorize("&cNowa misja będzie dostępna za:"));
            weeklyLore.add(Utils.colorize("&c" + Utils.formatTime(playerData.getRemainingCooldown(QuestType.Category.WEEKLY))));
        } else {
            weeklyLore.add(Utils.colorize("&aMożesz odebrać nową misję!"));
        }
        inv.setItem(13, createGuiItem(Material.COMPASS, "&b&lMisje Tygodniowe", weeklyLore));

        // Misje Miesięczne
        List<String> monthlyLore = new ArrayList<>();
        if (!playerData.getActiveQuestsForCategory(QuestType.Category.MONTHLY).isEmpty()) {
            monthlyLore.add(Utils.colorize("&6Masz już aktywne misje."));
        } else if (playerData.isOnCooldown(QuestType.Category.MONTHLY)) {
            monthlyLore.add(Utils.colorize("&cNowa misja będzie dostępna za:"));
            monthlyLore.add(Utils.colorize("&c" + Utils.formatTime(playerData.getRemainingCooldown(QuestType.Category.MONTHLY))));
        } else {
            monthlyLore.add(Utils.colorize("&aMożesz odebrać nową misję!"));
        }
        inv.setItem(15, createGuiItem(Material.BEACON, "&c&lMisje Miesięczne", monthlyLore));

        player.openInventory(inv);
    }

    // DEDYKOWANE MENU Z ODŚWIEŻANIEM
    public void openCategoryMenu(Player player, QuestType.Category category) {
        String configPath = category.name().toLowerCase();
        String title = Utils.colorize(plugin.getConfig().getString("gui." + configPath + "_quests_menu.title"));
        int size = plugin.getConfig().getInt("gui." + configPath + "_quests_menu.size");
        Inventory inv = Bukkit.createInventory(player, size, title);

        updateCategoryMenuItems(inv, player, category);
        player.openInventory(inv);

        Map<UUID, BukkitTask> tasks = plugin.getGuiUpdateTasks();
        if (tasks.containsKey(player.getUniqueId())) {
            tasks.get(player.getUniqueId()).cancel();
        }

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !player.getOpenInventory().getTitle().equals(title)) {
                    this.cancel();
                    tasks.remove(player.getUniqueId());
                    return;
                }
                updateCategoryMenuItems(player.getOpenInventory().getTopInventory(), player, category);
            }
        }.runTaskTimer(plugin, 20L, 20L);

        tasks.put(player.getUniqueId(), task);
    }

    // METODA AKTUALIZUJĄCA ITEMY W GUI
    public void updateCategoryMenuItems(Inventory inv, Player player, QuestType.Category category) {
        String configPath = category.name().toLowerCase();
        PlayerData playerData = plugin.getPlayerManager().getPlayerData(player.getUniqueId());
        if (playerData == null) return;

        // Czyszczenie starych itemów misji
        for (int i = 0; i < inv.getSize() - 9; i++) {
            inv.setItem(i, null);
        }

        List<PlayerProgress> activeQuests = playerData.getActiveQuestsForCategory(category);

        for (int i = 0; i < activeQuests.size(); i++) {
            PlayerProgress progress = activeQuests.get(i);
            Quest quest = plugin.getQuestManager().getQuestById(progress.getQuestId());
            if (quest != null) {
                List<String> lore = new ArrayList<>(Utils.colorize(quest.getLore()));
                lore.add("");
                lore.add(Utils.getMessage("quest_progress")
                        .replace("%progress%", String.valueOf(progress.getProgress()))
                        .replace("%goal%", String.valueOf(quest.getAmount())));

                long timeLimitSeconds = plugin.getConfig().getLong("time_limits." + category.name().toLowerCase());
                long timeLimitMillis = TimeUnit.SECONDS.toMillis(timeLimitSeconds);
                long remainingMillis = (progress.getStartTime() + timeLimitMillis) - System.currentTimeMillis();

                if (remainingMillis > 0) {
                    lore.add(Utils.getMessage("quest_time_left").replace("%time%", Utils.formatTime(remainingMillis)));
                } else {
                    lore.add(Utils.getMessage("quest_time_left").replace("%time%", "&cZakończono"));
                }

                inv.setItem(i, createGuiItem(Material.BOOK, quest.getName(), lore));
            }
        }

        // Opcja zakupu
        if (plugin.getConfig().getBoolean("buy_quest." + configPath + ".enabled")) {
            double moneyCost = plugin.getConfig().getDouble("buy_quest." + configPath + ".cost.money");
            List<String> itemCosts = plugin.getConfig().getStringList("buy_quest." + configPath + ".cost.items");

            List<String> lore = new ArrayList<>();
            lore.add("&7Kliknij, aby zakupić nową misję.");
            lore.add("");
            lore.add("&7Koszt:");
            if (moneyCost > 0) lore.add("&8- &e" + moneyCost + "$");
            for (String item : itemCosts) {
                String[] parts = item.split(":");
                lore.add("&8- &f" + parts[1] + "x " + parts[0].replace("_", " ").toLowerCase());
            }

            inv.setItem(inv.getSize() - 5, createGuiItem(Material.EMERALD, "&a&lKup dodatkową misję", lore));
        }
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

        if (clickedInventoryTitle.equals(mainMenuTitle)) {
            handleMainMenuClick(event, player);
        } else if (clickedInventoryTitle.equals(dailyMenuTitle) ||
                clickedInventoryTitle.equals(weeklyMenuTitle) ||
                clickedInventoryTitle.equals(monthlyMenuTitle)) {
            handleCategoryMenuClick(event, player);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        Map<UUID, BukkitTask> tasks = plugin.getGuiUpdateTasks();

        if (tasks.containsKey(player.getUniqueId())) {
            String closedTitle = event.getView().getTitle();
            String dailyMenuTitle = Utils.colorize(plugin.getConfig().getString("gui.daily_quests_menu.title"));
            String weeklyMenuTitle = Utils.colorize(plugin.getConfig().getString("gui.weekly_quests_menu.title"));
            String monthlyMenuTitle = Utils.colorize(plugin.getConfig().getString("gui.monthly_quests_menu.title"));

            if (closedTitle.equals(dailyMenuTitle) || closedTitle.equals(weeklyMenuTitle) || closedTitle.equals(monthlyMenuTitle)) {
                tasks.get(player.getUniqueId()).cancel();
                tasks.remove(player.getUniqueId());
            }
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
        PlayerData playerData = plugin.getPlayerManager().getPlayerData(player.getUniqueId());

        if (!playerData.canBuyQuest(category)) {
            player.sendMessage(Utils.getMessage("buy_limit_reached"));
            return;
        }

        if (plugin.getEconomyManager().takeCost(player, category)) {
            List<String> activeQuestIds = playerData.getActiveQuestsForCategory(category).stream()
                    .map(PlayerProgress::getQuestId)
                    .collect(Collectors.toList());

            Quest newQuest = plugin.getQuestManager().getRandomQuest(category, activeQuestIds);

            if (newQuest == null) {
                player.sendMessage(Utils.colorize("&cNie ma więcej dostępnych unikalnych misji do kupienia w tej kategorii."));
                // Zwróć koszt graczowi, jeśli to możliwe
                plugin.getEconomyManager().giveCost(player, category);
                return;
            }
            playerData.addAdditionalQuest(category, newQuest.getId());
            playerData.incrementQuestsBought(category);

            player.sendMessage(Utils.getMessage("buy_quest_success"));
            openCategoryMenu(player, category);
        }
    }

    private void handleMainMenuClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType().isAir()) return;

        PlayerData playerData = plugin.getPlayerManager().getPlayerData(player.getUniqueId());
        if (playerData == null) return;

        QuestType.Category category = null;
        if (clickedItem.getType() == Material.CLOCK) category = QuestType.Category.DAILY;
        else if (clickedItem.getType() == Material.COMPASS) category = QuestType.Category.WEEKLY;
        else if (clickedItem.getType() == Material.BEACON) category = QuestType.Category.MONTHLY;

        if (category != null) {
            player.closeInventory();
            if (!playerData.getActiveQuestsForCategory(category).isEmpty()) {
                openCategoryMenu(player, category);
            } else if (playerData.isOnCooldown(category)) {
                String time = Utils.formatTime(playerData.getRemainingCooldown(category));
                player.sendMessage(Utils.getMessage("on_cooldown").replace("%time%", time));
            } else {
                List<String> activeQuestIds = playerData.getActiveQuestsForCategory(category).stream()
                        .map(PlayerProgress::getQuestId)
                        .collect(Collectors.toList());

                Quest newQuest = plugin.getQuestManager().getRandomQuest(category, activeQuestIds);
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
            List<String> coloredLore = new ArrayList<>();
            for (String line : lore) {
                coloredLore.add(Utils.colorize(line));
            }
            meta.setLore(coloredLore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createGuiItem(Material material, String name, String... lore) {
        return createGuiItem(material, name, Arrays.asList(lore));
    }
}

