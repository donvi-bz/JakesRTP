package biz.donvi.jakesRTP.claimsIntegrations;

import biz.donvi.jakesRTP.JakesRtpPlugin;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public interface LocationRestrictor {

    public Plugin claimPlugin();

    public boolean isInside(Location location);

}
