package biz.donvi.jakesRTP.claimsIntegrations;

import biz.donvi.jakesRTP.JakesRtpPlugin;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import static org.bukkit.Bukkit.getServer;

public class CmGriefPrevention implements LocationRestrictor {
    public static final String PluginName = "GriefPrevention";

    protected Plugin          ownerPlugin;
    protected GriefPrevention cmPlugin;

    public CmGriefPrevention(Plugin ownerPlugin, GriefPrevention cmPlugin) {
        this.ownerPlugin = ownerPlugin;
        this.cmPlugin = cmPlugin;
    }

    @Override
    public Plugin claimPlugin() {return cmPlugin;}

    private Claim lastClaim = null;

    @Override
    public boolean isInside(Location location) {
        Claim currentClaim = cmPlugin.dataStore.getClaimAt(location, true, lastClaim);
        if (currentClaim == null) {
            return false;
        } else {
            lastClaim = currentClaim;
            return true;
        }
    }
}
