package pl.drytcha.venquests.fishing;

import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import pl.drytcha.venquests.VenQuests;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RodManager {

    private final VenQuests plugin;
    private ItemStack specialRod;
    private FileConfiguration rodConfig;
    private final NamespacedKey specialRodKey;
    private final List<Reward> rewardsPool = new ArrayList<>();
    private int totalWeight = 0;

    public RodManager(VenQuests plugin) {
        this.plugin = plugin;
        this.specialRodKey = new NamespacedKey(plugin, "special_fishing_rod");
        loadRodConfig();
        loadRod();
        loadRewards();
    }

    public void loadRodConfig() {
        File rodFile = new File(plugin.getDataFolder(), "wedka.yml");
        if (!rodFile.exists()) {
            plugin.saveResource("wedka.yml", false);
        }
        rodConfig = YamlConfiguration.loadConfiguration(rodFile);
    }

    public void loadRod() {
        String itemsAdderId = rodConfig.getString("rod.itemsadder-id", "");
        specialRod = null; // Reset itemu na wypadek przeładowania

        if (!itemsAdderId.isEmpty() && Bukkit.getPluginManager().isPluginEnabled("ItemsAdder")) {
            CustomStack customStack = CustomStack.getInstance(itemsAdderId);
            if (customStack != null) {
                specialRod = customStack.getItemStack();
            }
        }

        if (specialRod == null) {
            specialRod = new ItemStack(Material.FISHING_ROD);
        }

        ItemMeta meta = specialRod.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', rodConfig.getString("rod.name", "&bMagiczna Wędka")));
            List<String> lore = rodConfig.getStringList("rod.lore").stream()
                    .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                    .collect(Collectors.toList());
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(specialRodKey, PersistentDataType.BYTE, (byte) 1);
            specialRod.setItemMeta(meta);
        }
    }

    public void loadRewards() {
        rewardsPool.clear();
        totalWeight = 0;
        List<Map<?, ?>> rewardsList = rodConfig.getMapList("rewards");

        for (Map<?, ?> rewardMap : rewardsList) {
            try {
                String typeStr = (String) rewardMap.get("type");
                Reward.RewardType type = Reward.RewardType.valueOf(typeStr.toUpperCase());
                String content = (String) rewardMap.get("content");
                int weight = (int) rewardMap.get("weight");
                String message = (String) rewardMap.get("message");

                rewardsPool.add(new Reward(type, content, weight, message));
                totalWeight += weight;

            } catch (Exception e) {
                plugin.getLogger().warning("[VenQuests] Błąd podczas ładowania nagrody z wedka.yml: " + e.getMessage());
            }
        }
    }


    public ItemStack getSpecialRod() {
        return specialRod.clone();
    }

    public boolean isSpecialRod(ItemStack item) {
        if (item == null || item.getItemMeta() == null) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer().has(specialRodKey, PersistentDataType.BYTE);
    }

    public int getDuration() {
        return rodConfig.getInt("settings.duration", 10);
    }

    public double getMaxDistance() {
        return rodConfig.getDouble("settings.max-distance", 30.0);
    }

    public List<Reward> getRewardsPool() {
        return rewardsPool;
    }

    public int getTotalWeight() {
        return totalWeight;
    }
}

