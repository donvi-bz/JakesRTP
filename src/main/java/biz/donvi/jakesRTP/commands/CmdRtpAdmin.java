package biz.donvi.jakesRTP.commands;

import biz.donvi.jakesRTP.PluginMain;
import biz.donvi.jakesRTP.util.ArgsChecker;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.util.Collections;
import java.util.List;

public class CmdRtpAdmin implements TabExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        ArgsChecker argsChecker = new ArgsChecker(args);

        if (argsChecker.matches(true, "reload"))
            PluginMain.plugin.loadRandomTeleporter();
        else return false;
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return Collections.singletonList("reload");
        else return null;
    }
}
