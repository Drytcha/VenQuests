package pl.drytcha.venquests.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import pl.drytcha.venquests.VenQuests;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class QuestManager {

    private final VenQuests plugin;
    private final Map<String, Quest> quests = new HashMap<>();

    public QuestManager(VenQuests plugin) {
        this.plugin = plugin;
    }

    public void loadQuests() {
        quests.clear();
        loadQuestsFromFile("quests_daily.yml", QuestType.Category.DAILY);
        loadQuestsFromFile("quests_weekly.yml", QuestType.Category.WEEKLY);
        loadQuestsFromFile("quests_monthly.yml", QuestType.Category.MONTHLY);
        plugin.getLogger().info("Załadowano " + quests.size() + " misji.");
    }

    private void loadQuestsFromFile(String fileName, QuestType.Category category) {
        File questsFile = new File(plugin.getDataFolder(), fileName);
        if (!questsFile.exists()) {
            plugin.saveResource(fileName, false);
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(questsFile);
        for (String id : config.getKeys(false)) {
            String path = id + ".";
            String name = config.getString(path + "name");
            QuestType type = QuestType.valueOf(config.getString(path + "type").toUpperCase());
            int amount = config.getInt(path + "amount");
            List<String> rewards = config.getStringList(path + "rewards");
            List<String> lore = config.getStringList(path + "lore");
            List<String> mobs = config.getStringList(path + "mobs");
            List<String> blocks = config.getStringList(path + "blocks");
            String item = config.getString(path + "item");
            String enchant = config.getString(path + "enchant");
            int enchantLevel = config.getInt(path + "enchantLevel");
            String eatenItem = config.getString(path + "eatenItem");
            List<String> farmedItems = config.getStringList(path + "farmedItems");
            String fishedItem = config.getString(path + "fishedItem");

            String fullId = category.name() + "_" + id;
            quests.put(fullId, new Quest(fullId, name, type, category, mobs, blocks, item, amount, rewards, lore, enchant, enchantLevel, eatenItem, farmedItems, fishedItem));
        }
    }

    public Quest getQuestById(String id) {
        return quests.get(id);
    }

    /**
     * Losuje misję z danej kategorii, wykluczając te, które gracz już posiada.
     *
     * @param category Kategoria misji.
     * @param excludedQuestIds Lista ID misji do wykluczenia.
     * @return Losowa misja lub null, jeśli nie ma dostępnych.
     */
    public Quest getRandomQuest(QuestType.Category category, List<String> excludedQuestIds) {
        List<Quest> availableQuests = quests.values().stream()
                .filter(q -> q.getCategory() == category)
                .filter(q -> !excludedQuestIds.contains(q.getId()))
                .collect(Collectors.toList());

        if (availableQuests.isEmpty()) {
            return null;
        }

        Collections.shuffle(availableQuests);
        return availableQuests.get(0);
    }

    // Metoda zwracająca wszystkie misje z danej kategorii
    public List<Quest> getQuestsByCategory(QuestType.Category category) {
        return quests.values().stream()
                .filter(q -> q.getCategory() == category)
                .collect(Collectors.toList());
    }
}
