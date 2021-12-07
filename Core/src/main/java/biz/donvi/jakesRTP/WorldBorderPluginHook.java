package biz.donvi.jakesRTP;

import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.popcraft.chunky.shape.Shape;
import org.popcraft.chunkyborder.BorderData;
import org.popcraft.chunkyborder.ChunkyBorder;
import org.popcraft.chunkyborder.ChunkyBorderBukkit;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class WorldBorderPluginHook {

    static final String DIST_NAME_PREFIX = "world-border_";

    private final Server server;

    PluginSpecificHook hook;

    WorldBorderPluginHook(Server server) {
        this.server = server;
        this.hook = findHook();

        // And after the hook is (potentially) loaded...
        if (hasHook()) {
            JakesRtpPlugin.infoLog("It looks like your using the world border plugin '" + hook.name() + "'.");
            JakesRtpPlugin.infoLog("Random teleport locations will always end up inside any world border.");
        }
    }

    private PluginSpecificHook findHook() {
        try {
            PluginSpecificHook potentialHook;
            // Repeat this ↓ for each potential hook
            if ((potentialHook = new ChunkyBorderHook()).isUsable()) return potentialHook;
        } catch (Exception e) {
            JakesRtpPlugin.log(Level.WARNING,
                "Caught an error while trying to register a ChunkyBorder plugin hook. " +
                "Is it up to date? Is JRTP up to date?");
            JakesRtpPlugin.log(Level.WARNING, "You may want to manually set your RTP region for the time being.");
            e.printStackTrace(); // debug
        }
        // No hook?
        return new DefaultAsHook();
    }

    public boolean hasHook() {return !(hook instanceof DefaultAsHook);}

    public boolean isInside(Location loc) {
        if (hook == null || !hook.isUsable()) return true;
        else return hook.isInside(loc);
    }

    public Map<String, DistributionSettings> generateDistributions() {return hook.generateDistributions();}

    /* ================================================== *\
                    All the simple subclasses
    \* ================================================== */

    abstract class PluginSpecificHook {

        protected abstract String name();

        public abstract boolean isInside(Location loc);

        private Plugin instance = null;

        public Plugin instance() {
            if (instance == null) instance = server.getPluginManager().getPlugin(name());
            return instance;
        }

        public boolean hasInstance() {return instance() != null;}

        private int[] version = null;

        public int[] getVersion() {
            if (!hasInstance()) return null;
            if (version == null) {
                String[] versionParts = instance().getDescription().getVersion().split("\\.");
                version = new int[versionParts.length];
                for (int i = 0; i < versionParts.length; i++) version[i] = Integer.parseInt(versionParts[i]);
            }
            return version;
        }

        /**
         * Method to tell if the plugin hook is usable.
         * OVERRIDE THIS TO ADD CUSTOM CODE FOR EACH PLUGIN AS NECESSARY
         *
         * @return True if the plugin is usable, false otherwise.
         */
        public boolean isUsable() {return hasInstance() && !noLongerUsable;}

        protected boolean noLongerUsable = false; // Set if some incompatability is found later down the line

        public abstract Map<String, DistributionSettings> generateDistributions();
    }

    class DefaultAsHook extends PluginSpecificHook {

        @Override
        protected String name() {
            return "DefaultMC";
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

        @Override
        public boolean isUsable() {
            return getInstance() != null && super.isUsable();
        }

        private ChunkyBorder getInstance() {
            // TODO FIX THIS, ITS BROKEN
            return (ChunkyBorderBukkit) server.getPluginManager().getPlugin(name());
        }

        @Override
        protected String name() {return "ChunkyBorder";}

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
            try {
                for (Map.Entry<String, BorderData> set : getInstance().getBorders().entrySet()) {
                    BorderData bd = set.getValue();
                    distributions.put(
                        DIST_NAME_PREFIX + set.getKey(),
                        new DistributionSettings(
                            new DistributionShape.Rectangle(
                                (int) bd.getRadiusX(),
                                (int) bd.getRadiusZ()),
                            (int) bd.getCenterX(),
                            (int) bd.getCenterZ())
                    );
                }
            } catch (NoSuchMethodError e) {
                noLongerUsable = true;
                JakesRtpPlugin.log(Level.WARNING, "Turns out there was an error with world border compatibility...");
                JakesRtpPlugin.log(Level.WARNING, "No guarantees will be made about points being within the border.");
                e.printStackTrace();
            }
            return distributions;
        }
    }
}
