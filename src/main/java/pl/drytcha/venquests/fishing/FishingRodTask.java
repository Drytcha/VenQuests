package pl.drytcha.venquests.fishing;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import pl.drytcha.venquests.VenQuests;
import pl.drytcha.venquests.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
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
            cancelTask(false);
            return;
        }

        hooks.removeIf(hook -> hook == null || hook.isDead());

        if (hooks.isEmpty()) {
            cancelTask(false);
            return;
        }

        boolean anyHookInWater = hooks.stream().anyMatch(FishHook::isInWater);

        if (anyHookInWater) {
            timeLeft--;
            if (timeLeft <= 0) {
                cancelTask(true);
            }
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
        return null;
    }


    private void executeReward(Reward reward) {
        switch (reward.getType()) {
            case ITEM:
            case ITEM_DURABILITY:
                handleItemReward(reward);
                break;
            case COMMAND:
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), reward.getContent().replace("%player%", player.getName()));
                break;
        }

        if (reward.getMessage() != null && !reward.getMessage().isEmpty()) {
            player.sendMessage(Utils.colorize(reward.getMessage()));
        }
    }

    private void handleItemReward(Reward reward) {
        if (reward.getContent() == null || reward.getContent().isEmpty()) {
            return; // Pusty drop
        }
        try {
            Material material = Material.valueOf(reward.getContent().toUpperCase());
            int amount = parseRandomValue(reward.getAmount());
            ItemStack item = new ItemStack(material, amount);

            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                // Obsługa wytrzymałości
                if (reward.getType() == Reward.RewardType.ITEM_DURABILITY && meta instanceof Damageable) {
                    Damageable damageableMeta = (Damageable) meta;
                    int maxDurability = item.getType().getMaxDurability();
                    int durability = parseRandomValue(reward.getDurability());

                    if (durability > 0 && durability < maxDurability) {
                        damageableMeta.setDamage(maxDurability - durability);
                    }
                }

                // NOWA LOGIKA ENCHANTÓW
                applyRandomEnchantments(meta, reward.getEnchantments());

                item.setItemMeta(meta);
            }

            player.getInventory().addItem(item);
            VenQuests.getInstance().getQuestListeners().checkFishingProgress(player, item);

        } catch (Exception e) {
            Bukkit.getLogger().warning("[VenQuests] Błąd podczas tworzenia nagrody z wędki: " + e.getMessage());
        }
    }
    private void applyRandomEnchantments(ItemMeta meta, List<String> enchantStrings) {
        if (enchantStrings == null || enchantStrings.isEmpty()) {
            return;
        }

        List<EnchantmentData> possibleEnchantments = new ArrayList<>();
        for (String enchString : enchantStrings) {
            try {
                String[] parts = enchString.split(":");
                Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(parts[0].toLowerCase()));
                if (enchantment != null) {
                    int maxLevel = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
                    possibleEnchantments.add(new EnchantmentData(enchantment, maxLevel));
                }
            } catch (Exception e) {
                Bukkit.getLogger().warning("[VenQuests] Błąd parsowania enchantu: " + enchString);
            }
        }

        if (possibleEnchantments.isEmpty()) {
            return;
        }

        if (possibleEnchantments.size() == 1) {
            if (random.nextDouble() < 0.40) {
                EnchantmentData data = possibleEnchantments.get(0);
                int level = random.nextInt(data.getMaxLevel()) + 1;
                meta.addEnchant(data.getEnchantment(), level, true);
            }
        }
        // Przypadek z dwoma enchantami
        else if (possibleEnchantments.size() == 2) {
            double roll = random.nextDouble();
            EnchantmentData ench1 = possibleEnchantments.get(0);
            EnchantmentData ench2 = possibleEnchantments.get(1);

            if (roll < 0.10) { // 10% szansy na oba
                int level1 = random.nextInt(ench1.getMaxLevel()) + 1;
                int level2 = random.nextInt(ench2.getMaxLevel()) + 1;
                meta.addEnchant(ench1.getEnchantment(), level1, true);
                meta.addEnchant(ench2.getEnchantment(), level2, true);
            } else if (roll < 0.30) { // 20% szansy na pierwszy (0.10 + 0.20 = 0.30)
                int level1 = random.nextInt(ench1.getMaxLevel()) + 1;
                meta.addEnchant(ench1.getEnchantment(), level1, true);
            } else if (roll < 0.50) { // 20% szansy na drugi (0.30 + 0.20 = 0.50)
                int level2 = random.nextInt(ench2.getMaxLevel()) + 1;
                meta.addEnchant(ench2.getEnchantment(), level2, true);
            }
            // 50% szansy na brak enchantu (jeśli roll >= 0.50)
        }
        else if(possibleEnchantments.size() == 3){
            double roll = random.nextDouble();
            EnchantmentData ench1 = possibleEnchantments.get(0);
            EnchantmentData ench2 = possibleEnchantments.get(1);
            EnchantmentData ench3 = possibleEnchantments.get(2);

            if (roll < 0.05) { // 5% szansy na oba
                int level1 = random.nextInt(ench1.getMaxLevel()) + 1;
                int level2 = random.nextInt(ench2.getMaxLevel()) + 1;
                int level3 = random.nextInt(ench3.getMaxLevel()) + 1;
                meta.addEnchant(ench1.getEnchantment(), level1, true);
                meta.addEnchant(ench2.getEnchantment(), level2, true);
                meta.addEnchant(ench3.getEnchantment(), level3, true);
            } else if (roll < 0.10) { // 5% szansy na pierwszy i drugi (0.10 + 0.20 = 0.30)
                int level1 = random.nextInt(ench1.getMaxLevel()) + 1;
                int level2 = random.nextInt(ench2.getMaxLevel()) + 1;
                meta.addEnchant(ench1.getEnchantment(), level1, true);
                meta.addEnchant(ench2.getEnchantment(), level2, true);
            } else if (roll < 0.15) { // 5% szansy na drugi i 3(0.30 + 0.20 = 0.50)
                int level2 = random.nextInt(ench1.getMaxLevel()) + 1;
                int level3 = random.nextInt(ench2.getMaxLevel()) + 1;
                meta.addEnchant(ench1.getEnchantment(), level2, true);
                meta.addEnchant(ench2.getEnchantment(), level3, true);
            } else if (roll < 0.20) { // 5% szansy na 1 i 3 (0.30 + 0.20 = 0.50)
                int level1 = random.nextInt(ench1.getMaxLevel()) + 1;
                int level3 = random.nextInt(ench2.getMaxLevel()) + 1;
                meta.addEnchant(ench1.getEnchantment(), level1, true);
                meta.addEnchant(ench2.getEnchantment(), level3, true);
            } else if (roll < 0.30) { // 5% szansy na 1 i 3 (0.30 + 0.20 = 0.50)
                int level1 = random.nextInt(ench1.getMaxLevel()) + 1;
                meta.addEnchant(ench1.getEnchantment(), level1, true);
            } else if (roll < 0.40) { // 5% szansy na 1 i 3 (0.30 + 0.20 = 0.50)
                int level2 = random.nextInt(ench1.getMaxLevel()) + 1;
                meta.addEnchant(ench1.getEnchantment(), level2, true);
            } else if (roll < 0.50) { // 5% szansy na 1 i 3 (0.30 + 0.20 = 0.50)
                int level3 = random.nextInt(ench1.getMaxLevel()) + 1;
                meta.addEnchant(ench1.getEnchantment(), level3, true);
            }
            // 50% szansy na brak enchantu (jeśli roll >= 0.50)
        }
        // Można dodać logikę dla 3+ enchantów w przyszłości
        else {
            Collections.shuffle(possibleEnchantments);
            for(EnchantmentData data : possibleEnchantments) {
                if(random.nextDouble() < 0.3) { // 30% szans na każdy z listy
                    int level = random.nextInt(data.getMaxLevel()) + 1;
                    meta.addEnchant(data.getEnchantment(), level, true);
                }
            }
        }
    }

    // Klasa pomocnicza do przechowywania danych o enchancie
    private static class EnchantmentData {
        private final Enchantment enchantment;
        private final int maxLevel;

        public EnchantmentData(Enchantment enchantment, int maxLevel) {
            this.enchantment = enchantment;
            this.maxLevel = maxLevel;
        }

        public Enchantment getEnchantment() {
            return enchantment;
        }

        public int getMaxLevel() {
            return maxLevel;
        }
    }

    private int parseRandomValue(String value) {
        if (value.contains(":")) {
            String[] parts = value.split(":");
            int min = Integer.parseInt(parts[0]);
            int max = Integer.parseInt(parts[1]);
            return random.nextInt((max - min) + 1) + min;
        }
        return Integer.parseInt(value);
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

