package biz.donvi.jakesRTP;

import org.bukkit.Location;
import org.bukkit.Material;

import static biz.donvi.jakesRTP.SafeLocationUtils.requireMainThread;

public class SafeLocationFinderBukkitThread extends SafeLocationFinder {

    public SafeLocationFinderBukkitThread(final Location loc) { super(loc); }

    public SafeLocationFinderBukkitThread(
        final Location loc, int checkRadiusXZ, int checkRadiusVert,
        int lowBound, int highBound
    ) { super(loc, checkRadiusXZ, checkRadiusVert, lowBound, highBound); }

    @Override
    protected Material getLocMaterial(Location loc) {
        requireMainThread();
        return loc.getBlock().getType();
    }

    @Override
    protected void dropToGround() {
        requireMainThread();
        SafeLocationUtils.util.dropToGround(loc, lowBound, highBound);
    }

    @Override
    protected void dropToMiddle() {
        requireMainThread();
        SafeLocationUtils.util.dropToMiddle(loc, lowBound, highBound);
    }

}
