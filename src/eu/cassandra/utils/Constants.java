/*
Copyright 2011-2013 The Cassandra Consortium (cassandra-fp7.eu)


Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package eu.cassandra.utils;

import java.util.ArrayList;
import java.util.Comparator;

import org.apache.log4j.Logger;

import eu.cassandra.appliance.Appliance;

public class Constants
{

  static Logger log = Logger.getLogger(Constants.class);

  /**
   * This comparator is used for sorting the points of interest based on the
   * minute of interest.
   */
  public static Comparator<PointOfInterest> comp =
    new Comparator<PointOfInterest>() {
      @Override
      public int compare (PointOfInterest poi1, PointOfInterest poi2)
      {
        return Integer.compare(poi1.getMinute(), poi2.getMinute());
      }
    };

  /**
   * This comparator is used for sorting the points of interest based on the
   * minutes of interest, which of the two starts and ends first.
   */
  public static Comparator<PointOfInterest[]> comp2 =
    new Comparator<PointOfInterest[]>() {
      @Override
      public int compare (PointOfInterest[] poi1, PointOfInterest[] poi2)
      {
        if (poi1[0].getMinute() != poi2[0].getMinute())
          return Integer.compare(poi1[0].getMinute(), poi2[0].getMinute());
        else
          return Integer.compare(poi1[1].getMinute(), poi2[1].getMinute());
      }
    };

  /**
   * This comparator is used for sorting the activities based on the
   * minutes of interest, which of the two starts and ends first.
   */
  public static Comparator<String[]> comp3 = new Comparator<String[]>() {
    @Override
    public int compare (String[] activity1, String[] activity2)
    {
      if (Integer.parseInt(activity1[2]) != Integer.parseInt(activity2[2]))
        return Integer.compare(Integer.parseInt(activity1[2]),
                               Integer.parseInt(activity2[2]));
      else
        return Integer.compare(Integer.parseInt(activity1[3]),
                               Integer.parseInt(activity2[3]));
    }
  };

  /**
   * This comparator is used for sorting the points of interest based on the
   * absolute value of the active power.
   */
  public static Comparator<PointOfInterest> comp4 =
    new Comparator<PointOfInterest>() {
      @Override
      public int compare (PointOfInterest poi1, PointOfInterest poi2)
      {
        return Double.compare(-Math.abs(poi1.getPDiff()),
                              -Math.abs(poi2.getPDiff()));
      }
    };

  /**
   * This comparator is used for sorting a list of may points of interest based
   * on the active power the points of interest based on the absolute power of
   * the first point of the list, in descending order.
   */
  public static Comparator<ArrayList<PointOfInterest>> comp5 =
    new Comparator<ArrayList<PointOfInterest>>() {
      @Override
      public int compare (ArrayList<PointOfInterest> poi1,
                          ArrayList<PointOfInterest> poi2)
      {
        return Double.compare(-Math.abs(poi1.get(0).getPDiff()),
                              -Math.abs(poi2.get(0).getPDiff()));
      }
    };

  /**
   * This comparator is used for sorting the list of appliances based on the
   * times of appearance, so that the refrigerators may go to the top and the
   * other appliances are below that.
   */
  public static Comparator<Appliance> comp6 = new Comparator<Appliance>() {
    @Override
    public int compare (Appliance appliance1, Appliance appliance2)
    {
      return Integer.compare(-appliance1.getMatchingPoints().size(),
                             -appliance2.getMatchingPoints().size());
    }
  };

  /**
   * This comparator is used for sorting the list of appliances based on the
   * mean active power, so that the refrigerators may go to the top and the
   * other appliances are below that.
   */
  public static Comparator<Appliance> comp7 = new Comparator<Appliance>() {
    @Override
    public int compare (Appliance appliance1, Appliance appliance2)
    {
      return Double.compare(-appliance1.getMeanActive(),
                            -appliance2.getMeanActive());
    }
  };

  // ================= GENERAL CONSTANTS =================

  /**
   * This variable is used as the folder where the result files will be stored.
   */
  public static final String resultFolder = "ResultFiles/";

  /**
   * This variable is used as the folder where the temporary files will be
   * stored.
   */
  public static final String tempFolder = "TempFiles/";

  /**
   * This variable is used as the folder where the temporary files will be
   * stored.
   */
  public static final String chartFolder = "Charts/";

  /**
   * This variable shows how many minutes are in a day.
   */
  public static final int MINUTES_PER_DAY = 1440;
  /**
   * This variable shows how many days are in a week.
   */
  public static final int DAYS_PER_WEEK = 7;

  /**
   * This constant is used as a small error fringe during the washing machine
   * identification procedure.
   */
  public static final double ERROR_FRINGE = 0.05;
  /**
   * This constant is used as a small error fringe during the refrigerator
   * identification procedure.
   */
  public static final double ERROR_FRIDGE = 0.4;

  /**
   * This constant is used as a small error fringe in pairing between rising and
   * reduction points that can be matched.
   */
  public static final double PAIR_ERROR_FRINGE = 0.2;

  /**
   * This constant is used as a small number in case ww may have division with
   * zero.
   */
  public static final double NEAR_ZERO = 1.0E-6;

  /**
   * This constant is used in order to give more weight in the appliances the
   * user has put in as a base for the disaggregation procedure.
   */
  public static final int USER_HEADSTART = 100;

  /**
   * This constant is used in order to regulate the appliance identification
   * procedure.The type can be generic (no activity mention), activity and list.
   */
  public static String APPLIANCE_TYPE = "Generic";

  /**
   * In case we have a list of appliances then the file containing the
   * specifications in included in the setting.
   */
  public static String OLD_APPLIANCE_FILE = "";

  /**
   * This constant shows if the appliances in the end will be loosely clustered
   * or not.
   */
  public static boolean CLUSTER_APPLIANCES = true;

  /**
   * This constant shows if the disaggreation method will look for washing
   * machine or not.
   */
  public static boolean WASHING_MACHINE_DETECTION = true;

  /**
   * This constant shows how many weeks of measurements are available from the
   * installation.
   */
  public static int WEEKS = 1;

  /**
   * This constant shows if the Points of Interest in the events are going to be
   * cleaned automatically or manually.
   */
  public static boolean AUTOMATIC_CLEANING_POIS = true;

  /**
   * In case of manual cleaning of Event's Points Of Interest This constant
   * shows if the Points of Interest in the events are going to be
   * cleaned automatically or manually.
   */
  public static double CLEANING_POIS_THRESHOLD = 30;

  /**
   * This constant shows if we will use median for the event threshold
   * estimation or the mean value.
   */
  public static boolean MEDIAN_THRESHOLD = true;

  /**
   * This constant shows if we are going to have day by day analysis or not.
   */
  public static boolean DAY_BY_DAY_ANALYSIS = false;

  /**
   * This constant shows if we are going to have simple or complex time
   * complexity
   */
  public static boolean SIMPLE_TIME_COMPLEXITY = true;

  /**
   * This constant shows if we are going to remove large events in the
   * measurements as outliers or not
   */
  public static boolean REMOVE_LARGE_EVENTS = true;

  /**
   * This constant shows the threshold of time duration for a large event.
   */
  public static int LARGE_EVENT_THRESHOLD = (int) Double.POSITIVE_INFINITY;

  // ================= PREPROCESS CONSTANTS =================

  public static boolean CLEANING_DATASET = true;

  public static boolean NORMALIZING_DATASET = true;

  public static final double NORMALIZING_THRESHOLD = 10E-50;

  public static final double NORMALIZING_POINT = 0.9;

  public static final int REMOVAL_THRESHOLD = 2 * MINUTES_PER_DAY / 3;

  // ================= ID CONSTANTS =================

  /**
   * Constant used for auto-increment event ids.
   */
  public static int EVENTS_ID = 1;

  /**
   * Constant used for auto-increment point of interest ids.
   */
  public static int POI_ID = 1;

  /**
   * Constant used for auto-increment appliance ids.
   */
  public static int APPLIANCE_ID = 1;

  /**
   * Constant used for auto-increment water heater ids.
   */
  public static int REFRIGERATOR_ID = 1;

  /**
   * Constant used for auto-increment water heater ids.
   */
  public static int WASHING_MACHINE_ID = 1;

  /**
   * Constant used for auto-increment water heater ids.
   */
  public static int WATERHEATER_ID = 1;

  /**
   * Constant used for auto-increment lighting ids.
   */
  public static int LIGHTING_ID = 1;

  /**
   * Constant used for auto-increment entertainment ids.
   */
  public static int ENTERTAINMENT_ID = 1;

  /**
   * Constant used for auto-increment cooking ids.
   */
  public static int COOKING_ID = 1;

  /**
   * Constant used for auto-increment microwave oven ids.
   */
  public static int MICROWAVE_OVEN_ID = 1;

  /**
   * Constant used for auto-increment extractor ids.
   */
  public static int EXTRACTOR_ID = 1;

  /**
   * Constant used for auto-increment vacuum cleaner ids.
   */
  public static int VACUUM_CLEANER_ID = 1;

  /**
   * Constant used for auto-increment vacuum cleaner ids.
   */
  public static int GENERIC_ID = 1;

  // ================= BACKGROUND CONSTANTS =================

  /**
   * This constant is used as the minimum background stand-by consumption of an
   * installation.
   */
  public static double MINIMUM_THRESHOLD = 0;

  /**
   * This constant is used as the minimum background stand-by consumption of an
   * installation.
   */
  public static double BACKGROUND_THRESHOLD = 30;

  // ================= POINTS OF INTEREST LIMIT CONSTANTS =================

  /**
   * This constant is used as a minute limit when checking for the end of an
   * event. If no more than X minutes have not passed with small values before
   * rising up again, the event is not considered finished.
   * */
  public static final int EVENT_TIME_LIMIT = 5;

  /**
   * This constant is used as a difference limit when checking for the
   * derivatives of two consecutive values of active power. If the difference is
   * less that the limit, then this is not considered as big enough for a new
   * point of interest.
   * */
  public static final int DERIVATIVE_LIMIT = 5;

  /**
   * This constant is used when searching for the overall rising near a point of
   * interest, taking the XX nearest minutes of the point of interest to find
   * the largest rising value.
   * */
  public static final int RISING_LIMIT = 3;

  /**
   * This constant is used when searching for the overall reduction near a point
   * of interest, taking the XX nearest minutes of the point of interest to find
   * the largest reduction value.
   * */
  public static final int REDUCTION_LIMIT = 3;

  // ================= THRESHOLD CONSTANTS =================

  /**
   * This constants signifies the number of points of interest that must be
   * present for the enabling of the threshold setting procedure.
   * */
  public static final int THRESHOLD_POINT_LIMIT = 10;

  /**
   * This constants is the limit of the difference between the rising and
   * reduction points active power that is acceptable while
   * removing points of interest in the threshold procedure. If the difference
   * is greater than the limit, it is not acceptable to remove the points of
   * interest.
   * */
  public static final int DIFFERENCE_LIMIT_ACTIVE = 10;

  /**
   * This constants is the limit of the difference between the rising and
   * reduction points reactive power that is acceptable while
   * removing points of interest in the threshold procedure. If the difference
   * is greater than the limit, it is not acceptable to remove the points of
   * interest.
   * */
  public static final int DIFFERENCE_LIMIT_REACTIVE = 20;

  /**
   * This constants is the limit of the difference between the old and the newly
   * created active power curves that is acceptable while removing points of
   * interest in the threshold procedure. If the difference is greater than the
   * limit, it is not acceptable to remove the points of interest.
   * */
  public static final int OLD_DIFFERENCE_LIMIT = 10;

  /**
   * This constants is setting the default threshold in the beginning of the
   * threshold tuning procedure.
   */
  public static final int DEFAULT_THRESHOLD = 10;

  public static final int MAX_ZEROS_THRESHOLD = 3;

  // ================= ISOLATED APPLIANCE EXTRACTOR CONSTANTS =================

  /**
   * This constant sets the number of times a point of interest must be smaller
   * in order to remove it when in the end of an event when creating isolated
   * events.
   */
  public static final int ISOLATED_TIMES_UP = 2;

  /**
   * This constant sets the number of maximum clusters that are created during
   * the refrigerator identification procedure, in order to find the most
   * frequent cluster.
   */
  public static final int MAX_CLUSTERS_NUMBER = 10;

  /**
   * This constant sets the limit to use KMeans clustering in place of the
   * hierarchical clustering used in big datasets in order to find the most
   * frequent cluster.
   */
  public static final int KMEANS_LIMIT_NUMBER = 10;

  // ================= REFRIGERATOR CONSTANTS =================

  /**
   * This constant signifies the duration fringe that the refrigerator
   * cycle can take.
   */
  public static boolean REF_LOOSE_COUPLING = true;

  /**
   * This constant signifies the duration fringe that the refrigerator
   * cycle can take.
   */
  public static final double REF_DURATION_FRINGE = 10;

  /**
   * This constant signifies the duration fringe that the refrigerator
   * cycle can take.
   */
  public static final double STRICT_REF_DURATION_FRINGE = 0.3;

  /**
   * This constant signifies the minimum duration that the refrigerator
   * cycle can take.
   */
  public static final double REF_MIN_DURATION = 10;

  /**
   * This constant signifies the maximum duration fringe that the refrigerator
   * cycle can take.
   */
  public static final double REF_MAX_DURATION = 30;

  /**
   * This constant signifies the threshold of similarity that is acceptable in
   * order to recognize a pair of points of interest as refrigerator.
   */
  public static final double REF_THRESHOLD = 10;

  /**
   * This constant signifies the threshold of similarity that is acceptable in
   * order to recognize a pair of points of interest as refrigerator.
   */
  public static final double REF_UPPER_THRESHOLD = 300;

  // ================= WASHING MACHINE CONSTANTS =================

  /**
   * This constant is setting a power threshold above which the searching for a
   * washing machine is initialized in an event.
   */
  public static final int WASHING_MACHINE_POWER_THRESHOLD = 1500;

  /**
   * This constant is used in order to merge two sets of consecutive minutes if
   * they have smaller than XX temporal distance.
   */
  public static final int WASHING_MACHINE_MERGING_MINUTE_LIMIT = 4;

  /**
   * This constant is one of the limits set for the successful discovery of a
   * washing machine operation and is about the size of a consecutive set of
   * minutes.
   */
  public static final int WASHING_MACHINE_NUMBER_OF_MINUTES_LIMIT = 30;

  /**
   * This constant is one of the limits set for the successful discovery of a
   * washing machine operation and is about the value of the difference variable
   * of a consecutive set of minutes.
   */
  public static final int WASHING_MACHINE_DIFFERENCE_LIMIT = 20;

  // ================= SWITCHING POINTS CONSTANTS =================

  /**
   * This constant signifies the threshold of similarity that is acceptable in
   * order to recognize a pair of points of interest as a switching event.
   */
  public static final int SWITCHING_THRESHOLD = 10; // 25;

  // ================= CLOSE MATCHING CONSTANTS =================

  /**
   * This constant signifies the threshold of similarity that is acceptable in
   * order to recognize a pair of points of interest as a close pair match.
   */
  public static final int CLOSENESS_THRESHOLD = 5;

  // ================= CLUSTER LARGE EVENTS CONSTANTS =================

  /**
   * This constant is used to measure the concentration of points of interest in
   * a small temporal duration and see if they seem to be connected as a bigger
   * appliance operation.
   */
  public static final int CONCENTRATION_THRESHOLD = 50;

  /**
   * This constant signifies the threshold of similarity that is acceptable in
   * order to recognize a set of points of interest as a cluster for a bigger
   * operation event.
   */
  public static final int CLUSTER_THRESHOLD = 15;

  // ================= SHAPES MATCHING CONSTANTS =================

  /**
   * This constant signifies the threshold of similarity that is acceptable in
   * order to recognize a triplet of points of interest as a chair.
   */
  public static final int CHAIR_DISTANCE_THRESHOLD = 10;

  /**
   * This constant signifies the threshold of similarity that is acceptable in
   * order to recognize a triplet of points of interest as an inversed chair.
   */
  public static final int INVERSED_CHAIR_DISTANCE_THRESHOLD = 10;

  /**
   * This constant signifies the threshold of similarity that is acceptable in
   * order to recognize a pair of points of interest as a triangle.
   */
  public static final int TRIANGLE_DISTANCE_THRESHOLD = 10;

  /**
   * This constant signifies the threshold of similarity that is acceptable in
   * order to recognize a pair of points of interest as a rectangle.
   */
  public static final int RECTANGLE_DISTANCE_THRESHOLD = 10;

  // ================= INTEGER PROGRAMMING CONSTANTS =================

  /**
   * This constant signifies the threshold of similarity that is acceptable in
   * order to recognize a set of points of interest as a matching pair of
   * operation, in case of simple combination analysis.
   */
  public static final int DISTANCE_THRESHOLD = 20;

  /**
   * This constant signifies the threshold of similarity that is acceptable in
   * order to recognize a set of points of interest as a matching pair of
   * operation, in case of complex combination analysis.
   */
  public static final int SECOND_DISTANCE_THRESHOLD = 30;

  public static final int PERFECT_MATCH_DISTANCE_THRESHOLD = 60;

  public static final int ACCEPTANCE_FULL_THRESHOLD = 5;

  public static int TEMPORAL_THRESHOLD = 180;
  /**
   * This constant symbolizes the maximum number of reduction (rising) points
   * that can be combined with a single rising (reduction) point.
   */
  public static int MAX_POINTS_LIMIT = 4;

  /**
   * This constant is setting the threshold for the solution in the integer
   * programming in case the points under consideration are under 10 in number.
   */
  public static final int SOLUTION_THRESHOLD_UNDER_10 = 20000000;

  /**
   * This constant is setting the threshold for the solution in the integer
   * programming in case the points under consideration are under 15 in number.
   */
  public static final int SOLUTION_THRESHOLD_UNDER_20 = 20000000;

  /**
   * This constant is setting the threshold for the solution in the integer
   * programming in case we are using the perfect matching of all points and not
   * the subset.
   */
  public static final int OTHER_SOLUTION_THRESHOLD = 50000;

  /**
   * This constant is used to set the number of points that are acceptable after
   * removal in order to make the dataset smaller.
   */
  public static int REMOVAL_MAX_POINTS = 15;

  /**
   * This constant is setting the threshold of the points of interest that
   * change from the simple clustering procedure to the complex one.
   */
  public static int MAX_POINTS_OF_INTEREST = 15;

  public static final int ADD_CLUSTER_THRESHOLD = 5;

  public static final double REMAINING_POINTS_POWER_PENALTY = 0.2;

  // ================= APPLIANCES THRESHOLDS =================

  /**
   * This constant sets the threshold that identifies the heater as the
   * appliance that is operating.
   * */
  public static final int WATERHEATER_POWER_THRESHOLD = 3000;

  /**
   * This constant sets the threshold that identifies the presence of a
   * very small electrical appliance in operation.
   * */
  public static final int TINY_POWER_THRESHOLD = 30;

  /**
   * This constant sets the threshold that identifies the presence of a small
   * electrical appliance in operation.
   * */
  public static final int SMALL_POWER_THRESHOLD = 250;

  /**
   * This constant sets the threshold that identifies the presence of a medium
   * electrical appliance in operation.
   * */
  public static final int MEDIUM_POWER_THRESHOLD = 300;

  /**
   * This constant sets the threshold for the reactive power that differenciates
   * some appliances from others.
   * */
  public static final int REACTIVE_THRESHOLD = 50;

  /**
   * This constant signifies the threshold of percentage similarity that is
   * acceptable in order to recognize a pair of points of interest as an
   * operation of an already identified appliance.
   */
  public static final int PERCENTAGE_CLOSENESS_THRESHOLD = 10;

  /**
   * This constant signifies the threshold of percentage similarity that is
   * acceptable in order to recognize a pair of points of interest as an
   * operation of an already identified appliance.
   */
  public static final int CLUSTERED_PERCENTAGE_CLOSENESS_THRESHOLD = 15;

  /**
   * This constant signifies the threshold of absolute value similarity that is
   * acceptable in order to recognize a pair of points of interest as an
   * operation of an already identified appliance.
   */
  public static final int ABSOLUTE_CLOSENESS_THRESHOLD = 30;

  /**
   * This constant signifies the threshold of duration that is acceptable for a
   * microwave oven.
   */
  public static final int MICROWAVE_DURATION = 5;

  public static final void clear ()
  {

    EVENTS_ID = 1;

    POI_ID = 1;

    APPLIANCE_ID = 1;

    WATERHEATER_ID = 1;

    LIGHTING_ID = 1;

    ENTERTAINMENT_ID = 1;

    COOKING_ID = 1;

    MICROWAVE_OVEN_ID = 1;

    EXTRACTOR_ID = 1;

    VACUUM_CLEANER_ID = 1;

    GENERIC_ID = 1;

    BACKGROUND_THRESHOLD = 30;

  }

  public static final void setThreshold (double[] power)
  {
    MINIMUM_THRESHOLD = Utils.estimateThreshold(power, MEDIAN_THRESHOLD);

    if (MINIMUM_THRESHOLD < BACKGROUND_THRESHOLD)
      BACKGROUND_THRESHOLD += MINIMUM_THRESHOLD;
    else
      BACKGROUND_THRESHOLD = 1.5 * MINIMUM_THRESHOLD;

    // BACKGROUND_THRESHOLD = Utils.estimateThreshold(power, true)+
    // BACKGROUND_THRESHOLD;

    log.info("===============EVENT DETECTION================");
    log.info("Event Threshold: " + BACKGROUND_THRESHOLD);
    log.info("");
  }

  public static final void setApplianceType (String type,
                                             String oldApplianceFile)
  {

    if (type.equalsIgnoreCase("Generic") || type.equalsIgnoreCase("List")
        || type.equalsIgnoreCase("Activity"))
      APPLIANCE_TYPE = type;
    else
      System.out.println("Error with type input. Set to Generic by default.");

    if (type.equalsIgnoreCase("List"))
      OLD_APPLIANCE_FILE = oldApplianceFile;
  }

  public static final void setClusterAppliances (boolean loose)
  {
    CLUSTER_APPLIANCES = loose;
  }

  public static final void setFridgeCoupling (boolean loose)
  {
    REF_LOOSE_COUPLING = loose;
  }

  public static final void setMedianThreshold (boolean median)
  {
    MEDIAN_THRESHOLD = median;
  }

  public static final void setPointsPerCluster (int clusterPoints)
  {
    MAX_POINTS_OF_INTEREST = clusterPoints;
    REMOVAL_MAX_POINTS = (int) (3 * clusterPoints / 2);
  }

  public static final void
    setCombinationPointsPerCluster (int combinationPoints)
  {
    MAX_POINTS_LIMIT = combinationPoints;

  }

  public static final void setWashingMachineDetection (boolean washing)
  {
    WASHING_MACHINE_DETECTION = washing;
  }

  public static final void setTimeThresholdComplexity (boolean timeComplexity)
  {
    SIMPLE_TIME_COMPLEXITY = timeComplexity;
  }

  public static final void setTimeThreshold (int timeComplexity)
  {
    TEMPORAL_THRESHOLD = timeComplexity;
  }

  public static final void setRemoveLargeEvents (boolean large)
  {
    REMOVE_LARGE_EVENTS = large;
  }

  public static final void setLargeEventThreshold (int threshold)
  {
    LARGE_EVENT_THRESHOLD = threshold;
  }

  public static final void setCleaningDataset (boolean clean)
  {
    CLEANING_DATASET = clean;
  }

  public static final void setNormalizingDataset (boolean normalize)
  {
    NORMALIZING_DATASET = normalize;
  }

  public static final void setWeeks (int weeks)
  {
    if (weeks == 0)
      WEEKS = 1;
    else
      WEEKS = weeks;
  }

  public static final void setCleaningConstants (boolean auto, double thres)
  {
    AUTOMATIC_CLEANING_POIS = auto;
    if (!auto)
      CLEANING_POIS_THRESHOLD = thres;
  }

  public double getMinimumThreshold ()
  {

    return MINIMUM_THRESHOLD;

  }
}
