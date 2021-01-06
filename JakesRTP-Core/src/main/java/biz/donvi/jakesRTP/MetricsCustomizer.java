package biz.donvi.jakesRTP;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

class MetricsCustomizer {

    private final RandomTeleporter r;
    private final Metrics          m;

    MetricsCustomizer(JakesRtpPlugin plugin, Metrics metrics) {
        m = metrics;
        r = plugin.getRandomTeleporter();
        if (m.isEnabled()) customize();
    }

    private void customize() {
        addSimplePie("rtp-on-first-join", () -> r.firstJoinRtp ? "Enabled" : "Disabled");
        addSimplePie("rtp-on-death", () -> r.onDeathRtp ? "Enabled" : "Disabled");
        addSimplePie("rtp-settings-count", () -> String.valueOf(r.getRtpSettings().size()));

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
