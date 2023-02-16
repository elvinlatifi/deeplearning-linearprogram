import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

public class ThreadedRandomizer {

    private static Random rand = new Random();

    //private static final int WORKER_SIZE_DIVIDER = 16; //count / WORKER_SIZE_DIVIDER = worker count
    private static final int WORKER_WORK_SIZE = 5000;
    //private static final int WORKER_THRESHOLD = 1000;

    class ThreadedWorker implements Runnable
    {
        private String[] output_array_ref;
        private String[] bof_array_ref;
        private int worker_offset;
        private int worker_size;
        private int nrOfVariables;

        private String temp_name;

        public ThreadedWorker(String temp_name, String[] output_array_ref, String[] bof_array_ref, int worker_offset, int count, int nrOfVariables)
        {
            System.out.println("Started " + temp_name + " worker_offset: " + worker_offset + " worker_size = " + count);

            this.temp_name = temp_name;
            this.output_array_ref = output_array_ref;
            this.bof_array_ref = bof_array_ref;
            if (count < WORKER_WORK_SIZE)
            {
                worker_size = count;
            }
            else {
                worker_size = WORKER_WORK_SIZE;
            }

            this.worker_offset = worker_offset;
            this.nrOfVariables = nrOfVariables;
        }

        public void run()
        {
            int convertible = 0;
            int inconvertible = 0;

            while(convertible != worker_size || inconvertible != worker_size) {
                LinearProgram lp = generateLinearProgram(nrOfVariables);
                LinearProgram result;

                if (lp.solve()) {
                    result = flipSigns(lp, true);
                }
                else {
                    result = flipSigns(lp, false);
                }
                if (result == null) {
                    // DO NOTHING
                    continue;
                }
                else if (result.isConvertible()) {
                    if (convertible == worker_size) {
                        continue;
                    }

                    writeDataToArray(result.getRelevantData(), result.getBinaryOutputFeature(), convertible + inconvertible);

                    convertible++;
                }
                else {
                    if (inconvertible == worker_size) {
                        continue;
                    }

                    writeDataToArray(result.getRelevantData(), result.getBinaryOutputFeature(), convertible + inconvertible);

                    inconvertible++;
                }
            }

            System.out.println(temp_name  + ": Finished generating " + worker_size * 2 + " lps @ offset: " + worker_offset);
        }

        private void writeDataToArray(String[] input1, String[] input2, int curr_count)
        {
            var str1 = getCsvRowFromStrArray(input1);
            var str2 = getCsvRowFromStrArray(input2);

            try {
                output_array_ref[worker_offset + curr_count] = str1;
                bof_array_ref[worker_offset + curr_count] = str2;
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
        var output_str_arr = new String[count * 2];
        var bof_str_arr = new String[count * 2];

        int worker_count = count / WORKER_WORK_SIZE;

        if (count % WORKER_WORK_SIZE != 0)
        {
            worker_count++;
        }

        if (count < WORKER_WORK_SIZE)
        {
            worker_count = 1;
            System.out.println("Only using one thread, because totalCount is less than then the threshold(" + WORKER_WORK_SIZE + ")");
            var worker_thread = new Thread(new ThreadedWorker("t1", output_str_arr, bof_str_arr, 0, count, nrOfVariables));
            worker_thread.start();
            try {
                worker_thread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        else {
            var workerThreads = new ArrayList<Thread>();

            int remainder = count;

            for (int i = 0; i < worker_count; i++) {
                Thread worker_thread;

                if (remainder < WORKER_WORK_SIZE)
                {
                    worker_thread = new Thread(new ThreadedWorker("t" + i, output_str_arr, bof_str_arr, (WORKER_WORK_SIZE * i) * 2, remainder, nrOfVariables));
                }
                else {
                    worker_thread = new Thread(new ThreadedWorker("t" + i, output_str_arr, bof_str_arr, (WORKER_WORK_SIZE * i) * 2, WORKER_WORK_SIZE, nrOfVariables));
                }
                workerThreads.add(worker_thread);

                System.out.println("Created workerthread: " + "t" + i + " @ offset" + WORKER_WORK_SIZE * i + " remainder: " + remainder);

                worker_thread.start();
                remainder -= WORKER_WORK_SIZE;
            }

            try {
                for (var thread : workerThreads) {
                    thread.join();
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        try {
            BufferedWriter ow;
            ow = new BufferedWriter(new FileWriter(path + "random.output.csv"));
            for (int i = 0; i < output_str_arr.length; i++) {
                if (output_str_arr[i] == null) break;
                ow.write(output_str_arr[i]);
            }
            ow.flush();
            ow.close();

            BufferedWriter ow2;
            ow2 = new BufferedWriter(new FileWriter(path + "random.bof.csv"));
            for (int i = 0; i < bof_str_arr.length; i++) {
                if (bof_str_arr[i] == null) break;
                ow2.write(bof_str_arr[i]);
            }
            ow2.flush();
            ow2.close();
        }
        catch(IOException e)
        {
            System.err.println("IO error: " + e.getMessage());
        }


        System.out.println("Dataset generated using " + worker_count + " worker threads");
    }

    private LinearProgram generateLinearProgram(int nrOfVariables) {
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

    private LinearProgram flipSigns(LinearProgram lp, boolean originallyFeasible) {
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
