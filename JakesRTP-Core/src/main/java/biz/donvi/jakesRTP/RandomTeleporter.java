package biz.donvi.jakesRTP;

import biz.donvi.evenDistribution.RandomCords;
import io.papermc.lib.PaperLib;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;

import static biz.donvi.jakesRTP.PluginMain.infoLog;
import static biz.donvi.jakesRTP.PluginMain.plugin;

public class RandomTeleporter {

    final static String explicitPermPrefix = "jakesrtp.use.";

    private final ArrayList<RtpSettings> rtpSettings;

    public final boolean firstJoinRtp;
    public final RtpSettings firstJoinSettings;
    public final World firstJoinWorld;

    public final boolean onDeathRtp;
    public final boolean onDeathRespectBeds;
    public final boolean onDeathRequirePermission;
    public final RtpSettings onDeathSettings;
    public final World onDeathWorld;

    public final int asyncWaitTimeout;

    public final boolean /*Logging*/
            logRtpOnPlayerJoin,
            logRtpOnRespawn,
            logRtpOnCommand,
            logRtpOnForceCommand,
            logRtpForQueue;

    /**
     * Creating an instance of the RandomTeleporter object is required to be able to use the command.
     * On creation, all relevant parts of the config are loaded into memory.
     *
     * @param config The configurationSection that holds the relevant data for RTPing
     * @throws Exception A generic exception for any issue had when creating the object.
     *                   I have NOT made my own exceptions, but instead have written different messages.
     */
    public RandomTeleporter(ConfigurationSection config) throws Exception {
        // Modular settings:
        rtpSettings = new ArrayList<>();
        for (String key : config.getKeys(false))
            if (key.startsWith("random-teleport-settings"))
                try {
                    ConfigurationSection configSection = config.getConfigurationSection(key);
                    String configName = key.substring("random-teleport-settings".length() + 1);
                    if (configSection != null && configSection.getBoolean("enabled"))
                        rtpSettings.add(new RtpSettings(
                                configSection,
                                configName));
                    else infoLog("Not loading config " + configName + " since it is marked disabled.");
                } catch (NullPointerException | JrtpBaseException e) {
                    PluginMain.infoLog(
                            (e instanceof JrtpBaseException
                                    ? "Error: " + ((JrtpBaseException) e).getMessage() + '\n'
                                    : "") +
                            "Whoops! Something in the config wasn't right, " +
                            rtpSettings.size() + " configs have been loaded thus far.");
                }
        // Static settings:
        if (firstJoinRtp = config.getBoolean("rtp-on-first-join.enabled", false)) {
            firstJoinSettings = getRtpSettingsByName(config.getString("rtp-on-first-join.settings"));
            World world = PluginMain.plugin.getServer().getWorld(
                    Objects.requireNonNull(config.getString("rtp-on-first-join.world")));
            if (firstJoinSettings.getConfigWorlds().contains(world))
                firstJoinWorld = world;
            else throw new Exception("The RTP first join world is not an enabled world in the config's settings!");
        } else {
            firstJoinSettings = null;
            firstJoinWorld = null;
        }
        if (onDeathRtp = config.getBoolean("rtp-on-death.enabled", false)) {
            onDeathRespectBeds = config.getBoolean("rtp-on-death.respect-beds", true);
            onDeathSettings = getRtpSettingsByName(config.getString("rtp-on-death.settings"));
            onDeathRequirePermission = config.getBoolean("rtp-on-death.require-permission", true);
            World world = PluginMain.plugin.getServer().getWorld(
                    Objects.requireNonNull(config.getString("rtp-on-death.world")));
            if (onDeathSettings.getConfigWorlds().contains(world))
                onDeathWorld = world;
            else throw new Exception("The RTP first join world is not an enabled world in the config's settings!");
        } else {
            onDeathRespectBeds = false;
            onDeathRequirePermission = false;
            onDeathSettings = null;
            onDeathWorld = null;
        }
        if (config.getBoolean("location-cache-filler.enabled", true))
            asyncWaitTimeout = config.getInt("location-cache-filler.async-wait-timeout", 5);
        else
            asyncWaitTimeout = 1; //Yes a hard coded default. If set to 0 and accidentally used, there would be issues.
        //So much logging...
        logRtpOnPlayerJoin = config.getBoolean("logging.rtp-on-player-join", true);
        logRtpOnRespawn = config.getBoolean("logging.rtp-on-respawn", true);
        logRtpOnCommand = config.getBoolean("logging.rtp-on-command", true);
        logRtpOnForceCommand = config.getBoolean("logging.rtp-on-force-command", true);
        logRtpForQueue = config.getBoolean("logging.rtp-for-queue", false);
    }

    /* ================================================== *\
                    RtpSettings ← Getters
    \* ================================================== */

    /**
     * Getter for the ArrayList of RtpSettings. This contains all settings that are done per config sections.
     *
     * @return The ArrayList of RtpSettings.
     */
    public ArrayList<RtpSettings> getRtpSettings() { return rtpSettings; }

    /**
     * Gets the list of RtpSettings names.
     *
     * @return A list of RtpSettings names.
     */
    public ArrayList<String> getRtpSettingsNames() {
        ArrayList<String> rtpSettings = new ArrayList<>();
        for (RtpSettings rtpSetting : this.rtpSettings)
            rtpSettings.add(rtpSetting.name);
        return rtpSettings;
    }

    /**
     * Gets the {@code RtpSettings} that are being used by the given world. If more then one {@code RtpSettings}
     * objects are valid, the one with the highest priority will be returned.
     *
     * @param world World to get RTP settings for
     * @return The RtpSettings of that world
     * @throws NotPermittedException If the world does not exist.
     */
    public RtpSettings getRtpSettingsByWorld(World world) throws NotPermittedException {
        RtpSettings finSettings = null;
        for (RtpSettings settings : rtpSettings)
            for (World settingWorld : settings.getConfigWorlds())
                if (world.equals(settingWorld) && (
                        finSettings == null
                        || finSettings.priority < settings.priority)
                ) {
                    finSettings = settings;
                    break;
                }
        if (finSettings != null) return finSettings;
        else throw new NotPermittedException("RTP is not enabled in this world. ~ECW");
    }


    /**
     * Gets the RtpSettings object that has the given name (as defined in the config).
     *
     * @param name The name of the settings
     * @return The Rtp settings object with the given name
     * @throws JrtpBaseException If no settings have the given name
     */
    public RtpSettings getRtpSettingsByName(String name) throws JrtpBaseException {
        for (RtpSettings settings : rtpSettings)
            if (settings.name.equals(name))
                return settings;
        throw new JrtpBaseException("No RTP settings found with name " + name);
    }

    /**
     * Gets the RtpSettings that a specific player in a specific world should be using. This is intended to be
     * used for players running the rtp command, as it follows all rules that players are held to when rtp-ing.
     *
     * @param player The player whose information will be used to determine the relevant rtp settings
     * @return The RtpSettings for the player to use, normally for when they run the {@code /rtp} command.
     * @throws NotPermittedException If no settings can be used.
     */
    public RtpSettings getRtpSettingsByWorldForPlayer(Player player) throws NotPermittedException {
        RtpSettings finSettings = null;
        World playerWorld = player.getWorld();
        for (RtpSettings settings : rtpSettings)
            for (World settingWorld : settings.getConfigWorlds())
                //First, the world must be in the settings to become a candidate
                if (playerWorld.equals(settingWorld) &&
                    //Then we check if the settings are usable from the command
                    settings.commandEnabled &&
                    //Then we check the priority
                    (finSettings == null || finSettings.priority < settings.priority) &&
                    //Then we check if we require explicit perms
                    (!settings.requireExplicitPermission || player.hasPermission(explicitPermPrefix + settings.name))
                ) {
                    finSettings = settings;
                    break;
                }
        if (finSettings != null) return finSettings;
        else throw new NotPermittedException("RTP is not enabled in this world. ~ECP");
    }

    /* ================================================== *\
                    Rtp Locations ← Getters
    \* ================================================== */

    /**
     * This method acts as a bridge between this Minecraft specific class and my evenDistribution package
     * by calling the appropriate method from the package, and forwarding the relevant configuration
     * settings that have been saved in memory.
     *
     * @param rtpSettings The Rtp settings to use to get the random points
     * @return A random X and Z coordinate pair.
     * @throws Exception if a shape is not properly defined,
     *                   though realistic error checking beforehand should prevent this issue
     */
    private int[] getRtpXZ(RtpSettings rtpSettings) throws Exception {
        switch (rtpSettings.rtpRegionShape) {
            case SQUARE:
                if (rtpSettings.gaussianShrink == 0) return RandomCords.getRandXySquare(
                        rtpSettings.maxRadius,
                        rtpSettings.minRadius);
                else return RandomCords.getRandXySquare(
                        rtpSettings.maxRadius,
                        rtpSettings.minRadius,
                        rtpSettings.gaussianShrink,
                        rtpSettings.gaussianCenter);
            case CIRCLE:
                return RandomCords.getRandXyCircle(
                        rtpSettings.maxRadius,
                        rtpSettings.minRadius,
                        rtpSettings.gaussianShrink,
                        rtpSettings.gaussianCenter);
            case RECTANGLE:
                //return getRtpXzRectangle(); //This will get un-commented once I write a method for rectangles
            default:
                throw new Exception("RTP Region shape not properly defined.");
        }
    }

    /**
     * Creates the potential RTP location. If this location happens to be safe, is will be the exact location that
     * the player gets teleported to (though that is unlikely as the {@code y} is {@code 255} by default). <p>
     * This method differs from {@code getRtpXZ()} because it includes the offset and returns a {@code Location}
     * whereas {@code getRtpZX()} only gets the initial {@code x} and {@code z}, and returns a coordinate pair.
     *
     * @param callFromLoc A location representing where the call originated from. This is used to get either the world
     *                    spawn, or player location for the position offset
     * @param rtpSettings The relevant settings for RTP
     * @return The first location to check the safety of, which may end up being the final teleport location
     * @throws Exception Unlikely, but still possible.
     */
    @SuppressWarnings("ConstantConditions")
    private Location getPotentialRtpLocation(Location callFromLoc, RtpSettings rtpSettings) throws Exception {
        int[] xz = getRtpXZ(rtpSettings);
        int[] xzOffset;
        switch (rtpSettings.centerLocation) {
            case PLAYER_LOCATION:
                xzOffset = new int[]{
                        (int) callFromLoc.getX(),
                        (int) callFromLoc.getZ()};
                break;
            case WORLD_SPAWN:
                xzOffset = new int[]{
                        (int) callFromLoc.getWorld().getSpawnLocation().getX(),
                        (int) callFromLoc.getWorld().getSpawnLocation().getZ()};
                break;
            case PRESET_VALUE:
            default:
                xzOffset = new int[]{
                        rtpSettings.centerX,
                        rtpSettings.centerZ};
        }

        return new Location(
                callFromLoc.getWorld(),
                xz[0] + xzOffset[0],
                255,
                xz[1] + xzOffset[1]
        );
    }

    /**
     * Keeps getting potential teleport locations until one has been found.
     * A fail-safe is included to throw an exception if too many unsuccessful attempts have been made.
     *
     * @param player        The player to teleport. This is important as it lets us know which {@code RtpSettings} to
     *                      use, and in the case that the teleport is relative to the player, it also gives the
     *                      player's location.
     * @param force         Should we "forcefully" teleport the player? Forceful teleporting <em>bypasses</em> any
     *                      permission or command checks when teleporting, meaning that the player will teleport
     *                      regardless of whether they have permission to or not or whether the settings can be used
     *                      through a command.
     * @param takeFromQueue Should we attempt to take the location from the queue before finding one on the spot?
     *                      <p> - Set to {@code true} if all you care about is teleporting the player, as this method
     *                      will fall back to using a Location not from the queue if required.
     *                      <p> - Set to {@code false} if you are filling the queue,
     *                      or it is known that the queue is empty.
     * @return A random location that can be safely teleported to by a player.
     * @throws Exception
     */
    public Location getRtpLocation(Player player, boolean force, boolean takeFromQueue) throws Exception {
        if (force) return getRtpLocation( /*Type: Redirect*/
                getRtpSettingsByWorld(player.getWorld()),
                player.getLocation(),
                takeFromQueue);
        else return getRtpLocation( /*Type: Redirect*/
                getRtpSettingsByWorldForPlayer(player),
                player.getLocation(),
                takeFromQueue);
    }

    /**
     * Keeps getting potential teleport locations until one has been found.
     * A fail-safe is included to throw an exception if too many unsuccessful attempts have been made.
     * This method can bypass explicit permission checks.
     *
     * @param rtpSettings   The specific RtpSettings to get the location with.
     * @param callFromLoc   The location that the call originated from. Used to find the world spawn,
     *                      or player's current location.
     * @param takeFromQueue Should we attempt to take the location from the queue before finding one on the spot?
     *                      <p> - Set to {@code true} if all you care about is teleporting the player, as this method
     *                      will fall back to using a Location not from the queue if required.
     *                      <p> - Set to {@code false} if you are filling the queue, or it is known that the queue is empty.
     * @return A random location that can be safely teleported to by a player.
     * @throws Exception Only two points of this code are expected to be able to throw an exception:
     *                   getWorldRtpSettings() will throw an exception if the world is not RTP enabled.
     *                   getRtpXZ() will throw an exception if the rtp shape is not defined.
     */
    public Location getRtpLocation(final RtpSettings rtpSettings, Location callFromLoc,
                                   final boolean takeFromQueue
    ) throws Exception, SafeLocationFinder.PluginDisabledException {
        //Part 1: Force destination world if not current world
        if (rtpSettings.forceDestinationWorld && callFromLoc.getWorld() != rtpSettings.destinationWorld)
            callFromLoc = rtpSettings.destinationWorld.getSpawnLocation();

        //Part 2: Quick error checking
        if (!rtpSettings.getConfigWorlds().contains(callFromLoc.getWorld()))
            throw new NotPermittedException("RTP is not enabled in this world. ~ECG");

        //Part 3 option 1: The Queue Route.
        //If we want to take from the queue and the queue is enabled, go here.
        //TODO split this into two things:
        // First is if location caching is turned off
        // Second is if it can not be used because of a relative location. In this case we want to find a new pos async
        if (takeFromQueue && rtpSettings.useLocationQueue) {
            Location preselectedLocation = rtpSettings.getLocationQueue(callFromLoc.getWorld()).poll();
            if (preselectedLocation != null) {
                //<editor-fold desc="runTaskLaterAsynchronously(locFinderRunnable.syncNotify())">
                plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin,
                        //This is all just to tell the LocationCacheFiller to start back up
                        new Runnable() {
                            @Override
                            public void run() {
                                PluginMain.locFinderRunnable.syncNotify();
                            }
                        }, 100);
                //</editor-fold>
                return preselectedLocation;
            } else
                return getRtpLocation(rtpSettings, callFromLoc, false); /*Type: Recursive*/
        }

        //Part 3 option 2: The Normal Route.
        //If we need to find a NEW location (meaning we can't use the queue), go here.
        else {
            Location potentialRtpLocation;
            int randAttemptCount = 0;
            do {
                potentialRtpLocation = getPotentialRtpLocation(callFromLoc, rtpSettings);
                if (randAttemptCount++ > rtpSettings.maxAttempts)
                    throw new JrtpBaseException("Too many failed attempts.");
            } while (
                    Bukkit.isPrimaryThread() ?
                            !new SafeLocationFinderBukkitThread(
                                    potentialRtpLocation,
                                    rtpSettings.checkRadiusXZ,
                                    rtpSettings.checkRadiusVert,
                                    rtpSettings.lowBound,
                                    rtpSettings.highBound
                            ).tryAndMakeSafe(rtpSettings.checkProfile) :
                            !new SafeLocationFinderOtherThread(
                                    potentialRtpLocation,
                                    rtpSettings.checkRadiusXZ,
                                    rtpSettings.checkRadiusVert,
                                    rtpSettings.lowBound,
                                    rtpSettings.highBound,
                                    asyncWaitTimeout
                            ).tryAndMakeSafe(rtpSettings.checkProfile));
            return potentialRtpLocation;
        }

    }

    /* ================================================== *\
                    Misc ← Workers
    \* ================================================== */

    /**
     * This will fill up the queue of safe teleport locations for the specified {@code RtpSettings} and {@code World}
     * combination, waiting (though only if we are not on the main thread) a predetermined amount of time between
     * finding each location.
     *
     * @param settings The rtpSettings to use for the world
     * @param world    The world to find the locations in. This MUST be an enabled world in the given settings.
     * @return The number of locations added to the queue. (The result can be ignored if deemed unnecessary)
     * @throws NotPermittedException Should not realistically get thrown, but may occur if the world is not
     *                               enabled in the settings.
     */
    public int fillQueue(RtpSettings settings, World world) throws JrtpBaseException, SafeLocationFinder.PluginDisabledException {
        try {
            int changesMade = 0;
            for (Queue<Location> locationQueue = settings.getLocationQueue(world);
                 locationQueue.size() < settings.cacheLocationCount;
                 changesMade++
            ) {
                PluginMain.locFinderRunnable.waitIfNonMainThread();

                long startTime = System.currentTimeMillis();

                Location rtpLocation = getRtpLocation(settings, world.getSpawnLocation(), false);
                locationQueue.add(rtpLocation);

                long endTime = System.currentTimeMillis();
                if (logRtpForQueue) infoLog(
                        "Rtp-for-queue triggered. No player will be teleported." +
                        " Location: " + GeneralUtil.locationAsString(rtpLocation, 1, false) +
                        " Time: " + (endTime - startTime) + " ms.");
            }
            return changesMade;
        } catch (SafeLocationFinder.PluginDisabledException pluginDisabledException) {
            throw pluginDisabledException;
        } catch (Exception exception) {
            if (exception instanceof JrtpBaseException) throw (JrtpBaseException) exception;
            else exception.printStackTrace();
            return 0;
        }
    }

}