package biz.donvi.jakesRTP;

import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;

public class SafeLocationUtils_12 extends SafeLocationUtils {
    @Override
    boolean isSafeToBeIn(Material mat) {
        return false;
    }

    @Override
    boolean isSafeToBeOn(Material mat) {
        return false;
    }

    @Override
    boolean isTreeLeaves(Material mat) {
        return false;
    }

    @Override
    Material chunkLocMatFromSnapshot(int inX, int y, int inZ, ChunkSnapshot chunk) {
        return null;
    }
}
