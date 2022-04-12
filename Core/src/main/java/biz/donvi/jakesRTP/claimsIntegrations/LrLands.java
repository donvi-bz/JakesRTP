package biz.donvi.jakesRTP.claimsIntegrations;

import me.angeschossen.lands.api.integration.LandsIntegration;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

public class LrLands implements LocationRestrictor{
    protected Plugin lands;
    protected final LandsIntegration landsIntegration;
    public LrLands(Plugin plugin,Plugin rtpPlugin){
        this.lands = plugin;
        landsIntegration = new LandsIntegration(rtpPlugin);
    }
    @Override
    public Plugin supporterPlugin() {
        return lands;
    }

    @Override
    public boolean denyLandingAtLocation(Location location) {
        return landsIntegration.isClaimed(location);
    }
}
