package biz.donvi.jakesRTP;

import biz.donvi.jakesRTP.GeneralUtil.Pair;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.logging.Level;

import static biz.donvi.jakesRTP.JakesRtpPlugin.*;

public class RandomTeleporter {

    final static String EXPLICIT_PERM_PREFIX = "jakesrtp.use.";

    // Dynamic settings
    public final  Map<String, DistributionSettings> distributionSettings;
    private final ArrayList<RtpSettings>            rtpSettings;

    // First join settings
    public final boolean     firstJoinRtp;
    public final RtpSettings firstJoinSettings;
    // On death settings
    public final boolean     onDeathRtp;
    public final boolean     onDeathRespectBeds;
    public final boolean     onDeathRequirePermission;
    public final RtpSettings onDeathSettings;
    // Misc settings
    public final boolean     queueEnabled;
    public final int         asyncWaitTimeout;

    // Logging settings
    public final boolean
        logRtpOnPlayerJoin,
        logRtpOnRespawn,
        logRtpOnCommand,
        logRtpOnForceCommand,
        logRtpForQueue;

    /**
     * Creating an instance of the RandomTeleporter object is required to be able to use the command.
     * On creation, all relevant parts of the config are loaded into memory.
     *
     * @throws Exception A generic exception for any issue had when creating the object.
     *                   I have NOT made my own exceptions, but instead have written different messages.
     */
    public RandomTeleporter(
        ConfigurationSection globalConfig,
        List<Pair<String, FileConfiguration>> rtpSections,
        List<Pair<String, FileConfiguration>> distributions
    ) throws Exception {
        // Distributions:
        this.distributionSettings = new HashMap<>();
        for (Pair<String, FileConfiguration> item : distributions)
            try {
                distributionSettings.put(item.key, new DistributionSettings(item.value));
            } catch (JrtpBaseException.ConfigurationException e) {
                log(Level.WARNING, "Could not load distribution settings " + item.key);
                e.printStackTrace();
            }
        for (Map.Entry<String, DistributionSettings> en : worldBorderPluginHook.generateDistributions().entrySet()) {
            distributionSettings.put(en.getKey(), en.getValue());
        }
        // Modular settings:
        this.rtpSettings = new ArrayList<>();
        for (Pair<String, FileConfiguration> item : rtpSections)
            try {
                if (item.value.getBoolean("enabled"))
                    this.rtpSettings.add(new RtpSettings(item.value, item.key, distributionSettings));
                else infoLog("Not loading config " + item.key + " since it is marked disabled.");
            } catch (NullPointerException | JrtpBaseException e) {
                JakesRtpPlugin.infoLog(
                    (e instanceof JrtpBaseException ? "Error: " + e.getMessage() + '\n' : "") +
                    "Whoops! Something in the config wasn't right, " +
                    this.rtpSettings.size() + " configs have been loaded thus far.");
            }
        // Static settings:
        if (firstJoinRtp = globalConfig.getBoolean("rtp-on-first-join.enabled", false)) {
            firstJoinSettings = getRtpSettingsByName(globalConfig.getString("rtp-on-first-join.settings"));
            World world = JakesRtpPlugin.plugin.getServer().getWorld(
                Objects.requireNonNull(globalConfig.getString("rtp-on-first-join.world")));
        } else {
            firstJoinSettings = null;
        }
        if (onDeathRtp = globalConfig.getBoolean("rtp-on-death.enabled", false)) {
            onDeathRespectBeds = globalConfig.getBoolean("rtp-on-death.respect-beds", true);
            onDeathSettings = getRtpSettingsByName(globalConfig.getString("rtp-on-death.settings"));
            onDeathRequirePermission = globalConfig.getBoolean("rtp-on-death.require-permission", true);
            World world = JakesRtpPlugin.plugin.getServer().getWorld(
                Objects.requireNonNull(globalConfig.getString("rtp-on-death.world")));
        } else {
            onDeathRespectBeds = false;
            onDeathRequirePermission = false;
            onDeathSettings = null;
        }
        queueEnabled = globalConfig.getBoolean("location-cache-filler.enabled", true);
        if (queueEnabled)
            asyncWaitTimeout = globalConfig.getInt("location-cache-filler.async-wait-timeout", 5);
        else
            asyncWaitTimeout = 1; //Yes a hard coded default. If set to 0 and accidentally used, there would be issues.
        //So much logging...
        logRtpOnPlayerJoin = globalConfig.getBoolean("logging.rtp-on-player-join", true);
        logRtpOnRespawn = globalConfig.getBoolean("logging.rtp-on-respawn", true);
        logRtpOnCommand = globalConfig.getBoolean("logging.rtp-on-command", true);
        logRtpOnForceCommand = globalConfig.getBoolean("logging.rtp-on-force-command", true);
        logRtpForQueue = globalConfig.getBoolean("logging.rtp-for-queue", false);
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
     * Gets the names of all {@code rtpSettings} usable by the given player.
     *
     * @param player The player to check for settings with.
     * @return All the names of rtpSettings that the given player can use.
     */
    public ArrayList<String> getRtpSettingsNamesForPlayer(Player player) {
        ArrayList<String> rtpSettings = new ArrayList<>();
        for (RtpSettings rtpSetting : this.rtpSettings)
            if (rtpSetting.commandEnabled && (
                !rtpSetting.requireExplicitPermission ||
                player.hasPermission(EXPLICIT_PERM_PREFIX + rtpSetting.name))
            ) rtpSettings.add(rtpSetting.name);
        return rtpSettings;
    }

    /**
     * Gets the {@code RtpSettings} that are being used by the given world. If more then one {@code RtpSettings}
     * objects are valid, the one with the highest priority will be returned.
     *
     * @param world World to get RTP settings for
     * @return The RtpSettings of that world
     * @throws JrtpBaseException.NotPermittedException If the world does not exist.
     */
    public RtpSettings getRtpSettingsByWorld(World world) throws JrtpBaseException.NotPermittedException {
        RtpSettings finSettings = null;
        for (RtpSettings settings : rtpSettings)
            for (World settingWorld : settings.callFromWorlds)
                if (world.equals(settingWorld) &&
                    (finSettings == null || finSettings.priority < settings.priority)
                ) {
                    finSettings = settings;
                    break;
                }
        if (finSettings != null) return finSettings;
        else throw new JrtpBaseException.NotPermittedException(Messages.NP_R_NOT_ENABLED.format("~ECW"));
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
        throw new JrtpBaseException(Messages.NP_R_NO_RTPSETTINGS_NAME.format(name));
    }

    /**
     * Gets the RtpSettings that a specific player in a specific world should be using. This is intended to be
     * used for players running the rtp command, as it follows all rules that players are held to when rtp-ing.
     *
     * @param player The player whose information will be used to determine the relevant rtp settings
     * @return The RtpSettings for the player to use, normally for when they run the {@code /rtp} command.
     * @throws JrtpBaseException.NotPermittedException If no settings can be used.
     */
    public RtpSettings getRtpSettingsByWorldForPlayer(Player player) throws JrtpBaseException.NotPermittedException {
        RtpSettings finSettings = null;
        World playerWorld = player.getWorld();
        for (RtpSettings settings : rtpSettings)
            for (World settingWorld : settings.callFromWorlds)
                //First, the world must be in the settings to become a candidate
                if (playerWorld.equals(settingWorld) &&
                    //Then we check if the settings are usable from the command
                    settings.commandEnabled &&
                    //Then we check the priority
                    (finSettings == null || finSettings.priority < settings.priority) &&
                    //Then we check if we require explicit perms
                    (!settings.requireExplicitPermission || player.hasPermission(EXPLICIT_PERM_PREFIX + settings.name))
                ) {
                    finSettings = settings;
                    break;
                }
        if (finSettings != null) return finSettings;
        else throw new JrtpBaseException.NotPermittedException(Messages.NP_R_NOT_ENABLED.format("~ECP"));
    }

    /**
     * Gets the RtpSettings object that has the given name (as defined in the config) IF AND ONLY IF the settings with
     * the given name have property {@code commandEnabled} set to {@code true}, and the player either has the necessary
     * explicit permission for the settings, or the settings does not require one.
     *
     * @param player The player to find the settings for. Only settings this player can use will be returned.
     * @param name   The name of the settings to find.
     * @return The {@code rtpSettings} object with the matching name. If no valid {@code rtpSettings} is found, an
     * an exception will be thrown.
     * @throws JrtpBaseException.NotPermittedException if no valid {@code rtpSettings} object is found.
     */
    public RtpSettings getRtpSettingsByNameForPlayer(Player player, String name)
    throws JrtpBaseException.NotPermittedException {
        for (RtpSettings settings : rtpSettings)
            // First check if this settings can be called by a player command
            if (settings.commandEnabled &&
                // Then we need the names to match
                settings.name.equalsIgnoreCase(name) &&
                //Then we check if we require explicit perms
                (!settings.requireExplicitPermission || player.hasPermission(EXPLICIT_PERM_PREFIX + settings.name)))
                // Note: We never check priority because the name must be unique
                return settings;
        throw new JrtpBaseException.NotPermittedException(Messages.NP_R_NO_RTPSETTINGS_NAME_FOR_PLAYER.format(name));
    }
    /* ================================================== *\
                    Rtp Locations ← Getters
    \* ================================================== */

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
     */
    @SuppressWarnings("ConstantConditions")
    private Location getPotentialRtpLocation(Location callFromLoc, RtpSettings rtpSettings) throws JrtpBaseException {
        Location potentialLocation;
        int[] xz, xzOffset;

        switch (rtpSettings.distribution.center) {
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
                    rtpSettings.distribution.centerX,
                    rtpSettings.distribution.centerZ};
        }
        int attempts = 0;
        do {
            xz = rtpSettings.distribution.shape.getCords();
            potentialLocation = new Location(
                callFromLoc.getWorld(),
                xz[0] + xzOffset[0],
                255,
                xz[1] + xzOffset[1]
            );
            if (++attempts > 100) // TODO maybe set this as a static variable?
                throw new JrtpBaseException(Messages.NP_R_TOO_MANY_FAILED_ATTEMPTS.format() + " ~GP-RTP-L");
        } while (!isInWorldBorder(potentialLocation)); // Yeah, re-guessing, I know :(
        return potentialLocation;
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
     *                      <p> - Set to {@code false} if you are filling the queue, or it is known that the queue is
     *                      empty.
     * @return A random location that can be safely teleported to by a player.
     * @throws Exception Only two points of this code are expected to be able to throw an exception:
     *                   getWorldRtpSettings() will throw an exception if the world is not RTP enabled.
     *                   getRtpXZ() will throw an exception if the rtp shape is not defined.
     */
    public Location getRtpLocation(final RtpSettings rtpSettings, Location callFromLoc, final boolean takeFromQueue)
    throws JrtpBaseException, JrtpBaseException.PluginDisabledException {
        // Part 1: Force destination world if not current world
        if (callFromLoc.getWorld() != rtpSettings.landingWorld)
            callFromLoc.setWorld(rtpSettings.landingWorld);

        // Part 2: Quick error checking (world in world list)
        if (!rtpSettings.callFromWorlds.contains(callFromLoc.getWorld()))
            throw new JrtpBaseException.NotPermittedException(Messages.NP_R_NOT_ENABLED.format("~ECG"));

        // Part 3 option 1: The Queue Route.
        // If we want to take from the queue and the queue is enabled, go here.
        if (queueEnabled && takeFromQueue && rtpSettings.canUseLocQueue) {
            Location preselectedLocation = rtpSettings.locationQueue.poll();
            if (preselectedLocation != null) {
                plugin.getServer().getScheduler() // Tell queue to refill soon
                      .runTaskLaterAsynchronously(plugin, () -> locFinderRunnable.syncNotify(), 100);
                return preselectedLocation;
            }
        }

        // Part 3 option 2: The Normal Route.
        // If we need to find a NEW location (meaning we can't use the queue), go here.
        Location potentialRtpLocation;
        int randAttemptCount = 0;
        do {
            potentialRtpLocation = getPotentialRtpLocation(callFromLoc, rtpSettings);
            if (randAttemptCount++ > rtpSettings.maxAttempts)
                throw new JrtpBaseException(Messages.NP_R_TOO_MANY_FAILED_ATTEMPTS.format());
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

    /* ================================================== *\
                    Misc ← Workers
    \* ================================================== */

    private boolean isInWorldBorder(Location loc) {
        return (worldBorderPluginHook.isInside(loc));
    }

    /**
     * This will fill up the queue of safe teleport locations for the specified {@code RtpSettings} and {@code World}
     * combination, waiting (though only if we are not on the main thread) a predetermined amount of time between
     * finding each location.
     *
     * @param settings The rtpSettings to use for the world
     * @return The number of locations added to the queue. (The result can be ignored if deemed unnecessary)
     * @throws JrtpBaseException.NotPermittedException Should not realistically get thrown, but may occur if the
     *                                                 world is not
     *                                                 enabled in the settings.
     */
    public int fillQueue(RtpSettings settings)
    throws JrtpBaseException, JrtpBaseException.PluginDisabledException {
        try {
            int changesMade = 0;
            while (settings.locationQueue.size() < settings.cacheLocationCount) {
                JakesRtpPlugin.locFinderRunnable.waitIfNonMainThread();

                long startTime = System.currentTimeMillis();

                Location rtpLocation = getRtpLocation(settings, settings.landingWorld.getSpawnLocation(), false);
                settings.locationQueue.add(rtpLocation);

                long endTime = System.currentTimeMillis();
                if (logRtpForQueue) infoLog(
                    "Rtp-for-queue triggered. No player will be teleported." +
                    " Location: " + GeneralUtil.locationAsString(rtpLocation, 1, false) +
                    " Time: " + (endTime - startTime) + " ms.");

                changesMade++;
            }
            return changesMade;
        } catch (JrtpBaseException.PluginDisabledException pluginDisabledException) {
            throw pluginDisabledException;
        } catch (Exception exception) {
            if (exception instanceof JrtpBaseException) throw (JrtpBaseException) exception;
            else exception.printStackTrace();
            return 0;
        }
    }

}