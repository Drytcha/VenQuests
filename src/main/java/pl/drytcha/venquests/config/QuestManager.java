package pl.drytcha.venquests.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import pl.drytcha.venquests.VenQuests;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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

            // Wczytywanie listy lub pojedynczego stringa dla kompatybilności wstecznej
            List<String> items = getListOrSingle(config, path + "items", path + "item");
            List<String> eatenItems = getListOrSingle(config, path + "eatenItems", path + "eatenItem");
            List<String> fishedItems = getListOrSingle(config, path + "fishedItems", path + "fishedItem");

            String enchant = config.getString(path + "enchant");

            String fullId = category.name() + "_" + id;
            quests.put(fullId, new Quest(fullId, name, type, category, mobs, blocks, items, amount, rewards, lore, enchant, eatenItems, fishedItems));
        }
    }

    private List<String> getListOrSingle(FileConfiguration config, String listPath, String singlePath) {
        if (config.isList(listPath)) {
            return config.getStringList(listPath);
        }
        if (config.isString(singlePath)) {
            return Collections.singletonList(config.getString(singlePath));
        }
        return new ArrayList<>(); // Zwraca pustą listę, jeśli nic nie zdefiniowano
    }


    public Quest getQuestById(String id) {
        return quests.get(id);
    }

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

    public List<Quest> getQuestsByCategory(QuestType.Category category, boolean sorted) {
        List<Quest> filteredQuests = quests.values().stream()
                .filter(q -> q.getCategory() == category)
                .collect(Collectors.toList());

        if (sorted) {
            filteredQuests.sort(Comparator.comparing(q -> q.getType().name()));
        }

        return filteredQuests;
    }
}
