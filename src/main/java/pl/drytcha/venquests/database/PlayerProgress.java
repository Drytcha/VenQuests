package pl.drytcha.venquests.database;

public class PlayerProgress {
    private String questId; // Usunięto 'final'
    private int progress;
    private long startTime; // Usunięto 'final'

    /**
     * Konstruktor używany przy tworzeniu nowego postępu misji.
     * Automatycznie ustawia czas startu na moment utworzenia.
     */
    public PlayerProgress(String questId, int progress) {
        this.questId = questId;
        this.progress = progress;
        this.startTime = System.currentTimeMillis();
    }

    // Prywatny konstruktor dla GSON
    private PlayerProgress() {}

    public void incrementProgress(int amount) {
        this.progress += amount;
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
}

