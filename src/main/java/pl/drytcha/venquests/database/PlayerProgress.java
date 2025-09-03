package pl.drytcha.venquests.database;

public class PlayerProgress {
    private String questId;
    private int progress;

    public PlayerProgress(String questId, int progress) {
        this.questId = questId;
        this.progress = progress;
    }

    public String getQuestId() {
        return questId;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public void incrementProgress(int amount) {
        this.progress += amount;
    }
}
