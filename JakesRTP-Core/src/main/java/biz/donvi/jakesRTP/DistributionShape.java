package biz.donvi.jakesRTP;

import org.bukkit.configuration.ConfigurationSection;

import static biz.donvi.evenDistribution.RandomCords.*;

abstract class DistributionShape {

    public abstract int[] getCords();

    public static abstract class Symmetric extends DistributionShape {
        final int     radiusMax;
        final int     radiusMin;
        final boolean gaussianDistribution;
        final double  gaussianShrink;
        final double  gaussianCenter;

        public Symmetric(ConfigurationSection settings) {
            radiusMax = settings.getInt("radius.max");
            radiusMin = settings.getInt("radius.min");
            gaussianDistribution = settings.getBoolean("gaussian-distribution.enabled");
            gaussianShrink = settings.getDouble("gaussian-distribution.shrink");
            gaussianCenter = settings.getDouble("gaussian-distribution.center");
        }
    }

    public static class Circle extends Symmetric {
        public Circle(ConfigurationSection settings) { super(settings); }

        @Override
        public int[] getCords() {
            return asIntArray2w(
                getRandXyCircle(radiusMax, radiusMin, gaussianShrink, gaussianCenter)
            );
        }
    }

    public static class Square extends Symmetric {
        public Square(ConfigurationSection settings) { super(settings); }

        @Override
        public int[] getCords() {
            return asIntArray2w(
                !gaussianDistribution
                    ? getRandXySquare(radiusMax, radiusMin)
                    : getRandXySquare(radiusMax, radiusMin, gaussianShrink, gaussianCenter)
            );
        }
    }

    public static class Rectangle extends DistributionShape {

        final int     xRadius;
        final int     zRadius;
        final boolean gapEnabled;
        final int     gapXRadius;
        final int     gapZRadius;
        final int     gapXCenter;
        final int     gapZCenter;

        Rectangle(ConfigurationSection settings) {
            xRadius = settings.getInt("size.x-width") / 2;
            zRadius = settings.getInt("size.z-width") / 2;
            gapEnabled = settings.getBoolean("gap.enabled");
            if (gapEnabled) {
                gapXRadius = settings.getInt("gap.x-width") / 2;
                gapZRadius = settings.getInt("gap.z-width") / 2;
                gapXCenter = settings.getInt("gap.x-center") / 2;
                gapZCenter = settings.getInt("gap.z-center") / 2;
            } else {
                gapXRadius = gapZRadius = gapXCenter = gapZCenter = 0;
            }
        }

        @Override
        public int[] getCords() {
            return asIntArray2w(
                !gapEnabled
                    ? getRandXyRectangle(xRadius, zRadius)
                    : getRandXyRectangle(xRadius, zRadius, gapZRadius, gapZRadius, gapXCenter, gapZCenter)
            );
        }
    }

}
