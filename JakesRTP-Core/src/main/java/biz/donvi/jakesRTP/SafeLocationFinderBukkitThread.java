package biz.donvi.jakesRTP;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import static biz.donvi.jakesRTP.SafeLocationUtils.requireMainThread;

public class SafeLocationFinderBukkitThread extends SafeLocationFinder {

    public SafeLocationFinderBukkitThread(final Location loc) { super(loc); }

    public SafeLocationFinderBukkitThread(final Location loc, int checkRadiusXZ, int checkRadiusVert,
                                          int lowBound, int highBound) {
        super(loc, checkRadiusXZ, checkRadiusVert, lowBound, highBound);
    }

    @Override
    protected Material getLocMaterial(Location loc) throws Exception {
        requireMainThread();
        return loc.getBlock().getType();
    }

    @Override
    protected void dropToGround() throws Exception {
        requireMainThread();
        SafeLocationUtils.util.dropToGround(loc, lowBound);
    }

    @Override
    protected void dropToMiddle() throws Exception {
        requireMainThread();
        SafeLocationUtils.util.dropToMiddle(loc, lowBound, highBound);
    }

}
