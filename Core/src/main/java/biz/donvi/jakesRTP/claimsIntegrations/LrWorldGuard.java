package biz.donvi.jakesRTP.claimsIntegrations;

import biz.donvi.jakesRTP.JakesRtpPlugin;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import java.lang.ref.WeakReference;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LrWorldGuard implements LocationRestrictor {

    protected WorldGuardPlugin plugin;

    public LrWorldGuard(WorldGuardPlugin worldGuardPlugin) {
        if (worldGuardPlugin == null) throw new NullPointerException("worldGuardPlugin must NOT be null!");
        plugin = worldGuardPlugin;
    }

    @Override
    public Plugin supporterPlugin() {return plugin;}

    //<editor-fold desc="All this caching stuff I probably didn't actually need to write.">
    // Just some caching of values so quick usage doesn't need to keep finding them.
    // This is all probably unnecessary, I'm just afraid of persisting something from WorldGuard at the wrong time.
    private long                           maxAgeBeforeCacheClear  = 60 * 1000; // 60 seconds before we clear the cache
    private long                           lastTimeAccessed        = 0;
    private WeakReference<RegionContainer> regionContainer         = null;
    private WeakReference<World>           regionManagerIsForWorld = null;
    private WeakReference<RegionManager>   regionManagerWR         = null;

    /** Clears the temp variables if they are old, and registers the last time accessed as now.*/
    private void clearCacheIfOld() {
        if (System.currentTimeMillis() - lastTimeAccessed > maxAgeBeforeCacheClear) {
            regionContainer = null;
            regionManagerIsForWorld = null;
            regionManagerWR = null;
        }
        lastTimeAccessed = System.currentTimeMillis();
    }
    /** Gets a potentially cached regionContainer for the server */
    private RegionContainer getRegionContainer() {
        if (regionContainer == null || regionContainer.get() == null)
            regionContainer = new WeakReference<>(WorldGuard.getInstance().getPlatform().getRegionContainer());
        return regionContainer.get();
    }

    /** Gets the potentially cached region manager for the specified world */
    private RegionManager getRegionManager(World world) {
        clearCacheIfOld();
        // If the for-world thing is null or contains null, OR the region-manager is null or contains null
        if (regionManagerIsForWorld == null || regionManagerIsForWorld.get() != world ||
            regionManagerWR == null || regionManagerWR.get() == null
        ) { // If any of those, make these new.
            regionManagerIsForWorld = new WeakReference<>(world);
            regionManagerWR = new WeakReference<>(getRegionContainer().get(new BukkitWorld(world)));
        }
        return regionManagerWR.get();
    }
    //</editor-fold>

    @Override
    public boolean denyLandingAtLocation(Location location) {
        // Get a set of all regions at the location
        ApplicableRegionSet set = getRegionManager(location.getWorld()).getApplicableRegions(
            BlockVector3.at(location.getX(), location.getY(), location.getZ())
        );
        // And test if our flg is allowed.
        boolean flagStatus = set.testState(null, customJrtpFlag);
        return !flagStatus;
    }

    private static StateFlag customJrtpFlag = null;

    public static void registerWorldGuardFlag(Plugin plugin) {
        // ONLY RUN THIS IF WorldGuard DOES IN FACT EXIST
        if (plugin.getServer().getPluginManager().getPlugin("WorldGuard") == null) return;
        // Only needs to be done if it hasn't been done before.
        if (customJrtpFlag != null) return;
        // Actual logic.
        Logger logger = plugin.getLogger();
        FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
        try {
            StateFlag flag = new StateFlag("allow-jrtp-landing", true);
            registry.register(flag);
            customJrtpFlag = flag; // only set our field if there was no error
            logger.log(Level.INFO, "Added custom flag to world guard.");
        } catch (FlagConflictException e) {
            Flag<?> existing = registry.get("allow-jrtp-landing");
            if (existing instanceof StateFlag) {
                customJrtpFlag = (StateFlag) existing;
                logger.log(Level.INFO, "Custom flag was previously loaded.");
            } else {
                logger.log(Level.WARNING, "Could not create world-guard flag!");
            }
        }
    }
}
