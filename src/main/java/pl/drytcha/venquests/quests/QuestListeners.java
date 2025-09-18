package pl.drytcha.venquests.quests;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import pl.drytcha.venquests.VenQuests;
import pl.drytcha.venquests.database.PlayerProgress;
import pl.drytcha.venquests.player.PlayerData;
import pl.drytcha.venquests.utils.InventoryUtils;
import pl.drytcha.venquests.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class QuestListeners implements Listener {

    private final VenQuests plugin;

    public QuestListeners(VenQuests plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMobKill(EntityDeathEvent event) {
        if (event.getEntity().getKiller() == null) return;
        Player player = event.getEntity().getKiller();
        String mobType = event.getEntityType().name().toUpperCase();
        checkAndUpdateProgress(player, QuestType.MOBS, mobType, 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        String blockType = event.getBlock().getType().name().toUpperCase();
        checkAndUpdateProgress(player, QuestType.DESTROY, blockType, 1);
        if (isFarmable(event.getBlock().getType())) {
            checkAndUpdateProgress(player, QuestType.FARMING, blockType, 1);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        String itemType = event.getRecipe().getResult().getType().name().toUpperCase();
        checkAndUpdateProgress(player, QuestType.CRAFTING, itemType, event.getRecipe().getResult().getAmount());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerFish(PlayerFishEvent event) {
        // Sprawdzamy tylko zakończony połów, w którym coś zostało złowione
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH || !(event.getCaught() instanceof org.bukkit.entity.Item)) {
            return;
        }

        Player player = event.getPlayer();
        // Złowiona rzecz jest zawsze bytem typu Item, w którym jest ItemStack
        ItemStack caughtItemStack = ((org.bukkit.entity.Item) event.getCaught()).getItemStack();
        // Pobieramy nazwę materiału (np. "COD", "SALMON", "SADDLE")
        String caughtItemName = caughtItemStack.getType().name();
        int amount = caughtItemStack.getAmount();

        // Nazwa z .name() jest już wielkimi literami, więc przekazujemy ją bezpośrednio
        checkAndUpdateProgress(player, QuestType.FISHING, caughtItemName, amount);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        String blockType = event.getBlockPlaced().getType().name().toUpperCase();
        checkAndUpdateProgress(player, QuestType.PLACE, blockType, 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEnchant(EnchantItemEvent event) {
        Player player = event.getEnchanter();
        ItemStack item = event.getItem();
        Map<Enchantment, Integer> enchants = event.getEnchantsToAdd();
        PlayerData playerData = plugin.getPlayerManager().getPlayerData(player.getUniqueId());
        if (playerData == null) return;

        for (PlayerProgress progress : getAllActiveQuests(playerData)) {
            Quest quest = plugin.getQuestManager().getQuestById(progress.getQuestId());
            if (quest == null) continue;

            boolean match = false;
            switch (quest.getType()) {
                case ENCHANT:
                    match = true;
                    break;
                case ENCHANT_TYPE:
                    for (Enchantment enchantment : enchants.keySet()) {
                        if (enchantment.getKey().getKey().equalsIgnoreCase(quest.getEnchant())) {
                            match = true;
                            break;
                        }
                    }
                    break;
                case ENCHANT_ITEM:
                    if (quest.getItems().stream().anyMatch(i -> i.equalsIgnoreCase(item.getType().name()))) {
                        for (Enchantment enchantment : enchants.keySet()) {
                            if (enchantment.getKey().getKey().equalsIgnoreCase(quest.getEnchant())) {
                                match = true;
                                break;
                            }
                        }
                    }
                    break;
            }

            if (match) {
                progress.incrementProgress(1);
                if (progress.getProgress() >= quest.getAmount()) {
                    completeQuest(player, playerData, progress, quest);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEat(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        String itemType = event.getItem().getType().name().toUpperCase();
        checkAndUpdateProgress(player, QuestType.EATING, itemType, 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() && event.getFrom().getBlockY() == event.getTo().getBlockY() && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) return;
        Player player = event.getPlayer();
        PlayerData playerData = plugin.getPlayerManager().getPlayerData(player.getUniqueId());
        if (playerData == null) return;

        for (PlayerProgress progress : getAllActiveQuests(playerData)) {
            Quest quest = plugin.getQuestManager().getQuestById(progress.getQuestId());
            if (quest == null) continue;

            handleMovementQuests(player, progress, quest);
            handleCollectQuests(player, progress, quest);
        }
    }

    private void handleMovementQuests(Player player, PlayerProgress progress, Quest quest) {
        int currentStat = 0;
        switch (quest.getType()) {
            case WALK:
                currentStat = getTotalWalkStatistic(player);
                break;
            case SWIM:
                currentStat = player.getStatistic(Statistic.SWIM_ONE_CM);
                break;
            case GLIDING:
                currentStat = player.getStatistic(Statistic.AVIATE_ONE_CM);
                break;
            default:
                return;
        }

        if (currentStat > 0) {
            int progressMade = (currentStat - progress.getStartValue()) / 100; // cm to m
            if (progressMade >= quest.getAmount()) {
                progress.setProgress(quest.getAmount());
                completeQuest(player, plugin.getPlayerManager().getPlayerData(player.getUniqueId()), progress, quest);
            }
        }
    }

    private void handleCollectQuests(Player player, PlayerProgress progress, Quest quest) {
        if (quest.getType() == QuestType.COLLECT) {
            try {
                List<Material> requiredMaterials = quest.getItems().stream()
                        .map(String::toUpperCase)
                        .map(Material::valueOf)
                        .collect(Collectors.toList());

                int currentAmount = 0;
                for(Material mat : requiredMaterials) {
                    currentAmount += InventoryUtils.getItemCount(player, mat);
                }

                if (currentAmount >= quest.getAmount()) {
                    progress.setProgress(quest.getAmount());
                    completeQuest(player, plugin.getPlayerManager().getPlayerData(player.getUniqueId()), progress, quest);
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid material name in COLLECT quest " + quest.getId() + ": " + e.getMessage());
            }
        }
    }

    private void checkAndUpdateProgress(Player player, QuestType type, String target, int amount) {
        PlayerData playerData = plugin.getPlayerManager().getPlayerData(player.getUniqueId());
        if (playerData == null) return;

        for (PlayerProgress progress : getAllActiveQuests(playerData)) {
            Quest quest = plugin.getQuestManager().getQuestById(progress.getQuestId());
            if (quest == null || quest.getType() != type) continue;

            boolean match = false;
            List<String> upperCaseTargets;
            switch (type) {
                case MOBS:
                    upperCaseTargets = quest.getMobs().stream().map(String::toUpperCase).collect(Collectors.toList());
                    if (upperCaseTargets.contains(target)) match = true;
                    break;
                case DESTROY:
                case FARMING:
                    upperCaseTargets = quest.getBlocks().stream().map(String::toUpperCase).collect(Collectors.toList());
                    if (upperCaseTargets.contains(target)) match = true;
                    break;
                case CRAFTING:
                case PLACE:
                    upperCaseTargets = quest.getItems().stream().map(String::toUpperCase).collect(Collectors.toList());
                    if (upperCaseTargets.contains(target)) match = true;
                    break;
                case EATING:
                    upperCaseTargets = quest.getEatenItems().stream().map(String::toUpperCase).collect(Collectors.toList());
                    if (upperCaseTargets.contains(target)) match = true;
                    break;
                case FISHING:
                    upperCaseTargets = quest.getFishedItems().stream().map(String::toUpperCase).collect(Collectors.toList());
                    if (upperCaseTargets.contains(target)) match = true;
                    break;
            }

            if (match) {
                progress.incrementProgress(amount);
                if (progress.getProgress() >= quest.getAmount()) {
                    completeQuest(player, playerData, progress, quest);
                }
            }
        }
    }

    private void completeQuest(Player player, PlayerData playerData, PlayerProgress progress, Quest quest) {
        // Sprawdza, czy misja ma wystarczający postęp, aby ją ukończyć.
        if (progress.getProgress() < quest.getAmount()) {
            return;
        }

        // Atomowo sprawdza i oznacza misję jako "w trakcie ukończenia", aby zapobiec wielokrotnym wywołaniom.
        // Jeśli metoda zwróci false, oznacza to, że ta misja jest już przetwarzana.
        if (!playerData.startCompletingQuest(progress.getQuestId())) {
            return;
        }

        try {
            player.sendMessage(Utils.getMessage("quest_completed").replace("%quest_name%", Utils.colorize(quest.getName())));
            for (String command : quest.getRewards()) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", player.getName()));
            }
            QuestType.Category category = quest.getCategory();
            if (category != null) {
                if (playerData.isMainQuest(category, progress)) {
                    playerData.setCooldown(category);
                }
                // Usunięcie misji z listy aktywnych pozwala na jej ponowne wylosowanie w przyszłości.
                playerData.removeQuest(category, progress);
            }
        } finally {
            // Zapewnia, że "blokada" na misji zostanie zawsze usunięta, nawet jeśli wystąpi błąd.
            playerData.finishCompletingQuest(progress.getQuestId());
        }
    }

    private List<PlayerProgress> getAllActiveQuests(PlayerData playerData) {
        List<PlayerProgress> quests = new ArrayList<>();
        if (playerData.getDailyQuest() != null) quests.add(playerData.getDailyQuest());
        if (playerData.getWeeklyQuest() != null) quests.add(playerData.getWeeklyQuest());
        if (playerData.getMonthlyQuest() != null) quests.add(playerData.getMonthlyQuest());
        quests.addAll(playerData.getAdditionalDailyQuests());
        quests.addAll(playerData.getAdditionalWeeklyQuests());
        quests.addAll(playerData.getAdditionalMonthlyQuests());
        return quests.stream().filter(java.util.Objects::nonNull).collect(Collectors.toList());
    }

    private boolean isFarmable(Material material) {
        switch (material) {
            case WHEAT: case CARROTS: case POTATOES: case BEETROOTS:
            case NETHER_WART: case PUMPKIN: case MELON: case COCOA:
            case SUGAR_CANE: case CACTUS: case BAMBOO:
                return true;
            default:
                return false;
        }
    }

    private int getTotalWalkStatistic(Player player) {
        return player.getStatistic(Statistic.WALK_ONE_CM) +
                player.getStatistic(Statistic.CROUCH_ONE_CM) +
                player.getStatistic(Statistic.SPRINT_ONE_CM);
    }
}

