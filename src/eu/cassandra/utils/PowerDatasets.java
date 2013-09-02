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
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

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
  /**
   * This is an array of the active power measurements contained in the
   * measurement file.
   */
  double[] activePower;

  /**
   * This is an array of the reactive power measurements contained in the
   * measurement file.
   */
  double[] reactivePower;

  /**
   * This is the constructor function of the class. It takes the measurement
   * file and fills the array variables.
   * 
   * @param filename
   *          The name of the file that is imported from the user.
   * @throws FileNotFoundException
   */
  public PowerDatasets (String filename) throws FileNotFoundException
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

    while (input.hasNext()) {

      String line = input.next();

      // System.out.println(line);

      String[] contents = line.split(",");

      activeMap.put(Integer.parseInt(contents[0]),
                    Double.parseDouble(contents[1]));
      reactiveMap.put(Integer.parseInt(contents[0]),
                      Double.parseDouble(contents[2]));

    }

    input.close();

    // Now that the size is known the data sets are stored in arrays.
    activePower = new double[activeMap.size()];
    reactivePower = new double[reactiveMap.size()];

    int counter = 0;

    // Adding the values in the arrays, one by one.
    for (Integer timestamp: activeMap.keySet()) {

      activePower[counter] = activeMap.get(timestamp);
      reactivePower[counter] = reactiveMap.get(timestamp);

      counter++;
    }

    // Cleaning the unnecessary maps.
    activeMap.clear();
    reactiveMap.clear();

    // System.out.println(Arrays.toString(activePower));
    // System.out.println(Arrays.toString(reactivePower));

  }

  /**
   * This function is used as a getter for the active power array variable of
   * the power data set.
   * 
   * @return active power array.
   */
  public double[] getActivePower ()
  {
    return activePower;
  }

  /**
   * This function is used as a getter for the reactive power array variable of
   * the power data set.
   * 
   * @return reactive power array.
   */
  public double[] getReactivePower ()
  {
    return reactivePower;
  }

}
