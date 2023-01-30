import java.util.ArrayList;

public class Constraint {
    private double lb;
    private double ub;
    String name;
    private ArrayList<Double> coefficients;

    Constraint(double lb, double ub, String name, ArrayList<Double> coefficients) {
        this.lb = lb;
        this.ub = ub;
        this.name = name;
        this.coefficients = coefficients;
    }

    public double getLb() {
        return lb;
    }

    public double getUb() {
        return ub;
    }

    public String getName() {
        return name;
    }

    public ArrayList<Double> getCoefficients() {
        return coefficients;
    }
}
