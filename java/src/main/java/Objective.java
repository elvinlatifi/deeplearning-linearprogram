import java.util.ArrayList;

public class Objective {
    // private boolean minmax;
    private ArrayList<Double> coefficients;

    Objective(ArrayList<Double> coefficients) {
        this.coefficients = coefficients;
    }

    public ArrayList<Double> getCoefficients() {
        return coefficients;
    }
}
