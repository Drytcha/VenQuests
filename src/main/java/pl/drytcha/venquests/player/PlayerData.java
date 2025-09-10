package pl.drytcha.venquests.player;

import pl.drytcha.venquests.VenQuests;
import pl.drytcha.venquests.config.QuestType;
import pl.drytcha.venquests.database.PlayerProgress;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class PlayerData {
    private final UUID uuid;
    private PlayerProgress dailyQuest;
    private PlayerProgress weeklyQuest;
    private PlayerProgress monthlyQuest;
    private long dailyCooldown;
    private long weeklyCooldown;
    private long monthlyCooldown;

    private List<PlayerProgress> additionalDailyQuests = new ArrayList<>();
    private List<PlayerProgress> additionalWeeklyQuests = new ArrayList<>();
    private List<PlayerProgress> additionalMonthlyQuests = new ArrayList<>();

    private int dailyQuestsBought;
    private long lastDailyBuyTimestamp;

    // Zestaw do śledzenia misji, które są w trakcie oznaczania jako ukończone, aby uniknąć wielokrotnego przyznawania nagród.
    // 'transient' oznacza, że to pole nie będzie zapisywane w bazie danych.
    private transient final Set<String> questsBeingCompleted = new HashSet<>();

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
    }

    /**
     * Sprawdza i oznacza misję jako "w trakcie ukończenia".
     * @param questId ID misji do sprawdzenia.
     * @return true, jeśli misja została pomyślnie oznaczona; false, jeśli była już oznaczona.
     */
    public boolean startCompletingQuest(String questId) {
        return questsBeingCompleted.add(questId);
    }

    /**
     * Usuwa oznaczenie misji "w trakcie ukończenia".
     * @param questId ID misji.
     */
    public void finishCompletingQuest(String questId) {
        questsBeingCompleted.remove(questId);
    }

    public List<PlayerProgress> getActiveQuestsForCategory(QuestType.Category category) {
        List<PlayerProgress> active = new ArrayList<>();
        PlayerProgress main = getMainQuest(category);
        if(main != null) {
            active.add(main);
        }
        switch(category) {
            case DAILY: active.addAll(getAdditionalDailyQuests()); break;
            case WEEKLY: active.addAll(getAdditionalWeeklyQuests()); break;
            case MONTHLY: active.addAll(getAdditionalMonthlyQuests()); break;
        }
        return active;
    }

    public PlayerProgress getMainQuest(QuestType.Category category) {
        switch (category) {
            case DAILY: return dailyQuest;
            case WEEKLY: return weeklyQuest;
            case MONTHLY: return monthlyQuest;
            default: return null;
        }
    }

    public PlayerProgress addAdditionalQuest(QuestType.Category category, String questId) {
        PlayerProgress progress = new PlayerProgress(questId, 0);
        switch (category) {
            case DAILY: getAdditionalDailyQuests().add(progress); break;
            case WEEKLY: getAdditionalWeeklyQuests().add(progress); break;
            case MONTHLY: getAdditionalMonthlyQuests().add(progress); break;
        }
        return progress;
    }

    public PlayerProgress setMainQuest(QuestType.Category category, String questId) {
        PlayerProgress progress = new PlayerProgress(questId, 0);
        switch (category) {
            case DAILY: this.dailyQuest = progress; break;
            case WEEKLY: this.weeklyQuest = progress; break;
            case MONTHLY: this.monthlyQuest = progress; break;
        }
        return progress;
    }

    public void removeQuest(QuestType.Category category, PlayerProgress progress) {
        switch (category) {
            case DAILY:
                if (dailyQuest != null && dailyQuest.getQuestId().equals(progress.getQuestId())) dailyQuest = null;
                else getAdditionalDailyQuests().remove(progress);
                break;
            case WEEKLY:
                if (weeklyQuest != null && weeklyQuest.getQuestId().equals(progress.getQuestId())) weeklyQuest = null;
                else getAdditionalWeeklyQuests().remove(progress);
                break;
            case MONTHLY:
                if (monthlyQuest != null && monthlyQuest.getQuestId().equals(progress.getQuestId())) monthlyQuest = null;
                else getAdditionalMonthlyQuests().remove(progress);
                break;
        }
    }

    public boolean isOnCooldown(QuestType.Category category) {
        return getRemainingCooldown(category) > 0;
    }

    public long getRemainingCooldown(QuestType.Category category) {
        long cooldownEndTime = 0;
        switch (category) {
            case DAILY: cooldownEndTime = dailyCooldown; break;
            case WEEKLY: cooldownEndTime = weeklyCooldown; break;
            case MONTHLY: cooldownEndTime = monthlyCooldown; break;
        }
        return Math.max(0, cooldownEndTime - System.currentTimeMillis());
    }

    public void setCooldown(QuestType.Category category) {
        long cooldownSeconds = VenQuests.getInstance().getConfig().getLong("cooldowns." + category.name().toLowerCase());
        long cooldownMillis = TimeUnit.SECONDS.toMillis(cooldownSeconds);
        long newCooldown = System.currentTimeMillis() + cooldownMillis;
        switch (category) {
            case DAILY: this.dailyCooldown = newCooldown; break;
            case WEEKLY: this.weeklyCooldown = newCooldown; break;
            case MONTHLY: this.monthlyCooldown = newCooldown; break;
        }
    }

    public boolean isMainQuest(QuestType.Category category, PlayerProgress progress) {
        PlayerProgress main = getMainQuest(category);
        return main != null && main.getQuestId().equals(progress.getQuestId());
    }

    public boolean canBuyQuest(QuestType.Category category) {
        int limit = VenQuests.getInstance().getConfig().getInt("buy_quest." + category.name().toLowerCase() + ".limit", 99);
        if (!isSameDay(lastDailyBuyTimestamp, System.currentTimeMillis())) {
            dailyQuestsBought = 0;
        }
        int boughtAmount = 0;
        switch(category) {
            case DAILY: boughtAmount = dailyQuestsBought; break;
        }
        return boughtAmount < limit;
    }

    public void incrementQuestsBought(QuestType.Category category) {
        if (category == QuestType.Category.DAILY) {
            if (!isSameDay(lastDailyBuyTimestamp, System.currentTimeMillis())) {
                dailyQuestsBought = 0;
            }
            this.dailyQuestsBought++;
            this.lastDailyBuyTimestamp = System.currentTimeMillis();
        }
    }

    private boolean isSameDay(long time1, long time2) {
        if (time1 == 0) return false;
        Calendar cal1 = Calendar.getInstance();
        cal1.setTimeInMillis(time1);
        Calendar cal2 = Calendar.getInstance();
        cal2.setTimeInMillis(time2);
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    // Gettery i Settery
    public UUID getUuid() { return uuid; }
    public PlayerProgress getDailyQuest() { return dailyQuest; }
    public void setDailyQuest(PlayerProgress dailyQuest) { this.dailyQuest = dailyQuest; }
    public PlayerProgress getWeeklyQuest() { return weeklyQuest; }
    public void setWeeklyQuest(PlayerProgress weeklyQuest) { this.weeklyQuest = weeklyQuest; }
    public PlayerProgress getMonthlyQuest() { return monthlyQuest; }
    public void setMonthlyQuest(PlayerProgress monthlyQuest) { this.monthlyQuest = monthlyQuest; }
    public long getDailyCooldown() { return dailyCooldown; }
    public void setDailyCooldown(long dailyCooldown) { this.dailyCooldown = dailyCooldown; }
    public long getWeeklyCooldown() { return weeklyCooldown; }
    public void setWeeklyCooldown(long weeklyCooldown) { this.weeklyCooldown = weeklyCooldown; }
    public long getMonthlyCooldown() { return monthlyCooldown; }
    public void setMonthlyCooldown(long monthlyCooldown) { this.monthlyCooldown = monthlyCooldown; }
    public List<PlayerProgress> getAdditionalDailyQuests() { if(additionalDailyQuests == null) additionalDailyQuests = new ArrayList<>(); return additionalDailyQuests; }
    public void setAdditionalDailyQuests(List<PlayerProgress> quests) { this.additionalDailyQuests = quests; }
    public List<PlayerProgress> getAdditionalWeeklyQuests() { if(additionalWeeklyQuests == null) additionalWeeklyQuests = new ArrayList<>(); return additionalWeeklyQuests; }
    public void setAdditionalWeeklyQuests(List<PlayerProgress> quests) { this.additionalWeeklyQuests = quests; }
    public List<PlayerProgress> getAdditionalMonthlyQuests() { if(additionalMonthlyQuests == null) additionalMonthlyQuests = new ArrayList<>(); return additionalMonthlyQuests; }
    public void setAdditionalMonthlyQuests(List<PlayerProgress> quests) { this.additionalMonthlyQuests = quests; }
    public int getDailyQuestsBought() { return dailyQuestsBought; }
    public void setDailyQuestsBought(int dailyQuestsBought) { this.dailyQuestsBought = dailyQuestsBought; }
    public long getLastDailyBuyTimestamp() { return lastDailyBuyTimestamp; }
    public void setLastDailyBuyTimestamp(long lastDailyBuyTimestamp) { this.lastDailyBuyTimestamp = lastDailyBuyTimestamp; }
}

