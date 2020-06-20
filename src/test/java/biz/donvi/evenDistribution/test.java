package biz.donvi.evenDistribution;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static biz.donvi.evenDistribution.RandomCords.*;

public class test {

    public static void main(String[] args) throws IOException, InterruptedException {
        int radiusMax = 1000;
        int radiusMin = 250;

        File randPointDataFile = new File("plot/random.txt");
        if (!randPointDataFile.exists()) {
            randPointDataFile.getParentFile().mkdir();
            randPointDataFile.createNewFile();
        }
        FileWriter randPointDataFileWriter = new FileWriter(randPointDataFile);

        for (int i = 0; i < 20000; i++) {
            int[] randomCord = getRandXyCircle(radiusMax, radiusMin,4,.25);
            randPointDataFileWriter.write(randomCord[0] + " " + randomCord[1] + '\n');
        }


        randPointDataFileWriter.close();
        Runtime.getRuntime().exec(
                "\"C:/Program Files/gnuplot/bin/wgnuplot.exe\" " +
                "--persist \"D:/Laptop/Java/JakesRTP/plot/plotShape.sh\"");
    }
}
