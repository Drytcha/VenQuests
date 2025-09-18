package pl.drytcha.venquests.fishing;

public class Reward {
    private final RewardType type;
    private final String content;
    private final int weight;
    private final String message;

    public Reward(RewardType type, String content, int weight, String message) {
        this.type = type;
        this.content = content;
        this.weight = weight;
        this.message = message;
    }

    public RewardType getType() {
        return type;
    }

    public String getContent() {
        return content;
    }

    public int getWeight() {
        return weight;
    }

    public String getMessage() {
        return message;
    }

    public enum RewardType {
        ITEM,
        COMMAND
    }
}
