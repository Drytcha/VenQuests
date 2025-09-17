package pl.drytcha.venquests.commands;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Statistic;
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
import pl.drytcha.venquests.utils.InventoryUtils;
import pl.drytcha.venquests.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class GUI implements Listener {

    private final VenQuests plugin;

    // Stany dla menu listy misji
    private final Map<UUID, QuestType.Category> playerViewingCategory = new HashMap<>();
    private final Map<UUID, Integer> playerViewingPage = new HashMap<>();
    private final Map<UUID, SortType> playerSortType = new HashMap<>();

    private static final int QUESTS_PER_PAGE = 45;

    private enum SortType {
        NONE,
        BY_TYPE
    }


    public GUI(VenQuests plugin) {
        this.plugin = plugin;
    }

    // GŁÓWNE MENU
    public void openMainMenu(Player player) {
        PlayerData playerData = plugin.getPlayerManager().getPlayerData(player.getUniqueId());
        if (playerData == null) {
            player.sendMessage(Utils.colorize("&cWystąpił błąd podczas ładowania Twoich danych. Spróbuj wejść ponownie na serwer."));
            return;
        }

        checkAndRemoveExpiredQuests(player);

        String title = Utils.colorize(plugin.getConfig().getString("gui.main_menu.title"));
        int size = plugin.getConfig().getInt("gui.main_menu.size");
        Inventory inv = Bukkit.createInventory(player, size, title);

        updateMainMenuItems(inv, player);
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
                updateMainMenuItems(player.getOpenInventory().getTopInventory(), player);
            }
        }.runTaskTimer(plugin, 20L, 20L);

        tasks.put(player.getUniqueId(), task);
    }

    // METODA AKTUALIZUJĄCA ITEMY W GŁÓWNYM MENU
    public void updateMainMenuItems(Inventory inv, Player player) {
        PlayerData playerData = plugin.getPlayerManager().getPlayerData(player.getUniqueId());
        if (playerData == null) return;

        // Misje Dzienne
        List<String> dailyLore = new ArrayList<>();
        if (!playerData.getActiveQuestsForCategory(QuestType.Category.DAILY).isEmpty()) {
            dailyLore.add(Utils.colorize("&6Masz już aktywne misje."));
        } else if (playerData.isOnCooldown(QuestType.Category.DAILY)) {
            dailyLore.add(Utils.colorize("&cNowa darmowa misja będzie dostępna za:"));
            dailyLore.add(Utils.colorize("&c" + Utils.formatTime(playerData.getRemainingCooldown(QuestType.Category.DAILY))));
        } else {
            dailyLore.add(Utils.colorize("&aMożesz odebrać nową darmową misję!"));
        }
        dailyLore.add("");
        dailyLore.add(Utils.colorize("&7Kliknij, aby zobaczyć swoje misje"));
        dailyLore.add(Utils.colorize("&7lub odebrać nową."));
        inv.setItem(11, createGuiItem(Material.CLOCK, "&e&lMisje Dzienne", dailyLore));

        // Misje Tygodniowe
        List<String> weeklyLore = new ArrayList<>();
        if (!playerData.getActiveQuestsForCategory(QuestType.Category.WEEKLY).isEmpty()) {
            weeklyLore.add(Utils.colorize("&6Masz już aktywne misje."));
        } else if (playerData.isOnCooldown(QuestType.Category.WEEKLY)) {
            weeklyLore.add(Utils.colorize("&cNowa darmowa misja będzie dostępna za:"));
            weeklyLore.add(Utils.colorize("&c" + Utils.formatTime(playerData.getRemainingCooldown(QuestType.Category.WEEKLY))));
        } else {
            weeklyLore.add(Utils.colorize("&aMożesz odebrać nową darmową misję!"));
        }
        weeklyLore.add("");
        weeklyLore.add(Utils.colorize("&7Kliknij, aby zobaczyć swoje misje"));
        weeklyLore.add(Utils.colorize("&7lub odebrać nową."));
        inv.setItem(13, createGuiItem(Material.COMPASS, "&b&lMisje Tygodniowe", weeklyLore));

        // Misje Miesięczne
        List<String> monthlyLore = new ArrayList<>();
        if (!playerData.getActiveQuestsForCategory(QuestType.Category.MONTHLY).isEmpty()) {
            monthlyLore.add(Utils.colorize("&6Masz już aktywne misje."));
        } else if (playerData.isOnCooldown(QuestType.Category.MONTHLY)) {
            monthlyLore.add(Utils.colorize("&cNowa darmowa misja będzie dostępna za:"));
            monthlyLore.add(Utils.colorize("&c" + Utils.formatTime(playerData.getRemainingCooldown(QuestType.Category.MONTHLY))));
        } else {
            monthlyLore.add(Utils.colorize("&aMożesz odebrać nową darmową misję!"));
        }
        monthlyLore.add("");
        monthlyLore.add(Utils.colorize("&7Kliknij, aby zobaczyć swoje misje"));
        monthlyLore.add(Utils.colorize("&7lub odebrać nową."));
        inv.setItem(15, createGuiItem(Material.BEACON, "&c&lMisje Miesięczne", monthlyLore));
    }


    // DEDYKOWANE MENU Z ODŚWIEŻANIEM
    public void openCategoryMenu(Player player, QuestType.Category category) {
        PlayerData playerData = plugin.getPlayerManager().getPlayerData(player.getUniqueId());
        if (playerData == null) {
            player.sendMessage(Utils.colorize("&cWystąpił błąd podczas ładowania Twoich danych. Spróbuj wejść ponownie na serwer."));
            return;
        }
        checkAndRemoveExpiredQuests(player);

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
                checkAndRemoveExpiredQuests(player);
                updateCategoryMenuItems(player.getOpenInventory().getTopInventory(), player, category);
            }
        }.runTaskTimer(plugin, 20L, 20L);

        tasks.put(player.getUniqueId(), task);
    }

    // MENU Z LISTĄ WSZYSTKICH MISJI Z PAGINACJĄ
    public void openQuestListMenu(Player player) {
        String title = Utils.colorize(plugin.getConfig().getString("gui.quest_list_menu.title"));
        int size = plugin.getConfig().getInt("gui.quest_list_menu.size", 54);
        Inventory inv = Bukkit.createInventory(player, size, title);

        playerViewingCategory.put(player.getUniqueId(), QuestType.Category.DAILY);
        playerViewingPage.put(player.getUniqueId(), 0);
        playerSortType.put(player.getUniqueId(), SortType.NONE);

        drawQuestListPage(player, QuestType.Category.DAILY, 0, SortType.NONE, inv);

        player.openInventory(inv);
    }

    private void drawQuestListPage(Player player, QuestType.Category category, int page, SortType sortType, Inventory inv) {
        inv.clear();

        List<Quest> quests = plugin.getQuestManager().getQuestsByCategory(category, sortType == SortType.BY_TYPE);

        // Wypełnianie misjami (górne 5 rzędów)
        int startIndex = page * QUESTS_PER_PAGE;
        for (int i = 0; i < QUESTS_PER_PAGE; i++) {
            int questIndex = startIndex + i;
            if (questIndex < quests.size()) {
                Quest quest = quests.get(questIndex);
                List<String> lore = new ArrayList<>(Utils.colorize(quest.getLore()));
                lore.add("");
                lore.add(Utils.colorize("&7Typ: &f" + quest.getType().name()));
                lore.add(Utils.colorize("&8ID: " + quest.getId()));
                inv.setItem(i, createGuiItem(getIconForQuest(quest), quest.getName(), lore));
            } else {
                break; // Nie ma więcej misji do wyświetlenia na tej stronie
            }
        }

        // Rysowanie panelu nawigacyjnego (dolny rząd)
        ItemStack filler = createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, filler);
        }

        // Przycisk "Poprzednia strona"
        if (page > 0) {
            inv.setItem(45, createGuiItem(Material.ARROW, "&c&lPoprzednia strona", "&7Kliknij, aby wrócić."));
        }

        // Przycisk "Następna strona"
        int totalPages = (int) Math.ceil((double) quests.size() / QUESTS_PER_PAGE);
        if (page < totalPages - 1) {
            inv.setItem(53, createGuiItem(Material.ARROW, "&a&lNastępna strona", "&7Kliknij, aby przejść dalej."));
        }

        // Przycisk sortowania
        if(sortType == SortType.NONE) {
            inv.setItem(48, createGuiItem(Material.HOPPER, "&6&lSortowanie", "&7Aktualnie: &fDomyślne", "", "&aKliknij, aby posortować wg typu."));
        } else {
            inv.setItem(48, createGuiItem(Material.HOPPER, "&6&lSortowanie", "&7Aktualnie: &fWg typu", "", "&aKliknij, aby przywrócić domyślne."));
        }

        // Przełącznik kategorii
        ItemStack categorySwitcher;
        switch(category) {
            case DAILY:
                categorySwitcher = createGuiItem(Material.CLOCK, "&e&lMisje Dzienne", "&7Aktualnie przeglądasz misje dzienne.", "", "&aKliknij, aby przełączyć na Tygodniowe.");
                break;
            case WEEKLY:
                categorySwitcher = createGuiItem(Material.COMPASS, "&b&lMisje Tygodniowe", "&7Aktualnie przeglądasz misje tygodniowe.", "", "&aKliknij, aby przełączyć na Miesięczne.");
                break;
            case MONTHLY:
            default:
                categorySwitcher = createGuiItem(Material.BEACON, "&c&lMisje Miesięczne", "&7Aktualnie przeglądasz misje miesięczne.", "", "&aKliknij, aby przełączyć na Dzienne.");
                break;
        }
        inv.setItem(49, categorySwitcher);
    }


    // METODA AKTUALIZUJĄCA ITEMY W GUI AKTYWNYCH MISJI
    public void updateCategoryMenuItems(Inventory inv, Player player, QuestType.Category category) {
        String configPath = category.name().toLowerCase();
        PlayerData playerData = plugin.getPlayerManager().getPlayerData(player.getUniqueId());
        if (playerData == null) return;

        inv.clear(); // Czyścimy całe GUI przed ponownym wrysowaniem

        List<PlayerProgress> activeQuests = playerData.getActiveQuestsForCategory(category);

        for (int i = 0; i < activeQuests.size(); i++) {
            PlayerProgress progress = activeQuests.get(i);
            Quest quest = plugin.getQuestManager().getQuestById(progress.getQuestId());
            if (quest != null) {
                List<String> lore = new ArrayList<>(Utils.colorize(quest.getLore()));
                lore.add("");

                int currentProgress = progress.getProgress();
                if (isMovementQuest(quest.getType())) {
                    int currentStat = 0;
                    if(quest.getType() == QuestType.WALK) {
                        currentStat = getTotalWalkStatistic(player);
                    } else if(quest.getType() == QuestType.SWIM) {
                        currentStat = player.getStatistic(Statistic.SWIM_ONE_CM);
                    } else if(quest.getType() == QuestType.GLIDING) {
                        currentStat = player.getStatistic(Statistic.AVIATE_ONE_CM);
                    }
                    currentProgress = (currentStat - progress.getStartValue()) / 100;
                } else if (quest.getType() == QuestType.COLLECT) {
                    try {
                        List<String> requiredItems = quest.getItems();
                        int totalItems = 0;
                        for(String itemName : requiredItems) {
                            Material itemMaterial = Material.valueOf(itemName.toUpperCase());
                            totalItems += InventoryUtils.getItemCount(player, itemMaterial);
                        }
                        currentProgress = totalItems;
                    } catch (IllegalArgumentException e) {
                        // Błąd w nazwie materiału, postęp pozostaje 0
                    }
                }

                lore.add(Utils.getMessage("quest_progress")
                        .replace("%progress%", String.valueOf(Math.min(currentProgress, quest.getAmount())))
                        .replace("%goal%", String.valueOf(quest.getAmount())));

                long timeLimitSeconds = plugin.getConfig().getLong("time_limits." + category.name().toLowerCase());
                long timeLimitMillis = TimeUnit.SECONDS.toMillis(timeLimitSeconds);
                long remainingMillis = (progress.getStartTime() + timeLimitMillis) - System.currentTimeMillis();

                lore.add(Utils.getMessage("quest_time_left").replace("%time%", Utils.formatTime(remainingMillis)));

                inv.setItem(i, createGuiItem(getIconForQuest(quest), quest.getName(), lore));
            }
        }

        // Opcja zakupu
        if (plugin.getConfig().getBoolean("buy_quest." + configPath + ".enabled")) {
            double moneyCost = plugin.getConfig().getDouble("buy_quest." + configPath + ".cost.money");
            List<String> itemCosts = plugin.getConfig().getStringList("buy_quest." + configPath + ".cost.items");

            List<String> lore = new ArrayList<>();
            lore.add("&7Kliknij, aby zakupić nową misję.");

            if (category == QuestType.Category.DAILY) {
                int limit = plugin.getConfig().getInt("buy_quest.daily.limit");
                int bought = playerData.getDailyQuestsBoughtToday();
                lore.add("&7Dzienny limit: &e" + bought + "&7/&e" + limit);
            }

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

        String clickedInventoryTitle = event.getView().getTitle();

        String mainMenuTitle = Utils.colorize(plugin.getConfig().getString("gui.main_menu.title"));
        String dailyMenuTitle = Utils.colorize(plugin.getConfig().getString("gui.daily_quests_menu.title"));
        String weeklyMenuTitle = Utils.colorize(plugin.getConfig().getString("gui.weekly_quests_menu.title"));
        String monthlyMenuTitle = Utils.colorize(plugin.getConfig().getString("gui.monthly_quests_menu.title"));
        String questListTitle = Utils.colorize(plugin.getConfig().getString("gui.quest_list_menu.title"));

        if (clickedInventoryTitle.equals(questListTitle)) {
            handleQuestListClick(event, player);
            return;
        }

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
        UUID playerUUID = player.getUniqueId();

        // Czyszczenie stanu gracza z menu listy misji
        playerViewingCategory.remove(playerUUID);
        playerViewingPage.remove(playerUUID);
        playerSortType.remove(playerUUID);

        // Anulowanie taska odświeżającego
        Map<UUID, BukkitTask> tasks = plugin.getGuiUpdateTasks();
        if (tasks.containsKey(playerUUID)) {
            tasks.get(playerUUID).cancel();
            tasks.remove(playerUUID);
        }
    }

    private void handleQuestListClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType().isAir()) return;

        UUID playerUUID = player.getUniqueId();
        int currentPage = playerViewingPage.getOrDefault(playerUUID, 0);
        QuestType.Category currentCategory = playerViewingCategory.getOrDefault(playerUUID, QuestType.Category.DAILY);
        SortType currentSort = playerSortType.getOrDefault(playerUUID, SortType.NONE);
        int slot = event.getSlot();

        if (slot == 45 && currentPage > 0) { // Poprzednia strona
            playerViewingPage.put(playerUUID, currentPage - 1);
            drawQuestListPage(player, currentCategory, currentPage - 1, currentSort, event.getInventory());
        } else if (slot == 53) { // Następna strona
            List<Quest> quests = plugin.getQuestManager().getQuestsByCategory(currentCategory, currentSort == SortType.BY_TYPE);
            int totalPages = (int) Math.ceil((double) quests.size() / QUESTS_PER_PAGE);
            if (currentPage < totalPages - 1) {
                playerViewingPage.put(playerUUID, currentPage + 1);
                drawQuestListPage(player, currentCategory, currentPage + 1, currentSort, event.getInventory());
            }
        } else if (slot == 48) { // Zmiana sortowania
            SortType nextSort = (currentSort == SortType.NONE) ? SortType.BY_TYPE : SortType.NONE;
            playerSortType.put(playerUUID, nextSort);
            playerViewingPage.put(playerUUID, 0); // Reset do pierwszej strony
            drawQuestListPage(player, currentCategory, 0, nextSort, event.getInventory());
        } else if (slot == 49) { // Zmiana kategorii
            QuestType.Category nextCategory;
            switch(currentCategory) {
                case DAILY: nextCategory = QuestType.Category.WEEKLY; break;
                case WEEKLY: nextCategory = QuestType.Category.MONTHLY; break;
                case MONTHLY:
                default: nextCategory = QuestType.Category.DAILY; break;
            }
            playerViewingCategory.put(playerUUID, nextCategory);
            playerViewingPage.put(playerUUID, 0); // Reset do pierwszej strony
            drawQuestListPage(player, nextCategory, 0, currentSort, event.getInventory());
        }
    }


    private void handleCategoryMenuClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType().isAir()) return;

        String title = event.getView().getTitle();
        QuestType.Category category = null;
        if (title.contains("dzienne")) category = QuestType.Category.DAILY;
        else if (title.contains("tygodniowe")) category = QuestType.Category.WEEKLY;
        else if (title.contains("miesięczne")) category = QuestType.Category.MONTHLY;

        if (category == null) return;

        if (clickedItem.getType() == Material.EMERALD) {
            handleBuyQuest(player, category);
        }
    }

    private void handleBuyQuest(Player player, QuestType.Category category) {
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
                plugin.getEconomyManager().giveCost(player, category);
                return;
            }
            PlayerProgress progress = playerData.addAdditionalQuest(category, newQuest.getId());
            setInitialStatistic(player, newQuest, progress);
            playerData.incrementQuestsBought(category);

            player.sendMessage(Utils.getMessage("buy_quest_success"));
            updateCategoryMenuItems(player.getOpenInventory().getTopInventory(), player, category);
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
            if (playerData.getMainQuest(category) == null && !playerData.isOnCooldown(category)) {
                player.closeInventory();

                List<String> activeQuestIds = playerData.getActiveQuestsForCategory(category).stream()
                        .map(PlayerProgress::getQuestId)
                        .collect(Collectors.toList());

                Quest newQuest = plugin.getQuestManager().getRandomQuest(category, activeQuestIds);
                if (newQuest == null) {
                    player.sendMessage(Utils.colorize("&cNie znaleziono żadnych dostępnych misji w tej kategorii."));
                    return;
                }
                PlayerProgress progress = playerData.setMainQuest(category, newQuest.getId());
                setInitialStatistic(player, newQuest, progress);
                player.sendMessage(Utils.getMessage("new_quest_assigned").replace("%quest_name%", Utils.colorize(newQuest.getName())));
                openCategoryMenu(player, category);
            } else {
                openCategoryMenu(player, category);
            }
        }
    }

    private void checkAndRemoveExpiredQuests(Player player) {
        PlayerData playerData = plugin.getPlayerManager().getPlayerData(player.getUniqueId());
        if (playerData == null) return;

        for (QuestType.Category category : QuestType.Category.values()) {
            long timeLimitSeconds = plugin.getConfig().getLong("time_limits." + category.name().toLowerCase());
            long timeLimitMillis = TimeUnit.SECONDS.toMillis(timeLimitSeconds);

            new ArrayList<>(playerData.getActiveQuestsForCategory(category)).forEach(progress -> {
                long remainingMillis = (progress.getStartTime() + timeLimitMillis) - System.currentTimeMillis();
                if (remainingMillis <= 0) {
                    playerData.removeQuest(category, progress);
                }
            });
        }
    }


    private void setInitialStatistic(Player player, Quest quest, PlayerProgress progress) {
        if (isMovementQuest(quest.getType())) {
            int initialStat = 0;
            if(quest.getType() == QuestType.WALK) {
                initialStat = getTotalWalkStatistic(player);
            } else if (quest.getType() == QuestType.SWIM) {
                initialStat = player.getStatistic(Statistic.SWIM_ONE_CM);
            } else if (quest.getType() == QuestType.GLIDING) {
                initialStat = player.getStatistic(Statistic.AVIATE_ONE_CM);
            }
            progress.setStartValue(initialStat);
        }
    }

    private boolean isMovementQuest(QuestType type) {
        return type == QuestType.WALK || type == QuestType.SWIM || type == QuestType.GLIDING;
    }

    private int getTotalWalkStatistic(Player player) {
        return player.getStatistic(Statistic.WALK_ONE_CM) +
                player.getStatistic(Statistic.CROUCH_ONE_CM) +
                player.getStatistic(Statistic.SPRINT_ONE_CM);
    }

    private Material getIconForQuest(Quest quest) {
        String materialName = plugin.getConfig().getString("quest_type_icons." + quest.getType().name());
        if (materialName == null) {
            materialName = plugin.getConfig().getString("quest_type_icons.DEFAULT", "BOOK");
        }
        try {
            return Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Nieprawidłowa nazwa materiału w config.yml dla typu misji: " + quest.getType().name() + ". Użyto domyślnej ikony.");
            return Material.valueOf(plugin.getConfig().getString("quest_type_icons.DEFAULT", "BOOK"));
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
