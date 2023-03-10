import com.google.ortools.Loader;

import java.util.ArrayList;
import java.util.Random;
public class Randomizer {
    private static Random rand = new Random();
    private static final String dataset_path = "..\\dataset\\";
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

        switch (args.length)
        {
            case 1:
                if (args[0].equals("DEBUG"))
                {
                    GenerateDataset(500, 100, 20, dataset_path);
                }
                break;
            case 4:
                int count = Integer.parseInt(args[0]);
                int var_count = Integer.parseInt(args[1]);
                int worker_count = Integer.parseInt(args[2]);
                String path = args[3];
                GenerateDataset(count, var_count, worker_count, path);
                break;
            default:
                System.err.println("Usage: <count> <var_count> <worker_count> <path_to_dataset_dir>");
                break;
        }


        //RandomizerWorkerCountBenchmark();
    }

    public static void GenerateDataset(int count, int nrOfVariables, int workerCount, String path)
    {
        System.out.println("Generating dataset with count: " + count + " varCount: " + nrOfVariables + " threads: " + workerCount);

        var start = System.currentTimeMillis();

        var gen = new Generator(workerCount, nrOfVariables, path);
        gen.generate(count);

        var end = System.currentTimeMillis();

        System.out.println("Time: " + (end - start) + " ms");
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
