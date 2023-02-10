import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;

import java.util.ArrayList;

public class LinearProgram {
    transient private boolean convertible;
    private Objective objective;

    private ArrayList<Constraint> constraints;
    private ArrayList<Variable> variables;

    String[] binaryOutputFeature = new String[4];

    LinearProgram(Objective objective, ArrayList<Constraint> constraints, ArrayList<Variable> variables) {
        this.objective = objective;
        this.constraints = constraints;
        this.variables = variables;
    }

    LinearProgram(LinearProgram lp) {
        this.objective = new Objective(lp.getObjective());

        this.constraints = new ArrayList<Constraint>();

        for (var c : lp.getConstraints())
        {
            this.constraints.add(new Constraint(c));
        }

        this.variables = new ArrayList<Variable>();

        for (var v : lp.getVariables())
        {
            this.variables.add(new Variable(v));
        }
    }

    public Objective getObjective() {
        return objective;
    }

    public ArrayList<Constraint> getConstraints() {
        return constraints;
    }

    public ArrayList<Variable> getVariables() {
        return variables;
    }

    private MPSolver generateSolver() {
        MPSolver solver = MPSolver.createSolver("GLOP");

        ArrayList<MPVariable> mpVariables = new ArrayList<>();

        for (Variable var : variables) {
            mpVariables.add(solver.makeNumVar(var.getLb(), var.getUb(), var.getName()));
        }

        for (Constraint constr : constraints) {
            MPConstraint c = solver.makeConstraint(constr.getLb(), constr.getUb(), constr.getName());
            ArrayList<Double> constr_coef = constr.getCoefficients();
            for (int i=0; i<mpVariables.size(); i++) {
                c.setCoefficient(mpVariables.get(i),constr_coef.get(i));
            }

        }

        MPObjective objective = solver.objective();

        ArrayList<Double> obj_coef = this.objective.getCoefficients();

        for (int i = 0; i < mpVariables.size(); i++) {
            objective.setCoefficient(mpVariables.get(i), obj_coef.get(i));
        }

        objective.setMaximization();

        return solver;
    }

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
            return true;
        } else {
            //System.err.println("Did not find a solution");
            return false;
        }

    }

    public void flipSign(ArrayList<Integer> indices) {
        objective.flipSign(indices);
    }

    public void setConvertible() {
        convertible = true;
    }

    public boolean isConvertible() {
        return convertible;
    }

    public String[] getRelevantData() {
        double nrOfVariables = variables.size();
        ArrayList<String> data = new ArrayList<>();

        // Add number of variables
        data.add(nrOfVariables + "");

        // Get the coefficients for the objective, use 0 as padding if less than 4 variables
        for (int i=0;i<4;i++) {
            if (i >= nrOfVariables) {
                data.add("0");
            }
            else {
                data.add(objective.getCoefficients().get(i) + "");
            }
        }

        // Get the coefficients for the first constraint, use 0 as padding if less than 4 variables
        for (int i=0; i<4;i++) {
            if (i >= nrOfVariables) {
                data.add("0");
            }
            else {
                data.add(constraints.get(0).getCoefficients().get(i) + "");
            }
        }

        // Get the upper bound
        data.add(constraints.get(0).getUb() + "");
        // Get the coefficients for the second constraint, use 0 as padding if less than 4 variables
        for (int i=0; i<4;i++) {
            if (i >= nrOfVariables) {
                data.add("0");
            }
            else {
                data.add(constraints.get(1).getCoefficients().get(i) + "");
            }
        }
        data.add(constraints.get(1).getLb() + "");

        String[] arr = new String[data.size()];
        int i = 0;
        for (String s : data) {
            arr[i] = s;
            i++;
        }
        return arr;
    }

    public void setBinaryOutputFeature(String bof) {
        while (bof.length() < 4) {
            bof = bof + "-1";
        }
        for (int i=0;i< variables.size();i++) {
            binaryOutputFeature[i] = bof.charAt(i) +"";
        }
        for (int i = variables.size(); i<4; i++) {
            binaryOutputFeature[i] = "-1";
        }

    }

    public String[] getBinaryOutputFeature() {
        return binaryOutputFeature;
    }

    @Override
    public String toString() {
        MPSolver solver = generateSolver();
        return solver.exportModelAsLpFormat();
    }


}
