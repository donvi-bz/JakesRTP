package biz.donvi.jakesRTP;

import io.papermc.lib.PaperLib;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import static biz.donvi.jakesRTP.GeneralUtil.fillPlaceholders;
import static biz.donvi.jakesRTP.GeneralUtil.locationAsString;
import static biz.donvi.jakesRTP.PluginMain.infoLog;

public class RandomTeleportAction {

    public final RandomTeleporter randomTeleporter;
    public final RtpSettings rtpSettings;
    public final Location callFromLoc;
    public final boolean takeFromQueue;
    public final boolean timed;
    public final boolean log;
    public final String logMessage;

    private final Map<String, String> placeholders = new HashMap<>();

    private boolean used = false;
    private boolean completed = false;
    private long timeStart;
    private long timeEnd;
    private Player player;
    private Location landingLoc;

    public RandomTeleportAction(RandomTeleporter randomTeleporter, RtpSettings rtpSettings, Location callFromLoc,
                                boolean takeFromQueue, boolean timed, boolean log, String logMessage) {
        this.randomTeleporter = randomTeleporter;
        this.rtpSettings = rtpSettings;
        this.callFromLoc = callFromLoc;
        this.takeFromQueue = takeFromQueue;
        this.timed = timed;
        this.log = log;
        this.logMessage = logMessage;
    }


    public RandomTeleportAction teleportSync(Player player) throws Exception {
        SafeLocationUtils.requireMainThread();
        preTeleport(player);
        player.teleport(landingLoc);
        postTeleport(true);
        return this;
    }

    public RandomTeleportAction teleportSyncNonPrimaryThread(Player player) throws Exception {
        preTeleport(player);
        player.getServer().getScheduler().runTask(PluginMain.plugin, () -> {
            player.teleport(landingLoc);
            postTeleport(true);
        });
        return this;
    }

    public RandomTeleportAction teleportAsync(Player player) throws Exception {
        preTeleport(player);
        PaperLib.teleportAsync(player, landingLoc).thenAccept(this::postTeleport);
        return this;
    }

    public Location requestLocation() throws Exception {
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

    private void preTeleport(Player player) throws Exception {
        if (used) throw new RandomTeleportActionAlreadyUsedException();
        else used = true;
        if (timed) timeStart = System.currentTimeMillis();
        landingLoc = randomTeleporter.getRtpLocation(
                rtpSettings,
                callFromLoc,
                takeFromQueue);
        this.player = player;
        //<editor-fold desc="setPlaceholders();">
        if (rtpSettings.commandsToRun.length != 0) {
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

        if (rtpSettings.commandsToRun.length != 0)
            for (String command : rtpSettings.commandsToRun)
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), fillPlaceholders(command, placeholders));
    }

    static class RandomTeleportActionAlreadyUsedException extends RuntimeException {}

    static class RandomTeleportActionNotYetUsedException extends RuntimeException {}
}
