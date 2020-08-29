package biz.donvi.jakesRTP;

import io.papermc.lib.PaperLib;
import org.apache.commons.lang.text.StrBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.util.List;

import static biz.donvi.jakesRTP.PluginMain.infoLog;

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
                new DecimalFormat(new StrBuilder("0.").append(stringOf('#', decimalPlaces)).toString());
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
     * Conveniently teleports a given player with a given rtpSettings in a given world, logging and timing if requested,
     * and can be run sync or async.
     *
     * @param playerToTp The player to teleport.
     * @param randomTeleporter The random teleporter to supply the random location.
     * @param rtpSettings The rtpSettings to use.
     * @param callFromLoc The location that the player called RTP from (used to decide where they land).
     * @param takeFromQueue Should a location be taken from the queue (or generated on the spot).
     * @param timed Should we time this?
     * @param async Should we teleport the player asynchronously?
     * @param doLog Should we log this teleport to console?
     * @param logMsgStart The first line of the log message.
     * @return The location that the player was teleported to.
     * @throws Exception If something goes wrong.
     */
    public static Location teleportRandomlyWrapper( /*TODO use this more often*/
            Player playerToTp,
            RandomTeleporter randomTeleporter, RtpSettings rtpSettings, Location callFromLoc, boolean takeFromQueue,
            boolean timed, boolean async, boolean doLog, String logMsgStart) throws Exception {
        long startTime = timed ? System.currentTimeMillis() : 0;
        Location rtpLocation =
                randomTeleporter.getRtpLocation(
                        rtpSettings,
                        callFromLoc,
                        true);
        //TODO clean up message portion
        if (async) {
            PaperLib.teleportAsync(playerToTp, rtpLocation).thenAccept(teleported -> {
                long endTime = timed ? System.currentTimeMillis() : 0;
                if (doLog) infoLog(
                        logMsgStart +
                        " Teleported player " + playerToTp.getName() +
                        " to " + locationAsString(rtpLocation, 1, false) +
                        " taking " + (timed ? endTime - startTime : "N/A") + " ms.");
            });
        } else if (Bukkit.isPrimaryThread()) {
            playerToTp.teleport(rtpLocation);
            long endTime = timed ? System.currentTimeMillis() : 0;
            if (doLog) infoLog(
                    logMsgStart +
                    " Teleported player " + playerToTp.getName() +
                    " to " + locationAsString(rtpLocation, 1, false) +
                    " taking " + (timed ? endTime - startTime : "N/A") + " ms.");
        } else {
            playerToTp.getServer().getScheduler().runTask(PluginMain.plugin, () -> {
                playerToTp.teleport(rtpLocation);
                long endTime = timed ? System.currentTimeMillis() : 0;
                if (doLog) infoLog(
                        logMsgStart +
                        " Teleported player " + playerToTp.getName() +
                        " to " + locationAsString(rtpLocation, 1, false) +
                        " taking " + (timed ? endTime - startTime : "N/A") + " ms.");
            });
        }
        return rtpLocation;
    }

}
