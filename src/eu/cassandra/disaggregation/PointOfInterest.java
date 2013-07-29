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

public class PointOfInterest
{
  private int id;
  private int minute;
  private boolean rising;
  private double pDiff;
  private double qDiff;

  public PointOfInterest (int minute, boolean rising, double pdiff, double qdiff)
  {
    id = Constants.POI_ID++;
    this.minute = minute;
    this.rising = rising;
    pDiff = pdiff;
    qDiff = qdiff;
  }

  public int getID ()
  {
    return id;
  }

  public int getMinute ()
  {
    return minute;
  }

  public boolean getRising ()
  {
    return rising;
  }

  public double getPDiff ()
  {
    return pDiff;
  }

  public double getQDiff ()
  {
    return qDiff;
  }

  public void status ()
  {
    System.out.println("Minute: " + minute);
    System.out.println("Rising: " + rising);
    System.out.println("Active Power Difference: " + pDiff);
    System.out.println("Reactive Power Difference: " + qDiff);
  }

  public double euclideanLength ()
  {
    return Math.sqrt(Math.pow(pDiff, 2) + Math.pow(qDiff, 2));
  }

  public double absoluteEuclideanDistance (double[] meanValues)
  {
    return Math.sqrt(Math.pow(pDiff - meanValues[0], 2)
                     + Math.pow(qDiff - meanValues[1], 2));
  }

  public double percentageEuclideanDistance (double[] meanValues)
  {
    return 100
           * Math.sqrt(Math.pow(pDiff - meanValues[0], 2)
                       + Math.pow(qDiff - meanValues[1], 2))
           / euclideanLength();
  }

  public double percentageEuclideanDistance (Double[] meanValues)
  {
    return 100
           * Math.sqrt(Math.pow(pDiff - meanValues[0], 2)
                       + Math.pow(qDiff - meanValues[1], 2))
           / euclideanLength();
  }

  public String toString ()
  {
    return "POI " + id + " Minute: " + minute + " Rising: " + rising
           + " PDiff: " + pDiff + " QDiff: " + qDiff;
  }

}
