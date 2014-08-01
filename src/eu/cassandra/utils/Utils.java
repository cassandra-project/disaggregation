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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import weka.clusterers.SimpleKMeans;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.AddCluster;
import eu.cassandra.appliance.Appliance;
import eu.cassandra.event.Event;

/**
 * This class contains static functions that are used for general purposes
 * throughout the Disaggregation Module.
 * 
 * @author Antonios Chrysopoulos
 * @version 0.9, Date: 29.07.2013
 */

public class Utils
{
  static Logger log = Logger.getLogger(Utils.class);

  /** Loading a library for integer programming. */
  static {
    System.loadLibrary("jniconstraintsolver");
  }

  /**
   * This function is estimating the absolute euclidean distance of the active
   * and reactive power vector distance of two points of interest in the form of
   * arrays.
   * 
   * @param a1
   *          The first array of values
   * @param a2
   *          The second array of values
   * 
   * @return the estimated absolute euclidean distance.
   */
  public static double absoluteEuclideanDistance (double[] a1, double[] a2)
  {
    return Math.sqrt(Math.pow(a1[0] - a2[0], 2) + Math.pow(a1[1] - a2[1], 2));
  }

  /**
   * This function is estimating the percentage euclidean distance of the active
   * and reactive power vector distance of two points of interest in the form of
   * arrays.
   * 
   * @param a1
   *          The first array of values
   * 
   * @param a2
   *          The second array of values
   * 
   * @return the estimated percentage euclidean distance.
   */
  public static double percentageEuclideanDistance (double[] a1, double[] a2)
  {
    return 100
           * Math.sqrt(Math.pow(a1[0] - a2[0], 2) + Math.pow(a1[1] - a2[1], 2))
           / norm(a1);
  }

  /**
   * This function is estimating the euclidean length (or norm) of an array of
   * two values
   * 
   * @param poi
   *          The point of interest's array of values
   * @return the euclidean length of the array.
   */
  public static double norm (double[] poi)
  {
    return Math.sqrt(Math.pow(poi[0], 2) + Math.pow(poi[1], 2));
  }

  /**
   * This function is used in order to check if a certain appliance is within
   * the permitted limits
   * 
   * @param trueValue
   *          The value under examination
   * @param limit
   *          The limit value that is used as threshold
   * @return true if it is within limits, false otherwise
   */
  public static boolean checkLimit (double trueValue, double limit)
  {

    double lowerLimit = (1 - Constants.ERROR_FRINGE) * limit;

    return (trueValue > lowerLimit);
  }

  /**
   * This function is used in order to check if a certain appliance is within
   * the permitted limits
   * 
   * @param trueValue
   *          The value under examination
   * @param limit
   *          The limit value that is used as threshold
   * @return true if it is within limits, false otherwise
   */
  public static boolean checkLimitFridge (double trueValue, double limit)
  {
    double lowerLimit = 0, upperLimit = 0;

    if (Constants.REF_LOOSE_COUPLING) {

      lowerLimit = Constants.REF_DURATION_FRINGE - limit;
      upperLimit = Constants.REF_DURATION_FRINGE + limit;

    }
    else {

      lowerLimit = (1 - Constants.STRICT_REF_DURATION_FRINGE) * limit;
      upperLimit = (1 + Constants.STRICT_REF_DURATION_FRINGE) * limit;

    }

    log.debug("True Value: " + trueValue + " Limit: " + limit + " UpperLimit: "
              + upperLimit + " Lower Limit: " + lowerLimit);

    return (trueValue < upperLimit && trueValue > lowerLimit);
  }

  /**
   * This function is used in order to check if a certain appliance is within
   * the permitted limits
   * 
   * @param trueValue
   *          The value under examination
   * @param limit
   *          The limit value that is used as threshold
   * @return true if it is within limits, false otherwise
   */
  public static double pairingLimit (double trueValue)
  {

    double upperLimit = (1 + Constants.PAIR_ERROR_FRINGE) * trueValue;

    return upperLimit;
  }

  /**
   * This function is used for the detection of reduction points following a
   * rising point, so that there is a possibility they can be connected as a
   * pair
   * 
   * @param index
   *          The index of the rising point of interest.
   * @param pois
   *          The list of points of interest under examination.
   * @return an array of indices that contain possible combinatorial reduction
   *         points of interest.
   */
  public static Integer[] findRedPoints (int index,
                                         ArrayList<PointOfInterest> pois)
  {

    ArrayList<Integer> temp = new ArrayList<Integer>();
    double limit = pairingLimit(pois.get(index).getPDiff());
    double timeLimit = Double.POSITIVE_INFINITY;
    if (Constants.SIMPLE_TIME_COMPLEXITY == true)
      timeLimit = pois.get(index).getMinute() + Constants.TEMPORAL_THRESHOLD;

    for (int i = index + 1; i < pois.size(); i++)
      if (pois.get(i).getRising() == false
          && pois.get(i).getMinute() <= timeLimit
          && limit > -pois.get(i).getPDiff())
        temp.add(i);

    Integer[] result = new Integer[temp.size()];

    for (int i = 0; i < temp.size(); i++)
      result[i] = temp.get(i);

    return result;

  }

  /**
   * This function is used for the detection of rising points preceding a
   * reduction point, so that there is a possibility they can be connected as a
   * pair
   * 
   * @param index
   *          The index of the reduction point of interest.
   * @param pois
   *          The list of points of interest under examination.
   * @return an array of indices that contain possible combinatorial rising
   *         points of interest.
   */
  public static Integer[] findRisPoints (int index,
                                         ArrayList<PointOfInterest> pois)
  {

    ArrayList<Integer> temp = new ArrayList<Integer>();
    double limit = -pairingLimit(pois.get(index).getPDiff());
    double timeLimit = Double.NEGATIVE_INFINITY;
    if (Constants.SIMPLE_TIME_COMPLEXITY == true)
      timeLimit = pois.get(index).getMinute() - Constants.TEMPORAL_THRESHOLD;

    for (int i = 0; i < index; i++)
      if (pois.get(i).getRising() && pois.get(i).getMinute() >= timeLimit
          && limit > pois.get(i).getPDiff())
        temp.add(i);

    Integer[] result = new Integer[temp.size()];

    for (int i = 0; i < temp.size(); i++)
      result[i] = temp.get(i);

    return result;

  }

  /**
   * This is an auxiliary function used to estimate the mean values of a pair of
   * points of interest.
   * 
   * @param pois
   *          The pair of points of interest under examination.
   * @return an array of the mean values of active and reactive power.
   */
  public static double[] meanValues (PointOfInterest[] pois)
  {

    double[] result =
      { (pois[0].getPDiff() - pois[1].getPDiff()) / 2,
       (pois[0].getQDiff() - pois[1].getQDiff()) / 2 };

    return result;

  }

  /**
   * This function is used for the creation of final matching pairs of points of
   * interest from the solutions that the integer programming solver has
   * provided.
   * 
   * @param pois
   *          The list of points of interest under examination.
   * @param array
   *          An array of 0-1 that shows which points of interest are included
   *          in the solution.
   * @return a list of pairs of points of interest.
   */
  public static ArrayList<PointOfInterest[]>
    createFinalPairs (ArrayList<PointOfInterest> pois, int[] array)
  {
    // Initializing the auxiliary variables.
    ArrayList<PointOfInterest[]> result = new ArrayList<PointOfInterest[]>();
    ArrayList<PointOfInterest> rising = new ArrayList<PointOfInterest>();
    ArrayList<PointOfInterest> reduction = new ArrayList<PointOfInterest>();

    // For all the points if the are 1 are included in the solution
    for (int i = 0; i < array.length; i++) {

      if (array[i] == 1) {
        if (pois.get(i).getRising())
          rising.add(pois.get(i));
        else
          reduction.add(pois.get(i));
      }

    }

    // If there are one of each point types.
    if (rising.size() == 1 && reduction.size() == 1) {

      PointOfInterest[] temp = { rising.get(0), reduction.get(0) };
      result.add(temp);
    }
    // If there is only one rising
    else if (rising.size() == 1) {

      for (PointOfInterest red: reduction) {

        PointOfInterest start =
          new PointOfInterest(rising.get(0).getMinute(), true, -red.getPDiff(),
                              -red.getQDiff());

        PointOfInterest[] temp = { start, red };
        result.add(temp);
      }

    }
    // If there is only one reduction
    else {
      for (PointOfInterest rise: rising) {

        PointOfInterest end =
          new PointOfInterest(reduction.get(0).getMinute(), false,
                              -rise.getPDiff(), -rise.getQDiff());

        PointOfInterest[] temp = { rise, end };
        result.add(temp);
      }
    }

    return result;
  }

  /**
   * This function is used to extract the file name from a path of a file,
   * excluding the file extension.
   * 
   * @param filename
   *          The full name and path of the file of interest.
   * @return The name of the file without the file extension.
   */
  public static String getFileName (String filename)
  {
    return filename.substring(0, filename.length() - 4);
  }

  /**
   * This function is used in order to create clusters of points of interest
   * based on the active power difference they have.
   * 
   * @param pois
   *          The list of points of interest that will be clustered.
   * @return The newly created clusters with the points that are comprising
   *         them.
   * @throws Exception
   */
  public static ArrayList<ArrayList<PointOfInterest>>
    clusterPoints (ArrayList<PointOfInterest> pois, int bias) throws Exception
  {
    // Initialize the auxiliary variables
    ArrayList<ArrayList<PointOfInterest>> result =
      new ArrayList<ArrayList<PointOfInterest>>();

    // Estimating the number of clusters that will be created
    int numberOfClusters =
      (int) (Math.ceil((double) pois.size()
                       / (double) Constants.MAX_POINTS_OF_INTEREST))
              + bias;

    log.info("Clusters: " + pois.size() + " / "
             + Constants.MAX_POINTS_OF_INTEREST + " + " + bias + " = "
             + numberOfClusters);

    // Create a new empty list of points for each cluster
    for (int i = 0; i < numberOfClusters; i++)
      result.add(new ArrayList<PointOfInterest>());

    // Initializing auxiliary variables namely the attributes of the data set
    Attribute id = new Attribute("id");
    Attribute pDiffRise = new Attribute("pDiff");

    ArrayList<Attribute> attr = new ArrayList<Attribute>();
    attr.add(id);
    attr.add(pDiffRise);

    Instances instances = new Instances("Points of Interest", attr, 0);

    // Each event is translated to an instance with the above attributes
    for (int i = 0; i < pois.size(); i++) {

      Instance inst = new DenseInstance(2);
      inst.setValue(id, i);
      inst.setValue(pDiffRise, Math.abs(pois.get(i).getPDiff()));

      instances.add(inst);

    }

    // System.out.println(instances.toString());

    Instances newInst = null;

    log.debug("Instances: " + instances.toSummaryString());

    // Create the addcluster filter of Weka and the set up the hierarchical
    // clusterer.
    AddCluster addcluster = new AddCluster();

    SimpleKMeans kmeans = new SimpleKMeans();

    kmeans.setSeed(numberOfClusters);

    // This is the important parameter to set
    kmeans.setPreserveInstancesOrder(true);
    kmeans.setNumClusters(numberOfClusters);
    kmeans.buildClusterer(instances);

    addcluster.setClusterer(kmeans);
    addcluster.setInputFormat(instances);
    addcluster.setIgnoredAttributeIndices("1");

    // Cluster data set
    newInst = Filter.useFilter(instances, addcluster);

    // System.out.println(newInst.toString());

    // Parse through the dataset to see where each point is placed in the
    // clusters.
    for (int i = 0; i < newInst.size(); i++) {

      String cluster = newInst.get(i).stringValue(newInst.attribute(2));

      cluster = cluster.replace("cluster", "");

      log.debug("Point of Interest: " + i + " Cluster: " + cluster);

      result.get(Integer.parseInt(cluster) - 1).add(pois.get(i));
    }

    // Sorting the each cluster points by their minutes.
    for (int i = result.size() - 1; i >= 0; i--) {
      if (result.get(i).size() == 0)
        result.remove(i);
      else
        Collections.sort(result.get(i), Constants.comp);
    }

    // Sorting the all clusters by their active power.

    Collections.sort(result, Constants.comp5);

    return result;
  }

  /**
   * This function is utilized for the extraction of the points that are not
   * combined with other ones in order to create the final pairs of operation.
   * 
   * @param pois
   *          The list of all the points of interest.
   * @param solution
   *          This is the list that contains the solution vectors for the points
   *          of interest.
   * @param solutionArray
   *          This array contains the indices of the points of interest
   *          participating in each solution.
   * @return
   */
  public static ArrayList<PointOfInterest>
    extractRemainingPoints (ArrayList<PointOfInterest> pois,
                            ArrayList<Integer> solution, int[][] solutionArray)
  {

    ArrayList<PointOfInterest> result = new ArrayList<PointOfInterest>();
    int[] tempArray = new int[solutionArray[0].length];

    for (Integer index: solution)
      for (int i = 0; i < solutionArray[index].length; i++)
        if (solutionArray[index][i] == 1)
          tempArray[i] = 1;

    // System.out.println("TempArray:" + Arrays.toString(tempArray));

    for (int i = 0; i < tempArray.length; i++)
      if (tempArray[i] == 0)
        result.add(pois.get(i));

    if (result.size() == 0)
      result = null;

    return result;
  }

  /**
   * This function is used to remove the smallest points of interest from a list
   * in order to make its size viable to estimate the pairs.
   * 
   * @param pois
   *          The list of points of interest.
   * @return The list of points of interest with a percentage of the points
   *         removed.
   */
  public static ArrayList<PointOfInterest>
    removePoints (ArrayList<PointOfInterest> pois)
  {

    ArrayList<PointOfInterest> result = new ArrayList<PointOfInterest>();

    int number = pois.size() - Constants.REMOVAL_MAX_POINTS;

    log.debug("Initial Size: " + pois.size() + " Removing: " + number);

    Collections.sort(pois, Constants.comp4);

    log.debug("Initial POIS: " + pois.toString());

    Collections.sort(result, Constants.comp4);

    for (int i = 0; i < number; i++)
      result.add(pois.remove(pois.size() - 1));

    log.debug("Removed POIS: " + result.toString());

    return result;

  }

  /**
   * This function is used in order to find the maximum value from an array.
   * 
   * @param matrix
   * @return
   */
  public static double findMax (double[] matrix)
  {

    double result = Double.NEGATIVE_INFINITY;

    for (int i = 0; i < matrix.length; i++)
      if (result < matrix[i])
        result = matrix[i];

    return result;
  }

  /**
   * This function is used in order to find the maximum value from an array.
   * 
   * @param matrix
   * @return
   */
  public static double findMax (ArrayList<Double> matrix)
  {

    double result = Double.NEGATIVE_INFINITY;

    for (int i = 0; i < matrix.size(); i++)
      if (result < matrix.get(i))
        result = matrix.get(i);

    return result;
  }

  /**
   * This function is used when the user has already tracked the electrical
   * appliances installed in the installation. He can used them as a base case
   * and extend it with any additional ones that may be found during the later
   * stages of analysis of the consumption.
   * 
   * @param filename
   *          The filename of the file containing the appliances.
   * @return
   *         A list of appliances
   * @throws FileNotFoundException
   */
  public static ArrayList<Appliance> appliancesFromFile (String filename)
    throws FileNotFoundException
  {
    // Read appliance file and start appliance parsing
    File file = new File(filename);
    Scanner input = new Scanner(file);

    ArrayList<Appliance> appliances = new ArrayList<Appliance>();

    String nextLine;
    String[] line;

    while (input.hasNextLine()) {
      nextLine = input.nextLine();
      line = nextLine.split(",");
      String name = line[0];
      String activity = line[1];

      if (activity.contains("Standby") == false
          && activity.contains("Refrigeration") == false) {

        double p = Double.parseDouble(line[2]);
        double q = Double.parseDouble(line[3]);

        // For each appliance found in the file, an temporary Appliance
        // Entity is created.
        appliances.add(new Appliance(name, activity, p, q, 0, 0));

      }
    }

    System.out.println("Appliances:" + appliances.size());

    input.close();

    return appliances;
  }

  /**
   * This is an auxiliary function used to check if the distance in time and
   * space of a pair is close to this appliance, meaning that it belongs to this
   * appliance.
   * 
   * @param mean
   *          The mean active and reactive power measurements.
   * @param duration
   *          The duration of the end-use.
   * @param metrics
   *          The metrics that are the base level
   * @return true if it is close, false otherwise.
   */
  public static boolean isCloseRef (double[] mean, int duration,
                                    double[] metrics)
  {

    double[] meanValues = { metrics[0], metrics[1] };

    return ((Utils.percentageEuclideanDistance(mean, meanValues) < Constants.REF_THRESHOLD) && (Utils
            .checkLimitFridge(duration, metrics[2])));

  }

  /**
   * This function is called when the temporary files must be removed from the
   * temporary folder used to store the csv and xls used to create the entity
   * models during the procedure of training and disaggregation. It is done when
   * the program starts, when the program ends and when the reset button is
   * pressed by the user.
   */
  public static void cleanFiles ()
  {
    File directory = new File("TempFiles");
    File files[] = directory.listFiles();
    String extension = "";
    for (int index = 0; index < files.length; index++) {
      {
        extension =
          files[index].getAbsolutePath().substring(files[index]
                                                           .getAbsolutePath()
                                                           .length() - 3,
                                                   files[index]
                                                           .getAbsolutePath()
                                                           .length());
        if (extension.equalsIgnoreCase("csv")) {
          boolean wasDeleted = files[index].delete();
          if (!wasDeleted) {
            System.out.println("Not Deleted File " + files[index].toString());
          }
        }
      }
    }
  }

  public static double estimateThreshold (double[] power, boolean median)
  {
    double result = 0;

    ArrayList<Double> minimums = new ArrayList<Double>();
    double min = Double.POSITIVE_INFINITY;

    for (int i = 0; i < power.length; i++) {

      if (min > power[i])
        min = power[i];

      if (i % 1440 == 0 && i != 0) {
        minimums.add(min);
        min = Double.POSITIVE_INFINITY;
      }

    }

    if (minimums.size() == 0)
      minimums.add(min);

    log.debug("================THRESHOLD SETTING================");
    log.debug("Minimums: " + minimums.toString());
    log.debug("Median:" + median);

    if (median)
      result = Utils.estimateMedian(minimums);
    else
      result = Utils.estimateMean(minimums);

    log.debug("Resulting threshold: " + result);
    log.debug("");
    log.debug("");

    return result;
  }

  public static double estimateMedian (ArrayList<Double> values)
  {
    double result = 0.0;
    int index = -1;
    Collections.sort(values);

    log.info("Values: " + values);

    if (values.size() == 2)
      index = 0;
    else
      index = values.size() / 2;

    if (values.size() % 2 == 0)
      result = (values.get(index) + values.get(index + 1)) / 2;
    else
      result = values.get(index);

    log.info("Result:" + result);

    return result;
  }

  public static double estimateMean (ArrayList<Double> values)
  {
    double result = 0.0;
    double sum = 0.0;

    for (double minimum: values)
      sum += minimum;

    result = sum / values.size();

    return result;
  }

  public static double estimateStd (ArrayList<Double> values, double mean)
  {
    double result = 0.0;
    double sum = 0;

    for (double value: values)
      sum += Math.pow((value - mean), 2);

    sum /= values.size();
    result = Math.sqrt(sum);
    return result;
  }

  /**
   * This is an auxiliary function used for checking if all the points of
   * interest are of the same type.
   * 
   * @param pois
   *          A list of points of interest
   * @return true if they are all of the same type, false otherwise.
   */
  public static boolean allSamePoints (ArrayList<PointOfInterest> pois)
  {
    // Initializing the auxiliary variables
    boolean flag = true;
    boolean start = pois.get(0).getRising();

    for (PointOfInterest poi: pois)
      if (start != poi.getRising()) {
        flag = false;
        break;
      }

    return flag;
  }

  public static double[] normalizeReactive (Event event)
  {
    double[] result = new double[event.getReactivePowerConsumptions().length];

    return result;

  }

  /**
   * This function is used for the visualization of a Line Diagram.
   * 
   * @param title
   *          The title of the chart.
   * @param x
   *          The unit on the X axis of the chart.
   * @param y
   *          The unit on the Y axis of the chart.
   * @param data
   *          The array of values.
   * @return a chart panel with the graphical representation.
   */
  public static void createLineDiagram (String title, String x, String y,
                                        ArrayList<Double> data)
  {

    XYSeries series1 = new XYSeries("Active Power");
    for (int i = 0; i < data.size(); i++) {
      series1.add(i, data.get(i));
    }

    XYSeriesCollection dataset = new XYSeriesCollection();
    dataset.addSeries(series1);

    PlotOrientation orientation = PlotOrientation.VERTICAL;
    boolean show = true;
    boolean toolTips = false;
    boolean urls = false;

    JFreeChart chart =
      ChartFactory.createXYLineChart(title, x, y, dataset, orientation, show,
                                     toolTips, urls);

    int width = 1024;
    int height = 768;

    try {
      ChartUtilities.saveChartAsPNG(new File(Constants.chartFolder + title
                                             + ".PNG"), chart, width, height);
    }
    catch (IOException e) {
    }

  }

  public static double countPoints (int[] points)
  {
    int counter = 0;

    for (int i = 0; i < points.length; i++)
      if (points[i] == 1)
        counter++;

    return counter;
  }

  public static void durationCheck (ArrayList<Event> events)
  {

    log.info("====================DURATIONS========================");

    ArrayList<Integer> durations = new ArrayList<Integer>();
    int start = -1, end = -1, counter = 0;
    int duration = -1;
    for (Event event: events) {
      start = event.getStartMinute();
      end = event.getEndMinute();
      duration = end - start;
      if (duration > Constants.MINUTES_PER_DAY) {
        counter++;
        log.info("Start:" + +start + " End: " + end + " Duration:" + duration);
      }
      durations.add(duration);
    }

    Collections.sort(durations);
    log.info("Durations:" + durations.toString());
    log.info("Events over a day: " + counter);
  }

  public static void
    removePoints (ArrayList<PointOfInterest> points, int minute)
  {
    int i = 0;

    for (i = 0; i < points.size(); i++)
      if (points.get(i).getMinute() == minute)
        break;

    points.remove(i);

  }

  public static Map<Double, Double>
    estimateCumulativeValues (ArrayList<Double> dataset)
  {

    log.info("============ESTIMATE CUMULATIVE VALUES==================");

    Map<Double, Double> result = new TreeMap<Double, Double>();

    double mean = estimateMean(dataset);
    double std = estimateStd(dataset, mean);

    log.info("Mean: " + mean);
    log.info("Standard Deviation: " + std);

    for (Double value: dataset)
      if (result.containsKey(value) == false)
        result.put(value, 1 - Gaussian.bigPhi(value, mean, std));

    // System.out.println(result.toString());

    return result;

  }
}
