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

package eu.cassandra.appliance;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import eu.cassandra.event.Event;
import eu.cassandra.utils.ConsecutiveValues;
import eu.cassandra.utils.Constants;
import eu.cassandra.utils.PointOfInterest;
import eu.cassandra.utils.Utils;

/**
 * This class is responsible for the appliance identification and detection
 * procedures on the available event list. There are some complex functions used
 * exclusively for the refrigerator and the washing machine serach and
 * discovery, but there are also some more generic functions used for matching
 * known appliances to new patterns.
 * 
 * @author Antonios Chrysopoulos
 * @version 0.7, Date: 29.07.2013
 */
public class ApplianceIdentifier
{

  /**
   * This variable is a list of the appliances detected from the events.
   */
  ArrayList<Appliance> applianceList = new ArrayList<Appliance>();

  /**
   * This variable is a list of the activities as refined from the analysis of
   * the pairs of points of interest in the events.
   */
  ArrayList<String[]> activityList = new ArrayList<String[]>();

  /**
   * The simple constructor of the appliance identifier.
   */
  public ApplianceIdentifier ()
  {
    applianceList.clear();
    activityList.clear();
  }

  /**
   * This function is used as a getter for the list of the detected appliances.
   * 
   * @return a list with the detected appliances.
   */
  public ArrayList<Appliance> getApplianceList ()
  {
    return applianceList;
  }

  /**
   * This function is used in order to identify points of interest that may come
   * from the end-use of the refrigerator appliance.
   * 
   * @param events
   *          The list of events produced from the dataset.
   * @param iso
   *          The isolated appliance extractor, who contains information about
   *          the temporary refrigerator appliance.
   */
  public void refrigeratorIdentification (ArrayList<Event> events,
                                          IsolatedApplianceExtractor iso)
  {

    // Initializing the auxiliary variables
    Map<Integer, ArrayList<PointOfInterest>> risingPoints =
      new TreeMap<Integer, ArrayList<PointOfInterest>>();
    Map<Integer, ArrayList<PointOfInterest>> reductionPoints =
      new TreeMap<Integer, ArrayList<PointOfInterest>>();

    ArrayList<PointOfInterest> poiRef;
    ArrayList<Double[]> tempCompare = iso.getRefConsumptionMeans();

    // For each event available, each point of interest is compared with the
    // mean values of the refrigerator cluster.
    for (int i = 0; i < events.size(); i++) {
      // for (int i = 0; i < 3; i++) {

      int key = events.get(i).getId();

      // System.out.println("Event " + key);
      poiRef = new ArrayList<PointOfInterest>();

      // System.out.println("Rising Points");
      // Search through the rising points
      for (PointOfInterest poi: events.get(i).getRisingPoints()) {

        for (Double[] means: tempCompare) {

          if (poi.percentageEuclideanDistance(means) < Constants.REF_THRESHOLD) {
            // System.out.println("Euclidean Distance: "
            // + poi.percentageEuclideanDistance(means));
            poiRef.add(poi);
            break;
          }
        }
        if (poiRef.size() > 0)
          risingPoints.put(key, poiRef);
      }

      poiRef = new ArrayList<PointOfInterest>();
      // System.out.println("Reduction Points");
      // Search through the reduction points
      for (PointOfInterest poi: events.get(i).getReductionPoints()) {

        for (Double[] means: tempCompare) {

          Double[] tempMeans = { -means[0], -means[1] };

          if (poi.percentageEuclideanDistance(tempMeans) < Constants.REF_THRESHOLD) {
            // System.out.println("Euclidean Distance: "
            // + poi.percentageEuclideanDistance(means));
            poiRef.add(poi);
            break;
          }
        }
        if (poiRef.size() > 0)
          reductionPoints.put(key, poiRef);
      }
      // After finding the points of interest similar to the refrigerator's
      // end-use the event is cleaned of them
      cleanEvent(events, i, risingPoints.get(i), reductionPoints.get(i));
    }

    // Creation of the appliance and insert in the appliance list
    Appliance appliance =
      new Appliance("Refrigerator", "Refrigeration", risingPoints,
                    reductionPoints);

    applianceList.add(appliance);
  }

  /**
   * This function is used for the identification of a washing machine end use
   * in the event. There are some complex criteria taken into consideration for
   * the detection and confirmation of a washing machine at use.
   * 
   * @param events
   *          The list of the available events.
   */
  public void washingMachineIdentification (ArrayList<Event> events)
  {
    // Initializing the auxiliary variables
    boolean risingFlag = false, reductionFlag = false;
    Appliance appliance = null;

    // For each event
    for (int i = 0; i < events.size(); i++) {

      // Check if the event's duration is over a certain time interval
      if (events.get(i).getActivePowerConsumptions().length > Constants.WASHING_MACHINE_NUMBER_OF_MINUTES_LIMIT) {

        // Check if there are rising and reduction points with large active
        // power differences
        for (PointOfInterest rise: events.get(i).getRisingPoints())
          if (Math.abs(rise.getPDiff()) > Constants.WASHING_MACHINE_POWER_THRESHOLD) {
            risingFlag = true;
            // System.out.println("Rising Point: " + rise.toString());
            break;
          }

        for (PointOfInterest reduction: events.get(i).getReductionPoints())
          if (Math.abs(reduction.getPDiff()) > Constants.WASHING_MACHINE_POWER_THRESHOLD) {
            reductionFlag = true;
            // System.out.println("Reduction Point: " + reduction.toString());
            break;
          }

        // In case there are such points of interest
        if (risingFlag && reductionFlag) {
          // System.out.println("Search for Washing Machine in Event " + i);

          // Collect the event's active and reactive power measurements
          double[] tempReactive =
            Arrays.copyOf(events.get(i).getReactivePowerConsumptions(), events
                    .get(i).getReactivePowerConsumptions().length);
          double[] tempActive =
            Arrays.copyOf(events.get(i).getActivePowerConsumptions(), events
                    .get(i).getActivePowerConsumptions().length);

          // Removing refrigerator load from the measurements if it has been
          // detected in the current event.
          Appliance ref = applianceList.get(0);

          if (ref.getMatchingPoints().containsKey(i)) {

            double refReactive = ref.getMeanReactive();
            // System.out.println("Refrigerator Reactive: " + refReactive);
            int start = -1, end = -1;

            for (PointOfInterest[] poi: ref.getMatchingPoints(i)) {

              start = poi[0].getMinute();
              end = poi[1].getMinute();

              for (int j = start; j < end; j++)
                tempReactive[j] -= refReactive;
            }

            // System.out.println("Reactive: " + Arrays.toString(tempReactive));
          }

          // Find all the groups of consecutive values in the reactive power
          ArrayList<ConsecutiveValues> cons =
            createVectors(tempActive, tempReactive);

          // Find the group of consecutive points with the largest reactive
          // power measurement
          double maxAll = Double.NEGATIVE_INFINITY;
          int maxIndex = -1;
          for (int j = 0; j < cons.size(); j++) {
            // cons.get(j).status();
            if (maxAll < cons.get(j).getMaxQ()) {
              maxAll = cons.get(j).getMaxQ();
              maxIndex = j;
            }

          }

          // If all the criteria are met
          if (Utils.checkLimit(cons.get(maxIndex).getDifference(),
                               Constants.WASHING_MACHINE_DEVIATION_LIMIT)
              && cons.get(maxIndex).getNumberOfElements() > Constants.WASHING_MACHINE_NUMBER_OF_MINUTES_LIMIT) {

            // Switch the event's washing machine flag.
            events.get(i).setWashingMachineFlag();
            // Create points of interest and then add them to the washing
            // machine appliance (create if not already created).
            PointOfInterest[] match =
              {
               new PointOfInterest(cons.get(maxIndex).getStart(), true, cons
                       .get(maxIndex).getMaxQ(), cons.get(maxIndex).getMaxQ()),
               new PointOfInterest(cons.get(maxIndex).getEnd(), false, -cons
                       .get(maxIndex).getMaxQ(), -cons.get(maxIndex).getMaxQ()) };

            if (appliance == null)
              appliance = new Appliance("Washing Machine", "Cleaning");

            appliance.addMatchingPoints(i, match);

          }

          // Not only one at a time
          // for (int j = 0; j < cons.size(); j++) {
          // if (cons.get(j).getDifference() >
          // Constants.WASHING_MACHINE_DEVIATION_LIMIT
          // && cons.get(j).getNumberOfElements() >
          // Constants.WASHING_MACHINE_NUMBER_OF_MINUTES_LIMIT) {
          //
          // ArrayList<PointOfInterest> temp =
          // new ArrayList<PointOfInterest>();
          // temp.add(events.get(i).findPOI(cons.get(j).getStart(), true));
          // risingPoints.put(i, temp);
          //
          // temp = new ArrayList<PointOfInterest>();
          // temp.add(events.get(i).findPOI(cons.get(j).getStart(), true));
          // reductionPoints.put(i, temp);
          // break;
          // }
          //
          // }

        }
      }
      risingFlag = false;
      reductionFlag = false;

    }
    // If a washing machine was found add it to the appliance list.
    if (appliance != null)
      applianceList.add(appliance);

  }

  /**
   * This is an auxiliary function used for the cleaning of the events from
   * points of interest that are assigned to appliance.
   * 
   * @param events
   *          The list of events
   * @param index
   *          The index of the event under consideration
   * @param risingPoints
   *          The list of rising points of interest identified as refrigerator.
   * @param reductionPoints
   *          The list of reduction points of interest identified as
   *          refrigerator.
   */
  private void cleanEvent (ArrayList<Event> events, int index,
                           ArrayList<PointOfInterest> risingPoints,
                           ArrayList<PointOfInterest> reductionPoints)
  {

    // If there are both rising and reduction points.
    if (risingPoints != null && reductionPoints != null) {

      // System.out.println("rising: " + risingPoints.toString());
      // System.out.println("reduction: " + reductionPoints.toString());

      // In case all the rising and reduction points of the event are found to
      // be refrigerator then all are removed from the event
      if (events.get(index).getRisingPoints().size() == risingPoints.size()
          && events.get(index).getReductionPoints().size() == reductionPoints
                  .size()) {
        events.get(index).getReductionPoints().clear();
        events.get(index).getRisingPoints().clear();
      }
      // else if the number of rising and reduction points are equal, those
      // points are removed from the event's points of interest lists
      else if (risingPoints.size() == reductionPoints.size()) {

        for (PointOfInterest rise: risingPoints)
          events.get(index).getRisingPoints().remove(rise);

        for (PointOfInterest reduction: reductionPoints)
          events.get(index).getRisingPoints().remove(reduction);

      }
      // In any other case
      else {

        // Create a collection of the points of interest in chronological order
        ArrayList<PointOfInterest> temp =
          new ArrayList<PointOfInterest>(risingPoints);
        temp.addAll(reductionPoints);
        Collections.sort(temp, Constants.comp);

        // For all the points available, we search for pairs of rising -
        // reduction points and remove them from the event.
        for (int i = temp.size() - 2; i >= 0; i--) {

          if (temp.get(i).getRising() && temp.get(i + 1).getRising() == false) {

            events.get(index).getRisingPoints().remove(temp.get(i));
            events.get(index).getReductionPoints().remove(temp.get(i + 1));
            temp.remove(i + 1);
            temp.remove(i);
            i--;
          }
          // System.out.println(temp.size());

          // check if all the remaining points are of the same type (rising or
          // reduction).
          if (allSamePoints(temp))
            break;

        }

      }

    }

  }

  /**
   * This is an auxiliary function utilized for the creation of consecutive
   * values group. This helps in the detection of a washing machine.
   * 
   * @param tempActive
   *          The array of active power measurements.
   * @param tempReactive
   *          The array of reactive power measurements.
   * @return the list of groups of consecutive values.
   */
  private ArrayList<ConsecutiveValues> createVectors (double[] tempActive,
                                                      double[] tempReactive)
  {
    // Initializing the auxiliary variables
    ArrayList<ConsecutiveValues> cons = new ArrayList<ConsecutiveValues>();
    int start = 0, end = 0;
    boolean startFlag = false;

    // For each index in the measurement arrays check the reactive power.
    for (int j = 0; j < tempReactive.length; j++) {

      // In case it is positive and not a group has started
      if (tempReactive[j] > 0 && !startFlag) {
        start = j;
        startFlag = true;
      }

      // In case it is negative or zero and in the middle of a group creation
      if (tempReactive[j] <= 0 && startFlag) {

        // Checking if there are no positive values close enough meaning that
        // the consecutive values go on
        boolean flag = true;

        int endIndex =
          Math.min(j + Constants.WASHING_MACHINE_MERGING_MINUTE_LIMIT,
                   tempReactive.length);

        for (int k = j + 1; k < endIndex; k++)

          if (tempReactive[k] > 0) {

            flag = false;
            // System.out.println("Index: " + k + " Value: " + tempReactive[k]
            // + " Flag: " + flag);
            break;
          }

        // End the current group and add it to the list.
        if (flag) {
          end = j - 1;

          cons.add(new ConsecutiveValues(start, end, Arrays
                  .copyOfRange(tempActive, start, end + 1), Arrays
                  .copyOfRange(tempReactive, start, end + 1)));

          start = -1;
          end = -1;
          startFlag = false;
        }
      }
    }

    return cons;

  }

  /**
   * This is an auxiliary function used for checking if all the points of
   * interest are of the same type.
   * 
   * @param pois
   *          A list of points of interest
   * @return true if they are all of the same type, false otherwise.
   */
  private boolean allSamePoints (ArrayList<PointOfInterest> pois)
  {
    // Initializing the auxiliary variables
    boolean flag = true;
    boolean start = pois.get(0).getRising();

    for (PointOfInterest poi: pois)
      if (start != poi.getRising()) {
        flag = false;
        break;
      }

    return flag;
  }

  /**
   * This function is responsible for the analysis of an events final pairs of
   * points of interest. Each pair is compared with already detected appliance
   * and if they do not fit, then they are added to a new appliance.
   * 
   * @param event
   *          The event under examination.
   */
  public void analyseEvent (Event event)
  {
    // Initializing the auxiliary variables
    double[] meanValues = null;
    double minDistance = Double.POSITIVE_INFINITY;
    int minIndex = -1;
    int duration = 0;

    // For each final pair included in the event.
    for (int i = 0; i < event.getFinalPairs().size(); i++) {

      // System.out.println("Final Pair " + i);

      // Estimate the mean values and the duration of the pair
      meanValues = Utils.meanValues(event.getFinalPairs(i));
      duration =
        event.getFinalPairs(i)[1].getMinute()
                - event.getFinalPairs(i)[0].getMinute();

      // System.out.println("Mean Values: " + Arrays.toString(meanValues));

      // Try to match the pair with an already existing appliance.
      for (int j = 0; j < applianceList.size(); j++) {
        // System.out.println("Appliance " + j);
        // System.out.println("Appliance Mean Values "
        // + Arrays.toString(applianceList.get(j)
        // .getMeanValues()));

        // System.out
        // .println("Distance: "
        // + Utils.percentageEuclideanDistance(meanValues,
        // applianceList
        // .get(j)
        // .getMeanValues()));

        if (applianceList.get(j).isClose(meanValues, duration)
            && minDistance > Utils.percentageEuclideanDistance(applianceList
                    .get(j).getMeanValues(), meanValues)) {

          // System.out.println("Percentage Distance: "
          // + Utils.percentageEuclideanDistance(applianceList
          // .get(j).getMeanValues(), meanValues));
          //
          // System.out.println("Absolute Distance: "
          // + Utils.absoluteEuclideanDistance(applianceList
          // .get(j).getMeanValues(), meanValues));

          minDistance =
            Utils.percentageEuclideanDistance(applianceList.get(j)
                    .getMeanValues(), meanValues);
          minIndex = j;
        }

      }
      // If an appropriate appliance is found, the pair is added to that
      // appliance. Otherwise, a new appliance is created.
      if (minIndex != -1)
        applianceList.get(minIndex).addMatchingPoints(event.getId(),
                                                      event.getFinalPairs(i));
      else
        createNewAppliance(event.getId(), event.getFinalPairs(i), meanValues);

    }
    event.clear();
  }

  /**
   * This is the appliance creation function. A set of heuristic rules are used
   * to identify the appliance type and then the appliance is created and added
   * to the appliance list.
   * 
   * @param eventIndex
   * @param finalPair
   * @param meanValues
   */
  private void createNewAppliance (int eventIndex, PointOfInterest[] finalPair,
                                   double[] meanValues)
  {
    // Initializing the auxiliary variables
    int duration = finalPair[1].getMinute() - finalPair[0].getMinute();
    Appliance appliance = null;

    if (meanValues[0] > Constants.WATERHEATER_POWER_THRESHOLD) {

      appliance =
        new Appliance("Water Heater " + Constants.WATERHEATER_ID++, "Cleaning");

    }
    else if (meanValues[0] < Constants.SMALL_POWER_THRESHOLD
             && Math.abs(meanValues[1]) < Constants.REACTIVE_THRESHOLD) {
      if (meanValues[0] < Constants.TINY_POWER_THRESHOLD)
        appliance =
          new Appliance("Lighting " + Constants.LIGHTING_ID++, "Lighting");
      else
        appliance =
          new Appliance("Entertainment " + Constants.ENTERTAINMENT_ID++,
                        "Entertainment");
    }
    else if (meanValues[0] < Constants.WATERHEATER_POWER_THRESHOLD
             && meanValues[0] > Constants.SMALL_POWER_THRESHOLD
             && Math.abs(meanValues[1]) < Constants.REACTIVE_THRESHOLD) {

      appliance = new Appliance("Cooking " + Constants.COOKING_ID++, "Cooking");
    }
    else if (Math.abs(meanValues[1]) > Constants.REACTIVE_THRESHOLD) {
      if (duration <= Constants.MICROWAVE_DURATION)
        appliance =
          new Appliance("Microwave Oven " + Constants.MICROWAVE_OVEN_ID++,
                        "Cooking");
      else {

        if (meanValues[0] < Constants.MEDIUM_POWER_THRESHOLD)
          appliance =
            new Appliance("Extractor " + Constants.EXTRACTOR_ID++, "Cooking");
        else
          appliance =
            new Appliance("Vacuum Cleaner " + Constants.VACUUM_CLEANER_ID++,
                          "Cleaning");
      }
    }
    appliance.addMatchingPoints(eventIndex, finalPair);
    applianceList.add(appliance);
  }

  /**
   * This function is used to extract the knowledge of appliances and activities
   * resulted from the disaggregation procedure into files.
   * 
   * @param outputAppliance
   *          The file name of the output file containing the appliances.
   * @param outputActivity
   *          The file name of the output file containing the activity.
   * @param events
   *          The list of available events.
   * @throws FileNotFoundException
   */
  public void createDisaggregationFiles (String outputAppliance,
                                         String outputActivity,
                                         ArrayList<Event> events)
    throws FileNotFoundException
  {

    PrintStream realSystemOut = System.out;

    OutputStream output = new FileOutputStream(outputAppliance);
    PrintStream printOut = new PrintStream(output);
    System.setOut(printOut);

    String[] temp = null;

    for (Appliance appliance: applianceList) {
      temp = appliance.applianceToString();
      for (int i = 0; i < temp.length; i++) {
        if (i != temp.length - 1)
          System.out.print(temp[i] + ",");
        else
          System.out.println(temp[i]);
      }
      activityList.addAll(appliance.matchingPairsToString(events));
    }

    output = new FileOutputStream(outputActivity);
    printOut = new PrintStream(output);
    System.setOut(printOut);
    Collections.sort(activityList, Constants.comp3);

    for (String[] activity: activityList) {
      for (int i = 0; i < activity.length; i++) {
        if (i != activity.length - 1)
          System.out.print(activity[i] + ",");
        else
          System.out.println(activity[i]);
      }
    }
  }
}
