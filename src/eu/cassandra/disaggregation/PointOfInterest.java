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

/**
 * This is class implements the notion of a Point of Interest (as the name
 * suggests). These are the points where there is a significant increase or
 * decrease of the active power measurement, meaning that an appliance has
 * switched on / off. These are closely analysed to identify the appliances
 * end-uses.
 * 
 * @author Antonios Chrysopoulos
 * @version 0.7, Date: 29.07.2013
 */
public class PointOfInterest
{
  /**
   * This variable is a unique id number in order to separate the points of
   * interest.
   */
  private int id;

  /**
   * This variable is the minute the point of interest appears in the data set.
   */
  private int minute;

  /**
   * This variable states the type of change that the point of interest
   * represents (rising or not).
   */
  private boolean rising;

  /**
   * This variable is the value of the difference in the active power
   * measurements for the minute in question.
   */
  private double pDiff;

  /**
   * This variable is the value of the difference in the reactive power
   * measurements for the minute in question.
   */
  private double qDiff;

  /**
   * A constructor of a point of interest used in case we know most of the input
   * variables.
   * 
   * @param minute
   *          The minute of the point of interest in the data set.
   * @param rising
   *          If the point signifies increase in the active power or not.
   * @param pdiff
   *          The difference in the active power measurements.
   * @param qdiff
   *          The difference in the reactive power measurements.
   */
  public PointOfInterest (int minute, boolean rising, double pdiff, double qdiff)
  {
    id = Constants.POI_ID++;
    this.minute = minute;
    this.rising = rising;
    pDiff = pdiff;
    qDiff = qdiff;
  }

  /**
   * This function is used as a getter for the minute of the point of interest.
   * 
   * @return minute the point of interest is located.
   */
  public int getMinute ()
  {
    return minute;
  }

  /**
   * This function is used as a getter for the state of the point of interest.
   * 
   * @return true if there is an increase in active power, false otherwise.
   */
  public boolean getRising ()
  {
    return rising;
  }

  /**
   * This function is used as a getter for the difference in the active power of
   * the point of interest.
   * 
   * @return the difference in active power.
   */
  public double getPDiff ()
  {
    return pDiff;
  }

  /**
   * This function is used as a getter for the difference in the reactive power
   * of the point of interest.
   * 
   * @return the difference in reactive power.
   */
  public double getQDiff ()
  {
    return qDiff;
  }

  /**
   * This function is estimating the euclidean length (or norm) of the active
   * and reactive power vector of the point of interest.
   * 
   * @return the euclidean length of the point of interest with respect to
   *         active and reactive power differences.
   */
  public double euclideanLength ()
  {
    return Math.sqrt(Math.pow(pDiff, 2) + Math.pow(qDiff, 2));
  }

  /**
   * This function is estimating the absolute euclidean distance of the active
   * and reactive power vector distance of the point of interest with an array
   * of mean values.
   * 
   * @return the estimated absolute euclidean distance.
   */
  public double absoluteEuclideanDistance (double[] meanValues)
  {
    return Math.sqrt(Math.pow(pDiff - meanValues[0], 2)
                     + Math.pow(qDiff - meanValues[1], 2));
  }

  /**
   * This function is estimating the percentage euclidean distance of the active
   * and reactive power vector distance of the point of interest with an array
   * of mean values.
   * 
   * @return the estimated percentage euclidean distance.
   */
  public double percentageEuclideanDistance (double[] meanValues)
  {
    return 100
           * Math.sqrt(Math.pow(pDiff - meanValues[0], 2)
                       + Math.pow(qDiff - meanValues[1], 2))
           / euclideanLength();
  }

  /**
   * This function is estimating the percentage euclidean distance of the active
   * and reactive power vector distance of the point of interest with an array
   * of mean values.
   * 
   * @return the estimated percentage euclidean distance.
   */
  public double percentageEuclideanDistance (Double[] meanValues)
  {
    return 100
           * Math.sqrt(Math.pow(pDiff - meanValues[0], 2)
                       + Math.pow(qDiff - meanValues[1], 2))
           / euclideanLength();
  }

  @Override
  public String toString ()
  {
    return "POI " + id + " Minute: " + minute + " Rising: " + rising
           + " PDiff: " + pDiff + " QDiff: " + qDiff;
  }

  /**
   * This function is used to present the basic information of the activity
   * model on the console.
   */
  public void status ()
  {
    System.out.println("Minute: " + minute);
    System.out.println("Rising: " + rising);
    System.out.println("Active Power Difference: " + pDiff);
    System.out.println("Reactive Power Difference: " + qDiff);
  }

}
