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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;

import org.apache.log4j.Logger;

import eu.cassandra.appliance.Appliance;
import eu.cassandra.appliance.ApplianceIdentifier;
import eu.cassandra.appliance.IsolatedEventsExtractor;
import eu.cassandra.event.Event;
import eu.cassandra.event.EventDetector;
import eu.cassandra.utils.Constants;
import eu.cassandra.utils.PointOfInterest;
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

  static Logger log = Logger.getLogger(Disaggregate.class);

  /**
   * This variable is a list of the events as been extracted from the
   * measurements file.
   */
  static ArrayList<Event> events = new ArrayList<Event>();

  /**
   * This variable is implementing the event detector of the power dataset.
   */
  static EventDetector ed = null;

  /**
   * This variable is implementing the event detector of the power dataset.
   */
  static ApplianceIdentifier ai = null;

  /**
   * The Isolated Appliance Extractor helps the procedure of finding
   * the refrigerator and washing machine amongst others.
   * 
   */
  IsolatedEventsExtractor iso = null;

  /**
   * This is the configuration file that will be utilized to pass the parameters
   * that can be adjusted by user
   */
  Properties configuration = new Properties();

  /**
   * This is the configuration file name.
   */
  private String configFile = "Disaggregation.properties";

  /**
   * This is the constructor function of the Disaggregation class.
   * 
   * @param input
   *          The filename of the input
   * @throws Exception
   */
  public Disaggregate (String folder, String input, String... configFile)
    throws Exception
  {
    String inputPrefix =
      Constants.resultFolder + input.substring(0, input.length() - 4);

    if (configFile.length >= 1)
      this.configFile = configFile[0];

    if (configFile.length == 2)
      inputPrefix = configFile[1] + input.substring(0, input.length() - 4);

    long tStart = System.currentTimeMillis();

    log.info("");
    log.info("==============FILE SETTING====================");
    log.info("Folder: " + folder);
    log.info("Filename: " + input);
    log.info("Input: " + folder + input);
    log.info("Input prefix: " + inputPrefix);
    log.info("");
    log.info("");

    FileInputStream cfgFile = new FileInputStream(this.configFile);
    try {
      configuration.load(cfgFile);
      cfgFile.close();
    }
    catch (IOException e) {
      e.printStackTrace();
      return;
    }

    String type = configuration.getProperty("ApplianceType");
    boolean loose =
      Boolean.parseBoolean(configuration.getProperty("LooseCoupling"));
    boolean fridgeLoose =
      Boolean.parseBoolean(configuration.getProperty("FridgeLooseCoupling"));
    boolean washingMachineDetection =
      Boolean.parseBoolean(configuration.getProperty("WashingMachineDetection"));
    String temp = configuration.getProperty("CleaningPointsOfInterest");
    boolean medianThreshold =
      Boolean.parseBoolean(configuration.getProperty("MedianThreshold"));
    int clusterPoints =
      Integer.parseInt(configuration.getProperty("PointsPerCluster"));
    int combinationPoints =
      Integer.parseInt(configuration.getProperty("CombinationPoints"));

    String timeThresholdComplexity =
      configuration.getProperty("TimeThresholdComplexity");

    int timeThreshold =
      Integer.parseInt(configuration.getProperty("TimeThreshold"));

    boolean removeLargeEvents =
      Boolean.parseBoolean(configuration.getProperty("RemoveLargeEvents"));

    int largeEventThreshold =
      Integer.parseInt(configuration.getProperty("LargeEventThreshold"));

    boolean cleaning;
    double thres;
    if (temp.equalsIgnoreCase("Automatic")) {
      cleaning = true;
      thres = 0;
    }
    else {
      cleaning = false;
      thres =
        Double.parseDouble(configuration.getProperty("CleaningThreshold"));
    }

    boolean cleanDataset =
      Boolean.parseBoolean(configuration.getProperty("CleanDataset"));

    boolean normalizeDataset =
      Boolean.parseBoolean(configuration.getProperty("NormalizeDataset"));

    log.info("==============CONFIGURATION====================");
    log.info("Appliance Type: " + type);
    log.info("Loose Coupling: " + loose);
    log.info("Fridge Loose Coupling: " + fridgeLoose);
    log.info("Washing Machine Detection: " + washingMachineDetection);
    log.info("Automatic Cleaning POIs: " + cleaning);
    log.info("Cleaning POIs Threshold: " + thres);
    log.info("Median Threshold: " + medianThreshold);
    log.info("Points Per Cluster: " + clusterPoints);
    log.info("Combination Points Per Cluster: " + combinationPoints);
    log.info("Time Threshold Type: " + timeThresholdComplexity);
    if (timeThresholdComplexity.equalsIgnoreCase("Simple"))
      log.info("Time Threshold: " + timeThreshold);
    log.info("Remove Large Events: " + removeLargeEvents);
    if (removeLargeEvents)
      log.info("Large Events Threshold: " + largeEventThreshold);
    log.info("Clean Dataset: " + cleanDataset);
    log.info("Normalize Dataset: " + normalizeDataset);

    log.info("");
    log.info("");

    Constants.setApplianceType(type);

    Constants.setClusterAppliances(loose);

    Constants.setFridgeCoupling(fridgeLoose);

    Constants.setWashingMachineDetection(washingMachineDetection);

    Constants.setCleaningConstants(cleaning, thres);

    Constants.setMedianThreshold(medianThreshold);

    Constants.setPointsPerCluster(clusterPoints);

    Constants.setCombinationPointsPerCluster(combinationPoints);

    Constants.setCleaningDataset(cleanDataset);

    Constants.setNormalizingDataset(normalizeDataset);

    if (timeThresholdComplexity.equalsIgnoreCase("Complex"))
      Constants.setTimeThresholdComplexity(false);
    else
      Constants.setTimeThreshold(timeThreshold);

    Constants.setRemoveLargeEvents(removeLargeEvents);

    if (removeLargeEvents)
      Constants.setLargeEventThreshold(largeEventThreshold);

    initDisaggregation(folder, input, inputPrefix + "ApplianceList.csv",
                       inputPrefix + "ActivityList.csv");

    long tEnd = System.currentTimeMillis();
    long tDelta = tEnd - tStart;
    double elapsedSeconds = tDelta / 1000.0;

    System.out.println("Elapsed Time: " + elapsedSeconds);

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
  public void
    initDisaggregation (String folder, String filename, String outputAppliance,
                        String outputActivity) throws Exception
  {

    // MultiOutputStream multiOut = new MultiOutputStream(System.out, fout);
    // PrintStream stdout = new PrintStream(multiOut);
    // System.setOut(stdout);

    // Creating the data sets under investigation.
    PowerDatasets data = new PowerDatasets(folder + filename, false);

    PrintStream realSystemOut = System.out;
    OutputStream output = null;
    PrintStream printOut = null;
    try {
      output =
        new FileOutputStream(Constants.tempFolder
                             + filename.substring(0, filename.length() - 4)
                             + " Event Analysis.txt");
      printOut = new PrintStream(output);
      System.setOut(printOut);
    }
    catch (FileNotFoundException e) {
      e.printStackTrace();
    }

    Constants.setThreshold(data.getActivePower());

    // Initialize the auxiliary variables
    EventDetector ed = new EventDetector();

    File appliancesFile = new File(outputAppliance);

    if (Constants.APPLIANCE_TYPE.equalsIgnoreCase("List")
        && appliancesFile.exists())
      ai = new ApplianceIdentifier(outputAppliance);
    else
      ai = new ApplianceIdentifier();

    // Run the event detector in order to find the possible events in the
    // data
    events = ed.detectEvents(data.getActivePower(), data.getReactivePower());

    System.setOut(realSystemOut);

    System.out.println("Appliances in the beginning:"
                       + ai.getApplianceList().size());

    Utils.durationCheck(events);

    // The Isolated Appliance Extractor helps the procedure of finding the
    // refrigerator and washing machine amongst others.
    iso = new IsolatedEventsExtractor(events);

    System.setOut(printOut);

    if (iso.getIsolatedEvents().size() != 0)
      ai.appliancesFromIsolated(iso);

    // Setting the refrigerator
    if (ai.getApplianceList().size() > 0) {

      if (Constants.REF_LOOSE_COUPLING) {

        Collections.sort(ai.getApplianceList(), Constants.comp6);

        boolean flag = true;
        int i = 0;

        while (flag && i < ai.getApplianceList().size()) {
          if (ai.getApplianceList().get(i).getMeanActive() < Constants.REF_UPPER_THRESHOLD) {
            ai.getApplianceList().get(i).setActivity("Refrigeration");
            ai.getApplianceList().get(i).setName("Refrigerator");
            Appliance temp = ai.getApplianceList().remove(i);
            ai.getApplianceList().add(0, temp);
            flag = false;
          }
          i++;
        }

      }
      else {
        ai.refrigeratorIdentification(events);
      }
    }

    if (Constants.WASHING_MACHINE_DETECTION)
      ai.washingMachineIdentification(events);

    log.info("");
    log.info("===============APPLIANCE STATUS FIRST PHASE================");
    for (Appliance appliance: ai.getApplianceList())
      appliance.status();

    output.close();

    try {
      output =
        new FileOutputStream(Constants.tempFolder
                             + filename.substring(0, filename.length() - 4)
                             + " Final Pairs Analysis.txt");
      printOut = new PrintStream(output);
      System.setOut(printOut);
    }
    catch (FileNotFoundException e) {
      e.printStackTrace();
    }

    log.info("");
    log.info("===============DISAGGREGATION SECOND PHASE================");

    // For each event an analysis is at hand helping to separate the
    // different consumption models and identify their results
    for (Event event: events) {

      boolean riseFlag = (event.getRisingPoints().size() != 0);
      boolean reductionFlag = (event.getReductionPoints().size() != 0);

      if (event.getWashingMachineFlag() == false && riseFlag && reductionFlag) {

        log.info("");
        log.info("==================================");
        log.info("Event: " + event.getId());
        log.info("Start: " + event.getStartMinute() + " End: "
                 + event.getEndMinute());
        log.info("==================================");
        log.info("");

        event.detectSwitchingPoints(false);

        if (event.getRisingPoints().size() > 0
            && event.getReductionPoints().size() > 0)
          event.detectClusters(false);

        if (event.getRisingPoints().size() > 0
            && event.getReductionPoints().size() > 0)
          event.detectBasicShapes(false);

        if (event.getRisingPoints().size() > 0
            && event.getReductionPoints().size() > 0)
          event.detectMatchingPoints(false);

        if (event.getRisingPoints().size() > 0
            && event.getReductionPoints().size() > 0)
          event.findCombinations(false);
      }

      event.status2();
      if (ai.getApplianceList().get(0).getMatchingPoints(event.getId()) != null) {
        System.out.println("Fridge Points: ");
        for (PointOfInterest[] pois: ai.getApplianceList().get(0)
                .getMatchingPoints(event.getId()))
          System.out.println(Arrays.toString(pois));
      }

      event.calculateFinalPairs();
      if (event.getFinalPairs().size() > 0)
        ai.analyseEvent(event, false);

    }

    ai.createDisaggregationFiles(outputAppliance, outputActivity, events);
    output.close();
    System.setOut(realSystemOut);

    // The extracted appliances are printed to see the results of the
    // procedure for (Appliance appliance: ai.getApplianceList())
    // appliance.status();

    clearAll();

  }

  private void clearAll ()
  {
    if (events != null)
      events.clear();
    if (ai != null)
      ai.clear();
    if (iso != null)
      iso.clear();
    Constants.clear();
    // Utils.cleanFiles();
  }

  public static void main (String[] args) throws Exception
  {
    // String input = "Demo/Household1.csv";
    // String input = "Demo/Milioudis.csv";
    // String input = "Demo/measurements.csv";
    // String input = "Demo/Benchmark.csv";
    // String input = "Demo/rs1192New_CC.csv";
    // String input = "Demo/BenchmarkingTest1.csv";
    // String input = "Demo/rs1246New_CC.csv";
    // String applianceFile = "";

    File folder = new File("DataFiles/");
    String path = folder.getPath() + "/";
    String[] datasets = folder.list();
    System.out.println(path);
    System.out.println(Arrays.toString(datasets));
    for (int i = 0; i < datasets.length; i++) {
      // for (int i = 0; i < 1; i++) {

      System.out.println("File:" + datasets[i]);

      Disaggregate dis = new Disaggregate(path, datasets[i]);

      dis.clearAll();
    }

  }
}
