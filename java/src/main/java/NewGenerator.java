import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;

public class NewGenerator {
    private static Random rand = new Random();

    private static final int OBJECTIVE_CONSTANT = 50;
    private final double infinity = java.lang.Double.POSITIVE_INFINITY;

    private int convertible;
    private int inconvertible;
    private final ReentrantReadWriteLock convLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock inconvLock = new ReentrantReadWriteLock();
    private final String path;
    private final int nrOfVariables;
    private final int workerCount;


    public NewGenerator(int workerCount, int nrOfVariables, String path) {
        this.workerCount = workerCount;
        this.nrOfVariables = nrOfVariables;
        this.path = path +  "var" + nrOfVariables + "\\";
    }

    class Worker implements Runnable {
        MPSolver solver;

        private String[] outputArrayRef;
        private String[] bofArrayRef;

        private int nrOfVariables;

        private int count;

        ArrayList<MPVariable> mpVariables = new ArrayList<>();
        MPConstraint firstConstraint;
        MPConstraint secondConstraint;
        MPObjective objective;


        public Worker(String threadName, String[] outputArrayRef, String[] bofArrayRef, int nrOfVariables, int count) {
            System.out.println("Started " + threadName);

            this.outputArrayRef = outputArrayRef;
            this.bofArrayRef = bofArrayRef;
            this.nrOfVariables = nrOfVariables;
            this.count = count;
            initializeSolver();
            this.objective = solver.objective();
            this.objective.setMaximization();
            this.firstConstraint = solver.makeConstraint("c1");
            this.secondConstraint = solver.makeConstraint("c2");
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
                if (solve(lp)) {
                    if (solver.objective().value() < OBJECTIVE_CONSTANT) {
                        boolean result = flipSigns(lp);
                        if (result) {
                            if (convertible >= count / 2) continue;
                            convLock.writeLock().lock();
                            convertible++;
                            convLock.writeLock().unlock();
                        }
                        else {
                            if (inconvertible >= count / 2) continue;
                            inconvLock.writeLock().lock();
                            inconvertible++;
                            inconvLock.writeLock().unlock();
                        }
                        writeDataToArray(lp.getRelevantData(), result ? 1:0);
                    }
                }
            }
        }

        private boolean notFinished() {
            convLock.readLock().lock();
            inconvLock.readLock().lock();

            var actual_count = count / 2;
            //System.out.println("Inconv: " + inconvertible + " Conv: " + convertible);
            boolean ret = convertible < actual_count || inconvertible < actual_count;
            inconvLock.readLock().unlock();
            convLock.readLock().unlock();
            return ret;
        }

        private void writeDataToArray(String[] input1, int input2) {
            var str1 = getCsvRowFromStrArray(input1);
            var str2 = input2 + "\n";

            convLock.readLock().lock();
            inconvLock.readLock().lock();

            var curr_count = convertible + inconvertible;

            convLock.readLock().unlock();
            inconvLock.readLock().unlock();

            try {
                outputArrayRef[curr_count-1] = str1;
                bofArrayRef[curr_count-1] = str2;
            }
            catch(Exception e) {
                System.err.println(e.getMessage());
            }
        }

        private String getCsvRowFromStrArray(String[] input) {
            String output = "";

            for (int i = 0; i < input.length; i++) {
                output += input[i] + ", ";
            }
            output += OBJECTIVE_CONSTANT;

            return output;
        }

        private boolean flipSigns(LinearProgram lp) {
            int[] indices = new int[2];

            LinearProgram copy = new LinearProgram(lp);

            for (int i = 0; i<nrOfVariables; i++) {
                for (int j = i+1; j<nrOfVariables; j++) {
                    indices[0] = i;
                    indices[1] = j;
                    copy.flipSign(indices);
                    if (solve(copy)) {
                        if (solver.objective().value() > OBJECTIVE_CONSTANT) {
                            return true;
                        }
                    }
                }
            }
            return false;
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

        new File(path).mkdirs();

        try {
            BufferedWriter ow;
            ow = new BufferedWriter(new FileWriter(path + "output.csv"));
            for (int i = 0; i < outputStrArr.length; i++) {
                if (outputStrArr[i] == null)
                    break;
                ow.write(outputStrArr[i] + "\n");
            }
            ow.flush();
            ow.close();

            BufferedWriter ow2;
            ow2 = new BufferedWriter(new FileWriter(path + "bof.csv"));
            for (int i = 0; i < bofStrArr.length; i++) {
                if (bofStrArr[i] == null)
                    break;
                ow2.write(bofStrArr[i] + "\n");
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
            obj_data.add((double) rand.nextInt(-10, 10));
            const_coef.add((double) rand.nextInt(-10, 10));
            const_coef2.add((double) rand.nextInt(-10, 10));
            variables.add(new Variable(0.0, infinity, variablesString.charAt(i) + ""));
        }

        Objective obj = new Objective(obj_data);

        Constraint c1 = new Constraint(-infinity, rand.nextInt(100), "c1", const_coef);
        Constraint c2 = new Constraint(rand.nextInt(-100, 0), infinity, "c2", const_coef2);

        ArrayList<Constraint> c_list = new ArrayList<>();
        c_list.add(c1);
        c_list.add(c2);
        return new LinearProgram(obj, c_list, variables);
    }

    private double getRandomSignIntegerOne() {
        return Math.random() > 0.5 ? 1 : -1;
    }

}
