package biz.donvi.jakesRTP;

import biz.donvi.jakesRTP.JrtpBaseException.PluginDisabledException;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

/**
 * An object that can be given a location in a spigot world, and will try
 * and move around until it finds a spot that a player can safely stand on.
 */
public abstract class SafeLocationFinder {

    public final    Location loc;
    protected final int      lowBound;
    protected       int      highBound;
    protected final boolean  enableSelfChecking;
    protected final int      checkRadiusXZ;
    protected final int      checkRadiusVert;

    /* Small set of variables just for nextInSpiral */
    protected boolean xNotY    = true;
    protected int     flip     = 1;
    protected int     count    = 0;
    protected int     subCount = 0;
    protected int     stretch  = 1;

    /**
     * Just constructs the {@code SafeLocationFinder}, use {@code checkSafety} to check if
     * the current location is safe, and use {@code nextInSpiral} to move on to the next location.
     *
     * @param loc The location that will be checked for safety, and potentially modified.
     */
    public SafeLocationFinder(Location loc) {
        this.loc = loc;
        enableSelfChecking = false;
        checkRadiusXZ = checkRadiusVert = lowBound = 0;
        highBound = 128; //Note: This is not ideal for all circumstances
    }

    /**
     * This constructs a fully managed {@code SafeLocationFinder}. Because the bounds for the check operations are
     * supplied, this object can check the safety of the location itself by calling instance method
     * {@code tryAndMakeSafe()}. The given location <b>will</b> be modified.
     *
     * @param loc             The location to try and make safe. This <b>will</b> be modified.
     * @param checkRadiusXZ   The distance out from the center that the location cam move.
     * @param checkRadiusVert The distance up and down that the location can move.
     * @param lowBound        The lowest Y value the location can have.
     * @param highBound       The highest Y value the location can have.
     */
    public SafeLocationFinder(Location loc, int checkRadiusXZ, int checkRadiusVert, int lowBound, int highBound) {
        this.loc = loc;
        enableSelfChecking = true;
        this.checkRadiusXZ = checkRadiusXZ;
        this.checkRadiusVert = checkRadiusVert;
        this.lowBound = lowBound;
        this.highBound = highBound;
    }

    /**
     * Attempts to make the location safe using the given bounds: {@code checkRadiusXZ}, {@code checkRadiusVert}, and
     * {@code lowBound}. If the location can be made safe, this method will return true. If not, it will return false.
     * Regardless of whether the method returns true or false, the location <b>will</b> be modified.<p>
     * Node: Trying to run this method on a {@code SafeLocationFinder} that was <em>not</em> passed the bounds on
     * construction will cause an Exception to be thrown.
     *
     * @param checkProfile Which method to use to find the starting point.
     * @return True if the location is now safe, false if it could not be made safe.
     */
    public boolean tryAndMakeSafe(LocCheckProfiles checkProfile) throws JrtpBaseException {
        try {
            if (!enableSelfChecking)
                throw new JrtpBaseException("Tried to use self checking on an object that can not self check.");

            moveToStart(checkProfile);

            for (int i = 0, spiralArea = (int) Math.pow(checkRadiusXZ * 2 + 1, 2); i < spiralArea; i++)
                if (checkSafety(checkRadiusVert)) {
                    loc.add(0.5, 1, 0.5);
                    loc.setYaw((float) (360f * Math.random()));
                    return true;
                } else nextInSpiral();
        } catch (TimeoutException e) {
            JakesRtpPlugin.log(Level.WARNING, "Request to make location safe timed out. " +
                                              "This is only an issue if this warning is common.");
        }
        return false;
    }

    /**
     * Checks the safety of the current location. If a location at a different height is safe,
     * the location stored in the {@code SafeLocationFinder} object will be updated.
     *
     * @param avm Allowed Vertical Movement - How much (up or down) can we look to find a safe spot.<p>
     *            Ex: if {@code avm = 1}, the block itself, 1 up, and 1 down will be checked.
     * @return True if the location is safe, false if it is not.
     */
    public final boolean checkSafety(int avm) throws JrtpBaseException.PluginDisabledException, TimeoutException {
        if (avm < 0) throw new IllegalArgumentException("Avm can not be less than 0.");
        // Make a temporary location so we don't edit the main one unless its safe.
        Location tempLoc = loc.clone().add(0, avm + 1, 0);

        // Since the actual number of blocks to check per location is 3,
        //   we need to start with our range at 3, and add from there.
        int range = avm * 2 + 3;
        int safe = 0;
        // We will ALWAYS loop at least 3 times, even if avm is 0.
        //   Two for air space, one for foot space.
        for (int i = 0; i < range; i++) {
            // Only check for safety if were within the valid range, otherwise only move the temp position
            if (tempLoc.getY() < lowBound) break; // We are too low and will never make it back to a valid height
            if (tempLoc.getY() < highBound) { // We are at a valid height. Do stuff...
                Material mat = getLocMaterial(tempLoc);
                // If either of these conditions are reached, it is not worth checking
                //   the remaining spaces because the combined result will fail.
                if ((i == range - 2 && safe == 0) ||
                    (i == range - 1 && safe != 2)) break;
                // This is the part that checks if a player can safely stand and fit.
                if (safe < 2)
                    if (SafeLocationUtils.util.isSafeToBeIn(mat))
                        safe++;
                    else safe = 0;
                else if (safe == 2)
                    if (SafeLocationUtils.util.isSafeToBeOn(mat)) {
                        loc.setX(tempLoc.getX());
                        loc.setY(tempLoc.getY());
                        loc.setZ(tempLoc.getZ());
                        return true;
                    } else if (!SafeLocationUtils.util.isSafeToBeIn(mat)) {
                        safe = 0;
                    }
            }
            // Move down one block, and we loop again
            tempLoc.add(0, -1, 0);
        }
        // We only make it here if no safe place was found
        return false;
    }

    /**
     * Moves the location to the starting position using one of the preset methods. <p>
     * This should only ever need to be called once.
     *
     * @param checkProfile The profile setting. This determines which method will be called.
     */
    private void moveToStart(LocCheckProfiles checkProfile) throws PluginDisabledException, TimeoutException {
        if (checkProfile == LocCheckProfiles.TOP_DOWN)
            dropToGround();
        else if (checkProfile == LocCheckProfiles.MIDDLE_OUT)
            dropToMiddle();
        else {
            assert checkProfile == LocCheckProfiles.AUTO;
            World world = loc.getWorld();
            if (world != null && world.getEnvironment() == World.Environment.NETHER) {
                if (highBound > 127) highBound = 127;
                dropToMiddle();
            } else dropToGround();
        }
    }

    /**
     * This method will MOVE the current location so that it spirals outwards from the initial location.
     */
    public final void nextInSpiral() {
        if (xNotY) {
            if (subCount++ < stretch)
                loc.add(flip, 0, 0);
            else {
                subCount = 0;
                xNotY = false;
                nextInSpiral();
            }
        } else {
            if (subCount++ < stretch)
                loc.add(0, 0, flip);
            else {
                stretch++;
                flip *= -1;
                subCount = 0;
                xNotY = true;
                nextInSpiral();
            }
        }
        count++;
    }

    /**
     * Gets the material of the location as if by {@code loc.getBlock().getType()}.
     *
     * @param loc The location to get the material for.
     */
    protected abstract Material getLocMaterial(Location loc) throws PluginDisabledException, TimeoutException;

    protected abstract void dropToGround() throws PluginDisabledException, TimeoutException;

    protected abstract void dropToMiddle() throws PluginDisabledException, TimeoutException;

    public enum LocCheckProfiles {AUTO, TOP_DOWN, MIDDLE_OUT}

}
