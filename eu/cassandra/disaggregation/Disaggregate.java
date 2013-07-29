package disaggregation;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;

public class Disaggregate
{
  ArrayList<Event> events = new ArrayList<Event>();

  public Disaggregate (String filename) throws Exception
  {

    FileOutputStream fout = null;
    try {
      fout = new FileOutputStream("Demo/test.txt");
    }
    catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    MultiOutputStream multiOut = new MultiOutputStream(System.out, fout);
    PrintStream stdout = new PrintStream(multiOut);

    System.setOut(stdout);

    PowerDatasets data = new PowerDatasets(filename);

    EventDetector ed = new EventDetector();
    ApplianceIdentified ai = new ApplianceIdentified();

    events = ed.detectEvents(data.getActivePower(), data.getReactivePower());

    IsolatedApplianceExtractor iso = new IsolatedApplianceExtractor(events);

    ai.refrigeratorIdentification(events, iso);

    ai.washingMachineIdentification(events);

    for (Event event: events) {
      if (event.getWashingMachineFlag() == false) {
        event.detectMatchingPoints();
        event.detectSwitchingPoints();
        event.detectClusters();
        event.detectBasicShapes();
        event.createCombinations();
        // event.status2();
        event.calculateFinalPairs();
        event.status2();
        if (event.getFinalPairs().size() > 0)
          ai.analyseEvent(event);
      }
    }

    for (Appliance appliance: ai.getApplianceList())
      appliance.status();

  }

  public static void main (String[] args) throws Exception
  {
    Disaggregate dis = new Disaggregate("Demo/measurements.csv");

  }
}
