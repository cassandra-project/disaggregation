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

import eu.cassandra.utils.Constants;

/**
 * This is an of the most important classes of the Disaggregation Module. Event
 * Detector is response for taking the consumption measurements and identifying
 * the underlying consumption events from all the appliances that are installed
 * in the installation.
 * 
 * @author Antonios Chrysopoulos
 * @version 0.7, Date: 29.07.2013
 */
public class EventDetector
{

  /**
   * This is the threshold which must be passed for considering summary of the
   * power consumption normal.
   */
  double eventThreshold = 0;

  /**
   * Simple empty constructor of the Event Detector object
   */
  public EventDetector ()
  {
  }

  /**
   * This is the main function that takes the two power measurement arrays and
   * extracts the events detected within them.
   * 
   * @param activePower
   *          The array of active power measurements.
   * @param reactivePower
   *          The array of reactive power measurements.
   * @return
   */
  public ArrayList<Event> detectEvents (double[] activePower,
                                        double[] reactivePower)
  {
    // Initializing the auxiliary variables.
    ArrayList<Event> events = new ArrayList<Event>();
    boolean started = false;
    boolean check = true;
    int start = -1;
    int end = -1;

    // Defining the threshold as the minimum value plus the estimated threshold
    // of the installation type.
    eventThreshold = findMin(activePower) + estimateThreshold();

    // For each minute of measurements
    for (int i = 0; i < activePower.length; i++) {

      // If the active power surpasses the threshold and an event hasn't started
      // yet, then flag the start of an event.
      if (activePower[i] >= eventThreshold && started == false) {
        start = i - 1;
        started = true;
      }

      // If the active power goes below the threshold and an event has started
      // yet, then this may be the end of an event.
      if (activePower[i] < eventThreshold && started == true) {

        boolean flag = true;

        // Checking if the measurements do not pass the event threshold after a
        // small period of time, which would mean that the event has actually
        // not finished.
        int endingIndex =
          Math.min(i + Constants.EVENT_TIME_LIMIT, activePower.length);
        for (int j = i + 1; j < endingIndex; j++) {

          if (activePower[j] > eventThreshold) {

            flag = false;
            // System.out.println("Index: " + j + " Value: " + activePower[j]
            // + " Flag: " + flag);
            break;
          }
        }

        // If the event is over
        if (flag) {
          end = i;

          // System.out.println("Start: " + start + " End: " + end);

          // If the event has not began before the start of the measurements
          if (start != -1) {

            // Doing the check with the summary of active power.
            check =
              checkMeanConsumption(Arrays.copyOfRange(activePower, start,
                                                      end + 1));

            // If the check is passed then an event is created and added to the
            // event list.
            if (check) {
              Event temp =
                new Event(start, end, Arrays.copyOfRange(activePower, start,
                                                         end + 1),
                          Arrays.copyOfRange(reactivePower, start, end + 1));

              events.add(temp);
              // if (i > 200)
              // break;
            }
          }

          start = -1;
          end = -1;
          started = false;
        }
      }

    }

    return events;

  }

  /**
   * This is an auxiliary function used for searching over an array of active
   * power measurements for the smallest value available.
   * 
   * @param activePower
   *          The array of active power measurements.
   * @return the smallest value found.
   */
  private double findMin (double[] activePower)
  {

    double min = Double.POSITIVE_INFINITY;

    for (int i = 0; i < activePower.length; i++)
      if (min > activePower[i])
        min = activePower[i];

    return min;

  }

  /**
   * This is an auxiliary function helping to threshold of the event detection
   * algorithm and is depending on the installation type (Household, Commercial,
   * Factory etc.).
   * 
   * @return the threshold above which a consumption event has started.
   */
  private double estimateThreshold ()
  {
    return Constants.HOUSEHOLD_BACKGROUND_THRESHOLD;
  }

  /**
   * This is an auxiliary function checking if the active power consumption
   * array of an event is large enough to suggest an actual event or not.
   * 
   * @param active
   *          The array of active power consumption under consideration.
   * @return true if the check is passed, false otherwise.
   */
  private boolean checkMeanConsumption (double[] active)
  {

    boolean result = true;

    double sum = 0;

    for (int i = 0; i < active.length; i++)
      sum += active[i];

    sum /= active.length;

    if (sum < eventThreshold)
      result = false;

    return result;

  }
}
