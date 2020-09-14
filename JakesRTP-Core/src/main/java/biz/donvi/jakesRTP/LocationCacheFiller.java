package biz.donvi.jakesRTP;

import org.bukkit.Bukkit;
import org.bukkit.World;

import java.lang.ref.WeakReference;

import static biz.donvi.jakesRTP.PluginMain.infoLog;

//DEBUG ↓


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
        final String threadOldName = Thread.currentThread().getName();
        Thread.currentThread().setName("[J-RTP] Loc Cache Filler");
        try {
            SimpleLagTimer.blockingTimer(pluginMain(), 5000);
            infoLog("[J-RTP] LCF Started.");
            while (keepRunning && isPluginLoaded())
                try {
                    beginning:
                    for (RtpSettings settings : getCurrentRtpSettings()) {
                        for (World world : settings.getConfigWorlds())
                            if (!keepRunning) break beginning;
                            else if (!settings.forceDestinationWorld || settings.destinationWorld == world)
                                pluginMain().getRandomTeleporter().fillQueue(settings, world);
                    }
                    patientlyWait(recheckTime);
                } catch (JrtpBaseException ex) {
                    if (ex instanceof NotPermittedException)
                        infoLog("An exception has occurred that should be impossible to occur. Please report this.");
                    else
                        infoLog("Something has gone wrong, but this is most likely not an issue.");
                    ex.printStackTrace();
                }
        } catch (SafeLocationFinderOtherThread.PluginDisabledException ignored) {
            infoLog("[J-RTP] Plugin disabled while finding a location. Location scrapped.");
        } catch (ReferenceNonExistentException ignored) {
            infoLog("[J-RTP] Plugin no longer exists.");
        } catch (Exception e) {
            infoLog("Something unexpected went wrong.");
            e.printStackTrace();
        }
        infoLog("[J-RTP] Shutting location caching thread down.");
        Thread.currentThread().setName(threadOldName);
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
