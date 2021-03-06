package qualm;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static qualm.EventTemplate.DONT_CARE;
import static qualm.MidiCommand.CONTROL_CHANGE;
import static qualm.MidiCommand.NOTE_OFF;
import static qualm.MidiCommand.NOTE_ON;

import org.junit.Test;

public class EventTemplateTest {

  @Test
  public void matchNoteRange() {
    // c4 = middle C = MIDI 60, A#5 = MIDI 82
    EventTemplate et = EventTemplate.noteOn( 0, "c4-A#5" );
    assertTrue(et.match( new MidiCommand( 0, NOTE_ON, 60 )));
    assertFalse(et.match( new MidiCommand( 0, NOTE_ON, 59 )));
    assertTrue(et.match( new MidiCommand( 0, NOTE_ON, 82 )));
    assertFalse(et.match( new MidiCommand( 0, NOTE_ON, 83 )));
  }

  @Test
  public void matchOpenEndedHighRange() {
    // c4 = middle C = MIDI 60, A#5 = MIDI 82
    EventTemplate et = EventTemplate.noteOn( 0, "c4-" );
    assertTrue(et.match( new MidiCommand( 0, NOTE_ON, 60 )));
    assertFalse(et.match( new MidiCommand( 0, NOTE_ON, 59 )));
    assertTrue(et.match( new MidiCommand( 0, NOTE_ON, 82 )));
  }

  @Test
  public void matchOpenEndedLowRange() {
    // c4 = middle C = MIDI 60, A#5 = MIDI 82
    EventTemplate et = EventTemplate.noteOn( 0, "-a#5" );
    assertTrue(et.match( new MidiCommand( 0, NOTE_ON, 60 )));
    assertTrue(et.match( new MidiCommand( 0, NOTE_ON, 82 )));
    assertFalse(et.match( new MidiCommand( 0, NOTE_ON, 83 )));
  }

  @Test
  public void dontMatchDifferentChannel() {
    EventTemplate et = EventTemplate.noteOn( 0, "c4" );
    assertFalse(et.match( new MidiCommand( 1, NOTE_ON, 60 )));
  }

  @Test
  public void matchDontCareChannel() {
    EventTemplate et = EventTemplate.noteOn( DONT_CARE, "c4" );
    assertTrue(et.match( new MidiCommand( 1, NOTE_ON, 60 )));
  }

  @Test
  public void dontMatchWrongType() {
    EventTemplate et = EventTemplate.noteOn( DONT_CARE, null );
    assertTrue(et.match( new MidiCommand( 0, NOTE_ON, 60 )));
    assertFalse(et.match( new MidiCommand( 0, NOTE_OFF, 60 )));
    assertFalse(et.match( new MidiCommand( 0, CONTROL_CHANGE, 60 )));
  }

  @Test
  public void matchControllerString() {
    // sustain controller is MIDI 64
    EventTemplate et = EventTemplate.control( 0, "sustain", null );
    assertTrue(et.match( new MidiCommand( 0, CONTROL_CHANGE, 64, 120 )));
  }

  @Test
  public void matchControllerThreshold() {
    // volume controller is MIDI 7
    EventTemplate et = EventTemplate.control( 0, "volume", "100" );
    System.err.println(et);
    assertFalse(et.match( new MidiCommand( 0, CONTROL_CHANGE, 7, 20 )));
    assertTrue(et.match( new MidiCommand( 0, CONTROL_CHANGE, 7, 120 )));
  }

  @Test
  public void noteOnWithZeroMatchesNoteOff() {
    EventTemplate et = EventTemplate.noteOff( 0, "c4" );
    assertTrue(et.match( new MidiCommand( 0, NOTE_ON, 60, 0 )));
  }
    
}
