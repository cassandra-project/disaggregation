package disaggregation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

public class ApplianceIdentified
{

  ArrayList<Appliance> applianceList = new ArrayList<Appliance>();
  ArrayList<String[]> activityList = new ArrayList<String[]>();

  public ApplianceIdentified ()
  {
    applianceList.clear();
    activityList.clear();
  }

  public ArrayList<Appliance> getApplianceList ()
  {
    return applianceList;
  }

  public Appliance getApplianceList (int index)
  {
    return applianceList.get(index);
  }

  public void refrigeratorIdentification (ArrayList<Event> events,
                                          IsolatedApplianceExtractor iso)
  {

    Map<Integer, ArrayList<PointOfInterest>> risingPoints =
      new TreeMap<Integer, ArrayList<PointOfInterest>>();
    Map<Integer, ArrayList<PointOfInterest>> reductionPoints =
      new TreeMap<Integer, ArrayList<PointOfInterest>>();

    ArrayList<PointOfInterest> poiRef;

    ArrayList<Double[]> tempCompare = iso.getRefConsumptionMeans();

    for (int i = 0; i < events.size(); i++) {
      // for (int i = 0; i < 3; i++) {

      int key = events.get(i).getId();

      // System.out.println("Event " + key);
      poiRef = new ArrayList<PointOfInterest>();

      // System.out.println("Rising Points");
      for (PointOfInterest poi: events.get(i).getRisingPoints()) {

        for (Double[] means: tempCompare) {

          if (poi.percentageEuclideanDistance(means) < Constants.REF_THRESHOLD) {
            // System.out.println("Euclidean Distance: "
            // + poi.percentageEuclideanDistance(means));
            poiRef.add(poi);
            break;
          }
        }
        if (poiRef.size() > 0)
          risingPoints.put(key, poiRef);
      }

      poiRef = new ArrayList<PointOfInterest>();
      // System.out.println("Reduction Points");
      for (PointOfInterest poi: events.get(i).getReductionPoints()) {

        for (Double[] means: tempCompare) {

          Double[] tempMeans = { -means[0], -means[1] };

          if (poi.percentageEuclideanDistance(tempMeans) < Constants.REF_THRESHOLD) {
            // System.out.println("Euclidean Distance: "
            // + poi.percentageEuclideanDistance(means));
            poiRef.add(poi);
            break;
          }
        }
        if (poiRef.size() > 0)
          reductionPoints.put(key, poiRef);
      }

      cleanEvent(events, i, risingPoints.get(i), reductionPoints.get(i));
    }

    Appliance appliance =
      new Appliance("Refrigerator", "Refrigeration", risingPoints,
                    reductionPoints);

    applianceList.add(appliance);
  }

  public void washingMachineIdentification (ArrayList<Event> events)
  {
    boolean risingFlag = false, reductionFlag = false;

    Appliance appliance = null;

    for (int i = 0; i < events.size(); i++) {

      if (events.get(i).getActivePowerConsumptions().length > Constants.WASHING_MACHINE_NUMBER_OF_MINUTES_LIMIT) {

        for (PointOfInterest rise: events.get(i).getRisingPoints())
          if (Math.abs(rise.getPDiff()) > Constants.WASHING_MACHINE_POWER_THRESHOLD) {
            risingFlag = true;
            // System.out.println("Rising Point: " + rise.toString());
            break;
          }

        for (PointOfInterest reduction: events.get(i).getReductionPoints())
          if (Math.abs(reduction.getPDiff()) > Constants.WASHING_MACHINE_POWER_THRESHOLD) {
            reductionFlag = true;
            // System.out.println("Reduction Point: " + reduction.toString());
            break;
          }

        if (risingFlag && reductionFlag) {
          // System.out.println("Search for Washing Machine in Event " + i);

          double[] tempReactive =
            Arrays.copyOf(events.get(i).getReactivePowerConsumptions(), events
                    .get(i).getReactivePowerConsumptions().length);
          double[] tempActive =
            Arrays.copyOf(events.get(i).getActivePowerConsumptions(), events
                    .get(i).getActivePowerConsumptions().length);

          Appliance ref = applianceList.get(0);

          if (ref.getMatchingPoints().containsKey(i)) {

            double refReactive = ref.getMeanReactive();
            System.out.println("Refrigerator Reactive: " + refReactive);
            int start = -1, end = -1;

            for (PointOfInterest[] poi: ref.getMatchingPoints(i)) {

              start = poi[0].getMinute();
              end = poi[1].getMinute();

              for (int j = start; j < end; j++)
                tempReactive[j] -= refReactive;
            }

            // System.out.println("Reactive: " + Arrays.toString(tempReactive));
          }

          ArrayList<ConsecutiveValues> cons =
            createVectors(tempActive, tempReactive);

          double maxAll = Double.NEGATIVE_INFINITY;
          int maxIndex = -1;
          for (int j = 0; j < cons.size(); j++) {
            // cons.get(j).status();
            if (maxAll < cons.get(j).getMaxQ()) {
              maxAll = cons.get(j).getMaxQ();
              maxIndex = j;
            }

          }

          if (Utils.checkLimit(cons.get(maxIndex).getDifference(),
                               Constants.WASHING_MACHINE_DEVIATION_LIMIT)
              && cons.get(maxIndex).getNumberOfElements() > Constants.WASHING_MACHINE_NUMBER_OF_MINUTES_LIMIT) {

            events.get(i).setWashingMachine();
            PointOfInterest[] match =
              {
               new PointOfInterest(cons.get(maxIndex).getStart(), true, cons
                       .get(maxIndex).getMaxQ(), cons.get(maxIndex).getMaxQ()),
               new PointOfInterest(cons.get(maxIndex).getEnd(), false, -cons
                       .get(maxIndex).getMaxQ(), -cons.get(maxIndex).getMaxQ()) };

            if (appliance == null)
              appliance = new Appliance("Washing Machine", "Cleaning");

            appliance.addMatchingPoints(i, match);

          }

          // Not only one at a time
          // for (int j = 0; j < cons.size(); j++) {
          // if (cons.get(j).getDifference() >
          // Constants.WASHING_MACHINE_DEVIATION_LIMIT
          // && cons.get(j).getNumberOfElements() >
          // Constants.WASHING_MACHINE_NUMBER_OF_MINUTES_LIMIT) {
          //
          // ArrayList<PointOfInterest> temp =
          // new ArrayList<PointOfInterest>();
          // temp.add(events.get(i).findPOI(cons.get(j).getStart(), true));
          // risingPoints.put(i, temp);
          //
          // temp = new ArrayList<PointOfInterest>();
          // temp.add(events.get(i).findPOI(cons.get(j).getStart(), true));
          // reductionPoints.put(i, temp);
          // break;
          // }
          //
          // }

        }
      }
      risingFlag = false;
      reductionFlag = false;

    }

    if (appliance != null)
      applianceList.add(appliance);

  }

  private void cleanEvent (ArrayList<Event> events, int index,
                           ArrayList<PointOfInterest> risingPoints,
                           ArrayList<PointOfInterest> reductionPoints)
  {

    if (risingPoints != null && reductionPoints != null) {

      // System.out.println("rising: " + risingPoints.toString());
      // System.out.println("reduction: " + reductionPoints.toString());

      if (events.get(index).getRisingPoints().size() == risingPoints.size()
          && events.get(index).getReductionPoints().size() == reductionPoints
                  .size()) {
        events.get(index).getReductionPoints().clear();
        events.get(index).getRisingPoints().clear();
      }
      else if (risingPoints.size() == reductionPoints.size()) {

        for (PointOfInterest rise: risingPoints)
          events.get(index).getRisingPoints().remove(rise);

        for (PointOfInterest reduction: reductionPoints)
          events.get(index).getRisingPoints().remove(reduction);

      }
      else {

        ArrayList<PointOfInterest> temp =
          new ArrayList<PointOfInterest>(risingPoints);

        temp.addAll(reductionPoints);

        Collections.sort(temp, Constants.comp);

        for (int i = temp.size() - 1; i >= 0; i--) {

          if (temp.get(i - 1).getRising() && temp.get(i).getRising() == false) {

            events.get(index).getRisingPoints().remove(temp.get(i - 1));
            events.get(index).getReductionPoints().remove(temp.get(i));
            temp.remove(i);
            temp.remove(i - 1);

          }
          // System.out.println(temp.size());

          if (allSamePoints(temp))
            break;

        }

      }

    }

  }

  private ArrayList<ConsecutiveValues> createVectors (double[] tempActive,
                                                      double[] tempReactive)
  {

    ArrayList<ConsecutiveValues> cons = new ArrayList<ConsecutiveValues>();

    int start = 0, end = 0;
    boolean startFlag = false;

    for (int j = 0; j < tempReactive.length; j++) {

      if (tempReactive[j] > 0 && !startFlag) {
        start = j;
        startFlag = true;
      }

      if (tempReactive[j] <= 0 && startFlag) {

        boolean flag = true;

        int endIndex =
          Math.min(j + Constants.WASHING_MACHINE_MERGING_MINUTE_LIMIT,
                   tempReactive.length);

        for (int k = j + 1; k < endIndex; k++)

          if (tempReactive[k] > 0) {

            flag = false;
            // System.out.println("Index: " + k + " Value: " + tempReactive[k]
            // + " Flag: " + flag);
            break;
          }

        if (flag) {
          end = j - 1;

          cons.add(new ConsecutiveValues(start, end, Arrays
                  .copyOfRange(tempActive, start, end + 1), Arrays
                  .copyOfRange(tempReactive, start, end + 1)));

          start = -1;
          end = -1;
          startFlag = false;
        }
      }
    }

    return cons;

  }

  private boolean allSamePoints (ArrayList<PointOfInterest> pois)
  {

    boolean flag = true;

    boolean start = pois.get(0).getRising();

    for (PointOfInterest poi: pois)
      if (start != poi.getRising()) {
        flag = false;
        break;
      }

    return flag;
  }

  public void analyseEvent (Event event)
  {
    double[] meanValues = null;
    double minDistance = Double.POSITIVE_INFINITY;
    int minIndex = -1;
    int duration = 0;

    for (int i = 0; i < event.getFinalPairs().size(); i++) {

      System.out.println("Final Pair " + i);

      meanValues = Utils.meanValues(event.getFinalPairs(i));
      duration =
        event.getFinalPairs(i)[1].getMinute()
                - event.getFinalPairs(i)[0].getMinute();

      System.out.println("Mean Values: " + Arrays.toString(meanValues));

      for (int j = 0; j < applianceList.size(); j++) {
        System.out.println("Appliance " + j);
        System.out.println("Appliance Mean Values "
                           + Arrays.toString(applianceList.get(j)
                                   .getMeanValues()));

        // System.out
        // .println("Distance: "
        // + Utils.percentageEuclideanDistance(meanValues,
        // applianceList
        // .get(j)
        // .getMeanValues()));

        if (applianceList.get(j).isClose(meanValues, duration)
            && minDistance > Utils.percentageEuclideanDistance(applianceList
                    .get(j).getMeanValues(), meanValues)) {

          System.out.println("Percentage Distance: "
                             + Utils.percentageEuclideanDistance(applianceList
                                     .get(j).getMeanValues(), meanValues));

          System.out.println("Absolute Distance: "
                             + Utils.absoluteEuclideanDistance(applianceList
                                     .get(j).getMeanValues(), meanValues));

          minDistance =
            Utils.percentageEuclideanDistance(applianceList.get(j)
                    .getMeanValues(), meanValues);
          minIndex = j;
        }

      }

      if (minIndex != -1)
        applianceList.get(minIndex).addMatchingPoints(event.getId(),
                                                      event.getFinalPairs(i));
      else
        createNewAppliance(event.getId(), event.getFinalPairs(i), meanValues);

    }
    event.clear();
  }

  private void createNewAppliance (int eventIndex, PointOfInterest[] finalPair,
                                   double[] meanValues)
  {
    int duration = finalPair[1].getMinute() - finalPair[0].getMinute();

    Appliance appliance = null;

    if (meanValues[0] > Constants.WATERHEATER_POWER_THRESHOLD) {

      appliance =
        new Appliance("Water Heater " + Constants.WATERHEATER_ID++, "Cleaning");

    }
    else if (meanValues[0] < Constants.SMALL_POWER_THRESHOLD
             && Math.abs(meanValues[1]) < Constants.REACTIVE_THRESHOLD) {
      if (meanValues[0] < Constants.TINY_POWER_THRESHOLD)
        appliance =
          new Appliance("Lighting " + Constants.LIGHTING_ID++, "Lighting");
      else
        appliance =
          new Appliance("Entertainment " + Constants.ENTERTAINMENT_ID++,
                        "Entertainment");
    }
    else if (meanValues[0] < Constants.WATERHEATER_POWER_THRESHOLD
             && meanValues[0] > Constants.SMALL_POWER_THRESHOLD
             && Math.abs(meanValues[1]) < Constants.REACTIVE_THRESHOLD) {

      appliance = new Appliance("Cooking " + Constants.COOKING_ID++, "Cooking");
    }
    else if (Math.abs(meanValues[1]) > Constants.REACTIVE_THRESHOLD) {
      if (duration <= Constants.MICROWAVE_DURATION)
        appliance =
          new Appliance("Microwave Oven " + Constants.MICROWAVE_OVEN_ID++,
                        "Cooking");
      else {

        if (meanValues[0] < Constants.MEDIUM_POWER_THRESHOLD)
          appliance =
            new Appliance("Extractor " + Constants.EXTRACTOR_ID++, "Cooking");
        else
          appliance =
            new Appliance("Vacuum Cleaner " + Constants.VACUUM_CLEANER_ID++,
                          "Cleaning");
      }
    }
    appliance.addMatchingPoints(eventIndex, finalPair);
    applianceList.add(appliance);
  }
}
