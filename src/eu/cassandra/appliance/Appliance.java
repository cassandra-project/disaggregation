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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import eu.cassandra.event.Event;
import eu.cassandra.utils.Constants;
import eu.cassandra.utils.PointOfInterest;
import eu.cassandra.utils.Utils;

/**
 * This class is used for implementing the Appliance Models in the Training
 * Module of Cassandra Project. The models created here are compatible with the
 * Appliance Models used in the main Cassandra Platform and can be easily
 * exported to the User's Library.
 * 
 * @author Antonios Chrysopoulos
 * @version 0.9, Date: 29.07.2013
 */

public class Appliance
{
  static Logger log = Logger.getLogger(Appliance.class);

  /**
   * This variable is the name of the appliance.
   */
  private String name = "";

  /**
   * This variable is the activity that the appliance is participating to.
   */
  private String activity = "";

  /**
   * This variable is a map of the event id number and a list of the detected
   * rising points (points that the active power is increasing).
   */
  private final Map<Integer, ArrayList<PointOfInterest>> risingPoints =
    new TreeMap<Integer, ArrayList<PointOfInterest>>();

  /**
   * This variable is a map of the event id number and a list of the detected
   * reduction points (points that the active power is increasing).
   */
  private final Map<Integer, ArrayList<PointOfInterest>> reductionPoints =
    new TreeMap<Integer, ArrayList<PointOfInterest>>();

  /**
   * This variable is a map of the event id number and a list of the detected
   * matching pairs of points of interest.
   */
  private final Map<Integer, ArrayList<PointOfInterest[]>> matchingPoints =
    new TreeMap<Integer, ArrayList<PointOfInterest[]>>();

  /**
   * This variable represents the summary of the values of the active and
   * reactive power from the matching number of matching pairs.
   */
  private final double[] meanValuesSum = new double[2];

  /**
   * This variable represents the number of matching points detected as part of
   * this appliance.
   */
  private int numberOfMatchingPoints = 0;

  /**
   * This variable represents the mean duration of the appliance end-use.
   */
  private double durationSum = 0;

  /**
   * This variable represents the mean distance between triggering of the
   * appliance.
   */
  private double distance = 0;

  /**
   * This is a variable containing the active and reactive consumption model of
   * the washing machine.
   */
  private double[][] consumptionModel = null;

  /**
   * This is a variable containing the active and reactive consumption model of
   * the washing machine.
   */
  private int[] timeStamp = null;

  /**
   * The constructor of an Appliance Model.
   * 
   * @param name
   *          The name of the Appliance Model
   * @param activity
   *          The name of the activity the appliance participates in.
   */
  public Appliance (String name, String activity)
  {
    this.name = name;
    this.activity = activity;
    risingPoints.clear();
    reductionPoints.clear();
    matchingPoints.clear();
  }

  /**
   * The constructor of an Appliance Model coming from a file.
   * 
   * @param name
   *          The name of the Appliance Model
   * @param activity
   *          The name of the activity the appliance participates in.
   */
  public Appliance (String name, String activity, double p, double q,
                    double duration, int numberOfPoints)
  {
    this.name = name;
    this.activity = activity;
    meanValuesSum[0] = p * numberOfPoints;
    meanValuesSum[1] = q * numberOfPoints;
    durationSum = duration * numberOfPoints / 2;
    numberOfMatchingPoints = numberOfPoints;
  }

  /**
   * This function is used as a getter for the matching points of the appliance.
   * 
   * @return the map of the matching points of the appliance.
   */
  public Map<Integer, ArrayList<PointOfInterest[]>> getMatchingPoints ()
  {
    if (name.contains("Washing"))
      return null;
    else
      return matchingPoints;
  }

  /**
   * This function is used as a getter for the matching points of the appliance
   * for a certain event.
   * 
   * @return the list of the matching points of the appliance for a certain
   *         event.
   */
  public ArrayList<PointOfInterest[]> getMatchingPoints (int index)
  {
    if (name.contains("Washing"))
      return null;
    else
      return matchingPoints.get(index);
  }

  /**
   * This function is used as a getter for mean value of the active power
   * measurements.
   * 
   * @return the mean active power.
   */
  public double getMeanActive ()
  {
    if (name.contains("Washing"))
      return -1;
    else
      return meanValuesSum[0] / numberOfMatchingPoints;
  }

  /**
   * This function is used as a getter for mean value of the reactive power
   * measurements.
   * 
   * @return the mean reactive power.
   */
  public double getMeanReactive ()
  {
    if (name.contains("Washing"))
      return -1;
    else
      return meanValuesSum[1] / numberOfMatchingPoints;
  }

  /**
   * This function is used as a getter for mean value of the duration of
   * functioning.
   * 
   * @return the mean reactive power.
   */
  public double getMeanDuration ()
  {
    if (name.contains("Washing"))
      return -1;
    else
      return 2 * durationSum / numberOfMatchingPoints;
  }

  /**
   * This function is used as a getter for mean value of the active and the
   * reactive power measurements.
   * 
   * @return the mean active and reactive power.
   */
  public double[] getMeanValues ()
  {
    if (name.contains("Washing"))
      return null;
    else {
      double[] temp = { getMeanActive(), getMeanReactive() };
      return temp;
    }
  }

  /**
   * This function is used as a getter for the name of the appliance.
   * 
   * @return the name of the appliance.
   */
  public String getName ()
  {
    return name;
  }

  /**
   * This function is used as a getter for all the rising points detected for an
   * appliance.
   * 
   * @return a list of the rising points of the appliance.
   */
  public ArrayList<PointOfInterest> getRisingPoints ()
  {
    ArrayList<PointOfInterest> result = new ArrayList<PointOfInterest>();

    for (Integer index: matchingPoints.keySet())
      for (PointOfInterest[] pois: matchingPoints.get(index))
        result.add(pois[0]);

    return result;
  }

  /**
   * This function is used as a getter for all the reduction points detected for
   * an appliance.
   * 
   * @return a list of the reduction points of the appliance.
   */
  public ArrayList<PointOfInterest> getReductionPoints ()
  {
    ArrayList<PointOfInterest> result = new ArrayList<PointOfInterest>();

    for (Integer index: matchingPoints.keySet())
      for (PointOfInterest[] pois: matchingPoints.get(index))
        result.add(pois[1]);

    return result;
  }

  /**
   * This function is used as a getter for the activity of the appliance.
   * 
   * @return the activity of the appliance.
   */
  public String getActivity ()
  {
    return activity;
  }

  /**
   * This function is used as a getter for the distance between end-uses
   * of the appliance.
   * 
   * @return the distance between end uses.
   */
  public double getDistance ()
  {
    return distance;
  }

  /**
   * This function adds a certain point of interest to the reduction points'
   * list.
   * 
   * @param eventIndex
   *          The index of the event the point of interest is originated.
   * @param pois
   *          The pair of matching points of interest.
   */
  public void addMatchingPoints (int eventIndex, PointOfInterest[] pois)
  {

    if (!matchingPoints.containsKey(eventIndex))
      matchingPoints.put(eventIndex, new ArrayList<PointOfInterest[]>());

    matchingPoints.get(eventIndex).add(pois);
    addMeanValues(pois);

  }

  /**
   * This auxiliary function is used for the re-estimation of the mean values
   * when a new matching pair of points of interest is added to the appliance.
   * 
   * @param pois
   *          The new matching pair of points of interest
   */
  public void addMeanValues (PointOfInterest[] pois)
  {
    meanValuesSum[0] += pois[0].getPDiff() - pois[1].getPDiff();
    meanValuesSum[1] += pois[0].getQDiff() - pois[1].getQDiff();
    durationSum += pois[1].getMinute() - pois[0].getMinute();
    numberOfMatchingPoints += 2;
  }

  /**
   * This function is used for the printing of the matching pairs to the
   * console.
   * 
   * @param events
   *          The list of events
   * @return a list of string arrays containing the information needed.
   */
  public ArrayList<String[]> matchingPairsToString (ArrayList<Event> events)
  {

    ArrayList<String[]> result = new ArrayList<String[]>();
    int offset = 0;
    int start = -1, end = -1;

    boolean wmFlag = name.contains("Washing");

    if (wmFlag) {

      offset = events.get(timeStamp[0]).getStartMinute();

      start = offset + timeStamp[1];
      end = offset + timeStamp[2];

      String[] tempString =
        { name, activity, Integer.toString(start), Integer.toString(end) };
      result.add(tempString);

    }
    else {
      for (Integer key: matchingPoints.keySet()) {

        offset = events.get(key - 1).getStartMinute();

        for (PointOfInterest[] pois: matchingPoints.get(key)) {
          start = offset + pois[0].getMinute();
          end = offset + pois[1].getMinute();

          if (start > end) {
            log.debug("Problem with Start > End");
            log.debug("Event: " + events.get(key - 1).getId());
            log.debug("Appliance: " + name);
            log.debug("Start: " + start);
            log.debug("End: " + end);
            log.debug("");
          }

          String[] tempString =
            { name, activity, Integer.toString(start), Integer.toString(end),
             Double.toString(pois[0].getPDiff()),
             Double.toString(pois[0].getQDiff()),
             Double.toString(pois[1].getPDiff()),
             Double.toString(pois[1].getQDiff()) };
          result.add(tempString);
        }
      }
    }
    // for (String[] string: result)
    // System.out.println(Arrays.toString(string));
    return result;
  }

  /**
   * This function is used for the printing of the appliance attributes to the
   * console.
   * 
   * @return a string array containing the appliance information needed.
   */
  public String[] applianceToString ()
  {
    String[] result = null;

    boolean refFlag = activity.equalsIgnoreCase("Refrigeration");
    // boolean wmFlag = activity.equalsIgnoreCase("Cleaning");
    boolean wmFlag = name.contains("Washing");
    if (refFlag)
      result = new String[6];
    else if (wmFlag) {
      result = new String[2 + 2 * consumptionModel[0].length];
    }
    else
      result = new String[4];
    result[0] = name;
    result[1] = activity;
    // result[2] = Integer.toString(numberOfMatchingPoints / 2);
    result[2] = Double.toString(getMeanActive());
    result[3] = Double.toString(getMeanReactive());

    if (refFlag) {
      int duration = (int) (0.5 + getMeanDuration());
      result[4] = Integer.toString(duration);
      result[5] = Integer.toString((int) distance);
    }
    else if (wmFlag) {

      for (int i = 0; i < consumptionModel[0].length; i++) {
        result[2 + 2 * i] = Double.toString(consumptionModel[0][i]);
        result[3 + 2 * i] = Double.toString(consumptionModel[1][i]);
      }

    }

    return result;
  }

  /**
   * This is an auxiliary function used to check if the distance in time and
   * space of a pair is close to this appliance, meaning that it belongs to this
   * appliance.
   * 
   * @param mean
   *          The mean active and reactive power measurements.
   * @param duration
   *          The duration of the end-use.
   * @return true if it is close, false otherwise.
   */
  public boolean isClose (double[] mean, int duration)
  {

    double[] meanValues = { getMeanActive(), getMeanReactive() };

    if (activity.equalsIgnoreCase("Refrigeration"))
      return (Utils.percentageEuclideanDistance(mean, meanValues) < Constants.PERCENTAGE_CLOSENESS_THRESHOLD || Utils
              .absoluteEuclideanDistance(mean, meanValues) < Constants.ABSOLUTE_CLOSENESS_THRESHOLD)
             && Utils.checkLimitFridge(duration, getMeanDuration());
    else
      return (Utils.percentageEuclideanDistance(mean, meanValues) < Constants.PERCENTAGE_CLOSENESS_THRESHOLD || Utils
              .absoluteEuclideanDistance(mean, meanValues) < Constants.ABSOLUTE_CLOSENESS_THRESHOLD);

  }

  /**
   * This is an auxiliary function used to check if the distance in time and
   * space of a pair is close to this appliance, meaning that it belongs to this
   * appliance.
   * 
   * @param mean
   *          The mean active and reactive power measurements.
   * @param duration
   *          The duration of the end-use.
   * @return true if it is close, false otherwise.
   */
  public boolean isCloseClustered (double[] mean, int duration)
  {

    double[] meanValues = { getMeanActive(), getMeanReactive() };

    if (activity.equalsIgnoreCase("Refrigeration"))
      return (Utils.percentageEuclideanDistance(mean, meanValues) < Constants.CLUSTERED_PERCENTAGE_CLOSENESS_THRESHOLD || Utils
              .absoluteEuclideanDistance(mean, meanValues) < Constants.ABSOLUTE_CLOSENESS_THRESHOLD)
             && Utils.checkLimitFridge(duration, getMeanDuration());
    else
      return (Utils.percentageEuclideanDistance(mean, meanValues) < Constants.CLUSTERED_PERCENTAGE_CLOSENESS_THRESHOLD || Utils
              .absoluteEuclideanDistance(mean, meanValues) < Constants.ABSOLUTE_CLOSENESS_THRESHOLD);

  }

  /**
   * This function is used to present the basic information of the Appliance
   * Model on the console.
   */
  public void status ()
  {
    log.info("");
    log.info("Name:" + name);
    log.info("Activity:" + activity);

    Set<Integer> keys = new TreeSet<Integer>();
    keys.addAll(risingPoints.keySet());
    keys.addAll(reductionPoints.keySet());
    keys.addAll(matchingPoints.keySet());

    for (Integer key: keys) {
      log.info("Event: " + key);
      if (risingPoints.containsKey(key))
        log.info("Rising Points: " + risingPoints.get(key).toString());
      if (reductionPoints.containsKey(key))
        log.info("Reduction Points: " + reductionPoints.get(key).toString());
      if (matchingPoints.containsKey(key)) {
        log.info("Matching Points: ");
        Collections.sort(matchingPoints.get(key), Constants.comp2);
        for (PointOfInterest[] pois: matchingPoints.get(key))
          log.info(Arrays.toString(pois));
      }
    }
    if (activity.equalsIgnoreCase("Refrigeration"))
      log.info("Mean Duration: " + getMeanDuration());
    log.info("Mean Power: " + getMeanActive());
    log.info("Mean Reactive: " + getMeanReactive());

    if (consumptionModel != null) {
      log.info("Active Power: " + Arrays.toString(consumptionModel[0]));
      log.info("Reactive Power: " + Arrays.toString(consumptionModel[1]));
      log.info("TimeStamp: " + Arrays.toString(timeStamp));
    }
  }

  public boolean isSwitchedOn (Event event, int start, int end)
  {
    boolean result = false;
    int offset = -1, poiStart = -1, poiEnd = -1;
    ArrayList<PointOfInterest[]> tempMatching =
      matchingPoints.get(event.getId());

    // System.out.println("Matching Points: " + tempMatching);
    if (tempMatching != null && tempMatching.size() > 0) {
      for (PointOfInterest[] pois: tempMatching) {

        offset = event.getStartMinute();
        poiStart = pois[0].getMinute() + offset;
        poiEnd = pois[1].getMinute() + offset;

        result = result || (start >= poiStart && start <= poiEnd);

        result = result || (end >= poiStart && end <= poiEnd);

        if (result) {
          log.info("Pois: " + poiStart + " " + poiEnd);
          log.info("Start: " + start + " End:" + end);
          log.info("Switched On!");
          break;
        }

      }
    }
    return result;

  }

  public void setName (String name)
  {
    this.name = name;
  }

  public void setActivity (String activity)
  {
    this.activity = activity;
  }

  public void setTimeStamp (int[] timeStamp)
  {
    this.timeStamp = timeStamp;
  }

  public void setConsumption (double[][] consumption)
  {
    consumptionModel = consumption;
  }

  public void estimateDistance (ArrayList<Event> events, boolean median)
  {

    ArrayList<int[]> temp = new ArrayList<int[]>();
    ArrayList<Double> distanceList = new ArrayList<Double>();
    int offset = 0, start = -1, end = -1;
    double tempDistance = 0;

    for (Integer key: matchingPoints.keySet()) {

      offset = events.get(key - 1).getStartMinute();

      for (PointOfInterest[] pois: matchingPoints.get(key)) {
        start = offset + pois[0].getMinute();
        end = offset + pois[1].getMinute();
        if (start < end) {
          int[] timeStamp = { start, end };
          temp.add(timeStamp);
        }
      }
    }

    // for (int[] timeStamp: temp)
    // System.out.print(Arrays.toString(timeStamp) + " ");
    // System.out.println();

    for (int i = 0; i < temp.size() - 1; i++) {

      tempDistance = temp.get(i + 1)[0] - temp.get(i)[1];

      log.info("Previous end: " + temp.get(i)[1] + " Next Start: "
               + temp.get(i + 1)[0] + " Distance: " + tempDistance);

      distanceList.add(tempDistance);

    }

    log.info(toString());

    if (distanceList.size() > 0) {
      // System.out.println(distanceList.toString());
      if (median)
        distance = Utils.estimateMedian(distanceList);
      else
        distance = Utils.estimateMean(distanceList);
    }

    // System.out.println("Distance:" + distance);
  }

  @Override
  public String toString ()
  {
    return name;
  }

  public int operationTimes ()
  {

    // int result = 0;
    //
    // for (Integer index: matchingPoints.keySet()) {
    // result += matchingPoints.get(index).size();
    // }
    //
    // return result;

    return numberOfMatchingPoints;
  }
}
