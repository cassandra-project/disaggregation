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

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;

import eu.cassandra.appliance.ApplianceIdentifier;
import eu.cassandra.appliance.IsolatedApplianceExtractor;
import eu.cassandra.event.Event;
import eu.cassandra.event.EventDetector;
import eu.cassandra.utils.PowerDatasets;
import eu.cassandra.utils.Utils;

/**
 * This is the main class that is used for implementing the Disaggregation
 * procedure which is incorporated in the Training Module. The procedure
 * extracts two files that contain the appliances detected from the installation
 * measurements as well as the consumption events for each appliance /
 * activity.The results are appearing in the respective lists in the Training
 * Module GUI.
 * 
 * @author Antonios Chrysopoulos
 * @version 0.7, Date: 29.07.2013
 */

public class Disaggregate
{

  /**
   * This variable is a list of the events as been extracted from the
   * measurements file.
   */
  static ArrayList<Event> events = new ArrayList<Event>();

  /**
   * This is the constructor function of the Disaggregation class.
   * 
   * @param input
   *          The filename of the input
   * @throws Exception
   */
  public Disaggregate (String input) throws Exception
  {

    String inputPrefix = Utils.getFileName(input);

    initDisaggregation(input, inputPrefix + "ApplianceList.csv",
                       inputPrefix + "ActivityList.csv");
  }

  /**
   * This is the disaggregation function of the Disaggregation class.
   * 
   * @param filename
   *          The file name of the consumption measurements of an installation.
   * @param outputAppliance
   *          The file name of the output file containing the appliances.
   * @param outputActivity
   *          The file name of the output file containing the activities.
   * @throws Exception
   */
  public void initDisaggregation (String filename, String outputAppliance,
                                  String outputActivity) throws Exception
  {
    // Adding a file as a second output that will help keep track of the
    // procedure.
    PrintStream realSystemOut = System.out;
    try {
      OutputStream output = new FileOutputStream("Demo/test.txt");
      PrintStream printOut = new PrintStream(output);
      System.setOut(printOut);
    }
    catch (FileNotFoundException e) {
      e.printStackTrace();
    }

    // MultiOutputStream multiOut = new MultiOutputStream(System.out, fout);
    // PrintStream stdout = new PrintStream(multiOut);
    // System.setOut(stdout);

    // Creating the data sets under investigation.
    PowerDatasets data = new PowerDatasets(filename);

    // Initialize the auxiliary variables
    EventDetector ed = new EventDetector();
    ApplianceIdentifier ai = new ApplianceIdentifier();

    // Run the event detector in order to find the possible events in the
    // data
    events = ed.detectEvents(data.getActivePower(), data.getReactivePower());

    // The Isolated Appliance Extractor helps the procedure of finding the
    // refrigerator and washing machine amongst others.
    IsolatedApplianceExtractor iso = new IsolatedApplianceExtractor(events);

    ai.refrigeratorIdentification(events, iso);

    ai.washingMachineIdentification(events);

    // For each event an analysis is at hand helping to separate the different
    // consumption models and identify their results
    for (Event event: events) {
      if (event.getWashingMachineFlag() == false) {
        event.detectMatchingPoints();
        event.detectSwitchingPoints();
        event.detectClusters();
        event.detectBasicShapes();
        event.findCombinations();
        event.status2();
        event.calculateFinalPairs();
        event.status2();
        if (event.getFinalPairs().size() > 0)
          ai.analyseEvent(event);
      }
    }

    ai.createDisaggregationFiles(outputAppliance, outputActivity, events);

    System.setOut(realSystemOut);

    // The extracted appliances are printed to see the results of the procedure
    // for (Appliance appliance: ai.getApplianceList())
    // appliance.status();

  }

  public static void main (String[] args) throws Exception
  {
    // String input = "Demo/Household1.csv";
    String input = "Demo/Milioudis.csv";
    // String input = "Demo/Benchmark.csv";

    Disaggregate dis = new Disaggregate(input);
  }
}
