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
    NP_R_NO_RTPSETTINGS_NAME_FOR_PLAYER("no-settings-found-with-name-for-player"),
    NP_R_TOO_MANY_FAILED_ATTEMPTS("too-many-failed-attempts"),
    PLAYER_NOT_FOUND("player-not-found"),
    WORLD_NOT_FOUND("world-not-found"),
    RTPSETTINGS_NO_CONTAIN_WORLD("rtp-settings-no-contain-world"),
    RTPSETTINGS_MUST_USE_WORLD("rtp-settings-must-use-world"),
    NEED_WAIT_COOLDOWN("need-to-wait-for-cooldown"),
    WARMUP_TELEPORTING_IN_X("teleporting-in-x-seconds"),
    WARMUP_CANCEL_BECAUSE_MOVE("moved-during-warmup"),
    WARMUP_RTP_ALREADY_CALLED("rtp-called-while-in-warmup"),
    ECON_NOT_ENOUGH_MONEY("not-enough-money"),
    ECON_NO_LONGER_ENOUGH_MONEY("no-longer-enough-money"),
    ECON_YOU_WERE_CHARGED_X("you-were-charged-x"),
    ECON_ERROR("economy-error"),
    READABLE_TIME("readable-time"),
    READABLE_TIME_WORD_DAYS("readable-time-word-days"),
    READABLE_TIME_WORD_HOURS("readable-time-word-hours"),
    READABLE_TIME_WORD_MINUTES("readable-time-word-minutes"),
    READABLE_TIME_WORD_SECONDS("readable-time-word-seconds");

    /* ================================================== *\
                            Static
    \* ================================================== */

    private static final String[] mappedValues = new String[Messages.values().length];

    static void setMap(Map<String, String> newMap) {
        final ArrayList<String> emptyValues = new ArrayList<>();
        for (Messages m : Messages.values()) {
            mappedValues[m.ordinal()] = reformat(newMap.remove(m.key));
            if (mappedValues[m.ordinal()] == null) emptyValues.add(m.name() + " ~ " + m.key);
        }
        if (newMap.size() > 0) {
            JakesRtpPlugin.log(WARNING, "More mappings were given than expected. Extra keys:");
            for (String extraValue : newMap.keySet()) JakesRtpPlugin.log(WARNING, extraValue);
        }
        if (emptyValues.size() > 0) {
            JakesRtpPlugin.log(WARNING, "Some messages could not be assigned values. Missing keys:");
            for (String missingValue : emptyValues) JakesRtpPlugin.log(WARNING, missingValue);
        }
    }

    static int addMap(Map<String, String> newMap) {
        int numValuesAdded = 0;
        for (Messages m : Messages.values()) {
            String value = newMap.remove(m.key);
            if (value != null) {
                mappedValues[m.ordinal()] = reformat(value);
                numValuesAdded++;
            }
        }
        if (newMap.size() > 0) {
            JakesRtpPlugin.log(WARNING, "Some extra keys were found:");
            for (String extraValue : newMap.keySet()) JakesRtpPlugin.log(WARNING, extraValue);
        }
        return numValuesAdded;
    }

    private static String reformat(String s) {
        return s == null ? null : replaceNewColors(replaceLegacyColors(replaceWrittenLineBreaks(s)));
    }

    /* ================================================== *\
                          Instance
    \* ================================================== */

    final String key;

    Messages(String key) { this.key = key; }

    String getKey() { return key; }

    String raw() { return mappedValues[this.ordinal()]; }

    String format(Object... args) { return MessageFormat.format(raw(), args); }
}
