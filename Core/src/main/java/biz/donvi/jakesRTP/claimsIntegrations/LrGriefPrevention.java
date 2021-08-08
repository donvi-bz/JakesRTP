package biz.donvi.jakesRTP.claimsIntegrations;

import biz.donvi.jakesRTP.JakesRtpPlugin;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import static org.bukkit.Bukkit.getServer;

public class LrGriefPrevention implements LocationRestrictor {
    protected GriefPrevention cmPlugin;

    public LrGriefPrevention(GriefPrevention cmPlugin) {
        this.cmPlugin = cmPlugin;
    }

    @Override
    public Plugin supporterPlugin() {return cmPlugin;}

    private Claim lastClaim = null;

    @Override
    public boolean denyLandingAtLocation(Location location) {
        Claim currentClaim = cmPlugin.dataStore.getClaimAt(location, true, lastClaim);
        if (currentClaim == null) {
            return false;
        } else {
            lastClaim = currentClaim;
            return true;
        }
    }
}
