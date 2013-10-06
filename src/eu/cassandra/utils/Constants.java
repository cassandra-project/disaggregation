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

public class Constants
{
  public static Comparator<PointOfInterest> comp =
    new Comparator<PointOfInterest>() {
      public int compare (PointOfInterest poi1, PointOfInterest poi2)
      {
        return Integer.compare(poi1.getMinute(), poi2.getMinute());
      }
    };

  public static Comparator<PointOfInterest[]> comp2 =
    new Comparator<PointOfInterest[]>() {
      public int compare (PointOfInterest[] poi1, PointOfInterest[] poi2)
      {
        if (poi1[0].getMinute() != poi2[0].getMinute())
          return Integer.compare(poi1[0].getMinute(), poi2[0].getMinute());
        else
          return Integer.compare(poi1[1].getMinute(), poi2[1].getMinute());
      }
    };

  public static Comparator<String[]> comp3 = new Comparator<String[]>() {
    public int compare (String[] activity1, String[] activity2)
    {
      if (Integer.parseInt(activity1[0]) != Integer.parseInt(activity2[0]))
        return Integer.compare(Integer.parseInt(activity1[0]),
                               Integer.parseInt(activity2[0]));
      else
        return Integer.compare(Integer.parseInt(activity1[1]),
                               Integer.parseInt(activity2[1]));
    }
  };

  public static Comparator<PointOfInterest> comp4 =
    new Comparator<PointOfInterest>() {
      public int compare (PointOfInterest poi1, PointOfInterest poi2)
      {
        return Double.compare(-Math.abs(poi1.getPDiff()),
                              -Math.abs(poi2.getPDiff()));
      }
    };

  public static Comparator<ArrayList<PointOfInterest>> comp5 =
    new Comparator<ArrayList<PointOfInterest>>() {
      public int compare (ArrayList<PointOfInterest> poi1,
                          ArrayList<PointOfInterest> poi2)
      {
        return Double.compare(-Math.abs(poi1.get(0).getPDiff()),
                              -Math.abs(poi2.get(0).getPDiff()));
      }
    };

  // GENERAL CONSTANTS
  public static final double ERROR_FRINGE = 0.05;
  public static final double ERROR_FRIDGE = 0.4;
  public static final double PAIR_ERROR_FRINGE = 0.2;

  // ID CONSTANTS

  public static int EVENTS_ID = 1;
  public static int POI_ID = 1;
  public static int WATERHEATER_ID = 1;
  public static int LIGHTING_ID = 1;
  public static int ENTERTAINMENT_ID = 1;
  public static int COOKING_ID = 1;
  public static int MICROWAVE_OVEN_ID = 1;
  public static int EXTRACTOR_ID = 1;
  public static int VACUUM_CLEANER_ID = 1;

  // BACKGROUND CONSTANTS

  public static final int HOUSEHOLD_BACKGROUND_THRESHOLD = 30;
  public static final int COMMERCIAL_BACKGROUND_THRESHOLD = 30;
  public static final int HOUSEHOLD_FINE_TUNING_THRESHOLD = 50;

  // POINTS OF INTEREST LIMIT CONSTANTS

  public static final int EVENT_TIME_LIMIT = 5;
  public static final int DERIVATIVE_LIMIT = 5;
  public static final int RISING_LIMIT = 3;
  public static final int REDUCTION_LIMIT = 3;

  // THRESHOLD CONSTANTS

  public static final int DIFFERENCE_LIMIT = 5;
  public static final int OLD_DIFFERENCE_LIMIT = 15;
  public static final int DEFAULT_THRESHOLD = 10;

  // ISOLATED APPLIANCE EXTRACTOR CONSTANTS

  public static final int ISOLATED_TIMES_UP = 3;
  public static final int MAX_CLUSTERS_NUMBER = 5;
  public static final int KMEANS_LIMIT_NUMBER = 10;

  // REFRIGERATOR CONSTANTS

  public static final double REF_THRESHOLD = 10;

  // WASHING MACHINE CONSTANTS

  public static final int WASHING_MACHINE_POWER_THRESHOLD = 1500;
  public static final int WASHING_MACHINE_MERGING_MINUTE_LIMIT = 4;
  public static final int WASHING_MACHINE_NUMBER_OF_MINUTES_LIMIT = 30;
  public static final int WASHING_MACHINE_DEVIATION_LIMIT = 20;

  // SWITCHING POINTS CONSTANTS
  public static final int SWITCHING_THRESHOLD = 25;

  // CLOSE MATCHING CONSTANTS
  public static final int CLOSENESS_THRESHOLD = 5;

  // CLUSTER LARGE EVENTS CONSTANTS

  public static final int CONCENTRATION_THRESHOLD = 50;
  public static final int CLUSTER_THRESHOLD = 10;

  // SHAPES MATCHING CONSTANTS
  public static final int CHAIR_DISTANCE_THRESHOLD = 10;
  public static final int INVERSED_CHAIR_DISTANCE_THRESHOLD = 10;
  public static final int TRIANGLE_DISTANCE_THRESHOLD = 10;
  public static final int RECTANGLE_DISTANCE_THRESHOLD = 10;

  // INTEGER PROGRAMMING CONSTANTS
  public static final int DISTANCE_THRESHOLD = 60;
  public static final int SECOND_DISTANCE_THRESHOLD = 60;
  public static final double NEAR_ZERO = 1.0E-6;
  public static final int MAX_POINTS_LIMIT = 4;

  public static final int SOLUTION_THRESHOLD_UNDER_10 = 20000000;
  public static final int SOLUTION_THRESHOLD_UNDER_15 = 20000000;
  public static final int SOLUTION_THRESHOLD_UNDER_20 = 20000;
  public static final int SOLUTION_THRESHOLD_UNDER_25 = 10000;
  public static final int SOLUTION_THRESHOLD_OVER_25 = 2000;
  public static final int OTHER_SOLUTION_THRESHOLD = 50000;
  public static final int REMOVAL_PERCENTAGE = 20;
  public static final int MAX_POINTS_OF_INTEREST = 15;

  // APPLIANCES THRESHOLDS
  public static final int WATERHEATER_POWER_THRESHOLD = 3000;
  public static final int TINY_POWER_THRESHOLD = 30;
  public static final int SMALL_POWER_THRESHOLD = 250;
  public static final int MEDIUM_POWER_THRESHOLD = 300;
  public static final int REACTIVE_THRESHOLD = 50;
  public static final int PERCENTAGE_CLOSENESS_THRESHOLD = 10;
  public static final int ABSOLUTE_CLOSENESS_THRESHOLD = 30;
  public static final int MICROWAVE_DURATION = 5;
}
