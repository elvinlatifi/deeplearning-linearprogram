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

    Constraint(Constraint c) {
        this.lb = c.getLb();
        this.ub = c.getUb();
        this.name = c.getName();
        this.coefficients = new ArrayList<Double>(c.getCoefficients());
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
