package qualm;

public class EventTemplate {

  public EventTemplate () { } 

  public EventTemplate (EventTemplate et) {
      this.type = et.type;
      this.channel = et.channel;
      this.extra1Min = et.extra1Min;
      this.extra1Max = et.extra1Max;
      this.extra2Min = et.extra2Min;
      this.extra2Max = et.extra2Max;
  }

  public static EventTemplate noteOn( int ch, String noteRange ) { 
    EventTemplate t = new EventTemplate();
    t.type = MidiCommand.NOTE_ON;
    t.channel = ch;
    t._setNoteRange(noteRange);
    return t;
  }
  public static EventTemplate noteOff( int ch, String noteRange ) {  
    EventTemplate t = new EventTemplate();
    t.type = MidiCommand.NOTE_OFF;
    t.channel = ch;
    t._setNoteRange(noteRange);
    return t;
  }

  public static EventTemplate control( int ch, String ctrl, String thresh ) {
    EventTemplate t = new EventTemplate();
    t.type = MidiCommand.CONTROL_CHANGE;
    t.channel = ch;

    int extra1;
    try {
      extra1 = Integer.parseInt(ctrl);
    } catch (NumberFormatException nfe) {
      if (ctrl.equals("modulation"))
	extra1 = 1;
      else if (ctrl.equals("breath")) 
	extra1 = 2;
      else if (ctrl.equals("foot"))
	extra1 = 4;
      else if (ctrl.equals("volume"))
	extra1 = 7;
      else if (ctrl.equals("balance"))
	extra1 = 8;
      else if (ctrl.equals("pan"))
	extra1 = 10;
      else if (ctrl.equals("expression"))
	extra1 = 11;
      else if (ctrl.equals("effect 1"))
	extra1 = 12;
      else if (ctrl.equals("effect 2"))
	extra1 = 13;
      else if (ctrl.equals("damper"))
	extra1 = 64;
      else if (ctrl.equals("sustain"))
	extra1 = 64;
      else if (ctrl.equals("portamento"))
	extra1 = 65;
      else if (ctrl.equals("sustenuto"))
	extra1 = 66;
      else if (ctrl.equals("soft"))
	extra1 = 67;
      else if (ctrl.equals("legato"))
	extra1 = 68;
      else if (ctrl.equals("all sound off"))
	extra1 = 120;
      else if (ctrl.equals("reset controllers"))
	extra1 = 121;
      else if (ctrl.equals("all notes off"))
	extra1 = 123;
      else 
	throw new IllegalArgumentException("Cannot parse control change type '" +
					   ctrl + "'");
    }
    // no range allowed for control events
    t.extra1Min = extra1;
    t.extra1Max = extra1;

    t.extra2Min = 0;
    t.extra2Max = 127;
    if (thresh != null) 
      t.extra2Min = Integer.parseInt(thresh);
    return t;
  }

  // The note range string can take the form of:
  // ([A-G][#b]?[0-9]|[0-9]+)(-([A-G][#b]?[0-9]|[0-9]+))?
  private void _setNoteRange(String rangeString) {
    // if we don't have a value, we clearly don't care.
    if (rangeString == null) {
      extra1Min = extra1Max = DONT_CARE;
      return;
    }

    // if we end in a + sign, then change it to a - for later processing
    if (rangeString.indexOf('+') != -1) {
      rangeString = rangeString.replace('+','-');
    }

    // do we have a hyphen?
    int idx = rangeString.indexOf('-');
    if (idx == -1) {
      // we have no hyphen, so this is just a single noteName.
      extra1Min = extra1Max = Utilities.noteNameToMidi(rangeString);
      return;
    }

    // OK, we have a range.  Split the string in twain.
    String lo = rangeString.substring(0,idx).trim();
    String hi = rangeString.substring(idx+1).trim();
    extra1Min = (lo.equals("") ? 0 : Utilities.noteNameToMidi(lo));
    extra1Max = (hi.equals("") ? 127 : Utilities.noteNameToMidi(hi));
  }

  public String toString() {
    return "event[" + getTypeDesc() + "/" + channel + "/" + range1() + "/" + range2() + "]";
  }

  public boolean match(MidiCommand cmd) {
    // shortcut test
    if (type != cmd.getType()) {
      // special case handling -- a note-on with a velocity of zero is
      // equivalent to a note-off, so treat it as such.
      if (type != MidiCommand.NOTE_OFF
          || cmd.getType() != MidiCommand.NOTE_ON || cmd.getData2() > 0)
        return false;
    }

    // check for bad channel or first data byte
    if (channel != DONT_CARE 
	&& channel != cmd.getChannel())
      return false;

    // check for bad match to data
    if (extra1Min != DONT_CARE
        && (extra1Min > cmd.getData1() || extra1Max < cmd.getData1()))
      return false;

    // other checks.
    if (type == MidiCommand.NOTE_ON || type == MidiCommand.NOTE_OFF)
      return true;
    if (type == MidiCommand.CONTROL_CHANGE 
	&& extra2Min <= cmd.getData2()
        && extra2Max >= cmd.getData2())
      return true;
    return false;
  }

  public int getType() { return type; }
  
  public String getTypeDesc() { 
    String name = null;
    if (type == MidiCommand.NOTE_ON) { name = "NoteOn"; }
    if (type == MidiCommand.NOTE_OFF) { name = "NoteOff"; } 
    if (type == MidiCommand.CONTROL_CHANGE) { name = "Control"; } 
    if (type == MidiCommand.SYSEX) { name = "SysEx"; }
    return name;
  }
  public int channel() { return channel; }
  public int getExtra1() { return extra1Min; } 
  public String range1() { return (extra1Min == extra1Max ? ""+extra1Min : extra1Min + "-" + extra1Max); }
  public int getExtra2() { return extra2Min; }
  public String range2() { return extra2Min + "-" + extra2Max; }

  protected int type;
  protected int channel;
  protected int extra1Min;
  protected int extra1Max;
  protected int extra2Min;
  protected int extra2Max;

  public static int DONT_CARE = -1;
  
}
