import com.google.ortools.Loader;

import java.util.ArrayList;
import java.util.Random;
public class Randomizer {
    private static Random rand = new Random();
    double infinity = java.lang.Double.POSITIVE_INFINITY;
    public static void main(String[] args)
    {
        Loader.loadNativeLibraries();
        //Test();
        generateData();
    }

    public static void Test() {

        double infinity = java.lang.Double.POSITIVE_INFINITY;

        ArrayList<Double> obj_data = new ArrayList<Double>();

        obj_data.add(3.0);
        obj_data.add(4.0);

        Objective obj = new Objective(obj_data);

        ArrayList<Double> const_coef = new ArrayList<Double>();

        const_coef.add(1.0);
        const_coef.add(2.0);

        Constraint c1 = new Constraint(-infinity, 14, "c1", const_coef);

        ArrayList<Variable> variables = new ArrayList<Variable>();

        variables.add(new Variable(0.0, infinity, "x"));
        variables.add(new Variable(0.0, infinity, "y"));
        var c_list = new ArrayList<Constraint>();
        c_list.add(c1);

        var program = new LinearProgram(obj, c_list, variables);

        program.solve();
    }

    public static void generateData() {

        LinearProgram lp = generateLinearProgram(rand.nextInt(2, 5)); // Generate Linear Program with 2,3 or 4 variables
        LinearProgram result;
        if (lp.solve()) {
            result = flipSigns(lp, true);
        }
        else {
            result = flipSigns(lp, false);
        }
        if (result == null) {
            // DO NOTHING
            System.out.println("USELESS");
        }
        else if (result.isConvertible()) {
            // SEND TO CONVERTIBLE DATASET
            System.out.println("CONVERTIBLE");
        }
        else {
            // SEND TO INCONVERTIBLE DATASET
            System.out.println("INCOVERTIBLE");
        }


    }

    private static LinearProgram generate2() {
        double infinity = java.lang.Double.POSITIVE_INFINITY;
        ArrayList<Double> obj_data = new ArrayList<Double>();

        obj_data.add(rand.nextDouble(100));
        obj_data.add(rand.nextDouble(100));

        Objective obj = new Objective(obj_data);

        ArrayList<Double> const_coef = new ArrayList<Double>();

        const_coef.add(rand.nextDouble(-50, 50));
        const_coef.add(rand.nextDouble(-50, 50));

        ArrayList<Double> const_coef2 = new ArrayList<Double>();

        const_coef2.add(rand.nextDouble(-50, 50));
        const_coef2.add(rand.nextDouble(-50, 50));

        Constraint c1 = new Constraint(-infinity, rand.nextDouble(100), "c1", const_coef);
        Constraint c2 = new Constraint(rand.nextDouble(-100, 0), infinity, "c2", const_coef2);

        ArrayList<Variable> variables = new ArrayList<Variable>();

        variables.add(new Variable(0.0, infinity, "x"));
        variables.add(new Variable(0.0, infinity, "y"));
        ArrayList<Constraint> c_list = new ArrayList<>();
        c_list.add(c1);
        c_list.add(c2);

        return new LinearProgram(obj, c_list, variables);
    }

    private static LinearProgram generateLinearProgram(int nrOfVariables) {
        String variablesString = "xyzm";
        double infinity = java.lang.Double.POSITIVE_INFINITY;
        ArrayList<Double> obj_data = new ArrayList<Double>();
        ArrayList<Double> const_coef = new ArrayList<Double>();
        ArrayList<Double> const_coef2 = new ArrayList<Double>();
        ArrayList<Variable> variables = new ArrayList<Variable>();

        for (int i = 0; i < nrOfVariables; i++) {
            obj_data.add(rand.nextDouble(100));
            const_coef.add(rand.nextDouble(-50, 50));
            const_coef2.add(rand.nextDouble(-50, 50));
            variables.add(new Variable(0.0, infinity, variablesString.charAt(i) + ""));
        }

        Objective obj = new Objective(obj_data);

        Constraint c1 = new Constraint(-infinity, rand.nextDouble(100), "c1", const_coef);
        Constraint c2 = new Constraint(rand.nextDouble(-100, 0), infinity, "c2", const_coef2);

        ArrayList<Constraint> c_list = new ArrayList<>();
        c_list.add(c1);
        c_list.add(c2);
        return new LinearProgram(obj, c_list, variables);
    }


    private static LinearProgram flipSigns(LinearProgram lp, boolean originallyFeasible) {
        int variableNum = lp.getVariables().size();
        ArrayList<Integer> indices = new ArrayList<>();

        for (int i = 0; i<Math.pow(2, variableNum);i++) {
            LinearProgram copy = lp;
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
                copy.setConvertible();
                return copy; // Originally not feasible and made feasible, CONVERTIBLE DATASET
            }
            else if (!feasible && originallyFeasible) {
                copy.setConvertible();
                return copy; // Originally feasible and made infeasible, CONVERTIBLE DATASET
            }

        }
        if (!originallyFeasible) {
            return lp; // Originally infeasible and stayed infeasible after each sign flip, INCONVERTIBLE DATASET
        }
        return null; // Originally feasible and still feasible after each sign flip, USELESS
    }
}
