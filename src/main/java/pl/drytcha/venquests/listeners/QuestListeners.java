package pl.drytcha.venquests.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
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

        checkAndUpdateProgress(player, QuestType.CRAFTING, itemType, event.getRecipe().getResult().getAmount());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerFish(PlayerFishEvent event) {
        Player player = event.getPlayer();
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            ItemStack caught = event.getCaught() instanceof org.bukkit.entity.Item ? ((org.bukkit.entity.Item) event.getCaught()).getItemStack() : null;
            if (caught != null) {
                checkAndUpdateProgress(player, QuestType.FISHING, caught.getType().name(), caught.getAmount());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        String blockType = event.getBlockPlaced().getType().name();

        checkAndUpdateProgress(player, QuestType.PLACE, blockType, 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEnchant(EnchantItemEvent event) {
        Player player = event.getEnchanter();

        checkAndUpdateProgress(player, QuestType.ENCHANT, "any", 1);
        event.getEnchantsToAdd().forEach((enchantment, level) ->
                checkAndUpdateProgress(player, QuestType.ENCHANT_ITEM, enchantment.getKey().getKey().toUpperCase(), 1));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEat(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        String itemType = event.getItem().getType().name();
        checkAndUpdateProgress(player, QuestType.EATING, itemType, 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHarvest(BlockBreakEvent event) {
        Player player = event.getPlayer();
        String blockType = event.getBlock().getType().name();

        if (isFarmable(event.getBlock().getType())) {
            checkAndUpdateProgress(player, QuestType.FARMING, blockType, 1);
        }
    }

    @EventHandler
    public void onCollectItem(org.bukkit.event.player.PlayerPickupItemEvent event) {
        Player player = event.getPlayer();
        String itemType = event.getItem().getItemStack().getType().name();
        int amount = event.getItem().getItemStack().getAmount();

        checkAndUpdateProgress(player, QuestType.COLLECT, itemType, amount);
    }

    private void checkAndUpdateProgress(Player player, QuestType type, String target, int amount) {
        PlayerData playerData = plugin.getPlayerManager().getPlayerData(player.getUniqueId());
        if (playerData == null) {
            plugin.getLogger().warning("Błąd: Nie znaleziono danych gracza " + player.getName() + " podczas aktualizacji postępu misji.");
            return;
        }

        List<PlayerProgress> questsToUpdate = new ArrayList<>();
        questsToUpdate.add(playerData.getDailyQuest());
        questsToUpdate.add(playerData.getWeeklyQuest());
        questsToUpdate.add(playerData.getMonthlyQuest());
        questsToUpdate.addAll(playerData.getAdditionalDailyQuests());
        questsToUpdate.addAll(playerData.getAdditionalWeeklyQuests());
        questsToUpdate.addAll(playerData.getAdditionalMonthlyQuests());

        for (PlayerProgress progress : questsToUpdate) {
            if (progress == null) continue;

            Quest quest = plugin.getQuestManager().getQuestById(progress.getQuestId());
            if (quest == null) continue;

            boolean match = false;
            if (quest.getType() == type) {
                switch (type) {
                    case MOBS:
                        if (quest.getMobs().contains(target.toUpperCase())) match = true;
                        break;
                    case DESTROY:
                    case PLACE:
                        if (quest.getBlocks().contains(target.toUpperCase())) match = true;
                        break;
                    case COLLECT:
                    case CRAFTING:
                    case EATING:
                        if (quest.getItem().equalsIgnoreCase(target)) match = true;
                        break;
                    case ENCHANT:
                        if (target.equalsIgnoreCase("any")) match = true;
                        break;
                    case ENCHANT_ITEM:
                        if (quest.getEnchant().equalsIgnoreCase(target)) match = true;
                        break;
                    case FARMING:
                        if (quest.getFarmedItems().contains(target.toUpperCase())) match = true;
                        break;
                    case FISHING:
                        if (quest.getFishedItem().equalsIgnoreCase(target)) match = true;
                        break;
                }
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

        for (String command : quest.getRewards()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", player.getName()));
        }

        QuestType.Category category = quest.getCategory();
        if (category != null) {
            playerData.removeQuest(category, progress);
            if (playerData.isMainQuest(category, progress)) {
                playerData.setCooldown(category);
            }
        }
    }

    private boolean isFarmable(Material material) {
        switch (material) {
            case WHEAT:
            case CARROTS:
            case POTATOES:
            case BEETROOTS:
            case PUMPKIN:
            case MELON:
            case COCOA_BEANS:
            case SUGAR_CANE:
            case BAMBOO:
            case CACTUS:
                return true;
            default:
                return false;
        }
    }
}
