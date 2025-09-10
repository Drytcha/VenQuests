package pl.drytcha.venquests.config;

import java.util.List;

public class Quest {

    private final String id;
    private final String name;
    private final QuestType type;
    private final QuestType.Category category;
    private final List<String> mobs;
    private final List<String> blocks;
    private final List<String> items; // Zastępuje 'item'
    private final int amount;
    private final List<String> rewards;
    private final List<String> lore;
    private final String enchant;
    private final List<String> eatenItems; // Zastępuje 'eatenItem'
    private final List<String> fishedItems; // Zastępuje 'fishedItem'

    public Quest(String id, String name, QuestType type, QuestType.Category category, List<String> mobs, List<String> blocks, List<String> items, int amount, List<String> rewards, List<String> lore, String enchant, List<String> eatenItems, List<String> fishedItems) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.category = category;
        this.mobs = mobs;
        this.blocks = blocks;
        this.items = items;
        this.amount = amount;
        this.rewards = rewards;
        this.lore = lore;
        this.enchant = enchant;
        this.eatenItems = eatenItems;
        this.fishedItems = fishedItems;
    }

    // Gettery
    public String getId() { return id; }
    public String getName() { return name; }
    public QuestType getType() { return type; }
    public QuestType.Category getCategory() { return category; }
    public List<String> getMobs() { return mobs; }
    public List<String> getBlocks() { return blocks; }
    public List<String> getItems() { return items; }
    public int getAmount() { return amount; }
    public List<String> getRewards() { return rewards; }
    public List<String> getLore() { return lore; }
    public String getEnchant() { return enchant; }
    public List<String> getEatenItems() { return eatenItems; }
    public List<String> getFishedItems() { return fishedItems; }
}

