package biz.donvi.jakesRTP;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CmdRtp implements CommandExecutor {

    private final RandomTeleporter randomTeleporter;

    public CmdRtp(RandomTeleporter randomTeleporter) {this.randomTeleporter = randomTeleporter;}

    /**
     * This is called when a player runs the in-game "/rtp" command.
     * Anything (except errors) that directly deals with the player is done here.
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try {
            if (args.length == 0 && sender instanceof Player) {
                Player player = (Player) sender;
                long callTime = System.currentTimeMillis();
                CoolDownTracker coolDownTracker = randomTeleporter.getRtpSettingsByWorldForPlayer(player).coolDown;
                if (player.hasPermission("jakesRtp.noCooldown") || coolDownTracker.check(player.getName())) {
                    new RandomTeleportAction(
                            randomTeleporter,
                            randomTeleporter.getRtpSettingsByWorldForPlayer(player),
                            player.getLocation(),
                            true,
                            true,
                            randomTeleporter.logRtpOnCommand, "Rtp-from-command triggered!"
                    ).teleportAsync(player);
                    coolDownTracker.log(player.getName(), callTime);
                } else {
                    player.sendMessage(Messages.NEED_WAIT_COOLDOWN.format(coolDownTracker.timeLeftWords(player.getName())));
                }
            }
        } catch (NotPermittedException npe) {
            sender.sendMessage(Messages.NP_GENERIC.format(npe.getMessage()));
        } catch (Exception e) {
            sender.sendMessage(Messages.NP_UNEXPECTED_EXCEPTION.format(e.getMessage()));
            e.printStackTrace();
        }
        return true;
    }
}
