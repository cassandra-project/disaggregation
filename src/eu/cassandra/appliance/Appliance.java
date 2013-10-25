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
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

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
  public Appliance (String name, String activity, double p, double q)
  {
    this.name = name;
    this.activity = activity;
    meanValuesSum[0] = p * Constants.USER_HEADSTART;
    meanValuesSum[1] = q * Constants.USER_HEADSTART;
    numberOfMatchingPoints = Constants.USER_HEADSTART;
  }

  /**
   * This function is used as a getter for the matching points of the appliance.
   * 
   * @return the map of the matching points of the appliance.
   */
  public Map<Integer, ArrayList<PointOfInterest[]>> getMatchingPoints ()
  {
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
    return 2 * durationSum / numberOfMatchingPoints;
  }

  /**
   * This function is used as a getter for mean value of the active and the
   * reactive power measurements.
   * 
   * @return the mean active and reactive power.
   */
  double[] getMeanValues ()
  {
    double[] temp = { getMeanActive(), getMeanReactive() };
    return temp;
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

    for (Integer key: matchingPoints.keySet()) {

      offset = events.get(key - 1).getStartMinute();

      for (PointOfInterest[] pois: matchingPoints.get(key)) {
        start = offset + pois[0].getMinute();
        end = offset + pois[1].getMinute();

        // if (start > end) {
        // System.out.println("Event: " + events.get(key - 1).getId());
        // System.out.println("Appliance: " + name);
        // System.out.println("Start: " + start);
        // System.out.println("End: " + end);
        // System.out.println();
        // }

        String[] tempString =
          { name, activity, Integer.toString(start), Integer.toString(end),
           Double.toString(pois[0].getPDiff()),
           Double.toString(pois[0].getQDiff()),
           Double.toString(pois[1].getPDiff()),
           Double.toString(pois[1].getQDiff()) };
        result.add(tempString);
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

    String[] result = new String[4];
    result[0] = name;
    result[1] = activity;
    // result[2] = Integer.toString(numberOfMatchingPoints / 2);
    result[2] = Double.toString(getMeanActive());
    result[3] = Double.toString(getMeanReactive());

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
              .absoluteEuclideanDistance(mean, meanValues) < Constants.ABSOLUTE_CLOSENESS_THRESHOLD
                                                                                                                && Utils.checkLimitFridge(duration,
                                                                                                                                          getMeanDuration()));
    else
      return (Utils.percentageEuclideanDistance(mean, meanValues) < Constants.PERCENTAGE_CLOSENESS_THRESHOLD || Utils
              .absoluteEuclideanDistance(mean, meanValues) < Constants.ABSOLUTE_CLOSENESS_THRESHOLD);

  }

  /**
   * This function is used to present the basic information of the Appliance
   * Model on the console.
   */
  public void status ()
  {
    System.out.println("Name:" + name);

    Set<Integer> keys = new TreeSet<Integer>();
    keys.addAll(risingPoints.keySet());
    keys.addAll(reductionPoints.keySet());
    keys.addAll(matchingPoints.keySet());

    for (Integer key: keys) {
      System.out.println("Event: " + key);
      if (risingPoints.containsKey(key))
        System.out
                .println("Rising Points: " + risingPoints.get(key).toString());
      if (reductionPoints.containsKey(key))
        System.out.println("Reduction Points: "
                           + reductionPoints.get(key).toString());
      if (matchingPoints.containsKey(key)) {
        System.out.print("Matching Points: ");
        for (PointOfInterest[] pois: matchingPoints.get(key))
          System.out.println(Arrays.toString(pois));
      }
    }
    if (activity.equalsIgnoreCase("Refrigeration"))
      System.out.println("Mean Duration: " + getMeanDuration());
    System.out.println("Mean Power: " + getMeanActive());
    System.out.println("Mean Reactive: " + getMeanReactive());
  }
}
