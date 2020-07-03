package biz.donvi.jakesRTP;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import java.util.logging.Level;

import static biz.donvi.jakesRTP.PluginMain.infoLog;

/**
 * An object that can be given a location in a spigot world, and will try
 * and move around until it finds a spot that a player can safely stand on.
 */
public class SafeLocationFinder {

    public Location loc;
    private final boolean enableSelfChecking;
    private final int
            checkRadiusXZ,
            checkRadiusVert,
            lowBound;

    /* Small set of variables just for nextInSpiral */
    private boolean xNotY = true;
    private int
            flip = 1,
            count = 0,
            subCount = 0,
            stretch = 1;

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
     */
    public SafeLocationFinder(Location loc, int checkRadiusXZ, int checkRadiusVert, int lowBound) {
        this.loc = loc;
        enableSelfChecking = true;
        this.checkRadiusXZ = checkRadiusXZ;
        this.checkRadiusVert = checkRadiusVert;
        this.lowBound = lowBound;
    }

    /**
     * Attempts to make the location safe using the given bounds: {@code checkRadiusXZ}, {@code checkRadiusVert}, and
     * {@code lowBound}. If the location can be made safe, this method will return true. If not, it will return false.
     * Regardless of whether the method returns true or false, the location <b>will</b> be modified.<p>
     * Node: Trying to run this method on a {@code SafeLocationFinder} that was <em>not</em> passed the bounds on
     * construction will cause an Exception to be thrown.
     *
     * @return True if the location is now safe, false if it could not be made safe.
     * @throws Exception if this method was called from an object with no checking bounds.
     */
    public boolean tryAndMakeSafe() throws Exception {
        if (!enableSelfChecking)
            throw new Exception("Tried to use self checking on an object that can not self check.");

        dropToGround(loc, lowBound);

        for (int i = 0, spiralArea = (int) Math.pow(checkRadiusXZ * 2 + 1, 2); i < spiralArea; i++)
            if (checkSafety(checkRadiusVert)) {
                loc.add(0.5, 1, 0.5);
                loc.setYaw((float) (360f * Math.random()));
                return true;
            } else nextInSpiral();
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
    public boolean checkSafety(int avm) {
        if (avm < 0) throw new IllegalArgumentException("Avm can not be less than 0.");
        //Make a temporary location so we don't edit the main one unless its safe.
        Location tempLoc = loc.clone().add(0, avm + 1, 0);

        //Since the actual number of blocks to check per location is 3,
        // we need to start with our range at 3, and add from there.
        int range = avm * 2 + 3;
        int safe = 0;
        //We will ALWAYS loop at least 3 times, even if avm is 0.
        // Two for air space, one for foot space.
        for (int i = 0; i < range; i++) {
            Material mat = tempLoc.getBlock().getType();
            //If either of these conditions are reached, it is not worth checking
            // the remaining spaces because the combined result will fail.
            if ((i == range - 2 && safe == 0) ||
                (i == range - 1 && safe != 2)) break;
            //This is the part that checks if a player can safely stand and fit.
            if (safe < 2)
                if (isSafeToBeIn(mat))
                    safe++;
                else safe = 0;
            else if (safe == 2)
                if (isSafeToBeOn(mat)) {
                    loc.setX(tempLoc.getX());
                    loc.setY(tempLoc.getY());
                    loc.setZ(tempLoc.getZ());
                    return true;
                } else if (!isSafeToBeIn(mat)) {
                    safe = 0;
                }
            //Move down one block, and we loop again
            tempLoc.add(0, -1, 0);
        }
        //We only make it here if no safe place was found
        return false;
    }


    /**
     * This method will MOVE the current location so that it spirals outwards from the initial location.
     */
    public void nextInSpiral() {
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
     * Checks the given material against a <u>whitelist</u> of materials deemed to be "safe to be in"
     *
     * @param mat The material to check
     * @return Whether it is safe or not to be there
     */
    static boolean isSafeToBeIn(Material mat) {
        switch (mat) {
            case AIR:
            case SNOW:
            case FERN:
            case LARGE_FERN:
            case VINE:
            case GRASS:
            case TALL_GRASS:
                return true;
            case WATER:
            case LAVA:
            case CAVE_AIR:
            default:
                return false;
        }
    }

    static boolean isSafeToGoThrough(Material mat) {
        switch (mat) {
            case ACACIA_LEAVES:
            case BIRCH_LEAVES:
            case DARK_OAK_LEAVES:
            case JUNGLE_LEAVES:
            case OAK_LEAVES:
            case SPRUCE_LEAVES:
                return true;
            default:
                return false;
        }
    }

    /**
     * Checks the given material against a <u>blacklist</u> of materials deemed to be "safe to be on"
     *
     * @param mat The material to check
     * @return Whether it is safe or not to be there
     */
    static boolean isSafeToBeOn(Material mat) {
        switch (mat) {
            case LAVA:
            case MAGMA_BLOCK:
            case WATER:
            case AIR:
            case CAVE_AIR:
            case VOID_AIR:
            case CACTUS:
            case SEAGRASS:
            case TALL_SEAGRASS:
            case LILY_PAD:
                return false;
            case GRASS_BLOCK:
            case STONE:
            case DIRT:
            default:
                return true;
        }
    }

    /**
     * Checks if the location is in a tree. To be in a tree, you must both be on a log, and in leaves.
     *
     * @param loc The location to check.
     * @return True if the location is in a tree.
     */
    static boolean isInATree(Location loc) {
        for (Material material : new Material[]{
                loc.clone().add(0, 1, 0).getBlock().getType(),
                loc.clone().add(0, 2, 0).getBlock().getType()})
            switch (material) {
                case ACACIA_LEAVES:
                case BIRCH_LEAVES:
                case DARK_OAK_LEAVES:
                case JUNGLE_LEAVES:
                case OAK_LEAVES:
                case SPRUCE_LEAVES:
                    return true;
            }
        return false;
    }

    /**
     * Takes the given location, and moves it downwards until it is no longer inside something that is
     * considered safe to be in by {@code isSafeToBeIn()}
     *
     * @param loc The location to modify
     */
    static void dropToGround(Location loc) {
        while (isSafeToBeIn(loc.getBlock().getType()) || isSafeToGoThrough(loc.getBlock().getType()))
            loc.add(0, -1, 0);
    }

    /**
     * Takes the given location, and moves it downwards until it is no longer inside something that is
     * considered safe to be in by {@code isSafeToBeIn()}
     *
     * @param loc      The location to modify
     * @param lowBound The lowest the location can go
     */
    static void dropToGround(Location loc, int lowBound) {
        while (loc.getBlockY() > lowBound &&
               (isSafeToBeIn(loc.getBlock().getType()) ||
                isSafeToGoThrough(loc.getBlock().getType()))
        ) loc.add(0, -1, 0);
    }
}
