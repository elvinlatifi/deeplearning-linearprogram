public class Constraint {
    private double lb;
    private double ub;
    String name;

    Constraint(double lb, double ub, String name) {
        this.lb = lb;
        this.ub = ub;
        this.name = name;
    }
}
