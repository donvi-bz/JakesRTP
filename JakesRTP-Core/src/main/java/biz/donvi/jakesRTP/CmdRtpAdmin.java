package biz.donvi.jakesRTP;

import biz.donvi.argsChecker.ArgsChecker;
import biz.donvi.argsChecker.ArgsTester;
import biz.donvi.argsChecker.DynamicArgsMap;
import biz.donvi.jakesRTP.GeneralUtil.Pair;
import io.papermc.lib.PaperLib;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CmdRtpAdmin extends DynamicArgsMap implements TabExecutor {

    Map<String, Object> cmdMap;

    public CmdRtpAdmin(Map<String, Object> commandMap) {
        cmdMap = commandMap;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        ArgsChecker argsChecker = new ArgsChecker(args);

        if (argsChecker.matches(true, "reload"))
            subReload();
        else if (argsChecker.matches(true, "status", null))
            subStatus(sender, argsChecker.getRemainingArgs());
        else if (argsChecker.matches(true, "reload-messages"))
            subReloadMessages(sender);
        else return false;
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return ArgsTester.nextCompleteInTree(args, cmdMap, this);
    }

    @Override
    public void getPotential(String[] path) throws ResultAlreadySetException { }

    @Override
    public void getPotential(String path) throws ResultAlreadySetException {
        //noinspection SwitchStatementWithTooFewBranches
        switch (path) {
            case "status":
                setResult(getConfigNames());
        }
    }

    private void subReload() {
        PluginMain.plugin.loadRandomTeleporter();
        PluginMain.plugin.loadLocationCacheFiller();
    }

    private void subReloadMessages(CommandSender sender) {
        PluginMain.plugin.loadMessageMap();
    }


    /**
     * The result of {@code getConfigNames()}, stored with an expiration time. If the data has not expired, the method
     * should return the value of {@code getConfigNamesResults}. If it has expired, it should compute the new value,
     * save
     * it here, then return it.
     */
    private Pair<Long, List<String>> getConfigNamesResults;

    /**
     * Gets a list of the RTP config names. Because this method is expected to be called multiple times per second,
     * yet returns data that changed infrequently, it temporarily stores the resulting list and only rechecks the
     * after 1000 milliseconds.
     *
     * @return A list of the names of the RTP configs.
     */
    private List<String> getConfigNames() {
        if (getConfigNamesResults == null || getConfigNamesResults.key < System.currentTimeMillis()) {
            ArrayList<String> settingsNames = new ArrayList<>();
            for (RtpSettings settings : PluginMain.plugin.getRandomTeleporter().getRtpSettings()) {
                settingsNames.add(settings.name);
            }
            getConfigNamesResults = new Pair<>(System.currentTimeMillis() + 1000, settingsNames);
        }
        return getConfigNamesResults.value;
    }

    @SuppressWarnings("SpellCheckingInspection")
    private static final String[]    COLOR_S  = {"#157BEF", "#0CB863", "#0DDDC9"};
    private static final ChatColor[] COLOR_IL = PaperLib.getMinecraftVersion() >= 16 ?
        new ChatColor[]{
            ChatColor.of(COLOR_S[0]),
            ChatColor.of(COLOR_S[1]),
            ChatColor.of(COLOR_S[2])} :
        new ChatColor[]{
            ChatColor.BLUE,
            ChatColor.GREEN,
            ChatColor.GRAY};

    private void subStatus(CommandSender sender, String[] args) {
//        String box = "┏╍┓┃┗╍┛";
        if (args.length == 1 && args[0].equalsIgnoreCase("#static")) {
            RandomTeleporter rtper = PluginMain.plugin.getRandomTeleporter();
            sender.sendMessage(
                COLOR_IL[0] + "┏\u00A7l\u00A7m╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍\u00A7r\n" +
                COLOR_IL[0] + "┃ [J-RTP] " + COLOR_IL[1] + "Static Settings.\n" +
                COLOR_IL[0] + "┣\u00A7l\u00A7m╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍\u00A7r\n" +
                COLOR_IL[0] + "┃ " + COLOR_IL[1] + "RTP on first join: " + COLOR_IL[2] + (
                    rtper.firstJoinRtp
                        ? "Enabled\n" +
                          COLOR_IL[0] + "┃ • " + COLOR_IL[1] + "Settings to use: " + COLOR_IL[2] +
                          rtper.firstJoinSettings.name + "\n" +
                          COLOR_IL[0] + "┃ • " + COLOR_IL[1] + "World to tp in: " + COLOR_IL[2] +
                          rtper.firstJoinSettings.landingWorld.getName() + "\n"
                        : "Disabled\n") +
                COLOR_IL[0] + "┃ " + COLOR_IL[1] + "RTP on death: " + COLOR_IL[2] + (
                    rtper.onDeathRtp
                        ? "Enabled\n" +
                          COLOR_IL[0] + "┃ • " + COLOR_IL[1] + "Settings to use: " + COLOR_IL[2] +
                          rtper.onDeathSettings.name + "\n" +
                          COLOR_IL[0] + "┃ • " + COLOR_IL[1] + "World to tp in: " + COLOR_IL[2] +
                          rtper.onDeathSettings.landingWorld.getName() + "\n" +
                          COLOR_IL[0] + "┃ • " + COLOR_IL[1] + "Respect beds: " + COLOR_IL[2] +
                          rtper.onDeathRespectBeds +
                          "\n" +
                          COLOR_IL[0] + "┃ • " + COLOR_IL[1] + "Require permission: " + COLOR_IL[2] + (
                              rtper.onDeathRequirePermission
                                  ? "true (jakesrtp.rtpondeath)"
                                  : false
                          ) + "\n"
                        : "Disabled\n")
            );
        } else try {
            RtpSettings settings = PluginMain.plugin.getRandomTeleporter().getRtpSettingsByName(args[0]);
            String name = "[" + settings.name + "] ";
            ArrayList<String> lines = new ArrayList<>();
            lines.add("This debug menu is a work-in-pogress and is currently not at its final state."); //TODO
            // Always log
            lines.add(settings.infoStringCommandEnabled(true));
            lines.add(settings.infoStringRequireExplicitPermission(true));
            lines.add(settings.infoStringPriority(true));
            lines.add(settings.infoStringDestinationWorld(true));
            lines.addAll(settings.distribution.shape.infoStrings(true));
            lines.add(settings.infoStringRegionCenter(true));
            for(String message : lines) sender.sendMessage(message);
        } catch (Exception e) {
            sender.sendMessage(
                COLOR_IL[1] + "No settings found with name [" + COLOR_IL[2] + args[0] + "]\n" +
                COLOR_IL[1] + "Try one of these: " + COLOR_IL[2] + getConfigNames().toString());
        }

    }

}
