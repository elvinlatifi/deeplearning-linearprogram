import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;

import java.util.ArrayList;

public class LinearProgram {
    private Objective objective;

    private ArrayList<Constraint> constraints;
    private ArrayList<Variable> variables;

    LinearProgram(Objective objective, ArrayList<Constraint> constraints, ArrayList<Variable> variables) {
        this.objective = objective;
        this.constraints = constraints;
        this.variables = variables;
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

    protected boolean solve() {
        MPSolver solver = MPSolver.createSolver("GLOP");

        ArrayList<MPVariable> mpVariables = new ArrayList<>();

        for (Variable var : variables) {
            mpVariables.add(solver.makeNumVar(var.getLb(), var.getUb(), var.getName()));
        }

        ArrayList<MPConstraint> mpConstraints = new ArrayList<>();

        for (Constraint constr : constraints) {
            mpConstraints.add(solver.makeConstraint(constr.getLb(), constr.getUb(), constr.getName()));
        }

        MPObjective objective = solver.objective();

        ArrayList<Double> obj_coef = this.objective.getCoefficients();

        for (int i = 0; i < mpVariables.size(); i++) {
            objective.setCoefficient(mpVariables.get(i), obj_coef.get(i));
        }

        objective.setMaximization();

        final MPSolver.ResultStatus resultStatus = solver.solve();

        System.out.println("ResultStatus: " + resultStatus.toString());

        if (resultStatus == MPSolver.ResultStatus.FEASIBLE || resultStatus == MPSolver.ResultStatus.OPTIMAL) {
            System.out.println("Solution:");
            System.out.println("Objective value = " + objective.value());
            System.out.println("x = " + mpVariables.get(0).solutionValue());
            System.out.println("y = " + mpVariables.get(1).solutionValue());
        } else {
            System.err.println("Did not find a solution");
        }

        System.out.println("\nAdvanced usage:");
        System.out.println("Problem solved in " + solver.wallTime() + " milliseconds");
        System.out.println("Problem solved in " + solver.iterations() + " iterations");

        return true;
    }
}
