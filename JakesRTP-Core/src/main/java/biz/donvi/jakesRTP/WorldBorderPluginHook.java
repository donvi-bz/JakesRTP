package biz.donvi.jakesRTP;

import org.bukkit.Location;
import org.bukkit.Server;
import org.popcraft.chunky.shape.Shape;
import org.popcraft.chunkyborder.BorderData;
import org.popcraft.chunkyborder.ChunkyBorder;

import java.util.HashMap;
import java.util.Map;

public class WorldBorderPluginHook {

    private final Server server;

    PluginSpecificHook hook;

    WorldBorderPluginHook(Server server) {
        this.server = server;
        this.hook = findHook();

        // And after the hook is (potentially) loaded...
        if (hasHook()) PluginMain.infoLog(
            "It looks like your using the world border plugin '" + hook.name() + "'.\n" +
            "Random teleport locations will always end up inside any world border.");
    }

    private PluginSpecificHook findHook() {
        PluginSpecificHook potentialHook;
        // Repeat this ↓ for each potential hook
        if ((potentialHook = new ChunkyBorderHook()).hasInstance()) return potentialHook;
        // No hook?
        return null;
    }

    public boolean hasHook() {return hook != null;}

    public boolean isInside(Location loc) {
        if (hook == null) return true;
        else return hook.isInside(loc);
    }

    public Map<String, DistributionSettings> generateDistributions() { return hook.generateDistributions(); }

    /* ================================================== *\
                    All the simple subclasses
    \* ================================================== */

    abstract class PluginSpecificHook {
        protected abstract String name();

        public abstract boolean isInside(Location loc);

        public boolean hasInstance() { return server.getPluginManager().getPlugin(name()) != null; }

        public abstract Map<String, DistributionSettings> generateDistributions();
    }

    class ChunkyBorderHook extends PluginSpecificHook {

        private ChunkyBorder getInstance() { return (ChunkyBorder) server.getPluginManager().getPlugin(name()); }

        @Override
        protected String name() { return "ChunkyBorder"; }

        @Override
        public boolean isInside(Location loc) {
            BorderData borderData = getInstance()
                .getBorders()
                .get(loc.getWorld().getName());
            Shape shape = borderData == null
                ? null
                : borderData.getBorder();
            return shape == null || shape.isBounding(loc.getX(), loc.getZ());
        }

        @Override
        public Map<String, DistributionSettings> generateDistributions() {
            Map<String, DistributionSettings> distributions = new HashMap<>();
            for (Map.Entry<String, BorderData> set : getInstance().getBorders().entrySet()) {
                BorderData bd = set.getValue();
                distributions.put(
                    "fill_" + set.getKey(),
                    new DistributionSettings(
                        new DistributionShape.Rectangle(
                            bd.getRadiusX(),
                            bd.getRadiusZ()),
                        bd.getCenterX(),
                        bd.getCenterZ())
                );
            }
            return distributions;
        }
    }
}
