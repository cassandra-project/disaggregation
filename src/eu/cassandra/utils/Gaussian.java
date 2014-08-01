/*   
   Copyright 2011-2012 The Cassandra Consortium (cassandra-fp7.eu)

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
import java.util.Random;

/**
 * This class is used for implementing a Normal (Gaussian) distribution to use
 * in the activity models that are created in the Training Module of
 * Cassandra Project. The same class has been used, with small alterations in
 * the main Cassandra Platform.
 * 
 * @author Christos Diou, Antonios Chrysopoulos
 * @version 0.9, Date: 29.07.2013
 */

public class Gaussian

{

  /**
   * The name of the Normal distribution.
   */
  private String name = "";

  /**
   * The type of the Normal distribution.
   */
  private String type = "";

  /**
   * The mean value of the Normal distribution.
   */
  protected double mean;

  /**
   * The standard deviation value of the Normal distribution.
   */
  protected double sigma;

  /**
   * A boolean variable that shows if the values of the Normal distribution
   * histogram
   * has been precomputed or not.
   */
  protected boolean precomputed;

  /**
   * A variable presenting the number of bins that are created for the histogram
   * containing the values of the Normal distribution.
   */
  protected int numberOfBins;

  /**
   * The starting point of the bins for the precomputed values.
   */
  protected double precomputeFrom;

  /**
   * The ending point of the bins for the precomputed values.
   */
  protected double precomputeTo;

  /**
   * An array containing the probabilities of each bin precomputed for the
   * Normal distribution.
   */
  protected double[] histogram;

  /**
   * This is an array that contains the probabilities that the distribution has
   * value over a threshold.
   */
  private double[] greaterProbability;

  /** The id of the distribution as given by the Cassandra server. */
  private String distributionID = "";

  /**
   * Function that computes the phi of a value.
   * 
   * @param x
   *          The selected value.
   * 
   * @return the phi value of the input value.
   */
  private static double phi (double x)
  {
    return Math.exp(-(x * x) / 2) / Math.sqrt(2 * Math.PI);
  }

  /**
   * Function that computes the phi of a value.
   * 
   * @param x
   *          The selected value.
   * @param mu
   *          The mean value parameter of the Normal Distribution.
   * @param s
   *          The standard deviation value parameter of the Normal Distribution.
   * 
   * @return the phi value of the input value.
   */
  private static double phi (int x, double mu, double s)
  {
    return phi((x - mu) / s) / s;
  }

  /**
   * Function that computes the standard Gaussian cdf using Taylor
   * approximation.
   * 
   * @param z
   *          The selected value.
   * 
   * @return the phi value of the input value.
   */
  private static double bigPhi (double z)
  {
    if (z < -8.0) {
      return 0.0;
    }
    if (z > 8.0) {
      return 1.0;
    }

    double sum = 0.0;
    double term = z;
    for (int i = 3; Math.abs(term) > 1e-5; i += 2) {
      sum += term;
      term *= (z * z) / i;
    }
    return 0.5 + sum * phi(z);
  }

  /**
   * Function that computes the standard Gaussian cdf using Taylor
   * approximation.
   * 
   * @param z
   *          The selected value.
   * @param mu
   *          The mean value parameter of the Normal Distribution.
   * @param s
   *          The standard deviation value parameter of the Normal Distribution.
   * 
   * @return the phi value of the input value.
   */
  protected static double bigPhi (double z, double mu, double s)
  {
    return bigPhi((z - mu) / s);
  }

  /**
   * Constructor. Sets the parameters of the standard normal distribution,
   * with mean 0 and standard deviation 1.
   */
  public Gaussian ()
  {
    name = "Generic Normal";
    type = "Normal Distribution";
    mean = 0.0;
    sigma = 1.0;
    precomputed = false;

  }

  /**
   * Constructor of a Normal distribution with given parameters.
   * 
   * @param mu
   *          Mean value of the Gaussian distribution.
   * @param s
   *          Standard deviation of the Gaussian distribution.
   */
  public Gaussian (double mu, double s)
  {
    name = "Generic";
    type = "Normal Distribution";
    mean = mu;
    sigma = s;
    precomputed = false;

  }

  public String getName ()
  {
    return name;
  }

  public String getDistributionID ()
  {
    return distributionID;
  }

  public void setDistributionID (String id)
  {
    distributionID = id;
  }

  public String getDescription ()
  {
    String description = "Gaussian probability density function";
    return description;
  }

  public int getNumberOfParameters ()
  {
    return 2;
  }

  public double getParameter (int index)
  {
    switch (index) {
    case 0:
      return mean;
    case 1:
      return sigma;
    default:
      return 0.0;
    }

  }

  public void setParameter (int index, double value)
  {
    switch (index) {
    case 0:
      mean = value;
      break;
    case 1:
      sigma = value;
      break;
    default:
      return;
    }
  }

  public void precompute (int startValue, int endValue, int nBins)
  {
    if ((startValue >= endValue) || (nBins == 0)) {
      System.out.println("Start Value > End Value or Number of Bins = 0");
      return;
    }
    precomputeFrom = startValue;
    precomputeTo = endValue;
    numberOfBins = nBins;

    double div = (endValue - startValue) / (double) nBins;
    histogram = new double[nBins];

    double residual =
      bigPhi(startValue, mean, sigma) + 1 - bigPhi(endValue, mean, sigma);
    double res2 = 1 - bigPhi(0, mean, sigma);
    residual /= res2;

    for (int i = 0; i < nBins; i++) {
      // double x = startValue + i * div - small_number;
      double x = startValue + i * div;
      histogram[i] =
        bigPhi(x + div / 2.0, mean, sigma) - bigPhi(x - div / 2.0, mean, sigma);
      histogram[i] += (histogram[i] * residual);
    }
    precomputed = true;
  }

  public double getProbability (int x)
  {
    return phi(x, mean, sigma);
  }

  public double getPrecomputedProbability (int x)
  {
    if (!precomputed) {
      return -1;
    }
    double div = (precomputeTo - precomputeFrom) / (double) numberOfBins;
    int bin = (int) Math.floor((x - precomputeFrom) / div);
    if (bin == numberOfBins) {
      bin--;
    }
    return histogram[bin];
  }

  public int getPrecomputedBin ()
  {
    if (!precomputed) {
      return -1;
    }
    Random random = new Random();
    // double div = (precomputeTo - precomputeFrom) / (double) numberOfBins;
    double dice = random.nextDouble();
    double sum = 0;
    for (int i = 0; i < numberOfBins; i++) {
      sum += histogram[i];
      // if(dice < sum) return (int)(precomputeFrom + i * div);
      if (dice < sum)
        return i;
    }
    return -1;
  }

  public void status ()
  {
    System.out.print("Normal Distribution with");
    System.out.print(" Mean: " + getParameter(0));
    System.out.println(" Sigma: " + getParameter(1));
    System.out.println("Precomputed: " + precomputed);
    if (precomputed) {
      System.out.print("Number of Bins: " + numberOfBins);
      System.out.print(" Starting Point: " + precomputeFrom);
      System.out.println(" Ending Point: " + precomputeTo);
    }
    System.out.println(Arrays.toString(histogram));

  }

  public double[] getHistogram ()
  {
    return histogram;
  }

}
