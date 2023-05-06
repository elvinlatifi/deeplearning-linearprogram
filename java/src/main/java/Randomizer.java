import com.google.ortools.Loader;

public class Randomizer {
    private static final String dataset_path = "..\\dataset\\";
    public static void main(String[] args)
    {
        Loader.loadNativeLibraries();

        switch (args.length)
        {
            case 1:
                if (args[0].equals("DEBUG"))
                {
                    GenerateDataset(50, 4, 20, dataset_path);
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
}
