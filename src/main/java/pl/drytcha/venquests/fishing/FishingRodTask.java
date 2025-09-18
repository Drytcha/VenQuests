package pl.drytcha.venquests.fishing;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import pl.drytcha.venquests.VenQuests;
import pl.drytcha.venquests.utils.Utils;

import java.util.List;
import java.util.Random;

public class FishingRodTask extends BukkitRunnable {

    private final Player player;
    private final List<FishHook> hooks;
    private final RodManager rodManager;
    private final Location startLocation;
    private int timeLeft;
    private final Random random = new Random();

    public FishingRodTask(Player player, List<FishHook> hooks, RodManager rodManager) {
        this.player = player;
        this.hooks = hooks;
        this.rodManager = rodManager;
        this.startLocation = player.getLocation();
        this.timeLeft = rodManager.getDuration();
    }

    @Override
    public void run() {
        if (!player.isOnline() || !rodManager.isSpecialRod(player.getInventory().getItemInMainHand())) {
            cancelTask(false);
            return;
        }

        if (player.getLocation().distance(startLocation) > rodManager.getMaxDistance()) {
            player.sendMessage(Utils.getMessage("fishing_cancelled_moved_too_far"));
            cancelTask(false);
            return;
        }

        timeLeft--;
        if (timeLeft <= 0) {
            cancelTask(true);
        }
    }

    private void giveRewards() {
        for (int i = 0; i < 3; i++) {
            Reward reward = getRandomReward();
            if (reward != null) {
                executeReward(reward);
            }
        }
    }

    private Reward getRandomReward() {
        int totalWeight = rodManager.getTotalWeight();
        if (totalWeight <= 0) {
            return null;
        }

        int randomWeight = random.nextInt(totalWeight);
        int currentWeight = 0;

        for (Reward reward : rodManager.getRewardsPool()) {
            currentWeight += reward.getWeight();
            if (randomWeight < currentWeight) {
                return reward;
            }
        }
        return null; // Should not happen if weights are correct
    }


    private void executeReward(Reward reward) {
        if (reward.getType() == Reward.RewardType.ITEM) {
            if (reward.getContent() != null && !reward.getContent().isEmpty()) {
                try {
                    String[] parts = reward.getContent().split(":");
                    Material material = Material.valueOf(parts[0].toUpperCase());
                    int amount = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
                    player.getInventory().addItem(new ItemStack(material, amount));
                } catch (Exception e) {
                    Bukkit.getLogger().warning("[VenQuests] Nieprawidłowy format itemu w wedka.yml: " + reward.getContent());
                }
            }
        } else if (reward.getType() == Reward.RewardType.COMMAND) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), reward.getContent().replace("%player%", player.getName()));
        }

        if (reward.getMessage() != null && !reward.getMessage().isEmpty()) {
            player.sendMessage(Utils.colorize(reward.getMessage()));
        }
    }


    public void cancelTask(boolean giveRewards) {
        if (giveRewards) {
            giveRewards();
        }
        cleanupHooks();
        if (!isCancelled()) {
            cancel();
        }
        VenQuests.getInstance().getActiveFishingTasks().remove(player.getUniqueId());
    }

    private void cleanupHooks() {
        for (FishHook hook : hooks) {
            if (hook != null && !hook.isDead()) {
                hook.remove();
            }
        }
        hooks.clear();
    }
}

