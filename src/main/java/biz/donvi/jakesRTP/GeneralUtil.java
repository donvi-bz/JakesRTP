package biz.donvi.jakesRTP;

import org.apache.commons.lang.text.StrBuilder;
import org.bukkit.Location;

import java.text.DecimalFormat;

public final class GeneralUtil {


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
        DecimalFormat decimalFormat = decimalPlaces == 0 ? new DecimalFormat("0") :
                new DecimalFormat(new StrBuilder("0.").append(stringOf('#',decimalPlaces)).toString());
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

}
