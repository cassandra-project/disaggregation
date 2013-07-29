package disaggregation;
import java.util.ArrayList;
import java.util.Arrays;

public class EventDetector
{

  public EventDetector ()
  {

  }

  public ArrayList<Event> detectEvents (double[] activePower,
                                        double[] reactivePower)
  {
    double eventThreshold = 0;
    ArrayList<Event> events = new ArrayList<Event>();

    eventThreshold = findMin(activePower) + estimateThreshold();
    boolean started = false;
    boolean check = true;
    int start = -1;
    int end = -1;

    for (int i = 0; i < activePower.length; i++) {

      if (activePower[i] >= eventThreshold && started == false) {
        start = i - 1;
        started = true;
      }

      if (activePower[i] < eventThreshold && started == true) {

        boolean flag = true;

        int endingIndex =
          Math.min(i + Constants.EVENT_TIME_LIMIT, activePower.length);
        for (int j = i + 1; j < endingIndex; j++) {

          if (activePower[j] > eventThreshold) {

            flag = false;
            // System.out.println("Index: " + j + " Value: " + activePower[j]
            // + " Flag: " + flag);
            break;
          }
        }

        if (flag) {
          end = i;

          // System.out.println("Start: " + start + " End: " + end);

          if (start != -1) {
            check =
              checkMeanConsumption(eventThreshold,
                                   Arrays.copyOfRange(activePower, start,
                                                      end + 1));

            if (check) {
              Event temp =
                new Event(start, end, Arrays.copyOfRange(activePower, start,
                                                         end + 1),
                          Arrays.copyOfRange(reactivePower, start, end + 1));

              events.add(temp);
              // if (i > 200)
              // break;
            }
          }

          start = -1;
          end = -1;
          started = false;
        }
      }

    }

    return events;

  }

  private double findMin (double[] activePower)
  {

    double min = Double.POSITIVE_INFINITY;

    for (int i = 0; i < activePower.length; i++)
      if (min > activePower[i])
        min = activePower[i];

    return min;

  }

  private double estimateThreshold ()
  {

    return Constants.HOUSEHOLD_BACKGROUND_THRESHOLD;

  }

  private boolean checkMeanConsumption (double eventThreshold, double[] active)
  {

    boolean result = true;

    double sum = 0;

    for (int i = 0; i < active.length; i++)
      sum += active[i];

    sum /= active.length;

    if (sum < eventThreshold)
      result = false;

    return result;

  }
}
