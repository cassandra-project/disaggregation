package disaggregation;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

public class PowerDatasets
{

  double[] activePower;
  double[] reactivePower;
  Map<Integer, Double> activeMap = new TreeMap<Integer, Double>();
  Map<Integer, Double> reactiveMap = new TreeMap<Integer, Double>();

  public PowerDatasets (String filename) throws FileNotFoundException
  {

    File file = new File(filename);

    Scanner input = new Scanner(file);

    while (input.hasNext()) {

      String line = input.next();

      // System.out.println(line);

      String[] contents = line.split(",");

      activeMap.put(Integer.parseInt(contents[0]),
                    Double.parseDouble(contents[1]));
      reactiveMap.put(Integer.parseInt(contents[0]),
                      Double.parseDouble(contents[2]));

    }

    input.close();

    activePower = new double[activeMap.size()];
    reactivePower = new double[activeMap.size()];
    int counter = 0;

    for (Integer timestamp: activeMap.keySet()) {

      activePower[counter] = activeMap.get(timestamp);
      reactivePower[counter] = reactiveMap.get(timestamp);

      counter++;
    }

    // System.out.println(Arrays.toString(activePower));
    // System.out.println(Arrays.toString(reactivePower));

  }

  public double[] getActivePower ()
  {
    return activePower;
  }

  public double[] getReactivePower ()
  {
    return reactivePower;
  }

  public double getActivePower (int index)
  {
    return activePower[index];
  }

  public double getReactivePower (int index)
  {
    return reactivePower[index];
  }
}
