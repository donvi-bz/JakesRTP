package biz.donvi.jakesRTP;

import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.logging.Level;

import static biz.donvi.jakesRTP.CenterAllowedValues.*;
import static biz.donvi.jakesRTP.PluginMain.infoLog;
import static biz.donvi.jakesRTP.RtpRegionShape.*;

public class RtpSettings {

    private final ArrayList<World> configWorlds = new ArrayList<>();

    private final String name;
    private final RtpRegionShape rtpRegionShape;
    private final int maxRadius;
    private final int minRadius;
    private final CenterAllowedValues centerLocation;
    private final int centerX;
    private final int centerZ;
    private final int lowBound;
    public final int checkRadiusXZ;
    public final int checkRadiusVert;
    final CoolDownTracker coolDown;

    /**
     * Creates an RtpSettings object. This primarily deals with reading in data from a YAML config,
     * and saving all the data as something more accessible for the plugin.
     *
     * @param config The configuration section that contains the RTP settings
     * @throws Exception if any data can not be loaded.
     */
    RtpSettings(ConfigurationSection config, String name) throws Exception {
        infoLog("Loading random teleporter...");
        this.name = name;

        for (String worldName : config.getStringList("enabled-worlds"))
            try {
                configWorlds.add(Objects.requireNonNull(PluginMain.plugin.getServer().getWorld(worldName)));
            } catch (NullPointerException e) {
                PluginMain.logger.log(Level.WARNING, "World " + worldName + " not recognised.");
            }


        // SHAPE
        switch (config.getString("shape.value").toLowerCase()) {
            case "a":
                infoLog("[" + name + "] " + "RTP region shape set to SQUARE");
                rtpRegionShape = SQUARE;
                break;
            case "b":
                infoLog("[" + name + "] " + "RTP region shape set to CIRCLE");
                rtpRegionShape = CIRCLE;
                break;
            case "c":
                infoLog("[" + name + "] " + "RTP region shape set to RECTANGLE");
                rtpRegionShape = RECTANGLE;
                break;
            default:
                throw new Exception("Could not get rtp region shape: value not recognised");
        }


        // RADIUS
        maxRadius = config.getInt("defining-points.radius-center.radius.max");
        infoLog("[" + name + "] " + "Max Radius set to " + maxRadius);
        minRadius = config.getInt("defining-points.radius-center.radius.min");
        infoLog("[" + name + "] " + "Min radius set to " + minRadius);


        // RTP CENTER
        switch (config.getString("defining-points.radius-center.center.value").toLowerCase()) {
            case "a":
                infoLog("[" + name + "] " + "RTP center set to world spawn.");
                centerLocation = WORLD_SPAWN;
                break;
            case "b":
                infoLog("[" + name + "] " + "RTP center set to player location.");
                centerLocation = PLAYER_LOCATION;
                break;
            case "c":
                infoLog("[" + name + "] " + "RTP center is using the value defined in the config.");
                centerLocation = PRESET_VALUE;
                break;
            default:
                throw new Exception("Could not get center allowed values.");
        }


        // ENABLED WORLD
        StringBuilder stringBuilder = new StringBuilder();
        for (World world : configWorlds)
            stringBuilder.append(' ').append(world.getName());
        infoLog("[" + name + "] " +
                "Rtp enabled in the following worlds:" + stringBuilder.toString());


        // RTP CENTER X & Z
        centerX = config.getInt("defining-points.radius-center.center.x");
        centerZ = config.getInt("defining-points.radius-center.center.z");

        infoLog("[" + name + "] " +
                "RTP Center x & z are " + centerX + " and " + centerZ);

        // LOW BOUND
        lowBound = config.getInt("low-bound.value");
        infoLog("[" + name + "] " +
                "Set low bound to " + lowBound);

        // CHECK RADIUS
        checkRadiusXZ = config.getInt("check-radius.x-z");
        checkRadiusVert = config.getInt("check-radius.vert");
        infoLog("[" + name + "] " +
                "Check radius x and z set to " + checkRadiusXZ +
                " and vert set to " + checkRadiusVert);

        // COOL-DOWN TRACKER
        coolDown = new CoolDownTracker(config.getInt("cooldown.seconds"));
        infoLog("[" + name + "] " +
                "Cooldown time: " + coolDown.coolDownTime/1000 + " seconds.");
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

    /**
     * Returns the max radius as set in the config.
     *
     * @return The maximum radius.
     */
    public int getMaxRadius() {
        return maxRadius;
    }

    /**
     * Returns the min radius as set in the config.
     *
     * @return The minimum radius.
     */
    public int getMinRadius() {
        return minRadius;
    }

    /**
     * Returns the center x and z coordinates.
     * No methods were written to get x and z separably because
     * that felt like too much work for the two integers, and I
     * will just use the fields internally.
     *
     * @return The center x and z coordinates.
     */
    public int[] getCenterXz() {
        return new int[]{centerX, centerZ};
    }

    public RtpRegionShape getRtpRegionShape() {
        return rtpRegionShape;
    }

    public int getLowBound() {
        return lowBound;
    }

    public CenterAllowedValues getCenterLocation() {
        return centerLocation;
    }

    public ArrayList<World> getConfigWorlds() {
        return configWorlds;
    }


}


/**
 * Small enum to define the types of shapes that this plugin can RTP in
 */
enum RtpRegionShape {
    SQUARE, CIRCLE, RECTANGLE
}

enum CenterAllowedValues {
    WORLD_SPAWN, PLAYER_LOCATION, PRESET_VALUE
}