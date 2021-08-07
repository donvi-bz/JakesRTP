package biz.donvi.jakesRTP.claimsIntegrations;

import biz.donvi.jakesRTP.JakesRtpPlugin;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public class ClaimsManager {
    protected final Plugin             ownerPlugin;
    protected       LocationRestrictor theLocationRestrictor;

    public ClaimsManager(Plugin ownerPlugin) {
        this.ownerPlugin = ownerPlugin;
        theLocationRestrictor = tryMakeLocationRestrictor();

    }

    public boolean isInside(Location loc) {
        return theLocationRestrictor != null && theLocationRestrictor.isInside(loc);
    }


    private Plugin tryGetPlugin(String name) {return ownerPlugin.getServer().getPluginManager().getPlugin(name);}

    private LocationRestrictor tryMakeLocationRestrictor() {
        Plugin plugin;
        try {
            if ((plugin = tryGetPlugin(CmGriefPrevention.PluginName)) != null)
                return new CmGriefPrevention(ownerPlugin, (GriefPrevention) plugin);
        } catch (Exception e) {
            JakesRtpPlugin.log(
                Level.WARNING,
                "Tried to load plugin " + CmGriefPrevention.PluginName + " but failed with error message:");
            e.printStackTrace();
        }

        return null;
    }

}
