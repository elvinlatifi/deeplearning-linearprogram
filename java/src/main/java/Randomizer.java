import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.ortools.Loader;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Random;
public class Randomizer {
    private static Random rand = new Random();
    private static final String output_path = "dataset\\";
    private static final String train_output_path = "dataset_train\\";
    double infinity = java.lang.Double.POSITIVE_INFINITY;
    public static void main(String[] args)
    {
        Loader.loadNativeLibraries();
        //Test();
        //generateData();
        //generateDataGenerationStatistics();
        generateDatasets(10000, output_path);
        generateDatasets(10000, train_output_path);
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

    public static void generateData() {

        LinearProgram lp = generateLinearProgram(rand.nextInt(2, 5)); // Generate Linear Program with 2,3 or 4 variables
        LinearProgram result;
        if (lp.solve()) {
            result = flipSigns(lp, true);
        }
        else {
            result = flipSigns(lp, false);
        }
        if (result == null) {
            // DO NOTHING
            System.out.println("USELESS");
        }
        else if (result.isConvertible()) {
            // SEND TO CONVERTIBLE DATASET
            System.out.println("CONVERTIBLE");
        }
        else {
            // SEND TO INCONVERTIBLE DATASET
            System.out.println("INCOVERTIBLE");
        }
    }

    public static void generateDataGenerationStatistics()
    {
        int useless = 0;
        int convertible = 0;
        int incovertible = 0;

        for (int i = 0; i < 100000; i++) {
            LinearProgram lp = generateLinearProgram(rand.nextInt(2, 5)); // Generate Linear Program with 2,3 or 4 variables
            LinearProgram result;
            if (lp.solve()) {
                result = flipSigns(lp, true);
            } else {
                result = flipSigns(lp, false);
            }
            if (result == null) {
                // DO NOTHING
                //System.out.println("USELESS");
                useless++;
            } else if (result.isConvertible()) {
                // SEND TO CONVERTIBLE DATASET
                //System.out.println("CONVERTIBLE");
                convertible++;
            } else {
                // SEND TO INCONVERTIBLE DATASET
                //System.out.println("INCOVERTIBLE");
                incovertible++;
            }

            System.out.println("Useless: " + useless + " Convertible: " + convertible + " Inconvertible: " + incovertible);
        }
    }

    public static void generateDatasets(int count, String directory_path) {
        int total = 0; //unused
        int convertible = 0;
        int incovertible = 0;

        var gb = new GsonBuilder();
        //this fixes the issue with infinities
        gb.serializeSpecialFloatingPointValues();

        try {
            FileUtils.deleteDirectory(new File(directory_path));
        } catch (IOException e) {
            System.err.println("Could not delete dataset directory!?");
            return;
        }

        Gson gson = gb.create();

        //creates the folder structure
        try {
            Files.createDirectories(Paths.get(directory_path + "\\conv"));
            Files.createDirectories(Paths.get(directory_path + "\\inconv"));
        }
        catch(IOException e)
        {
            System.err.println("Could not create folders?");
            return;
        }

        System.out.println("Generating " + count + " datasets to output folder: " + directory_path);

        while(convertible != count || incovertible != count)
        {
            String final_path = "";
            LinearProgram lp = generateLinearProgram(rand.nextInt(2, 5)); // Generate Linear Program with 2,3 or 4 variables
            LinearProgram result;

            if (lp.solve()) {
                result = flipSigns(lp, true);
            } else {
                result = flipSigns(lp, false);
            }
            if (result == null) {
                // DO NOTHING
                continue;
            } else if (result.isConvertible()) {
                // SEND TO CONVERTIBLE DATASET
                //if count is reached for this type, skip writing more
                if (convertible == count)
                {
                    continue;
                }

                final_path = directory_path + "conv\\" + "conv_" + total + ".txt";
                convertible++;

                var test = result.solve();
            } else {
                // SEND TO INCONVERTIBLE DATASET
                //if count is reached for this type, skip writing more
                if (incovertible == count)
                {
                    continue;
                }

                final_path = directory_path + "inconv\\" + "inconv_" + total + ".txt";
                incovertible++;
            }

            String output;

            output = gson.toJson(result);

            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(final_path));
                writer.write(output);

                writer.close();
            }
            catch (IOException e)
            {
                System.err.println("Could not write file: " + final_path);
            }
            total++;
        }

        validateDataSet(directory_path);

        System.out.println("Two datasets generated (train and reg)! Total count: " + total);
    }

    private static boolean validateDataSet(String dir_path) {
        String[] class_names = { "conv", "inconv" };
        Gson gson = new Gson();

        for(var class_name : class_names)
        {
            File dir = new File(dir_path + class_name);
            File[] dirList = dir.listFiles();
            if (dirList != null) {
                for (File file : dirList) {
                    try {
                        BufferedReader read = new BufferedReader(new FileReader(file.getAbsolutePath()));
                        LinearProgram lp = gson.fromJson(read.readLine(), LinearProgram.class);
                        if (lp.solve()) {
                            System.out.println("Invalid dataset! file: " + file.getName());
                            return false;
                        }
                    } catch (FileNotFoundException e) {
                        System.out.println(e.getMessage());
                    } catch (IOException e) {
                        System.out.println(e.getMessage());
                    }
                }
            }
        }

        System.out.println("Dataset valid!");
        return true;
    }

    private static LinearProgram generateLinearProgram(int nrOfVariables) {
        String variablesString = "xyzm";
        double infinity = java.lang.Double.POSITIVE_INFINITY;
        ArrayList<Double> obj_data = new ArrayList<Double>();
        ArrayList<Double> const_coef = new ArrayList<Double>();
        ArrayList<Double> const_coef2 = new ArrayList<Double>();
        ArrayList<Variable> variables = new ArrayList<Variable>();

        for (int i = 0; i < nrOfVariables; i++) {
            obj_data.add(rand.nextDouble(100));
            const_coef.add(rand.nextDouble(-50, 50));
            const_coef2.add(rand.nextDouble(-50, 50));
            variables.add(new Variable(0.0, infinity, variablesString.charAt(i) + ""));
        }

        Objective obj = new Objective(obj_data);

        Constraint c1 = new Constraint(-infinity, rand.nextDouble(-100, 100), "c1", const_coef);
        Constraint c2 = new Constraint(rand.nextDouble(-100, 100), infinity, "c2", const_coef2);

        ArrayList<Constraint> c_list = new ArrayList<>();
        c_list.add(c1);
        c_list.add(c2);
        return new LinearProgram(obj, c_list, variables);
    }

    private static LinearProgram flipSigns(LinearProgram lp, boolean originallyFeasible) {
        int variableNum = lp.getVariables().size();
        ArrayList<Integer> indices = new ArrayList<>();

        for (int i = 0; i<Math.pow(2, variableNum);i++) {
            LinearProgram copy = new LinearProgram(lp);
            String bin = Integer.toBinaryString(i);
            while (bin.length() < variableNum) {
                bin = "0" + bin;
            }
            for (int j = 0; j<variableNum; j++) {
                if (bin.charAt(j) == '1') {
                    indices.add(j);
                }
            }
            copy.flipSign(indices);
            boolean feasible = copy.solve();
            if (feasible && !originallyFeasible) {
                lp.setConvertible();
                return lp; // Originally not feasible and made feasible, CONVERTIBLE DATASET
            }
            else if (!feasible && originallyFeasible) {
                copy.setConvertible();
                return copy; // Originally feasible and made infeasible, CONVERTIBLE DATASET
            }

        }
        if (!originallyFeasible) {
            return lp; // Originally infeasible and stayed infeasible after each sign flip, INCONVERTIBLE DATASET
        }
        return null; // Originally feasible and still feasible after each sign flip, USELESS
    }
}
