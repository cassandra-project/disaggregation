package disaggregation;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import weka.clusterers.HierarchicalClusterer;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.EuclideanDistance;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.AddCluster;

public class IsolatedApplianceExtractor
{
  private ArrayList<Event> isolated = new ArrayList<Event>();
  private Map<String, ArrayList<Integer>> clusters =
    new TreeMap<String, ArrayList<Integer>>();
  private String refrigeratorCluster = "";
  private ArrayList<Double[]> refConsumptionMeans = new ArrayList<Double[]>();

  public IsolatedApplianceExtractor (ArrayList<Event> events) throws Exception
  {

    boolean q1 = false;
    boolean q3 = false;
    boolean pDiff = false;

    for (Event event: events) {

      // System.out.println("Event:" + event.getId() + " Rising Points: "
      // + event.getRisingPoints().size()
      // + " Reduction Points: "
      // + event.getReductionPoints().size());

      if (event.getRisingPoints().size() == 1
          && event.getReductionPoints().size() == 1) {
        isolated.add(event);
      }
      else if (event.getRisingPoints().size() == 1
               && event.getReductionPoints().size() == 2) {

        q1 = (event.getRisingPoints().get(0).getQDiff() > 0);
        pDiff =
          (Math.abs(event.getReductionPoints().get(1).getPDiff()) > Constants.ISOLATED_TIMES_UP
                                                                    * Math.abs(event
                                                                            .getReductionPoints()
                                                                            .get(0)
                                                                            .getPDiff()));
        q3 = (event.getReductionPoints().get(1).getQDiff() < 0);

        if (q1 && q3 && pDiff) {
          event.getReductionPoints().remove(0);
          isolated.add(event);
        }

      }
    }

    Instances inst = createInstances(isolated);

    // System.out.println(inst.toString());

    fillClusters(inst);

    // System.out.println("Clusters:" + clusters.toString());

    findRefrigerator();

    System.out.println("Fridge Cluster:" + refrigeratorCluster);

  }

  public ArrayList<Double[]> getRefConsumptionMeans ()
  {
    return refConsumptionMeans;
  }

  private Instances createInstances (ArrayList<Event> isolated)
    throws Exception
  {

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

    for (Event event: isolated) {

      Instance inst = new DenseInstance(5);
      inst.setValue(id, event.getId());
      inst.setValue(pDiffRise, event.getRisingPoints().get(0).getPDiff());
      inst.setValue(qDiffRise, event.getRisingPoints().get(0).getQDiff());
      inst.setValue(pDiffReduce, event.getReductionPoints().get(0).getPDiff());
      inst.setValue(qDiffReduce, event.getReductionPoints().get(0).getQDiff());

      instances.add(inst);

    }

    AddCluster addcluster = new AddCluster();

    HierarchicalClusterer clusterer = new HierarchicalClusterer();

    String[] opt =
      { "-N", "" + Constants.MAX_CLUSTERS_NUMBER + "", "-P", "-D", "-L",
       "AVERAGE" };

    clusterer.setDistanceFunction(new EuclideanDistance());
    clusterer.setNumClusters(Constants.MAX_CLUSTERS_NUMBER);
    clusterer.setOptions(opt);
    clusterer.setPrintNewick(true);
    clusterer.setDebug(true);

    // clusterer.getOptions();

    addcluster.setClusterer(clusterer);
    addcluster.setInputFormat(instances);
    addcluster.setIgnoredAttributeIndices("1");

    Instances newInst = Filter.useFilter(instances, addcluster);

    return newInst;
  }

  private void fillClusters (Instances inst)
  {
    ArrayList<Integer> temp;

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

  private void findRefrigerator ()
  {
    int maxSize = 0;

    for (String cluster: clusters.keySet()) {
      if (maxSize < clusters.get(cluster).size()) {
        maxSize = clusters.get(cluster).size();
        refrigeratorCluster = cluster;
      }
    }

    clusterRefMeans();

  }

  public double refClusterMeanValues (int eventIndex)
  {
    boolean flag = clusters.get(refrigeratorCluster).contains(eventIndex);

    if (flag == false) {
      System.out.println("NOPE!");
      return 0.0;
    }
    else {
      int index = clusters.get(refrigeratorCluster).indexOf(eventIndex);
      return refConsumptionMeans.get(index)[1];
    }
  }

  private void clusterRefMeans ()
  {
    ArrayList<Integer> clusterEvents = clusters.get(refrigeratorCluster);

    for (Integer index: clusterEvents) {
      Double[] meanValues = new Double[2];
      meanValues[0] = isolated.get(index).getMeanValues()[0];
      meanValues[1] = isolated.get(index).getMeanValues()[1];
      refConsumptionMeans.add(meanValues);
    }
  }
}
