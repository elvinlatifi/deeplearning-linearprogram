import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class AltGenerator {
    private int convertible;
    private int inconvertible;

    private static Random rand = new Random();
    private static final String dataset_path = "..\\dataset\\";
    private int workerCount;
    private final ReentrantReadWriteLock convLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock inconvLock = new ReentrantReadWriteLock();


    public AltGenerator(int workerCount) {
        this.workerCount = workerCount;
    }
    class Worker implements Runnable
    {
        private String[] outputArrayRef;
        private String[] bofArrayRef;

        private int nrOfVariables;

        private String workerName;

        private int count;

        public Worker(String threadName, String[] outputArrayRef, String[] bofArrayRef, int nrOfVariables, int count) {
            System.out.println("Started " + threadName);

            this.workerName = threadName;
            this.outputArrayRef = outputArrayRef;
            this.bofArrayRef = bofArrayRef;
            this.nrOfVariables = nrOfVariables;
            this.count = count;
        }

        public void run() {


            while(notFinished()) {
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
                    // Read
                    convLock.readLock().lock();
                    if (convertible >= count) {
                        convLock.readLock().unlock();
                        continue;
                    }
                    convLock.readLock().unlock();


                    // Update
                    convLock.writeLock().lock();
                    inconvLock.writeLock().lock();
                    convertible++;
                    writeDataToArray(result.getRelevantData(), result.getBinaryOutputFeature(), convertible + inconvertible);
                    inconvLock.writeLock().unlock();
                    convLock.writeLock().unlock();

                }
                else {
                    // Read
                    inconvLock.readLock().lock();
                    if (inconvertible >= count) {
                        inconvLock.readLock().unlock();
                        continue;
                    }
                    inconvLock.readLock().unlock();

                    // Update
                    convLock.writeLock().lock();
                    inconvLock.writeLock().lock();
                    inconvertible++;
                    writeDataToArray(result.getRelevantData(), result.getBinaryOutputFeature(), convertible + inconvertible);
                    inconvLock.writeLock().unlock();
                    convLock.writeLock().unlock();
                }

            }

        }

        private boolean notFinished() {
            convLock.readLock().lock();
            inconvLock.readLock().lock();
            boolean ret = convertible < count || inconvertible < count;
            inconvLock.readLock().unlock();
            convLock.readLock().unlock();
            return ret;
        }



        private void writeDataToArray(String[] input1, String[] input2, int curr_count)
        {
            var str1 = getCsvRowFromStrArray(input1);
            var str2 = getCsvRowFromStrArray(input2);

            try {
                outputArrayRef[curr_count-1] = str1;
                bofArrayRef[curr_count-1] = str2;
            }
            catch(Exception e)
            {
                System.err.println(e.getMessage());
            }

        }

        private String getCsvRowFromStrArray(String[] input) {
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

        if (count < workerCount) {
            throw new IllegalArgumentException();
        }

        var workerThreads = new ArrayList<Thread>();

        for (int i = 0; i < workerCount; i++) {
            var workerThread = new Thread(new Worker("t" + i, outputStrArr, bofStrArr, nrOfVariables, count));
            workerThreads.add(workerThread);
            workerThread.start();
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
                if (outputStrArr[i] == null)
                    break;
                ow.write(outputStrArr[i]);
            }
            ow.flush();
            ow.close();

            BufferedWriter ow2;
            ow2 = new BufferedWriter(new FileWriter(path + "random.bof.csv"));
            for (int i = 0; i < bofStrArr.length; i++) {
                if (bofStrArr[i] == null)
                    break;
                ow2.write(bofStrArr[i]);
            }
            ow2.flush();
            ow2.close();
        }
        catch(IOException e) {
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