package biz.donvi.jakesRTP;

import biz.donvi.argsChecker.ArgsChecker;
import biz.donvi.argsChecker.ArgsTester;
import biz.donvi.argsChecker.DynamicArgsMap;
import javafx.util.Pair;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CmdRtpAdmin implements TabExecutor, DynamicArgsMap {

    Map<String, Object> cmdMap;

    public CmdRtpAdmin() {
        cmdMap = null;
    }

    public CmdRtpAdmin(Map<String, Object> commandMap) {
        cmdMap = commandMap;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        ArgsChecker argsChecker = new ArgsChecker(args);

        if (argsChecker.matches(true, "reload"))
            PluginMain.plugin.loadRandomTeleporter();
        else if (argsChecker.matches(true, "status"))
            subStatus(sender);
        else return false;
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return ArgsTester.nextCompleteInTree(args, cmdMap, this);
    }

    @Override
    public List<String> getPotential(String path) throws Exception {
        switch (path) {
            case "status":
                return getConfigNames();
            default:
                throw new Exception("Path led nowhere: " + path);
        }
    }


    private Pair<Long, List<String>> getConfigNamesResults;

    private List<String> getConfigNames() {
        if (getConfigNamesResults == null || getConfigNamesResults.getKey() < System.currentTimeMillis()) {
            ArrayList<String> settingsNames = new ArrayList<>();
            for (RtpSettings settings : PluginMain.plugin.getRandomTeleporter().getRtpSettings()) {
                settingsNames.add(settings.name);
            }
            getConfigNamesResults = new Pair<>(System.currentTimeMillis() + 1000, settingsNames);
        }
        return getConfigNamesResults.getValue();
    }

    private void subStatus(CommandSender sender) {

    }
}
