public class Variable {
    private double lb;
    private double ub;
    String name;
    Variable(double lb, double ub, String name) {
        this.lb = lb;
        this.ub = ub;
        this.name = name;
    }

    Variable(Variable v)
    {
        this.lb = v.getLb();
        this.ub = v.getUb();
        this.name = v.getName();
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
