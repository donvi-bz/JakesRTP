package biz.donvi.jakesRTP;

import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;

public class SafeLocationUtils_12 implements SafeLocationUtils_Patch{

    @Override
    public int getPatchVersion() {
        return 12;
    }

    @Override
    public boolean isSafeToBeIn(Material mat) {
        switch (mat) {
            case AIR:
            case SNOW:
            case VINE:
            case GRASS:
            case LONG_GRASS:
                return true;
            case WATER:
            case LAVA:
            default:
                return false;
        }
    }

    @Override
    public boolean isSafeToBeOn(Material mat) {
        switch (mat) {
            case LAVA:
            case STATIONARY_LAVA:
            case MAGMA:
            case WATER:
            case STATIONARY_WATER:
            case AIR:
            case CACTUS:
            case WATER_LILY:
                return false;
            case GRASS:
            case STONE:
            case DIRT:
            default:
                return true;
        }
    }

    @Override
    public boolean isTreeLeaves(Material mat) {
        switch (mat) {
            case LEAVES:
            case LEAVES_2:
                return true;
            default:
                return false;
        }
    }

    @Override
    public Material chunkLocMatFromSnapshot(int inX, int y, int inZ, ChunkSnapshot chunk) {
        return chunk.getBlockType(inX, y, inZ);
    }
}
