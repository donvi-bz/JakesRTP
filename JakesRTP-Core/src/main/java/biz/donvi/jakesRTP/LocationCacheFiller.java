package biz.donvi.jakesRTP;

import org.bukkit.Bukkit;
import org.bukkit.World;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

//DEBUG ↓
import biz.donvi.gnuPlotter.Plotter;

import static org.bukkit.Bukkit.getServer;

public class LocationCacheFiller implements Runnable {

    // For convenience, 1000L * 60L is added so the last number can define minutes.
    private final long recheckTime;
    private final long betweenTime;

    private final WeakReference<PluginMain> pluginReference;

    private boolean keepRunning = true;

    public LocationCacheFiller(PluginMain pluginMain, long recheckTime, long betweenTime) {
        pluginReference = new WeakReference<>(pluginMain);
        this.recheckTime = recheckTime;
        this.betweenTime = betweenTime;
    }


    @Override
    public void run() {
        patientlyWait(5000);//Debug line, remove later.
        System.out.println("[J-RTP] LCF Started.");
        try {
//            waitNGoodTicks(300); TODO ITEM - shelved for later
            while (keepRunning && isPluginLoaded())
                try {
                    ArrayList<Integer> qFills = new ArrayList<>();
                    beginning:
                    for (RtpSettings settings : getCurrentRtpSettings()) {
                        for (World world : settings.getConfigWorlds())
                            if (!keepRunning) break beginning;
                            else if (!settings.forceDestinationWorld || settings.destinationWorld == world)
                                qFills.add(pluginMain().getRandomTeleporter().fillQueue(settings, world));
                    }
                    patientlyWait(recheckTime);
                } catch (JrtpBaseException ex) {
                    if (ex instanceof NotPermittedException)
                        PluginMain.infoLog("An exception has occurred that should be impossible to occur. Please report this.");
                    else
                        PluginMain.infoLog("Something has gone wrong, but this is most likely not an issue.");
                    ex.printStackTrace();
                }
        } catch (SafeLocationFinderOtherThread.PluginDisabledException ignored) {
            System.out.println("[J-RTP] Plugin disabled while finding a location. Location scrapped.");
        } catch (ReferenceNonExistentException ignored) {
            System.out.println("[J-RTP] Plugin no longer exists.");
        } catch (Exception e) {
            System.out.println("Something unexpected went wrong.");
            e.printStackTrace();
        }
        System.out.println("[J-RTP] Shutting location caching thread down.");
    }

    /**
     * Checks to see if the plugin is loaded. If the plugin is either
     * unloaded or no longer exists in memory, it will return false.
     *
     * @return True if the plugin is loaded and active
     */
    private boolean isPluginLoaded() {
        //noinspection ConstantConditions as the value is checked against null first
        return pluginReference.get() != null &&
               pluginReference.get().isEnabled();
    }

    /**
     * Gets an array of the current {@code RtpSettings} for the plugin.
     *
     * @return An array of the current {@code RtpSettings} for the plugin.
     */
    private RtpSettings[] getCurrentRtpSettings() {
        //noinspection ConstantConditions
        return pluginReference.get().getRandomTeleporter().getRtpSettings().toArray(new RtpSettings[0]);
    }

    /**
     * Waits for the specified amount of time, returning either then the time has passed or an interrupt has occurred.
     */
    private synchronized void patientlyWait(long milliseconds) {
        if (!keepRunning) return;
        try {
            wait(milliseconds);
        } catch (InterruptedException ignored) { }
    }

    /**
     * Tells the runnable that it should stop looping, and interrupts it so it can end early.
     */
    public void markAsOver() {
        keepRunning = false;
        syncNotify();
    }

    public synchronized void syncNotify() {
        this.notifyAll();
    }

    public void waitIfNonMainThread() {
        if (!Bukkit.isPrimaryThread())
            synchronized (this) {
                final long startTime = System.currentTimeMillis();
                long currentTime;
                while (keepRunning && (currentTime = System.currentTimeMillis()) - startTime < betweenTime)
                    //This is more complicated than necessary, but will not freeze up, nor wait too long.
                    patientlyWait((int) ((startTime + betweenTime - currentTime) * 0.95) + 1);
            }
    }
//
//    /**
//     * Waits for the specified number of ticks to pass where every tick is less than 50ms.
//     *
//     * @param ticks Number of <50ms ticks in a row to wait for.
//     */
//    private void waitNGoodTicks(long ticks) throws ReferenceNonExistentException {
//        final LocationCacheFiller lcf = this;
//        final ArrayList<double[]> points = new ArrayList<>(); //DEBUG REMOVE
//        final Queue<Integer> lastPointsQueue = new LinkedList<>(); //DEBUG REMOVE
//        final int[] runningTotal = {0, 0};//DEBUG REMOVE
//        final int[] goodTicks = {0, 0, 0}; //DEBUG This should only be a 1d array
//        final long[] lastTime = {System.currentTimeMillis()};
//        final int[] taskNumber = new int[1];
//        taskNumber[0] = pluginMain().getServer().getScheduler().scheduleSyncRepeatingTask(pluginMain(),
//                new Runnable() {
//                    @Override
//                    public void run() {
//                        long currentTime = System.currentTimeMillis();
//                        //DEBUG REMOVE LINE ↓
//                        long timeDif = currentTime - lastTime[0];
//                        if (lastPointsQueue.size() >= 100) {
//                            int lastPointPoll = lastPointsQueue.poll();
//                            runningTotal[0] -= lastPointPoll;
//                            runningTotal[1] -= lastPointPoll;
//                        }
//                        if (timeDif > 45) {
//                            lastPointsQueue.add((int) timeDif);
//                            runningTotal[0] += timeDif;
//                            runningTotal[1] += timeDif;
//                        }
//                        points.add(new double[]{
//                                (double) (timeDif),
//                                goodTicks[0],
//                                (double) runningTotal[0] / lastPointsQueue.size(),
//                                goodTicks[1],
//                                (double) runningTotal[1] / lastPointsQueue.size(),
//                                goodTicks[2]
//                        });
//                        if (runningTotal[0] / lastPointsQueue.size() < 55)
//                            goodTicks[1]++;
//                        else
//                            goodTicks[1] = 0;
//                        if (runningTotal[1] / lastPointsQueue.size() < 55)
//                            goodTicks[2]++;
//                        else
//                            goodTicks[2] = 0;
//                        //DEBUG REMOVE LINE ↑
//                        if (currentTime - lastTime[0] < 55)
//                            goodTicks[0]++;
//                        else
//                            goodTicks[0] = 0;
//                        if (goodTicks[0] > ticks) {
//                            getServer().getScheduler().cancelTask(taskNumber[0]);
//                            lcf.syncNotify();
//                        }
//                        lastTime[0] = currentTime;
//                    }
//                }, 0, 1);
//        System.out.println("starting wait");
//        patientlyWait(1000 * 60 * 2);
//        System.out.println("ending wait");
//        new Plotter("C:/Program Files/gnuplot/bin/wgnuplot.exe") //DEBUG REMOVE
//                .writeData(points.toArray(new double[0][])).plot(false);
//    }

    /**
     * Gets the plugin's reference when it exists, or throws {@code ReferenceNonExistentException} if it is not.
     *
     * @return A reference to the plugin.
     * @throws ReferenceNonExistentException when the plugin no longer exists.
     */
    private PluginMain pluginMain() throws ReferenceNonExistentException {
        if (pluginReference.get() == null) throw new ReferenceNonExistentException();
        else return pluginReference.get();
    }


    /**
     * Exists for the sole purpose of identifying when we try to get the plugin's reference, but it is null
     */
    private static class ReferenceNonExistentException extends Exception {}
}
