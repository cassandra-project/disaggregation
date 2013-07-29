package disaggregation;
import java.util.Arrays;

public class ConsecutiveValues
{

  private int start = -1, end = -1;
  private double[] pValues;
  private double[] qValues;
  private double difference;
  private int numberOfElements = 0;
  private double maxQ = 0;

  public ConsecutiveValues (int start, int end, double[] pValues,
                            double[] qValues)
  {
    this.start = start;
    this.end = end;
    this.pValues = pValues;
    this.qValues = qValues;

    fillMetrics();
  }

  public int getStart ()
  {
    return start;
  }

  public int getEnd ()
  {
    return end;
  }

  public double getDifference ()
  {
    return difference;
  }

  public int getNumberOfElements ()
  {
    return numberOfElements;
  }

  public double getMaxQ ()
  {
    return maxQ;
  }

  private void fillMetrics ()
  {

    double[] diffActive = new double[pValues.length - 1];
    double[] diffReactive = new double[pValues.length - 1];
    double[] tempReactive = Arrays.copyOf(qValues, qValues.length);
    double metric2 = 0;

    for (int i = 0; i < diffActive.length; i++) {

      diffActive[i] = pValues[i + 1] - pValues[i];
      diffReactive[i] = qValues[i + 1] - qValues[i];

      if (diffActive[i] * diffReactive[i] < 0)
        tempReactive[i + 1] = tempReactive[i]; // ASK

    }

    qValues = Arrays.copyOf(tempReactive, tempReactive.length);

    maxQ = findMax(qValues);
    numberOfElements = tempReactive.length;

    for (int i = 0; i < tempReactive.length; i++) {
      tempReactive[i] /= maxQ;
      metric2 += tempReactive[i];
    }

    difference = 100 * ((numberOfElements - metric2) / numberOfElements);

  }

  private double findMax (double[] matrix)
  {

    double result = Double.NEGATIVE_INFINITY;

    for (int i = 0; i < matrix.length; i++)
      if (result < matrix[i])
        result = matrix[i];

    return result;
  }

  public void status ()
  {
    System.out.println("Start: " + start + " End: " + end);
    System.out.println("PValues: " + Arrays.toString(pValues));
    System.out.println("QValues: " + Arrays.toString(qValues));
    System.out.println("Difference: " + difference);
    System.out.println("Number of Elements: " + numberOfElements);
    System.out.println("MaxQ: " + maxQ);
  }

}
