import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

public class Generator {
    int convertible;
    int inconvertible;
    private static Random rand = new Random();
    private static final String dataset_path = "..\\dataset\\";

    //private static final int WORKER_SIZE_DIVIDER = 16; //count / WORKER_SIZE_DIVIDER = worker count
    private int workerCount;
    //private static final int WORKER_THRESHOLD = 1000;

    public Generator(int workerCount) {
        this.workerCount = workerCount;
    }
    class Worker implements Runnable
    {
        private String[] outputArrayRef;
        private String[] bofArrayRef;
        private int workerOffset;
        private int workerSize;
        private int nrOfVariables;

        private String tempName;

        public Worker(String threadName, String[] outputArrayRef, String[] bofArrayRef, int workerOffset, int workSize, int nrOfVariables)
        {
            System.out.println("Started " + threadName + " workerOffset: " + workerOffset + " worker_size = " + workSize);

            this.tempName = threadName;
            this.outputArrayRef = outputArrayRef;
            this.bofArrayRef = bofArrayRef;
            this.workerSize = workSize;
            this.workerOffset = workerOffset;
            this.nrOfVariables = nrOfVariables;
        }

        public void run()
        {
            int convertible = 0;
            int inconvertible = 0;

            while(convertible != workerSize || inconvertible != workerSize) {
                LinearProgram lp = generateLinearProgram(nrOfVariables);
                LinearProgram result;

                if (lp.solve()) {
                    result = flipSigns(lp, true);
                }
                else {
                    result = flipSigns(lp, false);
                }
                if (result == null) {
                    continue;
                }
                else if (result.isConvertible()) {
                    if (convertible == workerSize) {
                        continue;
                    }

                    writeDataToArray(result.getRelevantData(), result.getBinaryOutputFeature(), convertible + inconvertible);

                    convertible++;
                }
                else {
                    if (inconvertible == workerSize) {
                        continue;
                    }

                    writeDataToArray(result.getRelevantData(), result.getBinaryOutputFeature(), convertible + inconvertible);

                    inconvertible++;
                }
            }

            System.out.println(tempName + ": Finished generating " + workerSize * 2 + " lps @ offset: " + workerOffset);
        }

        private void writeDataToArray(String[] input1, String[] input2, int curr_count)
        {
            var str1 = getCsvRowFromStrArray(input1);
            var str2 = getCsvRowFromStrArray(input2);

            try {
                outputArrayRef[workerOffset + curr_count] = str1;
                bofArrayRef[workerOffset + curr_count] = str2;
            }
            catch(Exception e)
            {
                System.err.println("LOL");
            }

        }

        private String getCsvRowFromStrArray(String[] input)
        {
            String output = "";

            for (int i = 0; i < input.length; i++) {
                output += input[i];

                if (i < input.length - 1)
                {
                    output += ", ";
                }
            }

            output += "\n";

            return output;
        }
    }

    public void generate(int count, int nrOfVariables, String path) {
        var outputStrArr = new String[count * 2];
        var bofStrArr = new String[count * 2];

        int workSize = count / workerCount;
        if (count < workerCount) {
            throw new IllegalArgumentException();
        }


        var workerThreads = new ArrayList<Thread>();

        int remainder = count;

        for (int i = 0; i < workerCount; i++) {
            Thread workerThread;

            if (remainder < workSize)
            {
                workerThread = new Thread(new Worker("t" + i, outputStrArr, bofStrArr, (workSize * i) * 2, remainder, nrOfVariables));
            }
            else {
                workerThread = new Thread(new Worker("t" + i, outputStrArr, bofStrArr, (workSize * i) * 2, workSize, nrOfVariables));
            }
            workerThreads.add(workerThread);

            System.out.println("Created workerthread: " + "t" + i + " @ offset" + workSize * i + " remainder: " + remainder);

            workerThread.start();
            remainder -= workSize;
        }

        try {
            for (var thread : workerThreads) {
                thread.join();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }


        try {
            BufferedWriter ow;
            ow = new BufferedWriter(new FileWriter(path + "random.output.csv"));
            for (int i = 0; i < outputStrArr.length; i++) {
                if (outputStrArr[i] == null) break;
                ow.write(outputStrArr[i]);
            }
            ow.flush();
            ow.close();

            BufferedWriter ow2;
            ow2 = new BufferedWriter(new FileWriter(path + "random.bof.csv"));
            for (int i = 0; i < bofStrArr.length; i++) {
                if (bofStrArr[i] == null) break;
                ow2.write(bofStrArr[i]);
            }
            ow2.flush();
            ow2.close();
        }
        catch(IOException e)
        {
            System.err.println("IO error: " + e.getMessage());
        }


        System.out.println("Dataset generated using " + workerCount + " worker threads");
    }

    public LinearProgram generateLinearProgram(int nrOfVariables) {
        if (nrOfVariables > 24) {
            throw new IllegalArgumentException("Number of variables can not be higher than 24!");
        }

        String variablesString = "xyzwabcdefghijklmnopqrst";
        double infinity = java.lang.Double.POSITIVE_INFINITY;
        ArrayList<Double> obj_data = new ArrayList<Double>();
        ArrayList<Double> const_coef = new ArrayList<Double>();
        ArrayList<Double> const_coef2 = new ArrayList<Double>();
        ArrayList<Variable> variables = new ArrayList<Variable>();

        for (int i = 0; i < nrOfVariables; i++) {
            obj_data.add((double) rand.nextInt(-10, 10));
            const_coef.add((double) rand.nextInt(-10, 10));
            const_coef2.add((double) rand.nextInt(-10, 10));
            variables.add(new Variable(0.0, infinity, variablesString.charAt(i) + ""));
        }

        Objective obj = new Objective(obj_data);

        Constraint c1 = new Constraint(-infinity, rand.nextInt(-100, 0), "c1", const_coef);
        Constraint c2 = new Constraint(rand.nextInt(100), infinity, "c2", const_coef2);

        ArrayList<Constraint> c_list = new ArrayList<>();
        c_list.add(c1);
        c_list.add(c2);
        return new LinearProgram(obj, c_list, variables);
    }

    public LinearProgram flipSigns(LinearProgram lp, boolean originallyFeasible) {
        int variableNum = lp.getVariables().size();
        ArrayList<Integer> indices = new ArrayList<>();

        for (int i = 0; i<Math.pow(2, variableNum);i++) {
            LinearProgram copy = new LinearProgram(lp);
            String bin = Integer.toBinaryString(i);
            while (bin.length() < variableNum) {
                bin = "0" + bin;
            }
            for (int j = 0; j<variableNum; j++) {
                if (bin.charAt(j) == '1') {
                    indices.add(j);
                }
            }
            copy.flipSign(indices);
            boolean feasible = copy.solve();
            if (feasible && !originallyFeasible) {
                lp.setConvertible();
                lp.setBinaryOutputFeature(bin);
                return lp; // Originally not feasible and made feasible, CONVERTIBLE DATASET
            }
            else if (!feasible && originallyFeasible) {
                copy.setConvertible();
                copy.setBinaryOutputFeature(bin);
                return copy; // Originally feasible and made infeasible, CONVERTIBLE DATASET
            }

        }
        if (!originallyFeasible) {
            lp.setBinaryOutputFeature("0".repeat(variableNum));
            return lp; // Originally infeasible and stayed infeasible after each sign flip, INCONVERTIBLE DATASET
        }
        return null; // Originally feasible and still feasible after each sign flip, USELESS
    }
}