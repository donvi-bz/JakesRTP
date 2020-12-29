package biz.donvi.jakesRTP;

import biz.donvi.jakesRTP.SafeLocationFinder.LocCheckProfiles;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;

import static biz.donvi.jakesRTP.PluginMain.infoLog;
import static biz.donvi.jakesRTP.PluginMain.plugin;
import static biz.donvi.jakesRTP.RandomTeleporter.EXPLICIT_PERM_PREFIX;

public class RtpSettings {

    /**
     * A map of config worlds and their respective location cache as a queue. This is also the only storage
     * of which worlds are enabled in the config.
     */
    private final Map<World, ConcurrentLinkedQueue<Location>> configWorlds = new HashMap<>();

    public final boolean useLocationQueue;

    /* All settings below are read directly from the config */
    public final String               name;
    public final boolean              commandEnabled;
    public final boolean              requireExplicitPermission;
    public final float                priority;
    public final boolean              forceDestinationWorld;
    public final World                destinationWorld;
    public final DistributionSettings distribution;
    final        CoolDownTracker      coolDown;
    public final int                  lowBound;
    public final int                  highBound;
    public final int                  checkRadiusXZ;
    public final int                  checkRadiusVert;
    public final int                  maxAttempts;
    public final int                  cacheLocationCount;
    public final LocCheckProfiles     checkProfile;
    public       String[]             commandsToRun;

    /**
     * Creates an RtpSettings object. This primarily deals with reading in data from a YAML config,
     * and saving all the data as something more accessible for the plugin.
     *
     * @param config The configuration section that contains the RTP settings
     * @throws Exception if any data can not be loaded.
     */
    @SuppressWarnings("ConstantConditions")
    RtpSettings(final ConfigurationSection config, String name, Map<String, DistributionSettings> distributions)
    throws Exception {
        this.name = name;
        infoLog("Loading random teleporter...");
        commandEnabled = config.getBoolean("command-enabled", true);
        requireExplicitPermission = config.getBoolean("require-explicit-permission", false);
        priority = (float) config.getDouble("priority", 1f);
        forceDestinationWorld = config.getBoolean("force-destination-world.enabled", false);
        if (forceDestinationWorld) {
            destinationWorld = plugin.getServer().getWorld(config.getString(
                "force-destination-world.destination", null));
            if (destinationWorld == null) throw new JrtpBaseException("Force destination world not recognised.");
            configWorlds.putIfAbsent(destinationWorld, new ConcurrentLinkedQueue<>());
        } else destinationWorld = null;
        cacheLocationCount = config.getInt("preparations.cache-locations", 10);
        for (String worldName : config.getStringList("enabled-worlds"))
            try {
                configWorlds.put(
                    Objects.requireNonNull(plugin.getServer().getWorld(worldName)),
                    new ConcurrentLinkedQueue<>());
            } catch (NullPointerException e) {
                PluginMain.log(Level.WARNING, "![" + name + "] World " + worldName + " not recognised.");
            }
        distribution = distributions.get(config.getString("distributions"));
        coolDown = new CoolDownTracker(config.getInt("cooldown.seconds", 30));
        checkProfile = LocCheckProfiles.values()[config.getString(
            "location-checking-profile.value").toLowerCase().charAt(0) - 'a'];
        lowBound = config.getInt("bounds.low", 32);
        highBound = config.getInt("bounds.high", 255);
        checkRadiusXZ = config.getInt("check-radius.x-z", 2);
        checkRadiusVert = config.getInt("check-radius.vert", 2);
        maxAttempts = config.getInt("max-attempts.value", 10);
        //Some important finalization work.
        useLocationQueue = distribution.center != DistributionSettings.CenterTypes.PLAYER_LOCATION &&
                           cacheLocationCount > 0;
        commandsToRun = config.getStringList("then-execute").toArray(new String[0]);
        infoLogSettings();
    }

    /**
     * Running this will log all settings loaded in the config to console.
     */
    public void infoLogSettings() {
        // In the current context, we always need the name in square brackets.
        String name = "[" + this.name + "] ";
        for (String s : Arrays.asList(
            "Command " + (commandEnabled ? "enabled" : "disabled"),
            requireExplicitPermission
                ? "Requires permission node [" + EXPLICIT_PERM_PREFIX + this.name + "] to use"
                : "No explicit named permission required",
            "Priority: " + priority,
            "Rtp enabled in the following worlds: " + getWorldsAsString(),

            "TODO: ADD MISSING INFORMATION", //TODO: Add missing information
//            "Rtp region shape set to " + rtpRegionShape.toString(),
//            "Min radius set to " + minRadius + " | Max Radius set to " + maxRadius,
//            "Rtp Region center is set to " + getRtpRegionCenterAsString(false),
//            gaussianCenter == 0 && gaussianShrink == 0
//                ? "Using even distribution."
//                : "Gaussian distribution enabled. Shrink: " + gaussianShrink + " Center: " + gaussianCenter,

            coolDown.coolDownTime == 0
                ? "Cooldown disabled"
                : "Cooldown time: " + coolDown.coolDownTime / 1000 + " seconds.",
            "Low bound: " + lowBound + " | High bound: " + highBound,
            "Check radius x and z set to " + checkRadiusXZ + " and vert set to " + checkRadiusVert,
            "Max attempts set to " + maxAttempts,
            "Location caching " + (
                useLocationQueue
                    ? "disabled."
                    : "Enabled. Caching " + cacheLocationCount + " location per world."))
        ) { infoLog(name + s); }
    }

    public String getRtpRegionCenterAsString(final boolean mcFormat) {
        switch (distribution.center) {
            case WORLD_SPAWN:
                StringBuilder spawnLocation = new StringBuilder("World Spawn ")
                    .append(mcFormat ? "\u00A7o" : "")
                    .append("[");
                for (Iterator<World> iterator = getConfigWorlds().iterator(); iterator.hasNext(); ) {
                    World world = iterator.next();
                    spawnLocation
                        .append("(")
                        .append((int) world.getSpawnLocation().getX())
                        .append(", ")
                        .append((int) world.getSpawnLocation().getZ())
                        .append(") in ")
                        .append(world.getName())
                        .append(iterator.hasNext() ? "; " : "");
                }
                return spawnLocation
                    .append("]")
                    .append(mcFormat ? "\u00A7r" : "")
                    .toString();
            case PRESET_VALUE:
                return "Specified in Config " + (mcFormat ? "\u00A7o" : "") +
                       "(" + distribution.centerX + ", " + distribution.centerZ + ")" +
                       (mcFormat ? "\u00A7r" : "");
            case PLAYER_LOCATION:
                return "Player's Location";
        }
        return "??";
    }

    public String getWorldsAsString() {
        StringBuilder strb = new StringBuilder();
        World[] worldsAsList = getConfigWorlds().toArray(new World[0]);
        for (int i = 0; i < worldsAsList.length; i++) {
            if (i != 0) strb.append(" ");
            strb.append(worldsAsList[i].getName());
        }
        return strb.toString();
    }

    public String getQueueSizesAsString() {
        StringBuilder strb = new StringBuilder();
        for (World world : configWorlds.keySet())
            strb.append(world.getName()).append(" [").append(configWorlds.get(world).size()).append("] ");
        return strb.toString();
    }

    public Set<World> getConfigWorlds() {
        return configWorlds.keySet();
    }

    public Queue<Location> getLocationQueue(World world) throws NotPermittedException {
        Queue<Location> locationQueue = configWorlds.get(world);
        if (locationQueue == null) throw new NotPermittedException(Messages.NP_R_NOT_ENABLED.format("~ECQ"));
        else return locationQueue;
    }
}
