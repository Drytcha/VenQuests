package pl.drytcha.venquests.fishing;

import java.util.List;

public class Reward {
    private final RewardType type;
    private final String content;
    private final int weight;
    private final String message;
    private final String amount;
    private final String durability;
    private final List<String> enchantments;

    public Reward(RewardType type, String content, int weight, String message, String amount, String durability, List<String> enchantments) {
        this.type = type;
        this.content = content;
        this.weight = weight;
        this.message = message;
        this.amount = amount;
        this.durability = durability;
        this.enchantments = enchantments;
    }

    public RewardType getType() {
        return type;
    }

    public String getContent() {
        return content;
    }

    public int getWeight() {
        return weight;
    }

    public String getMessage() {
        return message;
    }

    public String getAmount() {
        return amount;
    }

    public String getDurability() {
        return durability;
    }

    public List<String> getEnchantments() {
        return enchantments;
    }


    public enum RewardType {
        ITEM,
        ITEM_DURABILITY,
        COMMAND
    }
}
