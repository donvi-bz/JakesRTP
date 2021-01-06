package biz.donvi.jakesRTP;

import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.popcraft.chunky.shape.Shape;
import org.popcraft.chunkyborder.BorderData;
import org.popcraft.chunkyborder.ChunkyBorder;

import java.util.HashMap;
import java.util.Map;

public class WorldBorderPluginHook {

    static final String DIST_NAME_PREFIX = "world-border_";

    private final Server server;

    PluginSpecificHook hook;

    WorldBorderPluginHook(Server server) {
        this.server = server;
        this.hook = findHook();

        // And after the hook is (potentially) loaded...
        if (hasHook()) JakesRtpPlugin.infoLog(
            "It looks like your using the world border plugin '" + hook.name() + "'.\n" +
            "Random teleport locations will always end up inside any world border.");
    }

    private PluginSpecificHook findHook() {
        PluginSpecificHook potentialHook;
        // Repeat this ↓ for each potential hook
        if ((potentialHook = new ChunkyBorderHook()).hasInstance()) return potentialHook;
        // No hook?
        return new DefaultAsHook();
    }

    public boolean hasHook() {return !(hook instanceof DefaultAsHook);}

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

    class DefaultAsHook extends PluginSpecificHook {

        @Override
        protected String name() {
            return "default";
        }

        @Override
        public boolean isInside(Location loc) {
            try {
                //noinspection ConstantConditions // The location came from a world, it will have a world
                return loc.getWorld().getWorldBorder().isInside(loc);
            } catch (NullPointerException npe) {
                return true;
            }
        }

        @Override
        public Map<String, DistributionSettings> generateDistributions() {
            Map<String, DistributionSettings> distributions = new HashMap<>();
            for (World w : server.getWorlds()) {
                WorldBorder wb = w.getWorldBorder();
                distributions.put(
                    DIST_NAME_PREFIX + w.getName(),
                    new DistributionSettings(
                        new DistributionShape.Rectangle(
                            (int) wb.getSize() / 2,
                            (int) wb.getSize() / 2),
                        wb.getCenter().getBlockX(),
                        wb.getCenter().getBlockZ())
                );
            }
            return distributions;
        }
    }

    class ChunkyBorderHook extends PluginSpecificHook {

        private ChunkyBorder getInstance() { return (ChunkyBorder) server.getPluginManager().getPlugin(name()); }

        @Override
        protected String name() { return "ChunkyBorder"; }

        @Override
        public boolean isInside(Location loc) {
            //noinspection ConstantConditions // A world from the server will never not have a name
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
                    DIST_NAME_PREFIX + set.getKey(),
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
