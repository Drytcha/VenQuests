package pl.drytcha.venquests.player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerManager {
    private final Map<UUID, PlayerData> playerDataMap = new HashMap<>();

    public void addPlayer(UUID uuid, PlayerData playerData) {
        playerDataMap.put(uuid, playerData);
    }

    public void removePlayer(UUID uuid) {
        playerDataMap.remove(uuid);
    }

    public PlayerData getPlayerData(UUID uuid) {
        return playerDataMap.get(uuid);
    }
}
