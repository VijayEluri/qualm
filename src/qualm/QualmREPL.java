package qualm;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import qualm.notification.CueChange;
import qualm.notification.EventMapActivation;
import qualm.notification.PatchChange;
import qualm.notification.QualmNotifier;

public class QualmREPL extends Thread implements CueChange, PatchChange {
  
  MasterController controller = null;
  private final BufferedReader reader;
  final PrintWriter output;
  private String inputFilename = null;
  private boolean isRunning = false;
  private boolean readlineHandlesPrompt = false;

  public QualmREPL( MasterController mc ) {
    this(new InputStreamReader( System.in ), new PrintWriter( System.out ), mc);
  }

  QualmREPL(Reader reader, Writer output, MasterController mc) {
    this.reader = new BufferedReader(reader);
    this.output = new PrintWriter(output);
    controller = mc;
    // add this controller to the notification manager
    controller.getNotificationManager().addNotifier(this);
    loadPreferences();
  }
  
  public void loadFilename( String filename ) {
    inputFilename = filename;
    try {
      controller.loadFilename(filename);
    } catch (IOException e) {
      output.println( "Unable to load " + filename + ": " + e);
    }
    output.println( "Loaded data from " + filename );

    if (isRunning)
      reset();
  }
  
  // visible for testing
  void loadPreferences() {
    try {
      if (controller != null && controller.getPreferencesManager() != null)
        controller.getPreferencesManager().loadPreferences();
      
    } catch (IllegalArgumentException iae) {
      output.println("Preferences: could not create or identify plugin '" + iae.getMessage() +
      "'; ignoring.");
    }
  }

  private void savePreferences() {
    controller.getPreferencesManager().savePreferences();
  }

  private String promptString() {
    String prompt="";
    
    boolean init = true;
    for (QController qc : controller.getControllers()) {
      if (!init) prompt += " | ";
      init = false;

      Cue curQ = qc.getCurrentCue();
      Cue pendingQ = qc.getPendingCue();

      if (curQ == null) 
	prompt += "START";
      else prompt += curQ.getCueNumber();

      prompt += "-";

      if (pendingQ == null) 
	prompt += "END";
      else prompt += pendingQ.getCueNumber();
    }

    return prompt + "> ";
  }

  public void updatePrompt() {
    output.print( promptString() );
    output.flush();
  }

  @Override
  public void run() {
    isRunning = true;

    // first, we reset the controllers.
    readlineHandlesPrompt = true;
    reset();
    readlineHandlesPrompt = false;

    while (true) {
      try {
	updatePrompt();

	String line = reader.readLine();
	processLine( line );
      } 
      catch (EOFException e) {
	break;
      } 
      catch (Exception e) {
	output.println(e);
      }
    }
  }

  public void updateCue( Collection<QEvent> c ) {
  }

  private void reset() { 
    gotoCue("0.0");
  }

  private void advanceController (String line) {
    String stream_id = line.substring(line.indexOf(" ")).trim();
    controller.advanceStream(stream_id);
  }

  private void reverseController (String line) {
    String stream_id = line.substring(line.indexOf(" ")).trim();
    controller.reverseStream(stream_id);
  }

  private void gotoCue(String cueName) { controller.gotoCue(cueName); }

  /* visible for unit testing */
  void processLine( String line ) {
    readlineHandlesPrompt = true;

    if (line == null || line.trim().equals("") ||
	line.trim().startsWith("\\") ||
	line.trim().startsWith("]")) {
      // advance the "mainline" patch
      controller.advanceMainPatch();
    } else {
      String lowerCase = line.toLowerCase();

      if (lowerCase.equals("quit")) {
	System.exit(0);
      }

      if (lowerCase.equals("dump")) {
	controller.getQData().dump(output);
      
      } else if (lowerCase.equals("reset")) {
	reset();

      } else if (lowerCase.equals("showmidi")) {
        VerboseReceiver.setDebugMIDI(true);
      } else if (lowerCase.equals("unshowmidi")) {
        VerboseReceiver.setDebugMIDI(false);

      } else if (lowerCase.startsWith("plugin")) {
	parsePluginLine(line);

      } else if (lowerCase.startsWith("save")) {
	savePreferences();

      } else if (lowerCase.startsWith("adv")) {
	advanceController(line);

      } else if (lowerCase.startsWith("rev")) {
	reverseController(line);

      } else if (lowerCase.startsWith("load")) {
	StringTokenizer st = new StringTokenizer(line);
	st.nextToken();
	String filename = st.nextToken();
	loadFilename( filename );

      } else if (lowerCase.startsWith("showxml")) {
    	QDataXMLReader.outputXML(controller.getQData(), output);
    	output.println("");
    	  
      } else if (lowerCase.startsWith("reload")) {
	loadFilename( inputFilename );

      } else if (lowerCase.startsWith("version")) {
	output.println( Qualm.versionString() );

      } else {
	gotoCue(line);
      }
    }

    readlineHandlesPrompt = false;
  }

  void addPlugin(String name) {
    if (controller != null) {
      controller.getNotificationManager().addNotification(name);
    }
  }

  private void removePlugin(String name) {
    Set<QualmNotifier> removed = controller.removePlugin(name);
    for(QualmNotifier plugin : removed) {
      output.println("Removed plugin " + plugin.getClass().getName());
    }
    if (removed.size() == 0) {
      output.println("Unable to find running plugin " + name);
    }
  }

  private void parsePluginLine(String line) {
    // XXX there's probably a better way to handle plugins.  Checkout 
    // http://jpf.sourceforge.net
    StringTokenizer st = new StringTokenizer(line);
    String tok = st.nextToken();
    if (!tok.equals("plugin")) {
      output.println("Odd error: plugin spec line did not start with 'plugin'");
      return;
    }

    boolean remove = false;

    tok = st.nextToken();
    if (tok.equals("list")) {
      displayPluginList();
      return;

    } else if (tok.equals("remove")) {
      remove = true;
      tok = st.nextToken();
    } 

    try {
      if (remove)
        removePlugin(tok);
      else
        addPlugin(tok);
    } catch (IllegalArgumentException iae) {
      output.println("Unable to create or identify requested plugin '" + tok + "; ignoring request.");
    }
  }

  private void displayPluginList() {
    NotificationManager pm = controller.getNotificationManager();
    NotificationMap notifiers = new NotificationMap();
    for (CueChange ccn : pm.getCueNotifiers())
      notifiers.add(ccn.getClass().getName(), "cue"); 
    
    for (PatchChange pcn : pm.getPatchNotifiers())
      notifiers.add(pcn.getClass().getName(), "patch");

    for (EventMapActivation emn : pm.getMapNotifiers())
      notifiers.add(emn.getClass().getName(), "mapper");
    
    for (String name : notifiers.keySet()) {
      output.print(name + ": " + notifiers.joinedValues(name, ", "));
    }
  }

  /* QualmNotification overrides */
  
  @Override
  public void patchChange(int channel, String channelName, Patch patch) {
    output.println( channelName + " -> " + patch.getDescription() );
  }

  @Override public void cueChange(MasterController mc) { }

  @Override
  public void beginNotifications() {
    if (!readlineHandlesPrompt) {
      // end the current line
      output.print( "\n" );
    }
  }

  @Override
  public void endNotifications() {
    if (!readlineHandlesPrompt) {
      updatePrompt();
    }
  }

  // a simple multi-map implmentation
  class NotificationMap extends HashMap<String, Set<String>> {
    private static final long serialVersionUID = 1L;
    public void add(String key, String val) {
      if (!containsKey(key)) {
        put(key, new TreeSet<String>());
      }
      Set<String> vals = get(key);
      vals.add(val);
      put(key, vals);
    }
    
    public String joinedValues(String key, String delim) {
      boolean first = true;
      StringBuilder output = new StringBuilder();
      for (String val : get(key)) {
        if (!first) {
          output.append(delim);
        }
        output.append(val);
        first = false;
      }
      return output.toString();
    }
  }
  
}
