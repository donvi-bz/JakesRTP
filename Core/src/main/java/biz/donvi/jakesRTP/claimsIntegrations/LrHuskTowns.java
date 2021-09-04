package biz.donvi.jakesRTP.claimsIntegrations;

import me.william278.husktowns.HuskTownsAPI;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

public class LrHuskTowns implements LocationRestrictor {

    protected Plugin cmPlugin;

    public LrHuskTowns(Plugin cmPlugin) {
        this.cmPlugin = cmPlugin;
    }

    @Override
    public Plugin supporterPlugin() {
        return cmPlugin;
    }

    @Override
    public boolean denyLandingAtLocation(Location location) {
        return (!HuskTownsAPI.getInstance().isWilderness(location));
    }
}