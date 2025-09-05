package pl.drytcha.venquests.utils;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class InventoryUtils {

    /**
     * Counts the total amount of a specific Material in a player's inventory.
     * This method correctly handles item stacks and items in all inventory slots.
     *
     * @param player The player whose inventory to check.
     * @param material The Material to count.
     * @return The total number of items of the specified type found in the inventory.
     */
    public static int getItemCount(Player player, Material material) {
        if (player == null || material == null) {
            return 0;
        }

        int count = 0;
        // Iterate through all items in the player's inventory
        for (ItemStack item : player.getInventory().getContents()) {
            // Check if the item is not null and matches the desired material
            if (item != null && item.getType() == material) {
                count += item.getAmount();
            }
        }
        return count;
    }
}
