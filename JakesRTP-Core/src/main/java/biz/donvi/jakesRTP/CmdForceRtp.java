package biz.donvi.jakesRTP;

import biz.donvi.argsChecker.ArgsChecker;
import biz.donvi.argsChecker.ArgsTester;
import biz.donvi.argsChecker.DynamicArgsMap;
import io.papermc.lib.PaperLib;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static biz.donvi.jakesRTP.PluginMain.infoLog;
import static org.bukkit.Bukkit.getServer;

public class CmdForceRtp extends DynamicArgsMap implements TabExecutor {

    Map<String, Object> cmdMap;

    private final RandomTeleporter rt;

    public CmdForceRtp(RandomTeleporter rt, Map<String, Object> commandMap) {
        this.rt = rt;
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
            else if (argsChecker.matches(true, null, "-c-w", null, null))
                subForceRtpWithConfigAndWorld(sender, argsChecker.getRemainingArgs());
            else return false;
        } catch (NotPermittedException npe) {
            sender.sendMessage("Could not RTP for reason: " + npe.getMessage());
        } catch (JrtpBaseException e) {
            sender.sendMessage(e.getMessage());
        } catch (Exception e) {
            sender.sendMessage("Error. Could not RTP for reason: " + e.getMessage());
            sender.sendMessage("Please check console for more info on why teleportation failed.");
            e.printStackTrace();
        }
        return true;
    }

    private void subForceRtpWithConfig(CommandSender sender, String[] args) throws Exception {
        Player playerToTp = sender.getServer().getPlayerExact(args[0]);
        if (playerToTp == null) {
            sender.sendMessage("Could not find player " + args[0]);
            return;
        }
        RtpSettings rtpSettings = rt.getRtpSettingsByName(args[1]);

        // ↑ Check step | Teleport step ↓

        GeneralUtil.teleportRandomlyWrapper(
                playerToTp,rt,
                rtpSettings, playerToTp.getLocation(), true,
                true, true,
                rt.logRtpOnForceCommand, "Rtp-from-force-command triggered!"
        );
    }

    private void subForceRtpWithWorld(CommandSender sender, String[] args) throws Exception {
        Player playerToTp = sender.getServer().getPlayerExact(args[0]);
        if (playerToTp == null) {
            sender.sendMessage("Could not find player " + args[0]);
            return;
        }
        World destWorld = GeneralUtil.getWorldIgnoreCase(sender.getServer(), args[1]);
        if ((destWorld) == null) {
            sender.sendMessage("Could not find world " + args[1]);
            return;
        }

        // ↑ Check step | Teleport step ↓

        GeneralUtil.teleportRandomlyWrapper(
                playerToTp, rt,
                rt.getRtpSettingsByWorld(destWorld),
                playerToTp.getLocation().getWorld() == destWorld
                        ? playerToTp.getLocation()
                        : destWorld.getSpawnLocation(),
                true,
                true, true,
                rt.logRtpOnForceCommand, "Rtp-from-force-command triggered!"
        );
    }

    private void subForceRtpWithConfigAndWorld(CommandSender sender, String[] args) throws Exception {
        Player playerToTp = sender.getServer().getPlayerExact(args[0]);
        if (playerToTp == null) {
            sender.sendMessage("Could not find player " + args[0]);
            return;
        }
        RtpSettings rtpSettings = rt.getRtpSettingsByName(args[1]);
        World destWorld = GeneralUtil.getWorldIgnoreCase(sender.getServer(), args[2]);
        if ((destWorld) == null) {
            sender.sendMessage("Could not find world " + args[2]);
            return;
        }

        if (!rtpSettings.getConfigWorlds().contains(destWorld)) {
            sender.sendMessage("Input mismatch: RtpSettings \"" + rtpSettings.name +
                               "\" does not contain the world \" " + destWorld.getName() +
                               "\"as one of its enabled worlds.");
            return;
        } else if (rtpSettings.forceDestinationWorld && rtpSettings.destinationWorld != destWorld) {
            assert rtpSettings.destinationWorld != null; // Force destination world implies this.
            sender.sendMessage("Input mismatch: RtpSettings \"" + rtpSettings.name +
                               "\"can only teleport people in world \"" +
                               rtpSettings.destinationWorld.getName() + "\"");
            return;
        }

        // ↑ Check step | Teleport step ↓

        GeneralUtil.teleportRandomlyWrapper(
                playerToTp, rt,
                rtpSettings, destWorld.getSpawnLocation(), true,
                true, true,
                rt.logRtpOnForceCommand, "Rtp-from-force-command triggered!"
        );

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
                case "-c-w":
                    setResult(rt.getRtpSettingsNames());
                    break;
                case "-w":
                    List<String> worldNames = new ArrayList<>();
                    for (World world : getServer().getWorlds())
                        worldNames.add(world.getName());
                    setResult(worldNames);
                    break;
            }
        } else if (path.length == 3 && path[1].equalsIgnoreCase("-c-w")) {
            List<String> worldNames = new ArrayList<>();
            for (World world : getServer().getWorlds())
                worldNames.add(world.getName());
            setResult(worldNames);
        }
    }

    @Override
    public void getPotential(String path) throws ResultAlreadySetException { }


}
