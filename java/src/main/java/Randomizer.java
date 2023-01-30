import com.google.ortools.Loader;
import com.google.ortools.linearsolver.MPSolver;
import com.google.protobuf.JavaType;

import java.sql.Array;
import java.util.ArrayList;

public class Randomizer {

    public static void main(String[] args)
    {
        Loader.loadNativeLibraries();
        Test();
    }

    public static void Test() {

        double infinity = java.lang.Double.POSITIVE_INFINITY;

        ArrayList<Double> obj_data = new ArrayList<Double>();

        obj_data.add(3.0);
        obj_data.add(4.0);

        Objective obj = new Objective(obj_data);

        ArrayList<Double> const_coef = new ArrayList<Double>();

        const_coef.add(1.0);
        const_coef.add(2.0);

        Constraint c1 = new Constraint(-infinity, 14, "c1", const_coef);

        ArrayList<Variable> variables = new ArrayList<Variable>();

        variables.add(new Variable(0.0, infinity, "x"));
        variables.add(new Variable(0.0, infinity, "y"));
        var c_list = new ArrayList<Constraint>();
        c_list.add(c1);

        var program = new LinearProgram(obj, c_list, variables);

        program.solve();
    }
}
