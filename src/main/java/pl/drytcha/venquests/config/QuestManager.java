package pl.drytcha.venquests.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import pl.drytcha.venquests.VenQuests;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class QuestManager {
    private final VenQuests plugin;
    private final Map<String, Quest> dailyQuests = new HashMap<>();
    private final Map<String, Quest> weeklyQuests = new HashMap<>();
    private final Map<String, Quest> monthlyQuests = new HashMap<>();

    public QuestManager(VenQuests plugin) {
        this.plugin = plugin;
    }

    public void loadQuests() {
        dailyQuests.clear();
        weeklyQuests.clear();
        monthlyQuests.clear();

        loadQuestFile("quests_daily.yml", dailyQuests);
        loadQuestFile("quests_weekly.yml", weeklyQuests);
        loadQuestFile("quests_monthly.yml", monthlyQuests);

        plugin.getLogger().info("Załadowano " + dailyQuests.size() + " misji dziennych.");
        plugin.getLogger().info("Załadowano " + weeklyQuests.size() + " misji tygodniowych.");
        plugin.getLogger().info("Załadowano " + monthlyQuests.size() + " misji miesięcznych.");
    }

    private void loadQuestFile(String fileName, Map<String, Quest> questMap) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            plugin.saveResource(fileName, false);
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        for (String key : config.getKeys(false)) {
            String name = config.getString(key + ".name");
            QuestType type = QuestType.valueOf(config.getString(key + ".type").toUpperCase());
            int amount = config.getInt(key + ".amount");
            List<String> lore = config.getStringList(key + ".lore");
            List<String> rewards = config.getStringList(key + ".rewards");

            Quest quest = new Quest(key, name, type, amount, lore, rewards);

            if (type == QuestType.MOBS) {
                quest.setEntities(config.getStringList(key + ".mobs").stream().map(String::toUpperCase).collect(Collectors.toList()));
            } else if (type == QuestType.DESTROY) {
                quest.setBlocks(config.getStringList(key + ".blocks").stream().map(String::toUpperCase).collect(Collectors.toList()));
            } else if (type == QuestType.CRAFTING) {
                quest.setItem(config.getString(key + ".item").toUpperCase());
            }

            questMap.put(key, quest);
        }
    }

    public Quest getQuestById(String id) {
        if (dailyQuests.containsKey(id)) return dailyQuests.get(id);
        if (weeklyQuests.containsKey(id)) return weeklyQuests.get(id);
        if (monthlyQuests.containsKey(id)) return monthlyQuests.get(id);
        return null;
    }

    public QuestType.Category getQuestCategory(String questId) {
        if (dailyQuests.containsKey(questId)) return QuestType.Category.DAILY;
        if (weeklyQuests.containsKey(questId)) return QuestType.Category.WEEKLY;
        if (monthlyQuests.containsKey(questId)) return QuestType.Category.MONTHLY;
        return null;
    }

    public Quest getRandomQuest(QuestType.Category category) {
        Map<String, Quest> questMap;
        switch (category) {
            case DAILY: questMap = dailyQuests; break;
            case WEEKLY: questMap = weeklyQuests; break;
            case MONTHLY: questMap = monthlyQuests; break;
            default: return null;
        }

        if (questMap.isEmpty()) return null;

        List<String> keys = new ArrayList<>(questMap.keySet());
        String randomKey = keys.get((int) (Math.random() * keys.size()));
        return questMap.get(randomKey);
    }
}

