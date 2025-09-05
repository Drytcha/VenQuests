package pl.drytcha.venquests.config;

public enum QuestType {
    MOBS,
    DESTROY,
    CRAFTING,
    COLLECT,
    PLACE,
    ENCHANT,
    ENCHANT_ITEM,
    EATING,
    FARMING,
    FISHING;

    public enum Category {
        DAILY,
        WEEKLY,
        MONTHLY
    }
}
