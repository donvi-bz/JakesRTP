package biz.donvi.jakesRTP;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

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
        addSimplePie("lang-custom-messages", () -> p.customMessageCount > 1 ? "Yes" : "No"); // TODO Add to bStats
        addSimplePie("lang-set-language", () -> p.lang); // TODO Add to bStats

        m.addCustomChart(new Metrics.AdvancedPie("rtp-region-shape", () -> {
            Map<String, Integer> pie = new HashMap<>();
            for (RtpSettings settings : r.getRtpSettings())
                pie.merge(settings.distribution.shape.shape(), 1, Integer::sum);
            return pie;
        }));

        m.addCustomChart(new Metrics.SingleLineChart("rtp-per-unit", RandomTeleportAction::getAndClearRtpCount));
    }

    private void addSimplePie(String chartId, Callable<String> callable) {
        m.addCustomChart(new Metrics.SimplePie(chartId, callable));
    }
}
