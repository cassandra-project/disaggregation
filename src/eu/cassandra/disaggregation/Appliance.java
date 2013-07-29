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
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class Appliance
{
  private String name = "";
  private String activity = "";
  private Map<Integer, ArrayList<PointOfInterest>> risingPoints =
    new TreeMap<Integer, ArrayList<PointOfInterest>>();
  private Map<Integer, ArrayList<PointOfInterest>> reductionPoints =
    new TreeMap<Integer, ArrayList<PointOfInterest>>();
  private Map<Integer, ArrayList<PointOfInterest[]>> matchingPoints =
    new TreeMap<Integer, ArrayList<PointOfInterest[]>>();
  private double[] meanValuesSum = new double[2];
  private int numberOfMatchingPoints = 0;
  private double meanDuration = 0;

  public Appliance (String name, String activity)
  {
    this.name = name;
    this.activity = activity;
    risingPoints.clear();
    reductionPoints.clear();
    matchingPoints.clear();
  }

  public Appliance (String name, String activity,
                    Map<Integer, ArrayList<PointOfInterest>> rising,
                    Map<Integer, ArrayList<PointOfInterest>> reduction)
  {
    this.name = name;
    this.activity = activity;
    risingPoints = rising;
    reductionPoints = reduction;
    if (activity.equalsIgnoreCase("Refrigeration"))
      calculateMetrics();

  }

  public Map<Integer, ArrayList<PointOfInterest>> getRisingPoints ()
  {
    return risingPoints;
  }

  public ArrayList<PointOfInterest> getRisingPoints (int index)
  {
    return risingPoints.get(index);
  }

  public Map<Integer, ArrayList<PointOfInterest>> getReductionPoints ()
  {
    return reductionPoints;
  }

  public ArrayList<PointOfInterest> getReductionPoints (int index)
  {
    return reductionPoints.get(index);
  }

  public Map<Integer, ArrayList<PointOfInterest[]>> getMatchingPoints ()
  {
    return matchingPoints;
  }

  public ArrayList<PointOfInterest[]> getMatchingPoints (int index)
  {
    return matchingPoints.get(index);
  }

  public double getMeanActive ()
  {
    return meanValuesSum[0] / numberOfMatchingPoints;
  }

  public double getMeanReactive ()
  {
    return meanValuesSum[1] / numberOfMatchingPoints;
  }

  public double[] getMeanValues ()
  {
    double[] temp = { getMeanActive(), getMeanReactive() };
    return temp;
  }

  public void addRisingPoint (int event, PointOfInterest rising)
  {
    if (risingPoints.containsKey(event))
      risingPoints.get(event).add(rising);
    else {
      ArrayList<PointOfInterest> temp = new ArrayList<PointOfInterest>();
      temp.add(rising);
      risingPoints.put(event, temp);
    }
  }

  public void addReductionPoint (int event, PointOfInterest reduction)
  {
    if (reductionPoints.containsKey(event))
      reductionPoints.get(event).add(reduction);
    else {
      ArrayList<PointOfInterest> temp = new ArrayList<PointOfInterest>();
      temp.add(reduction);
      reductionPoints.put(event, temp);
    }
  }

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
      System.out.println("Mean Duration: " + meanDuration);
    System.out.println("Mean Power: " + getMeanActive());
    System.out.println("Mean Reactive: " + getMeanReactive());
  }

  private void calculateMetrics ()
  {
    int counter = 0;

    Set<Integer> keys = new TreeSet<Integer>();
    keys.addAll(risingPoints.keySet());
    keys.addAll(reductionPoints.keySet());

    for (Integer key: keys) {

      if (risingPoints.containsKey(key) && reductionPoints.containsKey(key)
          && risingPoints.get(key).size() == 1
          && risingPoints.get(key).size() == reductionPoints.get(key).size()) {

        meanValuesSum[0] +=
          risingPoints.get(key).get(0).getPDiff()
                  - reductionPoints.get(key).get(0).getPDiff();
        meanValuesSum[1] +=
          risingPoints.get(key).get(0).getQDiff()
                  - reductionPoints.get(key).get(0).getQDiff();
        meanDuration +=
          reductionPoints.get(key).get(0).getMinute()
                  - risingPoints.get(key).get(0).getMinute();
        PointOfInterest[] temp =
          { risingPoints.get(key).get(0), reductionPoints.get(key).get(0) };

        ArrayList<PointOfInterest[]> tempArray =
          new ArrayList<PointOfInterest[]>();
        tempArray.add(temp);
        matchingPoints.put(key, tempArray);
        numberOfMatchingPoints += 2;
        counter++;
        risingPoints.remove(key);
        reductionPoints.remove(key);

      }
    }

    meanDuration /= counter;

    keys.clear();
    int duration = 0;

    for (Integer key: risingPoints.keySet()) {

      if (reductionPoints.containsKey(key)) {

        for (int i = risingPoints.get(key).size() - 1; i >= 0; i--) {

          for (PointOfInterest red: reductionPoints.get(key)) {

            if (red.getMinute() > risingPoints.get(key).get(i).getMinute()) {

              duration =
                red.getMinute() - risingPoints.get(key).get(i).getMinute();

              // System.out.println("Start: "
              // + risingPoints.get(key).get(i).getMinute()
              // + " Stop: " + red.getMinute() + " Duration: "
              // + duration);

              if (Utils.checkLimit(duration, meanDuration)) {

                PointOfInterest[] temp = { risingPoints.get(key).get(i), red };

                if (matchingPoints.containsKey(key)) {
                  matchingPoints.get(key).add(temp);
                }
                else {
                  ArrayList<PointOfInterest[]> tempArray =
                    new ArrayList<PointOfInterest[]>();
                  tempArray.add(temp);
                  matchingPoints.put(key, tempArray);
                }
                addMeanValues(temp);
                numberOfMatchingPoints += 2;
                reductionPoints.get(key).remove(red);
                risingPoints.get(key).remove(i);
                break;
              }
            }
          }
        }
      }
    }

    risingPoints.clear();
    reductionPoints.clear();

  }

  public void addMatchingPoints (int eventIndex, PointOfInterest[] pois)
  {

    if (!matchingPoints.containsKey(eventIndex))
      matchingPoints.put(eventIndex, new ArrayList<PointOfInterest[]>());

    matchingPoints.get(eventIndex).add(pois);
    addMeanValues(pois);

  }

  public void addMeanValues (PointOfInterest[] pois)
  {
    meanValuesSum[0] += pois[0].getPDiff() - pois[1].getPDiff();
    meanValuesSum[1] += pois[0].getQDiff() - pois[1].getQDiff();
    numberOfMatchingPoints += 2;
  }

  public ArrayList<String[]> matchingPairsToString (ArrayList<Event> events)
  {

    ArrayList<String[]> result = new ArrayList<String[]>();
    int offset = 0;
    for (Integer key: matchingPoints.keySet()) {

      offset = events.get(key).getStartMinute();
      for (PointOfInterest[] pois: matchingPoints.get(key)) {
        String[] tempString =
          { Integer.toString(offset + pois[0].getMinute()),
           Integer.toString(offset + pois[1].getMinute()),
           Double.toString(pois[0].getPDiff()),
           Double.toString(pois[0].getQDiff()),
           Double.toString(pois[1].getPDiff()),
           Double.toString(pois[1].getQDiff()), activity, name };
        result.add(tempString);
      }
    }
    // for (String[] string: result)
    // System.out.println(Arrays.toString(string));
    return result;
  }

  public boolean isClose (double[] mean, int duration)
  {

    double[] meanValues = { getMeanActive(), getMeanReactive() };

    if (activity.equalsIgnoreCase("Refrigeration"))
      return (Utils.percentageEuclideanDistance(mean, meanValues) < Constants.PERCENTAGE_CLOSENESS_THRESHOLD || Utils
              .absoluteEuclideanDistance(mean, meanValues) < Constants.ABSOLUTE_CLOSENESS_THRESHOLD
                                                                                                                && Utils.checkLimit(duration,
                                                                                                                                    meanDuration));
    else

      return (Utils.percentageEuclideanDistance(mean, meanValues) < Constants.PERCENTAGE_CLOSENESS_THRESHOLD || Utils
              .absoluteEuclideanDistance(mean, meanValues) < Constants.ABSOLUTE_CLOSENESS_THRESHOLD);

  }
}
