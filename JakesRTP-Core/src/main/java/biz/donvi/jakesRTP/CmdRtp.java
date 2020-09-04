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
                    player.sendMessage("Need to wait for cooldown: " + coolDownTracker.timeLeftWords(player.getName()));
                }
            }
        } catch (NotPermittedException npe) {
            sender.sendMessage("Could not RTP for reason: " + npe.getMessage());
        } catch (Exception e) {
            sender.sendMessage("Error. Could not RTP for reason: " + e.getMessage());
            sender.sendMessage("Please check console for more info on why teleportation failed.");
            e.printStackTrace();
        }
        return true;
    }
}
