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
import static biz.donvi.jakesRTP.MessageStyles.DebugDisplayLines.*;
import static biz.donvi.jakesRTP.MessageStyles.enabledOrDisabled;

public class RandomTeleporter {

    final static String EXPLICIT_PERM_PREFIX = "jakesrtp.use.";

    // The warmup thing...
    // A player can only be waiting for one rtp, regardless of how many they call.
    // The int stored is the id of the task to cancel it.
    final Map<UUID, Integer> playersInWarmup = new HashMap<>();

    // Dynamic settings
    public final  Map<String, DistributionSettings> distributionSettings;
    private final ArrayList<RtpProfile>             rtpSettings;

    //<editor-fold desc="'Static' Settings (final fields)">
    // First join settings
    public final boolean    firstJoinRtp;
    public final RtpProfile firstJoinSettings;
    // On death settings
    public final boolean    onDeathRtp;
    public final boolean    onDeathRespectBeds;
    public final boolean    onDeathRespectAnchors;
    public final boolean    onDeathRequirePermission;
    public final RtpProfile onDeathSettings;
    // Misc settings
    public final boolean    queueEnabled;
    public final int        asyncWaitTimeout;

    // Logging settings
    public final boolean
        logRtpOnPlayerJoin,
        logRtpOnRespawn,
        logRtpOnCommand,
        logRtpOnForceCommand,
        logRtpForQueue;
    //</editor-fold>

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
        boolean[] loaded = new boolean[rtpSections.size()];
        boolean allLoaded, oneLoaded;
        do {
            allLoaded = true;
            oneLoaded = false;
            // Loading begin
            for (int i = 0; i < rtpSections.size(); i++) {
                if (loaded[i]) continue;
                Pair<String, FileConfiguration> settingsFile = rtpSections.get(i);
                try {
                    if (settingsFile.value.getBoolean("enabled")) {
                        RtpProfile defaultsFrom;
                        try {
                            String getDefaultsFrom = settingsFile.value.getString("load-from", null);
                            defaultsFrom = getDefaultsFrom == null
                                ? RtpProfile.DEFAULT_SETTINGS
                                : getRtpSettingsByName(getDefaultsFrom);
                        } catch (JrtpBaseException e) {continue;}
                        this.rtpSettings.add(
                            new RtpProfile(
                                settingsFile.value,
                                settingsFile.key,
                                distributionSettings,
                                defaultsFrom
                            )
                        );
                        loaded[i] = oneLoaded = true; // Mark this one as good
                    } else {
                        loaded[i] = true;
                        infoLog("Not loading config " + settingsFile.key + " since it is marked disabled.");
                    }
                } catch (NullPointerException | JrtpBaseException e) {
                    log(Level.WARNING,
                        (e instanceof JrtpBaseException ? "Error: " + e.getMessage() + '\n' : "") +
                        "Whoops! Something in the config wasn't right, " +
                        this.rtpSettings.size() + " configs have been loaded thus far.");
                }
            }
            // Loading end
            for (boolean b : loaded)
                if (!b) {
                    allLoaded = false;
                    break;
                }
        } while (oneLoaded && !allLoaded);
        if (!allLoaded) plugin.getLogger().log(
            Level.WARNING, "One or more settings were not loaded due to a missing rtpSettings dependency. " +
                           "Make sure that at least one rtpSettings file does NOT have the key 'load-from' anywhere," +
                           "and that all files that do have the 'load-from' have valid settings names as their value." +
                           " ");
        // Static settings:
        if (firstJoinRtp = globalConfig.getBoolean("rtp-on-first-join.enabled", false)) {
            firstJoinSettings = getRtpSettingsByName(globalConfig.getString("rtp-on-first-join.settings"));
        } else {
            firstJoinSettings = null;
        }
        if (onDeathRtp = globalConfig.getBoolean("rtp-on-death.enabled", false)) {
            onDeathRespectBeds = globalConfig.getBoolean("rtp-on-death.respect-beds", true);
            onDeathRespectAnchors = globalConfig.getBoolean("rtp-on-death.respect-anchors", true);
            onDeathSettings = getRtpSettingsByName(globalConfig.getString("rtp-on-death.settings"));
            onDeathRequirePermission = globalConfig.getBoolean("rtp-on-death.require-permission", true);
        } else {
            onDeathRespectBeds = false;
            onDeathRespectAnchors = false;
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

        for (String line : infoStringAll(false)) infoLog("[#Static] " + line);
    }

    /* ================================================== *\
                    RtpSettings ← Getters
    \* ================================================== */

    //<editor-fold desc="RtpSettings ← Getters">

    /**
     * Getter for the ArrayList of RtpSettings. This contains all settings that are done per config sections.
     *
     * @return The ArrayList of RtpSettings.
     */
    public ArrayList<RtpProfile> getRtpSettings() {return rtpSettings;}

    /**
     * Gets the list of RtpSettings names.
     *
     * @return A list of RtpSettings names.
     */
    public ArrayList<String> getRtpSettingsNames() {
        ArrayList<String> rtpSettings = new ArrayList<>();
        for (RtpProfile rtpSetting : this.rtpSettings)
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
        for (RtpProfile rtpSetting : this.rtpSettings)
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
    public RtpProfile getRtpSettingsByWorld(World world) throws JrtpBaseException.NotPermittedException {
        RtpProfile finSettings = null;
        for (RtpProfile settings : rtpSettings)
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
    public RtpProfile getRtpSettingsByName(String name) throws JrtpBaseException {
        for (RtpProfile settings : rtpSettings)
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
    public RtpProfile getRtpSettingsByWorldForPlayer(Player player) throws JrtpBaseException.NotPermittedException {
        RtpProfile finSettings = null;
        World playerWorld = player.getWorld();
        for (RtpProfile settings : rtpSettings)
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
    public RtpProfile getRtpSettingsByNameForPlayer(Player player, String name)
    throws JrtpBaseException.NotPermittedException {
        for (RtpProfile settings : rtpSettings)
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
    //</editor-fold>

    /* ================================================== *\
                    Rtp Locations ← Getters
    \* ================================================== */

    //<editor-fold desc="Rtp Locations ← Getters">

    /**
     * Creates the potential RTP location. If this location happens to be safe, is will be the exact location that
     * the player gets teleported to (though that is unlikely as the {@code y} is {@code 255} by default). <p>
     * This method differs from {@code getRtpXZ()} because it includes the offset and returns a {@code Location}
     * whereas {@code getRtpZX()} only gets the initial {@code x} and {@code z}, and returns a coordinate pair.
     *
     * @param callFromLoc A location representing where the call originated from. This is used to get either the world
     *                    spawn, or player location for the position offset
     * @param rtpProfile  The relevant settings for RTP
     * @return The first location to check the safety of, which may end up being the final teleport location
     */
    @SuppressWarnings("ConstantConditions")
    private Location getPotentialRtpLocation(Location callFromLoc, RtpProfile rtpProfile) throws JrtpBaseException {
        // This (the xz) is where the random position comes from. The random position is based off the shape from
        // the distribution settings. The position then needs to be centered based on the centering criteria.
        int[] xz = rtpProfile.distribution.shape.getCords();
        // The offset is the centering criteria. Since we may need player / world specific locations, we do this here.
        int[] xzOffset = switch (rtpProfile.distribution.center) {
            case PLAYER_LOCATION -> new int[]{
                (int) callFromLoc.getX(),
                (int) callFromLoc.getZ()};
            case WORLD_SPAWN -> new int[]{
                (int) callFromLoc.getWorld().getSpawnLocation().getX(),
                (int) callFromLoc.getWorld().getSpawnLocation().getZ()};
            default -> new int[]{
                rtpProfile.distribution.centerX,
                rtpProfile.distribution.centerZ};
        };
        // Combind and return.
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
     * This method can bypass explicit permission checks.
     *
     * @param rtpProfile    The specific RtpSettings to get the location with.
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
    public Location getRtpLocation(final RtpProfile rtpProfile, Location callFromLoc, final boolean takeFromQueue)
    throws JrtpBaseException, JrtpBaseException.PluginDisabledException {
        // Part 1: Force destination world if not current world
        if (callFromLoc.getWorld() != rtpProfile.landingWorld)
            callFromLoc.setWorld(rtpProfile.landingWorld);

        // Part 2 option 1: The Queue Route.
        // If we want to take from the queue and the queue is enabled, go here.
        if (queueEnabled && takeFromQueue && rtpProfile.canUseLocQueue) {
            Location preselectedLocation = rtpProfile.locationQueue.poll();
            if (preselectedLocation != null) {
                plugin.getServer().getScheduler() // Tell queue to refill soon
                      .runTaskLaterAsynchronously(plugin, () -> locFinderRunnable.syncNotify(), 100);
                return preselectedLocation;
            }
        }

        // Part 2 option 2: The Normal Route.
        // If we need to find a NEW location (meaning we can't use the queue), go here.
        Location potentialRtpLocation;
        int randAttemptCount = 0;
        int failedToWorldBorder = 0, failedToClaimedLand = 0, failedToSafetyCheck = 0;
        boolean locationBad, temp = false;
        do {
            potentialRtpLocation = getPotentialRtpLocation(callFromLoc, rtpProfile);
            if (++randAttemptCount > rtpProfile.maxAttempts)
                throw new JrtpBaseException(
                    Messages.NP_R_TOO_MANY_FAILED_ATTEMPTS.format() + "\n[" +
                    "FailedToWorldBorder: " + failedToWorldBorder + ", " +
                    "FailedToClaims: " + failedToClaimedLand + ", " +
                    "FailedToSafety: " + failedToSafetyCheck + "]");
            //<editor-fold desc="Super verbose checking (and running `tryAndMakeSafe() method)">
            // Currently, I ASSUME that the `tryAndMakeSafe()` is by far the most expensive method, so I do the easy
            //   checks before (so we can avoid trying to make it safe) but then we also have to check them again at
            //   the end since `tryAndMakeSafe()` may move the location.
            // This next bit of code is going to be VERY verbose. Once one thing sets `locationBad` to true, we can
            //   skip everything else. The reason for the verbosity is so that we can log *why* the check failed. The
            //   general flow of the if statement goes like this: "If the location still looks safe, run this next
            //   check, and if it fails, mark the location as unsafe". Each of these if blocks MUST contain the
            //   `locationBad = temp` assignment because EVERY time we make it inside, the safty state of the location
            //   has changed!
            locationBad = false;
            //noinspection ConstantConditions // It's for readability’s sake. Just let it be.
            if (!locationBad && (temp = isOutsideWorldBorder(potentialRtpLocation))) {
                // 1: To be inside the world border (easy-op. do first)
                locationBad = temp;
                failedToWorldBorder++;
            }
            if (!locationBad && (temp = isInsideClaimedLand(potentialRtpLocation))) {
                // 2: To NOT be inside claimed land  (easy-op. do first)
                locationBad = temp;
                failedToClaimedLand++;
            }
            if (!locationBad && (temp = (Bukkit.isPrimaryThread() ?
                !new SafeLocationFinderBukkitThread(
                    potentialRtpLocation,
                    rtpProfile.checkRadiusXZ,
                    rtpProfile.checkRadiusVert,
                    rtpProfile.lowBound,
                    rtpProfile.highBound
                ).tryAndMakeSafe(rtpProfile.checkProfile) :
                !new SafeLocationFinderOtherThread(
                    potentialRtpLocation,
                    rtpProfile.checkRadiusXZ,
                    rtpProfile.checkRadiusVert,
                    rtpProfile.lowBound,
                    rtpProfile.highBound,
                    asyncWaitTimeout
                ).tryAndMakeSafe(rtpProfile.checkProfile)))) {
                // 3: For the location to actually be safe.
                // DON'T' FORGET, THIS MAY MOVE THE LOCATION
                locationBad = temp;
                failedToSafetyCheck++;
            }
            if (!locationBad && (temp = isOutsideWorldBorder(potentialRtpLocation))) {
                // 1: To be inside the world border (loc may have been moved. do again)
                locationBad = temp;
                failedToWorldBorder++;
            }
            if (!locationBad && (temp = isInsideClaimedLand(potentialRtpLocation))) {
                // 2: To NOT be inside claimed land (loc may have been moved. do again)
                locationBad = temp;
                failedToClaimedLand++;
            }
            //</editor-fold>
        } while (locationBad);
        return potentialRtpLocation;

    }
    //</editor-fold>

    /* ================================================== *\
                    Misc ← Workers
    \* ================================================== */

    //<editor-fold desc="Misc ← Workers">
    private boolean isOutsideWorldBorder(Location loc) {
        return !worldBorderPluginHook.isInside(loc);
    }

    private boolean isInsideClaimedLand(Location loc) {
        return claimsManager.isInside(loc);
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
    public int fillQueue(RtpProfile settings)
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
    //</editor-fold>

    /* ================================================== *\
                Info-Strings for Static Settings
    \* ================================================== */

    //<editor-fold desc="Info-Strings">
    public List<String> infoStringAll(boolean mcFormat) {
        ArrayList<String> lines = new ArrayList<>();
        if (mcFormat) {
            lines.add(HEADER_TOP.format(true));
            lines.add(HEADER_MID.format(true, "#Static Settings"));
            lines.add(HEADER_END.format(true));
        }
        lines.addAll(infoStringsFirstJoinRtp(mcFormat));
        lines.addAll(infoStringOnDeathRtp(mcFormat));
        lines.addAll(infoStringQueue(mcFormat));
        lines.addAll(infoStringLoggingSettings(mcFormat));
        return lines;
    }

    public List<String> infoStringsFirstJoinRtp(boolean mcFormat) {
        ArrayList<String> lines = new ArrayList<>();
        lines.add(LVL_01_SET.format(mcFormat, "RTP on first join", enabledOrDisabled(firstJoinRtp)));
        if (firstJoinRtp) {
            lines.add(LVL_02_SET.format(mcFormat, "Settings to use", firstJoinSettings.name));
            lines.add(LVL_02_SET.format(mcFormat, "Settings landing world", firstJoinSettings.landingWorld.getName()));
        }
        return lines;
    }

    public List<String> infoStringOnDeathRtp(boolean mcFormat) {
        ArrayList<String> lines = new ArrayList<>();
        lines.add(LVL_01_SET.format(mcFormat, "RTP on death", enabledOrDisabled(onDeathRtp)));
        if (onDeathRtp) {
            lines.add(LVL_02_SET.format(mcFormat, "Settings to use", onDeathSettings.name));
            lines.add(LVL_02_SET.format(mcFormat, "Settings landing world", onDeathSettings.landingWorld.getName()));
            lines.add(LVL_02_SET.format(mcFormat, "Respect beds", enabledOrDisabled(onDeathRespectBeds)));
            lines.add(LVL_02_SET.format(mcFormat, "Respect anchors", enabledOrDisabled(onDeathRespectAnchors)));
            lines.add(LVL_02_SET.format(mcFormat, "Require Permission", onDeathRequirePermission
                ? "True (jakesrtp.rtpondeath)" : "False"));
        }
        return lines;
    }

    public List<String> infoStringQueue(boolean mcFormat) {
        ArrayList<String> lines = new ArrayList<>();
        lines.add(LVL_01_SET.format(mcFormat, "Cache locations beforehand", enabledOrDisabled(queueEnabled)));
        return lines;
    }

    public List<String> infoStringLoggingSettings(boolean mcFormat) {
        ArrayList<String> lines = new ArrayList<>();
        lines.add(LVL_01_SET.format(mcFormat, "Logging", ""));
        lines.add(LVL_02_SET.format(mcFormat, "RTP on player join", enabledOrDisabled(logRtpOnPlayerJoin)));
        lines.add(LVL_02_SET.format(mcFormat, "RTP on respawn", enabledOrDisabled(logRtpOnRespawn)));
        lines.add(LVL_02_SET.format(mcFormat, "RTP on command", enabledOrDisabled(logRtpOnCommand)));
        lines.add(LVL_02_SET.format(mcFormat, "RTP on force command", enabledOrDisabled(logRtpOnForceCommand)));
        lines.add(LVL_02_SET.format(mcFormat, "RTP for queue", enabledOrDisabled(logRtpForQueue)));
        return lines;
    }
    //</editor-fold>
}