package biz.donvi.jakesRTP.claimsIntegrations;

import biz.donvi.jakesRTP.JakesRtpPlugin;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClaimsManager {
    protected final Plugin               ownerPlugin;
    protected       LocationRestrictor[] locationRestrictors;
    protected       ConfigurationSection configurationSection;

    public ClaimsManager(Plugin ownerPlugin, ConfigurationSection configSection) {
        this.ownerPlugin = ownerPlugin;
        locationRestrictors = tryMakeLocationRestrictor();
        configurationSection = configSection;

    }

    public boolean isInside(Location loc) {
        for (var restrictor : locationRestrictors)
            if (restrictor.isInside(loc))
                return true;
        return false;
    }


    private Plugin tryGetPlugin(String name) {return ownerPlugin.getServer().getPluginManager().getPlugin(name);}

    private LocationRestrictor[] tryMakeLocationRestrictor() {
        Logger l = ownerPlugin.getLogger();
        l.log(Level.INFO, "Looking for compatible land claim plugins...");
        List<LocationRestrictor> restrictors = new ArrayList<>();
        Plugin plugin;
        String pName = "";
        // GriefPrevention support!
        if (configurationSection.getBoolean("grief-prevention", true)) {
            try {
                pName = CmGriefPrevention.PluginName;
                if ((plugin = tryGetPlugin(pName)) != null) {
                    restrictors.add(new CmGriefPrevention(ownerPlugin, (GriefPrevention) plugin));
                    l.log(Level.INFO, "Found '" + pName + "'. Enabling support.");
                }
            } catch (Exception e) {
                l.log(Level.WARNING, "Tried to load plugin '" + pName + "' but failed with error message:");
                e.printStackTrace();
            } finally {pName = "";}
        } else l.log(Level.INFO, "Found '" + pName + "' but did not load support. (config said do not load)");
        // End support additions.
        l.log(Level.INFO, "Loaded support for " + restrictors.size() + " compatible land-claim type plugins.");
        return restrictors.toArray(LocationRestrictor[]::new);
    }

}
