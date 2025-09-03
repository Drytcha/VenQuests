package pl.drytcha.venquests.config;

import java.util.List;

public class Quest {
    private final String id;
    private final String name;
    private final QuestType type;
    private final int amount;
    private final List<String> lore;
    private final List<String> rewards;

    // Specyficzne dla typów
    private List<String> entities;
    private List<String> blocks;
    private String item;

    public Quest(String id, String name, QuestType type, int amount, List<String> lore, List<String> rewards) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.amount = amount;
        this.lore = lore;
        this.rewards = rewards;
    }

    // Gettery i Settery
    public String getId() { return id; }
    public String getName() { return name; }
    public QuestType getType() { return type; }
    public int getAmount() { return amount; }
    public List<String> getLore() { return lore; }
    public List<String> getRewards() { return rewards; }

    public List<String> getEntities() { return entities; }
    public void setEntities(List<String> entities) { this.entities = entities; }

    public List<String> getBlocks() { return blocks; }
    public void setBlocks(List<String> blocks) { this.blocks = blocks; }

    public String getItem() { return item; }
    public void setItem(String item) { this.item = item; }
}
