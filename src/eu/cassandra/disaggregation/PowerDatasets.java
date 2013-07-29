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

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

public class PowerDatasets
{

  double[] activePower;
  double[] reactivePower;
  Map<Integer, Double> activeMap = new TreeMap<Integer, Double>();
  Map<Integer, Double> reactiveMap = new TreeMap<Integer, Double>();

  public PowerDatasets (String filename) throws FileNotFoundException
  {

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

    activePower = new double[activeMap.size()];
    reactivePower = new double[activeMap.size()];
    int counter = 0;

    for (Integer timestamp: activeMap.keySet()) {

      activePower[counter] = activeMap.get(timestamp);
      reactivePower[counter] = reactiveMap.get(timestamp);

      counter++;
    }

    // System.out.println(Arrays.toString(activePower));
    // System.out.println(Arrays.toString(reactivePower));

  }

  public double[] getActivePower ()
  {
    return activePower;
  }

  public double[] getReactivePower ()
  {
    return reactivePower;
  }

  public double getActivePower (int index)
  {
    return activePower[index];
  }

  public double getReactivePower (int index)
  {
    return reactivePower[index];
  }
}
