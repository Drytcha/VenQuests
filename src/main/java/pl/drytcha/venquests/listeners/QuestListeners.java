package pl.drytcha.venquests.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import pl.drytcha.venquests.VenQuests;
import pl.drytcha.venquests.config.Quest;
import pl.drytcha.venquests.config.QuestType;
import pl.drytcha.venquests.database.PlayerProgress;
import pl.drytcha.venquests.player.PlayerData;
import pl.drytcha.venquests.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class QuestListeners implements Listener {

    private final VenQuests plugin;

    public QuestListeners(VenQuests plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMobKill(EntityDeathEvent event) {
        if (event.getEntity().getKiller() == null) return;
        Player player = event.getEntity().getKiller();
        String mobType = event.getEntityType().name();

        checkAndUpdateProgress(player, QuestType.MOBS, mobType, 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        String blockType = event.getBlock().getType().name();

        checkAndUpdateProgress(player, QuestType.DESTROY, blockType, 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        String itemType = event.getRecipe().getResult().getType().name();
        int craftAmount = event.getRecipe().getResult().getAmount(); // Uwzględnia ilość wytworzonych przedmiotów

        checkAndUpdateProgress(player, QuestType.CRAFTING, itemType, craftAmount);
    }

    private void checkAndUpdateProgress(Player player, QuestType actionType, String target, int amount) {
        PlayerData playerData = plugin.getPlayerManager().getPlayerData(player.getUniqueId());
        if (playerData == null) return;

        List<PlayerProgress> allQuests = new ArrayList<>();
        if (playerData.getDailyQuest() != null) allQuests.add(playerData.getDailyQuest());
        if (playerData.getWeeklyQuest() != null) allQuests.add(playerData.getWeeklyQuest());
        if (playerData.getMonthlyQuest() != null) allQuests.add(playerData.getMonthlyQuest());
        allQuests.addAll(playerData.getAdditionalDailyQuests());
        allQuests.addAll(playerData.getAdditionalWeeklyQuests());
        allQuests.addAll(playerData.getAdditionalMonthlyQuests());

        for (PlayerProgress progress : allQuests) {
            Quest quest = plugin.getQuestManager().getQuestById(progress.getQuestId());
            if (quest == null || quest.getType() != actionType) continue;

            boolean match = false;
            switch (actionType) {
                case MOBS:
                    if (quest.getEntities().contains(target.toUpperCase())) match = true;
                    break;
                case DESTROY:
                    if (quest.getBlocks().contains(target.toUpperCase())) match = true;
                    break;
                case CRAFTING:
                    if (quest.getItem().equalsIgnoreCase(target)) match = true;
                    break;
            }

            if (match) {
                progress.incrementProgress(amount);
                Quest questFromManager = plugin.getQuestManager().getQuestById(progress.getQuestId());
                if (progress.getProgress() >= questFromManager.getAmount()) {
                    completeQuest(player, playerData, progress, questFromManager);
                }
            }
        }
    }

    private void completeQuest(Player player, PlayerData playerData, PlayerProgress progress, Quest quest) {
        player.sendMessage(Utils.getMessage("quest_completed").replace("%quest_name%", Utils.colorize(quest.getName())));

        // Dajemy nagrody
        for (String command : quest.getRewards()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", player.getName()));
        }

        // Usuwamy misję i ustawiamy cooldown
        QuestType.Category category = plugin.getQuestManager().getQuestCategory(quest.getId());
        if (category != null) {
            playerData.removeQuest(category, progress); // Usuwa misję z listy gracza
            if (playerData.isMainQuest(category, progress)) {
                playerData.setCooldown(category); // Ustawia cooldown tylko jeśli to była główna misja
            }
        }
    }
}

