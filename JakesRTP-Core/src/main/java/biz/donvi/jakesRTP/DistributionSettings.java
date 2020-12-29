package biz.donvi.jakesRTP;

import org.bukkit.configuration.ConfigurationSection;

public class DistributionSettings {

    public final DistributionShape shape;
    public final CenterTypes       center;
    public final int               centerX;
    public final int               centerZ;

    DistributionSettings(ConfigurationSection settings) throws NullPointerException {
        switch (settings.getString("shape").toLowerCase()) {
            case "square":
                shape = new DistributionShape.Square(settings);
                break;
            case "circle":
                shape = new DistributionShape.Circle(settings);
                break;
            case "rectangle":
                shape = new DistributionShape.Rectangle(settings);
                break;
            default:
                throw new RuntimeException(
                    "Distribution shape not properly defined: " + settings.getString("shape").toLowerCase());
        }
        center = CenterTypes.values()[settings.getString("center.option").toLowerCase().charAt(0) - 'a'];
        if (center == CenterTypes.PRESET_VALUE) {
            centerX = settings.getInt("center.c-custom.x");
            centerZ = settings.getInt("center.c-custom.z");
        } else {
            centerX = 0;
            centerZ = 0;
        }
    }

    enum CenterTypes {WORLD_SPAWN, PLAYER_LOCATION, PRESET_VALUE}
}
