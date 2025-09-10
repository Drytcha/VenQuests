package pl.drytcha.venquests.config;

public enum QuestType {
    MOBS,
    DESTROY,
    CRAFTING,
    COLLECT,
    PLACE,
    ENCHANT, // Dowolny enchant na dowolnym przedmiocie
    ENCHANT_TYPE, // Określony enchant na dowolnym przedmiocie
    ENCHANT_ITEM, // Określony enchant na określonym przedmiocie
    EATING,
    FARMING,
    FISHING,
    SWIM,
    WALK,
    GLIDING;


    public enum Category {
        DAILY,
        WEEKLY,
        MONTHLY
    }
}

