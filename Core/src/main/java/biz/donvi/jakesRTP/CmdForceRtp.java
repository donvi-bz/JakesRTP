package biz.donvi.jakesRTP;

import biz.donvi.argsChecker.ArgsChecker;
import biz.donvi.argsChecker.ArgsTester;
import biz.donvi.argsChecker.DynamicArgsMap;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.bukkit.Bukkit.getServer;

public class CmdForceRtp extends DynamicArgsMap implements TabExecutor {

    Map<String, Object> cmdMap;

    private final RandomTeleporter randomTeleporter;

    public CmdForceRtp(RandomTeleporter randomTeleporter, Map<String, Object> commandMap) {
        this.randomTeleporter = randomTeleporter;
        this.cmdMap = commandMap;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        ArgsChecker argsChecker = new ArgsChecker(args);

        try {
            if (argsChecker.matches(true, null, "-c", null))
                subForceRtpWithConfig(sender, argsChecker.getRemainingArgs());
            else if (argsChecker.matches(true, null, "-w", null))
                subForceRtpWithWorld(sender, argsChecker.getRemainingArgs());
            else return false;
        } catch (JrtpBaseException.NotPermittedException npe) {
            sender.sendMessage(Messages.NP_GENERIC.format(npe.getMessage()));
        } catch (JrtpBaseException e) {
            sender.sendMessage(e.getMessage());
        } catch (Exception e) {
            sender.sendMessage(Messages.NP_UNEXPECTED_EXCEPTION.format(e.getMessage()));
            e.printStackTrace();
        }
        return true;
    }

    private void subForceRtpWithConfig(CommandSender sender, String[] args) throws Exception {
        Player playerToTp = sender.getServer().getPlayerExact(args[0]);
        if (playerToTp == null) {
            sender.sendMessage(Messages.PLAYER_NOT_FOUND.format(args[0]));
            return;
        }
        RtpProfile rtpProfile = randomTeleporter.getRtpSettingsByName(args[1]);

        // ↑ Check step | Teleport step ↓

        new RandomTeleportAction(
            randomTeleporter, rtpProfile, playerToTp.getLocation(), true, true,
            randomTeleporter.logRtpOnForceCommand, "Rtp-from-force-command triggered!"
        ).teleportAsync(playerToTp);
    }

    private void subForceRtpWithWorld(CommandSender sender, String[] args) throws Exception {
        Player playerToTp = sender.getServer().getPlayerExact(args[0]);
        if (playerToTp == null) {
            sender.sendMessage(Messages.PLAYER_NOT_FOUND.format(args[0]));
            return;
        }
        World destWorld = GeneralUtil.getWorldIgnoreCase(sender.getServer(), args[1]);
        if ((destWorld) == null) {
            sender.sendMessage(Messages.WORLD_NOT_FOUND.format(args[1]));
            return;
        }

        // ↑ Check step | Teleport step ↓

        new RandomTeleportAction(
            randomTeleporter,
            randomTeleporter.getRtpSettingsByWorld(destWorld),
            playerToTp.getLocation().getWorld() == destWorld
                ? playerToTp.getLocation()
                : destWorld.getSpawnLocation(),
            true,
            true,
            randomTeleporter.logRtpOnForceCommand, "Rtp-from-force-command triggered!"
        ).teleportAsync(playerToTp);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return ArgsTester.nextCompleteInTree(args, cmdMap, this);
    }

    @Override
    public void getPotential(String[] path) throws ResultAlreadySetException {
        if (path.length == 0) {
            List<String> players = new ArrayList<>();
            for (Player player : getServer().getOnlinePlayers())
                players.add(player.getName());
            setResult(players);
        } else if (path.length == 2) {
            switch (path[1]) {
                case "-c":
                    setResult(randomTeleporter.getRtpSettingsNames());
                    break;
                case "-w":
                    List<String> worldNames = new ArrayList<>();
                    for (World world : getServer().getWorlds())
                        worldNames.add(world.getName());
                    setResult(worldNames);
                    break;
            }
        }
    }

    @Override
    public void getPotential(String path) throws ResultAlreadySetException { }


}
