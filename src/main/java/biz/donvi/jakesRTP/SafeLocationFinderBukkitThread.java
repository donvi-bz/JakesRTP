package biz.donvi.jakesRTP;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import static biz.donvi.jakesRTP.PluginMain.logger;
import static biz.donvi.jakesRTP.SafeLocationUtils.requireMainThread;

public class SafeLocationFinderBukkitThread extends SafeLocationFinder {

    public SafeLocationFinderBukkitThread(final Location loc) { super(loc); }

    public SafeLocationFinderBukkitThread(final Location loc, int checkRadiusXZ, int checkRadiusVert, int lowBound) {
        super(loc, checkRadiusXZ, checkRadiusVert, lowBound);
    }

    @Override
    protected Material getLocMaterial(Location loc) throws Exception {
        requireMainThread();
        return loc.getBlock().getType();
    }

    @Override
    protected void dropToGround() throws Exception {
        requireMainThread();
        SafeLocationUtils.dropToGround(loc, lowBound);
    }

    @Override
    protected void dropToMiddle() throws Exception {
        requireMainThread();
        SafeLocationUtils.dropToMiddle(loc, lowBound, 96/*Todo: set value in config*/);
    }

}
