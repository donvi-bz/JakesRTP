package biz.donvi.jakesRTP;

import java.util.HashMap;
import java.util.Map;

class MetricsCustomizer {

    private final PluginMain       p;
    private final RandomTeleporter r;
    private final Metrics          m;

    MetricsCustomizer(PluginMain plugin, Metrics metrics) {
        p = plugin;
        m = metrics;
        r = p.getRandomTeleporter();
        if (m.isEnabled()) customize();
        ;
    }

    private void customize() {
        m.addCustomChart(new Metrics.SimplePie("rtp-on-first-join", () -> {
            return r.firstJoinRtp ? "Enabled" : "Disabled";
        }));

        m.addCustomChart(new Metrics.SimplePie("rtp-on-death", () -> {
            return r.onDeathRtp ? "Enabled" : "Disabled";
        }));

        m.addCustomChart(new Metrics.AdvancedPie("rtp-region-shape", () -> {
            Map<String, Integer> pie = new HashMap<>();
            for (RtpSettings settings : p.getRandomTeleporter().getRtpSettings())
                pie.merge(settings.distribution.shape.shape(), 1, Integer::sum);
            return pie;
        }));

        m.addCustomChart(new Metrics.SimplePie("rtp-settings-count", () -> {
            return String.valueOf(r.getRtpSettings().size());
        }));

        m.addCustomChart(new Metrics.SingleLineChart("rtp-per-unit", RandomTeleportAction::getAndClearRtpCount));
    }
}
