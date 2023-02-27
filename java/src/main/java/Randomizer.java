import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.ortools.Loader;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Random;
public class Randomizer {
    private static Random rand = new Random();
    private static final String dataset_path = "..\\dataset\\";
    double infinity = java.lang.Double.POSITIVE_INFINITY;
    public static void main(String[] args)
    {
        Loader.loadNativeLibraries();
        //Test();
        //generateData();
        //generateDataGenerationStatistics();
        //generateDatasets(10000, output_path);
        //generateDatasets(10000, train_output_path);
        //var startTime1 = System.currentTimeMillis();
        //generateCSV(100000, 4);
        //var endTime1 = System.currentTimeMillis();

        /*
        if (args.length == 1)
        {
            var count = Integer.parseInt(args[0]);

            var startTime = System.currentTimeMillis();

            var gen = new Generator(count);
            gen.generate(100000, 4, dataset_path);
            var endTime = System.currentTimeMillis();

            System.out.println("Generator " + count + " took: " + (endTime - startTime) + " ms");

            var superString = "Generator " + count + " : " + (endTime - startTime) + "\n";

            try {
                Files.write(Paths.get("benchmark.txt"), superString.getBytes(), StandardOpenOption.APPEND);
            }catch (IOException e) {
                //exception handling left as an exercise for the reader
            }
        }
         */

        RandomizerWorkerCountBenchmark();
    }

    public static void RandomizerWorkerCountBenchmark()
    {
        var timeTracker = new ArrayList<>();

        for (int i = 5; i <= 50; i += 5) {
            var startTime = System.currentTimeMillis();
            var gen = new AltGenerator(i);
            gen.generate(200000, 4, dataset_path);
            var endTime = System.currentTimeMillis();

            System.out.println("Generator " + i + " took: " + (endTime - startTime) + " ms");
            timeTracker.add(endTime - startTime);
        }

        System.out.println("**************");

        System.out.println("FINAL RESULTS: ");

        for (int i = 0; i < timeTracker.size(); i++) {
            System.out.println("Generator " + i + " took: " + timeTracker.get(i) + " ms");
        }

        /*
        long startTime = System.currentTimeMillis();

        var generator = new AltGenerator(10);

        generator.generate(100000, 4, dataset_path);

        long endTime = System.currentTimeMillis();

        long startTime2 = System.currentTimeMillis();

        Generator generator2 = new Generator(10);

        generator2.generate(100000, 4, dataset_path);

        long endTime2 = System.currentTimeMillis();

        System.out.println("AltGenerator finished in: " + (endTime - startTime) + " ms");
        System.out.println("Generator finished in: " + (endTime2 - startTime2) + " ms");
         */
    }
}
