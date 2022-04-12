package biz.donvi.jakesRTP.claimsIntegrations;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClaimsManager {
    protected final Plugin               ownerPlugin;
    protected final Logger               logger;
    protected       LocationRestrictor[] locationRestrictors;
    protected       List<String>         locationRestrictorNames;
    protected       ConfigurationSection configurationSection;


    /**
     * Creates an empty (and useless) claims manager. Yeah, this is bad design,
     * but the plugin rewrite has already started, so I'll fix it later.
     */
    public ClaimsManager() {
        ownerPlugin = null;
        logger = null;
        locationRestrictors = new LocationRestrictor[0];
        locationRestrictorNames = Collections.emptyList();
    }

    public ClaimsManager(Plugin ownerPlugin, ConfigurationSection configSection) {
        this.ownerPlugin = ownerPlugin;
        logger = ownerPlugin.getLogger();
        locationRestrictors = tryMakeLocationRestrictor();
        configurationSection = configSection;
    }

    public List<String> enabledLocationRestrictors() {
        List<String> names = new ArrayList<>();
        for (LocationRestrictor restrictor : locationRestrictors)
            names.add(restrictor.supporterPlugin().getName());
        return names;
    }

    public boolean isInside(Location loc) {
        for (var restrictor : locationRestrictors)
            if (restrictor.denyLandingAtLocation(loc))
                return true;
        return false;
    }


    private Plugin tryGetPlugin(String name) {return ownerPlugin.getServer().getPluginManager().getPlugin(name);}

    private LocationRestrictor[] tryMakeLocationRestrictor() {
        List<LocationRestrictor> restrictors = new ArrayList<>();
        logger.log(Level.INFO, "Looking for compatible land claim plugins...");

        // GriefPrevention support!
        generalPluginLoader("grief-prevention", "GriefPrevention", (pluginName) -> {
            Plugin plugin;
            if ((plugin = tryGetPlugin(pluginName)) != null) {
                restrictors.add(new LrGriefPrevention((GriefPrevention) plugin));
                return true;
            } else return false;
        });

        // WorldGuard support!
        generalPluginLoader("world-guard", "WorldGuard", (pluginName) -> {
            Plugin plugin;
            if ((plugin = tryGetPlugin(pluginName)) != null) {
                restrictors.add(new LrWorldGuard((WorldGuardPlugin) plugin));
                return true;
            } else return false;
        });

        //HuskTowns support!
        generalPluginLoader("husk-towns", "HuskTowns", (pluginName) -> {
            Plugin plugin;
            if ((plugin = tryGetPlugin(pluginName)) != null) {
                restrictors.add(new LrHuskTowns(plugin));
                return true;
            } else return false;
        });

        generalPluginLoader("lands","Lands",(pluginName) -> {
            Plugin plugin;
            if ((plugin = tryGetPlugin(pluginName)) != null) {
                restrictors.add(new LrLands(plugin,ownerPlugin));
                return true;
            }else return false;
        });

        // End support additions.
        logger.log(Level.INFO, "Loaded support for " + restrictors.size() + " compatible land-claim type plugins.");
        return restrictors.toArray(LocationRestrictor[]::new);
    }

    /**
     * A utility method for just to make loading and specifically logging easier. Essentially, all this method does is
     * run the given inside a try/catch and log if everything worked or not. Also, the `settingName` lets us look up if
     * we should/should-not enable the support.
     *
     * @param settingName The config-name of the plugin in the JRTP config to know if we should or should not run this.
     * @param pluginName  The name of the plugin, used to find the instance of the plugin.
     * @param supportInit A function which attempts to load and enable support for the named plugin.
     *                    This function is allowed to throw exceptions if anything goes wrong.
     *                    Note: Return value tells us if we successfully logged the thing or not.
     */
    private void generalPluginLoader(String settingName, String pluginName, Function<String, Boolean> supportInit) {
        if (configurationSection == null || configurationSection.getBoolean(settingName, true)) {
            try {
                if (supportInit.apply(pluginName))
                    logger.log(Level.INFO, "Found '" + pluginName + "'. Enabling support.");
            } catch (Exception e) {
                logger.log(Level.WARNING, "Tried to load plugin '" + pluginName + "' but failed with error message:");
                e.printStackTrace();
            }
        } else logger.log(Level.INFO, "Found '" + pluginName + "' but did not load support. (config said do not load)");
    }

}
