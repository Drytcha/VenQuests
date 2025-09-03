package pl.drytcha.venquests.database;

import pl.drytcha.venquests.player.PlayerData;
import java.util.UUID;

public interface DatabaseManager {
    void connect();
    void close();
    void createTables();
    PlayerData loadPlayerData(UUID uuid);
    void savePlayerData(PlayerData playerData);
    void resetPlayerData(UUID uuid, String type);
}

