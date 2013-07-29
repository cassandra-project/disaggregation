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
import java.io.PrintStream;
import java.util.ArrayList;

public class Disaggregate
{
  ArrayList<Event> events = new ArrayList<Event>();

  public Disaggregate (String filename) throws Exception
  {
    FileOutputStream fout = null;
    try {
      fout = new FileOutputStream("Demo/test.txt");
    }
    catch (FileNotFoundException e) {
      e.printStackTrace();
    }

    MultiOutputStream multiOut = new MultiOutputStream(System.out, fout);
    PrintStream stdout = new PrintStream(multiOut);
    System.setOut(stdout);

    PowerDatasets data = new PowerDatasets(filename);

    EventDetector ed = new EventDetector();
    ApplianceIdentified ai = new ApplianceIdentified();

    events = ed.detectEvents(data.getActivePower(), data.getReactivePower());

    IsolatedApplianceExtractor iso = new IsolatedApplianceExtractor(events);

    ai.refrigeratorIdentification(events, iso);

    ai.washingMachineIdentification(events);

    for (Event event: events) {
      if (event.getWashingMachineFlag() == false) {
        event.detectMatchingPoints();
        event.detectSwitchingPoints();
        event.detectClusters();
        event.detectBasicShapes();
        event.createCombinations();
        // event.status2();
        event.calculateFinalPairs();
        event.status2();
        if (event.getFinalPairs().size() > 0)
          ai.analyseEvent(event);
      }
    }

    for (Appliance appliance: ai.getApplianceList())
      appliance.status();

  }

  public static void main (String[] args) throws Exception
  {
    Disaggregate dis = new Disaggregate("Demo/measurements.csv");

  }
}
