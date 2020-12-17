package biz.donvi.jakesRTP;

import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;

public interface SafeLocationUtils_Patch {

    int getPatchVersion();

    default boolean matchesPatchVersion(int minor) {return getPatchVersion() == minor; }

    boolean isSafeToBeIn(Material mat);

    boolean isSafeToBeOn(Material mat);

    boolean isTreeLeaves(Material mat);

    Material chunkLocMatFromSnapshot(int inX, int y, int inZ, ChunkSnapshot chunk);

    class BlankPatch implements SafeLocationUtils_Patch {

        @Override
        public int getPatchVersion() {
            return 0;
        }

        @Override
        public boolean isSafeToBeIn(Material mat) {
            return false;
        }

        @Override
        public boolean isSafeToBeOn(Material mat) {
            return false;
        }

        @Override
        public boolean isTreeLeaves(Material mat) {
            return false;
        }

        @Override
        public Material chunkLocMatFromSnapshot(int inX, int y, int inZ, ChunkSnapshot chunk) {
            return null;
        }
    }
}
