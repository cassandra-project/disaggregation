package eu.cassandra.utils;

import java.util.ArrayList;
import java.util.Collections;

import org.apache.log4j.Logger;

public class ComplexLinearSolution
{

  private static Logger log = Logger.getLogger(ComplexLinearSolution.class);

  private ArrayList<ArrayList<PointOfInterest>> clusters =
    new ArrayList<ArrayList<PointOfInterest>>();

  private ArrayList<LinearSolution> complexSolution =
    new ArrayList<LinearSolution>();

  private double overallNormalizedDistance = 0;

  private ArrayList<PointOfInterest> remainingPoints =
    new ArrayList<PointOfInterest>();

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
  public ComplexLinearSolution (ArrayList<PointOfInterest> temp, int bias)
    throws Exception
  {

    clusters = Utils.clusterPoints(temp, bias);

    log.info(clusters);

    ArrayList<PointOfInterest> remaining = null;
    boolean flag1, flag2, flag3;
    LinearSolution finalSolution = null;

    for (int i = 0; i < clusters.size(); i++) {
      log.info("");
      log.info("Cluster " + (i + 1));
      log.info("Cluster Size Before Cleaning: " + clusters.get(i).size());

      while (clusters.get(i).size() > Constants.REMOVAL_MAX_POINTS)
        clusterCleaning(i);

      log.info("Cluster Size After Cleaning: " + clusters.get(i).size());

      finalSolution = solveCluster(i);

      complexSolution.add(finalSolution);
      remaining = finalSolution.getRemainingPoints();

      flag1 = (i < clusters.size() - 1);
      if (remaining != null)
        flag2 =
          (remaining.size() + remainingPoints.size() > Constants.ADD_CLUSTER_THRESHOLD);
      else
        flag2 = false;
      flag3 =
        (complexSolution.get(i).getOverallNormalizedDistance() != Double.POSITIVE_INFINITY);

      if (!flag1) {
        log.info("Flag2:" + flag2);
        log.info("Flag3:" + flag3);
      }

      if (remaining == null)
        log.info("No Points Remaining.");
      else {
        log.info("Remaining Size:" + remaining.size());
        log.info("Remaining: " + remaining.toString());
        if (i < clusters.size() - 1) {
          clusters.get(i + 1).addAll(remaining);
          Collections.sort(clusters.get(i + 1), Constants.comp);
        }
        else if (flag2 && flag3) {
          remaining.addAll(remainingPoints);
          clusters.add(remaining);
          remainingPoints.clear();
          Collections.sort(clusters.get(i + 1), Constants.comp);
        }
        else
          remainingPoints.addAll(remaining);

        Collections.sort(remainingPoints, Constants.comp);
      }

    }

    estimateOverallDistance();

    log.info("");
    log.info("");
    log.info("");
    log.info("");

    status();

  }

  private LinearSolution solveCluster (int i)
  {

    LinearSolution finalSolution = null;

    LinearSolution partial =
      new LinearSolution(clusters.get(i), true, false, false);

    finalSolution = partial;

    partial.status();

    if (partial.getRemainingPoints() != null) {

      LinearSolution full =
        new LinearSolution(clusters.get(i), true, true, false);

      full.status();

      log.info("Full Distance: " + full.getOverallNormalizedDistance()
               + " Partial Distance: " + partial.getOverallNormalizedDistance());

      log.info("Difference: "
               + (full.getOverallNormalizedDistance() - partial
                       .getOverallNormalizedDistance()));

      if (full.getOverallNormalizedDistance()
          - partial.getOverallNormalizedDistance() < Constants.ACCEPTANCE_FULL_THRESHOLD)
        finalSolution = full;

    }

    return finalSolution;
  }

  private void clusterCleaning (int i)
  {
    ArrayList<PointOfInterest> remaining = null;

    log.info("Size Before for index " + i + ": " + clusters.get(i).size());
    remaining = Utils.removePoints(clusters.get(i));
    log.info("Size After for index " + i + ": " + clusters.get(i).size());
    Collections.sort(clusters.get(i), Constants.comp);

    if (i < clusters.size() - 1) {
      log.info("Size Before for index " + (i + 1) + ": "
               + clusters.get(i + 1).size());
      clusters.get(i + 1).addAll(remaining);
      log.info("Size After for index " + (i + 1) + ": "
               + clusters.get(i + 1).size());
      Collections.sort(clusters.get(i + 1), Constants.comp);
    }
    else {
      remainingPoints.addAll(remaining);
      log.info("Remaining Points Overall Size: " + remainingPoints.size());
      log.info("Remaining Points Overall: " + remainingPoints.toString());
    }

  }

  private void estimateOverallDistance ()
  {
    for (int i = 0; i < complexSolution.size(); i++)
      if (complexSolution.get(i).getOverallNormalizedDistance() != Double.POSITIVE_INFINITY)
        overallNormalizedDistance +=
          complexSolution.get(i).getOverallNormalizedDistance();

    if (getRemainingPoints() != null) {
      for (PointOfInterest rest: getRemainingPoints())
        overallNormalizedDistance +=
          Math.abs(rest.getPDiff()) * Constants.REMAINING_POINTS_POWER_PENALTY;
    }
  }

  public double getOverallNormalizedDistance ()
  {
    return overallNormalizedDistance;
  }

  public ArrayList<PointOfInterest> getRemainingPoints ()
  {
    return complexSolution.get(complexSolution.size() - 1).getRemainingPoints();
  }

  public void status ()
  {
    log.info("");
    log.info("Complex Solution: ");
    for (int i = 0; i < complexSolution.size(); i++) {

      log.info("Level " + (i + 1));
      complexSolution.get(i).status();

    }
    log.info("");
    log.info("Overall Normalized Distance: " + overallNormalizedDistance);
    log.info("Number of Remaining Points: " + remainingPoints.size());
    Collections.sort(remainingPoints, Constants.comp4);
    log.info("Remaining Points: " + remainingPoints.toString());
    log.info("");
  }

  public ArrayList<PointOfInterest[]> extractFinalPairs ()
  {

    ArrayList<PointOfInterest[]> finalPairs =
      new ArrayList<PointOfInterest[]>();

    for (int i = 0; i < clusters.size(); i++) {
      finalPairs.addAll(complexSolution.get(i).extractFinalPairs());
    }

    return finalPairs;

  }
}
