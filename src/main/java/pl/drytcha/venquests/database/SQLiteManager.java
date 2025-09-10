package pl.drytcha.venquests.database;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import pl.drytcha.venquests.VenQuests;
import pl.drytcha.venquests.player.PlayerData;

import java.io.File;
import java.lang.reflect.Type;
import java.sql.*;
import java.util.ArrayList;
import java.util.UUID;

public class SQLiteManager implements DatabaseManager {

    private final VenQuests plugin;
    private Connection connection;
    private final Gson gson = new Gson();

    public SQLiteManager(VenQuests plugin) {
        this.plugin = plugin;
        connect();
        createTables();
    }

    @Override
    public void connect() {
        try {
            File dbFile = new File(plugin.getDataFolder(), "playerdata.db");
            if (!dbFile.exists()) {
                dbFile.createNewFile();
            }
            String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
            connection = DriverManager.getConnection(url);
            plugin.getLogger().info("Połączono z bazą danych SQLite.");
        } catch (Exception e) {
            plugin.getLogger().severe("Nie można połączyć z bazą SQLite! " + e.getMessage());
        }
    }

    @Override
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void createTables() {
        String query = "CREATE TABLE IF NOT EXISTS player_quests (" +
                "uuid VARCHAR(36) PRIMARY KEY NOT NULL," +
                "daily_quest TEXT," +
                "daily_cooldown BIGINT DEFAULT 0," +
                "weekly_quest TEXT," +
                "weekly_cooldown BIGINT DEFAULT 0," +
                "monthly_quest TEXT," +
                "monthly_cooldown BIGINT DEFAULT 0," +
                "additional_daily_quests TEXT," +
                "additional_weekly_quests TEXT," +
                "additional_monthly_quests TEXT," +
                "daily_quests_bought INTEGER DEFAULT 0," +
                "last_daily_buy_timestamp BIGINT DEFAULT 0" +
                ");";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public PlayerData loadPlayerData(UUID uuid) {
        String query = "SELECT * FROM player_quests WHERE uuid = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, uuid.toString());
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                PlayerData data = new PlayerData(uuid);
                Type progressType = new TypeToken<PlayerProgress>() {}.getType();
                Type progressListType = new TypeToken<ArrayList<PlayerProgress>>() {}.getType();

                data.setDailyQuest(gson.fromJson(rs.getString("daily_quest"), progressType));
                data.setWeeklyQuest(gson.fromJson(rs.getString("weekly_quest"), progressType));
                data.setMonthlyQuest(gson.fromJson(rs.getString("monthly_quest"), progressType));

                data.setDailyCooldown(rs.getLong("daily_cooldown"));
                data.setWeeklyCooldown(rs.getLong("weekly_cooldown"));
                data.setMonthlyCooldown(rs.getLong("monthly_cooldown"));

                data.setAdditionalDailyQuests(gson.fromJson(rs.getString("additional_daily_quests"), progressListType));
                data.setAdditionalWeeklyQuests(gson.fromJson(rs.getString("additional_weekly_quests"), progressListType));
                data.setAdditionalMonthlyQuests(gson.fromJson(rs.getString("additional_monthly_quests"), progressListType));

                data.setDailyQuestsBought(rs.getInt("daily_quests_bought"));
                data.setLastDailyBuyTimestamp(rs.getLong("last_daily_buy_timestamp"));

                return data;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new PlayerData(uuid); // Zwraca nowy obiekt, jeśli gracz nie istnieje w bazie
    }

    @Override
    public void savePlayerData(PlayerData playerData) {
        String query = "INSERT INTO player_quests (uuid, daily_quest, daily_cooldown, weekly_quest, weekly_cooldown, monthly_quest, monthly_cooldown, additional_daily_quests, additional_weekly_quests, additional_monthly_quests, daily_quests_bought, last_daily_buy_timestamp) " +
                "VALUES(?,?,?,?,?,?,?,?,?,?,?,?) " +
                "ON CONFLICT(uuid) DO UPDATE SET " +
                "daily_quest=excluded.daily_quest, daily_cooldown=excluded.daily_cooldown, " +
                "weekly_quest=excluded.weekly_quest, weekly_cooldown=excluded.weekly_cooldown, " +
                "monthly_quest=excluded.monthly_quest, monthly_cooldown=excluded.monthly_cooldown, " +
                "additional_daily_quests=excluded.additional_daily_quests, " +
                "additional_weekly_quests=excluded.additional_weekly_quests, " +
                "additional_monthly_quests=excluded.additional_monthly_quests, " +
                "daily_quests_bought=excluded.daily_quests_bought, " +
                "last_daily_buy_timestamp=excluded.last_daily_buy_timestamp;";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, playerData.getUuid().toString());
            statement.setString(2, gson.toJson(playerData.getDailyQuest()));
            statement.setLong(3, playerData.getDailyCooldown());
            statement.setString(4, gson.toJson(playerData.getWeeklyQuest()));
            statement.setLong(5, playerData.getWeeklyCooldown());
            statement.setString(6, gson.toJson(playerData.getMonthlyQuest()));
            statement.setLong(7, playerData.getMonthlyCooldown());
            statement.setString(8, gson.toJson(playerData.getAdditionalDailyQuests()));
            statement.setString(9, gson.toJson(playerData.getAdditionalWeeklyQuests()));
            statement.setString(10, gson.toJson(playerData.getAdditionalMonthlyQuests()));
            statement.setInt(11, playerData.getDailyQuestsBought());
            statement.setLong(12, playerData.getLastDailyBuyTimestamp());
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void resetPlayerData(UUID uuid, String type) {
        PlayerData data = loadPlayerData(uuid);
        switch (type) {
            case "daily":
                data.setDailyQuest(null);
                data.setDailyCooldown(0);
                if (data.getAdditionalDailyQuests() != null) data.getAdditionalDailyQuests().clear();
                data.setDailyQuestsBought(0);
                data.setLastDailyBuyTimestamp(0);
                break;
            case "weekly":
                data.setWeeklyQuest(null);
                data.setWeeklyCooldown(0);
                if (data.getAdditionalWeeklyQuests() != null) data.getAdditionalWeeklyQuests().clear();
                break;
            case "monthly":
                data.setMonthlyQuest(null);
                data.setMonthlyCooldown(0);
                if (data.getAdditionalMonthlyQuests() != null) data.getAdditionalMonthlyQuests().clear();
                break;
            case "all":
                data.setDailyQuest(null);
                data.setDailyCooldown(0);
                if (data.getAdditionalDailyQuests() != null) data.getAdditionalDailyQuests().clear();
                data.setDailyQuestsBought(0);
                data.setLastDailyBuyTimestamp(0);
                data.setWeeklyQuest(null);
                data.setWeeklyCooldown(0);
                if (data.getAdditionalWeeklyQuests() != null) data.getAdditionalWeeklyQuests().clear();
                data.setMonthlyQuest(null);
                data.setMonthlyCooldown(0);
                if (data.getAdditionalMonthlyQuests() != null) data.getAdditionalMonthlyQuests().clear();
                break;
        }
        savePlayerData(data);
    }
}

