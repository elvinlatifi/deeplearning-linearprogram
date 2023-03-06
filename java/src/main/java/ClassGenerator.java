import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;

public class ClassGenerator {
    private static Random rand = new Random();
    private final double infinity = java.lang.Double.POSITIVE_INFINITY;
    private final ReentrantReadWriteLock totalLock = new ReentrantReadWriteLock();
    HashMap<String, Integer> classes = new HashMap<>();
    private final String path;
    private final int nrOfVariables;
    private final int workerCount;
    private int total;

    ConcurrentHashMap<String, Integer> linearPrograms = new ConcurrentHashMap<>();

    public ClassGenerator(int workerCount, int nrOfVariables, String path) {
        this.workerCount = workerCount;
        this.nrOfVariables = nrOfVariables;
        this.path = path;
        initializeClasses();
    }

    private void initializeClasses() {
        for (int i=0; i<Math.pow(2, nrOfVariables);i++) {
            String bin = Integer.toBinaryString(i);
            while (bin.length() < nrOfVariables) {
                bin = "0" + bin;
            }
            classes.put(bin, i);
        }
    }

    class Worker implements Runnable {
        MPSolver solver;

        private String[] outputArrayRef;
        private String[] bofArrayRef;

        private int nrOfVariables;

        private String workerName;

        private int count;

        private int quota;

        ArrayList<MPVariable> mpVariables = new ArrayList<>();
        MPConstraint firstConstraint;
        MPConstraint secondConstraint;
        MPObjective objective;


        public Worker(String threadName, String[] outputArrayRef, String[] bofArrayRef, int nrOfVariables, int count) {
            System.out.println("Started " + threadName);

            this.workerName = threadName;
            this.outputArrayRef = outputArrayRef;
            this.bofArrayRef = bofArrayRef;
            this.nrOfVariables = nrOfVariables;
            this.count = count;
            initializeSolver();
            this.objective = solver.objective();
            this.objective.setMaximization();
            this.firstConstraint = solver.makeConstraint("c1");
            this.secondConstraint = solver.makeConstraint("c2");
            this.quota = count / (int)Math.pow(2, nrOfVariables);

            System.out.println("Quota: " + quota);
        }

        private void initializeSolver() {
            this.solver = MPSolver.createSolver("GLOP");
            String variablesString = "xyzwabcdefghijklmnopqrst";
            for (int i = 0; i < nrOfVariables; i++) {
                mpVariables.add(solver.makeNumVar(0.0, infinity, variablesString.charAt(i) + ""));
            }
        }


        private boolean solve(LinearProgram lp) {
            Constraint first = lp.getConstraints().get(0);
            Constraint second = lp.getConstraints().get(1);
            firstConstraint.setBounds(-infinity, first.getUb());
            secondConstraint.setBounds(second.getLb(), infinity);
            for (int i = 0; i < nrOfVariables; i++) {
                firstConstraint.setCoefficient(mpVariables.get(i), first.getCoefficients().get(i));
                secondConstraint.setCoefficient(mpVariables.get(i), second.getCoefficients().get(i));
                objective.setCoefficient(mpVariables.get(i), lp.getObjective().getCoefficients().get(i));
            }
            final MPSolver.ResultStatus resultStatus = solver.solve();
            return resultStatus == MPSolver.ResultStatus.FEASIBLE || resultStatus == MPSolver.ResultStatus.OPTIMAL;
        }

        public void run() {

            while(notFinished()) {
                LinearProgram lp = generateLinearProgram(nrOfVariables);
                LinearProgram result;

                if (solve(lp)) {
                    result = flipSigns(lp, true);
                }
                else {
                    result = flipSigns(lp, false);
                }

                if (result == null) {
                    continue;
                }

                if (linearPrograms.putIfAbsent(result.getBofAsStr(), 1) != null) {
                    for (int i=0; i<100;i++) {
                        int curCount = linearPrograms.get(result.getBofAsStr());
                        if (curCount >= quota) {
                            break;
                        }
                        if (linearPrograms.replace(result.getBofAsStr(), curCount, curCount+1)) {
                            totalLock.writeLock().lock();
                            total++;
                            writeDataToArray(result.getRelevantData(), result.getBinaryOutputFeature(), total);
                            totalLock.writeLock().unlock();
                            break;
                        }
                    }
                }
                else {
                    totalLock.writeLock().lock();
                    total++;
                    writeDataToArray(result.getRelevantData(), result.getBinaryOutputFeature(), total);
                    totalLock.writeLock().unlock();
                }

            }
            System.out.println(linearPrograms);
            System.out.println(linearPrograms.keySet().size());

        }

        private boolean notFinished() {
            totalLock.readLock().lock();
            boolean ret = total < count;
            totalLock.readLock().unlock();
            return ret;
        }

        private void writeDataToArray(String[] input1, String[] input2, int curr_count) {
            var str1 = getCsvRowFromStrArray(input1, false);
            var str2 = getCsvRowFromStrArray(input2, true);

            try {
                outputArrayRef[curr_count-1] = str1;
                bofArrayRef[curr_count-1] = str2;
            }
            catch(Exception e) {
                System.err.println(e.getMessage());
            }
        }

        private String getCsvRowFromStrArray(String[] input, boolean bof) {
            String output = "";

            for (int i = 0; i < input.length; i++) {
                output += input[i];
                if (!bof) {
                    if (i < input.length-1) {
                        output += ", ";
                    }
                }
            }

            return output;
        }

        private LinearProgram flipSigns(LinearProgram lp, boolean originallyFeasible) {
            int variableNum = lp.getVariables().size();
            ArrayList<Integer> indices = new ArrayList<>();

            HashSet<String> binsFound = new HashSet<>();

            LinearProgram ret = null;

            boolean moreThanOne = false;

            while (binsFound.size() < Math.pow(2, variableNum)) {
                LinearProgram copy = new LinearProgram(lp);

                int i = rand.nextInt((int)Math.pow(2, variableNum));

                String bin = Integer.toBinaryString(i);
                while (bin.length() < variableNum) {
                    bin = "0" + bin;
                }

                if (!binsFound.add(bin)) {
                    continue;
                }

                if (linearPrograms.get(bin) != null && linearPrograms.get(bin) >= quota) {
                    continue;
                }

                for (int j = 0; j<variableNum; j++) {
                    if (bin.charAt(j) == '1') {
                        indices.add(j);
                    }
                }

                copy.flipSign(indices);
                boolean feasible = solve(copy);
                if (feasible && !originallyFeasible) {
                    if (moreThanOne) {
                        return null;
                    }
                    lp.setConvertible();
                    lp.setBinaryOutputFeature(bin);
                    ret = lp;
                    moreThanOne = true;
                }
                else if (!feasible && originallyFeasible) {
                    if (moreThanOne) {
                        return null;
                    }
                    copy.setConvertible();
                    copy.setBinaryOutputFeature(bin);
                    ret = copy;
                    moreThanOne = true;
                }
            }

            if (ret != null)
                return ret;
            if (!originallyFeasible) {
                lp.setBinaryOutputFeature("0".repeat(variableNum));
                return lp; // Originally infeasible and stayed infeasible after each sign flip, INCONVERTIBLE DATASET
            }
            return null; // Originally feasible and still feasible after each sign flip, USELESS
        }
    }

    public void generate(int count) {
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
                ow.write(outputStrArr[i] + "\n");
            }
            ow.flush();
            ow.close();

            BufferedWriter ow2;
            ow2 = new BufferedWriter(new FileWriter(path + "random.bof.csv"));
            for (int i = 0; i < bofStrArr.length; i++) {
                if (bofStrArr[i] == null)
                    break;
                ow2.write(classes.get(bofStrArr[i]) + "\n");
            }
            ow2.flush();
            ow2.close();
        }
        catch(IOException e) {
            System.err.println("IO error: " + e.getMessage());
        }


        System.out.println("Dataset generated using " + workerCount + " worker threads");
    }

    private LinearProgram generateLinearProgram(int nrOfVariables) {
        if (nrOfVariables > 24) {
            throw new IllegalArgumentException("Number of variables can not be higher than 24!");
        }

        String variablesString = "xyzwabcdefghijklmnopqrst";
        ArrayList<Double> obj_data = new ArrayList<Double>();
        ArrayList<Double> const_coef = new ArrayList<Double>();
        ArrayList<Double> const_coef2 = new ArrayList<Double>();
        ArrayList<Variable> variables = new ArrayList<Variable>();

        for (int i = 0; i < nrOfVariables; i++) {
            obj_data.add(getRandomSignIntegerOne());
            const_coef.add(getRandomSignIntegerOne());
            const_coef2.add(getRandomSignIntegerOne());
            variables.add(new Variable(0.0, infinity, variablesString.charAt(i) + ""));
        }

        Objective obj = new Objective(obj_data);

        Constraint c1 = new Constraint(-infinity, getRandomSignIntegerOne(), "c1", const_coef);
        Constraint c2 = new Constraint(getRandomSignIntegerOne(), infinity, "c2", const_coef2);

        ArrayList<Constraint> c_list = new ArrayList<>();
        c_list.add(c1);
        c_list.add(c2);
        return new LinearProgram(obj, c_list, variables);
    }

    private double getRandomSignIntegerOne() {
        return Math.random() > 0.5 ? 1 : -1;
    }

}
