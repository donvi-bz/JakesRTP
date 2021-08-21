package biz.donvi.jakesRTP;

import io.papermc.lib.PaperLib;
import org.apache.commons.lang.text.StrBuilder;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GeneralUtil {

    public static final Pattern PLACEHOLDER_REGEX               = Pattern.compile("(?=[^\\\\])%(.*?[^\\\\])%");
    public static final Pattern SLASH_N_REGEX_W_LOOKBEHIND      = Pattern.compile("(?<!\\\\)\\\\n");
    public static final Pattern SLASH_N_REGEX_DOUBLE            = Pattern.compile("\\\\n");
    public static final Pattern LEGACY_COLOR_REGEX_W_LOOKBEHIND = Pattern.compile("(?<!&)&([0-9a-fk-orx])");
    public static final Pattern LEGACY_COLOR_REGEX_DOUBLE       = Pattern.compile("&&([0-9a-fk-orx])");
    public static final Pattern HEX_COLOR_REGEX                 = Pattern.compile(
        "\\{#([\\da-fA-F])([\\da-fA-F])([\\da-fA-F])([\\da-fA-F])([\\da-fA-F])([\\da-fA-F])}");

    /**
     * Returns a string representing the given location with generic formatting, leaving out the pitch and yaw,
     * and rounding numbers as necessary.
     *
     * @param loc           The location to describe
     * @param decimalPlaces The number of decimal places for x, y, and z
     * @param forceDecimal  If true, the same number of decimal places will be shown, using 0's if there is no digit
     * @return The location as a string.
     */
    public static String locationAsString(Location loc, int decimalPlaces, boolean forceDecimal) {
        double[] pos = {loc.getX(), loc.getY(), loc.getZ()};
        String[] posS = new String[3];
        String worldName = loc.getWorld() == null ? "" : loc.getWorld().getName();
        DecimalFormat decimalFormat = decimalPlaces == 0 ? new DecimalFormat("0")
            : new DecimalFormat(new StrBuilder("0.").append(stringOf('#', decimalPlaces)).toString());
        for (int i = 0; i < pos.length; i++) posS[i] = decimalFormat.format(pos[i]);
        return worldName + " (" + posS[0] + ", " + posS[1] + ", " + posS[2] + ")";
    }

    /**
     * Creates a string that is just {@code c} {@code times} times in a row
     *
     * @param c     The sole character to make up the string
     * @param times The number of times it should appear (length of string)
     * @return A string made up of character {@code c} {@code times} times
     */
    public static String stringOf(char c, int times) {
        if (times <= 0) return "";
        if (times == 1) return String.valueOf(c);
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < times; i++) s.append(c);
        return s.toString();
    }

    /**
     * If the world exists, it gets the name of it with the correct capitalization. <p>
     * If it doesn't exist, this will return null.
     *
     * @param server    The server that is running this plugin. This is where the list of worlds will be retrieved from.
     * @param worldName The name of the world, in whatever case you would like.
     * @return The proper name of the world if it exists, null if it doesn't.
     */
    public static String worldToProperCase(Server server, String worldName) {
        final List<World> worldList = server.getWorlds();
        for (World world : worldList)
            if (world.getName().equalsIgnoreCase(worldName))
                return world.getName();
        return null;
    }

    public static World getWorldIgnoreCase(Server server, String worldName) {
        final List<World> worldList = server.getWorlds();
        for (World world : worldList)
            if (world.getName().equalsIgnoreCase(worldName))
                return world;
        return null;
    }

    /**
     * Replaces placeholders in a string with values stored in a map. Placeholders should be given in the format {@code
     * %placeholder%}. Placeholders should be stored in lower case in the map, but may be in any case in the placeholder
     * itself.
     *
     * @param str          The string that contains the placeholders.
     * @param placeholders The map of placeholders to actual values
     * @return The string with all placeholders switched with the corresponding value in the map.
     */
    public static String fillPlaceholders(final String str, final Map<String, String> placeholders) {
        final Matcher matcher = PLACEHOLDER_REGEX.matcher(str);
        final StringBuffer sb = new StringBuffer();
        while (matcher.find()) matcher.appendReplacement(sb, placeholders.get(matcher.group(1).toLowerCase()));
        return matcher.appendTail(sb).toString();
    }

    public static String replaceWrittenLineBreaks(String s) {
        return
            SLASH_N_REGEX_DOUBLE.matcher(
                SLASH_N_REGEX_W_LOOKBEHIND.matcher(s
                ).replaceAll("\n")
            ).replaceAll("\\n");
    }

    public static String replaceLegacyColors(String s) {
        return
            LEGACY_COLOR_REGEX_DOUBLE.matcher(
                LEGACY_COLOR_REGEX_W_LOOKBEHIND.matcher(s
                ).replaceAll("\u00A7$1")
            ).replaceAll("&$1");
    }

    public static String replaceNewColors(String s) {
        return
            HEX_COLOR_REGEX.matcher(s
            ).replaceAll("\u00A7x\u00A7$1\u00A7$2\u00A7$3\u00A7$4\u00A7$5\u00A7$6");
    }

    public static String readableTime(long milliseconds) {
        int days, hours, minutes, seconds;
        seconds = (int) (milliseconds / 1000) % 60;
        minutes = (int) (milliseconds / (1000 * 60)) % 60;
        hours = (int) (milliseconds / (1000 * 60 * 60)) % 24;
        days = (int) (milliseconds / (1000 * 60 * 60 * 24));
        return Messages.READABLE_TIME.format(
            (days > 0 ? Messages.READABLE_TIME_WORD_DAYS.format(days) : ""),
            (hours > 0 ? Messages.READABLE_TIME_WORD_HOURS.format(hours) : ""),
            (minutes > 0 ? Messages.READABLE_TIME_WORD_MINUTES.format(minutes) : ""),
            (seconds > 0 ? Messages.READABLE_TIME_WORD_SECONDS.format(seconds) : ""));
    }

    public static boolean isDirEmpty(final Path directory) throws IOException {
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(directory)) {
            return !dirStream.iterator().hasNext();
        }
    }

    public static List<Pair<String, FileConfiguration>> getFileConfigFromFile(File[] files) {
        List<Pair<String, FileConfiguration>> configs = new ArrayList<>();
        for (File f : files)
            configs.add(new Pair<String, FileConfiguration>(
                f.getName().substring(0, f.getName().lastIndexOf(".")),
                YamlConfiguration.loadConfiguration(f)
            ));
        return configs;
    }

    public static class Pair<K, V> {
        public K key;
        public V value;

        Pair(K key, V value) {
            this.key = key;
            this.value = value;
        }

    }

    /**
     * If the current minecraft version has respawn anchors, this tells us if the event is an anchor spawn.
     * If the current version does NOT have anchors, this returns false.
     *
     * @param event PlayerRespawnEvent event to check.
     * @return Was this a 1.16+ respawn anchor spawn?
     */
    public static boolean isAnchorSpawn(PlayerRespawnEvent event) {return anchorSupport && event.isAnchorSpawn();}

    private static final boolean anchorSupport = PaperLib.getMinecraftVersion() >= 16;

    /**
     * Just lists out the items in a list. Doesn't say "and" or "or" at the end, its just a simple
     * comma separated list. Kinda like this: A, B, C, D
     *
     * @param items The items to list
     * @return The items as comma separated list.
     */
    public static String listText(List<String> items) {
        if (items == null || items.size() == 0) return null;
        if (items.size() == 1) return items.get(0);
        StringBuilder s = new StringBuilder(items.get(0));
        for (int i = 1; i < items.size(); i++)
            s.append(", ").append(items.get(i));
        return s.toString();
    }


    public static long uselessLong = 0; // Quite literally useless. Never used anywhere.

    public static String timeDifLog() { // Okay, that's a lie, it's used in this method.
        long time = System.currentTimeMillis(); // But this method is not used anywhere.
        long dif = time - uselessLong;
        uselessLong = time;
        return time + " " + dif;
    }
}
