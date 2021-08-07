package biz.donvi.jakesRTP.claimsIntegrations;

import biz.donvi.jakesRTP.JakesRtpPlugin;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class ClaimsManager {
    protected final Plugin               ownerPlugin;
    protected       LocationRestrictor[] locationRestrictors;

    public ClaimsManager(Plugin ownerPlugin) {
        this.ownerPlugin = ownerPlugin;
        locationRestrictors = tryMakeLocationRestrictor();

    }

    public boolean isInside(Location loc) {
        for (var restrictor : locationRestrictors)
            if (restrictor.isInside(loc))
                return true;
        return false;
    }


    private Plugin tryGetPlugin(String name) {return ownerPlugin.getServer().getPluginManager().getPlugin(name);}

    private LocationRestrictor[] tryMakeLocationRestrictor() {
        List<LocationRestrictor> restrictors = new ArrayList<>();
        Plugin plugin;
        // GriefPrevention support!
        try {
            if ((plugin = tryGetPlugin(CmGriefPrevention.PluginName)) != null)
                restrictors.add(new CmGriefPrevention(ownerPlugin, (GriefPrevention) plugin));
        } catch (Exception e) {
            JakesRtpPlugin.log(
                Level.WARNING,
                "Tried to load plugin " + CmGriefPrevention.PluginName + " but failed with error message:");
            e.printStackTrace();
        }
        // End support additions.
        return restrictors.toArray(LocationRestrictor[]::new);
    }

}
