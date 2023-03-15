import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;

import java.util.ArrayList;

public class LinearProgram {
    transient private boolean convertible;
    private Objective objective;

    private ArrayList<Constraint> constraints;

    private String[] binaryOutputFeature;

    private int nmrOfVariables;

    LinearProgram(Objective objective, ArrayList<Constraint> constraints, int nmrOfVariables) {
        this.objective = objective;
        this.constraints = constraints;
        this.nmrOfVariables = nmrOfVariables;
    }

    LinearProgram(LinearProgram lp) {
        this.objective = new Objective(lp.getObjective());

        this.constraints = new ArrayList<Constraint>();

        for (var c : lp.getConstraints())
        {
            this.constraints.add(new Constraint(c));
        }
    }

    public Objective getObjective() {
        return objective;
    }

    public ArrayList<Constraint> getConstraints() {
        return constraints;
    }


    /*
    protected boolean solve() {
        MPSolver solver = generateSolver();
        //System.out.println(solver.exportModelAsLpFormat());

        final MPSolver.ResultStatus resultStatus = solver.solve();

        //System.out.println("ResultStatus: " + resultStatus.toString());

        if (resultStatus == MPSolver.ResultStatus.FEASIBLE || resultStatus == MPSolver.ResultStatus.OPTIMAL) {
            //System.out.println("Solution:");
            //System.out.println("Objective value = " + objective.value());
            //System.out.println("x = " + mpVariables.get(0).solutionValue());
            //System.out.println("y = " + mpVariables.get(1).solutionValue());
            solver.clear();
            return true;
        } else {
            //System.err.println("Did not find a solution");
            solver.clear();
            return false;
        }

    }
    */

    public void flipSign(ArrayList<Integer> indices) {
        objective.flipSign(indices);
    }
    
    public void flipSign(int[] indices) {
        objective.flipSign(indices);
    }

    public void setConvertible() {
        convertible = true;
    }

    public boolean isConvertible() {
        return convertible;
    }

    public String[] getRelevantData() {
        double nrOfVariables = nmrOfVariables;
        ArrayList<Double> data = new ArrayList<>();

        // Get the coefficients for the objective
        for (int i=0;i<nrOfVariables;i++) {
            data.add(objective.getCoefficients().get(i));
        }

        // Get the coefficients for the first constraint
        for (int i=0; i<nrOfVariables;i++) {
            data.add(constraints.get(0).getCoefficients().get(i));
        }

        // Get the upper bound
        data.add(constraints.get(0).getUb());

        // Get the coefficients for the second constraint, use 0 as padding if less than 4 variables
        for (int i=0; i<nrOfVariables;i++) {
            data.add(constraints.get(1).getCoefficients().get(i));
        }

        // Get lower bound
        data.add(constraints.get(1).getUb());

        String[] arr = new String[data.size()];
        int i = 0;
        for (Double d : data) {
            arr[i] = d.toString();
            i++;
        }
        return arr;
    }
    public void setBinaryOutputFeature(String bof) {
        binaryOutputFeature = new String[nmrOfVariables];
        for (int i=0;i< nmrOfVariables;i++) {
            binaryOutputFeature[i] = bof.charAt(i) +"";
        }
    }

    public String[] getBinaryOutputFeature() {
        return binaryOutputFeature;
    }

    public String getBofAsStr() {
        String result = "";
        for (String s : binaryOutputFeature) {
            result += s;
        }
        return result;
    }


}
