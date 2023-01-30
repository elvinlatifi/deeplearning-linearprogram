public class Variable {
    private double lb;
    private double ub;
    String name;
    Variable(double lb, double ub, String name) {
        this.lb = lb;
        this.ub = ub;
        this.name = name;
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
}
