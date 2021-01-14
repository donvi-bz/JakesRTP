package biz.donvi.jakesRTP;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.ArrayList;
import java.util.List;

public class CmdRtp implements TabExecutor {

    private final RandomTeleporter randomTeleporter;

    public CmdRtp(RandomTeleporter randomTeleporter) { this.randomTeleporter = randomTeleporter; }

    /**
     * This is called when a player runs the in-game "/rtp" command.
     * Anything (except errors) that directly deals with the player is done here.
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try {
            if ((args.length == 0 || args.length == 1) && sender instanceof Player) {
                Player player = (Player) sender;
                if (args.length == 1 && !sender.hasPermission("jakesrtp.usebyname"))
                    return false;
                RtpSettings relSettings = args.length == 0
                    ? randomTeleporter.getRtpSettingsByWorldForPlayer(player)
                    : randomTeleporter.getRtpSettingsByNameForPlayer(player, args[0]);
                if (player.hasPermission("jakesrtp.nocooldown") // If the player has permission to skip cooldown
                    || player.hasPermission("jakesrtp.nocooldown." + relSettings.name.toLowerCase())
                    || relSettings.coolDown.check(player.getName())) { // Or they are not on cooldown
                    if (!randomTeleporter.playersInWarmup.containsKey(player.getUniqueId())) {
                        final boolean warmup =
                            relSettings.warmupEnabled && // Obvious...
                            !player.hasPermission("jakesrtp.nowarmup") && // Or if they have the perm to avoid it
                            !player.hasPermission("jakesrtp.nowarmup." + relSettings.name.toLowerCase());
                        final Runnable execRtp = makeRunnable(player, relSettings, warmup);
                        if (warmup) { // If there is a warmup, schedule the runnable
                            final int taskID = sender
                                .getServer().getScheduler() // Get the task ID so we can cancel it later.
                                .scheduleSyncRepeatingTask(JakesRtpPlugin.plugin, execRtp, 2, 20);
                            if (taskID == -1) // This should only really happen during shutdown.
                                throw new JrtpBaseException("Could not schedule rtp-after-warmup.");
                            randomTeleporter.playersInWarmup.put(player.getUniqueId(), taskID); // Needed for canceling.
                        } else execRtp.run(); // Just run the task now, none of this "schedule for later nonsense"
                    } else player.sendMessage(Messages.WARMUP_RTP_ALREADY_CALLED.format());
                } else player.sendMessage(Messages.
                    NEED_WAIT_COOLDOWN.format(
                    relSettings.coolDown.timeLeftWords(player.getName())
                ));
            }
        } catch (JrtpBaseException.NotPermittedException npe) {
            sender.sendMessage(Messages.NP_GENERIC.format(npe.getMessage()));
        } catch (JrtpBaseException e) {
            sender.sendMessage(e.getMessage());
            e.printStackTrace();
        }
        return true;
    }

    private Runnable makeRunnable(final Player player, final RtpSettings rtpSettings, boolean calculatedWarmup) {
        return new Runnable() {
            private final BukkitScheduler scheduler = player.getServer().getScheduler();
            private final Location startLoc = player.getLocation().clone();
            private final boolean warmup = calculatedWarmup;
            private final long startTime = System.currentTimeMillis();
            private int done = 0;

            @Override
            public void run() {
                // The annoying error message, lets hope we never need use this...
                if (done > 1) taskError();
                // If There should be no warmup, we teleport the user immediately.
                else if (!warmup) teleport();
                // If we want the user to stand still AND they move, we cancel this runnable / future rtp.
                else if (rtpSettings.warmupCancelOnMove && startLoc.distance(player.getLocation()) > 1) cancel();
                // If we have waited enough time, we teleport the user.
                else if (timeDifInSeconds() >= rtpSettings.warmup) teleport();
                // If we got to this point, the user still has to wait, and if wanted, we let them know how long.
                else if (rtpSettings.warmupCountDown) countDown();
                // If none of these were called, we just silently wait until the next time run() is called.
            }

            private int timeDifInSeconds() {
                return (int) ((System.currentTimeMillis() - startTime) / 1000);
            }

            private void countDown() {
                player.sendMessage(Messages.
                    WARMUP_TELEPORTING_IN_X.format(
                    rtpSettings.warmup - timeDifInSeconds()
                ));
            }

            private void teleport() {
                try {
                    rtpSettings.coolDown.log(player.getName(), System.currentTimeMillis());
                    new RandomTeleportAction(
                        randomTeleporter,
                        rtpSettings,
                        player.getLocation(),
                        true,
                        true,
                        randomTeleporter.logRtpOnCommand, "Rtp-from-command triggered!"
                    ).teleportAsync(player);
                } catch (Exception e) {
                    player.sendMessage(Messages.NP_UNEXPECTED_EXCEPTION.format(e.getMessage()));
                    e.printStackTrace();
                } finally {
                    cancelTask();
                }
            }

            private void cancel() {
                player.sendMessage(Messages.WARMUP_CANCEL_BECAUSE_MOVE.format());
                cancelTask();
            }

            private void cancelTask() {
                done++;
                Integer taskID = randomTeleporter.playersInWarmup.remove(player.getUniqueId());
                if (taskID != null)
                    scheduler.cancelTask(taskID); // Only cancel if task existed.
            }

            private void taskError() {
                if (done > 1000) scheduler.cancelTasks(JakesRtpPlugin.plugin); // Emergency cleanup.
                if (done < 10 || done % 100 == 0) throw new RuntimeException("RTP task run twice?? Please report.");
                done++; // This is meant to be annoying, but not *too* annoying.
            }
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        ArrayList<String> validSearches = new ArrayList<>();
        if (args.length == 0 ||
            !(sender instanceof Player) ||
            !sender.hasPermission("jakesrtp.usebyname")
        ) return validSearches;
        for (String name : randomTeleporter.getRtpSettingsNamesForPlayer((Player) sender))
            if (name.contains(args[0]))
                validSearches.add(name);
        return validSearches;
    }
}
