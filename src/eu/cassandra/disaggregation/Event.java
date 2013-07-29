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

package eu.cassandra.disaggregation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.paukov.combinatorics.Factory;
import org.paukov.combinatorics.Generator;
import org.paukov.combinatorics.ICombinatoricsVector;

public class Event
{

  static {
    System.loadLibrary("jniconstraintsolver");
  }

  private int id;
  private int startMinute = -1;
  private int endMinute = -1;
  private double[] activePowerConsumptions = new double[0];
  private double[] reactivePowerConsumptions = new double[0];
  private double[] derivative;
  private int[] marker;
  private boolean wmFlag = false;
  private double threshold = 0;

  private ArrayList<PointOfInterest> risingPoints =
    new ArrayList<PointOfInterest>();
  private ArrayList<PointOfInterest> reductionPoints =
    new ArrayList<PointOfInterest>();

  private ArrayList<PointOfInterest[]> clusters =
    new ArrayList<PointOfInterest[]>();
  private ArrayList<PointOfInterest[]> switchingPoints =
    new ArrayList<PointOfInterest[]>();
  private ArrayList<PointOfInterest[]> matchingPoints =
    new ArrayList<PointOfInterest[]>();
  private ArrayList<PointOfInterest[]> chairs =
    new ArrayList<PointOfInterest[]>();
  private ArrayList<PointOfInterest[]> inversedChairs =
    new ArrayList<PointOfInterest[]>();
  private ArrayList<PointOfInterest[]> triangles =
    new ArrayList<PointOfInterest[]>();
  private ArrayList<PointOfInterest[]> rectangles =
    new ArrayList<PointOfInterest[]>();
  private ArrayList<PointOfInterest[]> finalPairs =
    new ArrayList<PointOfInterest[]>();

  public Event (int start, int end, double[] active, double[] reactive)
  {
    id = Constants.EVENTS_ID++;
    startMinute = start;
    endMinute = end;
    activePowerConsumptions = active;
    reactivePowerConsumptions = reactive;
    normalizeConsumptions();
    findPointsOfInterest();
    Collections.sort(risingPoints, Constants.comp);
    Collections.sort(reductionPoints, Constants.comp);
    cleanPointsOfInterest();
    // cleanPointsOfInterest(Constants.MATCHING_THRESHOLD);
    // status();
  }

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

  public int getId ()
  {
    return id;
  }

  public int getStartMinute ()
  {
    return startMinute;
  }

  public int getEndMinute ()
  {
    return endMinute;
  }

  public double[] getActivePowerConsumptions ()
  {
    return activePowerConsumptions;
  }

  public double[] getReactivePowerConsumptions ()
  {
    return reactivePowerConsumptions;
  }

  public ArrayList<PointOfInterest> getRisingPoints ()
  {
    return risingPoints;
  }

  public ArrayList<PointOfInterest> getReductionPoints ()
  {
    return reductionPoints;
  }

  public ArrayList<PointOfInterest[]> getSwitchingPoints ()
  {
    return switchingPoints;
  }

  public ArrayList<PointOfInterest[]> getMatchingPoints ()
  {
    return matchingPoints;
  }

  public ArrayList<PointOfInterest[]> getFinalPairs ()
  {
    return finalPairs;
  }

  public PointOfInterest[] getFinalPairs (int index)
  {
    return finalPairs.get(index);
  }

  public boolean getWashingMachineFlag ()
  {
    return wmFlag;
  }

  public void setWashingMachine ()
  {
    wmFlag = true;
  }

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

  public String toString ()
  {
    return ("Event " + Integer.toString(id));
  }

  private void normalizeConsumptions ()
  {

    double active = activePowerConsumptions[0];
    double reactive = reactivePowerConsumptions[0];

    for (int i = 0; i < activePowerConsumptions.length; i++)
      activePowerConsumptions[i] -= active;

    for (int i = 0; i < reactivePowerConsumptions.length; i++)
      reactivePowerConsumptions[i] -= reactive;

    activePowerConsumptions[activePowerConsumptions.length - 1] = 0;
    reactivePowerConsumptions[reactivePowerConsumptions.length - 1] = 0;

  }

  private void findPointsOfInterest ()
  {

    derivative = new double[activePowerConsumptions.length - 1];
    marker = new int[activePowerConsumptions.length - 1];
    ArrayList<Integer> ind = new ArrayList<Integer>();
    ArrayList<ArrayList<Integer>> groups = new ArrayList<ArrayList<Integer>>();

    for (int i = 0; i < derivative.length; i++) {

      // For NaN
      if (activePowerConsumptions[i + 1] == 0
          && activePowerConsumptions[i] == 0)
        derivative[i] = 0;
      else
        derivative[i] =
          100 * ((activePowerConsumptions[i + 1] - activePowerConsumptions[i]) / activePowerConsumptions[i]);

      if (derivative[i] > Constants.DERIVATIVE_LIMIT)
        marker[i] = 1;
      else if (derivative[i] < -Constants.DERIVATIVE_LIMIT)
        marker[i] = -1;
    }

    // System.out.println(Arrays.toString(derivative));
    // System.out.println(Arrays.toString(marker));

    int[] markerNew = new int[marker.length + 2];
    ArrayList<Integer> temp = new ArrayList<Integer>();

    for (int i = 1; i < markerNew.length - 1; i++)
      markerNew[i] = marker[i - 1];

    // System.out.println(Arrays.toString(markerNew));

    for (int i = 1; i < markerNew.length - 1; i++) {

      if (markerNew[i] != 0) {

        if (markerNew[i - 1] == 0 && markerNew[i + 1] == 0)
          ind.add(i - 1);

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

    // System.out.println("Individuals:" + ind.toString());
    // System.out.println("Groups: " + groups.toString());

    if (ind.size() > 0)
      createIndividualPoints(ind);

    if (groups.size() > 0)
      analyseGroups(groups);

  }

  private void createIndividualPoints (ArrayList<Integer> ind)
  {

    int index = -1;
    ArrayList<Integer> rising = new ArrayList<Integer>();
    ArrayList<Integer> reduction = new ArrayList<Integer>();

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

    for (Integer rise: rising) {
      singleRisingPoint(rise);
    }

    for (Integer red: reduction) {
      singleReductionPoint(red);
    }

  }

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

  private void singleRisingPoint (int rise)
  {
    int lastReductionPoint = -1;
    int lastRisingPoint = -1;

    int minIndex = Math.max(0, rise - Constants.REDUCTION_LIMIT);
    int maxIndex =
      Math.min(derivative.length - 1, rise + Constants.RISING_LIMIT);

    // System.out.println("Min Index: " + minIndex + " MaxIndex: " + maxIndex);

    lastReductionPoint = rise;
    lastRisingPoint = rise + 1;

    for (int i = minIndex; i < rise; i++) {

      if (derivative[i] < 0 && marker[i] == 0)
        lastReductionPoint = i;
      else if (derivative[i] > 0 || marker[i] != 0)
        lastReductionPoint = rise;
    }

    for (int i = maxIndex; i > rise; i--) {
      if (derivative[i] > 0 && marker[i] == 0)
        lastRisingPoint = i;
      else if (derivative[i] < 0 || marker[i] != 0)
        lastRisingPoint = rise + 1;
    }

    // System.out.println("Index: " + rise + " Reduction Point: "
    // + lastReductionPoint + " Rising Point: "
    // + lastRisingPoint);

    double pdiff =
      activePowerConsumptions[lastRisingPoint]
              - activePowerConsumptions[lastReductionPoint];
    double qdiff =
      reactivePowerConsumptions[lastRisingPoint]
              - reactivePowerConsumptions[lastReductionPoint];

    risingPoints.add(new PointOfInterest(rise, true, pdiff, qdiff));
  }

  private void singleReductionPoint (int red)
  {

    int lastReductionPoint = -1;
    int lastRisingPoint = -1;

    int minIndex = Math.max(0, red - Constants.RISING_LIMIT);
    int maxIndex =
      Math.min(derivative.length - 1, red + Constants.REDUCTION_LIMIT);

    lastRisingPoint = red;
    lastReductionPoint = red + 1;

    for (int i = minIndex; i < red; i++) {

      if (derivative[i] > 0 && marker[i] == 0)
        lastRisingPoint = i;
      else if (derivative[i] < 0 || marker[i] != 0)
        lastRisingPoint = red;
    }

    for (int i = red + 1; i < maxIndex; i++) {
      if (derivative[i] < 0 && marker[i] == 0)
        lastReductionPoint = i;
      else if (derivative[i] > 0 || marker[i] != 0)
        lastReductionPoint = red + 1;
    }

    // System.out.println("Index: " + red + " Rising Point: " + lastRisingPoint
    // + " Reduction Point: " + lastReductionPoint);

    double pdiff =
      activePowerConsumptions[lastReductionPoint]
              - activePowerConsumptions[lastRisingPoint];
    double qdiff =
      reactivePowerConsumptions[lastReductionPoint]
              - reactivePowerConsumptions[lastRisingPoint];

    reductionPoints.add(new PointOfInterest(red, false, pdiff, qdiff));

  }

  private void analyseGroups (ArrayList<ArrayList<Integer>> groups)
  {

    int sum = 0;

    for (ArrayList<Integer> group: groups) {

      sum = 0;

      // System.out.println(group.toString());

      for (Integer index: group)
        sum += marker[index];

      if (sum == group.size()) {
        // System.out.println("All Rising");
        allRisingAnalysis(group);
      }
      else if (sum == -group.size()) {
        // System.out.println("All Reduction");
        allReductionAnalysis(group);
      }
      else {
        // System.out.println("Mixed");
        mixedAnalysis(group);
      }
    }

  }

  private void allRisingAnalysis (ArrayList<Integer> group)
  {
    int lastReductionPoint = -1;
    int lastRisingPoint = -1;

    if (group.size() % 2 == 1) {
      singleRisingPoint(group.get(group.size() - 1));
    }

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

      double pdiff =
        activePowerConsumptions[lastRisingPoint]
                - activePowerConsumptions[lastReductionPoint];
      double qdiff =
        reactivePowerConsumptions[lastRisingPoint]
                - reactivePowerConsumptions[lastReductionPoint];

      risingPoints.add(new PointOfInterest(i, true, pdiff, qdiff));

    }

  }

  private void allReductionAnalysis (ArrayList<Integer> group)
  {
    int lastReductionPoint = -1;
    int lastRisingPoint = -1;

    if (group.size() % 2 == 1) {
      singleReductionPoint(group.get(group.size() - 1));
    }

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

      double pdiff =
        activePowerConsumptions[lastReductionPoint]
                - activePowerConsumptions[lastRisingPoint];
      double qdiff =
        reactivePowerConsumptions[lastReductionPoint]
                - reactivePowerConsumptions[lastRisingPoint];

      reductionPoints.add(new PointOfInterest(i + 1, false, pdiff, qdiff));

    }

  }

  private void mixedAnalysis (ArrayList<Integer> group)
  {

    ArrayList<ArrayList<Integer>> rise = new ArrayList<ArrayList<Integer>>();
    ArrayList<ArrayList<Integer>> reduce = new ArrayList<ArrayList<Integer>>();
    ArrayList<Integer> temp = new ArrayList<Integer>();
    int index = -1;
    for (int i = 0; i < group.size(); i++) {

      index = group.get(i);

      if (temp.size() == 0 || marker[index - 1] == marker[index])
        temp.add(index);
      else {
        if (marker[index - 1] == 1)
          rise.add(temp);
        else
          reduce.add(temp);

        temp = new ArrayList<Integer>();
        temp.add(index);
      }

    }

    if (marker[temp.get(temp.size() - 1)] == 1)
      rise.add(temp);
    else
      reduce.add(temp);

    // System.out.println("Rise: " + rise.toString());
    // System.out.println("Reduce: " + reduce.toString());

    for (ArrayList<Integer> rising: rise) {

      if (rising.size() == 1)
        singleRisingPoint(rising.get(0));
      else
        allRisingAnalysis(rising);

    }

    for (ArrayList<Integer> reduction: reduce) {

      if (reduction.size() == 1)
        singleReductionPoint(reduction.get(0));
      else
        allReductionAnalysis(reduction);

    }
  }

  private void cleanPointsOfInterest (Double... threshold)
  {

    if (threshold.length == 1) {
      this.threshold = threshold[0];
    }
    else
      this.threshold = thresholdTuning();

    // System.out.println("Threshold: " + result);

    for (int i = risingPoints.size() - 1; i >= 0; i--)
      if (Math.abs(risingPoints.get(i).getPDiff()) < this.threshold) {
        // System.out.println(Math.abs(risingPoints.get(i).getPDiff()) + "<"
        // + result);
        risingPoints.remove(i);
      }
    for (int i = reductionPoints.size() - 1; i >= 0; i--)
      if (Math.abs(reductionPoints.get(i).getPDiff()) < this.threshold) {
        // System.out.println(Math.abs(reductionPoints.get(i).getPDiff()) + "<"
        // + result);
        reductionPoints.remove(i);
      }
  }

  private double thresholdTuning ()
  {
    double step = 0;

    ArrayList<Double> alterations = new ArrayList<Double>();
    ArrayList<Double> pdiffs = new ArrayList<Double>();

    for (PointOfInterest poi: risingPoints)
      pdiffs.add(Math.abs(poi.getPDiff()));

    for (PointOfInterest poi: reductionPoints)
      pdiffs.add(Math.abs(poi.getPDiff()));

    Collections.sort(pdiffs);
    step = pdiffs.get(0) / 2;

    if (step == 0)
      return Constants.DEFAULT_THRESHOLD;

    double temp = pdiffs.get(0);

    while (temp < pdiffs.get(pdiffs.size() - 1)) {
      alterations.add(temp);
      temp += step;
    }

    // System.out.println("Alterations: " + alterations.toString());

    double threshold = Constants.DEFAULT_THRESHOLD;

    for (Double alter: alterations) {

      // System.out.println("For alteration " + alter);

      // double sumOldRisingP = 0, sumOldReductionP = 0, devOldP = 0;
      double sumRisingP = 0, sumReductionP = 0, sumRisingQ = 0, sumReductionQ =
        0, devP = 0, devQ = 0;

      ArrayList<PointOfInterest> rising =
        new ArrayList<PointOfInterest>(risingPoints);
      ArrayList<PointOfInterest> reduction =
        new ArrayList<PointOfInterest>(reductionPoints);

      for (int i = rising.size() - 1; i >= 0; i--) {
        // sumOldRisingP += rising.get(i).getPDiff();
        if (Math.abs(rising.get(i).getPDiff()) < alter) {
          // System.out.println(Math.abs(risingPoints.get(i).getPDiff()) + "<"
          // + alter);
          rising.remove(i);
        }
      }
      for (int i = reduction.size() - 1; i >= 0; i--) {
        // sumOldReductionP += reduction.get(i).getPDiff();
        if (Math.abs(reduction.get(i).getPDiff()) < alter) {
          // System.out.println(Math.abs(reductionPoints.get(i).getPDiff()) +
          // "<"
          // + alter);
          reduction.remove(i);
        }
      }

      if (rising.size() != risingPoints.size()
          || reduction.size() != reductionPoints.size()) {

        // System.out.println("Something removed");
        // System.out.println(risingPoints.toString());
        // System.out.println(rising.toString());
        // System.out.println(reductionPoints.toString());
        // System.out.println(reduction.toString());

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

        devP = Math.abs(100 * (sumRisingP + sumReductionP) / sumRisingP);
        devQ = Math.abs(100 * (sumRisingQ + sumReductionQ) / sumRisingQ);
        // devOldP =
        // Math.abs(100
        // * ((sumOldRisingP - sumOldReductionP) - (sumRisingP - sumReductionP))
        // / (sumOldRisingP - sumOldReductionP));

        // System.out.println("Alteration: " + alter + " DevP: " + devP
        // + " DevQ: " + devQ + " DevOldP: " + devOldP);

        if (devP < Constants.DIFFERENCE_LIMIT
            && devQ < Constants.DIFFERENCE_LIMIT) {
          // && devOldP < Constants.OLD_DIFFERENCE_LIMIT) {
          // System.out.println("Alteration: " + alter + " DevP: " + devP
          // + " DevQ: " + devQ + " DevOldP: " + devOldP);

          rising.addAll(reduction);

          Collections.sort(rising, Constants.comp);

          double sumOld = 0, sumNew = 0;
          double[] Pnew = curveReconstruction(rising);

          for (int i = 0; i < Pnew.length; i++) {
            sumOld += activePowerConsumptions[i];
            sumNew += Pnew[i];
          }

          double distance = 100 * (Math.abs(sumOld - sumNew)) / sumOld;

          // System.out.println("SumOld: " + sumOld + " SumNew: " + sumNew
          // + " Distance: " + distance);

          if (distance < Constants.OLD_DIFFERENCE_LIMIT)
            threshold = alter;

        }
      }

    }

    return threshold;

  }

  public PointOfInterest findPOI (int minute, boolean rising)
  {

    if (rising) {

      for (PointOfInterest poi: risingPoints) {
        if (poi.getMinute() == minute)
          return poi;
      }
    }
    else {
      for (PointOfInterest poi: reductionPoints) {
        if (poi.getMinute() == minute)
          return poi;
      }
    }

    return null;
  }

  public void detectSwitchingPoints ()
  {
    ArrayList<PointOfInterest> temp =
      new ArrayList<PointOfInterest>(risingPoints);

    temp.addAll(reductionPoints);

    Collections.sort(temp, Constants.comp);
    // double distance = 0;

    for (int i = 0; i < temp.size() - 1; i++) {

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

  public void detectMatchingPoints ()
  {

    ArrayList<PointOfInterest> temp =
      new ArrayList<PointOfInterest>(risingPoints);

    temp.addAll(reductionPoints);

    Collections.sort(temp, Constants.comp);

    double distance = 0;
    double minDistance = Double.POSITIVE_INFINITY;
    int minIndex = -1;

    for (int i = 0; i < temp.size(); i++) {

      if (temp.get(i).getRising()) {

        distance = 0;
        minDistance = Double.POSITIVE_INFINITY;
        minIndex = -1;

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

        if (minIndex != -1) {
          PointOfInterest[] tempPOI = { temp.get(i), temp.get(minIndex) };
          matchingPoints.add(tempPOI);
        }
      }

    }

    if (matchingPoints.size() > 0)
      for (PointOfInterest[] pois: matchingPoints) {
        risingPoints.remove(pois[0]);
        reductionPoints.remove(pois[1]);
      }

  }

  public void detectClusters ()
  {
    ArrayList<PointOfInterest> temp =
      new ArrayList<PointOfInterest>(risingPoints);

    temp.addAll(reductionPoints);

    if (temp.size() > 3) {

      Collections.sort(temp, Constants.comp);

      double disturbance = 0;
      double minDistance = Double.POSITIVE_INFINITY;
      int minIndex = -1;

      for (int i = temp.size() - 1; i >= 0; i--) {

        if (temp.get(i).getRising()) {

          minDistance = Double.POSITIVE_INFINITY;
          minIndex = -1;
          double distance = 0, sumRisingP = 0, sumRisingQ = 0, sumReductionP =
            0, sumReductionQ = 0;

          for (int j = temp.size() - 1; j > i; j--) {

            if (temp.get(j).getRising() == false && (j - i + 1 > 3)) {

              disturbance =
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

              if (disturbance > Constants.DISTURBANCE_THRESHOLD) {

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

          if (minIndex != -1) {
            System.out.println("Something Something...");
            PointOfInterest[] cluster = { temp.get(i), temp.get(minIndex) };
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

  public void detectBasicShapes ()
  {

    ArrayList<PointOfInterest> temp =
      new ArrayList<PointOfInterest>(risingPoints);

    temp.addAll(reductionPoints);

    Collections.sort(temp, Constants.comp);

    detectChairs(temp);

    detectInversedChairs(temp);

    detectTrianglesRectangles(temp);

    // System.out.println(temp.toString());

  }

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

  public void createCombinations ()
  {

    Map<int[], Double> input = new HashMap<int[], Double>();
    Integer[] points = null;
    int[] pointsArray = null;
    List<Integer> subset = new ArrayList<Integer>();
    double distance = 0;

    ArrayList<PointOfInterest> temp =
      new ArrayList<PointOfInterest>(risingPoints);

    temp.addAll(reductionPoints);

    Collections.sort(temp, Constants.comp);

    if (temp.size() > 1) {

      System.out.println("Points Of Interest: " + temp.toString());

      for (int i = 0; i < temp.size(); i++) {

        if (temp.get(i).getRising()) {

          points = Utils.findRedPoints(i, temp);

          ICombinatoricsVector<Integer> initialSet =
            Factory.createVector(points);

          // System.out.println("Reduction Points for rising point " + i + ": "
          // + initialSet.toString());

          Generator<Integer> gen = Factory.createSubSetGenerator(initialSet);

          for (ICombinatoricsVector<Integer> subSet: gen) {

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
              // System.out.println("Subset: " + subSet.toString() +
              // " Distance: "
              // + distance);
              if (distance < Constants.DISTANCE_THRESHOLD)
                pointsArray = new int[temp.size()];
              pointsArray[i] = 1;
              for (Integer index: subset)
                pointsArray[index] = 1;

              boolean flag = true;
              for (int[] content: input.keySet()) {
                if (Arrays.equals(content, pointsArray)) {
                  flag = false;
                  break;
                }
              }

              if (flag)
                input.put(pointsArray, distance);
            }
          }

        }
        else {
          points = Utils.findRisPoints(i, temp);

          ICombinatoricsVector<Integer> initialSet =
            Factory.createVector(points);

          // System.out.println("Rising Points for reduction point " + i + ": "
          // + initialSet.toString());

          Generator<Integer> gen = Factory.createSubSetGenerator(initialSet);

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

              distance =
                1 / (Utils.percentageEuclideanDistance(sum, tempValues) + Constants.NEAR_ZERO);
              // System.out.println("Subset: " + subSet.toString() +
              // " Distance: "
              // + distance);
              if (distance < Constants.DISTANCE_THRESHOLD) {
                pointsArray = new int[temp.size()];
                pointsArray[i] = 1;
                for (Integer index: subset)
                  pointsArray[index] = 1;

                boolean flag = true;
                for (int[] content: input.keySet()) {
                  if (Arrays.equals(content, pointsArray)) {
                    flag = false;
                    break;
                  }
                }
                if (flag)
                  input.put(pointsArray, distance);
              }
            }
          }
        }
      }
      System.out.println("Input: " + input.size());
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
      // System.out.println("Array: " + Arrays.toString(tempArray[i]));
      // System.out.println("Cost: " + Arrays.toString(cost));

      if (input.size() == 1) {

        PointOfInterest[] pair =
          { risingPoints.get(0), reductionPoints.get(0) };

        finalPairs.add(pair);

      }
      else {

        ArrayList<Integer> solution = Utils.solve(tempArray, cost);

        System.out.println("Solution:");

        for (Integer index: solution) {
          System.out.println(Arrays.toString(tempArray[index])
                             + " Similarity: " + input.get(tempArray[index])
                             + " Distance: "
                             + (1 / input.get(tempArray[index])));

          ArrayList<PointOfInterest[]> newFinalPairs =
            Utils.createFinalPairs(temp, tempArray[index]);
          finalPairs.addAll(newFinalPairs);
        }
      }

      temp.clear();
      input.clear();
      risingPoints.clear();
      reductionPoints.clear();
    }
  }

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

    Collections.sort(finalPairs, Constants.comp2);

  }

}
