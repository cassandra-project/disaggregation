package disaggregation;
import java.util.ArrayList;

import com.google.ortools.constraintsolver.DecisionBuilder;
import com.google.ortools.constraintsolver.IntVar;
import com.google.ortools.constraintsolver.OptimizeVar;
import com.google.ortools.constraintsolver.Solver;

public class Utils
{

  public static double absoluteEuclideanDistance (double[] a1, double[] a2)
  {
    return Math.sqrt(Math.pow(a1[0] - a2[0], 2) + Math.pow(a1[1] - a2[1], 2));
  }

  public static double percentageEuclideanDistance (double[] a1, double[] a2)
  {
    return 100
           * Math.sqrt(Math.pow(a1[0] - a2[0], 2) + Math.pow(a1[1] - a2[1], 2))
           / norm(a1);
  }

  public static double norm (double[] poi)
  {
    return Math.sqrt(Math.pow(poi[0], 2) + Math.pow(poi[1], 2));
  }

  public static boolean checkLimit (double trueValue, double limit)
  {

    double upperLimit = (1 + Constants.ERROR_FRINGE) * limit;
    double lowerLimit = (1 - Constants.ERROR_FRINGE) * limit;

    return (trueValue < upperLimit && trueValue > lowerLimit);
  }

  public static Integer[] findRedPoints (int index,
                                         ArrayList<PointOfInterest> pois)
  {

    ArrayList<Integer> temp = new ArrayList<Integer>();

    for (int i = index + 1; i < pois.size(); i++)
      if (pois.get(i).getRising() == false)
        temp.add(i);

    Integer[] result = new Integer[temp.size()];

    for (int i = 0; i < temp.size(); i++)
      result[i] = temp.get(i);

    return result;

  }

  public static Integer[] findRisPoints (int index,
                                         ArrayList<PointOfInterest> pois)
  {

    ArrayList<Integer> temp = new ArrayList<Integer>();

    for (int i = 0; i < index; i++)
      if (pois.get(i).getRising())
        temp.add(i);

    Integer[] result = new Integer[temp.size()];

    for (int i = 0; i < temp.size(); i++)
      result[i] = temp.get(i);

    return result;

  }

  public static double[] meanValues (PointOfInterest[] pois)
  {

    double[] result =
      { (pois[0].getPDiff() - pois[1].getPDiff()) / 2,
       (pois[0].getQDiff() - pois[1].getQDiff()) / 2 };

    return result;

  }

  public static boolean independentPair (PointOfInterest[] poi1,
                                         PointOfInterest[] poi2)
  {
    return (poi2[0].getMinute() > poi1[1].getMinute());
  }

  public static int sumArray (int[] array)
  {
    int sum = 0;

    for (int i = 0; i < array.length; i++)
      sum += array[i];

    return sum;
  }

  public static ArrayList<Integer> solve (int[][] input, double[] cost)
  {

    Solver solver = new Solver("Integer Programming");

    int num_alternatives = cost.length;
    int num_objects = input[0].length;

    int[] costNew = new int[cost.length];

    for (int i = 0; i < costNew.length; i++)
      costNew[i] = (int) (10000 * cost[i]);

    //
    // variables
    //
    IntVar[] x = solver.makeIntVarArray(num_alternatives, 0, 1, "x");

    // number of assigned senators, to be minimize
    IntVar z = solver.makeScalProd(x, costNew).var();

    //
    // constraints
    //

    for (int j = 0; j < num_objects; j++) {
      IntVar[] b = new IntVar[num_alternatives];
      for (int i = 0; i < num_alternatives; i++) {
        b[i] = solver.makeProd(x[i], input[i][j]).var();
      }

      solver.addConstraint(solver.makeSumLessOrEqual(b, 1));

    }

    //
    // objective
    //
    OptimizeVar objective = solver.makeMaximize(z, 1);

    //
    // search
    //
    DecisionBuilder db =
      solver.makePhase(x, solver.INT_VAR_DEFAULT, solver.INT_VALUE_DEFAULT);
    solver.newSearch(db, objective);

    //
    // output
    //
    ArrayList<Integer> temp = new ArrayList<Integer>();
    while (solver.nextSolution()) {
      temp.clear();
      // System.out.println("z: " + z.value());
      // System.out.print("Selected alternatives: ");
      for (int i = 0; i < num_alternatives; i++) {
        if (x[i].value() == 1) {
          // System.out.print((1 + i) + " ");
          temp.add(i);
        }
      }
      // System.out.println("\n");

    }
    solver.endSearch();

    // Statistics
    System.out.println();
    System.out.println("Solutions: " + solver.solutions());
    System.out.println("Failures: " + solver.failures());
    System.out.println("Branches: " + solver.branches());
    System.out.println("Wall time: " + solver.wallTime() + "ms");

    return temp;

  }

  public static ArrayList<PointOfInterest[]>
    createFinalPairs (ArrayList<PointOfInterest> pois, int[] array)
  {
    ArrayList<PointOfInterest[]> result = new ArrayList<PointOfInterest[]>();
    ArrayList<PointOfInterest> rising = new ArrayList<PointOfInterest>();
    ArrayList<PointOfInterest> reduction = new ArrayList<PointOfInterest>();

    for (int i = 0; i < array.length; i++) {

      if (array[i] == 1) {
        if (pois.get(i).getRising())
          rising.add(pois.get(i));
        else
          reduction.add(pois.get(i));
      }

    }

    if (rising.size() == 1 && reduction.size() == 1) {

      PointOfInterest[] temp = { rising.get(0), reduction.get(0) };
      result.add(temp);
    }
    else if (rising.size() == 1) {

      for (PointOfInterest red: reduction) {

        PointOfInterest start =
          new PointOfInterest(rising.get(0).getMinute(), true, -red.getPDiff(),
                              -red.getQDiff());

        PointOfInterest[] temp = { start, red };
        result.add(temp);
      }

    }
    else {
      for (PointOfInterest rise: rising) {

        PointOfInterest end =
          new PointOfInterest(reduction.get(0).getMinute(), false,
                              -rise.getPDiff(), -rise.getQDiff());

        PointOfInterest[] temp = { rise, end };
        result.add(temp);
      }
    }

    return result;
  }
}
