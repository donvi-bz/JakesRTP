package biz.donvi.jakesRTP;

import org.apache.commons.lang.text.StrBuilder;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;

import java.text.DecimalFormat;
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

    public static String stringOf(char c, int times) {
        if (times <= 0) return "";
        if (times == 1) return String.valueOf(c);
        StringBuilder strb = new StringBuilder();
        for (int i = 0; i < times; i++) strb.append(c);
        return strb.toString();
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

}
