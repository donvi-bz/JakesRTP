package biz.donvi.jakesRTP;

import biz.donvi.jakesRTP.SafeLocationFinder.LocCheckProfiles;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Pattern;

import static biz.donvi.jakesRTP.PluginMain.infoLog;
import static biz.donvi.jakesRTP.PluginMain.plugin;
import static biz.donvi.jakesRTP.RandomTeleporter.EXPLICIT_PERM_PREFIX;

public class RtpSettings {

    /**
     * A map of config worlds and their respective location cache as a queue. This is also the only storage
     * of which worlds are enabled in the config.
     */
//    private final Map<World, ConcurrentLinkedQueue<Location>> configWorlds = new HashMap<>();
    public final ConcurrentLinkedQueue<Location> locationQueue = new ConcurrentLinkedQueue<>();

    /* All settings below are read directly from the config */
    public final String               name;
    public final boolean              commandEnabled;
    public final boolean              requireExplicitPermission;
    public final float                priority;
    public final World                landingWorld;
    public final List<World>          callFromWorlds;
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
    /* except this one */
    public final boolean              canUseLocQueue;

    /**
     * Creates an RtpSettings object. This primarily deals with reading in data from a YAML config,
     * and saving all the data as something more accessible for the plugin.
     *
     * @param config The configuration section that contains the RTP settings
     * @throws Exception if any data can not be loaded.
     */
    @SuppressWarnings("ConstantConditions")
    RtpSettings(final ConfigurationSection config, String name, Map<String, DistributionSettings> distributions)
    throws JrtpBaseException {
        this.name = name;
        infoLog("Loading random teleporter...");
        commandEnabled = config.getBoolean("command-enabled", true);
        requireExplicitPermission = config.getBoolean("require-explicit-permission", false);
        priority = (float) config.getDouble("priority", 1f);
        if ((landingWorld = plugin.getServer().getWorld(config.getString("landing-world", ""))) == null)
            throw new JrtpBaseException("Landing world not recognised.");
        callFromWorlds = new ArrayList<>();
        for (String callFromWorld : config.getStringList("call-from-worlds")) {
            Pattern patternFromConf = Pattern.compile(callFromWorld);
            for (World testByWorld : plugin.getServer().getWorlds())
                if (!callFromWorlds.contains(testByWorld) && patternFromConf.matcher(testByWorld.getName()).matches())
                    callFromWorlds.add(testByWorld);
        }
        if (callFromWorlds.size() == 0) callFromWorlds.add(landingWorld);
        try {
            String distName = config.getString("distribution");
            if (distName.equalsIgnoreCase("world-border"))
                distName += "_" + landingWorld.getName();
            distribution = distributions.get(distName);
            Objects.requireNonNull(distribution);
        } catch (NullPointerException e) {
            StringBuilder strb = new StringBuilder();
            for (String s : distributions.keySet()) strb.append(" ").append(s);
            throw new JrtpBaseException(
                "Distribution not found. Distribution given: '" + config.getString("distribution") +
                "', distributions available:" + strb.toString());
        }
        coolDown = new CoolDownTracker(config.getInt("cooldown", 30));
        lowBound = config.getInt("bounds.low", 32);
        highBound = config.getInt("bounds.high", 255);
        checkRadiusXZ = config.getInt("check-radius.x-z", 2);
        checkRadiusVert = config.getInt("check-radius.vert", 2);
        maxAttempts = config.getInt("max-attempts.value", 10);
        cacheLocationCount = config.getInt("preparations.cache-locations", 10);
        checkProfile = LocCheckProfiles.values()
            [config.getString("location-checking-profile", "a").toLowerCase().charAt(0) - 'a'];
        commandsToRun = config.getStringList("then-execute").toArray(new String[0]);
        canUseLocQueue = distribution.center != DistributionSettings.CenterTypes.PLAYER_LOCATION &&
                         cacheLocationCount > 0;
        infoLogSettings(true);
    }

    /**
     * Running this will log all settings loaded in the config to console.
     */
    public void infoLogSettings(boolean full) {
        // In the current context, we always need the name in square brackets.
        String name = "[" + this.name + "] ";
        ArrayList<String> lines = new ArrayList<>();
        // Always log
        lines.add(infoStringCommandEnabled(false));
        lines.add(infoStringRequireExplicitPermission(false));
        lines.add(infoStringPriority(false));
        lines.add(infoStringDestinationWorld(false));
        lines.addAll(distribution.shape.infoStrings(false));
        lines.add(infoStringRegionCenter(false));
        // Kinda useless
        if (full) {
            lines.add(infoStringCooldown(false));
            lines.add(infoStringLowBound(false));
            lines.add(infoStringCheckRadius(false));
            lines.add(infoStringMaxAttempts(false));
            lines.add(infoStringLocationCaching(false));
        }
        // Now log it all!
        for (String line : lines) infoLog(name + line);
    }


    public String getRtpRegionCenterAsString(final boolean mcFormat) {
        switch (distribution.center) {
            case WORLD_SPAWN:
                return MessageFormat.format(
                    "World Spawn {0}({1}, {2}){0}",
                    mcFormat ? "\u00A7o" : "",
                    (int) landingWorld.getSpawnLocation().getX(),
                    (int) landingWorld.getSpawnLocation().getZ());
            case PRESET_VALUE:
                return MessageFormat.format(
                    "Specified in Config {0}({1}, {2}){0}",
                    mcFormat ? "\u00A7o" : "",
                    distribution.centerX,
                    distribution.centerZ);
            case PLAYER_LOCATION:
                return "Player's Location";
        }
        return "??";
    }

    public String getQueueSizesAsString() {
        return landingWorld.getName() + " [" + locationQueue.size() + "] ";
    }

    /* ================================================== *\
                    Info Strings... todo respect mcFormat
    \* ================================================== */

    public String infoStringCommandEnabled(boolean mcFormat) {
        return "Command " + (commandEnabled ? "enabled" : "disabled");
    }

    public String infoStringRequireExplicitPermission(boolean mcFormat) {
        return requireExplicitPermission
            ? "Requires permission node [" + EXPLICIT_PERM_PREFIX + this.name + "] to use"
            : "No explicit named permission required";
    }

    public String infoStringPriority(boolean mcFormat) {
        return "Priority: " + priority;
    }

    public String infoStringDestinationWorld(boolean mcFormat) {
        return "The user will land in the following world: " + landingWorld.getName();
    }

    public String infoStringRegionCenter(boolean mcFormat) { // redundant? Look at `distribution.shape.infoStrings()`
        return "Rtp Region center is set to " + getRtpRegionCenterAsString(false);
    }

    public String infoStringCooldown(boolean mcFormat) {
        return coolDown.coolDownTime == 0
            ? "Cooldown disabled"
            : "Cooldown time: " + coolDown.coolDownTime / 1000 + " seconds.";
    }

    public String infoStringLowBound(boolean mcFormat) {
        return "Low bound: " + lowBound + " | High bound: " + highBound;
    }

    public String infoStringCheckRadius(boolean mcFormat) {
        return "Check radius x and z set to " + checkRadiusXZ + " and vert set to " + checkRadiusVert;
    }

    public String infoStringMaxAttempts(boolean mcFormat) {
        return "Max attempts set to " + maxAttempts;
    }

    public String infoStringLocationCaching(boolean mcFormat) {
        return "Location caching " + (
            canUseLocQueue
                ? "disabled."
                : "Enabled. Caching " + cacheLocationCount + " location per world.");
    }
}
