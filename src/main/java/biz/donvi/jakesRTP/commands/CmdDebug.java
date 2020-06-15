package biz.donvi.jakesRTP.commands;

import biz.donvi.jakesRTP.PluginMain;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.io.*;
import java.util.Objects;

public class CmdDebug implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        System.out.println(PluginMain.plugin.getCurrentConfigVersion());
        return true;
    }

}
