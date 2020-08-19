package biz.donvi.jakesRTP;

import io.papermc.lib.PaperLib;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static biz.donvi.jakesRTP.PluginMain.infoLog;

public class CmdRtp implements CommandExecutor {

    private final RandomTeleporter rt;

    public CmdRtp(RandomTeleporter rt) {this.rt = rt;}

    /**
     * This is called when a player runs the in-game "/rtp" command.
     * Anything (except errors) that directly deals with the player is done here.
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try {
            /* - - - - - - - - - - - - - - - - - - - - - - - - - - *|
            |* - - - - If a player tries to RTP themselves - - - - *|
            |* - - - - - - - - - - - - - - - - - - - - - - - - - - */
            if (args.length == 0 && sender instanceof Player) {
                Player player = (Player) sender;
                long callTime = System.currentTimeMillis();

                CoolDownTracker coolDownTracker = rt.getRtpSettingsByWorldForPlayer(player).coolDown;
                if (player.hasPermission("jakesRtp.noCooldown") || coolDownTracker.check(player.getName())) {
                    long startTime = System.currentTimeMillis();

                    Location rtpLocation = rt.getRtpLocation(player, false, true);
                    PaperLib.teleportAsync(player, rtpLocation);
                    coolDownTracker.log(player.getName(), callTime);

                    long endTime = System.currentTimeMillis();
                    if (rt.logRtpOnCommand) infoLog(
                            "Rtp-from-command triggered! " +
                            "Teleported player " + player.getName() +
                            " to " + GeneralUtil.locationAsString(rtpLocation, 1, false) +
                            " taking " + (endTime - startTime) + " ms.");
                } else {
                    player.sendMessage("Need to wait for cooldown: " + coolDownTracker.timeLeftWords(player.getName()));
                }
            }
            /* - - - - - - - - - - - - - - - - - - - - - - - - - - *|
            |* - - If a player tries to RTP someone else - - - - - *|
            |* - - - - - - - - - - - - - - - - - - - - - - - - - - */
            else if (args.length >= 1 && args.length <= 2 && sender.hasPermission("jakesRtp.others")) {
                Player playerToTp = sender.getServer().getPlayerExact(args[0]);
                World destWorld = args.length < 2 ? null : GeneralUtil.getWorldIgnoreCase(sender.getServer(), args[1]);
                if (playerToTp == null)
                    sender.sendMessage("Could not find player " + args[0]);
                else if (args.length >= 2 && destWorld == null)
                    sender.sendMessage("Could not find world" + args[1]);
                else {
                    long startTime = System.currentTimeMillis();

                    Location rtpLocation = destWorld == null ?
                            rt.getRtpLocation(
                                    playerToTp,
                                    true,
                                    true) :
                            rt.getRtpLocation(
                                    rt.getRtpSettingsByWorld(destWorld),
                                    destWorld.getSpawnLocation(),
                                    true);
                    PaperLib.teleportAsync(playerToTp, rtpLocation);

                    long endTime = System.currentTimeMillis();
                    if (rt.logRtpOnForceCommand) infoLog(
                            "Rtp-from-force-command triggered! " +
                            "Teleported player " + playerToTp.getName() +
                            " to " + GeneralUtil.locationAsString(rtpLocation, 1, false) +
                            " taking " + (endTime - startTime) + " ms.");
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
