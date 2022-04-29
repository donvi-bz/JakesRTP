package biz.donvi.jakesRTP.claimsIntegrations;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

public class LrTowny implements LocationRestrictor{
    protected Towny plugin;

    public LrTowny(Towny towny){
        this.plugin = towny;
    }

    @Override
    public Plugin supporterPlugin() {
        return plugin;
    }

    @Override
    public boolean denyLandingAtLocation(Location location) {
        return !TownyAPI.getInstance().isWilderness(location);
    }
}
