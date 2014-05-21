package eu.cassandra.utils;

import java.util.ArrayList;

import org.apache.log4j.Logger;

public class SimpleLinearSolution
{

  private static Logger log = Logger.getLogger(SimpleLinearSolution.class);

  private ArrayList<PointOfInterest> poiInput = null;

  private ArrayList<LinearSolution> partialSolution =
    new ArrayList<LinearSolution>();

  private LinearSolution fullSolution = null;

  boolean choosePartial = true;

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
  public SimpleLinearSolution (ArrayList<PointOfInterest> temp, boolean isolated)
    throws Exception
  {
    poiInput = temp;

    solve(isolated);

    chooseSolution(isolated);

    if (!isolated)
      status();

  }

  private void solve (boolean isolated)
  {

    ArrayList<PointOfInterest> temp = new ArrayList<PointOfInterest>(poiInput);

    LinearSolution partial = new LinearSolution(temp, false, false, isolated);
    partialSolution.add(partial);

    if (!isolated) {
      if (partial.getRemainingPoints() != null)
        fullSolution =
          new LinearSolution(new ArrayList<PointOfInterest>(temp), false, true,
                             isolated);
      else
        log.info("No Points Remaining.");
    }

    if (partial.getRemainingPoints() != null
        && partial.getRemainingPoints().size() > Constants.ADD_CLUSTER_THRESHOLD) {
      temp =
        new ArrayList<PointOfInterest>(partialSolution.get(0)
                .getRemainingPoints());
      log.info("Excessive Points Size: " + temp.size());
      log.info("Excessive Points: " + temp.toString());
      partial = new LinearSolution(temp, true, true, isolated);
      partialSolution.add(partial);
      partialSolution.get(0).getRemainingPoints().clear();
    }

  }

  private void chooseSolution (boolean isolated)
  {
    double partialDistance = 0;
    if (!isolated) {
      log.info("=========PARTIAL SOLUTION=============");
      for (int i = 0; i < partialSolution.size(); i++) {
        log.info("Level " + i);
        partialSolution.get(i).estimateOverallDistance(false);
        partialSolution.get(i).status();
        partialDistance +=
          partialSolution.get(i).getOverallNormalizedDistance();
      }

      if (fullSolution != null) {
        log.info("=========FULL SOLUTION=============");
        fullSolution.status();

        log.info("Full Distance: "
                 + fullSolution.getOverallNormalizedDistance()
                 + " Partial Distance: " + partialDistance);
      }

    }
    else {
      partialDistance = partialSolution.get(0).getOverallNormalizedDistance();
    }

    if (fullSolution != null && fullSolution.getSolution().size() != 0
        && fullSolution.getOverallNormalizedDistance() <= partialDistance) {
      choosePartial = false;
      overallNormalizedDistance = fullSolution.getOverallNormalizedDistance();
    }
    else {
      choosePartial = true;
      overallNormalizedDistance = partialDistance;
      remainingPoints =
        partialSolution.get(partialSolution.size() - 1).getRemainingPoints();
    }
  }

  public void status ()
  {
    log.info("");
    log.info("Simple Solution: ");

    if (choosePartial) {
      log.info("=========PARTIAL SOLUTION=============");
      for (int i = 0; i < partialSolution.size(); i++) {
        log.info("Level " + i);
        partialSolution.get(i).status();
      }
    }
    else
      fullSolution.status();
    log.info("");
    if (choosePartial) {
      log.info("Overall Normalized Distance: " + overallNormalizedDistance);
      if (remainingPoints != null) {
        log.info("Number of Remaining Points: " + remainingPoints.size());
        log.info("Remaining Points: " + remainingPoints.toString());
      }
      else
        log.info("No remaining Points!");
      log.info("");
    }

  }

  public ArrayList<PointOfInterest[]> extractFinalPairs ()
  {

    ArrayList<PointOfInterest[]> finalPairs =
      new ArrayList<PointOfInterest[]>();

    if (choosePartial)
      for (int i = 0; i < partialSolution.size(); i++)
        finalPairs.addAll(partialSolution.get(i).extractFinalPairs());
    else
      finalPairs = fullSolution.extractFinalPairs();

    return finalPairs;

  }
}
