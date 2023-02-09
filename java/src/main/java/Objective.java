import java.util.ArrayList;

public class Objective {
    // private boolean minmax;
    private ArrayList<Double> coefficients;

    Objective(ArrayList<Double> coefficients) {
        this.coefficients = coefficients;
    }

    Objective(Objective o)
    {
        coefficients = new ArrayList<Double>(o.getCoefficients());
    }

    public ArrayList<Double> getCoefficients() {
        return coefficients;
    }

    public void flipSign(ArrayList<Integer> indices) {
        for (Integer i: indices) {
            coefficients.set(i, coefficients.get(i) *-1);
        }
    }
}

