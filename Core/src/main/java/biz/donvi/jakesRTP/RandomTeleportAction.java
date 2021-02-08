package biz.donvi.jakesRTP;

import io.papermc.lib.PaperLib;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static biz.donvi.jakesRTP.GeneralUtil.fillPlaceholders;
import static biz.donvi.jakesRTP.GeneralUtil.locationAsString;
import static biz.donvi.jakesRTP.JakesRtpPlugin.infoLog;

/**
 * Class used for interfacing with the random teleport functionality. This class was made majority for internal use,
 * though it could possibly be used from other plugins if they can acquire the random teleporter and an rtpSettings.
 */
public class RandomTeleportAction {

    public final RandomTeleporter randomTeleporter;
    public final RtpProfile       rtpProfile;
    public final Location         callFromLoc;
    public final boolean          takeFromQueue;
    public final boolean          timed;
    public final boolean          log;
    public final String           logMessage;

    private final Map<String, String> placeholders = new HashMap<>();

    private boolean  used      = false;
    private boolean  completed = false;
    private long     timeStart;
    private long     timeEnd;
    private Player   player;
    private Location landingLoc;

    /**
     * Creates a new RandomTeleporterAction. Creating the action does nothing on its own, when you are ready to use
     * the action, use one of the four following methods:
     * {@link #teleportSync(Player) teleportSync},
     * {@link #teleportSyncNonPrimaryThread(Player) teleportSyncNonPrimaryThread},
     * {@link #teleportAsync(Player) teleportAsync},
     * or if you just want the location, use {@link #requestLocation()} (which does not require a player).
     *
     * @param randomTeleporter The randomTeleporter instance that does the heavy lifting.
     * @param rtpProfile      The settings to respect while finding the location.
     * @param callFromLoc      The initial location.
     * @param takeFromQueue    Take a location from the queue (if possible).
     * @param timed            Should we time this?
     * @param log              Should we log this teleport to console?
     * @param logMessage       A prefix to the standard log message. Helpful for which bit of code called this.
     */
    public RandomTeleportAction(
        RandomTeleporter randomTeleporter, RtpProfile rtpProfile, Location callFromLoc,
        boolean takeFromQueue, boolean timed, boolean log, String logMessage
    ) {
        this.randomTeleporter = randomTeleporter;
        this.rtpProfile = rtpProfile;
        this.callFromLoc = callFromLoc;
        this.takeFromQueue = takeFromQueue;
        this.timed = timed;
        this.log = log;
        this.logMessage = logMessage;
    }

    /**
     * Teleports a player <b>synchronously</b> on the main thread. This method should <b> only be called from the main
     * thread</b>, and will throw an exception if it is not. Use {@link #teleportSyncNonPrimaryThread(Player)} if you
     * need to teleport the player synchronously but are not on the main thread.
     *
     * @param player The player to teleport.
     * @return A reference to this object.
     * @throws JrtpBaseException If anything goes wrong...
     */
    public RandomTeleportAction teleportSync(Player player) throws JrtpBaseException {
        SafeLocationUtils.requireMainThread();
        preTeleport(player);
        player.teleport(landingLoc);
        postTeleport(true);
        return this;
    }

    /**
     * Teleports a player <b>synchronously</b> on the main thread. This method <b> can be called from any thread</b>.
     *
     * @param player The player to teleport.
     * @return A reference to this object.
     * @throws JrtpBaseException If anything goes wrong...
     */
    public RandomTeleportAction teleportSyncNonPrimaryThread(Player player) throws JrtpBaseException {
        preTeleport(player);
        player.getServer().getScheduler().runTask(JakesRtpPlugin.plugin, () -> {
            player.teleport(landingLoc);
            postTeleport(true);
        });
        return this;
    }

    /**
     * Teleports a player <b>asynchronously</b> with the help of Paper. This is generally the preferred method to use,
     * though there are times when the other methods are required instead.
     *
     * @param player The player to teleport.
     * @return A reference to this object.
     * @throws JrtpBaseException If anything goes wrong...
     */
    public RandomTeleportAction teleportAsync(Player player) throws JrtpBaseException {
        preTeleport(player);
        PaperLib.teleportAsync(player, landingLoc).thenAccept(this::postTeleport);
        return this;
    }

    /**
     * For when you just want a location and don't want to teleport anyone. This essentially allows you to use the
     * <code>randomTeleporter</code> as a super efficient random-safe-location generator.
     *
     * @return A random location generated using the given <code>rtpSettings</code>
     * @throws JrtpBaseException If anything goes wrong...
     */
    public Location requestLocation() throws JrtpBaseException {
        preTeleport(null);
        completed = true;
        if (timed) timeEnd = System.currentTimeMillis();
        if (log) infoLog(
            logMessage +
            " Generated location: " + locationAsString(landingLoc, 1, false) +
            " taking " + (timed ? timeEnd - timeStart : "N/A") + " ms.");
        return landingLoc;
    }

    //<editor-fold desc="======== Getters ========">
    public boolean isUsed() {return used;}

    public boolean isCompleted() { return completed; }

    public long getTimeStarted() {
        if (!used) throw new RandomTeleportActionNotYetUsedException();
        return timeStart;
    }

    public long getTimeEnded() {
        if (!used) throw new RandomTeleportActionNotYetUsedException();
        return timeEnd;
    }

    public Player getPlayerTeleported() {
        if (!used) throw new RandomTeleportActionNotYetUsedException();
        return player;
    }

    public Location getLandingLoc() {
        if (!used) throw new RandomTeleportActionNotYetUsedException();
        return landingLoc;
    }
    //</editor-fold>

    /**
     * This is everything that has to be done <i>before</i> the actual teleport happens. Mainly getting the point,
     * and filling in placeholders for user commands.
     *
     * @param player The player to get the random location for.
     * @throws JrtpBaseException If something goes wrong...
     */
    private void preTeleport(Player player) throws JrtpBaseException {
        if (used) throw new RandomTeleportActionAlreadyUsedException();
        else used = true;
        if (timed) timeStart = System.currentTimeMillis();
        landingLoc = randomTeleporter.getRtpLocation(
            rtpProfile,
            callFromLoc,
            takeFromQueue);
        this.player = player;
        //<editor-fold desc="setPlaceholders();">
        if (rtpProfile.commandsToRun.length != 0) {
            placeholders.put("location", locationAsString(landingLoc, 1, false));
            placeholders.put("world", Objects.requireNonNull(landingLoc.getWorld()).getName());
            placeholders.put("x", String.valueOf(landingLoc.getBlockX()));
            placeholders.put("y", String.valueOf(landingLoc.getBlockY()));
            placeholders.put("z", String.valueOf(landingLoc.getBlockZ()));
            if (player == null) return; //Everything from this point on NEEDS a player.
            placeholders.put("player", player.getName());
            placeholders.put("player_display_name", player.getDisplayName());
            Location playerLoc = player.getLocation();
            placeholders.put("location_old", locationAsString(playerLoc, 1, false));
            placeholders.put("world_old", Objects.requireNonNull(playerLoc.getWorld()).getName());
            placeholders.put("x_old", String.valueOf(playerLoc.getBlockX()));
            placeholders.put("y_old", String.valueOf(playerLoc.getBlockY()));
            placeholders.put("z_old", String.valueOf(playerLoc.getBlockZ()));
        }
        //</editor-fold>
    }

    /**
     * This is everything that needs to be done <i>after</i> the actual teleport happens. Mainly logging that it
     * happened, and potentially running commands.
     *
     * @param teleported If the teleport succeeded or not.
     */
    private void postTeleport(boolean teleported) {
        completed = true;
        if (timed) timeEnd = System.currentTimeMillis();
        if (log)
            if (teleported) infoLog(
                logMessage +
                " Teleported player " + player.getName() +
                " to " + locationAsString(landingLoc, 1, false) +
                " taking " + (timed ? timeEnd - timeStart : "N/A") + " ms.");
            else infoLog(
                logMessage +
                "Player did not teleport.");
        rtpCount++;
        if (rtpProfile.commandsToRun.length != 0)
            for (String command : rtpProfile.commandsToRun)
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), fillPlaceholders(command, placeholders));
    }

    static class RandomTeleportActionAlreadyUsedException extends RuntimeException {}

    static class RandomTeleportActionNotYetUsedException extends RuntimeException {}


    // For metrics
    private static int rtpCount = 0;

    public static int getRtpCount() {return rtpCount;}

    public static void clearRtpCount() {rtpCount = 0;}

    public static int getAndClearRtpCount(){
        int count = rtpCount;
        rtpCount = 0;
        return count;
    }
}
