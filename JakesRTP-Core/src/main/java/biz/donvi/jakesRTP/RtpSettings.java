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

import static biz.donvi.jakesRTP.JakesRtpPlugin.infoLog;
import static biz.donvi.jakesRTP.JakesRtpPlugin.plugin;
import static biz.donvi.jakesRTP.MessageStyles.DebugDisplayLines.*;
import static biz.donvi.jakesRTP.MessageStyles.enabledOrDisabled;
import static biz.donvi.jakesRTP.RandomTeleporter.EXPLICIT_PERM_PREFIX;

public class RtpSettings {

    /**
     * A map of config worlds and their respective location cache as a queue. This is also the only storage
     * of which worlds are enabled in the config.
     */
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
     * @throws JrtpBaseException if any data can not be loaded.
     */
    @SuppressWarnings("ConstantConditions")
    RtpSettings(final ConfigurationSection config, String name, Map<String, DistributionSettings> distributions)
    throws JrtpBaseException {
        this.name = name;
        String nameInLog = "[" + this.name + "] ";
        infoLog("Loading random teleporter...");

        commandEnabled = config.getBoolean("command-enabled", true);
        infoLog(nameInLog + infoStringCommandEnabled(false));

        requireExplicitPermission = config.getBoolean("require-explicit-permission", false);
        infoLog(nameInLog + infoStringRequireExplicitPermission(false));

        priority = (float) config.getDouble("priority", 1f);
        infoLog(nameInLog + infoStringPriority(false));

        if ((landingWorld = plugin.getServer().getWorld(config.getString("landing-world", null))) == null)
            throw new JrtpBaseException("Landing world not recognised.");
        infoLog(nameInLog + infoStringDestinationWorld(false));

        callFromWorlds = new ArrayList<>();
        for (String callFromWorld : config.getStringList("call-from-worlds"))
            for (World testByWorld : plugin.getServer().getWorlds())
                if (!callFromWorlds.contains(testByWorld) &&
                    Pattern.compile(callFromWorld).matcher(testByWorld.getName()).matches()
                ) callFromWorlds.add(testByWorld);
        if (callFromWorlds.size() == 0) callFromWorlds.add(landingWorld);
        for (String s : infoStringsCallFromWorlds(false)) infoLog(nameInLog + s);

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
        for (String s : distribution.shape.infoStrings(false)) infoLog(nameInLog + s);
        infoLog(nameInLog + infoStringRegionCenter(false)); // double log!

        coolDown = new CoolDownTracker(config.getInt("cooldown", 30));
        infoLog(nameInLog + infoStringCooldown(false));

        lowBound = config.getInt("bounds.low", 32);
        highBound = config.getInt("bounds.high", 255);
        infoLog(nameInLog + infoStringVertBounds(false));

        checkRadiusXZ = config.getInt("check-radius.x-z", 2);
        checkRadiusVert = config.getInt("check-radius.vert", 2);
        infoLog(nameInLog + infoStringCheckRadius(false));

        maxAttempts = config.getInt("max-attempts.value", 10);
        infoLog(nameInLog + infoStringMaxAttempts(false));

        cacheLocationCount = config.getInt("preparations.cache-locations", 10);
        checkProfile = LocCheckProfiles.values()
            [config.getString("location-checking-profile", "a").toLowerCase().charAt(0) - 'a'];
        commandsToRun = config.getStringList("then-execute").toArray(new String[0]);
        canUseLocQueue = distribution.center != DistributionSettings.CenterTypes.PLAYER_LOCATION &&
                         cacheLocationCount > 0;
        infoLog(nameInLog + infoStringLocationCaching(false));

    }


    public List<String> infoStringAll(boolean mcFormat, boolean full) {
        String name = "[" + this.name + "] ";
        ArrayList<String> lines = new ArrayList<>();
        if (mcFormat) {
            lines.add(HEADER_TOP.format(true));
            lines.add(HEADER_MID.format(true, "RtpSettings for " + name));
            lines.add(HEADER_END.format(true));
        }
        // Always log
        lines.add(infoStringCommandEnabled(mcFormat));
        lines.add(infoStringRequireExplicitPermission(mcFormat));
        lines.add(infoStringPriority(mcFormat));
        lines.add(infoStringDestinationWorld(mcFormat));
        lines.addAll(infoStringsCallFromWorlds(mcFormat));
        lines.addAll(distribution.shape.infoStrings(mcFormat));
        lines.add(infoStringRegionCenter(mcFormat));
        lines.add(infoStringCooldown(mcFormat));
        // Kinda useless
        if (full) {
            lines.add(HEADER_END.format(true));
            lines.add(infoStringVertBounds(mcFormat));
            lines.add(infoStringCheckRadius(mcFormat));
            lines.add(infoStringMaxAttempts(mcFormat));
            lines.add(infoStringLocationCaching(mcFormat));
        }
        if (!mcFormat) for (int i = 0; i < lines.size(); i++) lines.set(i, name + lines.get(i));

        return lines;
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
        return LVL_01_SET.format(mcFormat, "Command", enabledOrDisabled(commandEnabled));
    }

    public String infoStringRequireExplicitPermission(boolean mcFormat) {
        return LVL_01_SET.format(mcFormat, "Require explicit permission node", requireExplicitPermission
            ? "True (" + EXPLICIT_PERM_PREFIX + this.name + ")" : "False");
    }

    public String infoStringPriority(boolean mcFormat) {
        return LVL_01_SET.format(mcFormat, "Priority", String.valueOf(priority));
    }

    public String infoStringDestinationWorld(boolean mcFormat) {
        return LVL_01_SET.format(mcFormat, "The user will land in the following world", landingWorld.getName());
    }

    public List<String> infoStringsCallFromWorlds(boolean mcFormat) {
        ArrayList<String> lines = new ArrayList<>();
        lines.add(LVL_01_SET.format(mcFormat, "Call from worlds", ""));
        for (int i = 0; i < callFromWorlds.size(); i++) {
            lines.add(LVL_02_SET.format(mcFormat, i, callFromWorlds.get(i).getName()));
        }
        return lines;
    }

    public String infoStringRegionCenter(boolean mcFormat) { // redundant? Look at `distribution.shape.infoStrings()`
        return LVL_01_SET.format(mcFormat, "Rtp Region center is set to", getRtpRegionCenterAsString(false));
    }

    public String infoStringCooldown(boolean mcFormat) {
        return coolDown.coolDownTime == 0
            ? LVL_01_SET.format(mcFormat, "Cooldown", "Disabled")
            : LVL_01_SET.format(mcFormat, "Cooldown time", coolDown.coolDownTime / 1000 + " seconds.");
    }

    public String infoStringVertBounds(boolean mcFormat) {
        return DOU_01_SET.format(mcFormat, "Low bound", lowBound, "High bound", highBound);
    }

    public String infoStringCheckRadius(boolean mcFormat) {
        return DOU_01_SET.format(mcFormat, "Check radius x and z", checkRadiusXZ, "Vert", checkRadiusVert);
    }

    public String infoStringMaxAttempts(boolean mcFormat) {
        return LVL_01_SET.format(mcFormat, "Max attempts set to", maxAttempts);
    }

    public String infoStringLocationCaching(boolean mcFormat) {
        return canUseLocQueue
            ? DOU_01_SET.format(mcFormat, "Location caching", "Enabled", "Num", cacheLocationCount)
            : LVL_01_SET.format(mcFormat, "Location caching", "Disabled");
    }
}
