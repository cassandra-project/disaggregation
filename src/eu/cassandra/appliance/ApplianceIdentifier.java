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

import org.apache.log4j.Logger;

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

  static Logger log = Logger.getLogger(ApplianceIdentifier.class);

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
   * This variable is a list of the event index that need second pass for the
   * correct refrigerator identification in the events.
   */
  Map<Integer, ArrayList<PointOfInterest>> secondPass =
    new TreeMap<Integer, ArrayList<PointOfInterest>>();

  /**
   * This variable is a used for storing the metrics from the refrigerator
   * cluster and using them later on the create the refrigerator appliance.
   */
  double[] metrics = new double[3];

  /**
   * The simple constructor of the appliance identifier.
   */
  public ApplianceIdentifier ()
  {
    clear();
  }

  /**
   * This constructor creates the appliance list added by the user as a base for
   * the next steps of the procedure.
   * 
   * @param applianceFilename
   *          The filename of the file containing the attributes of the
   *          installed appliances.
   * @throws FileNotFoundException
   */
  public ApplianceIdentifier (String applianceFilename)
    throws FileNotFoundException
  {
    clear();
    applianceList = Utils.appliancesFromFile(applianceFilename);
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
  public void refrigeratorIdentification (ArrayList<Event> events)
  {

    log.info("=============FRIDGE DETECTION=================");

    Appliance ref = applianceList.get(0);

    // Initializing the auxiliary variables
    Map<Integer, ArrayList<PointOfInterest>> risingPoints =
      new TreeMap<Integer, ArrayList<PointOfInterest>>();
    Map<Integer, ArrayList<PointOfInterest>> reductionPoints =
      new TreeMap<Integer, ArrayList<PointOfInterest>>();

    ArrayList<PointOfInterest> poiRef;
    ArrayList<PointOfInterest> risingRef = ref.getRisingPoints();
    log.debug("Ref Rising Points:" + risingRef.toString());
    ArrayList<PointOfInterest> reductionRef = ref.getReductionPoints();
    log.debug("Ref Reduction Points:" + reductionRef.toString());
    log.debug("Ref Means:" + Arrays.toString(ref.getMeanValues()));

    // For each event available, each point of interest is compared with the
    // mean values of the refrigerator cluster.
    for (int i = 0; i < events.size(); i++) {

      int key = events.get(i).getId();
      log.debug("");
      log.debug("Event " + key);
      poiRef = new ArrayList<PointOfInterest>();

      // log.debug("Rising Points");
      // Search through the rising points
      for (PointOfInterest poi: events.get(i).getRisingPoints()) {

        double[] means = { poi.getPDiff(), poi.getQDiff() };
        // log.debug("Investigating Point: " + poi.getPDiff() + ", "
        // + poi.getQDiff());

        for (PointOfInterest rise: risingRef) {
          // log.debug("Rising Point: " + rise.getPDiff() + ", " +
          // rise.getQDiff());
          double distance = rise.percentageEuclideanDistance(means);
          // log.debug("Euclidean Distance Rising: " + distance);

          if (distance < Constants.REF_THRESHOLD) {
            // log.debug("Euclidean Distance Rising: "
            // + distance);
            poiRef.add(poi);
            break;
          }
        }

      }

      // log.debug("Poiref: " + poiRef.toString());
      if (poiRef.size() > 0)
        risingPoints.put(key, poiRef);

      poiRef = new ArrayList<PointOfInterest>();
      // log.debug("Reduction Points");
      // Search through the reduction points
      for (PointOfInterest poi: events.get(i).getReductionPoints()) {

        double[] means = { poi.getPDiff(), poi.getQDiff() };
        // log.debug("Investigating Point: " + poi.getPDiff() + ", "
        // + poi.getQDiff());

        for (PointOfInterest red: reductionRef) {
          // log.debug("Rising Point: " + red.getPDiff() + ", " +
          // red.getQDiff());
          double distance = red.percentageEuclideanDistance(means);
          // log.debug("Euclidean Distance Rising: " + distance);

          if (distance < Constants.REF_THRESHOLD) {
            // log.debug("Euclidean Distance Rising: "
            // + distance);
            poiRef.add(poi);
            break;
          }
        }
      }

      // log.debug("Poiref: " + poiRef.toString());
      if (poiRef.size() > 0)
        reductionPoints.put(key, poiRef);

      // log.debug(risingPoints.get(key));
      // log.debug(reductionPoints.get(key));

      // After finding the points of interest similar to the refrigerator's
      // end-use the event is cleaned of them
      if (risingPoints.get(key) != null)
        log.debug("Rising size:" + risingPoints.get(key).size());
      if (reductionPoints.get(key) != null)
        log.debug("Reduction size:" + reductionPoints.get(key).size());

      if (risingPoints.get(key) != null && reductionPoints.get(key) != null) {
        log.debug("Event " + events.get(i).getId() + " Before: Rising "
                  + risingPoints.get(key) + "  Reduction "
                  + reductionPoints.get(key));
        cleanEvent(events.get(i), risingPoints.get(key),
                   reductionPoints.get(key));
        log.debug("Event " + events.get(i).getId() + " After: Rising "
                  + risingPoints.get(key) + "  Reduction "
                  + reductionPoints.get(key));
        if (risingPoints.get(key).size() == 0) {
          risingPoints.remove(key);
          reductionPoints.remove(key);
        }
      }
    }

    metrics[0] = ref.getMeanActive();
    metrics[1] = ref.getMeanReactive();
    metrics[2] = ref.getMeanDuration();

    log.info("Metrics: ");
    log.info("Mean P: " + metrics[0]);
    log.info("Mean Q: " + metrics[1]);
    log.info("Mean Duration: " + metrics[2]);
    log.info("");
    log.info("==========REF SECOND PASS============");

    cleanSecondPass(events, ref);

    // ref.status();

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
  private void cleanEvent (Event event,
                           ArrayList<PointOfInterest> risingPoints,
                           ArrayList<PointOfInterest> reductionPoints)
  {

    if (risingPoints.size() > 0)
      log.debug("rising: " + risingPoints.size());
    if (reductionPoints.size() > 0)
      log.debug("reduction: " + reductionPoints.size());

    ArrayList<PointOfInterest> temp =
      new ArrayList<PointOfInterest>(risingPoints);
    temp.addAll(reductionPoints);
    secondPass.put(event.getId() - 1, temp);

    if ((risingPoints.size() == 1 && reductionPoints.size() == 1) == false) {
      risingPoints.clear();
      reductionPoints.clear();
    }
    else {
      int duration =
        reductionPoints.get(0).getMinute() - risingPoints.get(0).getMinute();

      if (duration < Constants.REF_MIN_DURATION
          || duration > Constants.REF_MAX_DURATION) {
        risingPoints.clear();
        reductionPoints.clear();
      }
    }
  }

  /**
   * Finding the refrigeration in some events with second passing
   * 
   * @param events
   *          The list of events.
   * @param fridge
   *          The refrigeration appliance as defined by that time.
   * @param secondPass
   *          The list of event indices that need a second pass.
   */
  private void cleanSecondPass (ArrayList<Event> events, Appliance fridge)
  {
    int duration = -1;
    double[] meanValues = new double[2];

    log.debug("");
    log.debug("");
    log.debug("Second Pass: " + secondPass.toString());

    // For each event in need of second pass
    for (Integer key: secondPass.keySet()) {

      ArrayList<PointOfInterest> temp = secondPass.get(key);
      Collections.sort(temp, Constants.comp);

      log.debug("Event: " + events.get(key).getId());
      log.debug("Before Size: " + temp.size());
      log.debug("Before: " + temp.toString());

      // For each point of interest
      for (int i = temp.size() - 2; i >= 0; i--) {

        if (temp.get(i).getRising()) {

          PointOfInterest rise = temp.get(i);

          for (int j = i; j < temp.size(); j++) {

            if (temp.get(j).getRising() == false) {

              PointOfInterest red = temp.get(j);

              PointOfInterest[] pois = { rise, red };

              meanValues = Utils.meanValues(pois);
              duration = red.getMinute() - rise.getMinute();

              if (Utils.isCloseRef(meanValues, duration, metrics)) {

                // System.out.println("Mean Values: "
                // + Arrays.toString(meanValues) + " Duration:"
                // + duration);

                fridge.addMatchingPoints(events.get(key).getId(), pois);

                events.get(key).getRisingPoints().remove(rise);
                events.get(key).getReductionPoints().remove(red);
                temp.remove(j);
                temp.remove(i);
                i--;
                break;
              }
            }
          }
        }
        if (temp.size() == 0 || Utils.allSamePoints(temp))
          break;
      }

      log.debug("After Size: " + temp.size());
      log.debug("After: " + temp.toString());
      log.debug("");
    }

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

    log.info("===============WASHING MACHINE=================");
    log.info("");
    // For each event
    for (int i = 0; i < events.size(); i++) {

      Event event = events.get(i);

      // Check if the event's duration is over a certain time interval
      if (event.getActivePowerConsumptions().length > Constants.WASHING_MACHINE_NUMBER_OF_MINUTES_LIMIT) {

        log.debug("");
        log.debug("Event " + event.getId() + " Start: "
                  + event.getStartMinute() + " End: " + event.getEndMinute());
        log.debug("===================");

        // Check if there are rising and reduction points with large active
        // power differences
        for (PointOfInterest rise: event.getRisingPoints())
          if (Math.abs(rise.getPDiff()) > Constants.WASHING_MACHINE_POWER_THRESHOLD) {
            risingFlag = true;
            // System.out.println("Rising Point: " + rise.toString());
            break;
          }

        for (PointOfInterest reduction: event.getReductionPoints())
          if (Math.abs(reduction.getPDiff()) > Constants.WASHING_MACHINE_POWER_THRESHOLD) {
            reductionFlag = true;
            // System.out.println("Reduction Point: " + reduction.toString());
            break;
          }

        log.debug("Rising over 1K5: " + risingFlag + " Reduction over 1K5: "
                  + reductionFlag);

        // In case there are such points of interest
        if (risingFlag && reductionFlag) {
          // System.out.println();
          // System.out.println();
          log.debug("Search for Washing Machine in Event "
                    + events.get(i).getId());
          log.debug("");
          // Collect the event's active and reactive power measurements
          double[] tempReactive =
            Arrays.copyOf(event.getReactivePowerConsumptions(),
                          event.getReactivePowerConsumptions().length);
          double[] tempActive =
            Arrays.copyOf(event.getActivePowerConsumptions(),
                          event.getActivePowerConsumptions().length);
          // System.out.println("Reactive: " + Arrays.toString(tempReactive));
          // Removing refrigerator load from the measurements if it has been
          // detected in the current event.
          if (applianceList.size() > 1) {
            Appliance ref = applianceList.get(0);

            if (ref.getMatchingPoints().containsKey(event.getId())) {

              log.debug("Reactive Before: " + Arrays.toString(tempReactive));

              double refReactive = ref.getMeanReactive();
              // System.out.println("Refrigerator Reactive: " + refReactive);
              int start = -1, end = -1;

              for (PointOfInterest[] poi: ref.getMatchingPoints(event.getId())) {

                start = poi[0].getMinute();
                end = poi[1].getMinute();

                for (int j = start; j < end; j++)
                  tempReactive[j] -= refReactive;
              }

              log.debug("Reactive After Refrigerator: "
                        + Arrays.toString(tempReactive));

            }
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

          for (int j = 0; j < cons.size(); j++)
            cons.get(j).status();

          if (maxIndex != -1) {

            log.debug("The WINNER IS");
            cons.get(maxIndex).status();

            boolean check =
              (Utils.checkLimit(cons.get(maxIndex).getDifference(),
                                Constants.WASHING_MACHINE_DIFFERENCE_LIMIT) && cons
                      .get(maxIndex).getNumberOfElements() > Constants.WASHING_MACHINE_NUMBER_OF_MINUTES_LIMIT);
            log.debug("Objectives Met: " + check);

            // If all the criteria are met
            if (check) {

              log.info("Event Id: " + events.get(i).getId()
                       + " Washing Machine Detected");
              cons.get(maxIndex).status();

              int[] pair =
                { i, cons.get(maxIndex).getStart(), cons.get(maxIndex).getEnd() };

              // Switch the event's washing machine flag.
              events.get(i).setWashingMachineFlag();

              // If a washing machine was found create it and add it to the
              // appliance list.
              Appliance washing =
                new Appliance("Washing Machine "
                              + Constants.WASHING_MACHINE_ID++, "Cleaning");

              double[][] consumption =
                { cons.get(maxIndex).getPValues(),
                 cons.get(maxIndex).getQValues() };
              washing.setConsumption(consumption);
              washing.setTimeStamp(pair);
              applianceList.add(washing);

            }
          }
        }
      }
      risingFlag = false;
      reductionFlag = false;

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
   * This function is responsible for the analysis of an events final pairs of
   * points of interest. Each pair is compared with already detected appliance
   * and if they do not fit, then they are added to a new appliance.
   * 
   * @param event
   *          The event under examination.
   * @param isolated
   *          The flag that shows if the event is isolated or not.
   */
  public void analyseEvent (Event event, boolean isolated)
  {
    // Initializing the auxiliary variables
    double[] meanValues = null;
    double minDistance = Double.POSITIVE_INFINITY;
    int minIndex = -1;
    int duration = 0;
    double percDistance = 0, absDistance = 0;
    boolean switchedOn, close;

    // For each final pair included in the event.
    for (int i = 0; i < event.getFinalPairs().size(); i++) {
      minIndex = -1;
      minDistance = Double.POSITIVE_INFINITY;
      // log.info("Final Pair " + i);

      // Estimate the mean values and the duration of the pair
      meanValues = Utils.meanValues(event.getFinalPairs(i));
      duration =
        event.getFinalPairs(i)[1].getMinute()
                - event.getFinalPairs(i)[0].getMinute();

      if (duration > Constants.TEMPORAL_THRESHOLD)
        log.info("Event: " + event.getId() + " Pair: "
                 + event.getFinalPairs(i)[0].toString() + "    "
                 + event.getFinalPairs(i)[1] + " Duration:" + duration);

      int startTime =
        event.getFinalPairs(i)[0].getMinute() + event.getStartMinute();

      int endTime =
        event.getFinalPairs(i)[1].getMinute() + event.getStartMinute();

      if (!isolated) {
        log.debug("");
        log.debug("Start: " + startTime + " End: " + endTime + " Duration: "
                  + duration);

        log.debug("Mean Values: " + Arrays.toString(meanValues));

        log.debug("");
      }
      // Try to match the pair with an already existing appliance.
      for (int j = 0; j < applianceList.size(); j++) {

        if (applianceList.get(j).getMeanActive() != -1) {

          if (!isolated) {
            log.debug("");
            log.debug(applianceList.get(j).toString());

            log.debug("Appliance Mean Values "
                      + Arrays.toString(applianceList.get(j).getMeanValues()));
          }
          percDistance =
            Utils.percentageEuclideanDistance(applianceList.get(j)
                    .getMeanValues(), meanValues);

          absDistance =
            Utils.absoluteEuclideanDistance(applianceList.get(j)
                    .getMeanValues(), meanValues);

          if (Constants.CLUSTER_APPLIANCES) {
            switchedOn = false;

            close = applianceList.get(j).isCloseClustered(meanValues, duration);
          }
          else {
            switchedOn =
              applianceList.get(j).isSwitchedOn(event, startTime, endTime);

            close = applianceList.get(j).isClose(meanValues, duration);
          }
          if (!isolated) {
            log.debug("Percentage Distance: " + percDistance);

            log.debug("Absolute Distance: " + absDistance);

            log.debug("Switched On: " + switchedOn);

            log.debug("Close By?: " + close);
          }
          if (close && (minDistance > percDistance) && (switchedOn == false)) {

            minDistance =
              Utils.percentageEuclideanDistance(applianceList.get(j)
                      .getMeanValues(), meanValues);
            minIndex = j;
            if (!isolated)
              log.debug("New Min Distance: " + minDistance);

            if (isolated
                && applianceList.get(j).getActivity()
                        .equalsIgnoreCase("Refrigeration"))
              minDistance = Constants.NEAR_ZERO;

          }

        }
      }
      // If an appropriate appliance is found, the pair is added to that
      // appliance. Otherwise, a new appliance is created.
      if (minIndex != -1) {
        if (!isolated)
          log.debug("Matches Appliance " + minIndex);
        applianceList.get(minIndex).addMatchingPoints(event.getId(),
                                                      event.getFinalPairs(i));

      }
      else {
        if (Constants.APPLIANCE_TYPE.equalsIgnoreCase("Generic"))
          createNewAppliance(event.getId(), event.getFinalPairs(i), meanValues);
        else if (Constants.APPLIANCE_TYPE.equalsIgnoreCase("Activity"))
          createNewApplianceActivity(event.getId(), event.getFinalPairs(i),
                                     meanValues);
        if (!isolated) {
          log.debug("No Match! New Appliance");

        }
      }
    }
    if (!isolated) {
      log.info("");
      log.info("Final Number Of Appliances: " + applianceList.size());
    }
    event.clear(isolated);
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
    log.info("");
    log.info("============== DISAGGREGATION FILES ================");

    OutputStream output = new FileOutputStream(outputAppliance);
    PrintStream printOut = new PrintStream(output);
    System.setOut(printOut);

    String[] temp = null;

    Collections.sort(applianceList, Constants.comp7);

    for (Appliance appliance: applianceList) {

      int operations = appliance.operationTimes();
      boolean wmFlag = appliance.getName().contains("Washing");
      log.debug("Appliance: " + appliance.toString());
      log.debug("Appliance Operations: " + operations + " Weeks: "
                + Constants.WEEKS);

      if (appliance.getActivity().equalsIgnoreCase("Refrigeration"))
        appliance.estimateDistance(events, true);

      if (operations > Constants.WEEKS || wmFlag) {
        // if (operations >= Constants.WEEKS || wmFlag) {
        log.debug("IN!");
        temp = appliance.applianceToString();
        for (int i = 0; i < temp.length; i++) {
          if (i != temp.length - 1)
            System.out.print(temp[i] + ",");
          else
            System.out.println(temp[i]);
        }
        activityList.addAll(appliance.matchingPairsToString(events));

      }
    }

    // Add standby consumption
    System.out.println("Consumption,Standby," + Constants.MINIMUM_THRESHOLD
                       + ",0.0");

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

  public void appliancesFromIsolated (IsolatedEventsExtractor iso)
  {
    if (Constants.REF_LOOSE_COUPLING == false) {
      double[] meanValues = iso.getRefMeans();
      Appliance fridge =
        new Appliance("Refrigerator Cluster", "Refrigeration", meanValues[0],
                      meanValues[1], meanValues[2], iso.getClusters()
                              .get(iso.getRefrigeratorCluster()).size());

      fridge.status();

      applianceList.add(fridge);
    }
    ArrayList<Event> isolated = iso.getIsolatedEvents();

    for (Event event: isolated) {
      analyseEvent(event, true);
    }
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
    // int duration = finalPair[1].getMinute() - finalPair[0].getMinute();
    // System.out.println("New Appliance");
    Appliance appliance =
      new Appliance("Appliance " + Constants.APPLIANCE_ID++, "Generic");

    appliance.addMatchingPoints(eventIndex, finalPair);
    applianceList.add(appliance);
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
  private void createNewApplianceActivity (int eventIndex,
                                           PointOfInterest[] finalPair,
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
    else {
      appliance = new Appliance("Generic " + Constants.GENERIC_ID++, "Generic");
    }
    appliance.addMatchingPoints(eventIndex, finalPair);
    applianceList.add(appliance);
  }

  public void clear ()
  {
    applianceList.clear();
    activityList.clear();
    secondPass.clear();
  }
}
