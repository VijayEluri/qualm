package qualm;

import java.util.regex.Pattern;
import javax.sound.midi.Receiver;

public class PatchChanger {
  // instance methods
  private PatchChanger() { }
  private void setChangeDelegate(ChangeDelegate cd) {
    changeDelegate = cd;
  }
  private ChangeDelegate getChangeDelegate() { return changeDelegate; }

  private static String delegatePrefix = "qualm.delegates.";

  public static void addPatchChanger( int ch,
				      String deviceType ) {
    if (changer[ch] == null)
      changer[ch] = new PatchChanger();

    // for now, we default to "Standard" as the device type
    
    if (deviceType == null) 
      deviceType = "Standard";

    // find the ChangeDelegate based on the deviceType name.
    Class delegate = null;
    String delegateName = deviceType;
    Pattern pattern = Pattern.compile( "\\W" );
    delegateName = pattern.matcher(delegateName).replaceAll("");

    try {
      delegate = Class.forName( delegatePrefix + delegateName + "Delegate" );
      if (!ChangeDelegate.class.isAssignableFrom(delegate))
	delegate = null;
    } catch (ClassNotFoundException cnfe) { }

    // if not found yet, just try the first word in the device type.
    if (delegate == null) {
      delegateName = deviceType.substring(0,deviceType.indexOf(' '));
      delegateName = pattern.matcher(delegateName).replaceAll("");
      try {
	delegate = Class.forName( delegatePrefix + delegateName + "Delegate" );
	if (!ChangeDelegate.class.isAssignableFrom(delegate)) 
	  delegate = null;
      } catch (ClassNotFoundException cnfe) { }
    }

    if (delegate == null)
      throw new RuntimeException("Could not locate patch changer for device type '" 
				 + deviceType + "'");
      
    try {
      changer[ch].setChangeDelegate( (ChangeDelegate) delegate.newInstance() );
    } catch (Exception ie) {
      throw new RuntimeException("Could not create patch changer " + delegate.getName());
    }
  }

  // and now the only important method
  public static synchronized void patchChange( ProgramChangeEvent pce,
					       Receiver midiOut ) {
    int ch = pce.getChannel();
    if (changer[ch] != null) {
      changer[ch].getChangeDelegate().patchChange(pce, midiOut);
    } else {
      throw new RuntimeException("Could not execute program change " + pce
				 + " on unknown channel " + ch);
    }
  }

  // ... except for this one, which is also fairly important
  public static synchronized void noteWindowChange( NoteWindowChangeEvent nwce,
						    Receiver midiOut ) {
    int ch = nwce.getChannel();
    if (changer[ch] != null) {
      changer[ch].getChangeDelegate().noteWindowChange(nwce, midiOut);
    } else {
      throw new RuntimeException("Could not execute note-window change " + nwce
				 + " on unknown channel " + ch);
    }
  }


  private ChangeDelegate changeDelegate;

  private static PatchChanger[] changer = new PatchChanger[16];

} 

