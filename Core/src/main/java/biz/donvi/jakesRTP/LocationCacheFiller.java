package biz.donvi.jakesRTP;

import org.bukkit.Bukkit;

import java.lang.ref.WeakReference;
import java.util.logging.Level;

import static biz.donvi.jakesRTP.JakesRtpPlugin.infoLog;
import static biz.donvi.jakesRTP.JakesRtpPlugin.log;


public class LocationCacheFiller implements Runnable {

    private final long recheckTime;
    private final long betweenTime;

    private final WeakReference<JakesRtpPlugin> pluginReference;

    private boolean keepRunning = true;

    public LocationCacheFiller(JakesRtpPlugin jakesRtpPlugin, long recheckTime, long betweenTime) {
        pluginReference = new WeakReference<>(jakesRtpPlugin);
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
            int issueCounter = 0, issueCounterMax = 10;
            while (keepRunning && isPluginLoaded()) {
                for (RtpProfile settings : getCurrentRtpSettings()) {
                    if (!keepRunning) break;
                    else if (settings.canUseLocQueue)
                        try {
                            pluginMain().getRandomTeleporter().fillQueue(settings);
                        } catch (JrtpBaseException ex) {
                            issueCounter += 2;
                            if (ex instanceof JrtpBaseException.PluginDisabledException) throw ex;
                            if (ex instanceof JrtpBaseException.NotPermittedException)
                                infoLog(
                                    "An exception has occurred that should be impossible to occur. Please report this" +
                                    ".");
                            else if (issueCounter < issueCounterMax)
                                infoLog("Something has gone wrong, but this is most likely not an issue.");
                            else
                                infoLog("Something has gone wrong multiple times, you may want to report this.");

                            log(Level.WARNING, "Error filling cache for " + settings.name + ".");
                            ex.printStackTrace();
                        }
                }
                if (issueCounter > 0) issueCounter--;

                patientlyWait(recheckTime);
            }
        } catch (JrtpBaseException.PluginDisabledException ignored) {
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
    private RtpProfile[] getCurrentRtpSettings() {
        //noinspection ConstantConditions
        return pluginReference.get().getRandomTeleporter().getRtpSettings().toArray(new RtpProfile[0]);
    }

    /**
     * Waits for the specified amount of time, returning either then the time has passed or an interrupt has occurred.
     */
    private synchronized void patientlyWait(long milliseconds) {
        if (!keepRunning) return;
        try {
            wait(milliseconds);
        } catch (InterruptedException ignored) {}
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
    private JakesRtpPlugin pluginMain() throws ReferenceNonExistentException {
        if (pluginReference.get() == null) throw new ReferenceNonExistentException();
        else return pluginReference.get();
    }


    /**
     * Exists for the sole purpose of identifying when we try to get the plugin's reference, but it is null
     */
    private static class ReferenceNonExistentException extends Exception {}
}
