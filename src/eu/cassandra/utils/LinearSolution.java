package eu.cassandra.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.paukov.combinatorics.Factory;
import org.paukov.combinatorics.Generator;
import org.paukov.combinatorics.ICombinatoricsVector;

import com.google.ortools.constraintsolver.DecisionBuilder;
import com.google.ortools.constraintsolver.IntVar;
import com.google.ortools.constraintsolver.OptimizeVar;
import com.google.ortools.constraintsolver.Solver;

public class LinearSolution
{

  private static Logger log = Logger.getLogger(LinearSolution.class);

  private ArrayList<PointOfInterest> poiInput = null;

  private Map<int[], Double> input = new HashMap<int[], Double>();

  private ArrayList<Integer> solution = new ArrayList<Integer>();

  private double[] cost;

  private int[][] tempArray;

  private double overallNormalizedDistance = 0;

  ArrayList<PointOfInterest> remainingPoints = new ArrayList<PointOfInterest>();

  /**
   * This function is used in case of a small number of points of interest in
   * the event. This procedure uses integer programming in order to find the
   * best candidates of the matching points.
   * 
   * @param temp
   *          The list of points of interest.
   * @param complex
   *          The flag that show that this is a complex procedure due to the
   *          large number of points of interest involved.
   * @return The remaining points of interest after finishing the procedure.
   * @throws Exception
   */
  public LinearSolution (ArrayList<PointOfInterest> temp, boolean complex,
                         boolean full, boolean isolated)
  {

    poiInput = temp;

    if (full) {
      if (!isolated)
        log.info("=========FULL SOLUTION=============");
      input = findCombinations(temp, complex, full);
    }
    else {
      if (!isolated)
        log.info("=========PARTIAL SOLUTION=============");
      input = findCombinations(temp, complex);
    }
    if (!isolated)
      log.info("Input Size: " + input.size());

    // Creating the input for the integer programming solver
    cost = new double[input.size()];
    tempArray = new int[input.size()][temp.size()];
    int counter = 0;
    for (int[] in: input.keySet()) {
      // log.info("Array: " + Arrays.toString(index) + " Distance: " + 1
      // / input.get(index) + " Similarity: " + input.get(index));
      tempArray[counter] = in;
      cost[counter++] = input.get(in);

    }

    for (int i = 0; i < input.size(); i++)
      if (!isolated)
        log.debug("Array: " + Arrays.toString(tempArray[i]) + " Cost: "
                  + cost[i]);

    if (input.size() == 0) {
      overallNormalizedDistance = Double.POSITIVE_INFINITY;
      remainingPoints = temp;
      if (!isolated) {
        log.info("No Available input");
        // log.info("Distance: " + overallNormalizedDistance);
        // log.info("Remaining Points: " + remainingPoints.toString());
      }
    }
    // In case of more solutions the integer solver is called.
    else {

      // ArrayList<ArrayList<Integer>> solutions = Utils.solve(tempArray,
      // cost);
      if (!isolated)
        log.info("INTEGER PROGRAMMING");

      // Solving the problem and presenting the solution
      if (full)
        solution = solve3(tempArray, cost);
      else
        solution = solve2(tempArray, cost, isolated);

      if (!isolated)
        log.info("Solution:" + solution.toString());

    }

    // input.clear();

    if (solution.size() > 0)
      remainingPoints = Utils.extractRemainingPoints(temp, solution, tempArray);

    estimateOverallDistance(complex);
  }

  public void estimateOverallDistance (boolean complex)
  {

    overallNormalizedDistance = 0;

    if (solution.size() == 0) {
      overallNormalizedDistance = Double.POSITIVE_INFINITY;
    }
    else {
      for (Integer index: solution) {

        overallNormalizedDistance +=
          ((1 / input.get(tempArray[index])) / Utils
                  .countPoints(tempArray[index]));
        // if (!isolated)
        // log.info("Normalized:" + overallNormalizedDistance);
      }
    }

    if (remainingPoints != null && !complex)
      for (PointOfInterest rest: getRemainingPoints())
        overallNormalizedDistance +=
          Math.abs(rest.getPDiff()) * Constants.REMAINING_POINTS_POWER_PENALTY;
  }

  public ArrayList<Integer> getSolution ()
  {
    return solution;
  }

  public int[][] getTempArray ()
  {
    return tempArray;
  }

  public double getOverallNormalizedDistance ()
  {
    return overallNormalizedDistance;
  }

  public ArrayList<PointOfInterest> getRemainingPoints ()
  {
    return remainingPoints;
  }

  /**
   * This function is responsible for creating the possible combinations of the
   * rising and reduction points of interest in the event in order to create the
   * final pairs that can be matched.
   * 
   * @param temp
   *          The list of points of interest in the procedure.
   * @param complex
   *          The flag that show that this is a complex procedure due to the
   *          large number of points of interest involved.
   * @return A hashmap of the matched points with the distance that they have.
   */
  public static Map<int[], Double>
    findCombinations (ArrayList<PointOfInterest> temp, boolean complex,
                      Boolean... full)
  {

    // Initializing the auxiliary variables
    Map<int[], Double> input = new HashMap<int[], Double>();
    Integer[] points = null;
    int[] pointsArray = null;
    List<Integer> subset = new ArrayList<Integer>();
    double distance = 0;

    int distanceThreshold = 0;

    if (!complex && (full.length == 1))
      distanceThreshold = Constants.PERFECT_MATCH_DISTANCE_THRESHOLD;
    else if (complex && (full.length == 1))
      distanceThreshold = Constants.SECOND_DISTANCE_THRESHOLD;
    else
      distanceThreshold = Constants.DISTANCE_THRESHOLD;

    // For each point
    for (int i = 0; i < temp.size(); i++) {
      // If rising point then we find the reduction points after that point,
      // create all the possible subsets from them and estimate the distance
      // of the active and reactive power measurements.If the distance is
      // under a certain threshold, the combination is accepted.

      double previousMaxDistance = Double.NEGATIVE_INFINITY, currentMaxDistance =
        Double.NEGATIVE_INFINITY;

      if (temp.get(i).getRising()) {

        // Returns the reduction points that can be associated with this rising
        // point
        points = Utils.findRedPoints(i, temp);

        // An initial vector of all the reduction points
        ICombinatoricsVector<Integer> initialSet = Factory.createVector(points);
        log.debug("Initial Set for point " + temp.get(i).toString() + ": "
                  + initialSet.toString());
        log.debug("Reduction Points for rising point " + i + ": "
                  + initialSet.toString());

        // Set the max combination of reduction point for each rising point
        int upperThres =
          Math.min(Constants.MAX_POINTS_LIMIT, initialSet.getSize());
        // log.debug("Upper Threshold:" + upperThres);

        // For a number of reduction points for the rising point
        for (int pairing = 1; pairing <= upperThres; pairing++) {

          Generator<Integer> gen =
            Factory.createSimpleCombinationGenerator(initialSet, pairing);

          for (ICombinatoricsVector<Integer> subSet: gen) {
            // log.debug(subSet.toString());
            if (subSet.getSize() > 0) {
              double sumP = 0, sumQ = 0;

              subset = subSet.getVector();

              for (Integer index: subset) {
                sumP += temp.get(index).getPDiff();
                sumQ += temp.get(index).getQDiff();
              }

              double[] tempValues = { -sumP, -sumQ };

              distance =
                1 / (temp.get(i).percentageEuclideanDistance(tempValues) + Constants.NEAR_ZERO);

              // If accepted then an array is created with 1 in the index of the
              // points included, 0 otherwise
              if ((1 / distance) < distanceThreshold) {

                log.debug("Subset: " + subSet.toString() + " Distance: " + 1
                          / distance + " Z: " + distance);

                pointsArray = new int[temp.size()];
                pointsArray[i] = 1;
                for (Integer index: subset)
                  pointsArray[index] = 1;

                if (distance > currentMaxDistance) {
                  // log.debug("Distance: " + distance
                  // + " Current Max Distance: "
                  // + currentMaxDistance);
                  currentMaxDistance = distance;
                }

                // Check if the array is already included in the alternatives.If
                // not, it is added to the alternatives.
                boolean flag = true;
                for (int[] content: input.keySet()) {
                  if (Arrays.equals(content, pointsArray)) {
                    flag = false;
                    break;
                  }
                }

                if (flag) {
                  input.put(pointsArray, distance);
                  // log.debug("Input:" + input);
                }
              }
            }
          }
          // log.debug("Input size for pairing of " + pairing + ":"
          // + input.size());
          // log.debug("Current Max Distance: " + currentMaxDistance
          // + " Previous Max Distance:" + previousMaxDistance);

          // Checking if the max distance is reduced and continue for larger
          // combination else stop the procedure
          if (previousMaxDistance < currentMaxDistance
              || currentMaxDistance == Double.NEGATIVE_INFINITY)
            previousMaxDistance = currentMaxDistance;
          else {
            // log.debug("Breaking for pairing = " + pairing);
            break;
          }

        }
      }
      // If reduction point then we find the rising points before that point,
      // create all the possible subsets from them and estimate the distance
      // of the active and reactive power measurements.If the distance is
      // under a certain threshold, the combination is accepted.
      else {
        points = Utils.findRisPoints(i, temp);

        ICombinatoricsVector<Integer> initialSet = Factory.createVector(points);

        log.debug("Initial Set for point " + temp.get(i).toString() + ": "
                  + initialSet.toString());
        log.debug("Rising Points for reduction point " + i + ": "
                  + initialSet.toString());

        int upperThres =
          Math.min(Constants.MAX_POINTS_LIMIT, initialSet.getSize());
        // log.debug("Upper Threshold:" + upperThres);

        for (int pairing = 1; pairing <= upperThres; pairing++) {

          Generator<Integer> gen =
            Factory.createSimpleCombinationGenerator(initialSet, pairing);

          for (ICombinatoricsVector<Integer> subSet: gen) {

            if (subSet.getSize() > 0) {
              double sumP = 0, sumQ = 0;

              subset = subSet.getVector();

              for (Integer index: subset) {
                sumP += temp.get(index).getPDiff();
                sumQ += temp.get(index).getQDiff();
              }

              double[] sum = { sumP, sumQ };
              double[] tempValues =
                { -temp.get(i).getPDiff(), -temp.get(i).getQDiff() };

              // TODO Add temporal distance

              distance =
                1 / (Utils.percentageEuclideanDistance(sum, tempValues) + Constants.NEAR_ZERO);

              // If accepted then an array is created with 1 in the index of the
              // points included, 0 otherwise
              if ((1 / distance) < distanceThreshold) {

                log.debug("Subset: " + subSet.toString() + " Distance: " + 1
                          / distance + " Z: " + distance);

                pointsArray = new int[temp.size()];
                pointsArray[i] = 1;
                for (Integer index: subset)
                  pointsArray[index] = 1;

                if (distance > currentMaxDistance) {
                  // log.debug("Distance: " + distance
                  // + " Current Max Distance: "
                  // + currentMaxDistance);
                  currentMaxDistance = distance;
                }

                // Check if the array is already included in the alternatives.If
                // not, it is added to the alternatives.
                boolean flag = true;
                for (int[] content: input.keySet()) {
                  if (Arrays.equals(content, pointsArray)) {
                    flag = false;
                    break;
                  }
                }

                if (flag) {
                  input.put(pointsArray, distance);
                  // log.debug("Input:" + input);
                }
              }
            }
          }

          // log.debug("Input size for pairing of " + pairing + ":"
          // + input.size());
          // log.debug("Current Max Distance: " + currentMaxDistance
          // + " Previous Max Distance:" + previousMaxDistance);

          if (previousMaxDistance < currentMaxDistance
              || currentMaxDistance == Double.NEGATIVE_INFINITY)
            previousMaxDistance = currentMaxDistance;
          else {
            // log.debug("Breaking for pairing = " + pairing);
            break;
          }
        }
      }
    }
    return input;
  }

  /**
   * This is an integer programming solver.
   * 
   * @param input
   *          The input array of alternatives.
   * @param cost
   *          The cost array of the alternatives.
   * @return a list of all the solutions.
   */
  public static ArrayList<ArrayList<Integer>> solve (int[][] input,
                                                     double[] cost)
  {
    ArrayList<ArrayList<Integer>> solutions =
      new ArrayList<ArrayList<Integer>>();
    Solver solver = new Solver("Integer Programming");

    int num_alternatives = cost.length;
    int num_objects = input[0].length;

    int[] costNew = new int[cost.length];
    int lambda = 1000;
    for (int i = 0; i < costNew.length; i++) {
      costNew[i] = (int) (100 * cost[i]);
      costNew[i] *= lambda;
    }
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
      solver.makePhase(x, Solver.INT_VAR_DEFAULT, Solver.INT_VALUE_DEFAULT);
    solver.newSearch(db, objective);

    //
    // output
    //

    // ArrayList<Integer> temp = ArrayList<Integer>()
    ArrayList<Integer> temp = null;
    while (solver.nextSolution()) {
      temp = new ArrayList<Integer>();
      log.debug("z: " + z.value());
      log.debug("Selected alternatives: ");
      for (int i = 0; i < num_alternatives; i++) {
        if (x[i].value() == 1) {
          log.debug((1 + i) + " ");
          temp.add(i);
        }
      }
      solutions.add(temp);

    }
    solver.endSearch();

    // Statistics
    log.info("");
    log.info("Solutions: " + solver.solutions());
    log.info("Failures: " + solver.failures());
    log.info("Branches: " + solver.branches());
    log.info("Wall time: " + solver.wallTime() + "ms");

    return solutions;

  }

  /**
   * This is an integer programming solver.
   * 
   * @param input
   *          The input array of alternatives.
   * @param cost
   *          The cost array of the alternatives.
   * @return a list of the indexes of the solution alternatives.
   */
  public static ArrayList<Integer> solve2 (int[][] input, double[] cost,
                                           boolean isolated)
  {

    Solver solver = new Solver("Integer Programming");

    int num_alternatives = cost.length;
    int num_objects = input[0].length;

    int solutionThreshold = 0;

    if (input[0].length < 10)
      solutionThreshold = Constants.SOLUTION_THRESHOLD_UNDER_10;
    else
      solutionThreshold = Constants.SOLUTION_THRESHOLD_UNDER_20;

    if (!isolated)
      log.info("Objects: " + num_objects + " Threshold: " + solutionThreshold);

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
      solver.makePhase(x, Solver.INT_VAR_DEFAULT, Solver.INT_VALUE_DEFAULT);
    solver.newSearch(db, objective);

    //
    // output
    //
    ArrayList<Integer> temp = new ArrayList<Integer>();
    while (solver.nextSolution()) {
      temp.clear();
      if (!isolated)
        log.debug("z: " + z.value());
      // log.debug("Selected alternatives: ");
      for (int i = 0; i < num_alternatives; i++) {
        if (x[i].value() == 1) {
          // log.debug((1 + i) + " ");
          temp.add(i);
        }
      }
      // if (z.value() > solutionThreshold)
      // break;
      // System.out.println("\n");

    }
    solver.endSearch();

    // Statistics
    if (!isolated) {
      log.info("");
      log.info("Solutions: " + solver.solutions());
      log.info("Failures: " + solver.failures());
      log.info("Branches: " + solver.branches());
      log.info("Wall time: " + solver.wallTime() + "ms");
    }

    return temp;

  }

  /**
   * Testing another type of solution.
   * 
   * @param input
   *          The input array of alternatives.
   * @param cost
   *          The cost array of the alternatives.
   * @return a list of the indexes of the solution alternatives.
   */
  public static ArrayList<Integer> solve3 (int[][] input, double[] cost)
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

      solver.addConstraint(solver.makeSumEquality(b, 1));

    }

    //
    // objective
    //
    OptimizeVar objective = solver.makeMaximize(z, 1);

    //
    // search
    //
    DecisionBuilder db =
      solver.makePhase(x, Solver.INT_VAR_DEFAULT, Solver.INT_VALUE_DEFAULT);
    solver.newSearch(db, objective);

    //
    // output
    //
    ArrayList<Integer> temp = new ArrayList<Integer>();
    while (solver.nextSolution()) {
      temp.clear();
      log.debug("z: " + z.value());
      // log.debug("Selected alternatives: ");
      for (int i = 0; i < num_alternatives; i++) {
        if (x[i].value() == 1) {
          // log.debug((1 + i) + " ");
          temp.add(i);
        }
      }
      if (z.value() > Constants.OTHER_SOLUTION_THRESHOLD)
        break;
      // System.out.println("\n");

    }
    solver.endSearch();

    // Statistics
    log.info("");
    log.info("Solutions: " + solver.solutions());
    log.info("Failures: " + solver.failures());
    log.info("Branches: " + solver.branches());
    log.info("Wall time: " + solver.wallTime() + "ms");

    return temp;

  }

  public ArrayList<PointOfInterest[]> extractFinalPairs ()
  {
    // For each part of the solution, the corresponding pairs are created
    // and added to the final pairs.
    ArrayList<PointOfInterest[]> finalPairs =
      new ArrayList<PointOfInterest[]>();

    for (Integer index: solution) {
      ArrayList<PointOfInterest[]> newFinalPairs =
        Utils.createFinalPairs(poiInput, tempArray[index]);
      finalPairs.addAll(newFinalPairs);
    }

    return finalPairs;

  }

  public void status ()
  {
    log.info("");
    log.info("Solution: ");
    if (solution.size() == 0) {
      log.info("No Available Solution");
      remainingPoints = poiInput;
    }
    else {
      for (Integer index: solution)
        log.info(Arrays.toString(tempArray[index]) + " Similarity: "
                 + input.get(tempArray[index]) + " Distance: "
                 + (1 / input.get(tempArray[index])));
    }
    log.info("Overall Distance: " + getOverallNormalizedDistance());
    if (remainingPoints != null) {
      log.info("Remaining Size: " + remainingPoints.size());
      log.info("Remaining Points: " + remainingPoints.toString());
    }
    else
      log.info("No Remaining Points");
    log.info("");
  }
}
