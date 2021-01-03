package biz.donvi.jakesRTP;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

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
                if (player.hasPermission("jakesrtp.nocooldown")
                    || player.hasPermission("jakesrtp.nocooldown." + relSettings.name.toLowerCase())
                    || relSettings.coolDown.check(player.getName())
                ) {
                    long callTime = System.currentTimeMillis();
                    new RandomTeleportAction(
                        randomTeleporter,
                        relSettings,
                        player.getLocation(),
                        true,
                        true,
                        randomTeleporter.logRtpOnCommand, "Rtp-from-command triggered!"
                    ).teleportAsync(player);
                    relSettings.coolDown.log(player.getName(), callTime);
                } else {
                    player.sendMessage(Messages.
                        NEED_WAIT_COOLDOWN.format(
                        relSettings.coolDown.timeLeftWords(player.getName())
                    ));
                }
            }
        } catch (JrtpBaseException.NotPermittedException npe) {
            sender.sendMessage(Messages.NP_GENERIC.format(npe.getMessage()));
        } catch (Exception e) {
            sender.sendMessage(Messages.NP_UNEXPECTED_EXCEPTION.format(e.getMessage()));
            e.printStackTrace();
        }
        return true;
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
