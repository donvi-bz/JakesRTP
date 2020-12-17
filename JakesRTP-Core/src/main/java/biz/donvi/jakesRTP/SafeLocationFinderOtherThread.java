package biz.donvi.jakesRTP;

import io.papermc.lib.PaperLib;
import org.bukkit.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static biz.donvi.jakesRTP.PluginMain.infoLog;


public class SafeLocationFinderOtherThread extends SafeLocationFinder {

    private final Map<String, ChunkSnapshot> chunkSnapshotMap = new HashMap<>();
    private final int                        timeout;

    /**
     * Just constructs the {@code SafeLocationFinder}, use {@code checkSafety} to check if
     * the current location is safe, and use {@code nextInSpiral} to move on to the next location.
     *
     * @param loc The location that will be checked for safety, and potentially modified.
     */
    public SafeLocationFinderOtherThread(Location loc) {
        super(loc);
        timeout = 5;
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
     * @param timeout         The max number of seconds to wait for data from another thread
     */
    public SafeLocationFinderOtherThread(
        Location loc, int checkRadiusXZ, int checkRadiusVert,
        int lowBound, int highBound, int timeout
    ) {
        super(loc, checkRadiusXZ, checkRadiusVert, lowBound, highBound);
        this.timeout = timeout;
    }

    /**
     * Gets the material of the location as if by {@code loc.getBlock().getType()}.<p>
     * Since this is the overridden version, we can not get the material the easy way.
     * Instead, we need to get and use a {@code ChunkSnapshot}.
     *
     * @param loc The location to get the material for.
     */
    @Override
    protected Material getLocMaterial(Location loc) throws TimeoutException, PluginDisabledException {
        return SafeLocationUtils.util.locMatFromSnapshot(loc, getChunkForLocation(loc));
    }

    @Override
    protected void dropToGround() throws TimeoutException, PluginDisabledException {
        SafeLocationUtils.util.dropToGround(loc, lowBound, getChunkForLocation(loc));
    }

    @Override
    protected void dropToMiddle() throws TimeoutException, PluginDisabledException {
        SafeLocationUtils.util.dropToMiddle(loc, lowBound, highBound, getChunkForLocation(loc));
    }

    private ChunkSnapshot getChunkForLocation(Location loc)
    throws TimeoutException, PluginDisabledException, IllegalStateException {
        String chunkKey = loc.getChunk().getX() + " " + loc.getChunk().getZ();
        ChunkSnapshot chunkSnapshot = chunkSnapshotMap.get(chunkKey);
        if (chunkSnapshot != null) return chunkSnapshot;
        try {
            // TODO - Don't run this code when the plugin is disabled. Oh, and deal with the consequences.
            //  Now make sure this solution works well.
            if (!PluginMain.plugin.isEnabled()) throw new PluginDisabledException();
            chunkSnapshotMap.put(
                chunkKey,
                chunkSnapshot = Bukkit.getScheduler().callSyncMethod(
                    PluginMain.plugin,
                    () -> PaperLib.getChunkAtAsync(loc).thenApply(Chunk::getChunkSnapshot)
                ).get(timeout, TimeUnit.SECONDS).get(timeout, TimeUnit.SECONDS));
//            PluginMain.infoLog("LOADED CHUNK SNAPSHOT USING PAPER");
        } catch (CancellationException ignored) {
            throw new PluginDisabledException();
        } catch (InterruptedException e) {
            infoLog("Caught an unexpected interrupt.");
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return chunkSnapshot;
    }

}
