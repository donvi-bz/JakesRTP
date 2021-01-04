package biz.donvi.jakesRTP;

import io.papermc.lib.PaperLib;
import org.bukkit.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

import static biz.donvi.jakesRTP.PluginMain.infoLog;
import static biz.donvi.jakesRTP.PluginMain.plugin;


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
    protected Material getLocMaterial(Location loc) throws JrtpBaseException.PluginDisabledException {
        return SafeLocationUtils.util.locMatFromSnapshot(loc, getChunkForLocation(loc));
    }

    @Override
    protected void dropToGround() throws JrtpBaseException.PluginDisabledException {
        SafeLocationUtils.util.dropToGround(loc, lowBound, getChunkForLocation(loc));
    }

    @Override
    protected void dropToMiddle() throws JrtpBaseException.PluginDisabledException {
        SafeLocationUtils.util.dropToMiddle(loc, lowBound, highBound, getChunkForLocation(loc));
    }

    private ChunkSnapshot getChunkForLocation(Location loc)
    throws JrtpBaseException.PluginDisabledException, IllegalStateException {
        String chunkKey = loc.getChunk().getX() + " " + loc.getChunk().getZ();
        ChunkSnapshot chunkSnapshot = chunkSnapshotMap.get(chunkKey);
        if (chunkSnapshot != null) return chunkSnapshot;
        try {
            long maxTime = System.currentTimeMillis() + timeout * 1000L; // timeout is in seconds
            boolean keepTrying;
            // Any call to load a chunk, whether sync or async, must come from the main thread. Since here we want to
            // load a chunk asynchronously, we first have to hop to the main thread, then have the main thread call
            // the async method to get the chunk. Now, this would be easy enough, and could be done with this code:
            //         chunkSnapshot = Bukkit.getScheduler().callSyncMethod(
            //             PluginMain.plugin,
            //             () -> PaperLib.getChunkAtAsync(loc).thenApply(Chunk::getChunkSnapshot)
            //         ).get(timeout, TimeUnit.SECONDS).get(timeout, TimeUnit.SECONDS));
            // but taking this approach leads to issues shutting down the request as we either need to wait for the
            // the timeout to be reached, or let the server forcefully kill it (and send us an annoying message in
            // console). To get around this, we break up the two gets into their own sections, and check every few
            // milliseconds if we can retrieve it, or if we should give up.
            CompletableFuture<ChunkSnapshot> getChunkSnapshotFuture = null;
            Future<CompletableFuture<ChunkSnapshot>> callSyncFuture =
                Bukkit.getScheduler().callSyncMethod(
                    PluginMain.plugin,
                    () -> PaperLib.getChunkAtAsync(loc).thenApply(Chunk::getChunkSnapshot));
            // Looks to get the result of `callSyncFuture` which will be the value of `getChunkSnapshotFuture`
            while (keepTrying = keepTrying(maxTime))
                if (callSyncFuture.isDone()) {
                    getChunkSnapshotFuture = callSyncFuture.get();
                    break;
                } else synchronized (this) {
                    wait(100);
                }
            // Looks to get the result of `getChunkSnapshotFuture` which is the value of `chunkSnapshot`
            while (keepTrying = keepTrying(maxTime) && keepTrying)
                if (getChunkSnapshotFuture.isDone()) {
                    chunkSnapshot = getChunkSnapshotFuture.get();
                    break;
                } else synchronized (this) {
                    wait(100);
                }
            // If we made it this far and still want to keep trying, then `chunkSnapshot` has a value
            if (keepTrying) chunkSnapshotMap.put(chunkKey, chunkSnapshot); // Save value for later
                // If not, we want to throw an exception to get us all the way back home (so we can exit)
            else throw new JrtpBaseException.PluginDisabledException();
        } catch (CancellationException ignored) {
            throw new JrtpBaseException.PluginDisabledException();
        } catch (InterruptedException e) {
            infoLog("Caught an unexpected interrupt.");
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return chunkSnapshot;
    }

    private boolean keepTrying(long maxTime) {
        return System.currentTimeMillis() < maxTime && plugin.isEnabled() && !plugin.disabling();
    }
}
