package qualm.delegates;

import javax.sound.midi.InvalidMidiDataException;

import qualm.MidiCommand;
import qualm.Patch;
import qualm.ProgramChangeEvent;
import qualm.QReceiver;

/**
 * ChangeDelegate for Korg NS5R which sends all change commands in
 * SysEx form so that they are targeted to affect a particular Part
 * (as opposed to any Part that's listening on a particular MIDI
 * channel).
 *
 * Note: make sure the Korg is set to receive sysex messages on
 * "exclusive channel 1".
 */
public class KorgNS5R_PartChangerDelegate extends KorgDelegate
{
  public void patchChange( ProgramChangeEvent pce,
			   QReceiver midiOut )
  {
    try {
      MidiCommand sysex;
      byte[] data;

      int channel = pce.getChannel();
      Patch patch = pce.getPatch();

      // Korg patches are numbered 1-128, so subtract 1
      int progNum = patch.getNumber() - 1;

      String bankName = patch.getBank();
      if (bankName != null)
      {
	// translate bank name into bank number (note: some Korg
	// banks can be selected with just an MSB)
	int msb, lsb = -1;

	if (bankName.equals("GM-a")) { msb = 0; lsb = 0; }
	else if (bankName.equals("PrgU")) msb = 0x50;
	else if (bankName.equals("PrgA")) msb = 0x51;
	else if (bankName.equals("PrgB")) msb = 0x52;
	else if (bankName.equals("PrgC")) msb = 0x53;
	else if (bankName.equals("CmbU")) msb = 0x58;
	else if (bankName.equals("CmbA")) msb = 0x59;
	else if (bankName.equals("CmbB")) msb = 0x5A;
	else if (bankName.equals("CmbC")) msb = 0x5B;
	else if (bankName.equals("GM-b")) { msb = 0x38; lsb = 0; }
	else if (bankName.equals("ySFX")) msb = 0x40;
	else if (bankName.equals("r:CM")) msb = 0x7D;
	else if (bankName.equals("yDr1")) msb = 0x7E;
	else if (bankName.equals("yDr2")) msb = 0x7F;
	else if (bankName.equals("rDrm")) msb = 0x3D;
	else if (bankName.equals("kDrm")) msb = 0x3E;
	else if (bankName.equals("****")) msb = 0x3F;
	else if (bankName.equals("y100")) { msb = 0; lsb = 100; }
	else if (bankName.equals("y101")) { msb = 0; lsb = 101; }

	// handle bank names like "r:17" (note: DECIMAL digits!)
	else if (bankName.startsWith("r:"))
	{
	  msb = Integer.parseInt(bankName.substring(2));

	  // note: the range below still includes some values that are
	  // not actually valid, but the effort required to check any
	  // more carefully than this would far outweigh the benefits.
	  if (msb < 1 || msb > 40)
	    throw new Exception("invalid bank name: " + bankName);
	}

	// handle bank names like "y:99" (note: DECIMAL digits!)
	else if (bankName.startsWith("y:"))
	{
	  msb = 0;
	  lsb = Integer.parseInt(bankName.substring(2));

	  // note: the range below still includes some values that are
	  // not actually valid, but the effort required to check any
	  // more carefully than this would far outweigh the benefits.
	  if (lsb < 1 || lsb > 99)
	    throw new Exception("invalid bank name: " + bankName);
	}

	else
	  throw new Exception("invalid bank name: " + bankName);

	// send SysEx to select Bank MSB for Part
	data = new byte[]
	  { (byte) 0xF0, 0x42, 0x30, 0x42, 0x12, 1, (byte) channel, 0,
	    (byte) msb, (byte) 0xF7 };

	sysex = new MidiCommand();
	sysex.setSysex( data );

	if (midiOut != null)
	  midiOut.handleMidiCommand(sysex);

	// send SysEx to select Bank LSB for Part, if needed
	if (lsb != -1)
	{
	  data[7] = 1;
	  data[8] = (byte) lsb;

	  sysex = new MidiCommand();
	  sysex.setSysex( data );

	  if (midiOut != null)
	    midiOut.handleMidiCommand(sysex);
	}
      }

      // send SysEx to select Program for Part
      data = new byte[]
	{ (byte) 0xF0, 0x42, 0x30, 0x42, 0x12, 1, (byte) channel, 2,
	  (byte) progNum, (byte) 0xF7 };

      sysex = new MidiCommand();
      sysex.setSysex( data );

      if (midiOut != null)
	midiOut.handleMidiCommand(sysex);


      if (patch.getVolume() != null)
      {
	// send SysEx to set volume for Part
	data = new byte[]
	  { (byte) 0xF0, 0x42, 0x30, 0x42, 0x12, 1, (byte) channel, 0x10,
	    patch.getVolume().byteValue(), (byte) 0xF7 };

	sysex = new MidiCommand();
	sysex.setSysex( data );

	if (midiOut != null)
	  midiOut.handleMidiCommand(sysex);
      }

    } catch (InvalidMidiDataException e) {
      System.out.println("Unable to send Program Change: " + pce);
      System.out.println(e);
    } catch (Exception e2) {
      e2.printStackTrace();
    }
  }

}
