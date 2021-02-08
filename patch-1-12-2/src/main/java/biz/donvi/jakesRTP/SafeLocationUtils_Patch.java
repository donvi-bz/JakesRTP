package biz.donvi.jakesRTP;

import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;

public interface SafeLocationUtils_Patch {

    public int getPatchVersion();

    public default boolean matchesPatchVersion(int minor) {return getPatchVersion() == minor; }

    public boolean isSafeToBeIn(Material mat);

    public boolean isSafeToBeOn(Material mat);

    public boolean isTreeLeaves(Material mat);

    public Material chunkLocMatFromSnapshot(int inX, int y, int inZ, ChunkSnapshot chunk);

}
