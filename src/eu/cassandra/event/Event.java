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

package eu.cassandra.event;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import eu.cassandra.utils.Constants;
import eu.cassandra.utils.PointOfInterest;
import eu.cassandra.utils.Utils;

/**
 * This is the main class that is used for capturing the notion of an
 * consumption event. As Consumption Event is considered a time period in the
 * consumption data set that the active and reactive activeOnly measurements are
 * high enough to imply the end use of an electrical appliance.
 * 
 * @author Antonios Chrysopoulos
 * @version 0.7, Date: 29.07.2013
 */
public class Event
{

  /**
   * This variable is a unique id number in order to separate the events.
   */
  private int id;

  /**
   * This variable is the start minute of the event in the summary of all
   * minutes of the data set.
   */
  private int startMinute = -1;

  /**
   * This variable is the end minute of the event in the summary of all
   * minutes of the data set.
   */
  private int endMinute = -1;

  /**
   * This variable is an array containing the active power measurements for the
   * duration of the event.
   */
  private double[] activePowerConsumptions = new double[0];

  /**
   * This variable is an array containing the reactive power measurements for
   * the duration of the event.
   */
  private double[] reactivePowerConsumptions = new double[0];

  /**
   * This variable is an array containing the derivative between the active
   * power measurements of one minute to the next (a[i+1] - a[i] / a[i]).
   */
  private double[] derivative;

  /**
   * This variable is an array of the derivative sign. It is 1 if the derivative
   * is rising, 0 if it is steady and -1 if it is declining.
   */
  private int[] marker;

  /**
   * This variable states the presence of a washing machine in the event.
   */
  private boolean wmFlag = false;

  /**
   * This variable is the threshold which states which points can be ommited
   * from the event without changing significantly the analysis and the result
   * of the disaggregation.
   */
  private double threshold = 0;

  /**
   * This variable is an list of the detected rising points (points that the
   * active power is increasing).
   */
  private ArrayList<PointOfInterest> risingPoints =
    new ArrayList<PointOfInterest>();

  /**
   * This variable is an list of the detected reduction points (points that the
   * active power is decreasing).
   */
  private ArrayList<PointOfInterest> reductionPoints =
    new ArrayList<PointOfInterest>();

  /**
   * This variable is an list of pairs of points of interest that mark an
   * complex consumption event of an appliance.
   */
  private ArrayList<PointOfInterest[]> clusters =
    new ArrayList<PointOfInterest[]>();

  /**
   * This variable is an list of pairs of points of interest that mark an
   * switching event of an appliance.
   */
  private ArrayList<PointOfInterest[]> switchingPoints =
    new ArrayList<PointOfInterest[]>();

  /**
   * This variable is an list of pairs of points of interest that mark an
   * definite matching event of an appliance.
   */
  private ArrayList<PointOfInterest[]> matchingPoints =
    new ArrayList<PointOfInterest[]>();

  /**
   * This variable is an list of pairs of points of interest that mark an
   * a chairing consumption event of an appliance (rising - reduction -
   * reduction).
   */
  private ArrayList<PointOfInterest[]> chairs =
    new ArrayList<PointOfInterest[]>();

  /**
   * This variable is an list of pairs of points of interest that mark an
   * an inverse chairing consumption event of an appliance (rising - rising -
   * reduction).
   */
  private ArrayList<PointOfInterest[]> inversedChairs =
    new ArrayList<PointOfInterest[]>();

  /**
   * This variable is an list of pairs of points of interest that mark an
   * a triangle consumption event of an appliance (rising - reduction in a
   * single minute).
   */
  private ArrayList<PointOfInterest[]> triangles =
    new ArrayList<PointOfInterest[]>();

  /**
   * This variable is an list of pairs of points of interest that mark an
   * a triangle consumption event of an appliance (rising - reduction for a time
   * interval larger than minute).
   */
  private ArrayList<PointOfInterest[]> rectangles =
    new ArrayList<PointOfInterest[]>();

  /**
   * This variable is an list of pairs of points of interest that mark an
   * the final pairs of appliance switching on / off that are included in this
   * event appliance.
   */
  private ArrayList<PointOfInterest[]> finalPairs =
    new ArrayList<PointOfInterest[]>();

  /**
   * A constructor of an event in the measurements used in case we know most of
   * the input variables.
   * 
   * @param start
   *          The start minute of the event.
   * @param end
   *          The end minute of the event.
   * @param active
   *          The array of active power measurements during the event interval.
   * @param reactive
   *          The array of reactive power measurements during the event
   *          interval.
   */
  public Event (int start, int end, double[] active, double[] reactive)
  {
    // Setting the variables in their respective values.
    id = Constants.EVENTS_ID++;
    startMinute = start;
    endMinute = end;
    activePowerConsumptions = active;
    reactivePowerConsumptions = reactive;

    // Normalize the values in the active and reactive power measurements of the
    // event.
    normalizeConsumptions();

    // Analyse the measurements and find the points of interest.
    findPointsOfInterest();

    // Sorting the rising and reduction points detected.
    Collections.sort(risingPoints, Constants.comp);
    Collections.sort(reductionPoints, Constants.comp);

    // Clear the points of interest that are not important to the event.
    cleanPointsOfInterest();

    // cleanPointsOfInterest(Constants.MATCHING_THRESHOLD);
    // status();
  }

  /**
   * This function is used for clearing the variables that are no longer used
   * for memory optimization.
   */
  public void clear ()
  {
    activePowerConsumptions = null;
    reactivePowerConsumptions = null;
    derivative = null;
    marker = null;
    risingPoints.clear();
    reductionPoints.clear();
    switchingPoints.clear();
    finalPairs.clear();
  }

  /**
   * This function is used as a getter for the id of the event.
   * 
   * @return event's id.
   */
  public int getId ()
  {
    return id;
  }

  /**
   * This function is used as a getter for the start minute of the event.
   * 
   * @return event's start minute.
   */
  public int getStartMinute ()
  {
    return startMinute;
  }

  /**
   * This function is used as a getter for the end minute of the event.
   * 
   * @return event's end minute.
   */
  public int getEndMinute ()
  {
    return endMinute;
  }

  /**
   * This function is used as a getter for the array of active power
   * measurements of the event.
   * 
   * @return event's active power measurements.
   */
  public double[] getActivePowerConsumptions ()
  {
    return activePowerConsumptions;
  }

  /**
   * This function is used as a getter for the array of reactive power
   * measurements of the event.
   * 
   * @return event's reactive power measurements.
   */
  public double[] getReactivePowerConsumptions ()
  {
    return reactivePowerConsumptions;
  }

  /**
   * This function is used as a getter for the list of rising points of the
   * event.
   * 
   * @return event's rising points of interest.
   */
  public ArrayList<PointOfInterest> getRisingPoints ()
  {
    return risingPoints;
  }

  /**
   * This function is used as a getter for the list of reduction points of the
   * event.
   * 
   * @return event's reduction points of interest.
   */
  public ArrayList<PointOfInterest> getReductionPoints ()
  {
    return reductionPoints;
  }

  /**
   * This function is used as a getter for the list of switching pairs of the
   * event.
   * 
   * @return event's switching point of interest pairs.
   */
  public ArrayList<PointOfInterest[]> getSwitchingPoints ()
  {
    return switchingPoints;
  }

  /**
   * This function is used as a getter for the list of matching pairs of the
   * event.
   * 
   * @return event's matching point of interest pairs.
   */

  public ArrayList<PointOfInterest[]> getMatchingPoints ()
  {
    return matchingPoints;
  }

  /**
   * This function is used as a getter for the list of final pairs of the
   * event.
   * 
   * @return event's final point of interest pairs.
   */
  public ArrayList<PointOfInterest[]> getFinalPairs ()
  {
    return finalPairs;
  }

  /**
   * This function is used as a getter for a single final pairs of the
   * event.
   * 
   * @param index
   *          The index of the final pair in the list.
   * @return event's single point of interest pair.
   */
  public PointOfInterest[] getFinalPairs (int index)
  {
    return finalPairs.get(index);
  }

  /**
   * This function is used as a getter for the washing machine flag of the
   * event.
   * 
   * @return event's washing machine flag.
   */
  public boolean getWashingMachineFlag ()
  {
    return wmFlag;
  }

  /**
   * This function is used for setting as true the washing machine flag of the
   * event.
   * 
   */
  public void setWashingMachineFlag ()
  {
    wmFlag = true;
  }

  /**
   * This auxiliary function is used to estimate the mean values of the
   * differences appearing in the rising and reduction points of interest.
   * 
   * @return the mean value of the active and reactive power differences of the
   *         points of interest.
   */
  public double[] getMeanValues ()
  {

    double[] meanValues = new double[2];

    for (int i = 0; i < risingPoints.size(); i++) {
      meanValues[0] += Math.abs(risingPoints.get(i).getPDiff());
      meanValues[1] += Math.abs(risingPoints.get(i).getQDiff());
    }

    for (int i = 0; i < reductionPoints.size(); i++) {
      meanValues[0] += Math.abs(reductionPoints.get(i).getPDiff());
      meanValues[1] += Math.abs(reductionPoints.get(i).getQDiff());
    }

    meanValues[0] /= (risingPoints.size() + reductionPoints.size());
    meanValues[1] /= (risingPoints.size() + reductionPoints.size());
    return meanValues;
  }

  /**
   * This auxiliary function is used for subtracting the first value of the
   * active and reactive measurements from the rest of the power arrays in order
   * to make the analysis easier.
   */
  private void normalizeConsumptions ()
  {

    double active = activePowerConsumptions[0];
    double reactive = reactivePowerConsumptions[0];

    for (int i = 0; i < activePowerConsumptions.length; i++) {
      if (i != activePowerConsumptions.length - 1) {
        activePowerConsumptions[i] -= active;
        reactivePowerConsumptions[i] -= reactive;
      }
      else {
        activePowerConsumptions[i] = 0;
        reactivePowerConsumptions[i] = 0;
      }
    }

  }

  /**
   * This is the point of interest detection function of the Disaggregation
   * Module. It uses the derivative of the prices and the signs to find
   * increases and decreases in active power and then seperate the points of
   * interest accordingly.
   */
  private void findPointsOfInterest ()
  {

    // Initialiaze the auxiliary variables.
    derivative = new double[activePowerConsumptions.length - 1];
    marker = new int[activePowerConsumptions.length - 1];
    ArrayList<Integer> temp = new ArrayList<Integer>();

    // variable for individual points.
    ArrayList<Integer> ind = new ArrayList<Integer>();
    // variable for group of points.
    ArrayList<ArrayList<Integer>> groups = new ArrayList<ArrayList<Integer>>();

    // for each index of the derivative array
    for (int i = 0; i < derivative.length; i++) {

      // if both this and next values of the active power array are 0
      if (activePowerConsumptions[i + 1] == 0
          && activePowerConsumptions[i] == 0)
        derivative[i] = 0;
      // give the derivative value to the index point.
      else
        derivative[i] =
          100 * ((activePowerConsumptions[i + 1] - activePowerConsumptions[i]) / activePowerConsumptions[i]);

      // Add the sign of the derivative to the marker array
      if (derivative[i] > Constants.DERIVATIVE_LIMIT)
        marker[i] = 1;
      else if (derivative[i] < -Constants.DERIVATIVE_LIMIT)
        marker[i] = -1;
    }

    System.out.println("Event " + id);
    System.out.println(Arrays.toString(derivative));
    System.out.println(Arrays.toString(marker));

    // New marker array with 0 in the first and last index
    int[] markerNew = new int[marker.length + 2];
    for (int i = 1; i < markerNew.length - 1; i++)
      markerNew[i] = marker[i - 1];

    System.out.println(Arrays.toString(markerNew));

    // Parsing through the new marker array we find the points of interest
    // either as individual points or as group of points.
    for (int i = 1; i < markerNew.length - 1; i++) {

      if (markerNew[i] != 0) {

        // If one single point
        if (markerNew[i - 1] == 0 && markerNew[i + 1] == 0)
          ind.add(i - 1);

        // else it is a group of points
        else {

          temp = new ArrayList<Integer>();

          while (markerNew[i] != 0) {
            temp.add(i - 1);
            i++;
          }
          groups.add(temp);
        }

      }

    }

    System.out.println("Individuals:" + ind.toString());
    System.out.println("Groups: " + groups.toString());

    // For the individual points
    if (ind.size() > 0)
      createIndividualPoints(ind);

    // For group of points
    if (groups.size() > 0)
      analyseGroups(groups);

  }

  /**
   * This function is capable of seperating the individual point of interest in
   * order to call the next function which will create them and add them to the
   * respective list.
   * 
   * @param ind
   *          The list of found individual points.
   */
  private void createIndividualPoints (ArrayList<Integer> ind)
  {
    // Initialize the auxiliary variables
    int index = -1;
    ArrayList<Integer> rising = new ArrayList<Integer>();
    ArrayList<Integer> reduction = new ArrayList<Integer>();

    // For each point found put it in the correct list of points by judging the
    // sign.
    for (int i = 0; i < ind.size(); i++) {

      index = ind.get(i);

      if (marker[index] > 0)
        rising.add(index);
      else
        reduction.add(index);
    }

    // System.out.println("Individual Rising Points:" + rising.toString());
    // System.out.println("Individual Reduction Points:" +
    // reduction.toString());

    // For each rising point
    for (Integer rise: rising)
      singleRisingPoint(rise);

    // For each reduction point
    for (Integer red: reduction)
      singleReductionPoint(red);

  }

  /**
   * This function is responsible for the creation the rising points and their
   * addition to the rising points list. The procedure is to find the difference
   * in active and reactive power before and after taking into consideration the
   * values in the proximity points and not only the current point.
   * 
   * @param rise
   *          The index of the point in time
   */
  private void singleRisingPoint (int rise)
  {
    // Initializing the auxiliary variables
    int lastReductionPoint = -1;
    int lastRisingPoint = -1;

    // The farthest point taken into consideration in the past
    int minIndex = Math.max(0, rise - Constants.REDUCTION_LIMIT);
    // The farthest point taken into consideration in the future
    int maxIndex =
      Math.min(derivative.length - 1, rise + Constants.RISING_LIMIT);

    // System.out.println("Min Index: " + minIndex + " MaxIndex: " + maxIndex);

    // Finding the first point that the reduction starts
    lastReductionPoint = rise;
    lastRisingPoint = rise + 1;

    for (int i = minIndex; i < rise; i++) {

      if (derivative[i] < 0 && marker[i] == 0)
        lastReductionPoint = i;
      else if (derivative[i] > 0 || marker[i] != 0)
        lastReductionPoint = rise;
    }

    // Finding the last point that the rising ends
    for (int i = maxIndex; i > rise; i--) {
      if (derivative[i] > 0 && marker[i] == 0)
        lastRisingPoint = i;
      else if (derivative[i] < 0 || marker[i] != 0)
        lastRisingPoint = rise + 1;
    }

    // System.out.println("Index: " + rise + " Reduction Point: "
    // + lastReductionPoint + " Rising Point: "
    // + lastRisingPoint);

    // Estimation of the active and reactive power difference and creation of
    // the point.
    double pdiff =
      activePowerConsumptions[lastRisingPoint]
              - activePowerConsumptions[lastReductionPoint];
    double qdiff =
      reactivePowerConsumptions[lastRisingPoint]
              - reactivePowerConsumptions[lastReductionPoint];

    risingPoints.add(new PointOfInterest(rise, true, pdiff, qdiff));
  }

  /**
   * This function is responsible for the creation the reduction points and
   * their addition to the reduction points list. The procedure is to find the
   * difference in active and reactive power before and after taking into
   * consideration the values in the proximity points and not only the current
   * point.
   * 
   * @param red
   *          The index of the point in time
   */
  private void singleReductionPoint (int red)
  {
    // Initializing the auxiliary variables
    int lastReductionPoint = -1;
    int lastRisingPoint = -1;

    // The farthest point taken into consideration in the past
    int minIndex = Math.max(0, red - Constants.RISING_LIMIT);
    // The farthest point taken into consideration in the future
    int maxIndex =
      Math.min(derivative.length - 1, red + Constants.REDUCTION_LIMIT);

    // Finding the first point that the rising ends
    lastRisingPoint = red;
    lastReductionPoint = red + 1;

    for (int i = minIndex; i < red; i++) {

      if (derivative[i] > 0 && marker[i] == 0)
        lastRisingPoint = i;
      else if (derivative[i] < 0 || marker[i] != 0)
        lastRisingPoint = red;
    }

    // Finding the first point that the reduction ends
    for (int i = red + 1; i < maxIndex; i++) {
      if (derivative[i] < 0 && marker[i] == 0)
        lastReductionPoint = i;
      else if (derivative[i] > 0 || marker[i] != 0)
        lastReductionPoint = red + 1;
    }

    // System.out.println("Index: " + red + " Rising Point: " + lastRisingPoint
    // + " Reduction Point: " + lastReductionPoint);

    // Estimation of the active and reactive power difference and creation of
    // the point.
    double pdiff =
      activePowerConsumptions[lastReductionPoint]
              - activePowerConsumptions[lastRisingPoint];
    double qdiff =
      reactivePowerConsumptions[lastReductionPoint]
              - reactivePowerConsumptions[lastRisingPoint];

    reductionPoints.add(new PointOfInterest(red, false, pdiff, qdiff));

  }

  /**
   * This function is responsible for the analysis of the group of points and
   * their split in smaller groups or individual points.
   * 
   * @param groups
   *          The groups of points for analysis
   */
  private void analyseGroups (ArrayList<ArrayList<Integer>> groups)
  {
    // Initializing the auxiliary variables
    int sum = 0;

    // For each group of points
    for (ArrayList<Integer> group: groups) {

      sum = 0;

      // System.out.println(group.toString());

      // Estimating the sum of the point signs
      for (Integer index: group)
        sum += marker[index];

      // If all rising
      if (sum == group.size()) {
        // System.out.println("All Rising");
        allRisingAnalysis(group);
      }
      // If all reduction
      else if (sum == -group.size()) {
        // System.out.println("All Reduction");
        allReductionAnalysis(group);
      }
      // Else they are mixed
      else {
        // System.out.println("Mixed");
        mixedAnalysis(group);
      }
    }

  }

  /**
   * This function analyses and separates the points from a group of points that
   * are all rising.
   * 
   * @param group
   *          The group of points for analysis
   */
  private void allRisingAnalysis (ArrayList<Integer> group)
  {
    // Initializing the auxiliary variables
    int lastReductionPoint = -1;
    int lastRisingPoint = -1;

    // In case of a single point that is single, we take the last one of the
    // group.
    if (group.size() % 2 == 1) {
      singleRisingPoint(group.get(group.size() - 1));
    }

    // Taking the points in groups of two. Then we look at the last reduction
    // point from before and take the pair as a single point more specifically
    // the first of the two for each pair.
    for (int i = group.get(0); i < group.get(group.size() - 1); i = i + 2) {

      lastReductionPoint = i;
      lastRisingPoint = i + 2;

      if (i == group.get(0)) {
        int minIndex = Math.max(0, i - Constants.REDUCTION_LIMIT);

        for (int j = minIndex; j < i; j++) {

          if (derivative[j] < 0 && marker[j] == 0)
            lastReductionPoint = j;
          else if (derivative[j] > 0 || marker[j] != 0)
            lastReductionPoint = i;
        }
      }

      // Estimation of the active and reactive power difference and creation of
      // the point.
      double pdiff =
        activePowerConsumptions[lastRisingPoint]
                - activePowerConsumptions[lastReductionPoint];
      double qdiff =
        reactivePowerConsumptions[lastRisingPoint]
                - reactivePowerConsumptions[lastReductionPoint];

      risingPoints.add(new PointOfInterest(i, true, pdiff, qdiff));

    }

  }

  /**
   * This function analyses and separates the points from a group of points that
   * are all reduction.
   * 
   * @param group
   *          The group of points for analysis
   */
  private void allReductionAnalysis (ArrayList<Integer> group)
  {
    // Initializing the auxiliary variables
    int lastReductionPoint = -1;
    int lastRisingPoint = -1;

    // In case of a single point that is single, we take the last one of the
    // group.
    if (group.size() % 2 == 1) {
      singleReductionPoint(group.get(group.size() - 1));
    }

    // Taking the points in groups of two. Then we look at the last rising
    // point from before and take the pair as a single point more specifically
    // the second of the two for each pair.
    for (int i = group.get(0); i < group.get(group.size() - 1); i = i + 2) {

      lastRisingPoint = i;
      lastReductionPoint = i + 2;

      if (i == group.get(0)) {
        int minIndex = Math.max(0, i - Constants.REDUCTION_LIMIT);

        for (int j = minIndex; j < i; j++) {

          if (derivative[j] > 0 && marker[j] == 0)
            lastRisingPoint = j;
          else if (derivative[j] < 0 || marker[j] != 0)
            lastRisingPoint = i;
        }

      }

      // Estimation of the active and reactive power difference and creation of
      // the point.
      double pdiff =
        activePowerConsumptions[lastReductionPoint]
                - activePowerConsumptions[lastRisingPoint];
      double qdiff =
        reactivePowerConsumptions[lastReductionPoint]
                - reactivePowerConsumptions[lastRisingPoint];

      reductionPoints.add(new PointOfInterest(i + 1, false, pdiff, qdiff));

    }

  }

  /**
   * This function analyses and separates the points from a group of points that
   * mixed to group of points with the same sign or individual points.
   * 
   * @param group
   *          The group of points for analysis
   */
  private void mixedAnalysis (ArrayList<Integer> group)
  {
    // Initializing the auxiliary variables
    ArrayList<ArrayList<Integer>> rise = new ArrayList<ArrayList<Integer>>();
    ArrayList<ArrayList<Integer>> reduce = new ArrayList<ArrayList<Integer>>();
    ArrayList<Integer> temp = new ArrayList<Integer>();
    int index = -1;

    // Each point in the group is compared with the previous and next points and
    // separated accordingly to single points or group of same type of points.
    for (int i = 0; i < group.size(); i++) {

      index = group.get(i);

      // if the temporary group is empty or current index has the same sign with
      // the previous one
      if (temp.size() == 0 || marker[index - 1] == marker[index])
        temp.add(index);
      // else close the temp group and add it to the rising or reduction
      // respectively, then create a new one and add current there
      else {
        if (marker[index - 1] == 1)
          rise.add(temp);
        else
          reduce.add(temp);

        temp = new ArrayList<Integer>();
        temp.add(index);
      }

    }

    // Adding the last temp group to the correct array
    if (marker[temp.get(temp.size() - 1)] == 1)
      rise.add(temp);
    else
      reduce.add(temp);

    // System.out.println("Rise: " + rise.toString());
    // System.out.println("Reduce: " + reduce.toString());

    // For each rising group of points, call the function for individual points
    // or group of points.
    for (ArrayList<Integer> rising: rise) {

      if (rising.size() == 1)
        singleRisingPoint(rising.get(0));
      else
        allRisingAnalysis(rising);

    }

    // For each reduction group of points, call the function for individual
    // points
    // or group of points.
    for (ArrayList<Integer> reduction: reduce) {

      if (reduction.size() == 1)
        singleReductionPoint(reduction.get(0));
      else
        allReductionAnalysis(reduction);

    }
  }

  /**
   * This function is used in order to remove some of the points of interest,
   * making the later analysis less complex.
   * 
   * @param threshold
   *          The threshold that the user entered as limit for the points of
   *          interest. May be an array of size of 0 or 1
   */
  private void cleanPointsOfInterest (Double... threshold)
  {
    // If there is a threshold provided by user take it
    if (threshold.length == 1) {
      this.threshold = threshold[0];
    }
    // else estimate the threshold automatically
    else
      this.threshold = thresholdTuning();

    // System.out.println("Threshold: " + result);

    // For all rising points remove those with active power change under the
    // threshold
    for (int i = risingPoints.size() - 1; i >= 0; i--)
      if (Math.abs(risingPoints.get(i).getPDiff()) < this.threshold) {
        // System.out.println(Math.abs(risingPoints.get(i).getPDiff()) + "<"
        // + result);
        risingPoints.remove(i);
      }

    // For all reduction points remove those with active power change under the
    // threshold
    for (int i = reductionPoints.size() - 1; i >= 0; i--)
      if (Math.abs(reductionPoints.get(i).getPDiff()) < this.threshold) {
        // System.out.println(Math.abs(reductionPoints.get(i).getPDiff()) + "<"
        // + result);
        reductionPoints.remove(i);
      }
  }

  /**
   * This function is used for the automatic estimation of an threshold for
   * point of interest removal. This is a pretty complex procedure that does not
   * guarantee the best of result but makes the search space for the later
   * procedures smaller thus our calculations faster.
   * 
   * @return the threshold estimated for the event by this automatic procedure.
   */
  private double thresholdTuning ()
  {
    // Initializing the auxiliary variables.
    double step = 0;
    ArrayList<Double> alterations = new ArrayList<Double>();
    ArrayList<Double> pdiffs = new ArrayList<Double>();
    threshold = Constants.DEFAULT_THRESHOLD;

    // Collecting all the active power differences from the points of interest.
    for (PointOfInterest poi: risingPoints)
      pdiffs.add(Math.abs(poi.getPDiff()));

    for (PointOfInterest poi: reductionPoints)
      pdiffs.add(Math.abs(poi.getPDiff()));

    // Sorting the differences
    Collections.sort(pdiffs);

    // Taking as a step for the procedure the half of the smaller difference
    step = pdiffs.get(0) / 2;

    // If the step is zero then an constant value is selected for threshold
    if (step == 0)
      return threshold;

    // Create an array of values for search space beginning from the smallest
    // and adding the step up to the largest value
    double temp = pdiffs.get(0);

    while (temp < pdiffs.get(pdiffs.size() - 1)) {
      alterations.add(temp);
      temp += step;
    }

    // System.out.println("Alterations: " + alterations.toString());

    // For each value in the created array we remove the points of interest that
    // are smaller and then see if the result is a closed system meaning the
    // active and reactive power are close enough at start and end and the
    // overall active power is not far from the starting summary.
    for (Double alter: alterations) {

      // System.out.println("For alteration " + alter);

      // Initialize the auxiliary variables
      // double sumOldRisingP = 0, sumOldReductionP = 0, devOldP = 0;
      double sumRisingP = 0, sumReductionP = 0, sumRisingQ = 0, sumReductionQ =
        0, devP = 0, devQ = 0;

      ArrayList<PointOfInterest> rising =
        new ArrayList<PointOfInterest>(risingPoints);
      ArrayList<PointOfInterest> reduction =
        new ArrayList<PointOfInterest>(reductionPoints);

      // Removing the smaller rising points of interest
      for (int i = rising.size() - 1; i >= 0; i--) {
        // sumOldRisingP += rising.get(i).getPDiff();
        if (Math.abs(rising.get(i).getPDiff()) < alter) {
          // System.out.println(Math.abs(risingPoints.get(i).getPDiff()) + "<"
          // + alter);
          rising.remove(i);
        }
      }

      // Removing the smaller reduction points of interest
      for (int i = reduction.size() - 1; i >= 0; i--) {
        // sumOldReductionP += reduction.get(i).getPDiff();
        if (Math.abs(reduction.get(i).getPDiff()) < alter) {
          // System.out.println(Math.abs(reductionPoints.get(i).getPDiff()) +
          // "<"
          // + alter);
          reduction.remove(i);
        }
      }

      // If at least one point of interest was removed
      if (rising.size() != risingPoints.size()
          || reduction.size() != reductionPoints.size()) {

        // System.out.println("Something removed");
        // System.out.println(risingPoints.toString());
        // System.out.println(rising.toString());
        // System.out.println(reductionPoints.toString());
        // System.out.println(reduction.toString());

        // Estimating the overall active and reactive power
        for (PointOfInterest rise: rising) {
          sumRisingP += rise.getPDiff();
          sumRisingQ += rise.getQDiff();
        }

        for (PointOfInterest red: reduction) {
          sumReductionP += red.getPDiff();
          sumReductionQ += red.getQDiff();
        }

        // System.out.println(sumRisingP + " " + sumRisingQ);
        // System.out.println(sumReductionP + " " + sumReductionQ);
        // System.out.println(sumOldRisingP + " " + sumOldReductionP);

        // Estimating the deviation for active and reactive power
        devP = Math.abs(100 * (sumRisingP + sumReductionP) / sumRisingP);
        devQ = Math.abs(100 * (sumRisingQ + sumReductionQ) / sumRisingQ);
        // devOldP =
        // Math.abs(100
        // * ((sumOldRisingP - sumOldReductionP) - (sumRisingP - sumReductionP))
        // / (sumOldRisingP - sumOldReductionP));

        // System.out.println("Alteration: " + alter + " DevP: " + devP
        // + " DevQ: " + devQ + " DevOldP: " + devOldP);

        // If they are in an acceptable rate
        if (devP < Constants.DIFFERENCE_LIMIT
            && devQ < Constants.DIFFERENCE_LIMIT) {
          // && devOldP < Constants.OLD_DIFFERENCE_LIMIT) {
          // System.out.println("Alteration: " + alter + " DevP: " + devP
          // + " DevQ: " + devQ + " DevOldP: " + devOldP);

          // Making a big collection of points of interest
          rising.addAll(reduction);
          Collections.sort(rising, Constants.comp);

          // Initialize the variables for the timeseries
          double sumOld = 0, sumNew = 0;
          double[] Pnew = curveReconstruction(rising);

          // Findind the summart of active power
          for (int i = 0; i < Pnew.length; i++) {
            sumOld += activePowerConsumptions[i];
            sumNew += Pnew[i];
          }

          // Estimating the distance
          double distance = 100 * (Math.abs(sumOld - sumNew)) / sumOld;

          // System.out.println("SumOld: " + sumOld + " SumNew: " + sumNew
          // + " Distance: " + distance);

          // If it is not over a threshold then this alteration is the new
          // threshold.
          if (distance < Constants.OLD_DIFFERENCE_LIMIT)
            threshold = alter;

        }
      }

    }

    return threshold;

  }

  /**
   * This is an auxiliary function used to reconstruct the active power curve of
   * the event without taking into consideration the points of interests that
   * are removed by applying the threshold.
   * 
   * @param pois
   *          The list of the available points of interest after the threshold
   *          application.
   * @return an array of active power measurements.
   */
  private double[] curveReconstruction (ArrayList<PointOfInterest> pois)
  {
    double[] result = new double[activePowerConsumptions.length];
    double p = 0;
    for (int i = 0; i < pois.size() - 1; i++) {
      p += pois.get(i).getPDiff();

      for (int j = pois.get(i).getMinute() + 1; j < pois.get(i + 1).getMinute(); j++)
        result[j] = p;
    }
    return result;
  }

  /**
   * This function is used for searching over the event for switching events.
   * Switching events are quick switching off and on again of an appliance that
   * are more of static disturbance than an actual end-use of an appliance and
   * should be removed from the data set.
   */
  public void detectSwitchingPoints ()
  {
    // Create an array of all the points of interest in chronological order.
    ArrayList<PointOfInterest> temp =
      new ArrayList<PointOfInterest>(risingPoints);
    temp.addAll(reductionPoints);
    Collections.sort(temp, Constants.comp);
    // double distance = 0;

    // For each point of interest
    for (int i = 0; i < temp.size() - 1; i++) {

      // Checking if there is a pair of reduction and rising points with
      // identical pattern that can be matched
      if (temp.get(i).getRising() == false && temp.get(i + 1).getRising()) {
        double[] tempValues =
          { -temp.get(i).getPDiff(), -temp.get(i).getQDiff() };
        // distance = temp.get(i + 1).percentageEuclideanDistance(tempValues);
        // System.out.println("Distance of " + temp.get(i).toString() + " and "
        // + temp.get(i + 1).toString());
        // System.out.println("Euclidean Distance: "
        // + temp.get(i + 1).euclideanDistance(tempValues)
        // + " Length: " + temp.get(i).euclideanLength()
        // + " = " + distance);

        // If the distance is close enough the points are stored in the
        // switching points list and removed from the event.
        if (temp.get(i + 1).percentageEuclideanDistance(tempValues) < Constants.SWITCHING_THRESHOLD) {

          // TODO another check to add temporal distance
          PointOfInterest[] tempPOI = { temp.get(i), temp.get(i + 1) };
          switchingPoints.add(tempPOI);
          risingPoints.remove(temp.get(i + 1));
          reductionPoints.remove(temp.get(i));

        }

      }

    }

  }

  /**
   * This function is used for searching over the event for matching events.
   * Matching events are pairs of switching on / off that are extremely close in
   * active and reactive power change and make certainly an appliance end-use
   * that must be added to the final pairs.
   */
  public void detectMatchingPoints ()
  {
    // Create an array of all the points of interest in chronological order.
    ArrayList<PointOfInterest> temp =
      new ArrayList<PointOfInterest>(risingPoints);
    temp.addAll(reductionPoints);
    Collections.sort(temp, Constants.comp);

    // Initializing auxiliary variables
    double distance = 0;
    double minDistance = Double.POSITIVE_INFINITY;
    int minIndex = -1;

    // Checking if there is a pair of rising and reduction points with
    // identical pattern that can be matched. If there are more than one in
    // match, the best match is chosen over the others
    for (int i = 0; i < temp.size(); i++) {

      // If this is a rising point
      if (temp.get(i).getRising()) {

        distance = 0;
        minDistance = Double.POSITIVE_INFINITY;
        minIndex = -1;

        // Checking the next reduction points for distance
        for (int j = i; j < temp.size(); j++) {

          if (temp.get(j).getRising() == false) {

            double[] tempValues =
              { -temp.get(j).getPDiff(), -temp.get(j).getQDiff() };

            // distance = temp.get(i).percentageEuclideanDistance(tempValues);
            // if (distance < 25)
            // System.out.println("Euclidean Distance: "
            // + temp.get(i).euclideanDistance(tempValues)
            // + " Length: " + temp.get(i).euclideanLength()
            // + " = " + distance);
            //
            // System.out.println("Distance of " + temp.get(i).toString()
            // + " and " + temp.get(j).toString() + "="
            // + distance);

            if (temp.get(i).percentageEuclideanDistance(tempValues) < Constants.CLOSENESS_THRESHOLD
                && distance < minDistance) {
              minIndex = j;
              minDistance = distance;
            }
          }

        }

        // If a combination that was close is found a matching pair is created
        // and added to the matching points array.
        if (minIndex != -1) {
          PointOfInterest[] tempPOI = { temp.get(i), temp.get(minIndex) };
          matchingPoints.add(tempPOI);
        }
      }

    }

    // The newly found matching points are removed from their respected arrays.
    if (matchingPoints.size() > 0)
      for (PointOfInterest[] pois: matchingPoints) {
        risingPoints.remove(pois[0]);
        reductionPoints.remove(pois[1]);
      }

  }

  /**
   * This function is used for searching over the event for clusters of points.
   * A cluster of points is defined as a large amount of points of interest
   * concentrated to a small time interval having large rising and reduction
   * points in the start and the end of the interval and many smaller changes
   * within. This signifies the end-use of an appliance with a complex
   * consumption model and is stored as one, and the points contained within are
   * removed from the arrays.
   */
  public void detectClusters ()
  {
    // Making a large collection of points of interest
    ArrayList<PointOfInterest> temp =
      new ArrayList<PointOfInterest>(risingPoints);

    temp.addAll(reductionPoints);

    // If there are more than 3 points of interest
    if (temp.size() > 3) {

      // Sort points of interest chronologically
      Collections.sort(temp, Constants.comp);

      // Initializing the auxiliary variables
      double concentration = 0;
      double minDistance = Double.POSITIVE_INFINITY;
      int minIndex = -1;

      // for each point of interest (from the end to the beginning so that
      // removing can be done easily)
      for (int i = temp.size() - 1; i >= 0; i--) {

        // If this is a rising point
        if (temp.get(i).getRising()) {

          minDistance = Double.POSITIVE_INFINITY;
          minIndex = -1;
          double distance = 0, sumRisingP = 0, sumRisingQ = 0, sumReductionP =
            0, sumReductionQ = 0;

          // Check the reduction points from that point on with some distance
          // between them
          for (int j = temp.size() - 1; j > i; j--) {

            if (temp.get(j).getRising() == false && (j - i + 1 > 3)) {

              // Estimate the concentration of points of interest
              concentration =
                100
                        * (double) (j - i + 1)
                        / (double) (temp.get(j).getMinute() - temp.get(i)
                                .getMinute());

              // System.out.println("Start Index: "
              // + i
              // + " End Index: "
              // + j
              // + " Number of POIs: "
              // + (j - i + 1)
              // + " Duration: "
              // + (temp.get(j).getMinute() - temp.get(i)
              // .getMinute()) + " = " + disturbance);

              // If it is large enough, then the active power quantity is
              // measured and compared
              if (concentration > Constants.CONCENTRATION_THRESHOLD) {

                double[] tempValues =
                  { -temp.get(j).getPDiff(), -temp.get(j).getQDiff() };

                // distance =
                // temp.get(i).percentageEuclideanDistance(tempValues)
                // ;
                // System.out.println("Euclidean Distance: "
                // + temp.get(i).euclideanDistance(tempValues)
                // + " Length: "
                // + temp.get(i).euclideanLength() + " = "
                // + distance);

                if (temp.get(i).percentageEuclideanDistance(tempValues) < Constants.CLUSTER_THRESHOLD) {

                  for (int k = i; k <= j; k++) {

                    if (temp.get(k).getRising()) {
                      sumRisingP += temp.get(k).getPDiff();
                      sumRisingQ += temp.get(k).getQDiff();
                    }
                    else {
                      sumReductionP += temp.get(k).getPDiff();
                      sumReductionQ += temp.get(k).getQDiff();
                    }

                  }
                  double[] sumRising = { sumRisingP, -sumRisingQ };
                  double[] sumReduction = { -sumReductionP, -sumReductionQ };

                  distance =
                    Utils.percentageEuclideanDistance(sumRising, sumReduction);

                  // System.out.println("Euclidean Distance: "
                  // + Utils.euclideanDistance(sumRising,
                  // sumReduction)
                  // + " Length: " + Utils.norm(sumRising)
                  // + " = " + distance);

                  if (Utils
                          .percentageEuclideanDistance(sumRising, sumReduction) < Constants.CLUSTER_THRESHOLD) {
                    minIndex = j;
                    minDistance = distance;
                  }

                }
              }

            }
          }

          // If a cluster was found
          if (minIndex != -1) {
            // System.out.println("Something Something...");
            // Making the pair of the start and end points of interest
            PointOfInterest[] cluster = { temp.get(i), temp.get(minIndex) };

            // Removing all the points of interest of the cluster from the
            // arrays and adding the pair to the cluster array.
            for (int k = minIndex; k >= i; k--) {
              if (temp.get(k).getRising())
                risingPoints.remove(temp.get(k));
              else
                reductionPoints.remove(temp.get(k));
              temp.remove(k);
            }
            clusters.add(cluster);
          }

        }
      }
    }
  }

  /**
   * This function is used for searching over the event for basic shapes of
   * consumption events. Matching events are pairs of switching on / off that
   * are close enough in active and reactive power change and make certainly an
   * appliance end-use that must be added to the final pairs.
   */
  public void detectBasicShapes ()
  {
    // Creating a chronological collection of points of interest
    ArrayList<PointOfInterest> temp =
      new ArrayList<PointOfInterest>(risingPoints);
    temp.addAll(reductionPoints);
    Collections.sort(temp, Constants.comp);

    // Searching for each shape
    detectChairs(temp);

    detectInversedChairs(temp);

    detectTrianglesRectangles(temp);

    // System.out.println(temp.toString());

  }

  /**
   * This function is used for searching over the event for chairs of
   * consumption, meaning a series of rising - reduction - reduction points of
   * interest close enough in active and reactive power change and make
   * certainly an appliance end-use that must be added to the final pairs.
   */
  private void detectChairs (ArrayList<PointOfInterest> pois)
  {

    // double distance = 0;
    double[] rise = new double[2];
    double[] red = new double[2];

    for (int i = pois.size() - 2; i >= 0; i--) {
      if (pois.size() > i + 2 && pois.get(i).getRising()
          && pois.get(i + 1).getRising() == false
          && pois.get(i + 2).getRising() == false) {

        rise[0] = -pois.get(i).getPDiff();
        rise[1] = -pois.get(i).getQDiff();

        red[0] = pois.get(i + 1).getPDiff() + pois.get(i + 2).getPDiff();
        red[1] = pois.get(i + 1).getQDiff() + pois.get(i + 2).getQDiff();

        // distance = Utils.percentageEuclideanDistance(rise, red);

        if (Utils.percentageEuclideanDistance(rise, red) < Constants.CHAIR_DISTANCE_THRESHOLD) {
          // System.out.println("Distance Chair: "
          // + Utils.percentageEuclideanDistance(rise, red));

          PointOfInterest[] chair =
            { pois.get(i), pois.get(i + 1), pois.get(i + 2) };
          chairs.add(chair);

          risingPoints.remove(pois.get(i));
          reductionPoints.remove(pois.get(i + 1));
          reductionPoints.remove(pois.get(i + 2));
          pois.remove(i + 2);
          pois.remove(i + 1);
          pois.remove(i);

        }

      }
    }

  }

  /**
   * This function is used for searching over the event for inversed chairs of
   * consumption, meaning a series of rising - rising - reduction points of
   * interest close enough in active and reactive power change and make
   * certainly an appliance end-use that must be added to the final pairs.
   */
  private void detectInversedChairs (ArrayList<PointOfInterest> pois)
  {

    // double distance = 0;
    double[] rise = new double[2];
    double[] red = new double[2];

    for (int i = pois.size() - 2; i >= 0; i--) {
      if (pois.size() > i + 2 && pois.get(i).getRising()
          && pois.get(i + 1).getRising()
          && pois.get(i + 2).getRising() == false) {

        rise[0] = -(pois.get(i).getPDiff() + pois.get(i + 1).getPDiff());
        rise[1] = -(pois.get(i).getQDiff() + pois.get(i + 1).getQDiff());

        red[0] = pois.get(i + 2).getPDiff();
        red[1] = pois.get(i + 2).getQDiff();

        // distance = Utils.percentageEuclideanDistance(rise, red);

        if (Utils.percentageEuclideanDistance(rise, red) < Constants.CHAIR_DISTANCE_THRESHOLD) {

          // System.out.println("Distance Inversed: "
          // + Utils.percentageEuclideanDistance(rise, red));
          PointOfInterest[] inversedChair =
            { pois.get(i), pois.get(i + 1), pois.get(i + 2) };
          inversedChairs.add(inversedChair);

          risingPoints.remove(pois.get(i));
          risingPoints.remove(pois.get(i + 1));
          reductionPoints.remove(pois.get(i + 2));
          pois.remove(i + 2);
          pois.remove(i + 1);
          pois.remove(i);

        }

      }
    }

  }

  /**
   * This function is used for searching over the event for triangles (one
   * minute distance) or rectangles (more than one minute distance) of
   * consumption, meaning a series of rising - reduction points of
   * interest close enough in active and reactive power change and make
   * certainly an appliance end-use that must be added to the final pairs.
   */
  private void detectTrianglesRectangles (ArrayList<PointOfInterest> pois)
  {

    // double distance = 0;
    double[] rise = new double[2];
    double[] red = new double[2];

    for (int i = pois.size() - 1; i >= 0; i--) {
      if (pois.size() > i + 1 && pois.get(i).getRising()
          && pois.get(i + 1).getRising() == false) {

        rise[0] = pois.get(i).getPDiff();
        rise[1] = pois.get(i).getQDiff();

        red[0] = -pois.get(i + 1).getPDiff();
        red[1] = -pois.get(i + 1).getQDiff();

        // distance = Utils.percentageEuclideanDistance(rise, red);

        if (Utils.percentageEuclideanDistance(rise, red) < Constants.TRIANGLE_DISTANCE_THRESHOLD) {
          // System.out.println("Distance Rectangle: "
          // + Utils.percentageEuclideanDistance(rise, red));
          PointOfInterest[] temp = { pois.get(i), pois.get(i + 1) };
          if (pois.get(i + 1).getMinute() - pois.get(i).getMinute() == 1)
            triangles.add(temp);
          else
            rectangles.add(temp);

          risingPoints.remove(pois.get(i));
          reductionPoints.remove(pois.get(i + 1));
          pois.remove(i + 1);
          pois.remove(i);

        }

      }
    }

  }

  /**
   * This is an important function used in order to match the remaining points
   * of interest that could not be matched to the primitive basic schemes that
   * have been searched up to now. In order to solve this problem, integer
   * programming over the set packing problem was used, meaning the solver
   * searches for close matching groups of points of interest without requiring
   * taking all of them in the solution, but the solution solving better the
   * distance problem at hand.
   */
  public void findCombinations ()
  {

    Map<int[], Double> input = new HashMap<int[], Double>();

    // Creating a collection of the points of interest in chronological order
    ArrayList<PointOfInterest> temp =
      new ArrayList<PointOfInterest>(risingPoints);
    temp.addAll(reductionPoints);
    Collections.sort(temp, Constants.comp);

    System.out.println("Points Of Interest: " + temp.size());
    System.out.println("Rising Points: " + risingPoints.toString());
    System.out.println("Reduction Points: " + reductionPoints.toString());

    // If there are more than one points.
    if (temp.size() > 1 && risingPoints.size() > 0
        && reductionPoints.size() > 0) {

      // input = Utils.findCombinations(temp);
      input = Utils.findCombinations2(temp);
      System.out.println("Input for event " + id + ": " + input.size());

      // Creating the input for the integer programming solver
      double[] cost = new double[input.size()];
      int[][] tempArray = new int[input.size()][temp.size()];
      int counter = 0;
      for (int[] in: input.keySet()) {
        // System.out.println("Array: " + Arrays.toString(index) + " Distance: "
        // + 1 / input.get(index) + " Similarity: "
        // + input.get(index));
        tempArray[counter] = in;
        cost[counter++] = input.get(in);

      }

      // for (int i = 0; i < input.size(); i++)
      // System.out.println("Array: " + Arrays.toString(tempArray[i])
      // + " Cost: " + cost[i]);

      // If there is only one pair, then this is the solution
      if (input.size() == 1) {

        // TODO Add threshold???
        PointOfInterest[] pair =
          { risingPoints.get(0), reductionPoints.get(0) };

        finalPairs.add(pair);

      }
      // In case of more solutions the integer solver is called.
      else {

        // ArrayList<ArrayList<Integer>> solutions = Utils.solve(tempArray,
        // cost);
        System.out.println("INTEGER PROGRAMMING");

        // Solving the problem and presenting the solution
        ArrayList<Integer> solution = Utils.solve2(tempArray, cost);

        // System.out.println("Solutions:");

        // for (int i = 0; i < solutions.size(); i++) {
        System.out.println("Solution:");

        // System.out.println("Solution " + (i + 1));
        for (Integer index: solution) {
          System.out.println(Arrays.toString(tempArray[index])
                             + " Similarity: " + input.get(tempArray[index])
                             + " Distance: "
                             + (1 / input.get(tempArray[index])));

          // For each part of the solution, the corresponding pairs are created
          // and added to the final pairs.
          ArrayList<PointOfInterest[]> newFinalPairs =
            Utils.createFinalPairs(temp, tempArray[index]);
          finalPairs.addAll(newFinalPairs);
        }
      }

    }
    else {

      if (temp.size() < 2)
        System.out.println("Not many POIs");
      else if (risingPoints.size() == 0)
        System.out.println("No Rising Points");
      else if (reductionPoints.size() == 0)
        System.out.println("No Reduction Points");

    }

    // Clearing the variables that will not be used again.
    temp.clear();
    input.clear();
    risingPoints.clear();
    reductionPoints.clear();

  }

  /**
   * This is the function that takes each of the lists created before (matching
   * points, clusters, triangles, rectangles, chairs, inversed chairs) and add
   * them to the final pairs.
   */
  public void calculateFinalPairs ()
  {

    finalPairs.addAll(matchingPoints);
    matchingPoints.clear();

    finalPairs.addAll(triangles);
    triangles.clear();

    finalPairs.addAll(rectangles);
    rectangles.clear();

    finalPairs.addAll(clusters);
    clusters.clear();

    // Creating two pairs with the same end point and two starting points for
    // chairs
    for (PointOfInterest[] tempChair: chairs) {

      PointOfInterest end1 = tempChair[1];
      PointOfInterest end2 = tempChair[2];

      PointOfInterest start1 =
        new PointOfInterest(tempChair[0].getMinute(), true,
                            -tempChair[1].getPDiff(), -tempChair[1].getQDiff());
      PointOfInterest start2 =
        new PointOfInterest(tempChair[0].getMinute(), true,
                            -tempChair[2].getPDiff(), -tempChair[2].getQDiff());

      PointOfInterest[] rectangle1 = { start1, end1 };
      PointOfInterest[] rectangle2 = { start2, end2 };

      finalPairs.add(rectangle1);
      finalPairs.add(rectangle2);

    }
    chairs.clear();

    // Creating two pairs with the same start point and two ending points for
    // inversed chairs.
    for (PointOfInterest[] tempChair: inversedChairs) {

      PointOfInterest start1 = tempChair[0];
      PointOfInterest start2 = tempChair[1];

      PointOfInterest end1 =
        new PointOfInterest(tempChair[2].getMinute(), false,
                            -tempChair[0].getPDiff(), -tempChair[0].getQDiff());
      PointOfInterest end2 =
        new PointOfInterest(tempChair[2].getMinute(), false,
                            -tempChair[1].getPDiff(), -tempChair[1].getQDiff());

      PointOfInterest[] rectangle1 = { start1, end1 };
      PointOfInterest[] rectangle2 = { start2, end2 };

      finalPairs.add(rectangle1);
      finalPairs.add(rectangle2);

    }
    inversedChairs.clear();

    // Sorting final pairs in chronological order.
    Collections.sort(finalPairs, Constants.comp2);

  }

  /**
   * This function is used to present the first part of basic information of the
   * event on the console.
   */
  public void status ()
  {
    System.out.println("Id: " + id);
    System.out.println("Start Minute: " + startMinute);
    System.out.println("End Minute: " + endMinute);
    System.out.println("Threshold: " + threshold);
    System.out.println("Active Load: "
                       + Arrays.toString(activePowerConsumptions));
    System.out.println("Reactive Load: "
                       + Arrays.toString(reactivePowerConsumptions));
    if (risingPoints.size() + reductionPoints.size() > 0)
      showPoints();

    System.out.println();
    System.out.println();
  }

  /**
   * This function is used to present the second part of basic information of
   * the event on the console.
   */
  public void status2 ()
  {

    System.out.println("Event: " + getId());

    if (finalPairs.size() > 0) {
      System.out.println("Final Pairs:");
      for (PointOfInterest[] pois: finalPairs)
        System.out.println(Arrays.toString(pois));
    }
    else {

      if (switchingPoints.size() > 0) {
        System.out.println("Switching Points:");
        for (PointOfInterest[] pois: switchingPoints)
          System.out.println(Arrays.toString(pois));
      }

      if (matchingPoints.size() > 0) {
        System.out.println("Matching Points:");
        for (PointOfInterest[] pois: matchingPoints)
          System.out.println(Arrays.toString(pois));
      }

      if (clusters.size() > 0) {
        System.out.println("Clusters:");
        for (PointOfInterest[] pois: clusters)
          System.out.println(Arrays.toString(pois));
      }

      if (chairs.size() > 0) {
        System.out.println("Chairs: ");
        for (PointOfInterest[] chair: chairs)
          System.out.println(Arrays.toString(chair));
      }

      if (inversedChairs.size() > 0) {
        System.out.println("Inversed Chairs: ");
        for (PointOfInterest[] ichair: inversedChairs)
          System.out.println(Arrays.toString(ichair));
      }

      if (triangles.size() > 0) {
        System.out.println("Triangles: ");
        for (PointOfInterest[] triangle: triangles)
          System.out.println(Arrays.toString(triangle));
      }

      if (rectangles.size() > 0) {
        System.out.println("Rectangles: ");
        for (PointOfInterest[] rectangle: rectangles)
          System.out.println(Arrays.toString(rectangle));
      }

    }

  }

  /**
   * This auxiliary function is used for showing the points of interest detected
   * by the detection algorithm.
   */
  private void showPoints ()
  {

    if (risingPoints.size() > 0) {
      System.out.println("Rising Points for Event " + id);

      for (PointOfInterest poi: risingPoints)
        poi.status();
    }

    if (reductionPoints.size() > 0) {
      System.out.println("Reduction Points for Event " + id);

      for (PointOfInterest poi: reductionPoints)
        poi.status();
    }
  }

  @Override
  public String toString ()
  {
    return ("Event " + Integer.toString(id));
  }
}
