package qualm;

import javax.sound.midi.*;
import java.util.*;

public class QController implements Receiver {

  Receiver midiOut;
  QAdvancer advancer;

  public QController( Receiver out, QData data ) {
    midiOut = out;
    advancer = new QAdvancer( data );

    // send out the presets
    sendEvents( advancer.getPresets() );
    setupTriggers();
  }

  public QData getQData() { return advancer.getQData(); }
  public Cue getCurrentCue() { return advancer.getCurrentCue(); }
  public Cue getPendingCue() { return advancer.getPendingCue(); }

  QualmREPL REPL = null;
  public QualmREPL getREPL() {
    return REPL;
  }

  public void setREPL(QualmREPL newREPL) {
    this.REPL = newREPL;
  }

  private void sendEvents( Collection c ) {
    Iterator i = c.iterator();
    while(i.hasNext()) {
      ProgramChangeEvent pce = (ProgramChangeEvent)i.next();
      sendPatchChange(pce);
    }

    // update print loop
    if (REPL != null) {
      REPL.updateCue( c );
    }
  }

  private void sendPatchChange(ProgramChangeEvent pce) {
    MidiMessage patchChange = new ShortMessage();

    try {
      ((ShortMessage)patchChange)
	.setMessage( ShortMessage.PROGRAM_CHANGE, 
		     pce.getChannel(), 
		     pce.getPatch(), 0 );
      midiOut.send(patchChange, -1);
    } catch (InvalidMidiDataException e) {
      System.out.println("Unable to send Program Change: " + pce);
      System.out.println(e);
    }
  }
  
  private void setupTriggers() {
    // set up triggers
    triggers = new HashMap();
    addCurrentTrigger();
    addReverseTrigger();
    buildTriggerCache();
  }

  public void switchToCue( String cuenum ) {
    sendEvents( advancer.switchToMeasure( cuenum ) );
    setupTriggers();
  }

  // Implementation of javax.sound.midi.Receiver

  /**
   * Describe <code>close</code> method here.
   *
   */
  public void close() { }

  /**
   * Describe <code>send</code> method here.
   *
   * @param midiMessage a <code>MidiMessage</code> value
   * @param l a <code>long</code> value
   */
  long waitForTime = -1;
  private boolean ignoreEvents() {
    if (waitForTime == -1) 
      return false;
    if (System.currentTimeMillis() < waitForTime) 
      return true;
    
    waitForTime=-1;
    return false;
  }
  private void setTimeOut() {
    waitForTime = System.currentTimeMillis() + 1000; 
  }

  public void advancePatch() {
    sendEvents( advancer.advancePatch() );
  }
  public void reversePatch() {
    sendEvents( advancer.reversePatch() );
  }
  
  public void send(MidiMessage midiMessage, long l) {
    // OK, we've received a message.  Check the triggers.
    if (ignoreEvents()) 
      return;
      
    for (int i=0;i<cachedTriggers.length;i++) {
      boolean triggered = false;

      Trigger trig = cachedTriggers[i];
      if (trig.match(midiMessage)) {
	triggered = true;
	String action = (String)triggers.get(trig);
	
	// remove the trigger
	removeTrigger(trig);

	// call the appropriate action
	if (action.equals("advance")) {
	  advancePatch();
	  addCurrentTrigger();
	}
	else if (action.equals( "reverse" )) {
	  reversePatch();
	  triggers = new HashMap(); // XXX
	  addReverseTrigger();
	}
	else 
	  throw new RuntimeException("Unknown action " + action);

      }
      if (triggered) {
	buildTriggerCache();
	setTimeOut();
      }
    }
    // no match, just ignore the message.
  }


  Trigger cachedTriggers[] = {};
  Map triggers;

  private void buildTriggerCache() {
    List l = new ArrayList();
    l.addAll(triggers.keySet());
    cachedTriggers = (Trigger[]) l.toArray(new Trigger[]{});
  }

  private void addTrigger( Trigger t, String action ) {
    triggers.put(t, action);
  }

  private void removeTrigger( Trigger t ) {
    triggers.remove(t);
  }

  private void addReverseTrigger() {
    Trigger t = advancer.getQData().getReverseTrigger();
    if (t!=null)
      addTrigger( t, "reverse" );
  }

  private void addCurrentTrigger() {
    Cue cue = advancer.getPendingCue();
    if (cue != null) {
      Trigger t = cue.getTrigger(); 
      addTrigger( t, "advance" );
    }
  }
 
} // QController
