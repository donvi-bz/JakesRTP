package biz.donvi.jakesRTP;

import org.bukkit.configuration.ConfigurationSection;

public class DistributionSettings {

    public final DistributionShape shape;
    public final CenterTypes       center;
    public final int               centerX;
    public final int               centerZ;


    DistributionSettings(DistributionShape shape, int centerX, int centerZ) {
        this.shape = shape;
        center = CenterTypes.PRESET_VALUE;
        this.centerX = centerX;
        this.centerZ = centerZ;
    }

    DistributionSettings(ConfigurationSection settings) throws JrtpBaseException.ConfigurationException {
        String shapeString = null;
        try {
            shapeString = settings.getString("shape").toLowerCase();
        } catch (NullPointerException npe) {
            throw new JrtpBaseException.ConfigurationException("Configuration shape not properly defined.");
        }
        switch (shapeString) {
            case "square" -> shape = new DistributionShape.Square(settings);
            case "circle" -> shape = new DistributionShape.Circle(settings);
            case "rectangle" -> shape = new DistributionShape.Rectangle(settings);
            default -> throw new JrtpBaseException.ConfigurationException(
                "Distribution shape not properly defined: " + shapeString);
        }
        try {
            char centerChar = settings.getString("center.option").toLowerCase().charAt(0);
            center = CenterTypes.values()[centerChar - 'a'];
        } catch (NullPointerException npe) {
            throw new JrtpBaseException.ConfigurationException("Configuration center not properly defined.");
        }
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
