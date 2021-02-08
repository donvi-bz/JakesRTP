package biz.donvi.jakesRTP;

import org.bukkit.Server;
import org.bukkit.plugin.Plugin;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Queue;

/**
 * A simple utility for measuring server lag over a time span. <p>
 * There is also a static method for convenience that will block the calling thread until the average time dips below
 * {@code MIN_LAG_TIME}.<p>
 * --------<p>
 * This class is described as 'simple' is because it was designed with a specific purpose in mind:
 * to allow the plugin to wait until lag as stopped before continuing. This was written expecting no more than one
 * {@code SimpleLagTimer} to exist at a time, though it perfectly handles multiple, it just makes no attempt to do so
 * efficiently. Also, it is not made with a 'perfect' readout of 'lag' because, as it turns out, the definition of 'lag'
 * can be a bit subjective. Example: Say we want to measure the tick time over 50 ticks and average it. If a lag spike
 * happens 2 ticks in and lasts for 100ms, then the next few ticks will run as fast as possible making the average tick
 * time stay at exactly 50ms despite the clear lag. For this timer, I get around that by ignoring any readout under
 * {@code MIN_REC_TIME} milliseconds because I assume those values happened to correct for lag.
 */
public class SimpleLagTimer {

    /**
     * The minimum time (in milliseconds) that a tick can take to be counted as an actual tick time. <p>
     * ANY TICKS LESS THAN OR EQUAL TO THIS WILL BE DISCARDED as they are considered "make up ticks"  (ticks after a
     * spike in lag that are only quick to make up for lost time)
     */
    protected static int MIN_REC_TIME         = 48;
    /**
     * The time (in milliseconds) that a tick must be equal to or less than to be considered lag free.
     */
    protected static int MIN_LAG_TIME         = 55;
    /**
     * Applicable only to the static {@code blockingTimer()} method, this is how many ticks to wait regardless of
     * whether the server is lagging or not.
     */
    protected static int TICKS_TO_ALWAYS_WAIT = 20;


    protected final Server          server;
    protected final Queue<TickInfo> tickInfoQueue;
    protected final long            startTime;
    final           int             taskNumber;

    protected TickInfo lastTick;
    protected long     tickQueueTimeSum;
    protected boolean  running = true;

    /**
     * Constructs a new simple lag timer. This registers a repeating task in the bukkit scheduler to measure how long
     * each tick takes.
     *
     * @param plugin   The plugin that created this object. This plugin will be used to register the task.
     * @param avgRange The range (in milliseconds) of ticks that will stay logged. <p>
     *                 Example: If {@code avgRange = 100}, the lengths of all ticks that started in the last 100ms will
     *                 stay logged.
     */
    @SuppressWarnings("ConstantConditions") // tickInfoQueue.peek() & tickInfoQueue.pull() are never null when checked.
    public SimpleLagTimer(Plugin plugin, int avgRange) {
        this.server = plugin.getServer();
        this.tickInfoQueue = new LinkedList<>();
        lastTick = new TickInfo(0);
        startTime = System.currentTimeMillis();
        taskNumber = server.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            TickInfo currentTick = new TickInfo(
                System.currentTimeMillis(),
                System.currentTimeMillis() - lastTick.timeOf,
                lastTick.number + 1);
            lastTick = currentTick;
            TickInfo oldestTick;

            if (currentTick.timeDif > MIN_REC_TIME) {
                tickInfoQueue.add(currentTick);
                tickQueueTimeSum += currentTick.timeDif;
            }
            while (tickInfoQueue.size() > 0 && System.currentTimeMillis() - tickInfoQueue.peek().timeOf > avgRange) {
                oldestTick = tickInfoQueue.poll();
                tickQueueTimeSum -= oldestTick.timeDif;
            }
            synchronized (this) {
                this.notifyAll();
            }
//            gnuPlotHook(currentTick); //DEBUG gnuPlotter line
        }, 0, 1);
    }

    /**
     * Gets the current tick the timer is on. Note: A tick with value {@code 0} represents the first tick this timer
     * logged, <em>not</em> the first server tick.
     *
     * @return The current tick's number.
     */
    public long getTickNumber() {
        TickInfo lastInserted = ((Deque<TickInfo>) tickInfoQueue).peekLast();
        return lastInserted != null ? lastInserted.number : 0;
    }

    /**
     * Gets the average tick time over the last {@code avgRange} milliseconds (where {@code avgRange} is a value passed
     * on construction).
     *
     * @return The average tick time over the last {@code avgRange} milliseconds.
     */
    public float getAverageTick() {
        return tickInfoQueue.size() == 0
            ? Float.MAX_VALUE
            : (float) tickQueueTimeSum / tickInfoQueue.size();
    }

    /**
     * Is this tick timer still running?
     *
     * @return Whether the tick timer is still running.
     */
    public boolean isRunning() { return running; }

    /**
     * Stops the tick timer and removes the task from the bukkit scheduler. Make sure to run this method before
     * discarding the {@code SimpleTickTimer} object.
     */
    public void end() {
        server.getScheduler().cancelTask(taskNumber);
        running = false;
//        gnuPlotEnd(); //DEBUG gnuPlotter line
    }

    //<editor-fold desc="GNU PLOT HOOKS">
//    private final ArrayList<double[]> gnuPlotData = new ArrayList<>();
//
//    private void gnuPlotHook(TickInfo tick) {
//        gnuPlotData.add(new double[]{
//                tick.timeOf - startTime,
//                tick.number,
//                tick.timeDif,
//                (double) tickQueueTimeSum / (double) tickInfoQueue.size()
//        });
//    }
//
//    private void gnuPlotEnd() {
//        new Plotter("C:/Program Files/gnuplot/bin/wgnuplot.exe", 10, 10)
//                .writeData(gnuPlotData.toArray(new double[0][])).plot(false);
//    }
    //</editor-fold>

    /**
     * Creates a simple tick timer and blocks the calling thread until the tick timer reads a value under {@code
     * MIN_LAG_TIME} milliseconds. Note: This will always block for at least {@code TICKS_TO_ALWAYS_WAIT} ticks.
     *
     * @param plugin   The plugin used to register a task.
     * @param avgRange The range (in milliseconds) given to the tick timer to average. Example: {@code 250} would make
     *                 the tick timer average together all ticks that started less than 250ms ago.
     */
    public static void blockingTimer(Plugin plugin, int avgRange) {
        final SimpleLagTimer simpleLagTimer;
        synchronized (simpleLagTimer = new SimpleLagTimer(plugin, avgRange)) {
            while (simpleLagTimer.getTickNumber() < TICKS_TO_ALWAYS_WAIT) {
                try {
                    simpleLagTimer.wait(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            while (simpleLagTimer.isRunning() && simpleLagTimer.getAverageTick() > MIN_LAG_TIME) {
                try {
                    simpleLagTimer.wait(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        simpleLagTimer.end();
    }

    /**
     * Simple data storage object for tick time and duration.
     */
    private static class TickInfo {
        /**
         * The time that the tick started (system time; milliseconds)
         */
        final long timeOf;
        /**
         * The length of the tick (in milliseconds)
         */
        final long timeDif;
        /**
         * The ticks number (where 0 is the first tick measured by the tick timer)
         */
        final long number;

        /**
         * Constructor for a tick that started at this instant (and thus can have no length yet)
         *
         * @param tickNum The number of the tick
         */
        TickInfo(long tickNum) {
            timeOf = System.currentTimeMillis();
            timeDif = -1;
            number = tickNum;
        }

        /**
         * Constructor for a tick after it has been measured.
         *
         * @param timeOf  The time the tick started (system time; ms)
         * @param timeDif The length of the tick (in milliseconds)
         * @param tickNum The number of the tick since the start of the tick timer
         */
        TickInfo(long timeOf, long timeDif, long tickNum) {
            this.timeOf = timeOf;
            this.timeDif = timeDif;
            this.number = tickNum;
        }
    }
}

