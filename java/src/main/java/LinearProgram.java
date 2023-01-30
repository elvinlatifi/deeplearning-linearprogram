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
}
