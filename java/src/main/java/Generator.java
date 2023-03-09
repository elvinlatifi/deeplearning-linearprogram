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

public class Generator {
    private static Random rand = new Random();
    private final double infinity = java.lang.Double.POSITIVE_INFINITY;
    private int positive;
    private int negative;
    private final ReentrantReadWriteLock positiveLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock negativeLock = new ReentrantReadWriteLock();
    private final String path;
    private final int nrOfVariables;
    private final int workerCount;


    public Generator(int workerCount, int nrOfVariables, String path) {
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

        int numberOfRounds = 0;
        int numberOfSolvables = 0;
        int numberOfUseful = 0;
        private double objectiveConst;

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
            for (int i = 0; i < nrOfVariables; i++) {
                mpVariables.add(solver.makeNumVar(0.0, infinity, "var" + i));
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
            var start = System.currentTimeMillis();
            while(notFinishedNegative()) {
                numberOfRounds++;
                generateNegativeExamples();

            }
            var end = System.currentTimeMillis();
            System.out.println("Negatives generation time: " + (end - start) + " ms");

            var start1 = System.currentTimeMillis();
            while (notFinishedPositive()) {
                generatePositiveExamples();
            }
            var end1 = System.currentTimeMillis();
            System.out.println("Positives generation time: " + (end1 - start1) + " ms");
        }

        int nmrFeasible1 = 0;
        int nmrNotFeasible1 = 0;

        private void generateNegativeExamples() {
            LinearProgram lp = generateLinearProgram(nrOfVariables);
            boolean feasible = solve(lp);
            if (feasible) {
                nmrFeasible1++;
                numberOfSolvables++;
                objectiveConst = solver.objective().value();
                objectiveConst = flipSignsNegative(lp);
            }
            else {
                nmrNotFeasible1++;
                System.out.println("Negative feasible ratio: feasible: " + nmrFeasible1 + " notfeasible1: " + nmrNotFeasible1);
                return;
            }
            negativeLock.writeLock().lock();
            positiveLock.writeLock().lock();
            negative++;
            writeDataToArray(lp.getRelevantData(), 0);
            positiveLock.writeLock().unlock();
            negativeLock.writeLock().unlock();
        }

        int nmrFeasible = 0;
        int nmrNotFeasible = 0;

        private void generatePositiveExamples() {
            LinearProgram lp = generateLinearProgram(nrOfVariables);
            boolean feasible = solve(lp);
            boolean valid = false;

            if (feasible) {
                nmrFeasible++;
                objectiveConst = solver.objective().value();
                valid = flipSignsPositive(lp);
            }
            else {
                nmrNotFeasible++;
                System.out.println("Positive feasible ratio: feasible: " + nmrFeasible + " notfeasible1: " + nmrNotFeasible);
                return;
            }
            if (!valid) {
                return;
            }
            negativeLock.writeLock().lock();
            positiveLock.writeLock().lock();
            positive++;
            writeDataToArray(lp.getRelevantData(), 1);
            positiveLock.writeLock().unlock();
            negativeLock.writeLock().unlock();
        }

        private double flipSignsNegative(LinearProgram lp) {
            int[] indices = new int[2];

            for (int i = 0; i<nrOfVariables; i++) {
                for (int j = i+1; j<nrOfVariables; j++) {
                    LinearProgram copy = new LinearProgram(lp);
                    indices[0] = i;
                    indices[1] = j;
                    copy.flipSign(indices);
                    if (solve(copy)) {
                        if (solver.objective().value() > objectiveConst) {
                            objectiveConst = solver.objective().value();
                        }
                    }
                }
            }
            return objectiveConst + 1;
        }

        private boolean flipSignsPositive(LinearProgram lp) {
            int[] indices = new int[2];

            for (int i = 0; i<nrOfVariables; i++) {
                for (int j = i+1; j<nrOfVariables; j++) {
                    LinearProgram copy = new LinearProgram(lp);
                    indices[0] = i;
                    indices[1] = j;
                    copy.flipSign(indices);
                    if (solve(copy)) {
                        if (solver.objective().value() > objectiveConst) {
                            return true;
                        }
                        else if (solver.objective().value() < objectiveConst) {
                            objectiveConst = solver.objective().value();
                            return true;
                        }
                    }
                }
            }

            return false;
        }

        int debug_count = 0;

        private boolean notFinishedNegative() {
            if (debug_count > 100)
            {
                //System.out.println("Negative: " + negative);
                debug_count = 0;
            }
            else {
                debug_count++;
            }

            negativeLock.readLock().lock();
            positiveLock.readLock().lock();

            boolean ret = negative < (count / 2);

            positiveLock.readLock().unlock();
            negativeLock.readLock().unlock();
            return ret;
        }
        private boolean notFinishedPositive() {
            if (debug_count > 100)
            {
               System.out.println("Positives: " + positive);
                debug_count = 0;
            }
            else {
                debug_count++;
            }

            negativeLock.readLock().lock();
            positiveLock.readLock().lock();

            boolean ret = positive < (count / 2);

            positiveLock.readLock().unlock();
            negativeLock.readLock().unlock();
            return ret;
        }

        private void writeDataToArray(String[] input1, int input2) {
            var str1 = getCsvRowFromStrArray(input1);
            var str2 = String.valueOf(input2);

            var curr_count = positive + negative;

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
            output += objectiveConst;

            return output;
        }
    }

    public void generate(int count) {
        var outputStrArr = new String[count * 2];
        var bofStrArr = new String[count * 2];

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
        ArrayList<Double> obj_data = new ArrayList<>();
        ArrayList<Double> const_coef = new ArrayList<>();
        ArrayList<Double> const_coef2 = new ArrayList<>();
        ArrayList<Variable> variables = new ArrayList<>();

        for (int i = 0; i < nrOfVariables; i++) {
            obj_data.add((double)rand.nextInt(-10, 10));
            const_coef.add((double)rand.nextInt(-10, 10));
            const_coef2.add((double)rand.nextInt(-10, 10));
            variables.add(new Variable(0.0, infinity, "var" + i));
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
        return Math.random() > 0.5 ? -1 : 1;
    }
}
