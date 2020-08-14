package biz.donvi.jakesRTP;

import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;

public class SafeLocationUtils_16 extends SafeLocationUtils {

    @Override
    boolean isSafeToBeIn(Material mat) {
        switch (mat) {
            case AIR:
            case SNOW:
            case FERN:
            case LARGE_FERN:
            case VINE:
            case GRASS:
            case TALL_GRASS:
                return true;
            case WATER:
            case LAVA:
            case CAVE_AIR:
            default:
                return false;
        }
    }

    @Override
    boolean isSafeToBeOn(Material mat) {
        switch (mat) {
            case LAVA:
            case MAGMA_BLOCK:
            case WATER:
            case AIR:
            case CAVE_AIR:
            case VOID_AIR:
            case CACTUS:
            case SEAGRASS:
            case TALL_SEAGRASS:
            case LILY_PAD:
                return false;
            case GRASS_BLOCK:
            case STONE:
            case DIRT:
            default:
                return true;
        }
    }

    @Override
    boolean isTreeLeaves(Material mat) {
        switch (mat) {
            case ACACIA_LEAVES:
            case BIRCH_LEAVES:
            case DARK_OAK_LEAVES:
            case JUNGLE_LEAVES:
            case OAK_LEAVES:
            case SPRUCE_LEAVES:
                return true;
            default:
                return false;
        }
    }

    @Override
    Material chunkLocMatFromSnapshot(int inX, int y, int inZ, ChunkSnapshot chunk) {
        return chunk.getBlockData(inX, y, inZ).getMaterial();
    }

}
