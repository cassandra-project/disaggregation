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

import java.util.Arrays;

/**
 * This is an auxiliary class used during the washing machine identification
 * procedure. It is used for storing the values of consecutive minutes where the
 * reactive power is over zero.
 * 
 * @author Antonios Chrysopoulos
 * @version 0.9, Date: 29.07.2013
 */

public class ConsecutiveValues
{

  /**
   * The starting minute of the set of consecutive minutes with positive
   * reactive values.
   */
  private int start = -1;

  /**
   * The ending minute of the set of consecutive minutes with positive
   * reactive values
   */
  private int end = -1;

  /**
   * The array that contains the active power values of the consecutive minutes.
   */
  private double[] pValues;

  /**
   * The array that contains the reactive power values of the consecutive
   * minutes.
   */
  private double[] qValues;

  /**
   * A metric showing how consistent is the reactive power value in the set.
   */
  private double difference;

  /**
   * The number of consecutive minutes.
   */
  private int numberOfElements = 0;

  /**
   * The maximum value of the reactive power array.
   */
  private double maxQ = 0;

  /**
   * A constructor of a consecutive values set of minutes.
   * 
   * @param start
   *          The starting minute of the consecutive set of minutes
   * @param end
   *          The ending minute of the consecutive set of minutes
   * @param pValues
   *          The array that contains the active power values of the consecutive
   *          minutes.
   * @param qValues
   *          The array that contains the reactive power values of the
   *          consecutive minutes.
   */
  public ConsecutiveValues (int start, int end, double[] pValues,
                            double[] qValues)
  {
    this.start = start;
    this.end = end;
    this.pValues = pValues;
    this.qValues = qValues;

    fillMetrics();
  }

  /**
   * 
   * This function is used as a getter for the start minute of the consecutive
   * minutes set.
   * 
   * @return the start minute of the set.
   */
  public int getStart ()
  {
    return start;
  }

  /**
   * 
   * This function is used as a getter for the end minute of the consecutive
   * minutes set.
   * 
   * @return the end minute of the set.
   */
  public int getEnd ()
  {
    return end;
  }

  /**
   * 
   * This function is used as a getter for the difference metric of the
   * consecutive
   * minutes set.
   * 
   * @return the difference metric of the set.
   */
  public double getDifference ()
  {
    return difference;
  }

  /**
   * 
   * This function is used as a getter for the number of minutes.
   * 
   * @return the number of minutes of the set.
   */
  public int getNumberOfElements ()
  {
    return numberOfElements;
  }

  /**
   * 
   * This function is used as a getter for the max reactive value of the
   * consecutive minutes set.
   * 
   * @return the max reactive value of the set.
   */
  public double getMaxQ ()
  {
    return maxQ;
  }

  /**
   * This function is used for the calculation of the several metrics that are
   * used for the washing machine identification procedure.
   */
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

    maxQ = Utils.findMax(qValues);
    numberOfElements = tempReactive.length;

    for (int i = 0; i < tempReactive.length; i++) {
      tempReactive[i] /= maxQ;
      metric2 += tempReactive[i];
    }

    difference = 100 * ((numberOfElements - metric2) / numberOfElements);

  }

  /**
   * This function is used to show the attributes and the details of the
   * consecutive values object.
   */
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
