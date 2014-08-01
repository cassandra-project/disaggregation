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

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

import org.apache.log4j.Logger;

/**
 * This class is used for parsing the power consumption measurements and storing
 * them to separate arrays in order to analyse closely. There is no error
 * checking mechanism included in this class since the Training Module has
 * already one implemented that will not allow for broken data sets to reach
 * this class.
 * 
 * @author Antonios Chrysopoulos
 * @version 0.9, Date: 29.07.2013
 */
public class PowerDatasets
{
  static Logger log = Logger.getLogger(PowerDatasets.class);

  /**
   * This is an array of the active power measurements contained in the
   * measurement file.
   */
  ArrayList<Double> activePower = new ArrayList<Double>();

  /**
   * This is an array of the reactive power measurements contained in the
   * measurement file.
   */
  ArrayList<Double> reactivePower = new ArrayList<Double>();

  ArrayList<Integer> days = new ArrayList<Integer>();

  ArrayList<Double> temp = new ArrayList<Double>();

  Map<Double, Integer> counter = new HashMap<Double, Integer>();

  /**
   * This is the constructor function of the class. It takes the measurement
   * file and fills the array variables.
   * 
   * @param filename
   *          The name of the file that is imported from the user.
   * @throws FileNotFoundException
   */
  public PowerDatasets (String filename, boolean timestamps)
    throws FileNotFoundException
  {
    // Since we don't know at first if the data sets are compatible (starting at
    // the same time) as well as how large they are, we use maps to store the
    // data.
    Map<Integer, Double> activeMap = new TreeMap<Integer, Double>();
    Map<Integer, Double> reactiveMap = new TreeMap<Integer, Double>();

    // Then the parsing through is in order, storing the variables in the
    // respective maps.
    File file = new File(filename);
    Scanner input = new Scanner(file);
    String[] contents = null;

    int counter = 0;
    int power = -1;

    while (input.hasNext()) {

      String line = input.next();

      // System.out.println(line);

      contents = line.split(",");

      if (contents.length == 2 || contents.length == 3)
        power = 1;

      if (power == 1) {

        if (Double.parseDouble(contents[power]) > 0)
          activeMap.put(counter, Double.parseDouble(contents[power]));
        else
          activeMap.put(counter, 0.0);

        if (contents.length == 3)
          reactiveMap.put(counter, Double.parseDouble(contents[power + 1]));
        else
          reactiveMap.put(counter, 0.0);

        counter++;
      }
      else {
        System.out
                .println("Problem with dataset. Check for erroneous data formulation.");
        System.exit(0);
      }
    }

    input.close();

    log.info("================WEEKS SETTING==================");
    log.info("Counter:" + activeMap.size());

    int numOfDays = activeMap.size() / Constants.MINUTES_PER_DAY;

    log.info("Days: " + numOfDays);

    ArrayList<Double> active = new ArrayList<Double>();

    for (Integer index: activeMap.keySet())
      active.add(activeMap.get(index));

    // Utils.createLineDiagram("00_Before Cleaning", "Minute", "Power", active);

    active.clear();

    int weeks =
      (int) Math
              .floor((double) counter
                     / (double) (Constants.MINUTES_PER_DAY * Constants.DAYS_PER_WEEK));

    Constants.setWeeks(weeks);

    log.info("Weeks:" + Constants.WEEKS);

    for (int i = 0; i < numOfDays; i++) {

      if (checkDay(i, activeMap) == false
          || Constants.CLEANING_DATASET == false)
        days.add(i);

    }

    log.info("Kept Days: " + days.toString());

    int start = -1;
    int end = -1;

    for (Integer day: days) {

      start = day * Constants.MINUTES_PER_DAY;
      end = (day + 1) * Constants.MINUTES_PER_DAY;

      for (int i = start; i < end; i++) {
        activePower.add(activeMap.get(i));
        reactivePower.add(reactiveMap.get(i));
      }

    }

    log.info("");

    // if (Constants.CLEANING_DATASET)
    // Utils.createLineDiagram("01_After Cleaning", "Minute", "Power",
    // activePower);

    if (Constants.NORMALIZING_DATASET) {
      normalizeData(activePower);

      // Utils.createLineDiagram("02_After Normalizing", "Minute", "Power",
      // activePower);
    }

    log.info("================POWER MEASUREMENTS==================");
    log.info(activePower.toString());
    log.info(reactivePower.toString());
    // log.info(activePower.subList(1710, 1800).toString());
    log.info("");
    log.info("");

    days.clear();
    activeMap.clear();
    reactiveMap.clear();

  }

  private boolean checkDay (int index, Map<Integer, Double> activeMap)
  {
    boolean flag = false;

    int start = index * Constants.MINUTES_PER_DAY;
    int end = (index + 1) * Constants.MINUTES_PER_DAY;

    for (int i = start; i < end; i++) {

      if (counter.containsKey(activeMap.get(i)))
        counter.put(activeMap.get(i), counter.get(activeMap.get(i)) + 1);
      else
        counter.put(activeMap.get(i), 1);

      if (counter.get(activeMap.get(i)) > Constants.REMOVAL_THRESHOLD) {

        log.debug("Day: " + index + " Value: " + activeMap.get(i));

        flag = true;
        break;

      }

    }

    temp.clear();
    counter.clear();

    return flag;
  }

  private void normalizeData (ArrayList<Double> dataset)
  {

    double mean = Utils.estimateMean(dataset);
    double maxThreshold = 0.9 * Utils.findMax(dataset);
    Map<Double, Double> percentages = Utils.estimateCumulativeValues(dataset);

    for (int i = 0; i < dataset.size(); i++) {
      if (percentages.get(dataset.get(i)) < Constants.NORMALIZING_THRESHOLD
          && dataset.get(i) > maxThreshold) {
        // System.out.println("Value: " + dataset.get(i) + " Percentage: "
        // + percentages.get(dataset.get(i)));
        dataset.set(i, mean);
      }
    }
  }

  /**
   * This function is used as a getter for the active power array variable of
   * the power data set.
   * 
   * @return active power array.
   */
  public double[] getActivePower ()
  {

    double[] result = new double[activePower.size()];

    for (int i = 0; i < activePower.size(); i++)
      result[i] = activePower.get(i);

    return result;
  }

  /**
   * This function is used as a getter for the reactive power array variable of
   * the power data set.
   * 
   * @return reactive power array.
   */
  public double[] getReactivePower ()
  {
    double[] result = new double[reactivePower.size()];

    for (int i = 0; i < reactivePower.size(); i++)
      result[i] = reactivePower.get(i);

    return result;
  }

}
