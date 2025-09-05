package pl.drytcha.venquests.utils;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import pl.drytcha.venquests.VenQuests;
import pl.drytcha.venquests.config.QuestType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EconomyManager {
    private final Economy economy;

    public EconomyManager(Economy economy) {
        this.economy = economy;
    }

    public boolean takeCost(Player player, QuestType.Category category) {
        String path = "buy_quest." + category.name().toLowerCase() + ".cost.";
        double moneyCost = VenQuests.getInstance().getConfig().getDouble(path + "money", 0);
        List<String> itemCostStrings = VenQuests.getInstance().getConfig().getStringList(path + "items");
        Map<Material, Integer> itemCosts = parseItemCosts(itemCostStrings);

        // Sprawdzenie, czy gracz ma wystarczająco zasobów
        if (economy != null && !economy.has(player, moneyCost)) {
            player.sendMessage(Utils.getMessage("not_enough_money").replace("%cost%", String.valueOf(moneyCost)));
            return false;
        }
        if (!hasEnoughItems(player, itemCosts)) {
            player.sendMessage(Utils.getMessage("not_enough_items"));
            return false;
        }

        // Pobranie zasobów
        if (economy != null && moneyCost > 0) {
            economy.withdrawPlayer(player, moneyCost);
        }
        removeItems(player, itemCosts);

        return true;
    }

    public void giveCost(Player player, QuestType.Category category) {
        String path = "buy_quest." + category.name().toLowerCase() + ".cost.";
        double moneyCost = VenQuests.getInstance().getConfig().getDouble(path + "money", 0);
        List<String> itemCostStrings = VenQuests.getInstance().getConfig().getStringList(path + "items");
        Map<Material, Integer> itemCosts = parseItemCosts(itemCostStrings);

        if (economy != null && moneyCost > 0) {
            economy.depositPlayer(player, moneyCost);
        }
        for (Map.Entry<Material, Integer> entry : itemCosts.entrySet()) {
            player.getInventory().addItem(new ItemStack(entry.getKey(), entry.getValue()));
        }
    }


    private boolean hasEnoughItems(Player player, Map<Material, Integer> itemCosts) {
        for (Map.Entry<Material, Integer> cost : itemCosts.entrySet()) {
            if (!player.getInventory().contains(cost.getKey(), cost.getValue())) {
                return false;
            }
        }
        return true;
    }

    private void removeItems(Player player, Map<Material, Integer> itemCosts) {
        for (Map.Entry<Material, Integer> cost : itemCosts.entrySet()) {
            player.getInventory().removeItem(new ItemStack(cost.getKey(), cost.getValue()));
        }
    }

    private Map<Material, Integer> parseItemCosts(List<String> itemCostStrings) {
        Map<Material, Integer> itemCosts = new HashMap<>();
        for (String s : itemCostStrings) {
            try {
                String[] parts = s.split(":");
                Material material = Material.valueOf(parts[0].toUpperCase());
                int amount = Integer.parseInt(parts[1]);
                itemCosts.put(material, amount);
            } catch (Exception e) {
                VenQuests.getInstance().getLogger().warning("Nieprawidłowy format kosztu przedmiotu: " + s);
            }
        }
        return itemCosts;
    }
}

