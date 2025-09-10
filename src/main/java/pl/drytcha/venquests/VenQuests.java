package pl.drytcha.venquests;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import pl.drytcha.venquests.commands.CommandManager;
import pl.drytcha.venquests.config.QuestManager;
import pl.drytcha.venquests.database.DatabaseManager;
import pl.drytcha.venquests.database.SQLiteManager;
import pl.drytcha.venquests.commands.GUI;
import pl.drytcha.venquests.player.PlayerEvents;
import pl.drytcha.venquests.config.QuestListeners;
import pl.drytcha.venquests.player.PlayerManager;
import pl.drytcha.venquests.utils.EconomyManager;
import pl.drytcha.venquests.utils.Utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class VenQuests extends JavaPlugin {

    private static VenQuests instance;
    private DatabaseManager databaseManager;
    private PlayerManager playerManager;
    private QuestManager questManager;
    private EconomyManager economyManager;
    private GUI gui;
    private Economy vaultEconomy = null;
    private final Map<UUID, BukkitTask> guiUpdateTasks = new HashMap<>();


    @Override
    public void onEnable() {
        instance = this;

        // Zapisywanie domyślnych plików konfiguracyjnych
        saveDefaultConfig();
        saveResource("messages.yml", false);
        saveResource("quests_daily.yml", false);
        saveResource("quests_weekly.yml", false);
        saveResource("quests_monthly.yml", false);
        Utils.loadMessages(this);

        // Inicjalizacja managerów
        this.playerManager = new PlayerManager();
        this.questManager = new QuestManager(this);
        this.questManager.loadQuests();

        // Inicjalizacja bazy danych
        if (Objects.requireNonNull(getConfig().getString("database.type")).equalsIgnoreCase("SQLITE")) {
            this.databaseManager = new SQLiteManager(this);
        } else {
            getLogger().severe("Nieprawidłowy typ bazy danych! Na ten moment wspierane jest tylko SQLITE.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Inicjalizacja GUI i Vault
        this.gui = new GUI(this);
        if (!setupEconomy()) {
            getLogger().warning("Nie znaleziono Vault lub pluginu ekonomii! Funkcje kupowania misji będą wyłączone.");
            this.economyManager = new EconomyManager(null);
        } else {
            getLogger().info("Pomyślnie połączono z Vault!");
            this.economyManager = new EconomyManager(vaultEconomy);
        }

        // Rejestracja komend i listenerów
        registerCommands();
        registerListeners();

        getLogger().info("Plugin VenQuests został pomyślnie włączony!");
    }

    @Override
    public void onDisable() {
        // Anuluj wszystkie działające zadania odświeżania GUI
        guiUpdateTasks.values().forEach(BukkitTask::cancel);
        guiUpdateTasks.clear();

        if (databaseManager != null) {
            databaseManager.close();
        }
        getLogger().info("Plugin VenQuests został wyłączony.");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        vaultEconomy = rsp.getProvider();
        return vaultEconomy != null;
    }

    private void registerCommands() {
        CommandManager commandManager = new CommandManager(this);
        getCommand("venquests").setExecutor(commandManager);
        getCommand("venquests").setTabCompleter(commandManager);
        getCommand("misje").setExecutor(commandManager);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerEvents(this), this);
        getServer().getPluginManager().registerEvents(new QuestListeners(this), this);
        getServer().getPluginManager().registerEvents(gui, this);
    }

    // Gettery
    public static VenQuests getInstance() {
        return instance;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public PlayerManager getPlayerManager() {
        return playerManager;
    }

    public QuestManager getQuestManager() {
        return questManager;
    }

    public GUI getGui() {
        return gui;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public Map<UUID, BukkitTask> getGuiUpdateTasks() {
        return guiUpdateTasks;
    }
}

