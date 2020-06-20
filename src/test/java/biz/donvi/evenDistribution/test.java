package biz.donvi.evenDistribution;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static biz.donvi.evenDistribution.RandomCords.*;

public class test {

    public static void main(String[] args) throws IOException, InterruptedException {
        int radiusMax = 1000;
        int radiusMin = 500;

        File randPointDataFile = new File("plot/random.txt");
        if (!randPointDataFile.exists()) {
            randPointDataFile.getParentFile().mkdir();
            randPointDataFile.createNewFile();
        }
        FileWriter randPointDataFileWriter = new FileWriter(randPointDataFile);

        for (int i = 0; i < 20000; i++) {
            int[] randomCord = getRandXySquare(radiusMax, radiusMin,3,0);
            randPointDataFileWriter.write(randomCord[0] + " " + randomCord[1] + '\n');
        }


        randPointDataFileWriter.close();
        Runtime.getRuntime().exec(
                "\"C:/Program Files/gnuplot/bin/wgnuplot.exe\" " +
                "--persist \"D:/Laptop/Java/JakesRTP/plot/plotShape.sh\"");
    }
}
