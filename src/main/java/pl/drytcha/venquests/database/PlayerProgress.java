package pl.drytcha.venquests.database;

public class PlayerProgress {
    private String questId;
    private int progress;
    private long startTime;
    private int startValue; // Do śledzenia statystyk początkowych (dla misji ruchowych)

    public PlayerProgress(String questId, int progress) {
        this.questId = questId;
        this.progress = progress;
        this.startTime = System.currentTimeMillis();
        this.startValue = 0; // Domyślna wartość
    }

    private PlayerProgress() {}

    public void incrementProgress(int amount) {
        this.progress += amount;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    // Gettery
    public String getQuestId() {
        return questId;
    }

    public int getProgress() {
        return progress;
    }

    public long getStartTime() {
        return startTime;
    }

    public int getStartValue() {
        return startValue;
    }

    public void setStartValue(int startValue) {
        this.startValue = startValue;
    }
}

