package biz.donvi.jakesRTP;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;

import static biz.donvi.jakesRTP.PluginMain.infoLog;
import static biz.donvi.jakesRTP.RandomTeleporter.explicitPermPrefix;

public class RtpSettings {

    /**
     * A map of config worlds and their respective location cache as a queue. This is also the only storage
     * of which worlds are enabled in the config.
     */
    private final Map<World, ConcurrentLinkedQueue<Location>> configWorlds = new HashMap<>();
    public final boolean useLocationQueue;
    /* All settings below are read directly from the config */
    public final String name;
    public final boolean commandEnabled;
    public final boolean requireExplicitPermission;
    public final float priority;
    public final RtpRegionShape rtpRegionShape;
    public final int maxRadius;
    public final int minRadius;
    public final CenterAllowedValues centerLocation;
    public final int centerX;
    public final int centerZ;
    public final double gaussianShrink;
    public final double gaussianCenter;
    final CoolDownTracker coolDown;
    public final int lowBound;
    public final int checkRadiusXZ;
    public final int checkRadiusVert;
    public final int maxAttempts;
    public final int cacheLocationCount;        //TODO | Add to infoLogSettings & rtp-admin status
    public final int chunkKeepLoadedCountMax;   // CONT| Add to infoLogSettings & rtp-admin status
    public final int chunkKeepLoadedCountPer;   // CONT| Add to infoLogSettings & rtp-admin status

    /**
     * Creates an RtpSettings object. This primarily deals with reading in data from a YAML config,
     * and saving all the data as something more accessible for the plugin.
     *
     * @param config The configuration section that contains the RTP settings
     * @throws Exception if any data can not be loaded.
     */
    @SuppressWarnings("ConstantConditions")
    RtpSettings(final ConfigurationSection config, String name) throws Exception {
        this.name = name;
        infoLog("Loading random teleporter...");
        commandEnabled = config.getBoolean("command-enabled");
        requireExplicitPermission = config.getBoolean("require-explicit-permission");
        priority = (float) config.getDouble("priority");
        cacheLocationCount = config.getInt("preparations.cache-locations");
        chunkKeepLoadedCountMax = config.getInt("preparations.keep-loaded-max");
        chunkKeepLoadedCountPer = config.getInt("preparations.keep-loaded-per");
        for (String worldName : config.getStringList("enabled-worlds"))
            try {
                configWorlds.put(
                        Objects.requireNonNull(PluginMain.plugin.getServer().getWorld(worldName)),
                        new ConcurrentLinkedQueue<>());
            } catch (NullPointerException e) {
                PluginMain.logger.log(Level.WARNING, "![" + name + "] World " + worldName + " not recognised.");
            }
        rtpRegionShape = RtpRegionShape.values()[config.getString(
                "shape.value").toLowerCase().charAt(0) - 'a'];
        maxRadius = config.getInt("defining-points.radius-center.radius.max");
        minRadius = config.getInt("defining-points.radius-center.radius.min");
        centerLocation = CenterAllowedValues.values()[config.getString(
                "defining-points.radius-center.center.value").toLowerCase().charAt(0) - 'a'];
        centerX = config.getInt("defining-points.radius-center.center.x");
        centerZ = config.getInt("defining-points.radius-center.center.z");
        if (config.getBoolean("defining-points.radius-center.gaussian-distribution.enabled")) {
            gaussianShrink = config.getDouble("defining-points.radius-center.gaussian-distribution.shrink");
            gaussianCenter = config.getDouble("defining-points.radius-center.gaussian-distribution.center");
        } else gaussianShrink = gaussianCenter = 0;
        coolDown = new CoolDownTracker(config.getInt("cooldown.seconds"));
        lowBound = config.getInt("low-bound.value");
        checkRadiusXZ = config.getInt("check-radius.x-z");
        checkRadiusVert = config.getInt("check-radius.vert");
        maxAttempts = config.getInt("max-attempts.value");
        //Some important finalization work.
        useLocationQueue = centerLocation != CenterAllowedValues.PLAYER_LOCATION &&
                           cacheLocationCount > 0;
        infoLogSettings();
    }

    /**
     * Running this will log all settings loaded in the config to console.
     */
    public void infoLogSettings() {
        // In the current context, we always need the name in square brackets.
        String name = "[" + this.name + "] ";
        infoLog(name + "Command " + (commandEnabled ? "enabled" : "disabled"));
        infoLog(name + (requireExplicitPermission ?
                "Requires permission node [" + explicitPermPrefix + this.name + "] to use" :
                "No explicit named permission required"));
        infoLog(name + "Priority: " + priority);
        infoLog(name + "Rtp enabled in the following worlds: " + getWorldsAsString());
        infoLog(name + "Rtp region shape set to " + rtpRegionShape.toString());
        infoLog(name + "Min radius set to " + minRadius + " | Max Radius set to " + maxRadius);
        infoLog(name + "Rtp Region center is set to " + getRtpRegionCenterAsString(false));
        infoLog(name + (gaussianCenter == 0 && gaussianShrink == 0 ?
                "Using even distribution." :
                "Gaussian distribution enabled. Shrink: " + gaussianShrink + " Center: " + gaussianCenter));
        infoLog(name + (coolDown.coolDownTime == 0 ?
                "Cooldown disabled" :
                "Cooldown time: " + coolDown.coolDownTime / 1000 + " seconds."));
        infoLog(name + "Set low bound to " + lowBound);
        infoLog(name + "Check radius x and z set to " + checkRadiusXZ + " and vert set to " + checkRadiusVert);
        infoLog(name + "Max attempts set to " + maxAttempts);
    }

    public String getRtpRegionCenterAsString(final boolean mcFormat) {
        switch (centerLocation) {
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
                       "(" + centerX + ", " + centerZ + ")" + (mcFormat ? "\u00A7r" : "");
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

    /**
     * Tells if the selected shape uses the "Radius & Center" style config.
     *
     * @return Whether "Radius & Center" config is used or not.
     */
    public boolean isRadiusAndCenter() {
        switch (rtpRegionShape) {
            case SQUARE:
            case CIRCLE:
                return true;
            case RECTANGLE:
            default:
                return false;
        }
    }

    /**
     * Tells if the selected shape uses the "Bounding Corners" style config.
     * Since there are currently only two types of config, this method just
     * returns the opposite of <c>isRadiusAndCenter()</c>
     *
     * @return Whether "Bounding Corners" config is used or not.
     */
    public boolean isBoundingCorners() {
        return !isRadiusAndCenter();
    }

    public Set<World> getConfigWorlds() {
        return configWorlds.keySet();
    }

    public Queue<Location> getLocationQueue(World world) throws NotPermittedException {
        Queue<Location> locationQueue = configWorlds.get(world);
        if (locationQueue == null) throw new NotPermittedException("RTP is not enabled in this world. ~ECQ");
        else return locationQueue;
    }


    /**
     * Small enum to define the types of shapes that this plugin can RTP in
     */
    enum RtpRegionShape {SQUARE, CIRCLE, RECTANGLE;}

    enum CenterAllowedValues {WORLD_SPAWN, PLAYER_LOCATION, PRESET_VALUE}
}
