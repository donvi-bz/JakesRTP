package biz.donvi.jakesRTP;

import biz.donvi.jakesRTP.claimsIntegrations.ClaimsManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Function;

import static biz.donvi.jakesRTP.GeneralUtil.readableTime;

class MetricsCustomizer {

    private final JakesRtpPlugin   p;
    private final RandomTeleporter r;
    private final Metrics          m;

    MetricsCustomizer(JakesRtpPlugin plugin, Metrics metrics) {
        p = plugin;
        r = plugin.getRandomTeleporter();
        m = metrics;
        if (m.isEnabled()) customize();
    }

    private void customize() {
        addSimplePie("rtp-on-first-join", () -> r.firstJoinRtp ? "Enabled" : "Disabled");
        addSimplePie("rtp-on-death", () -> r.onDeathRtp ? "Enabled" : "Disabled");
        addSimplePie("rtp-settings-count", () -> String.valueOf(r.getRtpSettings().size()));
        addSimplePie("lang-custom-messages", () -> p.customMessageCount > 1 ? "Yes" : "No");
        addSimplePie("lang-set-language", () -> p.lang);
        addSimplePie("world-border", () -> JakesRtpPlugin.worldBorderPluginHook.hook.name());//TODO ADD TO bSTATS

        addAdvancedPie("rtp-region-shape", r::getRtpSettings,
                       settings -> settings.distribution.shape.shape()); // TODO MAKE SURE WORKS ON bSTATS
//       // Replaced by ↑
//        m.addCustomChart(new Metrics.AdvancedPie("rtp-region-shape", () -> {
//            Map<String, Integer> pie = new HashMap<>();
//            for (RtpSettings settings : r.getRtpSettings())
//                pie.merge(settings.distribution.shape.shape(), 1, Integer::sum);
//            return pie;
//        }));

        addAdvancedPie("rtp-cooldown", r::getRtpSettings,
                       settings -> readableTime(settings.coolDown.coolDownTime)); // TODO ADD TO bSTATS
        addAdvancedPie("rtp-warmup", r::getRtpSettings,
                       settings -> readableTime(settings.warmup * 1000L)); // TODO ADD TO bSTATS
        addAdvancedPie("rtp-cost", r::getRtpSettings,
                       settings -> String.valueOf(settings.cost)); // TODO ADD TO bSTATS

        addDrillDownPie_specific();

        m.addCustomChart(new Metrics.SingleLineChart("rtp-per-unit", RandomTeleportAction::getAndClearRtpCount));
    }

    private void addSimplePie(String chartId, Callable<String> callable) {
        m.addCustomChart(new Metrics.SimplePie(chartId, callable));
    }

    private <T> void addAdvancedPie(String chartId, Callable<Iterable<T>> iterateOver, Function<T, String> toCount) {
        m.addCustomChart(new Metrics.AdvancedPie(chartId, () -> {
            Map<String, Integer> pie = new HashMap<>();
            for (T t : iterateOver.call()) pie.merge(toCount.apply(t), 1, Integer::sum);
            return pie;
        }));
    }

    private void addDrillDownPie_specific() {
        m.addCustomChart(new Metrics.DrilldownPie("location-restrictor-support", () -> {
            // Stuff for the graph itself.
            boolean isUsed;
            Map<String, Map<String, Integer>> outerMap = new HashMap<>();
            Map<String, Integer> innerMap = new HashMap<>();
            // Stuff specifically for the `if` statement
            ClaimsManager cm = JakesRtpPlugin.claimsManager;
            List<String> names = cm == null ? null : cm.enabledLocationRestrictors();
            // The `if` statement
            if (cm == null) { // This feature is forcibly disabled
                isUsed = false;
                innerMap.put("Disabled", 1);
            } else if (names.size() == 0) { // Not forcibly disabled, but no things are being used
                isUsed = false;
                innerMap.put("No supporting plugins found", 1);
            } else { // This means we have support ENABLED and there ARE loaded support things
                isUsed = true;
                for (String name : names)
                    innerMap.put(name, 1);
            }
            outerMap.put(isUsed ? "Used" : "Not Used", innerMap);
            return outerMap;
        }));
    }
}
