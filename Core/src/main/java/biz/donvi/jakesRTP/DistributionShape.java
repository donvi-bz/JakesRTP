package biz.donvi.jakesRTP;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

import static biz.donvi.evenDistribution.RandomCords.*;
import static biz.donvi.jakesRTP.MessageStyles.DebugDisplayLines.*;
import static biz.donvi.jakesRTP.MessageStyles.enabledOrDisabled;

abstract class DistributionShape {

    public abstract String shape();

    public abstract int getArea();

    public abstract int[] getCords();

    public abstract List<String> infoStrings(boolean mcFormat);

    /* ================================================== *\
                    All the simple subclasses
    \* ================================================== */

    public static abstract class Symmetric extends DistributionShape {
        final int     radiusMax;
        final int     radiusMin;
        final boolean gaussianDistribution;
        final double  gaussianShrink;
        final double  gaussianCenter;

        public Symmetric(int rMax, int rMin) {
            radiusMax = rMax;
            radiusMin = rMin;
            gaussianDistribution = false;
            gaussianShrink = gaussianCenter = 0;
        }

        public Symmetric(ConfigurationSection settings) {
            radiusMax = settings.getInt("radius.max");
            radiusMin = settings.getInt("radius.min");
            gaussianDistribution = settings.getBoolean("gaussian-distribution.enabled");
            gaussianShrink = settings.getDouble("gaussian-distribution.shrink");
            gaussianCenter = settings.getDouble("gaussian-distribution.center");
        }


        @Override
        public List<String> infoStrings(boolean mcFormat) {
            ArrayList<String> list = new ArrayList<>();
            list.add(LVL_01_SET.format(mcFormat, "Distribution shape", shape()));
            list.add(DOU_01_SET.format(mcFormat, "Radius max", radiusMax, "Radius min", radiusMin));
            list.add(LVL_01_SET.format(mcFormat, "Gaussian Distribution", enabledOrDisabled(gaussianDistribution)));
            if (gaussianDistribution) {
                list.add(LVL_02_SET.format(mcFormat, "Gaussian shrink", gaussianShrink));
                list.add(LVL_02_SET.format(mcFormat, "Gaussian center", gaussianCenter));
            }
            return list;
        }
    }

    public static class Circle extends Symmetric {
        public Circle(int rMax, int rMin) {super(rMax, rMin);}

        public Circle(ConfigurationSection settings) {super(settings);}

        @Override
        public String shape() {return "Circle";}

        @Override
        public int getArea() {
            return (int) Math.floor(Math.PI * radiusMax * radiusMax - Math.PI * radiusMin * radiusMin);
        }

        @Override
        public int[] getCords() {
            return asIntArray2w(
                getRandXyCircle(radiusMax, radiusMin, gaussianShrink, gaussianCenter)
            );
        }
    }

    public static class Square extends Symmetric {
        public Square(int rMax, int rMin) {super(rMax, rMin);}

        public Square(ConfigurationSection settings) {super(settings);}

        @Override
        public String shape() {return "Square";}

        @Override
        public int getArea() {
            return 4 * (radiusMax * radiusMax - radiusMin * radiusMin);
        }

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

        public Rectangle(int xRad, int zRad) {
            xRadius = xRad;
            zRadius = zRad;
            gapEnabled = false;
            gapXRadius = gapZRadius = gapXCenter = gapZCenter = 0;
        }

        public Rectangle(ConfigurationSection settings) {
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
        public String shape() {return "Rectangle";}

        @Override
        public int getArea() {
            return xRadius * zRadius * 4 - (gapEnabled ? gapXRadius * gapZRadius * 4 : 0);
        }

        @Override
        public int[] getCords() {
            return asIntArray2w(
                !gapEnabled
                    ? getRandXyRectangle(xRadius, zRadius)
                    : getRandXyRectangle(xRadius, zRadius, gapZRadius, gapZRadius, gapXCenter, gapZCenter)
            );
        }

        @Override
        public List<String> infoStrings(boolean mcFormat) {
            ArrayList<String> list = new ArrayList<>();
            list.add(LVL_01_SET.format(mcFormat, "Distribution shape", shape()));
            list.add(DOU_01_SET.format(mcFormat, "X radius", xRadius, "Z radius", zRadius));
            if (gapEnabled) {
                list.add(LVL_01_SET.format(mcFormat, "Gap", "True"));
                list.add(DOU_02_SET.format(mcFormat, "X radius", gapXRadius, "Z radius", zRadius));
                list.add(DOU_02_SET.format(mcFormat, "X center", gapXCenter, "Z center", gapZCenter));
            } else {
                list.add(LVL_01_SET.format(mcFormat, "Gap", "False"));
            }
            return list;
        }
    }

}
