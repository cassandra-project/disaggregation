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

package eu.cassandra.appliance;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import weka.clusterers.HierarchicalClusterer;
import weka.clusterers.SimpleKMeans;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.EuclideanDistance;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.AddCluster;
import eu.cassandra.event.Event;
import eu.cassandra.utils.Constants;
import eu.cassandra.utils.Utils;

/**
 * This class is responsible for finding events that contain isolated appliances
 * end-uses. An analysis is made in order to find out base loads (such as
 * refrigerator, freezer etc.) coming from those events and have make the
 * identification of these loads easire on the more complex events.
 * 
 * @author Antonios Chrysopoulos
 * @version 0.7, Date: 29.07.2013
 */

public class IsolatedApplianceExtractor
{
  /**
   * This is a list containing the events that are comprised of an isolated
   * appliance.
   */
  private ArrayList<Event> isolated = new ArrayList<Event>();

  /**
   * This is a map of the events contained in each cluster estimated later.
   */
  private Map<String, ArrayList<Integer>> clusters =
    new TreeMap<String, ArrayList<Integer>>();

  /**
   * The name of the cluster that is corresponding to the refrigerator.
   */
  private String refrigeratorCluster = "";

  /**
   * This is a list containing pairs of points of interest that correspond to
   * the refrigerator.
   */
  private ArrayList<Double[]> refConsumptionMeans = new ArrayList<Double[]>();

  /**
   * This is the constructor of the isolated appliance extractor class. It
   * created the clusters of the isolated events and detects which of them
   * corresponds to the refrigerator.
   * 
   * @param events
   *          The list of all the events detected by the Event Detector.
   * @throws Exception
   */
  public IsolatedApplianceExtractor (ArrayList<Event> events) throws Exception
  {
    // Initializing auxiliary variables
    boolean q1 = false;
    boolean q3 = false;
    boolean pDiff = false;

    // Checking each event. The ones that contain one rising and one reduction
    // points or two reduction points with the second much larger than the
    // first are selected and added to the array
    for (Event event: events) {

      // System.out.println("Event:" + event.getId() + " Rising Points: "
      // + event.getRisingPoints().size()
      // + " Reduction Points: "
      // + event.getReductionPoints().size());

      if (event.getRisingPoints().size() == 1
          && event.getReductionPoints().size() == 1) {
        isolated.add(event);
      }
      // else if (event.getRisingPoints().size() == 1
      // && event.getReductionPoints().size() == 2) {
      //
      // q1 = (event.getRisingPoints().get(0).getQDiff() > 0);
      // pDiff =
      // (Math.abs(event.getReductionPoints().get(1).getPDiff()) >
      // Constants.ISOLATED_TIMES_UP
      // * Math.abs(event
      // .getReductionPoints()
      // .get(0)
      // .getPDiff()));
      // q3 = (event.getReductionPoints().get(1).getQDiff() < 0);
      //
      // if (q1 && q3 && pDiff) {
      // event.getReductionPoints().remove(0);
      // isolated.add(event);
      // }
      // }
    }

    // The instances for the cluster procedure are created
    Instances inst = createInstances(isolated);

    // System.out.println(inst.toString());

    if (inst.size() > 0) {

      // The cluster is taking place
      fillClusters(inst);

      // System.out.println("Clusters:" + clusters.toString());

      // The refrigerator cluster is found
      findRefrigerator();

      // System.out.println("Fridge Cluster:" + refrigeratorCluster);

    }

  }

  /**
   * This function is used as a getter for consumption mean values of the
   * refrigerator cluster.
   * 
   * @return a list with mean values of active and reactive power measurements.
   */
  public ArrayList<Double[]> getRefConsumptionMeans ()
  {
    return refConsumptionMeans;
  }

  /**
   * This function is used as a getter for the isolated appliances list of the
   * events.
   * 
   * @return a list with the isolated appliance events.
   */
  public ArrayList<Event> getIsolatedEvents ()
  {
    return isolated;
  }

  /**
   * This is an auxiliary function that prepares the clustering data set. The
   * events must be translated to instances of the data set that can be used for
   * clustering.
   * 
   * @param isolated
   *          The list of the events containing an isolated appliance.
   * @return The instances of the data
   * @throws Exception
   */
  private Instances createInstances (ArrayList<Event> isolated)
    throws Exception
  {
    // Initializing auxiliary variables namely the attributes of the data set
    Attribute id = new Attribute("id");
    Attribute pDiffRise = new Attribute("pDiffRise");
    Attribute qDiffRise = new Attribute("qDiffRise");
    Attribute pDiffReduce = new Attribute("pDiffReduce");
    Attribute qDiffReduce = new Attribute("qDiffReduce");

    ArrayList<Attribute> attr = new ArrayList<Attribute>();
    attr.add(id);
    attr.add(pDiffRise);
    attr.add(qDiffRise);
    attr.add(pDiffReduce);
    attr.add(qDiffReduce);

    Instances instances = new Instances("Isolated", attr, 0);

    // Each event is translated to an instance with the above attributes
    for (Event event: isolated) {

      Instance inst = new DenseInstance(5);
      inst.setValue(id, event.getId());
      inst.setValue(pDiffRise, event.getRisingPoints().get(0).getPDiff());
      inst.setValue(qDiffRise, event.getRisingPoints().get(0).getQDiff());
      inst.setValue(pDiffReduce, event.getReductionPoints().get(0).getPDiff());
      inst.setValue(qDiffReduce, event.getReductionPoints().get(0).getQDiff());

      instances.add(inst);

    }

    int n = Constants.MAX_CLUSTERS_NUMBER;
    Instances newInst = null;

    System.out.println("Instances: " + instances.toSummaryString());
    System.out.println("Max Clusters: " + n);

    // Create the addcluster filter of Weka and the set up the hierarchical
    // clusterer.
    AddCluster addcluster = new AddCluster();

    if (instances.size() > Constants.KMEANS_LIMIT_NUMBER
        || instances.size() == 0) {

      HierarchicalClusterer clusterer = new HierarchicalClusterer();

      String[] opt = { "-N", "" + n + "", "-P", "-D", "-L", "AVERAGE" };

      clusterer.setDistanceFunction(new EuclideanDistance());
      clusterer.setNumClusters(n);
      clusterer.setOptions(opt);
      clusterer.setPrintNewick(true);
      clusterer.setDebug(true);

      // clusterer.getOptions();

      addcluster.setClusterer(clusterer);
      addcluster.setInputFormat(instances);
      addcluster.setIgnoredAttributeIndices("1");

      // Cluster data set
      newInst = Filter.useFilter(instances, addcluster);

    }
    else {

      SimpleKMeans kmeans = new SimpleKMeans();

      kmeans.setSeed(10);

      // This is the important parameter to set
      kmeans.setPreserveInstancesOrder(true);
      kmeans.setNumClusters(n);
      kmeans.buildClusterer(instances);

      addcluster.setClusterer(kmeans);
      addcluster.setInputFormat(instances);
      addcluster.setIgnoredAttributeIndices("1");

      // Cluster data set
      newInst = Filter.useFilter(instances, addcluster);

    }

    return newInst;

  }

  /**
   * This function is taking the instances coming out from clustering and put
   * each event to each respective cluster.
   * 
   * @param inst
   *          The clustered instances
   */
  private void fillClusters (Instances inst)
  {
    // Initializing auxiliary variables
    ArrayList<Integer> temp;

    // For each instance check the cluster value and put it to the correct
    // cluster
    for (int i = 0; i < inst.size(); i++) {

      String cluster = inst.get(i).stringValue(inst.attribute(5));

      if (!clusters.containsKey(cluster))
        temp = new ArrayList<Integer>();
      else
        temp = clusters.get(cluster);

      temp.add(i);

      clusters.put(cluster, temp);

    }

  }

  /**
   * This function is responsible for finding the larger cluster in size which
   * is going to be the refrigerator cluster.
   */
  private void findRefrigerator ()
  {
    // Initializing auxiliary variables
    int maxSize = 0;
    double distance1 = Double.POSITIVE_INFINITY, distance2 =
      Double.POSITIVE_INFINITY;
    // Magic Numbers for now
    double[] meanRef = { 100, 60 };

    for (String cluster: clusters.keySet()) {
      if (maxSize < clusters.get(cluster).size()) {
        maxSize = clusters.get(cluster).size();
        refrigeratorCluster = cluster;
        distance1 =
          Utils.percentageEuclideanDistance(meanRef, clusterMeans(cluster));
        // System.out.println("Mean Ref Distance: " + distance1);
      }
      else if (maxSize == clusters.get(cluster).size()) {
        distance2 =
          Utils.percentageEuclideanDistance(meanRef, clusterMeans(cluster));
        // System.out.println("Mean Previous Ref Distance: " + distance1);
        // System.out.println("New Ref Distance: " + distance2);
        // System.out.println("Smaller?: " + (distance2 < distance1));
        if (distance2 < distance1) {
          maxSize = clusters.get(cluster).size();
          refrigeratorCluster = cluster;
          distance1 =
            Utils.percentageEuclideanDistance(meanRef, clusterMeans(cluster));
          // System.out.println("Mean Ref Distance: " + distance1);
        }
      }
    }

    clusterRefMeans();

  }

  /**
   * This function is used for filling an array with the mean values of active
   * and reactive power of the refrigerator cluster events.
   */
  private void clusterRefMeans ()
  {
    // Initializing auxiliary variables
    ArrayList<Integer> clusterEvents = clusters.get(refrigeratorCluster);

    for (Integer index: clusterEvents) {
      Double[] meanValues = new Double[2];
      meanValues[0] = isolated.get(index).getMeanValues()[0];
      meanValues[1] = isolated.get(index).getMeanValues()[1];
      refConsumptionMeans.add(meanValues);
    }
  }

  /**
   * This function is used for filling an array with the mean values of active
   * and reactive power of the refrigerator cluster events.
   */
  private double[] clusterMeans (String clusterIndex)
  {
    // Initializing auxiliary variables
    ArrayList<Integer> clusterEvents = clusters.get(clusterIndex);
    double[] meanValues = new double[2];

    for (Integer index: clusterEvents) {
      meanValues[0] += isolated.get(index).getMeanValues()[0];
      meanValues[1] += isolated.get(index).getMeanValues()[1];
    }

    meanValues[0] /= clusterEvents.size();
    meanValues[1] /= clusterEvents.size();

    return meanValues;

  }
}
