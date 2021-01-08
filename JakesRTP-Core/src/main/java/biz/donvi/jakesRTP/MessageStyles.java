package biz.donvi.jakesRTP;

import io.papermc.lib.PaperLib;
import net.md_5.bungee.api.ChatColor;

import java.text.MessageFormat;

/**
 * Why is this file so disgusting you ask?
 * Please don't ask that question.
 */
class MessageStyles {

//        String box = "┏╍┓┃┗╍┛";

    @SuppressWarnings("SpellCheckingInspection")
    static final String[]    COLOR_S  = {"#157BEF", "#0CB863", "#0DDDC9"};
    static final ChatColor[] COLOR_IL = PaperLib.getMinecraftVersion() >= 16 ?
        new ChatColor[]{
            ChatColor.of(COLOR_S[0]),
            ChatColor.of(COLOR_S[1]),
            ChatColor.of(COLOR_S[2])} :
        new ChatColor[]{
            ChatColor.BLUE,
            ChatColor.GREEN,
            ChatColor.GRAY};

    enum DebugDisplayLines {
        HEADER_TOP(0),
        HEADER_MID(1),
        HEADER_END(0),
        LVL_01_SET(2),
        LVL_02_SET(2),
        DOU_01_SET(4),
        DOU_02_SET(4);


        private enum McVersion {
            HEADER_TOP(String.format("%s┏§l§m╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍§r", COLOR_IL[0])),
            HEADER_MID(String.format("%s┃ [J-RTP] %s{0}", COLOR_IL[0], COLOR_IL[1])),
            HEADER_END(String.format("%s┣§l§m╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍╍§r", COLOR_IL[0])),
            LVL_01_SET(String.format("%s┃ %s{0}: %s{1}", COLOR_IL[0], COLOR_IL[1], COLOR_IL[2])),
            LVL_02_SET(String.format("%s┃ • %s{0}: %s{1}", COLOR_IL[0], COLOR_IL[1], COLOR_IL[2])),
            DOU_01_SET(String.format("%s┃ %s{0}: %s{1}%s | {2}: %s{3}",
                                     COLOR_IL[0], COLOR_IL[1], COLOR_IL[2], COLOR_IL[1], COLOR_IL[2])),
            DOU_02_SET(String.format("%s┃ • %s{0}: %s{1}%s | {2}: %s{3}",
                                     COLOR_IL[0], COLOR_IL[1], COLOR_IL[2], COLOR_IL[1], COLOR_IL[2]));

            final String text;

            McVersion(String t) {text = t;}
        }

        private enum ConsoleVersion {
            HEADER_TOP(""),
            HEADER_MID(""),
            HEADER_END(""),
            LVL_01_SET("{0}: {1}"),
            LVL_02_SET(" - {0}: {1}"),
            DOU_01_SET("{0}: {1} | {2}: {3}"),
            DOU_02_SET(" - {0}: {1} | {2}: {3}");

            final String text;

            ConsoleVersion(String t) {text = t;}
        }


        DebugDisplayLines(int numPlaceholders) {
            assert this.name().equals(McVersion.values()[this.ordinal()].name());
            assert this.name().equals(ConsoleVersion.values()[this.ordinal()].name());

            mcText = McVersion.values()[this.ordinal()].text;
            consoleText = ConsoleVersion.values()[this.ordinal()].text;
            this.numPlaceholders = numPlaceholders;
        }

        final String mcText;
        final String consoleText;
        final int    numPlaceholders;

        String format(boolean mcFormat, Object... args) {
            assert args.length == numPlaceholders;
            return mcFormat
                ? MessageFormat.format(mcText, args)
                : MessageFormat.format(consoleText, args);
        }
    }

    static String enabledOrDisabled(boolean b) {
        return b ? "Enabled" : "Disabled";
    }
}
