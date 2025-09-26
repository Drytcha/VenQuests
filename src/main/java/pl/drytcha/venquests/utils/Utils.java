package pl.drytcha.venquests.utils;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import pl.drytcha.venquests.VenQuests;
import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Utils {

    private static FileConfiguration messagesConfig;
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    public static void loadMessages(VenQuests plugin) {
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public static String getMessage(String path) {
        if (messagesConfig == null) {
            loadMessages(VenQuests.getInstance());
        }
        String message = messagesConfig.getString(path, "&cMessage not found: " + path);
        return colorize(message);
    }

    public static String colorize(String message) {
        if (message == null) return "";

        // Ręczna konwersja formatu &#RRGGBB na format zrozumiały dla Spigota (&x&R&R&G&G&B&B)
        Matcher matcher = HEX_PATTERN.matcher(message);
        while (matcher.find()) {
            String hexCode = matcher.group(1);
            StringBuilder bukkitHex = new StringBuilder("&x");
            for (char c : hexCode.toCharArray()) {
                bukkitHex.append('&').append(c);
            }
            message = message.replace(matcher.group(), bukkitHex.toString());
            // Zresetuj matcher, aby znaleźć kolejne wystąpienia w zmodyfikowanym stringu
            matcher = HEX_PATTERN.matcher(message);
        }

        // Translate standard color codes and the newly formatted HEX codes
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static List<String> colorize(List<String> messages) {
        if (messages == null) return null;
        return messages.stream().map(Utils::colorize).collect(Collectors.toList());
    }

    public static String formatTime(long millis) {
        if (millis <= 0) {
            return "0s";
        }
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);
        long days = TimeUnit.SECONDS.toDays(seconds);
        seconds -= TimeUnit.DAYS.toSeconds(days);
        long hours = TimeUnit.SECONDS.toHours(seconds);
        seconds -= TimeUnit.HOURS.toSeconds(hours);
        long minutes = TimeUnit.SECONDS.toMinutes(seconds);
        seconds -= TimeUnit.MINUTES.toSeconds(minutes);

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0 || sb.length() == 0) sb.append(seconds).append("s");

        return sb.toString().trim();
    }
}

