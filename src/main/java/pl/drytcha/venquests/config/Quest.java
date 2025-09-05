package pl.drytcha.venquests.config;

import java.util.List;

public class Quest {

    private final String id;
    private final String name;
    private final QuestType type;
    private final QuestType.Category category;
    private final List<String> mobs;
    private final List<String> blocks;
    private final String item;
    private final int amount;
    private final List<String> rewards;
    private final List<String> lore;
    private final String enchant;
    private final int enchantLevel;
    private final String eatenItem;
    private final List<String> farmedItems;
    private final String fishedItem;

    /**
     * Kompletny konstruktor dla obiektu Misji.
     *
     * @param id Unikalne ID misji (np. DAILY_1)
     * @param name Nazwa misji widoczna dla gracza
     * @param type Typ misji (np. MOBS, DESTROY)
     * @param category Kategoria misji (DAILY, WEEKLY, MONTHLY)
     * @param mobs Lista mobów do zabicia
     * @param blocks Lista bloków do zniszczenia
     * @param item Przedmiot do stworzenia, zebrania lub umieszczenia
     * @param amount Wymagana ilość
     * @param rewards Komendy-nagrody
     * @param lore Opis misji
     * @param enchant Nazwa zaklecia
     * @param enchantLevel Poziom zaklecia
     * @param eatenItem Zjedzony przedmiot
     * @param farmedItems Lista przedmiotow
     * @param fishedItem Przedmiot zlowniony
     */
    public Quest(String id, String name, QuestType type, QuestType.Category category, List<String> mobs, List<String> blocks, String item, int amount, List<String> rewards, List<String> lore, String enchant, int enchantLevel, String eatenItem, List<String> farmedItems, String fishedItem) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.category = category;
        this.mobs = mobs;
        this.blocks = blocks;
        this.item = item;
        this.amount = amount;
        this.rewards = rewards;
        this.lore = lore;
        this.enchant = enchant;
        this.enchantLevel = enchantLevel;
        this.eatenItem = eatenItem;
        this.farmedItems = farmedItems;
        this.fishedItem = fishedItem;
    }

    // Gettery do wszystkich pól

    public String getId() { return id; }
    public String getName() { return name; }
    public QuestType getType() { return type; }
    public QuestType.Category getCategory() { return category; }
    public List<String> getMobs() { return mobs; }
    public List<String> getBlocks() { return blocks; }
    public String getItem() { return item; }
    public int getAmount() { return amount; }
    public List<String> getRewards() { return rewards; }
    public List<String> getLore() { return lore; }
    public String getEnchant() { return enchant; }
    public int getEnchantLevel() { return enchantLevel; }
    public String getEatenItem() { return eatenItem; }
    public List<String> getFarmedItems() { return farmedItems; }
    public String getFishedItem() { return fishedItem; }
}
