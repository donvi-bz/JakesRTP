package biz.donvi.jakesRTP;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Map;

import static biz.donvi.jakesRTP.GeneralUtil.*;
import static java.util.logging.Level.WARNING;

public enum Messages {
    NP_GENERIC("not-permitted-generic"),
    NP_UNEXPECTED_EXCEPTION("not-permitted-major-error"),
    NP_R_NOT_ENABLED("not-enabled-in-this-world"),
    NP_R_NO_RTPSETTINGS_NAME("no-settings-found-with-name"),
    NP_R_TOO_MANY_FAILED_ATTEMPTS("too-many-failed-attempts"),
    PLAYER_NOT_FOUND("player-not-found"),
    WORLD_NOT_FOUND("world-not-found"),
    RTPSETTINGS_NO_CONTAIN_WORLD("rtp-settings-no-contain-world"),
    RTPSETTINGS_MUST_USE_WORLD("rtp-settings-must-use-world"),
    NEED_WAIT_COOLDOWN("need-to-wait-for-cooldown"),
    READABLE_TIME("readable-time"),
    READABLE_TIME_WORD_DAYS("readable-time-word-days"),
    READABLE_TIME_WORD_HOURS("readable-time-word-hours"),
    READABLE_TIME_WORD_MINUTES("readable-time-word-minutes"),
    READABLE_TIME_WORD_SECONDS("readable-time-word-seconds");


    private static final String[] mappedValues = new String[Messages.values().length];
    final String key;

    static void setMap(Map<String, String> newMap)  {
        final ArrayList<String> emptyValues = new ArrayList<>();
        for (Messages m : Messages.values()) {
            mappedValues[m.ordinal()] = reformat(newMap.remove(m.key));
            if (mappedValues[m.ordinal()] == null) emptyValues.add(m.name() + " ~ " + m.key);
        }
        if (newMap.size() > 0) {
            PluginMain.logger.log(WARNING, "More mappings were given than expected. Extra keys:");
            for (String extraValue : newMap.keySet()) PluginMain.logger.log(WARNING, extraValue);
        }
        if (emptyValues.size() > 0) {
            PluginMain.logger.log(WARNING, "Some messages could not be assigned values. Missing keys:");
            for (String missingValue : emptyValues) PluginMain.logger.log(WARNING, missingValue);
        }
    }

    static void addMap(Map<String, String> newMap) {
        for (Messages m : Messages.values()) {
            String value = newMap.remove(m.key);
            if (value != null) mappedValues[m.ordinal()] = reformat(value);
        }
        if (newMap.size() > 0) {
            PluginMain.logger.log(WARNING, "Some extra keys were found:");
            for (String extraValue : newMap.keySet()) PluginMain.logger.log(WARNING, extraValue);
        }
    }

    private static String reformat(String s) {
        return s == null ? null : replaceNewColors(replaceLegacyColors(replaceWrittenLineBreaks(s)));
    }

    private Messages(String key) { this.key = key; }


    String getKey() { return key; }

    String raw() { return mappedValues[this.ordinal()]; }

    String format(Object... args) { return MessageFormat.format(raw(), args); }
}
