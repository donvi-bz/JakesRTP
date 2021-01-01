package biz.donvi.jakesRTP;

import org.bukkit.Location;
import org.bukkit.Server;
import org.popcraft.chunky.shape.Shape;
import org.popcraft.chunkyborder.ChunkyBorder;

public class WorldBorderPluginHook {

    private final Server server;

    PluginSpecificHook hook;

    WorldBorderPluginHook(Server server) {
        this.server = server;
        PluginSpecificHook potentialHook;
        // For each potential hook...
        if (!(potentialHook = new ChunkyBorderHook()).hasInstance()) potentialHook = null;

        // And after the hook is (potentially) loaded...
        if (hasHook()) PluginMain.infoLog(
            "It looks like your using the world border plugin '" + hook.name() + "'.\n" +
            "Random teleport locations will always end up inside any world border.");
    }

    public boolean hasHook() {return hook != null;}

    public boolean isInside(Location loc) {
        if (hook == null) return true;
        else return hook.isInside(loc);
    }

    /* ================================================== *\
                    All the simple subclasses
    \* ================================================== */

    abstract class PluginSpecificHook {
        protected abstract String name();

        public abstract boolean isInside(Location loc);

        public boolean hasInstance() { return server.getPluginManager().getPlugin(name()) != null; }
    }

    class ChunkyBorderHook extends PluginSpecificHook {

        private ChunkyBorder getInstance() { return (ChunkyBorder) server.getPluginManager().getPlugin(name()); }

        @Override
        protected String name() { return "ChunkyBorder"; }

        @Override
        public boolean isInside(Location loc) {
            Shape shape = getInstance()
                .getBorders()
                .get(loc.getWorld().getName())
                .getBorder();
            return shape == null || shape.isBounding(loc.getX(), loc.getZ());
        }
    }
}
